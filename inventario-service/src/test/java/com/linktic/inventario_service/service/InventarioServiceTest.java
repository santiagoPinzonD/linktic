package com.linktic.inventario_service.service;

import com.linktic.inventario_service.client.ProductoClient;
import com.linktic.inventario_service.dto.CompraRequest;
import com.linktic.inventario_service.dto.InventarioRequest;
import com.linktic.inventario_service.dto.InventarioResponse;
import com.linktic.inventario_service.dto.ProductoDTO;
import com.linktic.inventario_service.entity.Inventario;
import com.linktic.inventario_service.entity.InventarioCambiadoEvent;
import com.linktic.inventario_service.exception.ProductoServiceException;
import com.linktic.inventario_service.exception.ResourceNotFoundException;
import com.linktic.inventario_service.repository.InventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InventarioService inventarioService;

    private ProductoDTO producto;
    private Inventario inventario;

    @BeforeEach
    void setUp() {
        // Crear un ProductoDTO simulado
        producto = ProductoDTO.builder()
                .id(1L)
                .attributes(
                        ProductoDTO.Attributes.builder()
                                .nombre("Laptop Dell XPS")
                                .precio(new BigDecimal("1500.00"))
                                .build()
                )
                .build();

        // Crear entidad Inventario de ejemplo
        inventario = Inventario.builder()
                .id(1L)
                .productoId(1L)
                .cantidad(100)
                .cantidadMinima(10)
                .cantidadMaxima(200)
                .createdAt(LocalDateTime.now().minusDays(1))
                .ultimaActualizacion(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("consultarInventario: existente => devuelve InventarioResponse con datos de producto")
    void consultarInventario_DeberiaRetornarInventarioConProducto() {
        // Given: el repositorio devuelve Optional.of(inventario) y el cliente de productos funciona
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.of(inventario));
        when(productoClient.obtenerProducto(1L)).thenReturn(producto);

        // When
        InventarioResponse response = inventarioService.consultarInventario(1L);

        // Then: verificar valores devueltos
        assertThat(response).isNotNull();
        assertThat(response.getAttributes().getCantidad()).isEqualTo(100);
        assertThat(response.getRelationships().getProducto().getData().getId()).isEqualTo(1L);
        assertThat(response.getRelationships().getProducto().getData().getNombre())
                .isEqualTo("Laptop Dell XPS");

        verify(inventarioRepository).findByProductoId(1L);
        verify(productoClient).obtenerProducto(1L);
    }

    @Test
    @DisplayName("consultarInventario: no existente => lanza ResourceNotFoundException")
    void consultarInventarioNoExistente_DeberiaLanzarExcepcion() {
        // Given: el repositorio devuelve Optional.empty()
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventarioService.consultarInventario(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventario no encontrado para productoId=1");

        verify(inventarioRepository).findByProductoId(1L);
        verify(productoClient, never()).obtenerProducto(anyLong());
    }

    @Test
    @DisplayName("consultarInventario: error en servicio de productos => propaga ProductoServiceException")
    void consultarInventario_ErrorServicioProductos_DeberiaLanzarExcepcion() {
        // Given: repositorio devuelve inventario, pero productoClient arroja excepción
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.of(inventario));
        when(productoClient.obtenerProducto(1L))
                .thenThrow(new ProductoServiceException("Servicio no disponible"));

        // When & Then
        assertThatThrownBy(() -> inventarioService.consultarInventario(1L))
                .isInstanceOf(ProductoServiceException.class)
                .hasMessageContaining("Servicio no disponible");

        verify(inventarioRepository).findByProductoId(1L);
        verify(productoClient).obtenerProducto(1L);
    }

    @Test
    @DisplayName("procesarCompra: stock suficiente => decrementa y publica evento")
    void procesarCompra_StockSuficiente_DeberiaActualizarInventario() {
        // Given
        CompraRequest compraReq = CompraRequest.builder()
                .cantidad(10)
                .build();

        when(inventarioRepository.findByProductoIdWithLock(1L)).thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoClient.obtenerProducto(1L)).thenReturn(producto);

        // When
        InventarioResponse response = inventarioService.procesarCompra(1L, compraReq);

        // Then
        assertThat(response.getAttributes().getCantidad()).isEqualTo(90);

        ArgumentCaptor<Inventario> invCaptor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioRepository).save(invCaptor.capture());
        Inventario saved = invCaptor.getValue();
        assertThat(saved.getCantidad()).isEqualTo(90);

        ArgumentCaptor<InventarioCambiadoEvent> eventCaptor = ArgumentCaptor.forClass(InventarioCambiadoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        InventarioCambiadoEvent evento = eventCaptor.getValue();
        assertThat(evento.getProductoId()).isEqualTo(1L);
        assertThat(evento.getCantidadAnterior()).isEqualTo(100);
        assertThat(evento.getCantidadNueva()).isEqualTo(90);
        assertThat(evento.getTipoOperacion()).isEqualTo("COMPRA");
    }

    @Test
    @DisplayName("procesarCompra: stock insuficiente => lanza IllegalArgumentException")
    void procesarCompra_StockInsuficiente_DeberiaLanzarExcepcion() {
        // Given
        CompraRequest compraReq = CompraRequest.builder()
                .cantidad(150)
                .build();
        when(inventarioRepository.findByProductoIdWithLock(1L)).thenReturn(Optional.of(inventario));

        // When & Then
        assertThatThrownBy(() -> inventarioService.procesarCompra(1L, compraReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente para productoId=1");

        verify(inventarioRepository).findByProductoIdWithLock(1L);
        verify(inventarioRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("procesarCompra: inventario no existe => lanza ResourceNotFoundException")
    void procesarCompra_NoExisteInventario_DeberiaLanzarExcepcion() {
        // Given
        CompraRequest compraReq = CompraRequest.builder()
                .cantidad(10)
                .build();
        when(inventarioRepository.findByProductoIdWithLock(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventarioService.procesarCompra(1L, compraReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventario no encontrado para productoId=1");

        verify(inventarioRepository).findByProductoIdWithLock(1L);
        verify(inventarioRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("crearInventario: éxito => crea nuevo inventario y publica evento")
    void crearInventario_DeberiaCrearNuevoInventario() {
        // Given
        InventarioRequest createReq = InventarioRequest.builder()
                .cantidad(50)
                .cantidadMinima(5)
                .cantidadMaxima(100)
                .build();

        when(inventarioRepository.existsByProductoId(1L)).thenReturn(false);
        when(productoClient.obtenerProducto(1L)).thenReturn(producto);
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocation -> {
            Inventario inv = invocation.getArgument(0);
            inv.setId(1L);
            return inv;
        });

        // When
        InventarioResponse response = inventarioService.crearInventario(1L, createReq);

        // Then
        assertThat(response.getAttributes().getCantidad()).isEqualTo(50);

        ArgumentCaptor<Inventario> invCaptor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioRepository).save(invCaptor.capture());
        Inventario saved = invCaptor.getValue();
        assertThat(saved.getCantidad()).isEqualTo(50);
        assertThat(saved.getCantidadMinima()).isEqualTo(5);
        assertThat(saved.getCantidadMaxima()).isEqualTo(100);

        ArgumentCaptor<InventarioCambiadoEvent> eventCaptor = ArgumentCaptor.forClass(InventarioCambiadoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        InventarioCambiadoEvent evento = eventCaptor.getValue();
        assertThat(evento.getProductoId()).isEqualTo(1L);
        assertThat(evento.getCantidadAnterior()).isEqualTo(0);
        assertThat(evento.getCantidadNueva()).isEqualTo(50);
        assertThat(evento.getTipoOperacion()).isEqualTo("CREACION");
    }

    @Test
    @DisplayName("actualizarInventario: éxito => actualiza cantidad y publica evento")
    void actualizarInventario_DeberiaActualizarCantidad() {
        // Given
        InventarioRequest updateReq = InventarioRequest.builder()
                .cantidad(200)
                .cantidadMinima(10)
                .cantidadMaxima(300)
                .build();

        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.of(inventario));
        when(productoClient.obtenerProducto(1L)).thenReturn(producto);
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InventarioResponse response = inventarioService.actualizarInventario(1L, updateReq);

        // Then
        assertThat(response.getAttributes().getCantidad()).isEqualTo(200);

        ArgumentCaptor<Inventario> invCaptor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioRepository).save(invCaptor.capture());
        Inventario saved = invCaptor.getValue();
        assertThat(saved.getCantidad()).isEqualTo(200);

        ArgumentCaptor<InventarioCambiadoEvent> eventCaptor = ArgumentCaptor.forClass(InventarioCambiadoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        InventarioCambiadoEvent evento = eventCaptor.getValue();
        assertThat(evento.getCantidadAnterior()).isEqualTo(100);
        assertThat(evento.getCantidadNueva()).isEqualTo(200);
        assertThat(evento.getTipoOperacion()).isEqualTo("ACTUALIZACION");
    }

    @Test
    @DisplayName("actualizarInventario: inventario no existe => lanza ResourceNotFoundException")
    void actualizarInventario_NoExisteInventario_DeberiaLanzarExcepcion() {
        // Given: no existe inventario
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.empty());

        InventarioRequest updateReq = InventarioRequest.builder()
                .cantidad(200)
                .build();

        // When & Then
        assertThatThrownBy(() -> inventarioService.actualizarInventario(1L, updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventario no encontrado para productoId=1");

        verify(inventarioRepository).findByProductoId(1L);
        verify(inventarioRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
