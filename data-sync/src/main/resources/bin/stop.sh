#!/bin/bash

WORK_PATH=$(cd `dirname $0`;pwd)
LOG_PATH=${WORK_PATH}/../log

if [ -f ${LOG_PATH}/pid.log ]
then
    echo "cat ${LOG_PATH}/pid.log | kill"
    cat ${LOG_PATH}/pid.log | kill
fi