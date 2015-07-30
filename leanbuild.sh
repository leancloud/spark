#!/bin/bash

export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=1024M -XX:ReservedCodeCacheSize=1024m"
mvn -e -Pspark-ganglia-lgpl -Pyarn -Psparkr -Phive -Dhadoop.version=2.3.0-cdh5.0.5 -Dprotobuf.version=2.5.0 -DskipTests clean package
