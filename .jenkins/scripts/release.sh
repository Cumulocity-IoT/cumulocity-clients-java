#!/bin/bash
set -e
release_args="-Dmaven.javadoc.skip=true -Dskip.microservice.package=false -Dskip.agent.package.container=false -Dnexus.url=http://nexus:8081  -Darguments=-Dskip.microservice.package=false -Dskip.agent.package.rpm=false -Dskip.agent.package.container=false -Dnexus.url=http://nexus:8081"
source ${BASH_SOURCE%/*}/common.sh
call-mvn clean -T 4
call-mvn  versions:use-releases release:prepare release:perform ${release_args} -Dincludes=com.nsn.cumulocity*:*,com.cumulocity*:*