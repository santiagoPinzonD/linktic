package com.linktic.productos_service.controller;

import com.linktic.productos_service.dto.JsonApiDocument;
import com.linktic.productos_service.dto.JsonApiLinks;
import com.linktic.productos_service.dto.ProductoRequest;
import com.linktic.productos_service.dto.ProductoResponse;
import com.linktic.productos_service.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productos", description = "API para gestión de productos")
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    @Operation(summary = "Crear un nuevo producto")
    public ResponseEntity<JsonApiDocument<ProductoResponse>> crearProducto(
            @Valid @RequestBody JsonApiDocument<ProductoRequest> request) {
        log.info("POST /api/v1/productos - Creando producto");

        ProductoResponse producto = productoService.crear(request.getData());

        JsonApiDocument<ProductoResponse> response = JsonApiDocument.<ProductoResponse>builder()
                .data(producto)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto por ID")
    public ResponseEntity<JsonApiDocument<ProductoResponse>> obtenerProducto(
            @PathVariable Long id) {
        log.info("GET /api/v1/productos/{} - Obteniendo producto", id);

        ProductoResponse producto = productoService.obtenerPorId(id);

        JsonApiDocument<ProductoResponse> response = JsonApiDocument.<ProductoResponse>builder()
                .data(producto)
                .links(JsonApiLinks.builder()
                        .self("/api/v1/productos/" + id)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar un producto")
    public ResponseEntity<JsonApiDocument<ProductoResponse>> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody JsonApiDocument<ProductoRequest> request) {
        log.info("PATCH /api/v1/productos/{} - Actualizando producto", id);

        ProductoResponse producto = productoService.actualizar(id, request.getData());

        JsonApiDocument<ProductoResponse> response = JsonApiDocument.<ProductoResponse>builder()
                .data(producto)
                .links(JsonApiLinks.builder()
                        .self("/api/v1/productos/" + id)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un producto")
    public void eliminarProducto(@PathVariable Long id) {
        log.info("DELETE /api/v1/productos/{} - Eliminando producto", id);
        productoService.eliminar(id);
    }

    @GetMapping
    @Operation(summary = "Listar productos con paginación")
    public ResponseEntity<JsonApiDocument<Page<ProductoResponse>>> listarProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {
        log.info("GET /api/v1/productos - Listando productos página: {}", page);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<ProductoResponse> productos = productoService.listarTodos(pageRequest);

        Map<String, Object> meta = new HashMap<>();
        meta.put("totalPages", productos.getTotalPages());
        meta.put("totalElements", productos.getTotalElements());
        meta.put("currentPage", page);
        meta.put("pageSize", size);

        JsonApiLinks links = JsonApiLinks.builder()
                .self(String.format("/api/v1/productos?page=%d&size=%d", page, size))
                .first("/api/v1/productos?page=0&size=" + size)
                .last(String.format("/api/v1/productos?page=%d&size=%d",
                        productos.getTotalPages() - 1, size))
                .build();

        if (page > 0) {
            links.setPrev(String.format("/api/v1/productos?page=%d&size=%d", page - 1, size));
        }
        if (page < productos.getTotalPages() - 1) {
            links.setNext(String.format("/api/v1/productos?page=%d&size=%d", page + 1, size));
        }

        JsonApiDocument<Page<ProductoResponse>> response = JsonApiDocument.<Page<ProductoResponse>>builder()
                .data(productos)
                .meta(meta)
                .links(links)
                .build();

        return ResponseEntity.ok(response);
    }
}