#!/usr/bin/env bash
SCRIPT_DIR=$(dirname $(readlink -f $0))
HERE=$(pwd)

HOST="0.0.0.0"
PORT=9001
JDW_PORT=9091
JMX_REMOTE_PORT=10091
OBSERVABILITY="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:$JDW_PORT -Dcom.sun.management.jmxremote.port=$JMX_REMOTE_PORT -Dcom.sun.management.jmxremote.rmi.port=$JMX_REMOTE_PORT -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"


cd $SCRIPT_DIR
java $OBSERVABILITY -cp "lib/*" org.codingmatters.poom.containers.load.tests.sut.service.UndertowSutService $HOST $PORT