package com.autodrive.dto.response;

import com.autodrive.model.enums.EstadoMantenimiento;
import com.autodrive.model.enums.EstadoVehiculo;
import com.autodrive.model.enums.TipoMantenimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MantenimientoResponseDTO(
        Long id,
        Long vehiculoId,
        String vehiculoPlaca,
        EstadoVehiculo estadoVehiculo,
        TipoMantenimiento tipo,
        String descripcion,
        BigDecimal costo,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        EstadoMantenimiento estado
) {
}
