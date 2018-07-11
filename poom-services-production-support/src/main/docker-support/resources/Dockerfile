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
ENV JVM_MEMORY_TUNING=""

ENV JVM_MAX_HEAP="64m"
ENV JVM_MIN_HEAP="$JVM_MAX_HEAP"
ENV JVM_METASPACE="64m"
ENV JVM_COMPRESSED_CLASS_SPACE="8m"
ENV JVM_THREAD_STACK_SIZE="256k"
ENV JVM_YOUNG_GEN_HEAP="8m"
ENV JVM_INITIAL_CODE_CACHE_SIZE="4m"
ENV JVM_RESERVED_CODE_CACHE_SIZE="8m"
ENV JVM_MAX_DIRECT_MEMORY_SIZE="32m"
ENV JVM_MAX_CACHED_BUFFER_SIZE="0"


ENTRYPOINT ["/var/service/service-endpoint.sh", "${service.class.name}"]
