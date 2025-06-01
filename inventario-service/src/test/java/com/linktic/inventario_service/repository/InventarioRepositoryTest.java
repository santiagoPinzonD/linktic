package com.linktic.inventario_service.repository;

import com.linktic.inventario_service.entity.Inventario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class InventarioRepositoryTest {

    @Autowired
    private InventarioRepository inventarioRepository;

    private Inventario inventarioPersistido;

    @BeforeEach
    void setUp() {
        // Crear y persistir un inventario de prueba
        Inventario inventario = Inventario.builder()
                .productoId(100L)
                .cantidad(50)
                .cantidadMinima(10)
                .cantidadMaxima(100)
                .build();

        inventarioPersistido = inventarioRepository.save(inventario);
    }

    @Test
    @DisplayName("findByProductoId: cuando existe inventario retorna Optional con entidad")
    void testFindByProductoIdExiste() {
        Optional<Inventario> resultado = inventarioRepository.findByProductoId(100L);

        assertThat(resultado)
                .as("Verificar que findByProductoId retorne un Optional no vacío cuando el inventario existe")
                .isPresent();

        Inventario inv = resultado.get();
        assertThat(inv.getProductoId()).isEqualTo(100L);
        assertThat(inv.getCantidad()).isEqualTo(50);
    }

    @Test
    @DisplayName("findByProductoId: cuando no existe inventario retorna Optional vacío")
    void testFindByProductoIdNoExiste() {
        Optional<Inventario> resultado = inventarioRepository.findByProductoId(999L);

        assertThat(resultado)
                .as("Verificar que findByProductoId retorne Optional vacío cuando el inventario no existe")
                .isNotPresent();
    }

    @Test
    @DisplayName("existsByProductoId: retorna true si existe inventario")
    void testExistsByProductoIdTrue() {
        boolean existe = inventarioRepository.existsByProductoId(100L);

        assertThat(existe)
                .as("Verificar que existsByProductoId sea true si hay inventario con ese productoId")
                .isTrue();
    }

    @Test
    @DisplayName("existsByProductoId: retorna false si no existe inventario")
    void testExistsByProductoIdFalse() {
        boolean existe = inventarioRepository.existsByProductoId(888L);

        assertThat(existe)
                .as("Verificar que existsByProductoId sea false si no hay inventario con ese productoId")
                .isFalse();
    }

    @Test
    @DisplayName("findCantidadByProductoId: retorna cantidad cuando existe inventario")
    void testFindCantidadByProductoIdExiste() {
        Optional<Integer> cantidadOpt = inventarioRepository.findCantidadByProductoId(100L);

        assertThat(cantidadOpt)
                .as("Verificar que findCantidadByProductoId retorne Optional con la cantidad")
                .isPresent();

        assertThat(cantidadOpt.get())
                .as("Verificar que la cantidad retornada sea la esperada")
                .isEqualTo(50);
    }

    @Test
    @DisplayName("findCantidadByProductoId: retorna Optional vacío cuando no existe inventario")
    void testFindCantidadByProductoIdNoExiste() {
        Optional<Integer> cantidadOpt = inventarioRepository.findCantidadByProductoId(777L);

        assertThat(cantidadOpt)
                .as("Verificar que findCantidadByProductoId retorne Optional vacío cuando no existe inventario")
                .isNotPresent();
    }

    @Test
    @DisplayName("findByProductoIdWithLock: retorna inventario cuando existe (lock pesimista)")
    void testFindByProductoIdWithLock() {
        Optional<Inventario> resultado = inventarioRepository.findByProductoIdWithLock(100L);

        assertThat(resultado)
                .as("Verificar que findByProductoIdWithLock retorne Optional con entidad")
                .isPresent();

        Inventario inv = resultado.get();
        assertThat(inv.getProductoId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Guardado de inventario con productoId duplicado lanza excepción")
    void testProductoIdUnicoConstraint() {
        Inventario duplicado = Inventario.builder()
                .productoId(100L)
                .cantidad(20)
                .build();

        assertThatThrownBy(() -> inventarioRepository.saveAndFlush(duplicado))
                .as("Verificar que al intentar persistir un inventario con productoId duplicado se lanza DataIntegrityViolationException")
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
