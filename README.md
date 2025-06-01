# 🛍️ Microservicios: Productos e Inventario

## 📋 Descripción

Solución compuesta por dos microservicios independientes que interactúan entre sí utilizando JSON API como estándar para la comunicación. Desarrollado como prueba técnica para demostrar habilidades en desarrollo backend con Spring Boot.

### 🎯 Objetivos Cumplidos

- ✅ **Microservicio de Productos**: CRUD completo con paginación
- ✅ **Microservicio de Inventario**: Gestión de stock con integración
- ✅ **JSON API**: Estándar implementado en todas las respuestas
- ✅ **Docker**: Servicios completamente containerizados
- ✅ **Autenticación**: API Keys entre servicios
- ✅ **Tolerancia a fallos**: Resilience4j (Circuit Breaker, Retry)
- ✅ **Pruebas**: Unitarias e integración con alta cobertura
- ✅ **Documentación**: Swagger completa

## 🏗️ Arquitectura

```
┌─────────────────┐    HTTP/JSON API   ┌─────────────────┐
│                 │◄──────────────────►│                 │
│ Inventario      │                    │ Productos       │
│ Service         │                    │ Service         │
│ (Puerto 8082)   │                    │ (Puerto 8081)   │
│                 │                    │                 │
└─────────┬───────┘                    └─────────┬───────┘
          │                                      │
          ▼                                      ▼
┌─────────────────┐                    ┌─────────────────┐
│ PostgreSQL      │                    │ PostgreSQL      │
│ inventario_db   │                    │ productos_db    │
└─────────────────┘                    └─────────────────┘
```

### 🔄 Flujo de Comunicación

1. **Inventario Service** consulta productos via HTTP → **Productos Service**
2. **Circuit Breaker** protege contra fallos del servicio externo
3. **Retry** maneja reintentos automáticos
4. **API Keys** aseguran la comunicación entre servicios

## 🚀 Inicio Rápido

### Prerrequisitos

- Docker & Docker Compose
- Java 17+ (para desarrollo local)
- Maven 3.8+ (para desarrollo local)

### 1️⃣ Clonar y Configurar

```bash
git clone <repository-url>
cd linktic

# Configurar proyecto
make setup
```

### 2️⃣ Iniciar Servicios

```bash
# Opción 1: Usar Makefile (recomendado)
make run

# Opción 2: Docker Compose directo
docker-compose up -d
```

### 3️⃣ Verificar Salud

```bash
# Verificar que todo funcione
make health-check

# Ver estado de servicios
make status
```

### 4️⃣ Cargar Datos de Prueba

```bash
make load-data
```

## 🌐 URLs de Acceso

| Servicio | URL | Documentación |
|----------|-----|---------------|
| **Productos** | http://localhost:8081 | http://localhost:8081/swagger-ui/index.html |
| **Inventario** | http://localhost:8082 | http://localhost:8082/swagger-ui/index.html |
| **Health Productos** | http://localhost:8081/actuator/health | - |
| **Health Inventario** | http://localhost:8082/actuator/health | - |

## 📡 API Reference

### 🛍️ Productos Service

```bash
# API Key: secret-key

# Crear producto
POST /api/v1/productos
Content-Type: application/json
X-API-Key: secret-key

{
  "data": {
    "nombre": "Laptop Gaming",
    "precio": 1299.99
  }
}

# Obtener producto
GET /api/v1/productos/{id}
X-API-Key: secret-key

# Listar productos
GET /api/v1/productos?page=0&size=10
X-API-Key: secret-key
```

### 📦 Inventario Service

```bash
# API Key: secret-key

# Crear inventario
POST /api/v1/inventarios/productos/{productoId}
Content-Type: application/json
X-API-Key: secret-key

{
  "data": {
    "cantidad": 50,
    "cantidadMinima": 10,
    "cantidadMaxima": 200
  }
}

# Consultar inventario
GET /api/v1/inventarios/productos/{productoId}
X-API-Key: secret-key

# Procesar compra
PATCH /api/v1/inventarios/productos/{productoId}/compra
Content-Type: application/json
X-API-Key: secret-key

{
  "data": {
    "cantidad": 5
  }
}

# Ver stock bajo
GET /api/v1/inventarios/stock-bajo
X-API-Key: secret-key
```

