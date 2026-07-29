# SAMI Deployment Commands

## Install
```bash
sudo apt update && sudo apt install -y ca-certificates curl git
```

## Build
```bash
cd ~/sami-full-project/sami-backend
docker compose --env-file .env -f docker-compose.prod.yml config
docker compose --env-file .env -f docker-compose.prod.yml build
```

## Start
```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

## Stop
```bash
docker compose -f docker-compose.prod.yml stop
```

## Restart
```bash
docker compose -f docker-compose.prod.yml restart
```

## Status
```bash
docker compose -f docker-compose.prod.yml ps
curl -f http://localhost/health
```

## Logs
```bash
docker compose -f docker-compose.prod.yml logs
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs --tail=200 frontend
docker compose -f docker-compose.prod.yml logs --tail=200 db
```

## Update
```bash
cd ~/sami-full-project
git pull --ff-only origin development
cd sami-backend
docker compose --env-file .env -f docker-compose.prod.yml up --build -d
```

## Backup
```bash
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U sami -d sami -Fc > "sami-$(date +%F-%H%M).dump"
```

## Restore
```bash
cat BACKUP.dump | docker compose -f docker-compose.prod.yml exec -T db \
  pg_restore -U sami -d sami --clean --if-exists
```

## Troubleshooting
```bash
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml ps -a
docker compose -f docker-compose.prod.yml logs --tail=300
docker system df
df -h
free -h
sudo ss -ltnp
```
