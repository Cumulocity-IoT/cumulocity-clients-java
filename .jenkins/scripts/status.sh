#!/usr/bin/env bash


function update-status {
    commitId=${GIT_REVISION}
    status=${1}
    name=${2}
    key=$( echo $3 | rev | cut -c -40 | rev )
    echo "Sending build status update for  commitId: ${commitId}, status: ${status}, name: ${name} , key: ${key}"
    #echo $(curl -s --user ${BITBUCKET_USER}:${BITBUCKET_PASSWORD} -H "Content-Type: application/json" -X POST https://api.bitbucket.org/2.0/repositories/m2m/cumulocity-clients-java-test-git/commit/${commitId}/statuses/build --data "{\"state\":\"${status}\", \"name\": \"${name}\", \"key\": \"${key}\", \"url\": \"http://localhost:8081/job/Microservice/job/Correctness/job/Development_tests/job/git-jobs/job/cumulocity-clients-test-git/job/${BRANCH_NAME}/${BUILD_ID}/\", \"description\": \"\" }")
}

update-status $1 $2 $3 
