#!/usr/bin/env bash
mvn clean package; java -jar target/mondeytransfer-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json
