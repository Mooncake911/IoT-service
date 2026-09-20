# RabbitMQ with plugins baked in (replaces the `sh -c '... sleep 15 ...'`
# command hack in docker-compose.yml: proper PID1, no fixed sleeps).
# Keep default in sync with RABBIT_VERSION in .env
ARG RABBITMQ_VERSION=4.0-management
FROM rabbitmq:${RABBITMQ_VERSION} AS rabbitmq

COPY rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
COPY entrypoint.sh /usr/local/bin/iot-entrypoint.sh

RUN rabbitmq-plugins enable --offline rabbitmq_web_stomp rabbitmq_prometheus \
    && chmod +x /usr/local/bin/iot-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/iot-entrypoint.sh"]
