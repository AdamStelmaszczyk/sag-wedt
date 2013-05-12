#!/bin/sh
# The CLASSPATH separator is platform dependent
# Windows ;
# Unix :
# Semicolon between agents must be escaped using \
CP=lib/jade.jar:bin
java -cp $CP jade.Boot -gui -local-host 127.0.0.1 -local-port 1099 &
sleep 1
java -cp $CP jade.Boot -container -local-host 127.0.0.1 -host localhost -port 1099 \
-agents bing:BingSA\;crawler:DA\(Isaac\ Asimow\#The\ Robots\ of\ Dawn,David\ Brin\#Startide\ Rising\) &
sleep 1
java -cp $CP jade.Boot -container -local-host 127.0.0.1 -host localhost -port 1099 \
-agents bing2:BingSA\;crawler2:DA\(Charles\ Dickens\#Great\ Expectations,William\ Shakespeare\#The\ Comedy\ of\ Errors\)
