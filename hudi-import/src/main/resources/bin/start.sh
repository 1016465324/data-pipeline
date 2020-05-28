#!/bin/bash

WORK_PATH=$(cd `dirname $0`;pwd)
CONF_PATH=${WORK_PATH}/../conf
LOG_PATH=${WORK_PATH}/../logs

EXTRA_JARS=`ls -1 ${WORK_PATH}/../lib/*jar | grep -v '*avro*-1.' | tr '\n' ','`

EXECUTE_JAR=`ls -1 ${WORK_PATH}/../*.jar`
EXECUTE_CLASS=com.clinbrain.HdfsDataImporter

spark-submit --jars ${EXTRA_JARS} --master yarn --deploy-mode client \
    --keytab=/etc/security/keytabs/clin.keytab \
    --principal=clin@HUAXIHDP.COM \
    --name $1 \
    --class ${EXECUTE_CLASS} ${EXECUTE_JAR} \
    $2 ${CONF_PATH}/spark_config_template.properties \
    ${CONF_PATH}/hoodie_config_template.properties ${LOG_PATH}/ > ${LOG_PATH}/import_${1}.log 2>&1