#!/bin/bash -xe

RELEASE_TYPE=$1
shift 1

export MAVEN_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=128m ${MAVEN_OPTS}"
source ${BASH_SOURCE%/*}/common.sh
call-mvn -s $MVN_SETTINGS deploy -T 2C -Dmaven.install.skip=true $release_args -DskipTests "$@"

if [[ "$RELEASE_TYPE" == "snapshot" ]]; then
    echo "Skipping of snapshot publishing to staging resources"
    exit 0;
fi

RESOURCES_SERVER=hudson@staging-resources.cumulocity.com
RESOURCES_DIR="/var/www/staging-resources/sag/java-sdk/${VERSION}"



echo "Publishing version.txt marker file to staging resources"
ssh ${RESOURCES_SERVER} "mkdir -p ${RESOURCES_DIR} && echo ${VERSION} > ${RESOURCES_DIR}/version.txt"

echo "Publishing all archives to staging resources"
scp cumulocity-sdk/maven-repository/target/maven-repository-${VERSION}.tar.gz \
    java-client/target/java-client-${VERSION}-javadoc.jar \
    microservice/target/microservice-dependencies-${VERSION}-javadoc.jar \
    lpwan-backend/target/lpwan-backend-${VERSION}-javadoc.jar \
    ${RESOURCES_SERVER}:${RESOURCES_DIR}/

echo "Published cumulocity-sdk/maven-repository/target/maven-repository-${VERSION}.tar.gz to staging resources"
echo "Published java-client/target/java-client-${VERSION}-javadoc.jar to staging resources"
echo "Published microservice/target/microservice-dependencies-${VERSION}-javadoc.jar to staging resources"
echo "Published lpwan-backend/target/lpwan-backend-${VERSION}-javadoc.jar to staging resources"
