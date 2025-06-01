package com.linktic.inventario_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventarios",
        indexes = @Index(name = "idx_producto_id", columnList = "producto_id", unique = true)
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false, unique = true)
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @Column(nullable = false)
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    private Integer cantidad;

    @Column(name = "cantidad_minima")
    @Min(value = 0, message = "La cantidad mínima no puede ser negativa")
    private Integer cantidadMinima = 10;

    @Column(name = "cantidad_maxima")
    @Min(value = 1, message = "La cantidad máxima debe ser al menos 1")
    private Integer cantidadMaxima = 1000;

    // Eliminamos cualquier inicializador y @CreatedDate; este campo se rellenará en @PrePersist
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    @Version
    private Long version;

    /**
     *
     * Método que se ejecuta antes de persistir la entidad.
     */
    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
