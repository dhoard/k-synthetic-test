#!/bin/bash

if [ "${1}" == "" ]
then
  echo "Usage: ${0} <producer properties> <consumer properties>"
  exit 1
fi

if [ "${2}" == "" ]
then
  echo "Usage: ${0} <producer properties> <consumer properties>"
  exit 1
fi

java -jar target/kafka-synthetic-test-0.0.1.jar "${1}" "${2}"
