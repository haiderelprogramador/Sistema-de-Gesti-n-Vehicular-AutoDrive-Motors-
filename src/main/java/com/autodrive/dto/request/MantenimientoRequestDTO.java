package com.autodrive.dto.request;

import com.autodrive.model.enums.TipoMantenimiento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MantenimientoRequestDTO(

        @NotNull(message = "El id del vehículo es obligatorio")
        @Positive(message = "El id del vehículo debe ser positivo")
        Long vehiculoId,

        @NotNull(message = "El tipo de mantenimiento es obligatorio (PREVENTIVO, CORRECTIVO o REVISION)")
        TipoMantenimiento tipo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El costo es obligatorio")
        @PositiveOrZero(message = "El costo no puede ser negativo")
        BigDecimal costo,

        // Opcional: si no se envía se usa la fecha actual
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
        LocalDate fechaIngreso
) {
}
