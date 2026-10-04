package com.autodrive.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * copUsd: cuántos dólares vale 1 peso.  usdCop: cuántos pesos vale 1 dólar.
 */
public record TasaCambioDTO(
        String base,
        String destino,
        BigDecimal copUsd,
        BigDecimal usdCop,
        String ultimaActualizacionFuente,
        String fuente,
        LocalDateTime fechaConsulta
) {
}
