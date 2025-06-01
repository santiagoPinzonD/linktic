#!/bin/bash
# scripts/health-check.sh
# Script para verificar el estado de los servicios

echo "🏥 Verificando estado de los servicios..."

# Función para verificar un servicio
check_service() {
    local name=$1
    local url=$2
    
    if curl -s -f "$url" > /dev/null; then
        echo "✅ $name está funcionando"
        return 0
    else
        echo "❌ $name no está respondiendo"#!/bin/bash

# Script para verificar la salud de los microservicios
# Crear en: scripts/health-check.sh

set -e

echo "🔍 Verificando salud de los microservicios..."

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# URLs de los servicios
PRODUCTOS_URL="http://localhost:8081"
INVENTARIO_URL="http://localhost:8082"

# Función para verificar servicio
check_service() {
    local service_name="$1"
    local url="$2"
    local max_attempts=30
    local attempt=1

    echo -e "${BLUE}🔍 Verificando ${service_name}...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "${url}/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ ${service_name} está funcionando correctamente${NC}"
            return 0
        else
            echo -e "${YELLOW}⏳ Intento ${attempt}/${max_attempts} - ${service_name} no está listo aún...${NC}"
            sleep 2
            ((attempt++))
        fi
    done
    
    echo -e "${RED}❌ ${service_name} no responde después de ${max_attempts} intentos${NC}"
    return 1
}

# Función para verificar endpoint específico
check_endpoint() {
    local service_name="$1"
    local url="$2"
    local api_key="$3"
    
    echo -e "${BLUE}🔍 Verificando endpoint ${service_name}...${NC}"
    
    if curl -s -f -H "X-API-Key: ${api_key}" "${url}" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Endpoint ${service_name} funciona correctamente${NC}"
        return 0
    else
        echo -e "${RED}❌ Endpoint ${service_name} no responde${NC}"
        return 1
    fi
}

# Verificar servicios básicos
echo -e "${BLUE}📋 Estado de los contenedores:${NC}"
docker-compose ps

echo ""
echo -e "${BLUE}🏥 Verificando health checks...${NC}"

# Verificar servicio de productos
if check_service "Productos Service" "$PRODUCTOS_URL"; then
    # Verificar endpoint específico
    check_endpoint "Productos API" "$PRODUCTOS_URL/api/v1/productos?size=1" "productos-secret-key"
fi

echo ""

# Verificar servicio de inventario
if check_service "Inventario Service" "$INVENTARIO_URL"; then
    # Verificar endpoint específico
    check_endpoint "Inventario API" "$INVENTARIO_URL/api/v1/inventarios?size=1" "inventario-secret-key"
fi

echo ""
echo -e "${BLUE}📊 URLs de los servicios:${NC}"
echo -e "🛍️  Productos API: ${PRODUCTOS_URL}/swagger-ui.html"
echo -e "📦 Inventario API: ${INVENTARIO_URL}/swagger-ui.html"
echo -e "🏥 Health Productos: ${PRODUCTOS_URL}/actuator/health"
echo -e "🏥 Health Inventario: ${INVENTARIO_URL}/actuator/health"

echo ""
echo -e "${GREEN}✅ Verificación de salud completada${NC}"
        return 1
    fi
}

# Verificar servicios
check_service "Productos Service" "http://localhost:8081/actuator/health"
PRODUCTOS_STATUS=$?

check_service "Inventario Service" "http://localhost:8082/actuator/health"
INVENTARIO_STATUS=$?

# Resultado general
if [ $PRODUCTOS_STATUS -eq 0 ] && [ $INVENTARIO_STATUS -eq 0 ]; then
    echo "✅ Todos los servicios están saludables"
else
    echo "⚠️  Algunos servicios tienen problemas"
    exit 1
fi
