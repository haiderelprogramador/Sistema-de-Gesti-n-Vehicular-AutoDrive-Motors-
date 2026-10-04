package com.autodrive.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * La fecha y el total NO se reciben: el sistema los genera automáticamente (Regla 5).
 */
public record VentaRequestDTO(

        @NotNull(message = "El id del cliente es obligatorio")
        @Positive(message = "El id del cliente debe ser positivo")
        Long clienteId,

        @NotNull(message = "El id del vehículo es obligatorio")
        @Positive(message = "El id del vehículo debe ser positivo")
        Long vehiculoId,

        @Size(max = 30, message = "El método de pago no puede superar 30 caracteres")
        String metodoPago
) {
}
