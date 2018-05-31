#!/usr/bin/env sh

echo "starting service..."
export SERVICE_HOST=$(hostname -i)

if [ -z ${LOGGING_TYPE} ]
then
    LOGGING_TYPE="text"
fi

echo "setting logging type to ${LOGGING_TYPE}"
exec java -cp "/var/service/lib/*:/var/service/config/logs/logback-${LOGGING_TYPE}.xml:/var/service/config/" $JVM_VM "$@"
echo "service stopped"