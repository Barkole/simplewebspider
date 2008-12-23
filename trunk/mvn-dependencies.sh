#!/bin/bash
#mvn -Dmdep.useRepositoryLayout=true \
#    -Dmdep.copyPom=true \
#    clean:clean \
#    dependency:copy-dependencies

mvn clean:clean \
    dependency:copy-dependencies
