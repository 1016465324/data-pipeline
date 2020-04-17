package com.clinbrain

import com.alibaba.fastjson.JSON
import com.clinbrain.database.CachedbRecord
import com.clinbrain.global.CacheGlobalConfig
import com.clinbrain.sink.KafkaSink
import com.clinbrain.source.cache.CDCGlobalLog
import com.intersys.objects.CacheDatabase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010._
import org.slf4j.{Logger, LoggerFactory}

object CachedbMessageBuild {

    val logger: Logger = LoggerFactory.getLogger(CachedbMessageBuild.getClass)



    def main(args: Array[String]): Unit = {
        if (args.length != 5) {
            println("usage:")
            println("    CachedbMessageBuild global.xlsx table_metadata.txt conf.properties sqluser_map.txt database-namespace.csv")
            return
        }

        val props = UtilHelper.loadProperties(args(2))

        //初始化config配置
        val cacheGlobalConfig = new CacheGlobalConfig
        cacheGlobalConfig.init(args(0))
        //初始化dbMetadata
        val allDBTableMeta = UtilHelper.loadTableMetadata(args(1))
        val sqluserTablePerNamespace = UtilHelper.loadSQLUserMap(args(3))
        val schemaMapNamespace = UtilHelper.loadSchemaNamespace(args(4))

        val ssc: StreamingContext = UtilHelper.buildStreamingContext("CachedbMessageBuild", props)

        val broadData = ssc.sparkContext.broadcast(cacheGlobalConfig)
        val broadMeta = ssc.sparkContext.broadcast(allDBTableMeta)
        val bcProps = ssc.sparkContext.broadcast(props)
        val bcSQLUserTablePerNamespace = ssc.sparkContext.broadcast(sqluserTablePerNamespace)
        val bcSchemaMapNamespace = ssc.sparkContext.broadcast(schemaMapNamespace)

        //kafka参数
        val kafkaParams = UtilHelper.buildKafkaParams(props)
        //目前只处理一个topic
        val topics = props.getProperty("topics").split(",")
        val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.
                createDirectStream[String, String](ssc,
                    LocationStrategies.PreferConsistent, Subscribe[String, String](topics, kafkaParams))
        //kafka下标
        var offsetRanges = Array[OffsetRange]()
        stream.foreachRDD { rdd =>
            if (!rdd.isEmpty()) {
                println("count: " + rdd.count())
                //获取下标
                offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

                val errorMessageDir = props.getProperty("cdclog.queryMessageErrorDir") + UtilHelper.getDateDir
                rdd.mapPartitions(partition => {
                    val bcSQLUserTablePerNamespaceValue = bcSQLUserTablePerNamespace.value
                    val bcSchemaMapNamespaceValue = bcSchemaMapNamespace.value
                    val tmpCacheDBConfig = broadData.value
                    val broadMetaValue = broadMeta.value
                    val bcPropsValue = bcProps.value
                    val producer = new KafkaSink(bcPropsValue)
                    val cacheDatabase = CacheDatabase.getDatabase(bcPropsValue.getProperty("url"),
                        bcPropsValue.getProperty("username"), bcPropsValue.getProperty("password"))
                    val sendTopic = bcPropsValue.getProperty("ogg.topics")

                    val allNamespaceConnection = bcPropsValue.getProperty("namespaces").split(",").map(namespace => {
                        (namespace.toLowerCase, CacheDatabase.getDatabase(bcPropsValue.getProperty("host") + namespace,
                            bcPropsValue.getProperty("username"), bcPropsValue.getProperty("password")))
                    }).toMap

                    println(allNamespaceConnection)
                    val errorMessagePartition = partition.map(message => {
                        println("message: " + message.value().toString)

                        var errorMessage: String = null
                        //转对象
                        val cdcGlobalLog: CDCGlobalLog = JSON.parseObject(message.value().toString, classOf[CDCGlobalLog])

                        //通过维护的dbname与namespace的关系获取dbname所属的所有namespace，遍历查询
                        val dbName = cdcGlobalLog.getDBName.substring(cdcGlobalLog.getDBName.lastIndexOf(":") + 1).toLowerCase
                        val allNamespace = bcSchemaMapNamespaceValue.get(dbName)
                        if (null != allNamespace) {
                            var exist = false
                            val iter = allNamespace.iterator()
                            while (iter.hasNext && !exist) {
                                val namespace = iter.next()
                                val cachedbRecord: CachedbRecord = tmpCacheDBConfig.findByGlobal("^" + cdcGlobalLog.getGlobalName,
                                    namespace, bcSQLUserTablePerNamespaceValue.get(namespace))
                                if (cachedbRecord != null) {
                                    try {
                                        val cacheConnection = CacheDatabase.getDatabase(bcPropsValue.getProperty("host") + namespace,
                                            bcPropsValue.getProperty("username"), bcPropsValue.getProperty("password"))
                                        val result = cachedbRecord.queryObject(cacheConnection)
                                        cacheConnection.close()
                                        if (null != result) {
                                            producer.sendMessage(sendTopic, result, cachedbRecord.getTableName)
                                            logger.info("sending message to kafka: " + result)
                                        } else {
                                            logger.warn("error message: " + message.value().toString + " in namespace " + namespace)
                                            errorMessage = "error message: " + message.value().toString + " in namespace " + namespace
                                        }
                                    } catch {
                                        case e: Exception =>
                                            e.printStackTrace()
                                            errorMessage = "error message: " + message.value().toString + " in namespace " + namespace + ", exception: " + e.getMessage
                                    }

                                    exist = true
                                }
                            }
                        }

                        errorMessage
                    }).filter(_ != null)

                    producer.close()
                    cacheDatabase.close()
                    allNamespaceConnection.foreach(_._2.close())

                    errorMessagePartition
                }).saveAsTextFile(errorMessageDir)
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