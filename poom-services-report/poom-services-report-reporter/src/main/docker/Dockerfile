FROM alpine:3.8

RUN apk --update add bash curl

COPY report.sh /usr/local/bin/report.sh
RUN chmod a+x /usr/local/bin/report.sh

VOLUME /var/reports

ENV SCANNED_DIR="/var/reports"
ENV SCAN_DELAY=10
ENV REPORT_API_URL=http://service-report:9999/service-report

ENTRYPOINT ["/usr/local/bin/report.sh", "${SCANNED_DIR}"]