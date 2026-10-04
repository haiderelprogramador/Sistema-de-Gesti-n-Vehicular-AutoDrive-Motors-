package com.autodrive.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VehiculoRequestDTO(

        // Formato colombiano: ABC123 (carros) o ABC12D (motos)
        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^[A-Za-z]{3}-?[0-9]{2}[0-9A-Za-z]$",
                message = "La placa debe tener el formato ABC123 o ABC12D")
        String placa,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 50, message = "La marca no puede superar 50 caracteres")
        String marca,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 50, message = "El modelo no puede superar 50 caracteres")
        String modelo,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 1950, message = "El año debe ser mayor o igual a 1950")
        @Max(value = 2100, message = "El año no es válido")
        Integer anio,

        @Size(max = 30, message = "El color no puede superar 30 caracteres")
        String color,

        // Regla 2: no se permiten precios negativos
        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "El precio tiene un formato inválido")
        BigDecimal precio
) {
}
