package com.autodrive.service;

import com.autodrive.dto.response.ReporteResumenDTO;
import com.autodrive.model.entity.Venta;
import com.autodrive.model.enums.EstadoMantenimiento;
import com.autodrive.model.enums.EstadoVehiculo;
import com.autodrive.model.entity.Mantenimiento;
import com.autodrive.repository.ClienteRepository;
import com.autodrive.repository.MantenimientoRepository;
import com.autodrive.repository.VehiculoRepository;
import com.autodrive.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final VentaRepository ventaRepository;
    private final MantenimientoRepository mantenimientoRepository;

    public ReporteResumenDTO resumen() {
        List<Venta> ventas = ventaRepository.findAll();
        BigDecimal ingresos = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuentos = ventas.stream().map(Venta::getDescuento).reduce(BigDecimal.ZERO, BigDecimal::add);
        long mantEnProceso = mantenimientoRepository.findAll().stream()
                .map(Mantenimiento::getEstado).filter(e -> e == EstadoMantenimiento.EN_PROCESO).count();

        return new ReporteResumenDTO(
                clienteRepository.count(),
                vehiculoRepository.count(),
                vehiculoRepository.findByEstado(EstadoVehiculo.DISPONIBLE).size(),
                vehiculoRepository.findByEstado(EstadoVehiculo.VENDIDO).size(),
                vehiculoRepository.findByEstado(EstadoVehiculo.EN_MANTENIMIENTO).size(),
                ventas.size(),
                ingresos,
                descuentos,
                mantEnProceso);
    }
}
