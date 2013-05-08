#!/bin/sh
# The CLASSPATH separator is platform dependent
# Windows ;
# Unix :
# Semicolon between agents must be escaped using \
java -cp lib/jade.jar:bin jade.Boot -gui -local-host 127.0.0.1 -agents google:SA\;crawler:DA
