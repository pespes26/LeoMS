FROM mysql:8.4.0
COPY leoms-entrypoint.sh leoms-grant-watcher.sh /usr/local/bin/
RUN chmod 0555 /usr/local/bin/leoms-entrypoint.sh /usr/local/bin/leoms-grant-watcher.sh
ENTRYPOINT ["/usr/local/bin/leoms-entrypoint.sh"]
CMD ["mysqld"]
