#!/bin/sh
# The CLASSPATH separator is platform dependent
# Windows ;
# Unix :
# Semicolon between agents must be escaped using \
CP=lib/jade.jar:lib/gson-2.2.3.jar:bin
java -cp $CP jade.Boot -gui -local-host 127.0.0.1 -local-port 1099 &
sleep 1
java -cp $CP jade.Boot -container -local-host 127.0.0.1 -host localhost -port 1099 \
-agents faroo:search.FarooSA\;viewer:view.VA\;crawler:dipre.DA\(Isaac\ Asimow\ The\ Robots\ of\ Dawn,David\ Brin\ Startide\ Rising\) &
sleep 1
java -cp $CP jade.Boot -container -local-host 127.0.0.1 -host localhost -port 1099 \
-agents faroo2:search.FarooSA\;crawler2:dipre.DA\(Charles\ Dickens\ Great\ Expectations,William\ Shakespeare\ The\ Comedy\ of\ Errors\)
