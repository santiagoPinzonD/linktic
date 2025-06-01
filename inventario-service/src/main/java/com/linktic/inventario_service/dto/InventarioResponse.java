package com.linktic.inventario_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioResponse {

    private String type = "inventarios";
    private Long id;
    private Attributes attributes;
    private Relationships relationships;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attributes {
        private Integer cantidad;

        @JsonProperty("cantidad_minima")
        private Integer cantidadMinima;

        @JsonProperty("cantidad_maxima")
        private Integer cantidadMaxima;

        @JsonProperty("stock_bajo")
        private Boolean stockBajo;

        @JsonProperty("ultima_actualizacion")
        private LocalDateTime ultimaActualizacion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Relationships {
        private Producto producto;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Producto {
            private ProductoData data;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ProductoData {
            private String type = "productos";
            private Long id;
            private String nombre;
            private BigDecimal precio;
        }
    }
}
