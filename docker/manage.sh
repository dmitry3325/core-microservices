#!/bin/bash

# CoreMS Docker Management Script
# Easily start/stop infrastructure and services

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if .env exists
if [ ! -f ".env" ]; then
    echo -e "${RED}Error: .env file not found${NC}"
    echo -e "${YELLOW}Copy .env-example to .env and configure it${NC}"
    exit 1
fi

# Function to show usage
show_usage() {
    echo -e "${BLUE}CoreMS Docker Management${NC}"
    echo ""
    echo "Usage: ./manage.sh [command] [component]"
    echo ""
    echo "Commands:"
    echo "  start     - Start component(s)"
    echo "  stop      - Stop component(s)"
    echo "  restart   - Restart component(s)"
    echo "  rebuild   - Rebuild and restart component"
    echo "  logs      - View logs for component(s)"
    echo "  status    - Show status of all components"
    echo "  clean     - Stop all and remove volumes"
    echo ""
    echo "Components:"
    echo "  infra     - All infrastructure (postgres, rabbitmq, minio)"
    echo "  services  - All microservices"
    echo "  all       - Everything"
    echo "  postgres  - PostgreSQL database"
    echo "  rabbitmq  - RabbitMQ message broker"
    echo "  minio     - MinIO S3 storage"
    echo "  user      - User service"
    echo "  comm      - Communication service"
    echo "  doc       - Document service"
    echo "  trans     - Translation service"
    echo ""
    echo "Examples:"
    echo "  ./manage.sh start infra       # Start all infrastructure"
    echo "  ./manage.sh start postgres    # Start only postgres"
    echo "  ./manage.sh logs user         # View user service logs"
    echo "  ./manage.sh status            # Show status of all"
}

# Function to ensure network exists
ensure_network() {
    if ! docker network inspect corems-network >/dev/null 2>&1; then
        echo -e "${YELLOW}Creating corems-network...${NC}"
        docker network create corems-network
    fi
}

# Function to start component
start_component() {
    local component=$1
    local file=""
    
    # Determine file path based on component
    case $component in
        postgres|rabbitmq|s3-minio)
            file="infrastructure/${component}-compose.yaml"
            ;;
        user)
            file="services/user-service-compose.yaml"
            ;;
        comm)
            file="services/communication-service-compose.yaml"
            ;;
        doc)
            file="services/document-service-compose.yaml"
            ;;
        trans)
            file="services/translation-service-compose.yaml"
            ;;
        *)
            file="${component}-compose.yaml"
            ;;
    esac
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}Starting $component...${NC}"
        docker-compose --env-file .env -f "$file" up -d
    else
        echo -e "${RED}Error: $file not found${NC}"
        return 1
    fi
}

# Function to stop component
stop_component() {
    local component=$1
    local file=""
    
    # Determine file path based on component
    case $component in
        postgres|rabbitmq|s3-minio)
            file="infrastructure/${component}-compose.yaml"
            ;;
        user)
            file="services/user-service-compose.yaml"
            ;;
        comm)
            file="services/communication-service-compose.yaml"
            ;;
        doc)
            file="services/document-service-compose.yaml"
            ;;
        trans)
            file="services/translation-service-compose.yaml"
            ;;
        *)
            file="${component}-compose.yaml"
            ;;
    esac
    
    if [ -f "$file" ]; then
        echo -e "${YELLOW}Stopping $component...${NC}"
        docker-compose -f "$file" down
    fi
}

# Function to show logs
show_logs() {
    local component=$1
    local file=""
    
    # Determine file path based on component
    case $component in
        postgres|rabbitmq|s3-minio)
            file="infrastructure/${component}-compose.yaml"
            ;;
        user)
            file="services/user-service-compose.yaml"
            ;;
        comm)
            file="services/communication-service-compose.yaml"
            ;;
        doc)
            file="services/document-service-compose.yaml"
            ;;
        trans)
            file="services/translation-service-compose.yaml"
            ;;
        *)
            file="${component}-compose.yaml"
            ;;
    esac
    
    if [ -f "$file" ]; then
        docker-compose -f "$file" logs -f
    fi
}

# Main logic
COMMAND=$1
COMPONENT=$2

if [ -z "$COMMAND" ]; then
    show_usage
    exit 0
fi

