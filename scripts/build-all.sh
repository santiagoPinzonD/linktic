#!/bin/bash
# scripts/build-all.sh
# Script para construir todos los microservicios

echo "🔨 Construyendo microservicios..."

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Construir Productos Service
echo "📦 Construyendo Productos Service..."
cd productos-service
if mvn clean package -DskipTests; then
    echo -e "${GREEN}✓ Productos Service construido exitosamente${NC}"
else
    echo -e "${RED}✗ Error al construir Productos Service${NC}"
    exit 1
fi
cd ..

# Construir Inventario Service
echo "📦 Construyendo Inventario Service..."
cd inventario-service
if mvn clean package -DskipTests; then
    echo -e "${GREEN}✓ Inventario Service construido exitosamente${NC}"
else
    echo -e "${RED}✗ Error al construir Inventario Service${NC}"
    exit 1
fi
cd ..

echo -e "${GREEN}✅ Todos los servicios construidos exitosamente${NC}"
