#!/bin/bash
#to install:
#cd to dockfish directory
#mvn install
#cd web/target (or better: copy dockfish-web-runner.jar to an working directory

export ENGINE_DIRECTORY=c:\\Prog\\chessengines
export ADDITIONAL_ENGINE_DIRECTORY=c:\\Prog\\chessengines\\lc0

export RABBITMQ_HOST=REPLACE
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=REPLACE
export RABBITMQ_PASSWORD=REPLACE

export MP_MESSAGING_INCOMING_SUBMITTASKCOMMAND_ENABLED=false
export MP_MESSAGING_INCOMING_SUBMITTASKCOMMAND_CONSUMER_ARGUMENTS=x-priority:10

export MP_MESSAGING_INCOMING_SUBMITTASKCOMMAND2_ENABLED=true
export MP_MESSAGING_INCOMING_SUBMITTASKCOMMAND2_CONSUMER_ARGUMENTS=x-priority:20
export MP_MESSAGING_INCOMING_SUBMITTASKCOMMAND2_QUEUE_NAME=dockfish.tasks.windows
export MP_MESSAGING_INCOMING_SUBMITTASKCOMMAND2_ROUTING_KEYS=windows

export uci_option_Threads=4
export uci_option_Hash=8092
export uci_option_Contempt=0
export uci_option_SyzygyPath=c:\\Prog\\sy3-4-5

java -Dquarkus.http.port=8072 -jar dockfish-web-runner.jar

#and then open locahost:8072

