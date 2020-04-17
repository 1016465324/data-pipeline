data-transform
===========
## 介绍
    1、备份Caché数据库的日志变更数据。
    2、将Caché数据库的日志数据转换为完整的表的一条完整数据，并发送到下层kafka。
##项目部署
### 1 应用打包

```shell script
mvn clean install -DskipTests
```
### 2 配置修改
```properties
#/global/checkpoint/backup  /global/checkpoint/build
cdclog.checkPointDir=/global/checkpoint/build
bootstrap.servers=127.0.0.1:9092
#cdclog_backup cdclog_build
cdclog.groupId=cdclog_build
topics=cachetest
cdclog.dateDirFormat=yyyy-MM-dd
cdclog.resultDir=/global/cdclog/
cdclog.otherResultDir=/global/cdclog_other/
cdclog.queryMessageErrorDir=/global/cdclog_error/
#latest / earliest
cdclog.reset=latest
cdclog.consumeTime=60

#cache database
url=jdbc:Cache://127.0.0.1:1972/SAMPLES
host=jdbc:Cache://127.0.0.1:1972/
namespaces=SAMPLES,USER
username=_SYSTEM
password=sys

ogg.topics=oggtest

#吞吐量 maxRatePerPartition * kafka分区数 * (分钟数 * 60)
spark.streaming.kafka.maxRatePerPartition=1000000
spark.streaming.backpressure.enabled=true
```
### 3 应用启动
日志变更数据备份
    sh bin/start_backup.sh
    
日志变更数据转换
    sh bin/start_build.sh    