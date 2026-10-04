package com.autodrive.service.impl;

import com.autodrive.dto.request.VentaRequestDTO;
import com.autodrive.dto.response.VentaResponseDTO;
import com.autodrive.exception.RecursoNoEncontradoException;
import com.autodrive.exception.ReglaNegocioException;
import com.autodrive.mapper.EntityMapper;
import com.autodrive.model.entity.Cliente;
import com.autodrive.model.entity.Vehiculo;
import com.autodrive.model.entity.Venta;
import com.autodrive.model.enums.EstadoVehiculo;
import com.autodrive.repository.ClienteRepository;
import com.autodrive.repository.VehiculoRepository;
import com.autodrive.repository.VentaRepository;
import com.autodrive.service.VentaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final EntityMapper mapper;
    private final BigDecimal umbralDescuento;
    private final BigDecimal porcentajeDescuento;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            ClienteRepository clienteRepository,
                            VehiculoRepository vehiculoRepository,
                            EntityMapper mapper,
                            @Value("${venta.umbral-descuento:100000000}") BigDecimal umbralDescuento,
                            @Value("${venta.porcentaje-descuento:5}") BigDecimal porcentajeDescuento) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.mapper = mapper;
        this.umbralDescuento = umbralDescuento;
        this.porcentajeDescuento = porcentajeDescuento;
    }

    /**
     * Registra una venta aplicando todas las reglas de negocio en una sola transacción:
     * si algo falla, no se guarda la venta ni se cambia el estado del vehículo.
     */
    @Override
    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", dto.clienteId()));
        Vehiculo vehiculo = vehiculoRepository.findById(dto.vehiculoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", dto.vehiculoId()));

        // Regla 1: no se puede vender un vehículo vendido o en mantenimiento
        if (vehiculo.getEstado() == EstadoVehiculo.VENDIDO || ventaRepository.existsByVehiculoId(vehiculo.getId())) {
            throw new ReglaNegocioException("El vehículo con placa " + vehiculo.getPlaca() + " ya fue vendido");
        }
        if (vehiculo.getEstado() == EstadoVehiculo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("El vehículo con placa " + vehiculo.getPlaca()
                    + " está en mantenimiento y no puede venderse");
        }

        // Regla 6 y Regla 5: cálculo automático de descuento y total
        BigDecimal precioBase = vehiculo.getPrecio();
        BigDecimal descuento = calcularDescuento(precioBase);
        BigDecimal total = precioBase.subtract(descuento).setScale(2, RoundingMode.HALF_UP);

        Venta venta = Venta.builder()
                .cliente(cliente)
                .vehiculo(vehiculo)
                .fechaVenta(LocalDateTime.now())   // Regla 5: fecha automática
                .precioBase(precioBase)
                .descuento(descuento)
                .total(total)
                .metodoPago(dto.metodoPago())
                .build();

        // Cambio automático del estado a VENDIDO
        vehiculo.setEstado(EstadoVehiculo.VENDIDO);
        vehiculoRepository.save(vehiculo);

        return mapper.toDto(ventaRepository.save(venta));
    }

    @Override
    public List<VentaResponseDTO> listar() {
        return ventaRepository.findAllByOrderByFechaVentaDesc().stream().map(mapper::toDto).toList();
    }

    @Override
    public VentaResponseDTO buscarPorId(Long id) {
        return ventaRepository.findWithDetalleById(id).map(mapper::toDto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }

    /** Si el precio SUPERA el umbral (> 100.000.000) se aplica el 5 % de descuento. */
    BigDecimal calcularDescuento(BigDecimal precio) {
        if (precio.compareTo(umbralDescuento) > 0) {
            return precio.multiply(porcentajeDescuento)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2);
    }
}
