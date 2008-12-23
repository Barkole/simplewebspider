#!/bin/bash
mvn install:install-file \
    -Dfile=lib/htmlcleaner2_1.jar \
    -DgroupId=org.htmlcleaner \
    -DartifactId=htmlcleaner \
    -Dversion=2.1 \
    -Dpackaging=jar \
    -DgeneratePom=true
