package com.clinbrain.common

import com.alibaba.fastjson.{JSON, JSONObject}

/**
 * @ClassName Config
 * @Description TODO
 * @Author p
 * @Date 2020/4/26 16:13
 * @Version 1.0
 **/
class Config(args: Array[ String ]) {
    if (args.length != 4) {
        println("example:")
        print("    HdfsDataImporter paramJson sparkConfigFilePath hoodieConfigFilePath logPath")
        throw new RuntimeException("param error.")
    }

    val params: JSONObject = JSON.parseObject(args(0))

    //spark参数
    var sparkConfigFilePath: String = args(1)
    //hoodie参数
    var hoodieConfigFilePath: String = args(2)
    //执行shell命令日志路径
    var logPath: String = args(3)
    //写入模式
    var saveMode: String = params.getString("hudi_save_mode")
    //目标表
    var tableName: String = params.getString("hudi_table_name")
    //合并字段
    var combineColumn: String = params.getString("hudi_combine_column")
    //源hdfs文件路径
    var sourcePath: String = params.getString("hudi_source_path")
    //写入目标路径
    var destPath: String = params.getString("hudi_dest_path")
    //hudi分区字段
    var hudiPartitionColumn: String = params.getString("hudi_partition_column")
    //hive分区字段
    var hivePartitionColumn: String = params.getString("hive_partition_column")
    //主键字段
    var rowKey: String = params.getString("hudi_pk_column")
    //hudi库名
    var database: String = params.getString("hudi_database")
    //hudi  keygeneratorClass
    var keyGeneratorClass: String = if ("null".equalsIgnoreCase(hudiPartitionColumn)) {
        "org.apache.hudi.keygen.NonpartitionedKeyGenerator"
    } else {
        "org.apache.hudi.keygen.SimpleKeyGenerator"
    }
    //hudi  partition_extractor_class
    var partitionExtractorClass: String = if ("null".equalsIgnoreCase(hudiPartitionColumn)) {
        "org.apache.hudi.hive.NonPartitionedExtractor"
    } else {
        "org.apache.hudi.hive.MultiPartKeysValueExtractor"
    }
    //hudi  hive_sync_enable
    var hiveSyncEnable: String = params.getString("hive_sync_enable")
    //read data format
    var readDataFormat: String = params.getString("format")

    override def toString: String = {
        "Config { \n" +
                "    sparkConfigFilePath : " + sparkConfigFilePath + "\n" +
                "    hoodieConfigFilePath : " + hoodieConfigFilePath + "\n" +
                "    logPath : " + logPath + "\n" +
                "    saveMode : " + saveMode + "\n" +
                "    tableName : " + tableName + "\n" +
                "    combineColumn : " + combineColumn + "\n" +
                "    sourcePath : " + sourcePath + "\n" +
                "    destPath : " + destPath + "\n" +
                "    hudiPartitionColumn : " + hudiPartitionColumn + "\n" +
                "    hivePartitionColumn : " + hivePartitionColumn + "\n" +
                "    rowKey : " + rowKey + "\n" +
                "    database : " + database + "\n" +
                "    keyGeneratorClass : " + keyGeneratorClass + "\n" +
                "    partitionExtractorClass : " + partitionExtractorClass + "\n" +
                "    hiveSyncEnable : " + hiveSyncEnable + "\n" +
                "    readDataFormat : " + readDataFormat + "\n" +
                "}\n"
    }
}