case $COMMAND in
    start)
        ensure_network
        case $COMPONENT in
            infra)
                start_component "postgres"
                start_component "rabbitmq"
                start_component "s3-minio"
                ;;
            services)
                start_component "user"
                start_component "comm"
                start_component "doc"
                start_component "trans"
                ;;
            all)
                start_component "postgres"
                start_component "rabbitmq"
                start_component "s3-minio"
                start_component "user"
                start_component "comm"
                start_component "doc"
                start_component "trans"
                ;;
            postgres|rabbitmq|minio|user|comm|doc|trans)
                start_component "$COMPONENT"
                ;;
            *)
                echo -e "${RED}Unknown component: $COMPONENT${NC}"
                show_usage
                exit 1
                ;;
        esac
        echo -e "${GREEN}✓ Done${NC}"
        ;;
        
    stop)
        case $COMPONENT in
            infra)
                stop_component "postgres"
                stop_component "rabbitmq"
                stop_component "s3-minio"
                ;;
            services)
                stop_component "user"
                stop_component "comm"
                stop_component "doc"
                stop_component "trans"
                ;;
            all)
                stop_component "user"
                stop_component "comm"
                stop_component "doc"
                stop_component "trans"
                stop_component "postgres"
                stop_component "rabbitmq"
                stop_component "s3-minio"
                ;;
            postgres|rabbitmq|minio|user|comm|doc|trans)
                stop_component "$COMPONENT"
                ;;
            *)
                echo -e "${RED}Unknown component: $COMPONENT${NC}"
                show_usage
                exit 1
                ;;
        esac
        echo -e "${GREEN}✓ Done${NC}"
        ;;
        
    restart)
        $0 stop "$COMPONENT"
        sleep 2
        $0 start "$COMPONENT"
        ;;
        
    rebuild)
        if [ -z "$COMPONENT" ]; then
            echo -e "${RED}Please specify a component to rebuild${NC}"
            exit 1
        fi
        
        case $COMPONENT in
            services)
                echo -e "${YELLOW}Rebuilding all services...${NC}"
                $0 rebuild user
                $0 rebuild comm
                $0 rebuild doc
                $0 rebuild trans
                echo -e "${GREEN}✓ All services rebuilt${NC}"
                ;;
            infra)
                echo -e "${YELLOW}Rebuilding all infrastructure...${NC}"
                $0 rebuild postgres
                $0 rebuild rabbitmq
                $0 rebuild s3-minio
                echo -e "${GREEN}✓ All infrastructure rebuilt${NC}"
                ;;
            all)
                echo -e "${YELLOW}Rebuilding everything...${NC}"
                $0 rebuild infra
                $0 rebuild services
                echo -e "${GREEN}✓ Everything rebuilt${NC}"
                ;;
            *)
                echo -e "${YELLOW}Rebuilding $COMPONENT...${NC}"
                
                file=""
                case $COMPONENT in
                    postgres|rabbitmq|s3-minio)
                        file="infrastructure/${COMPONENT}-compose.yaml"
                        ;;
                    user)
                        file="services/user-service-compose.yaml"
                        ;;
                    comm)
                        file="services/communication-service-compose.yaml"
                        ;;
                    doc)
                        file="services/document-service-compose.yaml"
                        ;;
                    trans)
                        file="services/translation-service-compose.yaml"
                        ;;
                    *)
                        echo -e "${RED}Unknown component: $COMPONENT${NC}"
                        exit 1
                        ;;
                esac
                
                if [ -f "$file" ]; then
                    # Stop first
                    docker-compose -f "$file" down 2>/dev/null || true
                    # Rebuild and start
                    docker-compose --env-file .env -f "$file" up -d --build
                    echo -e "${GREEN}✓ Rebuilt and started $COMPONENT${NC}"
                else
                    echo -e "${RED}Error: $file not found${NC}"
                    exit 1
                fi
                ;;
        esac
        ;;
        
    logs)
        if [ -z "$COMPONENT" ]; then
            echo -e "${RED}Please specify a component${NC}"
            exit 1
        fi
        show_logs "$COMPONENT"
        ;;
        
    status)
        echo -e "${BLUE}CoreMS Docker Status${NC}"
        echo ""
        docker ps --filter "name=corems-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        ;;
        
    clean)
        echo -e "${YELLOW}Stopping all services and removing volumes...${NC}"
        stop_component "user"
        stop_component "comm"
        stop_component "doc"
        stop_component "trans"
        stop_component "postgres"
        stop_component "rabbitmq"
        stop_component "s3-minio"
        echo -e "${GREEN}✓ Cleaned${NC}"
        ;;
        
    *)
        echo -e "${RED}Unknown command: $COMMAND${NC}"
        show_usage
        exit 1
        ;;
esac
