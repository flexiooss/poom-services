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

if [ -z ${KEEP_DUMP_WHEN_FAILS} ]
then
    KEEP_DUMP_WHEN_FAILS=false
    echo "KEEP_DUMP_WHEN_FAILS not set, defaulting to $KEEP_DUMP_WHEN_FAILS"
else
    echo "KEEP_DUMP_WHEN_FAILS set to $KEEP_DUMP_WHEN_FAILS"
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

        HEADERS=$(mktemp)
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
            $URL/reports ${CURL_OPTS} \
            -v -D $HEADERS


        RESPONSE201=$(cat $HEADERS | grep -i 'HTTP/1.1 201')
        if [[ ${RESPONSE201} =~ "Created" ]] || [[ ${RESPONSE201} =~ "created" ]] || [[ ${RESPONSE201} =~ "CREATED" ]]; then
            echo "Sucessfully posted to service, will delete $(dirname $DESCRIPTOR)"
            rm -rf $(dirname $DESCRIPTOR)
        elif [[ ${KEEP_DUMP_WHEN_FAILS} = false ]]; then
            echo "Error posting dump to reporter, deleting $(dirname $DESCRIPTOR) anyway. In order to keep directory, set KEEP_DUMP_WHEN_FAILS env to 'true'"
            rm -rf $(dirname $DESCRIPTOR)
        else
            echo "Error posting report, keeping in place and renaming to failure to avoid retry"
            mv ${DESCRIPTOR} ${DESCRIPTOR}.failed
            mv $(dirname $DESCRIPTOR) $(dirname $DESCRIPTOR)___$(date +%FT%H-%M-%S)
        fi


        echo "done."
    done
    sleep $SCAN_DELAY
done