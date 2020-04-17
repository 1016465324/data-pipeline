#!/bin/bash

WORK_PATH=$(cd `dirname $0`;pwd)
echo "work path: ${WORK_PATH}"

CONF_PATH=${WORK_PATH}/../conf

EXTRA_JARS=`ls -1 ${WORK_PATH}/../lib/*jar | grep -v '*avro*-1.' | tr '\n' ','`
echo ${EXTRA_JARS}

EXECUTE_JAR=`ls -1 ${WORK_PATH}/../*.jar`
EXECUTE_CLASS=com.clinbrain.CachedbMessageBuild

spark-submit --jars ${EXTRA_JARS} --master local[8] --deploy-mode client \
    --driver-memory 2g \
    --num-executors 2 \
    --executor-memory 6g \
    --conf spark.driver.maxResultSize=2g \
    --files "${CONF_PATH}/kafka_client_jaas.conf,${CONF_PATH}/kafka_client_jaas1.conf" \
    --driver-java-options "-Djava.security.auth.login.config=${CONF_PATH}/kafka_client_jaas.conf" \
    --conf "spark.executor.extraJavaOptions=-Djava.security.auth.login.config=./kafka_client_jaas.conf" \
    --keytab=/etc/security/keytabs/clin.keytab \
    --principal=clin@HUAXIHDP.COM \
    --class ${EXECUTE_CLASS} ${EXECUTE_JAR} ${CONF_PATH}/sumary.xlsx ${CONF_PATH}/DHC-APP.txt ${CONF_PATH}/cdclog.properties