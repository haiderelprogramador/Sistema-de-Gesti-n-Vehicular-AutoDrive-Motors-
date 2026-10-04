package com.autodrive.dto.response;

import java.time.LocalDateTime;

public record ClienteResponseDTO(
        Long id,
        String nombre,
        String apellido,
        String documento,
        String correo,
        String telefono,
        String direccion,
        LocalDateTime fechaRegistro
) {
}
