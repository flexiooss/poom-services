#!/usr/bin/env sh

echo "starting service..."
export SERVICE_HOST=$(hostname -i)

if [ -z ${LOGGING_TYPE} ]
then
    LOGGING_TYPE="text"
fi
echo "setting logging type to ${LOGGING_TYPE}"

LOGGER_CONFIG="-Dlogback.configurationFile=/var/service/config/logs/logback-${LOGGING_TYPE}.xml"
CLASSPATH="/var/service/lib/*:/var/service/config/"

exec java -cp $CLASSPATH $LOGGER_CONFIG $JVM_VM "$@"
echo "service stopped"