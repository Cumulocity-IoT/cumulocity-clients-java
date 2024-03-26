#!/bin/bash -xe

set -o pipefail # fail on the first command when using bash's pipe |

RELEASE_TYPE=$1
shift 1

RESOURCES_SSH_ARGS=" -o StrictHostKeyChecking=no hudson@staging-resources.cumulocity.com"
STAGING_RESOURCES_DIR="/var/www/staging-resources/sag/java-sdk/${VERSION}"
RESOURCES="/resources/maven/repository"



echo "Unpacking the published versions from staging to resources"

# below, 2>&1 keeps the order of output in place. Besides this, it doesn't change much
# 'awk' will print "[ssh]" before every line in ssh
ssh ${RESOURCES_SSH_ARGS} 'bash -xe 2>&1' << DISCONNECT_FROM_RESOURCES_SSH | awk '{print "[ssh] " $0 }'
    # maven-repository
    echo "Publish staging maven-repository-${VERSION}.tar.gz to resources "
    # By defualt `tar` extracts files with timestamps (--touch) and proper access rights (--no-overwrite-dir). 
    # But without sudo we can't always do it, for example there are old dependencies 
    # that did not change, perhaps set by different user.  
    # Overall, it doesn't matter to us, so these options are skipping this preservation.
    tar  --touch --no-overwrite-dir -xvf ${STAGING_RESOURCES_DIR}/maven-repository-${VERSION}.tar.gz --directory /resources/maven/repository/ com
    tar  --touch --no-overwrite-dir -xvf ${STAGING_RESOURCES_DIR}/maven-repository-${VERSION}.tar.gz --directory /resources/maven/repository/ c8y

    # java-client-javadoc
    echo "Publishing staging java-client-${VERSION}-javadoc.jar to resources "
    mkdir --parents /resources/documentation/javasdk/${VERSION}
    # unzip `-o` overwrites files, `-d` is destination
    unzip -o ${STAGING_RESOURCES_DIR}/java-client-${VERSION}-javadoc.jar -d /resources/documentation/javasdk/${VERSION}

    # microservice-dependencies-javadoc
    echo "Publishing staging microservice-dependencies-${VERSION}-javadoc.jar to resources "
    mkdir --parents /resources/documentation/microservicesdk/${VERSION}
    unzip -o ${STAGING_RESOURCES_DIR}/microservice-dependencies-${VERSION}-javadoc.jar -d /resources/documentation/microservicesdk/${VERSION}

    # lpwan-backend-javadoc
    echo "Publishing staging lpwan-backend-${VERSION}-javadoc.jar to resources "
    mkdir --parents /resources/documentation/lpwan-backend/${VERSION}
    unzip -o ${STAGING_RESOURCES_DIR}/lpwan-backend-${VERSION}-javadoc.jar -d /resources/documentation/lpwan-backend/${VERSION}

    if [ "release" == "${RELEASE_TYPE}" ]; then
      echo "Update current symbolic link of javasdk javadocs"
      rm --force /resources/documentation/javasdk/current
      ln --symbolic /resources/documentation/javasdk/${VERSION} /resources/documentation/javasdk/current

      echo "Update current symbolic link of microservicesdk javadocs"
      rm --force /resources/documentation/microservicesdk/current
      ln --symbolic /resources/documentation/microservicesdk/${VERSION} /resources/documentation/microservicesdk/current

      echo "Update current symbolic link of lpwan-backend javadocs"
      rm --force /resources/documentation/lpwan-backend/current
      ln --symbolic /resources/documentation/lpwan-backend/${VERSION} /resources/documentation/lpwan-backend/current
    fi
DISCONNECT_FROM_RESOURCES_SSH
