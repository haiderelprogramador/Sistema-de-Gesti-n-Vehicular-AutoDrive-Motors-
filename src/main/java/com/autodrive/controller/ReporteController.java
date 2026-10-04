package com.autodrive.controller;

import com.autodrive.dto.response.ReporteResumenDTO;
import com.autodrive.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/resumen")
    public ReporteResumenDTO resumen() {
        return reporteService.resumen();
    }
}
