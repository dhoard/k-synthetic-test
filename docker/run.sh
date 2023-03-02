#!/bin/bash

if [ $# -eq 0 ];
then
  echo "Usage: ./run.sh <port> <test properties> <port to expose>"
  exit 1
fi

docker run -v ./"${1}":/k-synthetic-test.properties -p ${2}:8080 dhoard/k-synthetic-test:0.0.7
