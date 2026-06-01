#!/bin/sh
set -eu

sleep 15

curl -X POST "http://kibana:5601/api/data_views/data_view" \
  -H "Content-Type: application/json" \
  -H "kbn-xsrf: true" \
  --data "@/setup/data-view.json"
