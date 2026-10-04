package com.autodrive.service;

import com.autodrive.dto.request.VentaRequestDTO;
import com.autodrive.dto.response.VentaResponseDTO;

import java.util.List;

public interface VentaService {

    VentaResponseDTO registrar(VentaRequestDTO dto);

    List<VentaResponseDTO> listar();

    VentaResponseDTO buscarPorId(Long id);
}
