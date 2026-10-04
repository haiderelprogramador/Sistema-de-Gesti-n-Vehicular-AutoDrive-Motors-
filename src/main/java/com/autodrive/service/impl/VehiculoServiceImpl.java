package com.autodrive.service.impl;

import com.autodrive.client.ExchangeRateClient;
import com.autodrive.dto.request.VehiculoRequestDTO;
import com.autodrive.dto.response.PrecioConvertidoDTO;
import com.autodrive.dto.response.TasaCambioDTO;
import com.autodrive.dto.response.VehiculoResponseDTO;
import com.autodrive.exception.RecursoDuplicadoException;
import com.autodrive.exception.RecursoNoEncontradoException;
import com.autodrive.exception.ReglaNegocioException;
import com.autodrive.mapper.EntityMapper;
import com.autodrive.model.entity.Vehiculo;
import com.autodrive.model.enums.EstadoVehiculo;
import com.autodrive.repository.MantenimientoRepository;
import com.autodrive.repository.VehiculoRepository;
import com.autodrive.repository.VentaRepository;
import com.autodrive.service.VehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final VentaRepository ventaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final ExchangeRateClient exchangeRateClient;
    private final EntityMapper mapper;

    @Override
    public List<VehiculoResponseDTO> listar() {
        return vehiculoRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public VehiculoResponseDTO buscarPorId(Long id) {
        return mapper.toDto(obtener(id));
    }

    @Override
    @Transactional
    public VehiculoResponseDTO registrar(VehiculoRequestDTO dto) {
        validarPrecio(dto.precio());
        String placa = EntityMapper.normalizarPlaca(dto.placa());
        // Regla 3: placa única
        if (vehiculoRepository.existsByPlacaIgnoreCase(placa)) {
            throw new RecursoDuplicadoException("Ya existe un vehículo registrado con la placa " + placa);
        }
        Vehiculo vehiculo = mapper.toEntity(dto);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        return mapper.toDto(vehiculoRepository.save(vehiculo));
    }

    @Override
    @Transactional
    public VehiculoResponseDTO actualizar(Long id, VehiculoRequestDTO dto) {
        Vehiculo vehiculo = obtener(id);
        if (vehiculo.getEstado() == EstadoVehiculo.VENDIDO) {
            throw new ReglaNegocioException("No se puede modificar un vehículo que ya fue vendido");
        }
        validarPrecio(dto.precio());
        String placa = EntityMapper.normalizarPlaca(dto.placa());
        if (vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(placa, id)) {
            throw new RecursoDuplicadoException("La placa " + placa + " ya pertenece a otro vehículo");
        }
        // El estado NO se cambia por aquí: solo lo cambian las ventas y los mantenimientos
        mapper.updateEntity(vehiculo, dto);
        return mapper.toDto(vehiculoRepository.save(vehiculo));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Vehiculo vehiculo = obtener(id);
        if (vehiculo.getEstado() == EstadoVehiculo.VENDIDO || ventaRepository.existsByVehiculoId(id)) {
            throw new ReglaNegocioException("No se puede eliminar un vehículo vendido (se perdería el registro de la venta)");
        }
        if (mantenimientoRepository.existsByVehiculoId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el vehículo porque tiene historial de mantenimientos");
        }
        vehiculoRepository.delete(vehiculo);
    }

    @Override
    public List<VehiculoResponseDTO> buscarPorMarca(String marca) {
        return vehiculoRepository.findByMarcaIgnoreCase(marca.trim()).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<VehiculoResponseDTO> listarDisponibles() {
        return vehiculoRepository.findByEstado(EstadoVehiculo.DISPONIBLE).stream().map(mapper::toDto).toList();
    }

    @Override
    public PrecioConvertidoDTO precioEnDolares(Long id) {
        Vehiculo v = obtener(id);
        TasaCambioDTO tasa = exchangeRateClient.obtenerTasaCopUsd();
        BigDecimal usd = v.getPrecio().multiply(tasa.copUsd()).setScale(2, RoundingMode.HALF_UP);
        return new PrecioConvertidoDTO(v.getId(), v.getPlaca(), EntityMapper.descripcion(v),
                v.getPrecio(), tasa.copUsd(), usd, tasa.fuente(), tasa.fechaConsulta());
    }

    private void validarPrecio(BigDecimal precio) {
        // Regla 2 (segunda línea de defensa además de la validación del DTO)
        if (precio == null || precio.signum() < 0) {
            throw new ReglaNegocioException("El precio del vehículo no puede ser negativo");
        }
    }

    private Vehiculo obtener(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", id));
    }
}
