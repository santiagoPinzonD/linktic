#!/bin/bash

# Diagnóstico rápido para problemas de conectividad
# Crear en: scripts/quick-diagnosis.sh

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔍 Diagnóstico Rápido de Servicios${NC}"
echo "=================================="

# 1. Estado de contenedores
echo -e "\n${BLUE}📊 Estado de contenedores:${NC}"
docker-compose ps

# 2. Verificar puertos
echo -e "\n${BLUE}🔌 Verificando puertos:${NC}"
echo "Puerto 8081 (productos):"
curl -s -m 5 http://localhost:8081/actuator/health 2>/dev/null && echo -e "${GREEN}✅ Responde${NC}" || echo -e "${RED}❌ No responde${NC}"

echo "Puerto 8082 (inventario):"
curl -s -m 5 http://localhost:8082/actuator/health 2>/dev/null && echo -e "${GREEN}✅ Responde${NC}" || echo -e "${RED}❌ No responde${NC}"

# 3. Verificar con API Key
echo -e "\n${BLUE}🔐 Verificando con API Keys:${NC}"
echo "Productos con API Key:"
curl -s -m 5 -H "X-API-Key: productos-secret-key" http://localhost:8081/api/v1/productos?size=1 2>/dev/null && echo -e "${GREEN}✅ API Key válida${NC}" || echo -e "${RED}❌ API Key inválida o servicio no responde${NC}"

echo "Inventario con API Key:"
curl -s -m 5 -H "X-API-Key: inventario-secret-key" http://localhost:8082/api/v1/inventarios?size=1 2>/dev/null && echo -e "${GREEN}✅ API Key válida${NC}" || echo -e "${RED}❌ API Key inválida o servicio no responde${NC}"

# 4. Logs recientes
echo -e "\n${BLUE}📋 Logs recientes de servicios:${NC}"
echo -e "${YELLOW}--- Productos Service (últimas 10 líneas) ---${NC}"
docker-compose logs --tail=1000 productos-service 2>/dev/null || echo "No hay logs disponibles"

echo -e "\n${YELLOW}--- Inventario Service (últimas 10 líneas) ---${NC}"
docker-compose logs --tail=100 inventario-service 2>/dev/null || echo "No hay logs disponibles"

# 5. Verificar bases de datos
echo -e "\n${BLUE}🗄️ Verificando bases de datos:${NC}"
echo "Productos DB:"
docker-compose exec -T productos-db pg_isready -U admin 2>/dev/null && echo -e "${GREEN}✅ Conectada${NC}" || echo -e "${RED}❌ No conectada${NC}"

echo "Inventario DB:"
docker-compose exec -T inventario-db pg_isready -U admin 2>/dev/null && echo -e "${GREEN}✅ Conectada${NC}" || echo -e "${RED}❌ No conectada${NC}"

echo -e "\n${BLUE}💡 Próximos pasos sugeridos:${NC}"
echo "================================="
echo "Si algún servicio no responde:"
echo "1. make logs-productos    # Ver logs detallados"
echo "2. make logs-inventario   # Ver logs detallados"
echo "3. make restart           # Reiniciar servicios"
echo "4. make health-check      # Verificación completa"