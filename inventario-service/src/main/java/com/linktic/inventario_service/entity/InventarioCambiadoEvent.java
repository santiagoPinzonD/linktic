package com.linktic.inventario_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventarioCambiadoEvent {
    private Long productoId;
    private Integer cantidadAnterior;
    private Integer cantidadNueva;
    private String tipoOperacion;
    private LocalDateTime timestamp;
}
