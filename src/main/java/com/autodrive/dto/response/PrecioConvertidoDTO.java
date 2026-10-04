package com.autodrive.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrecioConvertidoDTO(
        Long vehiculoId,
        String placa,
        String descripcion,
        BigDecimal precioCop,
        BigDecimal tasaCopUsd,
        BigDecimal precioUsd,
        String fuente,
        LocalDateTime fechaConsulta
) {
}
