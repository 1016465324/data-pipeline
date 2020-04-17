package com.clinbrain

import java.io.File
import java.text.SimpleDateFormat

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import com.clinbrain.database.CachedbRecord
import com.clinbrain.global.CacheGlobalConfig
import com.clinbrain.source.cache.CDCGlobalLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

object CachedbMessageBackup {

    val logger: Logger = LoggerFactory.getLogger(CachedbMessageBackup.getClass)

    def main(args: Array[String]): Unit = {
        if (args.length != 4) {
            println("usage:")
            println("    CachedbMessageBackup global.xlsx conf.properties sqluser_map.txt database-namespace.csv")
            return
        }

        val props = UtilHelper.loadProperties(args(1))

        val cacheGlobalConfig = new CacheGlobalConfig
        cacheGlobalConfig.init(args(0))

        val sqluserTablePerNamespace = UtilHelper.loadSQLUserMap(args(2))
        val schemaMapNamespace = UtilHelper.loadSchemaNamespace(args(3))

        val ssc: StreamingContext = UtilHelper.buildStreamingContext("CachedbMessageBackup", props)

        val bcCacheDBConfig = ssc.sparkContext.broadcast(cacheGlobalConfig)
        val bcProps = ssc.sparkContext.broadcast(props)
        val bcSQLUserTablePerNamespace = ssc.sparkContext.broadcast(sqluserTablePerNamespace)
        val bcSchemaMapNamespace = ssc.sparkContext.broadcast(schemaMapNamespace)

        //kafka参数
        val kafkaParams = UtilHelper.buildKafkaParams(props)
        //目前只处理一个topic
        val topics = props.getProperty("topics").split(",")
        val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](ssc,
            LocationStrategies.PreferConsistent,
            Subscribe[String, String](topics, kafkaParams))
        //kafka下标
        var offsetRanges = Array[OffsetRange]()
        stream.foreachRDD { rdd =>
            if (!rdd.isEmpty()) {
                println("count: " + rdd.count())
                val timeDir = UtilHelper.getDateDir
                //获取下标
                offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

                val data: RDD[(String, ListBuffer[String])] = rdd.map(_.value().toString).repartition(6).mapPartitions(partition => {
                    val bcPropsValue = bcProps.value
                    val tmpCacheDBConfig = bcCacheDBConfig.value
                    val bcSQLUserTablePerNamespaceValue = bcSQLUserTablePerNamespace.value
                    val bcSchemaMapNamespaceValue = bcSchemaMapNamespace.value
                    partition.map(message => {
                        //转对象
                        val cdcGlobalLog: CDCGlobalLog = JSON.parseObject(message, classOf[CDCGlobalLog])

                        //获取时间、时间处理
                        val LogTimeStamp = cdcGlobalLog.getTS
                        val sdf = new SimpleDateFormat(bcPropsValue.getProperty("cdclog.dateDirFormat"))

                        val partitionDate = sdf.parse(LogTimeStamp)
                        val partitionDir: String = sdf.format(partitionDate)

                        //通过维护的dbname与namespace的关系获取dbname所属的所有namespace，遍历查询
                        val dbName = cdcGlobalLog.getDBName.substring(cdcGlobalLog.getDBName.lastIndexOf(":") + 1).toLowerCase
                        val allNamespace = bcSchemaMapNamespaceValue.get(dbName)
                        if (null == allNamespace) {
                            (null, null)
                        } else {
                            var exist = false
                            val iter = allNamespace.iterator()
                            while (iter.hasNext && !exist) {
                                val namespace = iter.next()
                                val record: CachedbRecord = tmpCacheDBConfig.findByGlobal("^" + cdcGlobalLog.getGlobalName,
                                    namespace, bcSQLUserTablePerNamespaceValue.get(namespace))
                                if (record != null) {
                                    exist = true
                                }
                            }

                            var hdfsdir = ""
                            if (exist) {
                                hdfsdir = bcPropsValue.getProperty("cdclog.resultDir") + dbName +
                                        File.separator + partitionDir +
                                        File.separator + timeDir
                            } else {
                                hdfsdir = bcPropsValue.getProperty("cdclog.otherResultDir") + dbName +
                                        File.separator + partitionDir +
                                        File.separator + timeDir
                            }

                            val result = JSON.toJSONString(cdcGlobalLog, SerializerFeature.WriteMapNullValue)
                            (hdfsdir, ListBuffer(result))
                        }
                    })
                }).filter(_._1 != null).reduceByKey(_.++=:(_)).cache()

                val dirs: Array[String] = data.map(_._1).collect()
                for (dir <- dirs) {
                    val result: RDD[(String, ListBuffer[String])] = data.filter(_._1.equals(dir))
                    result.flatMap(_._2).repartition(1).saveAsTextFile(dir)
                }

                data.unpersist()
                //更新偏移量。数据处理完更新偏移量到kafkagroup中
                stream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
            } else {
                println("rdd is empty.")
            }
        }

        ssc.start()
        //等待关闭
        ssc.awaitTermination()
    }

}