package com.autodrive.repository;

import com.autodrive.model.entity.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    List<Venta> findAllByOrderByFechaVentaDesc();

    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    Optional<Venta> findWithDetalleById(Long id);

    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);

    boolean existsByVehiculoId(Long vehiculoId);

    boolean existsByClienteId(Long clienteId);
}
