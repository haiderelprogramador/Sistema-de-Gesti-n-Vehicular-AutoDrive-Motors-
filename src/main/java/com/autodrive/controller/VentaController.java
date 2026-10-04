package com.autodrive.controller;

import com.autodrive.dto.request.VentaRequestDTO;
import com.autodrive.dto.response.VentaResponseDTO;
import com.autodrive.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrar(@Valid @RequestBody VentaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(dto));
    }

    @GetMapping
    public List<VentaResponseDTO> listar() {
        return ventaService.listar();
    }

    @GetMapping("/{id}")
    public VentaResponseDTO buscarPorId(@PathVariable Long id) {
        return ventaService.buscarPorId(id);
    }
}
