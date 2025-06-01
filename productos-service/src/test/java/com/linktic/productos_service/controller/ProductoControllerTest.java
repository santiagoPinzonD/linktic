package com.linktic.productos_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.productos_service.dto.JsonApiDocument;
import com.linktic.productos_service.dto.ProductoRequest;
import com.linktic.productos_service.dto.ProductoResponse;
import com.linktic.productos_service.exception.ProductoNotFoundException;
import com.linktic.productos_service.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
@Import(com.linktic.productos_service.config.SecurityConfig.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductoService productoService;

    private ProductoResponse productoResponse;

    @BeforeEach
    void setUp() {
        productoResponse = ProductoResponse.builder()
                .type("productos")
                .id(1L)
                .attributes(ProductoResponse.Attributes.builder()
                        .nombre("Laptop Dell XPS")
                        .precio(new BigDecimal("1500.00"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/productos - Crear producto")
    @WithMockUser
    void crearProducto_DeberiaRetornar201() throws Exception {
        // Given
        ProductoRequest request = ProductoRequest.builder()
                .nombre("Laptop Dell XPS")
                .precio(new BigDecimal("1500.00"))
                .build();

        JsonApiDocument<ProductoRequest> jsonApiRequest = JsonApiDocument.<ProductoRequest>builder()
                .data(request)
                .build();

        when(productoService.crear(any(ProductoRequest.class))).thenReturn(productoResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/productos")
                        .header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jsonApiRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("productos"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.attributes.nombre").value("Laptop Dell XPS"))
                .andExpect(jsonPath("$.data.attributes.precio").value(1500.00));

        verify(productoService).crear(any(ProductoRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} - Obtener producto")
    @WithMockUser
    void obtenerProducto_DeberiaRetornar200() throws Exception {
        // Given
        when(productoService.obtenerPorId(1L)).thenReturn(productoResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/productos/1")
                        .header("X-API-Key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("productos"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.attributes.nombre").value("Laptop Dell XPS"))
                .andExpect(jsonPath("$.links.self").value("/api/v1/productos/1"));
    }

    @Test
    @DisplayName("GET /api/v1/productos/{id} - Producto no encontrado")
    @WithMockUser
    void obtenerProductoNoExistente_DeberiaRetornar404() throws Exception {
        // Given
        when(productoService.obtenerPorId(999L))
                .thenThrow(new ProductoNotFoundException("Producto no encontrado con ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/productos/999")
                        .header("X-API-Key", "test-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].title").value("Producto no encontrado"));
    }

    @Test
    @DisplayName("DELETE /api/v1/productos/{id} - Eliminar producto")
    @WithMockUser
    void eliminarProducto_DeberiaRetornar204() throws Exception {
        // Given
        doNothing().when(productoService).eliminar(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/productos/1")
                        .header("X-API-Key", "test-key"))
                .andExpect(status().isNoContent());

        verify(productoService).eliminar(1L);
    }

    @Test
    @DisplayName("GET /api/v1/productos - Listar productos con paginación")
    @WithMockUser
    void listarProductos_DeberiaRetornarPagina() throws Exception {
        // Given
        List<ProductoResponse> productos = List.of(productoResponse);
        Page<ProductoResponse> page = new PageImpl<>(productos, PageRequest.of(0, 10), 1);

        when(productoService.listarTodos(any(PageRequest.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/productos")
                        .header("X-API-Key", "test-key")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(1))
                .andExpect(jsonPath("$.links.self").exists());
    }

    @Test
    @DisplayName("Request sin API Key debe retornar 403")
    void requestSinApiKey_DeberiaRetornar403() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isForbidden());
    }
}