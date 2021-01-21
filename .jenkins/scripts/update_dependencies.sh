#!/usr/bin/env bash
set -e
source ${BASH_SOURCE%/*}/common.sh

function update-dependencies {
    echo "Update properties in SDK POMs to new version"
    if [[ "$1" =~ .*SNAPSHOT ]]; then
        echo "Not updating cumulocity-sdk to ${1} version "
    else
        echo "Updating to cumulocity-sdk ${1} version "
        cd cumulocity-sdk
        update-property cumulocity.version ${1}
        if [ -n "$(git status -s)" ]; then
            git commit -am "Update dependencies to new version" --allow-empty
        else
            echo "no changes found no commit"
        fi
        cd -
    fi

}

update-dependencies $1
