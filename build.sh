#!/bin/sh
mkdir -p bin
javac -cp lib/jade.jar:lib/gson-2.2.3.jar -d bin src/*/*.java