## 🛠️ Comandos de Desarrollo

```bash
# Mostrar ayuda
make help

# Desarrollo
make test             # Ejecutar pruebas

# Docker
make build            # Construir imágenes
make run              # Iniciar servicios
make stop             # Detener servicios
make restart          # Reiniciar servicios
make clean            # Limpiar recursos


# Utilidades
make load-data        # Cargar datos de prueba
make health-check     # Verificar salud
make status           # Ver estado de contenedores

```

## 📊 Monitoreo y Observabilidad

### Health Checks

Los servicios exponen endpoints de salud:

- **Productos**: `GET /actuator/health`
- **Inventario**: `GET /actuator/health`

### Métricas

Métricas disponibles en `/actuator/metrics`:

- JVM metrics
- HTTP request metrics
- Database connection pool metrics
- Resilience4j circuit breaker metrics


## 🧪 Testing

### Pruebas Unitarias

```bash
# Ejecutar todas las pruebas
make test

# Con reporte de cobertura
make test-coverage

# Por servicio
cd productos-service && mvn test
cd inventario-service && mvn test
```


### Pruebas de Integración

Las pruebas incluyen:
- ✅ Testcontainers para PostgreSQL
- ✅ WireMock para servicios externos
- ✅ Rest Assured para APIs
- ✅ Validación de circuit breakers

## 🔧 Configuración

### Variables de Entorno

Crear archivo `.env`:

```bash
# API Keys
PRODUCTOS_API_KEY=secret-key
INVENTARIO_API_KEY=secret-key

# Database
DB_PASSWORD=admin123

# JVM
JAVA_OPTS=-Xmx512m -Xms256m
```

### Perfiles de Spring

- **default**: Desarrollo local
- **docker**: Contenedores Docker
- **test**: Pruebas automáticas

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      productos-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
```

## 📁 Estructura del Proyecto

```
microservicios/
├── productos-service/           # Microservicio de productos
│   ├── src/main/java/          # Código fuente
│   ├── src/test/java/          # Pruebas
│   ├── Dockerfile              # Imagen Docker
│   └── pom.xml                 # Dependencias Maven
├── inventario-service/         # Microservicio de inventario
│   ├── src/main/java/          # Código fuente
│   ├── src/test/java/          # Pruebas
│   ├── Dockerfile              # Imagen Docker
│   └── pom.xml                 # Dependencias Maven
├── scripts/                    # Scripts de utilidad
│   ├── health-check.sh         # Verificación de salud
│   └── load-test-data.sh       # Datos de prueba
├── monitoring/                 # Configuración de monitoreo
├── docker-compose.yml          # Orquestación de servicios
├── Makefile                    # Comandos de desarrollo
└── README.md                   # Esta documentación
```

## 🔐 Seguridad

### Autenticación

- **API Keys** para comunicación entre servicios
- **Headers**: `X-API-Key: <secret-key>`
- **Usuarios no-root** en contenedores Docker

### Validación

- Bean Validation (`@Valid`, `@NotNull`, etc.)
- Validación de rangos y formatos
- Manejo global de excepciones

## 🚨 Tolerancia a Fallos

### Circuit Breaker

- **Estado Cerrado**: Operaciones normales
- **Estado Abierto**: Fallo rápido cuando hay problemas
- **Estado Semi-abierto**: Prueba gradual de recuperación

### Retry

- **Reintentos automáticos** en fallos transitorios
- **Backoff exponencial** para evitar sobrecarga
- **Timeout** configurable por operación

### Timeouts

- **Connection timeout**: 5s
- **Read timeout**: 5s
- **Circuit breaker timeout**: 10s

## 🐛 Troubleshooting

### Problemas Comunes

#### 1. Servicios no inician

```bash
# Verificar logs
# Reiniciar
make restart
```

#### 2. Error de conexión entre servicios

```bash
# Verificar network
docker network ls

# Verificar health checks
make health-check

# Verificar API keys
docker-compose logs inventario-service | grep "API"
```


#### 3. Puertos ocupados

```bash
# Cambiar puertos en docker-compose.yml
ports:
  - "8083:8081"  # En lugar de 8081:8081
```

