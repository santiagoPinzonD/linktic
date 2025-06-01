package com.linktic.inventario_service.service;

import com.linktic.inventario_service.client.ProductoClient;
import com.linktic.inventario_service.dto.CompraRequest;
import com.linktic.inventario_service.dto.InventarioRequest;
import com.linktic.inventario_service.dto.InventarioResponse;
import com.linktic.inventario_service.dto.ProductoDTO;
import com.linktic.inventario_service.entity.Inventario;
import com.linktic.inventario_service.entity.InventarioCambiadoEvent;
import com.linktic.inventario_service.exception.ResourceNotFoundException;
import com.linktic.inventario_service.repository.InventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoClient productoClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InventarioResponse crearInventario(Long productoId, InventarioRequest request) {
        ProductoDTO producto = productoClient.obtenerProducto(productoId);

        if (inventarioRepository.existsByProductoId(productoId)) {
            throw new IllegalArgumentException("Ya existe inventario para productoId=" + productoId);
        }
        Inventario nuevo = Inventario.builder()
                .productoId(productoId)
                .cantidad(request.getCantidad())
                .cantidadMinima(
                        request.getCantidadMinima() != null ? request.getCantidadMinima() : 10
                )
                .cantidadMaxima(
                        request.getCantidadMaxima() != null ? request.getCantidadMaxima() : 1000
                )
                .build();
        Inventario guardado = inventarioRepository.save(nuevo);

        InventarioCambiadoEvent event = new InventarioCambiadoEvent(
                productoId,
                0,
                guardado.getCantidad(),
                "CREACION",
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);

        return mapToResponse(guardado, producto);
    }

    @Transactional(readOnly = true)
    public InventarioResponse consultarInventario(Long productoId) {
        Inventario inv = inventarioRepository.findByProductoId(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventario no encontrado para productoId=" + productoId));

        ProductoDTO producto = productoClient.obtenerProducto(productoId);
        return mapToResponse(inv, producto);
    }

    @Transactional
    public InventarioResponse procesarCompra(Long productoId, CompraRequest request) {
        Inventario inv = inventarioRepository.findByProductoIdWithLock(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventario no encontrado para productoId=" + productoId));

        int anterior = inv.getCantidad();
        int aRestar = request.getCantidad();
        if (anterior < aRestar) {
            throw new IllegalArgumentException(
                    "Stock insuficiente para productoId=" + productoId);
        }

        inv.setCantidad(anterior - aRestar);
        Inventario actualizado = inventarioRepository.save(inv);

        InventarioCambiadoEvent event = new InventarioCambiadoEvent(
                productoId,
                anterior,
                actualizado.getCantidad(),
                "COMPRA",
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);

        ProductoDTO producto = productoClient.obtenerProducto(productoId);
        return mapToResponse(actualizado, producto);
    }

    @Transactional
    public InventarioResponse actualizarInventario(Long productoId, InventarioRequest request) {
        Inventario inv = inventarioRepository.findByProductoId(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventario no encontrado para productoId=" + productoId));

        int anterior = inv.getCantidad();
        inv.setCantidad(request.getCantidad());
        if (request.getCantidadMinima() != null) {
            inv.setCantidadMinima(request.getCantidadMinima());
        }
        if (request.getCantidadMaxima() != null) {
            inv.setCantidadMaxima(request.getCantidadMaxima());
        }

        Inventario actualizado = inventarioRepository.save(inv);

        InventarioCambiadoEvent event = new InventarioCambiadoEvent(
                productoId,
                anterior,
                actualizado.getCantidad(),
                "ACTUALIZACION",
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);

        ProductoDTO producto = productoClient.obtenerProducto(productoId);
        return mapToResponse(actualizado, producto);
    }

    @Transactional(readOnly = true)
    public Page<InventarioResponse> listarInventarios(Pageable pageable) {
        return inventarioRepository.findAll(pageable)
                .map(inv -> {
                    ProductoDTO producto = productoClient.obtenerProducto(inv.getProductoId());
                    return mapToResponse(inv, producto);
                });
    }

    @Transactional(readOnly = true)
    public Page<InventarioResponse> listarInventariosConStockBajo(Pageable pageable) {
        Page<Inventario> inventariosPage = inventarioRepository.findAll(pageable);

        List<InventarioResponse> filtrado = inventariosPage.stream()
                .filter(inv -> inv.getCantidad() <= inv.getCantidadMinima())
                .map(inv -> {
                    ProductoDTO producto = productoClient.obtenerProducto(inv.getProductoId());
                    return mapToResponse(inv, producto);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(
                filtrado,
                pageable,
                filtrado.size()
        );
    }

    private InventarioResponse mapToResponse(Inventario inv, ProductoDTO producto) {
        String nombre = producto.getAttributes().getNombre();
        java.math.BigDecimal precio = producto.getAttributes().getPrecio();

        InventarioResponse.Relationships.ProductoData dataProducto =
                InventarioResponse.Relationships.ProductoData.builder()
                        .type("productos")
                        .id(producto.getId())
                        .nombre(nombre)
                        .precio(precio)
                        .build();

        InventarioResponse.Relationships rel = InventarioResponse.Relationships.builder()
                .producto(
                        InventarioResponse.Relationships.Producto.builder()
                                .data(dataProducto)
                                .build()
                )
                .build();

        return InventarioResponse.builder()
                .type("inventarios")
                .id(inv.getId())
                .attributes(InventarioResponse.Attributes.builder()
                        .cantidad(inv.getCantidad())
                        .cantidadMinima(inv.getCantidadMinima())
                        .cantidadMaxima(inv.getCantidadMaxima())
                        .stockBajo(inv.getCantidad() <= inv.getCantidadMinima())
                        .ultimaActualizacion(inv.getUltimaActualizacion())
                        .build())
                .relationships(rel)
                .build();
    }
}
