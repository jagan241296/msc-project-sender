#!/bin/bash
clear

# RUN LONG TESTS FOR ALL QUEUES
echo "EXECUTING LONG TESTS"

# RABBITMQ
echo "=========================RABBITMQ========================"
echo "start time "
date +%s
java -jar SenderApp.jar "/home/azureuser/videos" RABBITMQ 1048576 20 1000 20 & javapid=$!
sleep 1800
kill $javapid
date +%s
echo "END RABBITMQ TEST"


echo "=========================KAFKA========================"
echo "start time "
date +%s
java -jar SenderApp.jar "/home/azureuser/videos" KAFKA 1048576 20 1000 20 & javapid=$!
sleep 1800
kill $javapid
date +%s
echo "END KAFKA TEST"


echo "=========================SERVICE BUS========================"
echo "start time "
date +%s
java -jar SenderApp.jar "/home/azureuser/videos" SERVICE_BUS 250880 40 500 5 & javapid=$!
sleep 10
kill $javapid
date +%s
echo "END SERVICE BUS TEST"