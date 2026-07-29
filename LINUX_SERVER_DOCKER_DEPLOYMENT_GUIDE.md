# SAMI ERP Linux Server Docker Deployment Guide

## 1. Deployment overview

This test deployment runs three containers: PostgreSQL 16, the Spring Boot API,
and nginx serving the Vue application. Only host port `80` is public. The
browser opens `http://SERVER_PUBLIC_IP`; nginx serves the SPA and forwards
`/api` to the backend. PostgreSQL, uploads and managed files use named Docker
volumes and are not publicly exposed.

## 2. Server requirements

Use Ubuntu 22.04/24.04 LTS or Debian 12, with a public IPv4 address, SSH access,
2 CPU cores, 4 GB RAM and 20 GB free disk as a practical minimum. Open the
configured SSH port and TCP 80. TCP 443 is optional until HTTPS is added.

## 3. Information to collect

- Server IP, SSH username and SSH port
- Repository URL and branch (`development`)
- Strong database and bootstrap-admin passwords
- Two different random JWT secrets of at least 32 characters
- Optional future domain name

Never paste secrets into source files or commit `.env`.

## 4. Connect to the server

```bash
ssh USER@SERVER_PUBLIC_IP
# Non-standard SSH port:
ssh -p SSH_PORT USER@SERVER_PUBLIC_IP
```

## 5. Update Ubuntu/Debian

```bash
sudo apt update
sudo apt upgrade -y
```

## 6. Install Git, curl, Docker and Compose

```bash
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
docker --version
sudo docker compose version
```

For Debian, replace `linux/ubuntu` with `linux/debian` in both Docker repository
URLs.

## 7. Docker permissions

```bash
sudo usermod -aG docker "$USER"
exit
```

Reconnect over SSH, then verify:

```bash
docker run --rm hello-world
```

## 8. Firewall

Do not enable a firewall until SSH is allowed.

```bash
sudo apt install -y ufw
sudo ufw allow SSH_PORT/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

Replace `SSH_PORT`. Do not allow port 5432.

## 9. Clone the monorepo and select the branch

```bash
cd ~
git clone REPOSITORY_URL sami-full-project
cd sami-full-project
git switch development
git pull --ff-only origin development
```

The expected directories are `sami-backend` and `sami-frontend` under this one
repository.

## 10. Create the environment file

```bash
cd ~/sami-full-project/sami-backend
cp .env.test.example .env
chmod 600 .env
nano .env
```

Replace every `CHANGE_ME` and `SERVER_PUBLIC_IP`. Generate secrets with:

```bash
openssl rand -base64 48
```

`POSTGRES_PASSWORD`, `JWT_SECRET`, `PORTAL_JWT_SECRET` and
`BOOTSTRAP_ADMIN_PASSWORD` are secret and required. JWT secrets must differ.
`CORS_ALLOWED_ORIGINS` should be `http://SERVER_PUBLIC_IP`.
`SPRING_PROFILES_ACTIVE=prod`, `VITE_API_BASE_URL=/api`, port 80 and disabled
demo/licensing enforcement are appropriate test defaults.

## 11. Validate, build and start

```bash
cd ~/sami-full-project/sami-backend
docker compose --env-file .env -f docker-compose.prod.yml config
docker compose --env-file .env -f docker-compose.prod.yml build
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

## 12. Status and logs

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs --tail=200 frontend
docker compose -f docker-compose.prod.yml logs --tail=200 db
```

All three services should be running and healthy. Backend logs should show
Flyway completing without validation errors.

## 13. Verify migrations and open the application

```bash
curl -f http://localhost/health
docker compose -f docker-compose.prod.yml exec db \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c 'select installed_rank, version, description, success from flyway_schema_history order by installed_rank;'
```

Open `http://SERVER_PUBLIC_IP`. The bootstrap login is the email and password
you placed in `.env`; it is created only when that email does not already exist.

