package com.clinbrain

import com.clinbrain.common.Config
import com.clinbrain.utils.{CommandUtil, UtilHelper}
import org.apache.hudi.DataSourceWriteOptions
import org.apache.hudi.config.HoodieWriteConfig
import org.apache.spark.sql.{DataFrameWriter, Row, SaveMode, SparkSession}

/**
 * import hdfs data to hoodie
 */
object HdfsDataImporter {
    def main(args: Array[String]): Unit = {
        println("[ begin to import hdfs data to hoodie... ]")
        val configParam = new Config(args)
        println(configParam.toString)

        println("[ begin start read spark conf... ]")
        val conf = UtilHelper.buildSparkConf(configParam.sparkConfigFilePath)
        println("[ begin start read hoodie conf... ]")
        val allHoodieConfig = UtilHelper.loadProperties(configParam.hoodieConfigFilePath)
        println(allHoodieConfig)
        val spark = SparkSession.builder()
                .appName("data-import-" + configParam.tableName)
                .config(conf)
                .getOrCreate()

        if (!UtilHelper.checkSourcePathHasData(configParam.sourcePath, spark.sparkContext.hadoopConfiguration)) {
            println("source path [%s] has no data. don't import".format(configParam.sourcePath))
            sys.exit(0)
        }

        println("[ begin read data from hdfs... ]")
        val insertData = configParam.readDataFormat match {
            case "json" => spark.read.json(configParam.sourcePath)
            case "orc" => spark.read.orc(configParam.sourcePath)
            case "parquet" => spark.read.parquet(configParam.sourcePath)
            case _ => throw new RuntimeException("data format error, support json orc parquet.")
        }

        println("[ begin write data ro hudi... ]")
        /**
         * 分区/非分区 写入
         */
        val data: DataFrameWriter[Row] = insertData.write.format("org.apache.hudi")
                .option(HoodieWriteConfig.TABLE_NAME, "%s.%s".format(configParam.database, configParam.tableName))
                .option(DataSourceWriteOptions.TABLE_NAME_OPT_KEY, configParam.tableName)
                .option(DataSourceWriteOptions.HIVE_TABLE_OPT_KEY, configParam.tableName)
                .option(DataSourceWriteOptions.RECORDKEY_FIELD_OPT_KEY, configParam.rowKey)
                .option(DataSourceWriteOptions.PRECOMBINE_FIELD_OPT_KEY, configParam.combineColumn)
                .option(DataSourceWriteOptions.HIVE_DATABASE_OPT_KEY, configParam.database)
                .option(DataSourceWriteOptions.KEYGENERATOR_CLASS_OPT_KEY, configParam.keyGeneratorClass)
                .option(DataSourceWriteOptions.HIVE_PARTITION_EXTRACTOR_CLASS_OPT_KEY, configParam.partitionExtractorClass)
                .option(DataSourceWriteOptions.HIVE_SYNC_ENABLED_OPT_KEY, configParam.hiveSyncEnable)
                .options(allHoodieConfig)
        if (!"null".equalsIgnoreCase(configParam.hudiPartitionColumn)) {
            data.option(DataSourceWriteOptions.PARTITIONPATH_FIELD_OPT_KEY, configParam.hudiPartitionColumn)
                    .option(DataSourceWriteOptions.HIVE_PARTITION_FIELDS_OPT_KEY, configParam.hudiPartitionColumn)
        }

        data.mode(if (configParam.saveMode.equalsIgnoreCase(SaveMode.Overwrite.name())) SaveMode.Overwrite else SaveMode.Append)
                .save(configParam.destPath)

        println("[################################ IMPORT SUCCESS #################################]")
        spark.stop()

        val syncCommand: String = UtilHelper.buildHiveSyncShell(configParam, allHoodieConfig)
        val commandLogPath: String = "%simport_%s.%s.log".format(configParam.logPath, configParam.database, configParam.tableName)
        println("sync table meta to hive: %s".format(syncCommand))
        println("sync log path: %s".format(commandLogPath))
        CommandUtil.execCommand(syncCommand, commandLogPath)
    }
}