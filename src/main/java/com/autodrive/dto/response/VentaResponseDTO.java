package com.autodrive.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResponseDTO(
        Long id,
        LocalDateTime fechaVenta,
        Long clienteId,
        String clienteNombre,
        String clienteCorreo,
        Long vehiculoId,
        String vehiculoPlaca,
        String vehiculoDescripcion,
        BigDecimal precioBase,
        boolean descuentoAplicado,
        BigDecimal descuento,
        BigDecimal total,
        String metodoPago
) {
}
