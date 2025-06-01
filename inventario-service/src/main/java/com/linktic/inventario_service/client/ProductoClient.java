package com.linktic.inventario_service.client;

import com.linktic.inventario_service.dto.JsonApiDocument;
import com.linktic.inventario_service.dto.ProductoDTO;
import com.linktic.inventario_service.exception.ProductoServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ProductoClient {

    private final RestTemplate restTemplate;
    private final String productosServiceUrl;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final String apiKey;

    public ProductoClient(RestTemplate restTemplate,
                          @Value("${productos.service.url}") String productosServiceUrl,
                          @Value("${productos.api.key}") String apiKey,
                          CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restTemplate = restTemplate;
        this.productosServiceUrl = productosServiceUrl;
        this.apiKey = apiKey;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public ProductoDTO obtenerProducto(Long productoId) {
        return circuitBreakerFactory.create("productos-service").run(
                () -> {
                    log.info("Llamando a servicio de productos para ID: {}", productoId);

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-API-Key", apiKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(MediaType.parseMediaTypes("application/vnd.api+json,application/json"));

                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    try {
                        ResponseEntity<JsonApiDocument<ProductoDTO>> response = restTemplate.exchange(
                                productosServiceUrl + "/api/v1/productos/" + productoId,
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<JsonApiDocument<ProductoDTO>>() {}
                        );

                        // Validar respuesta
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new ProductoServiceException("Error HTTP: " + response.getStatusCode());
                        }

                        if (response.getBody() == null || response.getBody().getData() == null) {
                            throw new ProductoServiceException("Producto no encontrado con ID: " + productoId);
                        }

                        return response.getBody().getData();

                    } catch (Exception e) {
                        log.error("Error al llamar al servicio de productos: {}", e.getMessage());
                        throw new ProductoServiceException("Error de comunicación con servicio de productos: " + e.getMessage());
                    }
                },
                throwable -> {
                    log.error("Circuit breaker activado para producto ID {}: {}", productoId, throwable.getMessage());
                    throw new ProductoServiceException(
                            "Servicio de productos no disponible para ID=" + productoId + ". " + throwable.getMessage()
                    );
                }
        );
    }
}