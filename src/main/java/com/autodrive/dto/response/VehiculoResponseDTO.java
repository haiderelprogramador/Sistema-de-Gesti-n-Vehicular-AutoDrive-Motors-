package com.autodrive.dto.response;

import com.autodrive.model.enums.EstadoVehiculo;

import java.math.BigDecimal;

public record VehiculoResponseDTO(
        Long id,
        String placa,
        String marca,
        String modelo,
        Integer anio,
        String color,
        BigDecimal precio,
        EstadoVehiculo estado
) {
}
