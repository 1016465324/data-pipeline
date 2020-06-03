package com.clinbrain.utils

import java.io.FileInputStream
import java.util.Properties

import com.clinbrain.common.Config
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{LocatedFileStatus, Path, RemoteIterator}
import org.apache.hudi.client.HoodieWriteClient
import org.apache.hudi.exception.HoodieIOException
import org.apache.spark.SparkConf

object UtilHelper {
    def loadProperties(path: String): Map[String, String] = {
        if (StringUtils.isEmpty(path)) {
            throw new HoodieIOException("properties file path is null or empty.")
        }

        val props = new Properties()
        val inputStream = new FileInputStream(path)
        props.load(inputStream)
        inputStream.close()

        var allConfig = Map[String, String]()
        val iter = props.entrySet().iterator()
        while (iter.hasNext) {
            val entry = iter.next()
            val key = entry.getKey.toString
            val value = if (null == entry.getValue) "" else entry.getValue.toString
            allConfig += (key -> value)
        }

        allConfig
    }

    def buildSparkConf(sparkConfigPath: String): SparkConf = {
        val conf = new SparkConf()
        loadProperties(sparkConfigPath).foreach(one => {
            conf.set(one._1, one._2)
        })

        HoodieWriteClient.registerClasses(conf)
    }

    def checkSourcePathHasData(sourcePath: String, config: Configuration): Boolean = {
        val hdfsPath = new Path(sourcePath)
        val fileSystem = hdfsPath.getFileSystem(config)
        val exists = fileSystem.exists(hdfsPath)
        if (exists) {
            var allFileSize: Long = 0
            val allFile: RemoteIterator[LocatedFileStatus] = fileSystem.listFiles(hdfsPath, true)
            while (allFile.hasNext) {
                val file: LocatedFileStatus = allFile.next()
                allFileSize += file.getLen

                if (allFileSize > 0) {
                    return true
                }
            }

            false
        } else {
            false
        }
    }

    def buildHiveSyncShell(configParam: Config, allHoodieConfig: Map[String, String]): String = {
        val stringBuilder = new StringBuilder("sh %s ")
        stringBuilder.append("--base-path %s ")
        stringBuilder.append("--database %s ")
        stringBuilder.append("--table %s ")
        stringBuilder.append("--jdbc-url '%s' ")
        stringBuilder.append("--partition-value-extractor %s ")
        stringBuilder.append("--user %s ")
        stringBuilder.append("--pass %s ")

        if (!"null".equalsIgnoreCase(configParam.partitionColumn)) {
            if (configParam.destPath.startsWith("/datalake")) {
                stringBuilder.append("--partitioned-by date_prt ")
            } else if (configParam.destPath.startsWith("/datacenter")) {
                stringBuilder.append("--partitioned-by medorgcode,date_prt ")
            } else {
                if (configParam.database.equalsIgnoreCase("dc_db")) {
                    stringBuilder.append("--partitioned-by medorgcode,date_prt ")
                } else {
                    stringBuilder.append("--partitioned-by date_prt ")
                }
            }

        }

        stringBuilder.toString().format(allHoodieConfig("hive.sync.shell.path"), configParam.destPath,
            configParam.database, configParam.tableName,
            allHoodieConfig("hoodie.datasource.hive_sync.jdbcurl"),
            configParam.partitionExtractorClass,
            allHoodieConfig("hoodie.datasource.hive_sync.username"),
            allHoodieConfig("hoodie.datasource.hive_sync.password"))

    }
}
