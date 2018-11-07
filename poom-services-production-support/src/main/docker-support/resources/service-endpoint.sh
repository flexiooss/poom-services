#!/usr/bin/env sh

echo "starting service..."

if [ -z ${SERVICE_HOST} ]
then
    export SERVICE_HOST="0.0.0.0"
    echo "SERVICE_HOST not set, defaulting to $SERVICE_HOST"
fi

if [ -z ${SERVICE_PORT} ]
then
    export SERVICE_PORT="80"
    echo "SERVICE_PORT not set, defaulting to $SERVICE_PORT"
fi

if [ -z ${SERVICE_URL} ]
then
    export SERVICE_URL="http://$(hostname):$SERVICE_PORT"
    echo "SERVICE_URL not set, defaulting to $SERVICE_URL"
fi

if [ -z ${LOGGING_TYPE} ]
then
    LOGGING_TYPE="text"
fi

echo "setting logging type to ${LOGGING_TYPE} with level ${LOG_LEVEL}"

LOGGER_CONFIG="-Dlogback.configurationFile=/var/service/config/logs/logback-${LOGGING_TYPE}.xml"
if [ -z ${LOG_LEVEL} ]
then
    LOGGER_CONFIG="${LOGGER_CONFIG} -Droot.logger.level=${LOG_LEVEL}"
fi

CLASSPATH="/var/service/lib/*:/var/service/config/"

##
#   Initializing service report directory 
#
REPORT_DIR="/var/service/report/$(hostname)"
mkdir -p $REPORT_DIR

echo "MAIN_CLASS=${MAIN_CLASS}" >> $REPORT_DIR/service.desc.temp
echo "SERVICE_NAME=${SERVICE_NAME}" >> $REPORT_DIR/service.desc.temp
echo "SERVICE_VERSION=${SERVICE_VERSION}" >> $REPORT_DIR/service.desc.temp
echo "SERVICE_START=$(date --utc +%Y-%m-%dT%T)" >> $REPORT_DIR/service.desc.temp
echo "CONTAINER_ID=$(hostname)" >> $REPORT_DIR/service.desc.temp

##
#   Setting up JVM options 
#
JVM_VM="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap "
JVM_VM="${JVM_VM} -Xms${JVM_MIN_HEAP}"
JVM_VM="${JVM_VM} -Xmx${JVM_MAX_HEAP}"
JVM_VM="${JVM_VM} -XX:MaxMetaspaceSize=${JVM_METASPACE} "
JVM_VM="${JVM_VM} -XX:CompressedClassSpaceSize=${JVM_COMPRESSED_CLASS_SPACE}"
JVM_VM="${JVM_VM} -Xss${JVM_THREAD_STACK_SIZE}"
JVM_VM="${JVM_VM} -Xmn${JVM_YOUNG_GEN_HEAP}"
JVM_VM="${JVM_VM} -XX:InitialCodeCacheSize=${JVM_INITIAL_CODE_CACHE_SIZE}"
JVM_VM="${JVM_VM} -XX:ReservedCodeCacheSize=${JVM_RESERVED_CODE_CACHE_SIZE}"
JVM_VM="${JVM_VM} -XX:MaxDirectMemorySize=${JVM_MAX_DIRECT_MEMORY_SIZE}"
JVM_VM="${JVM_VM} -Djdk.nio.maxCachedBufferSize=${JVM_MAX_CACHED_BUFFER_SIZE}"
JVM_VM="${JVM_VM} $JVM_MEMORY_TUNING $JVM_OPTS"
JVM_VM="${JVM_VM} -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${REPORT_DIR}/heap.hprof"

##
#   Trapping SIGINT and SIGTERM to pass them to the service
#
SERVICE_PID=0
proxy_sigterm() {
    echo "terminating service with pid ${SERVICE_PID}"
    kill -SIGTERM $SERVICE_PID
}
proxy_sigint() {
    echo "interrupting service with pid ${SERVICE_PID}"
    kill -SIGINT $SERVICE_PID
}
trap 'proxy_sigterm' SIGTERM
trap 'proxy_sigint' SIGINT

##
#   Running the service in the background
#
echo "starting JVM with the following options : ${JVM_VM}"
java -cp $CLASSPATH $LOGGER_CONFIG $JVM_VM $MAIN_CLASS "$@" &
SERVICE_PID=$!

wait $SERVICE_PID
EXIT_STATUS=$?
echo "service terminated with status ${EXIT_STATUS}"

##
#   finalizing service report
#
echo "SERVICE_END=$(date --utc +%Y-%m-%dT%T)" >> $REPORT_DIR/service.desc.temp
echo "SERVICE_EXIT_STATUS=${EXIT_STATUS}" >> $REPORT_DIR/service.desc.temp
mv $REPORT_DIR/service.desc.temp $REPORT_DIR/service.desc

##
#   Exiting with service's exit code
#
exit $EXIT_STATUS