#!/bin/sh

if [ "$3" == "" ]; then
    echo "Usage: get-war.sh groupId artifactId version"
    echo "Example: get-war.sh org.slf4j slf4j-api 1.3.0"
    exit
fi

source $(dirname $0)/setrepo.sh

GROUPID=`echo "$1" | tr . /`
URL="$M2_REMOTE_REPO/$GROUPID/$2/$3/$2-$3.war"
ARTIFACT_NAME="$2-$3.war"

cd $(dirname $0)/../webapps

if command -v aria2c >/dev/null 2; then
    aria2c -x 16 $URL
else
    wget $URL -O $ARTIFACT_NAME.part
    mv $ARTIFACT_NAME.part $ARTIFACT_NAME
fi
