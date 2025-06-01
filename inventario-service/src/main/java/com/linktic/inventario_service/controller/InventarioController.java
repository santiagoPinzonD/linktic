package com.linktic.inventario_service.controller;

import com.linktic.inventario_service.dto.*;
import com.linktic.inventario_service.service.InventarioService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventarios", description = "API para gestión de inventarios")
public class InventarioController {

    private final InventarioService inventarioService;

    @PostMapping("/productos/{productoId}")
    @Operation(summary = "Crear inventario para un producto")
    public ResponseEntity<JsonApiDocument<InventarioResponse>> crearInventario(
            @PathVariable Long productoId,
            @Valid @RequestBody JsonApiDocument<InventarioRequest> request) {
        log.info("POST /api/v1/inventarios/productos/{} - Creando inventario", productoId);

        InventarioResponse inventario = inventarioService.crearInventario(
                productoId, request.getData()
        );

        JsonApiDocument<InventarioResponse> response = JsonApiDocument.<InventarioResponse>builder()
                .data(inventario)
                .links(JsonApiLinks.builder()
                        .self("/api/v1/inventarios/productos/" + productoId)
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/productos/{productoId}")
    @Operation(summary = "Consultar inventario de un producto")
    public ResponseEntity<JsonApiDocument<InventarioResponse>> consultarInventario(
            @PathVariable Long productoId) {
        log.info("GET /api/v1/inventarios/productos/{} - Consultando inventario", productoId);

        InventarioResponse inventario = inventarioService.consultarInventario(productoId);

        JsonApiDocument<InventarioResponse> response = JsonApiDocument.<InventarioResponse>builder()
                .data(inventario)
                .links(JsonApiLinks.builder()
                        .self("/api/v1/inventarios/productos/" + productoId)
                        .related("/api/v1/productos/" + productoId)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/productos/{productoId}/compra")
    @Operation(summary = "Procesar compra y actualizar inventario")
    public ResponseEntity<JsonApiDocument<InventarioResponse>> procesarCompra(
            @PathVariable Long productoId,
            @Valid @RequestBody JsonApiDocument<CompraRequest> request) {
        log.info("PATCH /api/v1/inventarios/productos/{}/compra - Procesando compra", productoId);

        InventarioResponse inventario = inventarioService.procesarCompra(
                productoId, request.getData()
        );

        Map<String, Object> meta = new HashMap<>();
        meta.put("operacion", "compra");
        meta.put("cantidad_comprada", request.getData().getCantidad());

        if (inventario.getAttributes().getStockBajo()) {
            meta.put("alerta", "Stock bajo - Se recomienda reabastecer");
        }

        JsonApiDocument<InventarioResponse> response = JsonApiDocument.<InventarioResponse>builder()
                .data(inventario)
                .meta(meta)
                .links(JsonApiLinks.builder()
                        .self("/api/v1/inventarios/productos/" + productoId)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/productos/{productoId}")
    @Operation(summary = "Actualizar inventario de un producto")
    public ResponseEntity<JsonApiDocument<InventarioResponse>> actualizarInventario(
            @PathVariable Long productoId,
            @Valid @RequestBody JsonApiDocument<InventarioRequest> request) {
        log.info("PATCH /api/v1/inventarios/productos/{} - Actualizando inventario", productoId);

        InventarioResponse inventario = inventarioService.actualizarInventario(
                productoId, request.getData()
        );

        JsonApiDocument<InventarioResponse> response = JsonApiDocument.<InventarioResponse>builder()
                .data(inventario)
                .links(JsonApiLinks.builder()
                        .self("/api/v1/inventarios/productos/" + productoId)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos los inventarios con paginación")
    public ResponseEntity<JsonApiDocument<List<InventarioResponse>>> listarInventarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {
        log.info("GET /api/v1/inventarios - Listando inventarios página: {}", page);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<InventarioResponse> inventariosPage = inventarioService.listarInventarios(pageRequest);

        // Extraer solo el contenido de la página
        List<InventarioResponse> inventarios = inventariosPage.getContent();

        Map<String, Object> meta = new HashMap<>();
        meta.put("totalPages", inventariosPage.getTotalPages());
        meta.put("totalElements", inventariosPage.getTotalElements());
        meta.put("currentPage", page);
        meta.put("pageSize", size);
        meta.put("hasNext", inventariosPage.hasNext());
        meta.put("hasPrevious", inventariosPage.hasPrevious());

        JsonApiLinks links = JsonApiLinks.builder()
                .self(String.format("/api/v1/inventarios?page=%d&size=%d", page, size))
                .build();

        // Agregar enlaces de navegación si existen
        if (inventariosPage.hasNext()) {
            links.setNext(String.format("/api/v1/inventarios?page=%d&size=%d", page + 1, size));
        }
        if (inventariosPage.hasPrevious()) {
            links.setPrev(String.format("/api/v1/inventarios?page=%d&size=%d", page - 1, size));
        }
        if (inventariosPage.getTotalPages() > 0) {
            links.setFirst(String.format("/api/v1/inventarios?page=0&size=%d", size));
            links.setLast(String.format("/api/v1/inventarios?page=%d&size=%d",
                    inventariosPage.getTotalPages() - 1, size));
        }

        JsonApiDocument<List<InventarioResponse>> response =
                JsonApiDocument.<List<InventarioResponse>>builder()
                        .data(inventarios)  // Solo la lista, no el Page completo
                        .meta(meta)
                        .links(links)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock-bajo")
    @Operation(summary = "Listar inventarios con stock bajo")
    public ResponseEntity<JsonApiDocument<List<InventarioResponse>>> listarInventariosStockBajo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/v1/inventarios/stock-bajo - Listando inventarios con stock bajo");

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<InventarioResponse> inventariosPage = inventarioService.listarInventariosConStockBajo(pageRequest);

        // Extraer solo el contenido de la página
        List<InventarioResponse> inventarios = inventariosPage.getContent();

        Map<String, Object> meta = new HashMap<>();
        meta.put("totalPages", inventariosPage.getTotalPages());
        meta.put("totalElements", inventariosPage.getTotalElements());
        meta.put("filtro", "stock_bajo");
        meta.put("currentPage", page);
        meta.put("pageSize", size);

        JsonApiDocument<List<InventarioResponse>> response =
                JsonApiDocument.<List<InventarioResponse>>builder()
                        .data(inventarios)  // Solo la lista, no el Page completo
                        .meta(meta)
                        .build();

        return ResponseEntity.ok(response);
    }
}