package com.linktic.inventario_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {
    private String type;
    private Long id;
    private Attributes attributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attributes {
        private String nombre;
        private BigDecimal precio;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}