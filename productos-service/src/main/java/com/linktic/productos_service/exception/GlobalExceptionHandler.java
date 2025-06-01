package com.linktic.productos_service.exception;

import com.linktic.productos_service.dto.JsonApiDocument;
import com.linktic.productos_service.dto.JsonApiError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<JsonApiDocument<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        log.error("Violación de restricciones: {}", ex.getMessage());

        List<JsonApiError> errors = new ArrayList<>();

        ex.getConstraintViolations().forEach(violation -> {
            errors.add(JsonApiError.builder()
                    .status("400")
                    .code("VALIDATION_ERROR")
                    .title("Error de validación")
                    .detail(violation.getPropertyPath() + ": " + violation.getMessage())
                    .build());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                JsonApiDocument.<Void>builder().errors(errors).build()
        );
    }

    @ExceptionHandler(ProductoNotFoundException.class)
    public ResponseEntity<JsonApiDocument<Void>> handleProductoNotFound(
            ProductoNotFoundException ex) {
        log.error("Producto no encontrado: {}", ex.getMessage());

        JsonApiError error = JsonApiError.builder()
                .status("404")
                .code("PRODUCT_NOT_FOUND")
                .title("Producto no encontrado")
                .detail(ex.getMessage())
                .build();

        JsonApiDocument<Void> response = JsonApiDocument.<Void>builder()
                .errors(List.of(error))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<JsonApiDocument<Void>> handleDuplicateProduct(
            DuplicateProductException ex) {
        log.error("Producto duplicado: {}", ex.getMessage());

        JsonApiError error = JsonApiError.builder()
                .status("409")
                .code("DUPLICATE_PRODUCT")
                .title("Producto duplicado")
                .detail(ex.getMessage())
                .build();

        JsonApiDocument<Void> response = JsonApiDocument.<Void>builder()
                .errors(List.of(error))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonApiDocument<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        List<JsonApiError> errors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();

            errors.add(JsonApiError.builder()
                    .status("400")
                    .code("VALIDATION_ERROR")
                    .title("Error de validación")
                    .detail(fieldName + ": " + errorMessage)
                    .build());
        });

        JsonApiDocument<Void> response = JsonApiDocument.<Void>builder()
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonApiDocument<Void>> handleGenericException(Exception ex) {
        log.error("Error inesperado: ", ex);

        JsonApiError error = JsonApiError.builder()
                .status("500")
                .code("INTERNAL_ERROR")
                .title("Error interno del servidor")
                .detail("Ha ocurrido un error inesperado")
                .build();

        JsonApiDocument<Void> response = JsonApiDocument.<Void>builder()
                .errors(List.of(error))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}