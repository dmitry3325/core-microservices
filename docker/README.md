# Docker — Local dev quick-start

This folder holds docker helpers for local development.
Step 1 is running the development database.

### Prereqs
- Docker & Docker Compose
- A project `.env` one level up with DB vars (DATABASE_USER, DATABASE_PASSWORD, DATABASE_DB, POSTGRES_PORT)

### Quick start
From this folder:

```cmd
# Start Postgres
docker-compose --env-file ../.env -f postgres-compose.yaml up -d

# Start RabbitMQ
docker-compose --env-file ../.env -f rabbitmq-compose.yaml up -d
```

### Connect DATABASE
- From host (if you have psql):
```cmd
psql -h localhost -p %POSTGRES_PORT% -U %DATABASE_USER% -d %DATABASE_DB%
```
- Or exec into container:
```cmd
docker exec -it <container> psql -U %DATABASE_USER% -d %DATABASE_DB%
```