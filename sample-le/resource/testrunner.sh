#!/usr/bin/env bash

leader_election=/home/jeff/Huawei/SAMC-IPC
classpath=.:$leader_election/bin
lib=$leader_election/lib
for j in `ls $lib/*.jar`; do
  classpath=$classpath:$j
done
export CLASSPATH=$CLASSPATH:$classpath
export PATH=$PATH:bin/

#java -Dsun.rmi.dgc.cleanInterval=10000 -Dsun.rmi.dgc.server.gcInterval=10000 -Dlog4j.configuration=mc_log.properties -Delectiontest.dir=WORKING_DIR edu.uchicago.cs.ucare.samc.server.TestRunner -p ./mc.conf
#java -Dlog4j.configuration=mc_log.properties -Delectiontest.dir=WORKING_DIR edu.uchicago.cs.ucare.samc.server.TestRunner -p ./mc.conf
java -Dlog4j.configuration=mc_log.properties -Delectiontest.dir=WORKING_DIR edu.uchicago.cs.ucare.samc.server.TestRunner ./target-sys.conf

