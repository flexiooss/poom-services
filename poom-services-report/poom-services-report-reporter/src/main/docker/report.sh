#!/usr/bin/env bash
set -x

USAGE="usage: $0 <scanned folder> <service report api url> OR set SCANNED_DIR and REPORT_API_URL environment variables"

if [ "$#" -lt 2 ]; then
    if [ -z ${SCANNED_DIR} ]; then
        echo $USAGE
        exit 1
    fi
    if [ -z ${REPORT_API_URL} ]; then
        echo $USAGE
        exit 2
    fi
    URL=$REPORT_API_URL
else
    SCANNED_DIR=$1
    URL=$2
fi


if [ -z ${SCAN_DELAY} ]
then
    SCAN_DELAY=10
    echo "SCAN_DELAY not set, defaulting to $SCAN_DELAY"
fi

echo "starting reporter  [SCANNED_DIR=${SCANNED_DIR} ; REPORT_API_URL=${REPORT_API_URL} ; SCAN_DELAY=${SCAN_DELAY}]"

while true
do
    for DESCRIPTOR in $(find $SCANNED_DIR -name '*.desc')
    do
        echo "reporting error : ${DESCRIPTOR}"

        unset MAIN_CLASS SERVICE_NAME SERVICE_VERSION SERVICE_START CONTAINER_ID SERVICE_END SERVICE_EXIT_STATUS
        source $DESCRIPTOR

        if [ -z ${DUMP_NAME} ]; then
            DUMP_NAME="heap.hprof"
        fi

        DUMP=$(dirname $DESCRIPTOR)/${DUMP_NAME}
        CURL_OPTS=""
        if [ -f ${DUMP} ]; then
            CURL_OPTS=" --data-binary @${DUMP}"
        fi

        curl -v -XPOST \
            --header "x-name: ${SERVICE_NAME}" \
            --header "x-version: ${SERVICE_VERSION}" \
            --header "x-main-class: ${MAIN_CLASS}" \
            --header "x-container-id: ${CONTAINER_ID}" \
            --header "x-start: ${SERVICE_START}Z" \
            --header "x-end: ${SERVICE_END}Z" \
            --header "x-exit-status: ${SERVICE_EXIT_STATUS}" \
            --header "expect:" \
            --header "Content-Type: application/octet-stream" \
            $URL/reports ${CURL_OPTS}

        rm -rf $(dirname $DESCRIPTOR)

        echo "done."
    done
    sleep $SCAN_DELAY
done