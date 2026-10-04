package com.autodrive.repository;

import com.autodrive.model.entity.Mantenimiento;
import com.autodrive.model.enums.EstadoMantenimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    @EntityGraph(attributePaths = {"vehiculo"})
    List<Mantenimiento> findAllByOrderByFechaIngresoDesc();

    @EntityGraph(attributePaths = {"vehiculo"})
    Optional<Mantenimiento> findWithVehiculoById(Long id);

    @EntityGraph(attributePaths = {"vehiculo"})
    List<Mantenimiento> findByVehiculoIdOrderByFechaIngresoDesc(Long vehiculoId);

    boolean existsByVehiculoIdAndEstado(Long vehiculoId, EstadoMantenimiento estado);

    boolean existsByVehiculoId(Long vehiculoId);
}
