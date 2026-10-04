package com.autodrive.service;

import com.autodrive.dto.request.ClienteRequestDTO;
import com.autodrive.dto.response.ClienteResponseDTO;
import com.autodrive.dto.response.VentaResponseDTO;

import java.util.List;

public interface ClienteService {

    List<ClienteResponseDTO> listar();

    ClienteResponseDTO buscarPorId(Long id);

    ClienteResponseDTO registrar(ClienteRequestDTO dto);

    ClienteResponseDTO actualizar(Long id, ClienteRequestDTO dto);

    void eliminar(Long id);

    List<VentaResponseDTO> historialCompras(Long id);
}
