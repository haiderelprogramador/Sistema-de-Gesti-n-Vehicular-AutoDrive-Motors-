package com.autodrive.service.impl;

import com.autodrive.dto.request.ClienteRequestDTO;
import com.autodrive.dto.response.ClienteResponseDTO;
import com.autodrive.dto.response.VentaResponseDTO;
import com.autodrive.exception.RecursoDuplicadoException;
import com.autodrive.exception.RecursoNoEncontradoException;
import com.autodrive.exception.ReglaNegocioException;
import com.autodrive.mapper.EntityMapper;
import com.autodrive.model.entity.Cliente;
import com.autodrive.repository.ClienteRepository;
import com.autodrive.repository.VentaRepository;
import com.autodrive.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final EntityMapper mapper;

    @Override
    public List<ClienteResponseDTO> listar() {
        return clienteRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public ClienteResponseDTO buscarPorId(Long id) {
        return mapper.toDto(obtener(id));
    }

    @Override
    @Transactional
    public ClienteResponseDTO registrar(ClienteRequestDTO dto) {
        // Regla 4: correo único
        if (clienteRepository.existsByCorreoIgnoreCase(dto.correo().trim())) {
            throw new RecursoDuplicadoException("Ya existe un cliente registrado con el correo " + dto.correo());
        }
        if (clienteRepository.existsByDocumento(dto.documento().trim())) {
            throw new RecursoDuplicadoException("Ya existe un cliente registrado con el documento " + dto.documento());
        }
        Cliente guardado = clienteRepository.save(mapper.toEntity(dto));
        return mapper.toDto(guardado);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = obtener(id);
        if (clienteRepository.existsByCorreoIgnoreCaseAndIdNot(dto.correo().trim(), id)) {
            throw new RecursoDuplicadoException("El correo " + dto.correo() + " ya pertenece a otro cliente");
        }
        if (clienteRepository.existsByDocumentoAndIdNot(dto.documento().trim(), id)) {
            throw new RecursoDuplicadoException("El documento " + dto.documento() + " ya pertenece a otro cliente");
        }
        mapper.updateEntity(cliente, dto);
        return mapper.toDto(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtener(id);
        // No se borra un cliente con ventas: se perdería el historial de ventas
        if (ventaRepository.existsByClienteId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el cliente porque tiene ventas registradas");
        }
        clienteRepository.delete(cliente);
    }

    @Override
    public List<VentaResponseDTO> historialCompras(Long id) {
        obtener(id);
        return ventaRepository.findByClienteIdOrderByFechaVentaDesc(id).stream().map(mapper::toDto).toList();
    }

    private Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }
}
