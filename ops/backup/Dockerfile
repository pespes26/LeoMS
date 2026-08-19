FROM alpine:3.22
RUN apk add --no-cache bash coreutils gzip jq mariadb-client restic tzdata
COPY backup-worker /usr/local/bin/backup-worker
RUN chmod 0555 /usr/local/bin/backup-worker \
    && mkdir -p /var/lib/leoms-backup \
    && chown -R 10002:10002 /var/lib/leoms-backup
USER 10002:10002
ENTRYPOINT ["/usr/local/bin/backup-worker"]
CMD ["run"]
