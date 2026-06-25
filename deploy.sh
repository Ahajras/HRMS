#!/usr/bin/env bash
#
# HRMS server deploy script.
# Pulls the latest code from GitHub and rebuilds the Docker stack.
#
# Usage (on the VPS):
#   cd /opt/hrms
#   ./deploy.sh
#
set -euo pipefail

REPO_DIR="${HRMS_DIR:-/opt/hrms}"
COMPOSE_FILE="docker-compose.prod.yml"
BRANCH="${HRMS_BRANCH:-main}"

cd "$REPO_DIR"

echo "==> Pulling latest code (branch: $BRANCH)"
git fetch origin "$BRANCH"
git reset --hard "origin/$BRANCH"

echo "==> Rebuilding and restarting containers"
docker compose -f "$COMPOSE_FILE" up -d --build

echo "==> Pruning old images"
docker image prune -f >/dev/null 2>&1 || true

echo "==> Done. Backend health:"
sleep 5
curl -fsS http://localhost:${HTTP_PORT:-82}/actuator/health || echo "(health check not ready yet - give it a minute, then check again)"
echo
echo "==> Recent backend logs:"
docker compose -f "$COMPOSE_FILE" logs --tail=20 backend
