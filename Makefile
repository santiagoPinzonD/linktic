# Makefile para Microservicios Productos e Inventario
# Uso: make <comando>

.PHONY: help build test run stop clean logs setup dev prod

# Variables
COMPOSE_FILE := docker-compose.yml
PROJECT_NAME := microservicios
WAIT_TIME := 60

# Colores para output
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## 📖 Mostrar esta ayuda
	@echo ""
	@echo "$(BLUE)🛠️  Comandos disponibles para $(PROJECT_NAME):$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "$(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ""

setup: ## 🔧 Configuración inicial del proyecto
	@echo "$(BLUE)🔧 Configurando proyecto...$(NC)"
	@mkdir -p logs scripts monitoring
	@chmod +x scripts/*.sh 2>/dev/null || true
	@cp .env.example .env 2>/dev/null || echo "# Crear .env manualmente" > .env
	@echo "$(GREEN)✅ Proyecto configurado$(NC)"

build: ## 🏗️ Construir todas las imágenes Docker
	@echo "$(BLUE)🏗️ Construyendo imágenes Docker...$(NC)"
	docker-compose build --no-cache
	@echo "$(GREEN)✅ Imágenes construidas$(NC)"


test: ## 🧪 Ejecutar todas las pruebas
	@echo "$(BLUE)🧪 Ejecutando pruebas...$(NC)"
	@cd productos-service && mvn clean test
	@cd inventario-service && mvn clean test
	@echo "$(GREEN)✅ Pruebas completadas$(NC)"

test-coverage: ## 📊 Ejecutar pruebas con reporte de cobertura
	@echo "$(BLUE)📊 Ejecutando pruebas con cobertura...$(NC)"
	@cd productos-service && mvn clean test jacoco:report
	@cd inventario-service && mvn clean test jacoco:report
	@echo "$(GREEN)✅ Reportes de cobertura generados$(NC)"
	@echo "$(YELLOW)📁 Ver reportes en:$(NC)"
	@echo "   productos-service/target/site/jacoco/index.html"
	@echo "   inventario-service/target/site/jacoco/index.html"

run: ## 🚀 Iniciar todos los servicios
	@echo "$(BLUE)🚀 Iniciando servicios...$(NC)"
	docker-compose up -d
	@echo "$(YELLOW)⏳ Esperando que los servicios inicien ($(WAIT_TIME)s)...$(NC)"
	@sleep $(WAIT_TIME)
	@$(MAKE) health-check

run-build: build run ## 🔄 Construir e iniciar servicios

dev: ## 💻 Modo desarrollo (con logs en tiempo real)
	@echo "$(BLUE)💻 Iniciando en modo desarrollo...$(NC)"
	docker-compose up --build

stop: ## ⏹️ Detener todos los servicios
	@echo "$(BLUE)⏹️ Deteniendo servicios...$(NC)"
	docker-compose down
	@echo "$(GREEN)✅ Servicios detenidos$(NC)"

restart: stop run ## 🔄 Reiniciar todos los servicios

clean: ## 🧹 Limpiar contenedores, volúmenes e imágenes
	@echo "$(BLUE)🧹 Limpiando recursos Docker...$(NC)"
	docker-compose down -v --remove-orphans
	docker system prune -f
	@echo "$(YELLOW)🗑️ Limpiando directorios target...$(NC)"
	@find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
	@find . -name "logs" -type d -exec rm -rf {} + 2>/dev/null || true
	@echo "$(GREEN)✅ Limpieza completada$(NC)"

clean-all: clean ## 🧹 Limpieza completa (incluye imágenes)
	@echo "$(BLUE)🧹 Limpieza completa...$(NC)"
	docker rmi $(PROJECT_NAME)_productos-service $(PROJECT_NAME)_inventario-service 2>/dev/null || true
	docker volume prune -f
	@echo "$(GREEN)✅ Limpieza completa realizada$(NC)"

health-check: ## 🏥 Verificar salud de los servicios
	@echo "$(BLUE)🏥 Verificando salud de los servicios...$(NC)"
	@./scripts/health-check.sh || echo "$(RED)❌ Error en health check - ¿están los scripts ejecutables?$(NC)"

status: ## 📊 Ver estado de los contenedores
	@echo "$(BLUE)📊 Estado de los contenedores:$(NC)"
	@docker-compose ps
	@echo ""
	@echo "$(BLUE)🌐 URLs de los servicios:$(NC)"
	@echo "$(YELLOW)🛍️  Productos API:$(NC) http://localhost:8081/swagger-ui.html"
	@echo "$(YELLOW)📦 Inventario API:$(NC) http://localhost:8082/swagger-ui.html"
	@echo "$(YELLOW)🏥 Health Productos:$(NC) http://localhost:8081/actuator/health"
	@echo "$(YELLOW)🏥 Health Inventario:$(NC) http://localhost:8082/actuator/health"

load-data: ## 📊 Cargar datos de prueba
	@echo "$(BLUE)📊 Cargando datos de prueba...$(NC)"
	@./scripts/load-data.sh || echo "$(RED)❌ Error cargando datos - ¿están los servicios funcionando?$(NC)"


quick-diag: ## 🔍 Diagnóstico rápido de conectividad
	@echo "$(BLUE)🔍 Ejecutando diagnóstico rápido...$(NC)"
	@chmod +x scripts/quick-diagnosis.sh 2>/dev/null || true
	@./scripts/quick-diagnosis.sh || echo "$(RED)❌ Error ejecutando diagnóstico - ¿está el script ejecutable?$(NC)"

simple-test: ## 🧪 Test simple de conectividad
	@echo "$(BLUE)🧪 Ejecutando test simple...$(NC)"
	@chmod +x scripts/simple-test.sh 2>/dev/null || true
	@./scripts/simple-test.sh || echo "$(RED)❌ Error ejecutando test$(NC)"

full-setup: ## 🚀 Setup completo: build + run + test + load-data
	@echo "$(BLUE)🚀 Ejecutando setup completo...$(NC)"
	@echo "$(YELLOW)Paso 1: Construyendo servicios...$(NC)"
	@$(MAKE) build
	@echo "$(YELLOW)Paso 2: Iniciando servicios...$(NC)"
	@$(MAKE) run
	@echo "$(YELLOW)Paso 3: Esperando 30s para que inicien...$(NC)"
	@sleep 30
	@echo "$(YELLOW)Paso 4: Ejecutando test simple...$(NC)"
	@$(MAKE) simple-test
	@echo "$(YELLOW)Paso 5: Cargando datos de prueba...$(NC)"
	@$(MAKE) load-data
	@echo "$(GREEN)✅ Setup completo terminado${NC}"

troubleshoot: ## 🔧 Secuencia de troubleshooting
	@echo "$(BLUE)🔧 Ejecutando troubleshooting...$(NC)"
	@echo "$(YELLOW)1. Estado actual:$(NC)"
	@$(MAKE) status
	@echo "$(YELLOW)2. Diagnóstico rápido:$(NC)"
	@$(MAKE) quick-diag
	@echo "$(YELLOW)3. Intentando fix automático:$(NC)"
	@$(MAKE) fix-db
	@echo "$(YELLOW)4. Test final:$(NC)"
	@$(MAKE) simple-test


fix-db: ## 🔧 Solución rápida para problemas de BD
	@echo "$(BLUE)🔧 Aplicando solución rápida...$(NC)"
	@echo "$(YELLOW)⏹️ Deteniendo servicios...$(NC)"
	@docker-compose down -v
	@echo "$(YELLOW)🧹 Limpiando volúmenes...$(NC)"
	@docker volume prune -f
	@echo "$(YELLOW)🔄 Reiniciando servicios...$(NC)"
	@$(MAKE) run

backup-db: ## 💾 Backup de las bases de datos
	@echo "$(BLUE)💾 Creando backup de las bases de datos...$(NC)"
	@mkdir -p backups
	docker-compose exec -T productos-db pg_dump -U admin productos_db > backups/productos_$(shell date +%Y%m%d_%H%M%S).sql
	docker-compose exec -T inventario-db pg_dump -U admin inventario_db > backups/inventario_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✅ Backup completado en directorio backups/$(NC)"

# Comando por defecto
.DEFAULT_GOAL := help