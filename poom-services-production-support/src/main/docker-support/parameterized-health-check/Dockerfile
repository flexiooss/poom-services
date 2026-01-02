FROM ${poom.service.base.image.namespace}/${poom.service.base.image}:${poom.service.base.image.version}

RUN mkdir -p /var/service/lib
RUN mkdir -p /var/service/config/logs
COPY lib/* /var/service/lib/
COPY logback-text.xml /var/service/config/logs/
COPY logback-json.xml /var/service/config/logs/
COPY service-endpoint.sh /var/service/
RUN chmod a+x /var/service/service-endpoint.sh

EXPOSE 80
ENV SERVICE_PORT=80

HEALTHCHECK --interval=${health.check.interval} --timeout=${health.check.timeout} --start-period=${health.check.start.period} --retries=${health.check.retries} \
  CMD ${health.check.command}

ENV MAX_RAM_PERCENTAGE=70.0
# ENV INITIAL_RAM_PERCENTAGE=70.0
# ENV MIN_RAM_PERCENTAGE=70.0

ENV JVM_OPTS=""
ENV JVM_MEMORY_TUNING=""

#ENV JVM_MAX_HEAP="64m"
#ENV JVM_MIN_HEAP="$JVM_MAX_HEAP"
#ENV JVM_METASPACE="32m"
#ENV JVM_COMPRESSED_CLASS_SPACE="8m"
#ENV JVM_THREAD_STACK_SIZE="256k"
#ENV JVM_YOUNG_GEN_HEAP="8m"
#ENV JVM_INITIAL_CODE_CACHE_SIZE="4m"
#ENV JVM_RESERVED_CODE_CACHE_SIZE="8m"
#ENV JVM_MAX_DIRECT_MEMORY_SIZE="32m"
#ENV JVM_MAX_CACHED_BUFFER_SIZE="0"

ENV MAIN_CLASS="${service.class.name}"
ENV SERVICE_NAME="${project.groupId}:${project.artifactId}"
ENV SERVICE_VERSION="${project.version}"

VOLUME /var/service/report

ENTRYPOINT ["/var/service/service-endpoint.sh"]
