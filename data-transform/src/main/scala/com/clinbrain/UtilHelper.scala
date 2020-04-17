package com.clinbrain

import java.io.{File, FileInputStream}
import java.util
import java.util.{Calendar, Properties}

import com.alibaba.fastjson.JSON
import com.clinbrain.database.TableMeta
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.io.Source

object UtilHelper {

    def loadProperties(propPath: String): Properties = {
        val props = new Properties()
        val input = new FileInputStream(propPath)
        props.load(input)
        input.close()

        props
    }

    def loadTableMetadata(tableMetadataPath: String): util.HashMap[String, TableMeta] = {
        val allDBTableMeta = new util.HashMap[String, TableMeta]()

        val file = new File(tableMetadataPath)
        if (file.exists) {
            val input = Source.fromFile(tableMetadataPath)
            var i = 1
            for (itr <- input.getLines()) {
                val tableMetadata = itr.toString
//                println(i)
                if (!tableMetadata.isEmpty) {
                    val index = tableMetadata.indexOf(CachedbMetadataExport.META_SEPARATOR)
                    val key = tableMetadata.substring(0, index)
                    val json = tableMetadata.substring(index + CachedbMetadataExport.META_SEPARATOR.length)
                    val tableMeta = JSON.parseObject(json, classOf[TableMeta])
                    allDBTableMeta.put(key, tableMeta)
                }

                i = i + 1
            }

            input.close()
        } else {
            throw new RuntimeException("table metadata file not exist.")
        }

        println("database table size: " + allDBTableMeta.size())
        allDBTableMeta
    }

    def loadSQLUserMap(path: String): util.HashMap[String, util.HashSet[String]] = {
        val sqluserTablePerNamespace = new util.HashMap[String, util.HashSet[String]]()
        val file = new File(path)
        if (file.exists()) {
            val input = Source.fromFile(path)
            for (itr <- input.getLines()) {
                val line = itr.toString
                //namespace,entityName,tableName
                if (!line.isEmpty) {
                    val fields = line.toLowerCase.split(",")
                    var allSQLUserTable: util.HashSet[String] = sqluserTablePerNamespace.get(fields(0))
                    if (null == allSQLUserTable) {
                        allSQLUserTable = new util.HashSet[String]()
                        allSQLUserTable.add(fields(1))

                        sqluserTablePerNamespace.put(fields(0), allSQLUserTable)
                    } else {
                        allSQLUserTable.add(fields(1))
                    }
                }
            }

            input.close()
        } else {
            throw new RuntimeException(path + " not exist.")
        }

        println("namespace size: " + sqluserTablePerNamespace.size())
        sqluserTablePerNamespace
    }

    def loadSchemaNamespace(path: String): util.HashMap[String, util.HashSet[String]] = {
        val schemaMapNamespace = new util.HashMap[String, util.HashSet[String]]()
        val file = new File(path)
        if (file.exists()) {
            val input = Source.fromFile(path)
            for (itr <- input.getLines()) {
                val line = itr.toString
                //namespace,schema
                if (!line.isEmpty) {
                    val fields = line.toLowerCase.split(",")
                    var allNamespace: util.HashSet[String] = schemaMapNamespace.get(fields(1))
                    if (null == allNamespace) {
                        allNamespace = new util.HashSet[String]()
                        allNamespace.add(fields(0))

                        schemaMapNamespace.put(fields(1), allNamespace)
                    } else {
                        allNamespace.add(fields(0))
                    }
                }
            }

            input.close()
        } else {
            throw new RuntimeException(path + " not exist.")
        }

        println("schema size: " + schemaMapNamespace.size())
        schemaMapNamespace
    }

    def getDateDir: String = {
        val calendar = Calendar.getInstance()
        "%04d%02d%02d%02d%02d%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND))
    }

    def buildStreamingContext(appName: String, props: Properties): StreamingContext = {
        val interval = Integer.parseInt(props.getProperty("cdclog.consumeTime"))
        val checkpointPath = props.getProperty("cdclog.checkPointDir")
        val conf: SparkConf = new SparkConf().setAppName(appName)
        conf.set("spark.streaming.kafka.maxRatePerPartition", props.getProperty("spark.streaming.kafka.maxRatePerPartition"))
        conf.set("spark.streaming.backpressure.enabled", props.getProperty("spark.streaming.backpressure.enabled"))
        val ssc: StreamingContext = new StreamingContext(conf, Seconds(interval))
        ssc.checkpoint(checkpointPath)
        ssc
    }

    def buildKafkaParams(props: Properties): Map[String, Object] = {
        Map[String, Object](
            "bootstrap.servers" -> props.getProperty("bootstrap.servers"),
            "key.deserializer" -> classOf[StringDeserializer],
            "value.deserializer" -> classOf[StringDeserializer],
            "group.id" -> props.getProperty("cdclog.groupId"),
            "auto.offset.reset" -> props.getProperty("cdclog.reset"),
            "enable.auto.commit" -> (false: java.lang.Boolean),
            "security.protocol" -> "SASL_PLAINTEXT",
            "sasl.mechanism" -> "GSSAPI",
            "sasl.kerberos.service.name" -> "kafka"
        )
    }
}
