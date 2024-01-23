#!/bin/bash -xe

RELEASE_TYPE=$1
shift 1

RESOURCES_SERVER=hudson@staging-resources.cumulocity.com
STAGING_RESOURCES_DIR="/var/www/staging-resources/sag/java-sdk/${VERSION}"
RESOURCES="/resources/maven/repository"



echo "Unpacking the published versions from staging to resources"

# below, 2>&1 keeps the order of output in place. Besides this, it doesn't change much
# 'awk' will print "[ssh]" before every line in ssh
ssh ${RESOURCES_SERVER} "bash -xe 2>&1" << DISCONNECT_FROM_RESOURCES_SSH  | awk '{print "[ssh] " $0 }'
    # maven-repository
    echo "Publish staging maven-repository-${VERSION}.tar.gz to resources "
    tar -xvzf ${STAGING_RESOURCES_DIR}/maven-repository-${VERSION}.tar.gz -C /resources/maven/repository/ com
    tar -xvzf ${STAGING_RESOURCES_DIR}/maven-repository-${VERSION}.tar.gz -C /resources/maven/repository/ c8y

    # java-client-javadoc
    echo "Publishing staging java-client-${VERSION}-javadoc.jar to resources "
    mkdir -p /resources/documentation/javasdk/${VERSION}
    unzip -o ${STAGING_RESOURCES_DIR}/java-client-${VERSION}-javadoc.jar -d /resources/documentation/javasdk/${VERSION}

    # microservice-dependencies-javadoc
    echo "Publishing staging microservice-dependencies-${VERSION}-javadoc.jar to resources "
    mkdir -p /resources/documentation/microservicesdk/${VERSION}
    unzip -o ${STAGING_RESOURCES_DIR}/microservice-dependencies-${VERSION}-javadoc.jar -d /resources/documentation/microservicesdk/${VERSION}

    # lpwan-backend-javadoc
    echo "Publishing staging lpwan-backend-${VERSION}-javadoc.jar to resources "
    mkdir -p /resources/documentation/lpwan-backend/${VERSION}
    unzip -o ${STAGING_RESOURCES_DIR}/lpwan-backend-${VERSION}-javadoc.jar -d /resources/documentation/lpwan-backend/${VERSION}

    if [ "release" == "${RELEASE_TYPE}" ]; then
      echo "Update current symbolic link of javasdk javadocs"
      rm -f /resources/documentation/javasdk/current
      ln -s /resources/documentation/javasdk/${VERSION} /resources/documentation/javasdk/current

      echo "Update current symbolic link of microservicesdk javadocs"
      rm -f /resources/documentation/microservicesdk/current
      ln -s /resources/documentation/microservicesdk/${VERSION} /resources/documentation/microservicesdk/current

      echo "Update current symbolic link of lpwan-backend javadocs"
      rm -f /resources/documentation/lpwan-backend/current
      ln -s /resources/documentation/lpwan-backend/${VERSION} /resources/documentation/lpwan-backend/current
    fi
DISCONNECT_FROM_RESOURCES_SSH