#!/bin/bash

# Docker build test script for CoreMS services

set -e

echo "🐳 Testing Docker builds for CoreMS services..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test user-service
echo "📦 Building user-service..."
if docker build -f user-ms/Dockerfile -t corems-user-service:test . > /dev/null 2>&1; then
    echo -e "${GREEN}✓ user-service build successful${NC}"
else
    echo -e "${RED}✗ user-service build failed${NC}"
    exit 1
fi

# Test communication-service
echo "📦 Building communication-service..."
if docker build -f communication-ms/Dockerfile -t corems-communication-service:test . > /dev/null 2>&1; then
    echo -e "${GREEN}✓ communication-service build successful${NC}"
else
    echo -e "${RED}✗ communication-service build failed${NC}"
    exit 1
fi

# Test document-service
echo "📦 Building document-service..."
if docker build -f document-ms/Dockerfile -t corems-document-service:test . > /dev/null 2>&1; then
    echo -e "${GREEN}✓ document-service build successful${NC}"
else
    echo -e "${RED}✗ document-service build failed${NC}"
    exit 1
fi

# Test translation-service
echo "📦 Building translation-service..."
if docker build -f translation-ms/Dockerfile -t corems-translation-service:test . > /dev/null 2>&1; then
    echo -e "${GREEN}✓ translation-service build successful${NC}"
else
    echo -e "${RED}✗ translation-service build failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 All Docker builds successful!${NC}"
echo ""
echo "Next steps:"
echo "  1. Test with docker-compose: docker-compose up --build"
echo "  2. Check health endpoints: curl http://localhost:3000/actuator/health"
echo "  3. View logs: docker-compose logs -f"
