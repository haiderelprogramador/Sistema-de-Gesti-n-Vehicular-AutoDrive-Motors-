package com.autodrive.service;

import com.autodrive.dto.request.MantenimientoRequestDTO;
import com.autodrive.dto.response.MantenimientoResponseDTO;

import java.util.List;

public interface MantenimientoService {

    MantenimientoResponseDTO registrar(MantenimientoRequestDTO dto);

    List<MantenimientoResponseDTO> listar();

    MantenimientoResponseDTO buscarPorId(Long id);

    List<MantenimientoResponseDTO> historialPorVehiculo(Long vehiculoId);

    MantenimientoResponseDTO finalizar(Long id);
}
