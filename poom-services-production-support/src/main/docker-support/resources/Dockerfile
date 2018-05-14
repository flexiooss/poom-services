FROM openjdk:8-jdk-alpine

RUN mkdir -p /var/service/lib
RUN mkdir -p /var/service/config
COPY lib/* /var/service/lib/
COPY logback.xml /var/service/config/
COPY service-endpoint.sh /var/service/
RUN chmod a+x /var/service/service-endpoint.sh

EXPOSE 9999
ENV SERVICE_PORT=9999

ENV JVM_MAX_HEAP="256m"
ENV JVM_MIN_HEAP="$JVM_MAX_HEAP"
ENV JVM_VM="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xms$JVM_MIN_HEAP -Xmx$JVM_MAX_HEAP"

ENTRYPOINT ["/var/service/service-endpoint.sh", "${service.class.name}"]
