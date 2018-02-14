#!/bin/bash
CLASSPATH=classes:$(ls lib/* | sed 's/ /:/')
JAVAS=$(find src -name '*.java')
mkdir -p classes
javac -cp $CLASSPATH -sourcepath src -d classes $JAVAS
