package com.linktic.inventario_service.repository;

import com.linktic.inventario_service.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProductoId(Long productoId);

    boolean existsByProductoId(Long productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventario i WHERE i.productoId = :productoId")
    Optional<Inventario> findByProductoIdWithLock(@Param("productoId") Long productoId);

    @Query("SELECT i.cantidad FROM Inventario i WHERE i.productoId = :productoId")
    Optional<Integer> findCantidadByProductoId(@Param("productoId") Long productoId);
}
