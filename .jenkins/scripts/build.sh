#!/bin/bash
set -e
source ${BASH_SOURCE%/*}/common.sh

./mvnw install -Pci,javadoc -s $MVN_SETTINGS "$@"
cd microservice
../mvnw install javadoc:aggregate-jar -Pci -s $MVN_SETTINGS "$@"
cd -
cd lpwan-backend
../mvnw install javadoc:aggregate-jar -Pci -s $MVN_SETTINGS "$@"
cd - 

