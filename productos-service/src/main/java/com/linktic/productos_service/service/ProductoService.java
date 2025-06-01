package com.linktic.productos_service.service;

import com.linktic.productos_service.dto.ProductoRequest;
import com.linktic.productos_service.dto.ProductoResponse;
import com.linktic.productos_service.exception.DuplicateProductException;
import com.linktic.productos_service.exception.ProductoNotFoundException;
import com.linktic.productos_service.model.Producto;
import com.linktic.productos_service.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoResponse crear(ProductoRequest request) {
        log.debug("Creando producto: {}", request.getNombre());

        if (productoRepository.existsByNombre(request.getNombre())) {
            throw new DuplicateProductException(
                    "Ya existe un producto con el nombre: " + request.getNombre()
            );
        }

        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .precio(request.getPrecio())
                .build();

        producto = productoRepository.save(producto);
        log.info("Producto creado con ID: {}", producto.getId());

        return mapToResponse(producto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        log.debug("Buscando producto con ID: {}", id);

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(
                        "Producto no encontrado con ID: " + id
                ));

        return mapToResponse(producto);
    }

    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        log.debug("Actualizando producto con ID: {}", id);

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(
                        "Producto no encontrado con ID: " + id
                ));

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());

        producto = productoRepository.save(producto);
        log.info("Producto actualizado: {}", producto.getId());

        return mapToResponse(producto);
    }

    public void eliminar(Long id) {
        log.debug("Eliminando producto con ID: {}", id);

        if (!productoRepository.existsById(id)) {
            throw new ProductoNotFoundException(
                    "Producto no encontrado con ID: " + id
            );
        }

        productoRepository.deleteById(id);
        log.info("Producto eliminado: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listarTodos(Pageable pageable) {
        log.debug("Listando productos - página: {}, tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return productoRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private ProductoResponse mapToResponse(Producto producto) {
        return ProductoResponse.builder()
                .type("productos")
                .id(producto.getId())
                .attributes(ProductoResponse.Attributes.builder()
                        .nombre(producto.getNombre())
                        .precio(producto.getPrecio())
                        .createdAt(producto.getCreatedAt())
                        .updatedAt(producto.getUpdatedAt())
                        .build())
                .build();
    }
}