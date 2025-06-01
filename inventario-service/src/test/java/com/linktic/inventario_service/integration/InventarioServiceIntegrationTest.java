package com.linktic.inventario_service.integration;

import com.linktic.inventario_service.client.ProductoClient;
import com.linktic.inventario_service.dto.*;
import com.linktic.inventario_service.entity.Inventario;
import com.linktic.inventario_service.exception.ResourceNotFoundException;
import com.linktic.inventario_service.repository.InventarioRepository;
import com.linktic.inventario_service.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InventarioServiceIntegrationTest {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private InventarioRepository inventarioRepository;

    @MockBean
    private ProductoClient productoClient;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private ProductoDTO productoMock;

    @BeforeEach
    void setUp() {
        inventarioRepository.deleteAll();

        productoMock = ProductoDTO.builder()
                .type("productos")
                .id(1L)
                .attributes(ProductoDTO.Attributes.builder()
                        .nombre("Mouse Logitech")
                        .precio(new BigDecimal("25.99"))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .build();

        when(productoClient.obtenerProducto(anyLong())).thenReturn(productoMock);
    }

    @Test
    void crearInventario_SinCantidadMinima_DebeUsarValorPorDefecto() {
        // Given
        InventarioRequest request = InventarioRequest.builder()
                .cantidad(100)
                .build();

        // When
        InventarioResponse response = inventarioService.crearInventario(1L, request);

        // Then
        assertThat(response.getAttributes().getCantidadMinima()).isEqualTo(10);
        assertThat(response.getAttributes().getCantidadMaxima()).isEqualTo(1000);
    }

    @Test
    void crearInventario_ProductoConInventarioExistente_DebeLanzarExcepcion() {
        // Given
        Inventario existente = Inventario.builder()
                .productoId(1L)
                .cantidad(50)
                .build();
        inventarioRepository.save(existente);

        InventarioRequest request = InventarioRequest.builder()
                .cantidad(100)
                .build();

        // When & Then
        assertThatThrownBy(() -> inventarioService.crearInventario(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe inventario para productoId=1");
    }

    @Test
    void consultarInventario_Existente_DebeRetornarConRelaciones() {
        // Given
        Inventario inventario = Inventario.builder()
                .productoId(1L)
                .cantidad(75)
                .cantidadMinima(10)
                .cantidadMaxima(500)
                .build();
        inventarioRepository.save(inventario);

        // When
        InventarioResponse response = inventarioService.consultarInventario(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAttributes().getCantidad()).isEqualTo(75);
        assertThat(response.getRelationships().getProducto().getData().getId()).isEqualTo(1L);
        assertThat(response.getRelationships().getProducto().getData().getPrecio())
                .isEqualTo(new BigDecimal("25.99"));
    }

    @Test
    void consultarInventario_NoExistente_DebeLanzarExcepcion() {
        // When & Then
        assertThatThrownBy(() -> inventarioService.consultarInventario(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventario no encontrado para productoId=999");
    }

    @Test
    void procesarCompra_StockInsuficiente_DebeLanzarExcepcion() {
        // Given
        Inventario inventario = Inventario.builder()
                .productoId(1L)
                .cantidad(10)
                .cantidadMinima(5)
                .cantidadMaxima(100)
                .build();
        inventarioRepository.save(inventario);

        CompraRequest compra = CompraRequest.builder()
                .cantidad(15)
                .build();

        // When & Then
        assertThatThrownBy(() -> inventarioService.procesarCompra(1L, compra))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }


    @Test
    void listarInventariosConStockBajo_DebeFiltrarCorrectamente() {
        // Given
        // Inventario con stock normal
        Inventario inv1 = Inventario.builder()
                .productoId(1L)
                .cantidad(50)
                .cantidadMinima(10)
                .cantidadMaxima(200)
                .build();
        inventarioRepository.save(inv1);

        // Inventarios con stock bajo
        Inventario inv2 = Inventario.builder()
                .productoId(2L)
                .cantidad(5)
                .cantidadMinima(10)
                .cantidadMaxima(200)
                .build();
        inventarioRepository.save(inv2);

        Inventario inv3 = Inventario.builder()
                .productoId(3L)
                .cantidad(10) // Igual al mínimo
                .cantidadMinima(10)
                .cantidadMaxima(200)
                .build();
        inventarioRepository.save(inv3);

        // When
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<InventarioResponse> page = inventarioService.listarInventariosConStockBajo(pageRequest);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .allMatch(inv -> inv.getAttributes().getStockBajo());
    }
}
