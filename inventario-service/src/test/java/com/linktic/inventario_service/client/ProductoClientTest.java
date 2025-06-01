package com.linktic.inventario_service.client;

import com.linktic.inventario_service.dto.JsonApiDocument;
import com.linktic.inventario_service.dto.ProductoDTO;
import com.linktic.inventario_service.exception.ProductoServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoClientTest {

    @Mock private RestTemplate restTemplate;
    @Mock private CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    @Mock private CircuitBreaker circuitBreaker;

    private ProductoClient productoClient;
    private final String serviceUrl = "http://productos-service:8081";
    private final String apiKey = "productos-secret-key";

    @BeforeEach
    void setUp() {
        // 1) Al pedir circuit breaker "productos-service", devolvemos nuestro mock
        when(circuitBreakerFactory.create("productos-service")).thenReturn(circuitBreaker);

        // 2) Configuramos circuitBreaker.run(supplier, fallback):
        //    Si "supplier.get()" arroja, invocar fallback.apply(exception).
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    Function<Throwable, ?> fallback = invocation.getArgument(1);

                    try {
                        return supplier.get();
                    } catch (Throwable t) {
                        return fallback.apply(t);
                    }
                });

        productoClient = new ProductoClient(restTemplate, serviceUrl, apiKey, circuitBreakerFactory);
    }

    @Test
    @DisplayName("Obtener producto exitosamente")
    void obtenerProducto_Exitoso() {
        Long productoId = 1L;

        // 1) Simulamos el body con data no nulo
        ProductoDTO.Attributes atributos = ProductoDTO.Attributes.builder()
                .nombre("Laptop Dell XPS")
                .precio(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductoDTO productoData = ProductoDTO.builder()
                .type("productos")
                .id(productoId)
                .attributes(atributos)
                .build();

        JsonApiDocument<ProductoDTO> body = JsonApiDocument.<ProductoDTO>builder()
                .data(productoData)
                .build();

        ResponseEntity<JsonApiDocument<ProductoDTO>> responseEntity =
                new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(serviceUrl + "/api/v1/productos/" + productoId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        ProductoDTO resultado = productoClient.obtenerProducto(productoId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getType()).isEqualTo("productos");
        assertThat(resultado.getAttributes().getNombre()).isEqualTo("Laptop Dell XPS");
        assertThat(resultado.getAttributes().getPrecio()).isEqualTo(new BigDecimal("1500.00"));

        verify(restTemplate).exchange(
                eq(serviceUrl + "/api/v1/productos/" + productoId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );

        verify(circuitBreakerFactory).create("productos-service");
    }

    @Test
    @DisplayName("Producto no encontrado debe lanzar excepción")
    void obtenerProducto_NoEncontrado() {
        Long productoId = 999L;

        // Simulamos 404 Not Found
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        ));

        assertThatThrownBy(() -> productoClient.obtenerProducto(productoId))
                .isInstanceOf(ProductoServiceException.class)
                .hasMessageContaining("Servicio de productos no disponible para ID=" + productoId);

        verify(circuitBreakerFactory).create("productos-service");
    }

    @Test
    @DisplayName("Error de conexión debe lanzar excepción")
    void obtenerProducto_ErrorConexion() {
        Long productoId = 1L;

        // Simulamos un RuntimeException del RestTemplate
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> productoClient.obtenerProducto(productoId))
                .isInstanceOf(ProductoServiceException.class)
                .hasMessageContaining("Servicio de productos no disponible para ID=" + productoId);

        verify(circuitBreakerFactory).create("productos-service");
    }

    @Test
    @DisplayName("Respuesta con status no exitoso debe lanzar excepción")
    void obtenerProducto_StatusNoExitoso() {
        Long productoId = 1L;

        // Simulamos una respuesta con status 500
        JsonApiDocument<ProductoDTO> body = JsonApiDocument.<ProductoDTO>builder()
                .data(null)
                .build();

        ResponseEntity<JsonApiDocument<ProductoDTO>> responseEntity =
                new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        assertThatThrownBy(() -> productoClient.obtenerProducto(productoId))
                .isInstanceOf(ProductoServiceException.class)
                .hasMessageContaining("Servicio de productos no disponible para ID=" + productoId);
    }

    @Test
    @DisplayName("Respuesta nula (sin data) debe lanzar excepción")
    void obtenerProducto_RespuestaNula() {
        Long productoId = 1L;

        // Simulamos que exchange() retorna un JsonApiDocument sin data
        JsonApiDocument<ProductoDTO> emptyBody = JsonApiDocument.<ProductoDTO>builder()
                .data(null)
                .build();

        ResponseEntity<JsonApiDocument<ProductoDTO>> responseEntity =
                new ResponseEntity<>(emptyBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        assertThatThrownBy(() -> productoClient.obtenerProducto(productoId))
                .isInstanceOf(ProductoServiceException.class)
                .hasMessageContaining("Servicio de productos no disponible para ID=" + productoId);
    }

    @Test
    @DisplayName("Respuesta con body nulo debe lanzar excepción")
    void obtenerProducto_BodyNulo() {
        Long productoId = 1L;

        ResponseEntity<JsonApiDocument<ProductoDTO>> responseEntity =
                new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        assertThatThrownBy(() -> productoClient.obtenerProducto(productoId))
                .isInstanceOf(ProductoServiceException.class)
                .hasMessageContaining("Servicio de productos no disponible para ID=" + productoId);
    }

    @Test
    @DisplayName("Verificar headers correctos en la petición")
    void obtenerProducto_VerificarHeaders() {
        Long productoId = 1L;

        ProductoDTO.Attributes atributos = ProductoDTO.Attributes.builder()
                .nombre("Test")
                .precio(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductoDTO productoData = ProductoDTO.builder()
                .type("productos")
                .id(productoId)
                .attributes(atributos)
                .build();

        JsonApiDocument<ProductoDTO> body = JsonApiDocument.<ProductoDTO>builder()
                .data(productoData)
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        productoClient.obtenerProducto(productoId);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq(serviceUrl + "/api/v1/productos/" + productoId),
                eq(HttpMethod.GET),
                captor.capture(),
                any(ParameterizedTypeReference.class)
        );

        HttpHeaders headers = captor.getValue().getHeaders();

        assertThat(headers.getFirst("X-API-Key")).isEqualTo(apiKey);
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(headers.getAccept())
                .containsExactlyInAnyOrder(
                        MediaType.parseMediaType("application/vnd.api+json"),
                        MediaType.APPLICATION_JSON
                );
    }

    @Test
    @DisplayName("Verificar que se use el circuit breaker correcto")
    void obtenerProducto_VerificarCircuitBreaker() {
        Long productoId = 1L;

        ProductoDTO.Attributes atributos = ProductoDTO.Attributes.builder()
                .nombre("Test")
                .precio(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductoDTO productoData = ProductoDTO.builder()
                .type("productos")
                .id(productoId)
                .attributes(atributos)
                .build();

        JsonApiDocument<ProductoDTO> body = JsonApiDocument.<ProductoDTO>builder()
                .data(productoData)
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        productoClient.obtenerProducto(productoId);

        verify(circuitBreakerFactory).create("productos-service");

        verify(circuitBreaker).run(any(Supplier.class), any(Function.class));
    }
}