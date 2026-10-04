package com.autodrive.service;

import com.autodrive.dto.request.VehiculoRequestDTO;
import com.autodrive.dto.response.PrecioConvertidoDTO;
import com.autodrive.dto.response.VehiculoResponseDTO;

import java.util.List;

public interface VehiculoService {

    List<VehiculoResponseDTO> listar();

    VehiculoResponseDTO buscarPorId(Long id);

    VehiculoResponseDTO registrar(VehiculoRequestDTO dto);

    VehiculoResponseDTO actualizar(Long id, VehiculoRequestDTO dto);

    void eliminar(Long id);

    List<VehiculoResponseDTO> buscarPorMarca(String marca);

    List<VehiculoResponseDTO> listarDisponibles();

    PrecioConvertidoDTO precioEnDolares(Long id);
}
