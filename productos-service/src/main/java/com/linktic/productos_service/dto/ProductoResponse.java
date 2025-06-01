package com.linktic.productos_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponse {
    private String type = "productos";
    private Long id;
    private Attributes attributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attributes {
        private String nombre;
        private BigDecimal precio;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
    }
}