## 14. Smoke test

1. Open the frontend and login page.
2. Log in with the configured bootstrap account.
3. Confirm the dashboard loads.
4. Open `http://SERVER_PUBLIC_IP/health`.
5. Confirm an authenticated API request succeeds in browser developer tools.
6. Confirm PostgreSQL and backend are healthy.
7. Confirm all Flyway rows show success.
8. Refresh a nested frontend URL and confirm no 404.
9. Confirm no critical browser-console or backend-log errors.
10. Restart the stack and confirm login/data remain.
11. Test an upload if file features are enabled.

## 15. Common problems

- **Docker permission denied:** reconnect after `usermod`, or temporarily use
  `sudo docker`.
- **Port 80 occupied:** `sudo ss -ltnp | grep ':80'`; stop the conflicting
  service or set `FRONTEND_PORT`.
- **Container exits:** inspect `docker compose ... logs SERVICE`.
- **Database refused:** wait for `db` health; verify the three `POSTGRES_*`
  variables and disk space.
- **Flyway failure:** do not edit old migrations; capture backend logs and the
  `flyway_schema_history` rows.
- **Frontend/API/CORS failure:** keep `VITE_API_BASE_URL=/api`; ensure
  `CORS_ALLOWED_ORIGINS` exactly matches the browser origin.
- **Blank page or route 404:** rebuild frontend and verify nginx is using the
  tracked configuration.
- **Login points to localhost:** confirm browser requests use relative `/api`.
- **JWT error:** use two distinct secrets of at least 32 characters.
- **Out of memory:** inspect `free -h`; use at least 4 GB RAM or add swap.
- **Disk full:** inspect `df -h` and `docker system df`; never delete volumes
  without a verified backup.
- **Firewall:** `sudo ufw status`; allow SSH and port 80.

## 16. Restart, stop and reboot recovery

```bash
docker compose -f docker-compose.prod.yml restart
docker compose -f docker-compose.prod.yml stop
docker compose -f docker-compose.prod.yml start
```

`restart: unless-stopped` and enabled Docker restart containers after reboot.
Verify with `docker compose ... ps` and `curl -f http://localhost/health`.

## 17. Safe update

```bash
cd ~/sami-full-project
git status
git pull --ff-only origin development
cd sami-backend
docker compose --env-file .env -f docker-compose.prod.yml build
docker compose --env-file .env -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=200 backend
```

Back up before an update containing migrations.

## 18. Backup and restore

```bash
# Backup
cd ~/sami-full-project/sami-backend
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U sami -d sami -Fc > "sami-$(date +%F-%H%M).dump"

# Restore (destructive to the target database)
docker compose -f docker-compose.prod.yml stop backend
cat BACKUP.dump | docker compose -f docker-compose.prod.yml exec -T db \
  pg_restore -U sami -d sami --clean --if-exists
docker compose -f docker-compose.prod.yml start backend
```

Replace database/user names if `.env` differs. Store backups off-server and
also back up the `uploads`, `managed-files` and `file-staging` volumes.

## 19. Docker cleanup and full reset

Safe cleanup:

```bash
docker image prune
docker builder prune
```

Do not use `docker compose down --volumes` during normal maintenance.

> **DESTRUCTIVE FULL RESET — permanently deletes the database and stored files**
>
> ```bash
> docker compose -f docker-compose.prod.yml down --volumes
> ```
>
> Run only after confirming the project directory and a tested backup.

## 20. Security notes and future production improvements

Use no real customer data during HTTP-only testing. Change all example
credentials, restrict SSH, keep PostgreSQL private, protect `.env`, and back up
regularly. Add a domain and Let's Encrypt HTTPS before real use.

Future work—not part of this test deployment—includes TLS/reverse-proxy
hardening, automated backups, monitoring, centralized logs, CI/CD, a secret
manager, rate limiting/WAF, object storage and high availability.
