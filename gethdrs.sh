#!/bin/sh
LIB=/usr/share/java/libbzdev.jar
java -classpath GETHDIR/geth.jar:$LIB HttpHeaders "$@"

