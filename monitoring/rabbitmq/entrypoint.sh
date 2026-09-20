#!/bin/bash
# Starts rabbitmq-server, waits until it is ready, applies the lazy-queue
# policy once, then execs the server so signals (SIGTERM) reach PID1.
set -eu

rabbitmq-server -detached

echo "Waiting for RabbitMQ to become ready..."
for i in $(seq 1 60); do
  if rabbitmq-diagnostics -q check_running >/dev/null 2>&1; then
    echo "RabbitMQ is running."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "RabbitMQ did not start in time." >&2
    exit 1
  fi
  sleep 2
done

rabbitmqctl set_policy GlobalLazy ".*" '{"queue-mode":"lazy"}' --apply-to queues || true

# Restart in foreground so the broker itself is PID1 (correct SIGTERM handling).
rabbitmqctl shutdown || true
exec rabbitmq-server
