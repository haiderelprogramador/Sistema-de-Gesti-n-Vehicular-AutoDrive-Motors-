package com.autodrive.controller;

import com.autodrive.dto.request.VehiculoRequestDTO;
import com.autodrive.dto.response.MantenimientoResponseDTO;
import com.autodrive.dto.response.PrecioConvertidoDTO;
import com.autodrive.dto.response.VehiculoResponseDTO;
import com.autodrive.service.MantenimientoService;
import com.autodrive.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final MantenimientoService mantenimientoService;

    @GetMapping
    public List<VehiculoResponseDTO> listar() {
        return vehiculoService.listar();
    }

    @GetMapping("/disponibles")
    public List<VehiculoResponseDTO> disponibles() {
        return vehiculoService.listarDisponibles();
    }

    @GetMapping("/marca/{marca}")
    public List<VehiculoResponseDTO> porMarca(@PathVariable String marca) {
        return vehiculoService.buscarPorMarca(marca);
    }

    @GetMapping("/{id}")
    public VehiculoResponseDTO buscarPorId(@PathVariable Long id) {
        return vehiculoService.buscarPorId(id);
    }

    /** Consume la API externa y convierte el precio del vehículo de COP a USD. */
    @GetMapping("/{id}/precio-usd")
    public PrecioConvertidoDTO precioUsd(@PathVariable Long id) {
        return vehiculoService.precioEnDolares(id);
    }

    @GetMapping("/{id}/mantenimientos")
    public List<MantenimientoResponseDTO> historialMantenimientos(@PathVariable Long id) {
        return mantenimientoService.historialPorVehiculo(id);
    }

    @PostMapping
    public ResponseEntity<VehiculoResponseDTO> registrar(@Valid @RequestBody VehiculoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.registrar(dto));
    }

    @PutMapping("/{id}")
    public VehiculoResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequestDTO dto) {
        return vehiculoService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
