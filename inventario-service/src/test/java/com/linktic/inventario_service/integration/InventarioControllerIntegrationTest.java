package com.linktic.inventario_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.inventario_service.client.ProductoClient;
import com.linktic.inventario_service.dto.*;
import com.linktic.inventario_service.entity.Inventario;
import com.linktic.inventario_service.repository.InventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InventarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventarioRepository inventarioRepository;

    @MockBean
    private ProductoClient productoClient;

    private final String API_KEY = "secret-key";

    @BeforeEach
    void setUp() {
        // Limpiar datos previos
        inventarioRepository.deleteAll();

        // Crear producto mock
        ProductoDTO productoMock = ProductoDTO.builder()
                .type("productos")
                .id(1L)
                .attributes(ProductoDTO.Attributes.builder()
                        .nombre("Laptop Dell")
                        .precio(new BigDecimal("1200.50"))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .build();

        when(productoClient.obtenerProducto(anyLong())).thenReturn(productoMock);
    }

    @Test
    void crearInventario_SinApiKey_DebeRetornar403() throws Exception {
        // Given
        InventarioRequest request = InventarioRequest.builder()
                .cantidad(100)
                .build();

        JsonApiDocument<InventarioRequest> document = JsonApiDocument.<InventarioRequest>builder()
                .data(request)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/inventarios/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearInventario_InventarioDuplicado_DebeRetornar400() throws Exception {
        // Given - Inventario existente
        Inventario existente = Inventario.builder()
                .productoId(1L)
                .cantidad(50)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(existente);

        InventarioRequest request = InventarioRequest.builder()
                .cantidad(100)
                .build();

        JsonApiDocument<InventarioRequest> document = JsonApiDocument.<InventarioRequest>builder()
                .data(request)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/inventarios/productos/1")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarInventario_NoExistente_DebeRetornar404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/inventarios/productos/999")
                        .header("X-API-Key", API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void procesarCompra_StockSuficiente_DebeActualizarInventario() throws Exception {
        // Given
        Inventario inventario = Inventario.builder()
                .productoId(1L)
                .cantidad(100)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inventario);

        CompraRequest compra = CompraRequest.builder()
                .cantidad(30)
                .build();

        JsonApiDocument<CompraRequest> document = JsonApiDocument.<CompraRequest>builder()
                .data(compra)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/inventarios/productos/1/compra")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.cantidad").value(70))
                .andExpect(jsonPath("$.data.attributes.stock_bajo").value(false))
                .andExpect(jsonPath("$.meta.operacion").value("compra"))
                .andExpect(jsonPath("$.meta.cantidad_comprada").value(30));
    }

    @Test
    void procesarCompra_StockInsuficiente_DebeRetornar400() throws Exception {
        // Given
        Inventario inventario = Inventario.builder()
                .productoId(1L)
                .cantidad(20)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inventario);

        CompraRequest compra = CompraRequest.builder()
                .cantidad(30)
                .build();

        JsonApiDocument<CompraRequest> document = JsonApiDocument.<CompraRequest>builder()
                .data(compra)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/inventarios/productos/1/compra")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void procesarCompra_ResultaEnStockBajo_DebeIncluirAlerta() throws Exception {
        // Given
        Inventario inventario = Inventario.builder()
                .productoId(1L)
                .cantidad(15)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inventario);

        CompraRequest compra = CompraRequest.builder()
                .cantidad(6)
                .build();

        JsonApiDocument<CompraRequest> document = JsonApiDocument.<CompraRequest>builder()
                .data(compra)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/inventarios/productos/1/compra")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.cantidad").value(9))
                .andExpect(jsonPath("$.data.attributes.stock_bajo").value(true))
                .andExpect(jsonPath("$.meta.alerta").value("Stock bajo - Se recomienda reabastecer"));
    }

    @Test
    void actualizarInventario_ConDatosValidos_DebeActualizar() throws Exception {
        // Given
        Inventario inventario = Inventario.builder()
                .productoId(1L)
                .cantidad(50)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inventario);

        InventarioRequest update = InventarioRequest.builder()
                .cantidad(200)
                .cantidadMinima(20)
                .cantidadMaxima(1000)
                .build();

        JsonApiDocument<InventarioRequest> document = JsonApiDocument.<InventarioRequest>builder()
                .data(update)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/inventarios/productos/1")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.cantidad").value(200))
                .andExpect(jsonPath("$.data.attributes.cantidad_minima").value(20))
                .andExpect(jsonPath("$.data.attributes.cantidad_maxima").value(1000));
    }

    @Test
    void listarInventariosStockBajo_DebeRetornarSoloStockBajo() throws Exception {
        // Given
        // Inventarios con stock normal
        Inventario inv1 = Inventario.builder()
                .productoId(1L)
                .cantidad(100)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inv1);

        // Inventarios con stock bajo
        Inventario inv2 = Inventario.builder()
                .productoId(2L)
                .cantidad(5)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inv2);

        Inventario inv3 = Inventario.builder()
                .productoId(3L)
                .cantidad(8)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inv3);

        // When & Then
        mockMvc.perform(get("/api/v1/inventarios/stock-bajo")
                        .header("X-API-Key", API_KEY)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].attributes.stock_bajo", everyItem(is(true))))
                .andExpect(jsonPath("$.meta.filtro").value("stock_bajo"));
    }
}