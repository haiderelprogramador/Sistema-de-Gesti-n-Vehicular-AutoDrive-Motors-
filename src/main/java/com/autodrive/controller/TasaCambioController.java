package com.autodrive.controller;

import com.autodrive.client.ExchangeRateClient;
import com.autodrive.dto.response.TasaCambioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasa-cambio")
@RequiredArgsConstructor
public class TasaCambioController {

    private final ExchangeRateClient exchangeRateClient;

    /** Tasa de cambio actual COP -> USD consultada a la API externa. */
    @GetMapping
    public TasaCambioDTO actual() {
        return exchangeRateClient.obtenerTasaCopUsd();
    }
}
