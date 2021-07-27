#!/bin/bash
set -e
# source ${BASH_SOURCE%/*}/common.sh

./mvnw install -Pci,javadoc -s $MVN_SETTINGS -U $1
cd microservice
../mvnw install javadoc:aggregate-jar -Pci -s $MVN_SETTINGS -U $1
cd -
cd lpwan-backend
../mvnw install javadoc:aggregate-jar -Pci -s $MVN_SETTINGS -U $1
cd -