package com.autodrive.service.impl;

import com.autodrive.dto.request.MantenimientoRequestDTO;
import com.autodrive.dto.response.MantenimientoResponseDTO;
import com.autodrive.exception.RecursoNoEncontradoException;
import com.autodrive.exception.ReglaNegocioException;
import com.autodrive.mapper.EntityMapper;
import com.autodrive.model.entity.Mantenimiento;
import com.autodrive.model.entity.Vehiculo;
import com.autodrive.model.enums.EstadoMantenimiento;
import com.autodrive.model.enums.EstadoVehiculo;
import com.autodrive.repository.MantenimientoRepository;
import com.autodrive.repository.VehiculoRepository;
import com.autodrive.service.MantenimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MantenimientoServiceImpl implements MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final EntityMapper mapper;

    @Override
    @Transactional
    public MantenimientoResponseDTO registrar(MantenimientoRequestDTO dto) {
        Vehiculo vehiculo = vehiculoRepository.findById(dto.vehiculoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", dto.vehiculoId()));

        if (mantenimientoRepository.existsByVehiculoIdAndEstado(vehiculo.getId(), EstadoMantenimiento.EN_PROCESO)) {
            throw new ReglaNegocioException("El vehículo con placa " + vehiculo.getPlaca()
                    + " ya tiene un mantenimiento en proceso. Finalícelo antes de registrar otro");
        }

        Mantenimiento m = Mantenimiento.builder()
                .vehiculo(vehiculo)
                .tipo(dto.tipo())
                .descripcion(dto.descripcion().trim())
                .costo(dto.costo())
                .fechaIngreso(dto.fechaIngreso() != null ? dto.fechaIngreso() : LocalDate.now())
                .estado(EstadoMantenimiento.EN_PROCESO)
                .build();

        // Un vehículo de inventario pasa a EN_MANTENIMIENTO (ya no se puede vender).
        // Un vehículo VENDIDO puede recibir servicio postventa, pero conserva su estado VENDIDO.
        if (vehiculo.getEstado() == EstadoVehiculo.DISPONIBLE) {
            vehiculo.setEstado(EstadoVehiculo.EN_MANTENIMIENTO);
            vehiculoRepository.save(vehiculo);
        }

        return mapper.toDto(mantenimientoRepository.save(m));
    }

    @Override
    public List<MantenimientoResponseDTO> listar() {
        return mantenimientoRepository.findAllByOrderByFechaIngresoDesc().stream().map(mapper::toDto).toList();
    }

    @Override
    public MantenimientoResponseDTO buscarPorId(Long id) {
        return mapper.toDto(obtener(id));
    }

    @Override
    public List<MantenimientoResponseDTO> historialPorVehiculo(Long vehiculoId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            throw new RecursoNoEncontradoException("Vehículo", vehiculoId);
        }
        return mantenimientoRepository.findByVehiculoIdOrderByFechaIngresoDesc(vehiculoId)
                .stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public MantenimientoResponseDTO finalizar(Long id) {
        Mantenimiento m = obtener(id);
        if (m.getEstado() == EstadoMantenimiento.FINALIZADO) {
            throw new ReglaNegocioException("El mantenimiento " + id + " ya fue finalizado");
        }
        m.setEstado(EstadoMantenimiento.FINALIZADO);
        m.setFechaSalida(LocalDate.now());

        // El vehículo vuelve a estar disponible para la venta
        Vehiculo v = m.getVehiculo();
        if (v.getEstado() == EstadoVehiculo.EN_MANTENIMIENTO) {
            v.setEstado(EstadoVehiculo.DISPONIBLE);
            vehiculoRepository.save(v);
        }
        return mapper.toDto(mantenimientoRepository.save(m));
    }

    private Mantenimiento obtener(Long id) {
        return mantenimientoRepository.findWithVehiculoById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mantenimiento", id));
    }
}
