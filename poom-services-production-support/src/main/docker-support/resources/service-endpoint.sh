#!/usr/bin/env sh

echo "starting service..."

if [ -z ${SERVICE_HOST} ]
then
    export SERVICE_HOST=$(hostname)
    echo "SERVICE_HOST not set, defaulting to $SERVICE_HOST"
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

JVM_VM="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xms$JVM_MIN_HEAP -Xmx$JVM_MAX_HEAP $JVM_OPTS"
exec java -cp $CLASSPATH $LOGGER_CONFIG $JVM_VM "$@"
echo "service stopped"