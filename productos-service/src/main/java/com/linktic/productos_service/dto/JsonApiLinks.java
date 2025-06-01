package com.linktic.productos_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JsonApiLinks {
    private String self;
    private String first;
    private String last;
    private String prev;
    private String next;
}

