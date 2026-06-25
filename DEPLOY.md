# Deploying HRMS to a VPS (Docker)

This brings up the whole stack — PostgreSQL, Redis, RabbitMQ, MinIO, the Spring Boot backend, and the React frontend — with one command. No need to install Java, Maven, or Node on the VPS; everything is built and run inside containers.

## Prerequisites on the VPS

- A Linux server with Docker Engine and the Compose plugin:
  ```bash
  docker --version
  docker compose version
  ```
- Inbound port **80** open in the firewall / security group.
- About **2 vCPU / 4 GB RAM** is comfortable for testing (the backend build step is the heaviest moment).

## 1. Get the code onto the VPS

Either `git clone` your repository, or copy the project folder up with `scp`/`rsync`:

```bash
rsync -av --exclude node_modules --exclude target ./HRMS/ user@VPS_IP:/opt/hrms/
```

## 2. Configure secrets

```bash
cd /opt/hrms
cp .env.example .env
nano .env          # set strong POSTGRES_PASSWORD etc.
```

`HTTP_PORT` controls the public port (default `80`). For a quick test on a non-privileged port you can set e.g. `HTTP_PORT=8088`.

## 3. Build and start

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

First run takes a few minutes (Maven downloads dependencies and compiles, npm builds the frontend). Watch progress:

```bash
docker compose -f docker-compose.prod.yml logs -f backend
```

Flyway applies migrations V1–V5 automatically on backend startup.

## 4. Open it

- App: `http://VPS_IP` (or `http://VPS_IP:HTTP_PORT`)
- Backend health: `http://VPS_IP/actuator/health` → should show `{"status":"UP"}`

## 5. First use — log in

The app requires authentication (Phase 2). Two accounts are seeded:

**Normal use — `manager` (recommended):** a company administrator tied to a real company (migration V7).

- **Username:** `manager`
- **Password:** `Admin@123`  ← change this after first login (Users screen)

Logging in as `manager` carries the company in the token, so there is **no Company ID field** — you work straight inside your company. This is the account to use day-to-day and to hand to other users (create more from the **Users** screen, assign roles from **Roles**).

**Platform admin — `admin`:** a global super-admin not tied to any company (migration V6), for managing across companies.

- **Username:** `admin`  **Password:** `Admin@123`

Because `admin` has no company, the top bar shows a **Company ID** field; that field only appears for platform admins. Reference data (currencies, countries, org levels) is global; everything else is company-scoped.

Set a strong `HRMS_JWT_SECRET` (≥ 32 chars) in `.env` before going beyond a test environment.

## Useful commands

```bash
# Status
docker compose -f docker-compose.prod.yml ps

# Tail logs
docker compose -f docker-compose.prod.yml logs -f

# Rebuild after pulling new code
docker compose -f docker-compose.prod.yml up -d --build

# Stop (keeps data volumes)
docker compose -f docker-compose.prod.yml down

# Stop and wipe data (fresh DB)
docker compose -f docker-compose.prod.yml down -v
```

## Notes

- Data persists in named Docker volumes (`pgdata`, `redisdata`, `rabbitdata`, `miniodata`); `down` keeps them, `down -v` removes them.
- Postgres/Redis/RabbitMQ/MinIO are **not** published to the host in this prod compose — only the frontend (port 80) is reachable. The backend is reached through the Nginx `/api` proxy on the same origin, so there are no CORS issues.
- To expose the RabbitMQ or MinIO consoles temporarily for debugging, add a `ports:` mapping to that service and re-run `up -d`.
- This setup uses plain HTTP on an IP, which is fine for a test environment. For a real deployment put it behind a domain + HTTPS (e.g. a reverse proxy like Caddy or Nginx with Let's Encrypt) — ask and I'll add that.
