package com.linktic.productos_service.service;

import com.linktic.productos_service.dto.ProductoRequest;
import com.linktic.productos_service.dto.ProductoResponse;
import com.linktic.productos_service.exception.DuplicateProductException;
import com.linktic.productos_service.exception.ProductoNotFoundException;
import com.linktic.productos_service.model.Producto;
import com.linktic.productos_service.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private ProductoRequest productoRequest;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRequest = ProductoRequest.builder()
                .nombre("Laptop Dell XPS")
                .precio(new BigDecimal("1500.00"))
                .build();

        producto = Producto.builder()
                .id(1L)
                .nombre("Laptop Dell XPS")
                .precio(new BigDecimal("1500.00"))
                .build();
    }

    @Test
    @DisplayName("Crear producto exitosamente")
    void crearProducto_DeberiaRetornarProductoCreado() {
        // Given
        when(productoRepository.existsByNombre(anyString())).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        // When
        ProductoResponse response = productoService.crear(productoRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAttributes().getNombre()).isEqualTo("Laptop Dell XPS");
        assertThat(response.getAttributes().getPrecio()).isEqualTo(new BigDecimal("1500.00"));

        verify(productoRepository).existsByNombre("Laptop Dell XPS");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Crear producto duplicado debe lanzar excepción")
    void crearProductoDuplicado_DeberiaLanzarExcepcion() {
        // Given
        when(productoRepository.existsByNombre(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productoService.crear(productoRequest))
                .isInstanceOf(DuplicateProductException.class)
                .hasMessageContaining("Ya existe un producto con el nombre");

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener producto por ID exitosamente")
    void obtenerPorId_DeberiaRetornarProducto() {
        // Given
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        // When
        ProductoResponse response = productoService.obtenerPorId(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAttributes().getNombre()).isEqualTo("Laptop Dell XPS");
    }

    @Test
    @DisplayName("Obtener producto no existente debe lanzar excepción")
    void obtenerProductoNoExistente_DeberiaLanzarExcepcion() {
        // Given
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productoService.obtenerPorId(999L))
                .isInstanceOf(ProductoNotFoundException.class)
                .hasMessageContaining("Producto no encontrado con ID: 999");
    }

    @Test
    @DisplayName("Actualizar producto exitosamente")
    void actualizarProducto_DeberiaRetornarProductoActualizado() {
        // Given
        ProductoRequest updateRequest = ProductoRequest.builder()
                .nombre("Laptop Dell XPS Updated")
                .precio(new BigDecimal("1800.00"))
                .build();

        Producto productoActualizado = Producto.builder()
                .id(1L)
                .nombre("Laptop Dell XPS Updated")
                .precio(new BigDecimal("1800.00"))
                .build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(productoActualizado);

        // When
        ProductoResponse response = productoService.actualizar(1L, updateRequest);

        // Then
        assertThat(response.getAttributes().getNombre()).isEqualTo("Laptop Dell XPS Updated");
        assertThat(response.getAttributes().getPrecio()).isEqualTo(new BigDecimal("1800.00"));

        verify(productoRepository).findById(1L);
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Eliminar producto exitosamente")
    void eliminarProducto_DeberiaEliminarCorrectamente() {
        // Given
        when(productoRepository.existsById(1L)).thenReturn(true);

        // When
        productoService.eliminar(1L);

        // Then
        verify(productoRepository).existsById(1L);
        verify(productoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Eliminar producto no existente debe lanzar excepción")
    void eliminarProductoNoExistente_DeberiaLanzarExcepcion() {
        // Given
        when(productoRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> productoService.eliminar(999L))
                .isInstanceOf(ProductoNotFoundException.class)
                .hasMessageContaining("Producto no encontrado con ID: 999");

        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Listar productos con paginación")
    void listarTodos_DeberiaRetornarPaginaDeProductos() {
        // Given
        List<Producto> productos = List.of(producto);
        Page<Producto> page = new PageImpl<>(productos);
        PageRequest pageRequest = PageRequest.of(0, 10);

        when(productoRepository.findAll(pageRequest)).thenReturn(page);

        // When
        Page<ProductoResponse> response = productoService.listarTodos(pageRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
    }
}
