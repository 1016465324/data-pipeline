data-capture
===========
## 介绍
    抓取数据库的变更日志并发送到kafka集群，目前支持Caché和Mongo数据库。
##项目部署
### 1 应用打包

```shell script
mvn clean install -DskipTests
```
### 2 配置修改
项目分为Caché 和 Mongo变更日志数据采集，配置方法相同。
```properties
#database cache/mongo
database_type=cache

#cache database cdc log
cdc_url=jdbc:Cache://127.0.0.1:1972/CDCLOG
cdc_username=_SYSTEM
cdc_password=sys
cdc_table=cdc.CDCLog

#cache database
cache_url=jdbc:Cache://127.0.0.1:1972/
cache_namespace=SAMPLES
namespace_username=_SYSTEM
namespace_password=sys


#mongo
mongo_uri=
mongo_host=localhost
mongo_port=27017
mongo_dbs=test.test

#source
batch_size=10000
offset_path=./offset/

#kafka
topics=cachetest
bootstrap.servers=127.0.0.1:9092
```
### 3 应用启动
bin/start_capture.sh
    