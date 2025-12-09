# Docker Setup

Run CoreMS services and infrastructure using Docker.

## Setup

```bash
# Copy environment file
cp .env-example .env

# Edit .env with your configuration
nano .env
```

## Quick Start

```bash
# Start infrastructure (postgres, rabbitmq, minio)
./manage.sh start infra

# Start all services
./manage.sh start services

# Start everything
./manage.sh start all

# Check status
./manage.sh status
```

## Structure

```
docker/
├── .env                      # Environment config (gitignored)
├── .env-example              # Example configuration
├── manage.sh                 # Management script
├── docker-test.sh            # Test builds
├── README.md                 # This file
├── infrastructure/           # Database, queue, storage
│   ├── postgres-compose.yaml
│   ├── rabbitmq-compose.yaml
│   ├── s3-minio-compose.yaml
│   ├── postgres-data/       # Data (gitignored)
│   └── rabbitmq-data/       # Data (gitignored)
└── services/                 # Microservices
    ├── user-service-compose.yaml
    ├── communication-service-compose.yaml
    ├── document-service-compose.yaml
    └── translation-service-compose.yaml
```

## Management Commands

```bash
# Start components
./manage.sh start infra      # All infrastructure
./manage.sh start services   # All microservices
./manage.sh start all        # Everything
./manage.sh start postgres   # Individual component
./manage.sh start user       # Individual service

# Stop components
./manage.sh stop infra
./manage.sh stop services
./manage.sh stop all

# View logs
./manage.sh logs postgres
./manage.sh logs user

# Check status
./manage.sh status

# Clean everything (removes volumes)
./manage.sh clean
```

## Service Endpoints

**Infrastructure:**
- PostgreSQL: `localhost:5432`
- RabbitMQ: `localhost:5672` (UI: `http://localhost:15672`)
- MinIO: `localhost:9000` (Console: `http://localhost:9001`)

**Services:**
- User: `http://localhost:3000` (Health: `/actuator/health`)
- Communication: `http://localhost:3001` (Health: `/actuator/health`)
- Document: `http://localhost:3002` (Health: `/actuator/health`)
- Translation: `http://localhost:3003` (Health: `/actuator/health`)

## Development Workflow

### Option 1: Infrastructure Only (Recommended)

Start infrastructure, run services from IDE:

```bash
./manage.sh start infra
```

Benefits:
- Fast code iteration (no rebuild)
- IDE debugging with breakpoints
- Instant changes

### Option 2: Full Docker

Run everything in Docker:

```bash
./manage.sh start all
```

Benefits:
- Integration testing
- Production simulation
- Consistent environment

### Option 3: Mixed

Start infrastructure + specific services:

```bash
./manage.sh start infra
./manage.sh start user
```

Run other services from IDE.

## Manual Commands

If you prefer not to use the management script:

```bash
# Create network
docker network create corems-network

# Start infrastructure
docker-compose --env-file .env -f infrastructure/postgres-compose.yaml up -d
docker-compose --env-file .env -f infrastructure/rabbitmq-compose.yaml up -d
docker-compose --env-file .env -f infrastructure/s3-minio-compose.yaml up -d

# Start services
docker-compose --env-file .env -f services/user-service-compose.yaml up -d
docker-compose --env-file .env -f services/communication-service-compose.yaml up -d
docker-compose --env-file .env -f services/document-service-compose.yaml up -d
docker-compose --env-file .env -f services/translation-service-compose.yaml up -d
```

## Environment Variables

All compose files use `.env` file in this directory.

**Setup:**
```bash
cp .env-example .env
# Edit .env with your configuration
```

## Testing

```bash
# Test all Docker builds
./docker-test.sh

# Test health endpoints
curl http://localhost:3000/actuator/health
curl http://localhost:3001/actuator/health
curl http://localhost:3002/actuator/health
curl http://localhost:3003/actuator/health
```

## Troubleshooting

### Services won't start

```bash
# Ensure network exists
docker network create corems-network

# Check logs
./manage.sh logs <component>

# Restart
./manage.sh restart <component>
```

### Port conflicts

Edit compose file and change host port:
```yaml
ports:
  - "3100:3000"  # Changed from 3000:3000
```

### Clean slate

```bash
./manage.sh clean
./manage.sh start infra
```

## AWS Deployment

Dockerfiles are production-ready:
- Multi-stage builds (small images ~200MB)
- Non-root user for security
- Built-in health checks
- Optimized layer caching

Build and push to ECR:
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin {account}.dkr.ecr.us-east-1.amazonaws.com
docker build -f ../user-ms/Dockerfile -t corems-user-service ..
docker tag corems-user-service:latest {account}.dkr.ecr.us-east-1.amazonaws.com/corems-user-service:latest
docker push {account}.dkr.ecr.us-east-1.amazonaws.com/corems-user-service:latest
```
