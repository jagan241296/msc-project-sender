#!/bin/bash
clear

# RUN PRESSURE TESTS FOR ALL QUEUES
echo "EXECUTING PRESSURE TESTS"

# RABBITMQ
echo "=========================RABBITMQ========================"
echo "start time "
date +%s
java -jar SenderApp.jar "/home/azureuser/videos" RABBITMQ 10240 0 2000 20 & javapid=$!
sleep 1800
kill $javapid
date +%s
echo "END RABBITMQ TEST"


echo "=========================KAFKA========================"
echo "start time "
date +%s
java -jar SenderApp.jar "/home/azureuser/videos" KAFKA 10240 0 2000 20 & javapid=$!
sleep 1800
kill $javapid
date +%s
echo "END KAFKA TEST"