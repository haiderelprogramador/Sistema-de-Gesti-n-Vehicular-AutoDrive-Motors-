package com.autodrive.controller;

import com.autodrive.dto.request.MantenimientoRequestDTO;
import com.autodrive.dto.response.MantenimientoResponseDTO;
import com.autodrive.service.MantenimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mantenimientos")
@RequiredArgsConstructor
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    @PostMapping
    public ResponseEntity<MantenimientoResponseDTO> registrar(@Valid @RequestBody MantenimientoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mantenimientoService.registrar(dto));
    }

    @GetMapping
    public List<MantenimientoResponseDTO> listar() {
        return mantenimientoService.listar();
    }

    @GetMapping("/{id}")
    public MantenimientoResponseDTO buscarPorId(@PathVariable Long id) {
        return mantenimientoService.buscarPorId(id);
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public List<MantenimientoResponseDTO> historialPorVehiculo(@PathVariable Long vehiculoId) {
        return mantenimientoService.historialPorVehiculo(vehiculoId);
    }

    /** Cierra el mantenimiento y devuelve el vehículo al estado DISPONIBLE. */
    @PutMapping("/{id}/finalizar")
    public MantenimientoResponseDTO finalizar(@PathVariable Long id) {
        return mantenimientoService.finalizar(id);
    }
}
