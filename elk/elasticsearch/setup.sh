#!/bin/sh
set -eu

sleep 15

curl -X PUT "http://elasticsearch:9200/_ilm/policy/logs-retention-14d" \
  -H "Content-Type: application/json" \
  --data "@/setup/ilm-policy.json"

curl -X PUT "http://elasticsearch:9200/_index_template/logs-template" \
  -H "Content-Type: application/json" \
  --data "@/setup/index-template.json"
