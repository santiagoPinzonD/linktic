#!/bin/bash
# scripts/run-tests.sh
# Script para ejecutar todas las pruebas con reporte de cobertura

echo "🧪 Ejecutando pruebas..."

# Ejecutar pruebas de Productos Service
echo "Testing Productos Service..."
cd productos-service
mvn test jacoco:report
PRODUCTOS_RESULT=$?
cd ..

# Ejecutar pruebas de Inventario Service
echo "Testing Inventario Service..."
cd inventario-service
mvn test jacoco:report
INVENTARIO_RESULT=$?
cd ..

# Verificar resultados
if [ $PRODUCTOS_RESULT -eq 0 ] && [ $INVENTARIO_RESULT -eq 0 ]; then
    echo "✅ Todas las pruebas pasaron exitosamente"
    echo "📊 Reportes de cobertura disponibles en:"
    echo "   - productos-service/target/site/jacoco/index.html"
    echo "   - inventario-service/target/site/jacoco/index.html"
else
    echo "Algunas pruebas fallaron"
    exit 1
fi