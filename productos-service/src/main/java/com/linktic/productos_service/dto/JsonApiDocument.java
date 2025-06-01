package com.linktic.productos_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiDocument<T> {
    private T data;
    private List<JsonApiError> errors;
    private Map<String, Object> meta;
    private JsonApiLinks links;
}