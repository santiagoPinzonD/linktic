package com.linktic.inventario_service.dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JsonApiError {
    private String status;
    private String code;
    private String title;
    private String detail;
}


