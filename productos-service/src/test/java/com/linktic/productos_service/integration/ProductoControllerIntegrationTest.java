package com.linktic.productos_service.integration;

import com.linktic.productos_service.dto.JsonApiDocument;
import com.linktic.productos_service.dto.ProductoRequest;
import com.linktic.productos_service.model.Producto;
import com.linktic.productos_service.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductoControllerIntegrationTest extends ProductoIntegrationTestBase {

    @Autowired
    private ProductoRepository productoRepository;

    @BeforeEach
    void cleanDatabase() {
        productoRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/productos - Debe crear producto exitosamente")
    void debeCrearProductoExitosamente() throws Exception {
        // Given
        ProductoRequest request = ProductoRequest.builder()
                .nombre("Laptop Gaming")
                .precio(new BigDecimal("1299.99"))
                .build();

        JsonApiDocument<ProductoRequest> jsonApiRequest = JsonApiDocument.<ProductoRequest>builder()
                .data(request)
                .build();

        String requestBody = objectMapper.writeValueAsString(jsonApiRequest);

        // When & Then
        givenAuthenticatedRequest()
                .body(requestBody)
                .when()
                .post("/api/v1/productos")
                .then()
                .statusCode(201)
                .body("data.type", equalTo("productos"))
                .body("data.id", notNullValue())
                .body("data.attributes.nombre", equalTo("Laptop Gaming"))
                .body("data.attributes.precio", equalTo(1299.99f))
                .body("data.attributes.created_at", notNullValue())
                .body("data.attributes.updated_at", notNullValue());

        // Verify in database
        assertEquals(1, productoRepository.count());
    }

    @Test
    @DisplayName("POST /api/v1/productos - Debe fallar sin autenticación")
    void debeRechazarCreacionSinAutenticacion() throws Exception {
        // Given
        ProductoRequest request = ProductoRequest.builder()
                .nombre("Producto Test")
                .precio(new BigDecimal("100.00"))
                .build();

        JsonApiDocument<ProductoRequest> jsonApiRequest = JsonApiDocument.<ProductoRequest>builder()
                .data(request)
                .build();

        String requestBody = objectMapper.writeValueAsString(jsonApiRequest);

        // When & Then
        givenUnauthenticatedRequest()
                .body(requestBody)
                .when()
                .post("/api/v1/productos")
                .then()
                .statusCode(403); // Spring Security retorna 403 por defecto
    }

    @Test
    @DisplayName("POST /api/v1/productos - Debe fallar con validaciones")
    void debeRechazarCreacionConValidacionesIncorrectas() throws Exception {
        // Given
        ProductoRequest request = ProductoRequest.builder()
                .nombre("") // Nombre vacío
                .precio(new BigDecimal("-10.00")) // Precio negativo
                .build();

        JsonApiDocument<ProductoRequest> jsonApiRequest = JsonApiDocument.<ProductoRequest>builder()
                .data(request)
                .build();

        String requestBody = objectMapper.writeValueAsString(jsonApiRequest);

        // When & Then - Primero vamos a ver qué error devuelve
        givenAuthenticatedRequest()
                .body(requestBody)
                .when()
                .post("/api/v1/productos")
                .then()
                .log().all() // Esto imprimirá toda la respuesta para debugging
                .statusCode(400)
                .body("errors", hasSize(greaterThan(0)))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} - Debe obtener producto existente")
    void debeObtenerProductoExistente() {
        // Given
        Producto producto = Producto.builder()
                .nombre("Mouse Inalámbrico")
                .precio(new BigDecimal("45.99"))
                .build();
        Producto saved = productoRepository.save(producto);

        // When & Then
        givenAuthenticatedRequest()
                .when()
                .get("/api/v1/productos/{id}", saved.getId())
                .then()
                .statusCode(200)
                .body("data.type", equalTo("productos"))
                .body("data.id", equalTo(saved.getId().intValue()))
                .body("data.attributes.nombre", equalTo("Mouse Inalámbrico"))
                .body("data.attributes.precio", equalTo(45.99f))
                .body("links.self", equalTo("/api/v1/productos/" + saved.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} - Debe retornar 404 para producto inexistente")
    void debeRetornar404ParaProductoInexistente() {
        // When & Then
        givenAuthenticatedRequest()
                .when()
                .get("/api/v1/productos/{id}", 999L)
                .then()
                .statusCode(404)
                .body("errors[0].status", equalTo("404"))
                .body("errors[0].code", equalTo("PRODUCT_NOT_FOUND"))
                .body("errors[0].detail", containsString("999"));
    }

    @Test
    @DisplayName("PATCH /api/v1/productos/{id} - Debe actualizar producto exitosamente")
    void debeActualizarProductoExitosamente() throws Exception {
        // Given
        Producto producto = Producto.builder()
                .nombre("Teclado Mecánico")
                .precio(new BigDecimal("120.00"))
                .build();
        Producto saved = productoRepository.save(producto);

        ProductoRequest updateRequest = ProductoRequest.builder()
                .nombre("Teclado Mecánico RGB")
                .precio(new BigDecimal("150.00"))
                .build();

        JsonApiDocument<ProductoRequest> jsonApiRequest = JsonApiDocument.<ProductoRequest>builder()
                .data(updateRequest)
                .build();

        String requestBody = objectMapper.writeValueAsString(jsonApiRequest);

        // When & Then
        givenAuthenticatedRequest()
                .body(requestBody)
                .when()
                .patch("/api/v1/productos/{id}", saved.getId())
                .then()
                .statusCode(200)
                .body("data.id", equalTo(saved.getId().intValue()))
                .body("data.attributes.nombre", equalTo("Teclado Mecánico RGB"))
                .body("data.attributes.precio", equalTo(150.00f));
    }

    @Test
    @DisplayName("DELETE /api/v1/productos/{id} - Debe eliminar producto exitosamente")
    void debeEliminarProductoExitosamente() {
        // Given
        Producto producto = Producto.builder()
                .nombre("Webcam HD")
                .precio(new BigDecimal("89.99"))
                .build();
        Producto saved = productoRepository.saveAndFlush(producto); // Fuerza el commit inmediato

        // When & Then
        givenAuthenticatedRequest()
                .when()
                .delete("/api/v1/productos/{id}", saved.getId())
                .then()
                .statusCode(204);

        // Verify deletion
        assertFalse(productoRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/productos - Debe listar productos con paginación")
    void debeListarProductosConPaginacion() {
        // Given - Crear múltiples productos
        for (int i = 1; i <= 15; i++) {
            Producto producto = Producto.builder()
                    .nombre("Producto " + i)
                    .precio(new BigDecimal("10.00"))
                    .build();
            productoRepository.save(producto);
        }

        // When & Then - Primera página
        givenAuthenticatedRequest()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/productos")
                .then()
                .statusCode(200)
                .body("data.content", hasSize(10))
                .body("meta.totalElements", equalTo(15))
                .body("meta.totalPages", equalTo(2))
                .body("meta.currentPage", equalTo(0))
                .body("links.self", containsString("page=0&size=10"))
                .body("links.next", containsString("page=1&size=10"));

        // When & Then - Segunda página
        givenAuthenticatedRequest()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/productos")
                .then()
                .statusCode(200)
                .body("data.content", hasSize(5))
                .body("links.prev", containsString("page=0&size=10"));
    }

    @Test
    @DisplayName("POST /api/v1/productos - Debe rechazar producto duplicado")
    void debeRechazarProductoDuplicado() throws Exception {
        // Given - Crear producto inicial
        Producto producto = Producto.builder()
                .nombre("Monitor 4K")
                .precio(new BigDecimal("599.99"))
                .build();
        productoRepository.save(producto);

        // Given - Intentar crear producto con mismo nombre
        ProductoRequest duplicateRequest = ProductoRequest.builder()
                .nombre("Monitor 4K")
                .precio(new BigDecimal("650.00"))
                .build();

        JsonApiDocument<ProductoRequest> jsonApiRequest = JsonApiDocument.<ProductoRequest>builder()
                .data(duplicateRequest)
                .build();

        String requestBody = objectMapper.writeValueAsString(jsonApiRequest);

        // When & Then
        givenAuthenticatedRequest()
                .body(requestBody)
                .when()
                .post("/api/v1/productos")
                .then()
                .statusCode(409)
                .body("errors[0].status", equalTo("409"))
                .body("errors[0].code", equalTo("DUPLICATE_PRODUCT"))
                .body("errors[0].detail", containsString("Monitor 4K"));
    }

    @Test
    @DisplayName("Endpoints públicos - Health check debe ser accesible sin autenticación")
    void healthCheckDebeSerAccesibleSinAutenticacion() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Swagger UI debe ser accesible sin autenticación")
    void swaggerUIDebeSerAccesibleSinAutenticacion() {
        // Probar la URL principal de Swagger UI
        given()
                .when()
                .get("/swagger-ui/index.html")
                .then()
                .statusCode(200);
    }
}