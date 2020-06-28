#!/bin/bash

WORK_PATH=$(cd `dirname $0`;pwd)
CONF_PATH=${WORK_PATH}/../conf
LOG_PATH=${WORK_PATH}/../log

if [ ! -d ${LOG_PATH} ]
then
    mkdir -p ${LOG_PATH}
fi

EXECUTE_JAR=`ls -1 ${WORK_PATH}/../lib/*.jar | tr '\n' ':'`

echo "nohup java -server -cp ${EXECUTE_JAR} com.clinbrain.DataSync ${CONF_PATH} > /dev/null 2>&1 &"
nohup java -server -cp ${EXECUTE_JAR} com.clinbrain.DataSync ${CONF_PATH} > /dev/null 2>&1 &
PID=$!

echo "${PID}" > ${LOG_PATH}/pid.log
