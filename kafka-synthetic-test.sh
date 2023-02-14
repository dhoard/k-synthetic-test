#!/bin/bash

if [ "${1}" == "" ]
then
  echo "Usage: ${0} <properties>"
  exit 1
fi

java -jar target/kafka-synthetic-test-0.0.3.jar "${1}"
