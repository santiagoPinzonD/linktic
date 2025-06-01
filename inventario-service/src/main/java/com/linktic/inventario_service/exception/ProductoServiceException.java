package com.linktic.inventario_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lanza 503 cuando falla la comunicación con el servicio de Productos.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ProductoServiceException extends RuntimeException {
    public ProductoServiceException(String message) {
        super(message);
    }
}
