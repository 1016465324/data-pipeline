#!/bin/bash

WORK_PATH=$(cd `dirname $0`;pwd)
EXECUTE_JAR=`ls -1 ${WORK_PATH}/../*dependencies.jar`

java -cp ${EXECUTE_JAR} com.clinbrain.MetadataExport ${WORK_PATH}/../conf/config.properties