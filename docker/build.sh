#!/bin/bash

cp ../target/k-synthetic-test*.jar .
docker build -t dhoard/k-synthetic-test:0.0.7 .
