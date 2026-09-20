#!/bin/sh
# Polls the SonarQube/SonarCloud quality gate for a project and fails
# the caller when the gate is ERROR. Unlike the stock quality-gate action
# it does not need .scannerwork/report-task.txt (absent for Maven builds),
# and it prints the real API error instead of crashing on empty output.
#
# Required env: SONAR_HOST_URL, SONAR_TOKEN, SONAR_PROJECT_KEY
# Optional env: BRANCH_NAME (default: master), PR_NUMBER (pull-request mode),
#   GITHUB_EVENT_NAME (when pull_request + PR_NUMBER set -> PR mode),
#   TIMEOUT_ATTEMPTS (default 20), SLEEP_SECONDS (default 15).
set -eu

: "${SONAR_HOST_URL:?SONAR_HOST_URL is not set}"
: "${SONAR_TOKEN:?SONAR_TOKEN is not set}"
: "${SONAR_PROJECT_KEY:?SONAR_PROJECT_KEY is not set}"
BRANCH_NAME="${BRANCH_NAME:-master}"
TIMEOUT_ATTEMPTS="${TIMEOUT_ATTEMPTS:-20}"
SLEEP_SECONDS="${SLEEP_SECONDS:-15}"

if [ "${GITHUB_EVENT_NAME:-push}" = "pull_request" ] && [ -n "${PR_NUMBER:-}" ]; then
  QUERY="projectKey=${SONAR_PROJECT_KEY}&pullRequest=${PR_NUMBER}"
else
  QUERY="projectKey=${SONAR_PROJECT_KEY}&branch=${BRANCH_NAME}"
fi

RESP_FILE="$(mktemp)"
trap 'rm -f "$RESP_FILE"' EXIT INT TERM

echo "Polling quality gate status for ${QUERY} ..."
attempt=1
while [ "$attempt" -le "$TIMEOUT_ATTEMPTS" ]; do
  HTTP_CODE="$(curl -sS -o "$RESP_FILE" -w "%{http_code}" \
    -u "${SONAR_TOKEN}:" "${SONAR_HOST_URL}/api/qualitygates/project_status?${QUERY}")" || {
    echo "curl to SonarQube API failed (attempt $attempt)."
    exit 1
  }
  if [ "$HTTP_CODE" != "200" ]; then
    echo "SonarQube API returned HTTP $HTTP_CODE (attempt $attempt):"
    head -c 500 "$RESP_FILE"
    echo ""
    exit 1
  fi
  STATUS="$(python3 -c "import sys,json; print(json.load(open('$RESP_FILE'))['projectStatus']['status'])")"
  echo "Attempt $attempt/$TIMEOUT_ATTEMPTS: $STATUS"
  if [ "$STATUS" = "OK" ]; then
    echo "Quality gate passed."
    exit 0
  fi
  if [ "$STATUS" = "ERROR" ]; then
    echo "Quality gate FAILED. Conditions:"
    python3 -c "import sys,json; [print('-', c['metricKey'], c['status'], c.get('actualValue')) for c in json.load(open('$RESP_FILE'))['projectStatus']['conditions']]"
    exit 1
  fi
  attempt=$((attempt + 1))
  sleep "$SLEEP_SECONDS"
done

echo "Quality gate status unknown after $TIMEOUT_ATTEMPTS attempts."
exit 1
