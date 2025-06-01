package com.linktic.inventario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.inventario_service.dto.CompraRequest;
import com.linktic.inventario_service.dto.InventarioRequest;
import com.linktic.inventario_service.dto.InventarioResponse;
import com.linktic.inventario_service.dto.JsonApiDocument;
import com.linktic.inventario_service.exception.ResourceNotFoundException;
import com.linktic.inventario_service.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventarioController.class)
@Import(com.linktic.inventario_service.config.SecurityConfig.class)
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventarioService inventarioService;

    private InventarioResponse sampleResponse;

    @BeforeEach
    void setUp() {
        InventarioResponse.Relationships.ProductoData productoData =
                InventarioResponse.Relationships.ProductoData.builder()
                        .type("productos")
                        .id(1L)
                        .nombre("Laptop Dell XPS")
                        .precio(new BigDecimal("1500.00"))
                        .build();

        InventarioResponse.Relationships.Producto relProducto =
                InventarioResponse.Relationships.Producto.builder()
                        .data(productoData)
                        .build();

        InventarioResponse.Relationships relationships =
                InventarioResponse.Relationships.builder()
                        .producto(relProducto)
                        .build();

        InventarioResponse.Attributes attributes =
                InventarioResponse.Attributes.builder()
                        .cantidad(100)
                        .cantidadMinima(10)
                        .cantidadMaxima(200)
                        .stockBajo(false)
                        .ultimaActualizacion(LocalDateTime.now())
                        .build();

        sampleResponse = InventarioResponse.builder()
                .type("inventarios")
                .id(1L)
                .attributes(attributes)
                .relationships(relationships)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/inventarios/productos/{id} - Crear inventario => 201")
    @WithMockUser
    void crearInventario_DeberiaRetornar201() throws Exception {
        InventarioRequest requestData = InventarioRequest.builder()
                .cantidad(100)
                .cantidadMinima(10)
                .cantidadMaxima(200)
                .build();

        JsonApiDocument<InventarioRequest> jsonApiRequest = JsonApiDocument.<InventarioRequest>builder()
                .data(requestData)
                .build();

        when(inventarioService.crearInventario(eq(1L), any(InventarioRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/inventarios/productos/{productoId}", 1L)
                        .header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jsonApiRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("inventarios"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.attributes.cantidad").value(100))
                .andExpect(jsonPath("$.data.relationships.producto.data.id").value(1))
                .andExpect(jsonPath("$.data.relationships.producto.data.nombre").value("Laptop Dell XPS"))
                .andExpect(jsonPath("$.links.self").value("/api/v1/inventarios/productos/1"));

        verify(inventarioService).crearInventario(eq(1L), any(InventarioRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/inventarios/productos/{id} - Consultar inventario => 200")
    @WithMockUser
    void consultarInventario_DeberiaRetornar200() throws Exception {
        when(inventarioService.consultarInventario(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/inventarios/productos/{productoId}", 1L)
                        .header("X-API-Key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("inventarios"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.attributes.cantidad").value(100))
                .andExpect(jsonPath("$.data.relationships.producto.data.id").value(1))
                .andExpect(jsonPath("$.links.self").value("/api/v1/inventarios/productos/1"))
                .andExpect(jsonPath("$.links.related").value("/api/v1/productos/1"));

        verify(inventarioService).consultarInventario(1L);
    }

    @Test
    @DisplayName("GET /api/v1/inventarios/productos/{id} - Inventario no encontrado => 404")
    @WithMockUser
    void consultarInventarioNoExistente_DeberiaRetornar404() throws Exception {
        when(inventarioService.consultarInventario(999L))
                .thenThrow(new ResourceNotFoundException("Inventario no encontrado para productoId=999"));

        mockMvc.perform(get("/api/v1/inventarios/productos/{productoId}", 999L)
                        .header("X-API-Key", "test-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Inventario no encontrado para productoId=999"));

        verify(inventarioService).consultarInventario(999L);
    }

    @Test
    @DisplayName("PATCH /api/v1/inventarios/productos/{id}/compra - Procesar compra => 200")
    @WithMockUser
    void procesarCompra_DeberiaRetornar200() throws Exception {
        CompraRequest compraReq = CompraRequest.builder()
                .cantidad(10)
                .build();
        JsonApiDocument<CompraRequest> jsonApiRequest = JsonApiDocument.<CompraRequest>builder()
                .data(compraReq)
                .build();

        InventarioResponse afterPurchase = InventarioResponse.builder()
                .type("inventarios")
                .id(1L)
                .attributes(InventarioResponse.Attributes.builder()
                        .cantidad(90)
                        .cantidadMinima(10)
                        .cantidadMaxima(200)
                        .stockBajo(false)
                        .ultimaActualizacion(LocalDateTime.now())
                        .build())
                .relationships(sampleResponse.getRelationships())
                .build();

        when(inventarioService.procesarCompra(eq(1L), any(CompraRequest.class)))
                .thenReturn(afterPurchase);

        mockMvc.perform(patch("/api/v1/inventarios/productos/{productoId}/compra", 1L)
                        .header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jsonApiRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.cantidad").value(90))
                .andExpect(jsonPath("$.meta.operacion").value("compra"))
                .andExpect(jsonPath("$.meta.cantidad_comprada").value(10));

        verify(inventarioService).procesarCompra(eq(1L), any(CompraRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/inventarios/productos/{id}/compra - Stock insuficiente => 400")
    @WithMockUser
    void procesarCompra_StockInsuficiente_DeberiaRetornar400() throws Exception {
        CompraRequest compraReq = CompraRequest.builder()
                .cantidad(1000)
                .build();
        JsonApiDocument<CompraRequest> jsonApiRequest = JsonApiDocument.<CompraRequest>builder()
                .data(compraReq)
                .build();

        when(inventarioService.procesarCompra(eq(1L), any(CompraRequest.class)))
                .thenThrow(new IllegalArgumentException("Stock insuficiente para productoId=1"));

        mockMvc.perform(patch("/api/v1/inventarios/productos/{productoId}/compra", 1L)
                        .header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jsonApiRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Stock insuficiente para productoId=1"));

        verify(inventarioService).procesarCompra(eq(1L), any(CompraRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/inventarios/productos/{id} - Actualizar inventario => 200")
    @WithMockUser
    void actualizarInventario_DeberiaRetornar200() throws Exception {
        InventarioRequest updReq = InventarioRequest.builder()
                .cantidad(200)
                .cantidadMinima(10)
                .cantidadMaxima(300)
                .build();
        JsonApiDocument<InventarioRequest> jsonApiRequest = JsonApiDocument.<InventarioRequest>builder()
                .data(updReq)
                .build();

        when(inventarioService.actualizarInventario(eq(1L), any(InventarioRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/inventarios/productos/{productoId}", 1L)
                        .header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jsonApiRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("inventarios"))
                .andExpect(jsonPath("$.links.self").value("/api/v1/inventarios/productos/1"));

        verify(inventarioService).actualizarInventario(eq(1L), any(InventarioRequest.class));
    }

    @Test
    @DisplayName("Request sin API Key debe retornar 403")
    void requestSinApiKey_DeberiaRetornar403() throws Exception {
        mockMvc.perform(get("/api/v1/inventarios/productos/{productoId}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Validación - Cantidad negativa en compra => 500")
    @WithMockUser
    void procesarCompra_CantidadNegativa_DeberiaRetornar500() throws Exception {
        CompraRequest compraReq = CompraRequest.builder()
                .cantidad(-5)
                .build();
        JsonApiDocument<CompraRequest> jsonApiRequest = JsonApiDocument.<CompraRequest>builder()
                .data(compraReq)
                .build();

        mockMvc.perform(patch("/api/v1/inventarios/productos/{productoId}/compra", 1L)
                        .header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jsonApiRequest)))
                .andExpect(status().isInternalServerError());
    }
}
