#!/bin/bash

echo "🚀 Cargando datos de prueba..."

# Configuración
API_KEY="secret-key"
PRODUCTOS_URL="http://localhost:8081"
INVENTARIO_URL="http://localhost:8082"

echo "📦 PASO 1: Creando productos..."

# Crear productos uno por uno
echo "Creando Laptop..."
laptop=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"data":{"nombre":"Laptop Gaming","precio":1500.00}}' \
  $PRODUCTOS_URL/api/v1/productos)
laptop_id=$(echo $laptop | grep -o '"id":[0-9]*' | cut -d: -f2)
echo "✅ Laptop creada - ID: $laptop_id"

echo "Creando Mouse..."
mouse=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"data":{"nombre":"Mouse Gaming","precio":50.00}}' \
  $PRODUCTOS_URL/api/v1/productos)
mouse_id=$(echo $mouse | grep -o '"id":[0-9]*' | cut -d: -f2)
echo "✅ Mouse creado - ID: $mouse_id"

echo "Creando Teclado..."
teclado=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"data":{"nombre":"Teclado Mecánico","precio":100.00}}' \
  $PRODUCTOS_URL/api/v1/productos)
teclado_id=$(echo $teclado | grep -o '"id":[0-9]*' | cut -d: -f2)
echo "✅ Teclado creado - ID: $teclado_id"

echo ""
echo "📋 PASO 2: Creando inventarios..."

# Crear inventarios
echo "Inventario para Laptop ($laptop_id)..."
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"data":{"cantidad":10,"cantidadMinima":5,"cantidadMaxima":50}}' \
  $INVENTARIO_URL/api/v1/inventarios/productos/$laptop_id > /dev/null
echo "✅ Inventario Laptop creado"

echo "Inventario para Mouse ($mouse_id)..."
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"data":{"cantidad":50,"cantidadMinima":10,"cantidadMaxima":200}}' \
  $INVENTARIO_URL/api/v1/inventarios/productos/$mouse_id > /dev/null
echo "✅ Inventario Mouse creado"

echo "Inventario para Teclado ($teclado_id)..."
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"data":{"cantidad":25,"cantidadMinima":8,"cantidadMaxima":100}}' \
  $INVENTARIO_URL/api/v1/inventarios/productos/$teclado_id > /dev/null
echo "✅ Inventario Teclado creado"

echo ""
echo "🎉 ¡COMPLETADO!"
echo "🌐 Ver en: http://localhost:8081/swagger-ui.html"
echo "📦 Inventarios: http://localhost:8082/swagger-ui.html"
