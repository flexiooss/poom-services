#!/usr/bin/env bash

if [ "$#" -lt 2 ]; then
    echo "usage: $0 <report folder> <service report api url>"
    exit 1
fi

SCANNED_DIR=$1
URL=$2

if [ -z ${SCAN_DELAY} ]
then
    SCAN_DELAY=10
    echo "SCAN_DELAY not set, defaulting to $SCAN_DELAY"
fi

while true
do
    for DESCRIPTOR in $(find $SCANNED_DIR -name '*.desc')
    do
        echo "reporting error : ${DESCRIPTOR}"

        unset MAIN_CLASS SERVICE_NAME SERVICE_VERSION SERVICE_START CONTAINER_ID SERVICE_END SERVICE_EXIT_STATUS
        source $DESCRIPTOR

        curl -v -XPOST \
            --header "x-name: ${SERVICE_NAME}" \
            --header "x-version: ${SERVICE_VERSION}" \
            --header "x-main-class: ${MAIN_CLASS}" \
            --header "x-container-id: ${CONTAINER_ID}" \
            --header "x-start: ${SERVICE_START}Z" \
            --header "x-end: ${SERVICE_END}Z" \
            --header "x-exit-status: ${SERVICE_EXIT_STATUS}" \
            $URL/reports

        rm -rf $(dirname $DESCRIPTOR)

        echo "done."
    done
    sleep $SCAN_DELAY
done