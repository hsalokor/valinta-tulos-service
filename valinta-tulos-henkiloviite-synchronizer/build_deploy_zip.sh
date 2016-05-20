#!/bin/bash
set -e

SERVICE=valinta-tulos-henkiloviite-synchronizer
BRANCH=master
ZIP=${PWD}/target/${SERVICE}_${BRANCH}_$(date +%Y%m%d%H%M%S).zip

cd ./target
zip "${ZIP}" *jar-with-dependencies.jar
cd -
cd ./src/main/resources
zip -r "${ZIP}" oph-configuration
cd -
