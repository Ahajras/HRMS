# HRMS — Server Operations Cheat Sheet

The VPS runs the whole stack with Docker Compose. Code lives in `/opt/hrms`.
Public app: `http://<VPS_IP>:82`.

## Deploy / update (the normal loop)

You push from your PC, then update the server:

```bash
cd /opt/hrms
./deploy.sh
```

`deploy.sh` does: `git fetch` + `git reset --hard origin/main` + rebuild containers + show health and logs.
It uses `reset --hard`, so any **manual edits to tracked files on the server are discarded** — always change code on your PC and push. Your `.env` is *not* tracked, so it is safe.

## Containers

```bash
COMPOSE="docker compose -f docker-compose.prod.yml"

$COMPOSE ps                      # status of all services
$COMPOSE logs -f                 # tail all logs
$COMPOSE logs -f backend         # tail just the backend
$COMPOSE restart backend         # restart one service
$COMPOSE up -d --build           # rebuild + restart (what deploy.sh runs)
$COMPOSE down                    # stop (keeps data volumes)
$COMPOSE down -v                 # stop AND wipe the database/volumes
```

## Health checks

```bash
curl http://localhost:82/actuator/health     # -> {"status":"UP"}
curl http://localhost:82/api/v1/currencies    # needs auth (401 without a token = working)
```

## First-time login

- URL: `http://<VPS_IP>:82`
- User: `admin`  ·  Password: `Admin@123`  ← change after first login (Users screen)

The `admin` account is a *platform* admin (no company). To see/create company-scoped
data (employees, pay components), paste any UUID into the **Company ID** field in the top bar
(e.g. `11111111-1111-1111-1111-111111111111`) and reload.

## Secrets — set once in `/opt/hrms/.env`

```bash
cd /opt/hrms
nano .env
```

Make sure these are strong:
- `POSTGRES_PASSWORD` — database password
- `HRMS_JWT_SECRET` — must be **at least 32 characters**; signs login tokens
- `HTTP_PORT` — public port (currently `82`)

After editing `.env`, apply it: `docker compose -f docker-compose.prod.yml up -d`.

## Database

```bash
# open a psql shell
docker compose -f docker-compose.prod.yml exec postgres psql -U hrms -d hrms

# Flyway runs migrations automatically on backend startup; check what's applied:
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U hrms -d hrms -c "select version, description, success from flyway_schema_history order by installed_rank;"
```

## Fresh start (wipe everything)

```bash
cd /opt/hrms
docker compose -f docker-compose.prod.yml down -v
./deploy.sh
```
