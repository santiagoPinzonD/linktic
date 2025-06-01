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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoClient productoClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InventarioResponse crearInventario(Long productoId, InventarioRequest request) {
        log.debug("Creando inventario para producto ID: {}", productoId);

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
        log.info("Inventario creado: ID={}, productoId={}, cantidad={}",
                guardado.getId(), productoId, guardado.getCantidad());

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
        log.debug("Consultando inventario para producto ID: {}", productoId);

        Inventario inv = inventarioRepository.findByProductoId(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventario no encontrado para productoId=" + productoId));

        ProductoDTO producto = productoClient.obtenerProducto(productoId);
        return mapToResponse(inv, producto);
    }

    @Transactional
    public InventarioResponse procesarCompra(Long productoId, CompraRequest request) {
        log.debug("Procesando compra para producto ID: {}, cantidad: {}", productoId, request.getCantidad());

        Inventario inv = inventarioRepository.findByProductoIdWithLock(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventario no encontrado para productoId=" + productoId));

        int anterior = inv.getCantidad();
        int aRestar = request.getCantidad();
        if (anterior < aRestar) {
            throw new IllegalArgumentException(
                    "Stock insuficiente para productoId=" + productoId +
                            ". Disponible: " + anterior + ", Solicitado: " + aRestar);
        }

        inv.setCantidad(anterior - aRestar);
        Inventario actualizado = inventarioRepository.save(inv);

        log.info("Compra procesada: productoId={}, cantidadAnterior={}, cantidadNueva={}",
                productoId, anterior, actualizado.getCantidad());

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
        log.debug("Actualizando inventario para producto ID: {}", productoId);

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

        log.info("Inventario actualizado: productoId={}, cantidadAnterior={}, cantidadNueva={}",
                productoId, anterior, actualizado.getCantidad());

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
        log.debug("Listando inventarios - página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Inventario> inventarios = inventarioRepository.findAll(pageable);

        if (inventarios.isEmpty()) {
            log.debug("No se encontraron inventarios");
            return Page.empty(pageable);
        }

        List<InventarioResponse> inventariosValidos = inventarios.getContent()
                .stream()
                .map(this::mapToResponseSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Inventarios válidos encontrados: {}/{}",
                inventariosValidos.size(), inventarios.getContent().size());

        return new PageImpl<>(inventariosValidos, pageable, inventariosValidos.size());
    }

    @Transactional(readOnly = true)
    public Page<InventarioResponse> listarInventariosConStockBajo(Pageable pageable) {
        log.debug("Listando inventarios con stock bajo");

        Page<Inventario> inventarios = inventarioRepository.findAll(pageable);

        List<InventarioResponse> stockBajo = inventarios.getContent()
                .stream()
                .filter(inv -> inv.getCantidad() <= inv.getCantidadMinima())
                .map(this::mapToResponseSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Inventarios con stock bajo encontrados: {}", stockBajo.size());

        return new PageImpl<>(stockBajo, pageable, stockBajo.size());
    }

    @Transactional
    public int limpiarInventariosHuerfanos() {
        log.info("Iniciando limpieza de inventarios huérfanos...");

        List<Inventario> todosInventarios = inventarioRepository.findAll();
        int eliminados = 0;

        for (Inventario inventario : todosInventarios) {
            try {
                productoClient.obtenerProducto(inventario.getProductoId());
            } catch (ProductoServiceException ex) {
                log.info("Eliminando inventario huérfano: ID={}, productoId={}",
                        inventario.getId(), inventario.getProductoId());

                inventarioRepository.delete(inventario);

                InventarioCambiadoEvent event = new InventarioCambiadoEvent(
                        inventario.getProductoId(),
                        inventario.getCantidad(),
                        0,
                        "ELIMINACION_HUERFANO",
                        LocalDateTime.now()
                );
                eventPublisher.publishEvent(event);

                eliminados++;
            }
        }

        log.info("Limpieza completada. Inventarios huérfanos eliminados: {}", eliminados);
        return eliminados;
    }

    private InventarioResponse mapToResponseSafe(Inventario inventario) {
        try {
            ProductoDTO producto = productoClient.obtenerProducto(inventario.getProductoId());
            return mapToResponse(inventario, producto);

        } catch (ProductoServiceException ex) {
            log.warn("Producto no encontrado para inventario ID={}, productoId={}. " +
                            "Inventario será filtrado de la respuesta: {}",
                    inventario.getId(), inventario.getProductoId(), ex.getMessage());
            return null;
        }
    }

    private InventarioResponse mapToResponse(Inventario inv, ProductoDTO producto) {
        String nombre = producto.getAttributes().getNombre();
        BigDecimal precio = producto.getAttributes().getPrecio();

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