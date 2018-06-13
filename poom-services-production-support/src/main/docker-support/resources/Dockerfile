FROM openjdk:8-jdk-alpine

RUN mkdir -p /var/service/lib
RUN mkdir -p /var/service/config/logs
COPY lib/* /var/service/lib/
COPY logback-text.xml /var/service/config/logs/
COPY logback-json.xml /var/service/config/logs/
COPY service-endpoint.sh /var/service/
RUN chmod a+x /var/service/service-endpoint.sh

EXPOSE 9999
ENV SERVICE_PORT=9999

ENV JVM_OPTS=""
ENV JVM_MAX_HEAP="256m"
ENV JVM_MIN_HEAP="$JVM_MAX_HEAP"
ENV JVM_VM="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xms$JVM_MIN_HEAP -Xmx$JVM_MAX_HEAP $JVM_OPTS"

ENTRYPOINT ["/var/service/service-endpoint.sh", "${service.class.name}"]
