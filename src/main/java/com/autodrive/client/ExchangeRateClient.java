package com.autodrive.client;

import com.autodrive.dto.response.TasaCambioDTO;
import com.autodrive.exception.ServicioExternoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Consume la API pública de tasas de cambio.
 * Guarda la última tasa en memoria durante 10 minutos para no consultar la API en cada petición.
 */
@Slf4j
@Component
public class ExchangeRateClient {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String FUENTE = "open.er-api.com (ExchangeRate-API)";

    private final RestClient restClient;
    private final String apiUrl;

    private volatile TasaCambioDTO cache;

    public ExchangeRateClient(RestClient exchangeRestClient,
                              @Value("${exchange.api.url}") String apiUrl) {
        this.restClient = exchangeRestClient;
        this.apiUrl = apiUrl;
    }

    public TasaCambioDTO obtenerTasaCopUsd() {
        TasaCambioDTO actual = cache;
        if (actual != null && actual.fechaConsulta().plus(CACHE_TTL).isAfter(LocalDateTime.now())) {
            return actual;
        }

        ExchangeRateApiResponse resp;
        try {
            resp = restClient.get().uri(apiUrl).retrieve().body(ExchangeRateApiResponse.class);
        } catch (RestClientException ex) {
            log.warn("No se pudo consultar la API de tasas: {}", ex.getMessage());
            throw new ServicioExternoException(
                    "No fue posible consultar la tasa de cambio en este momento. Intente más tarde.", ex);
        }

        if (resp == null || !"success".equalsIgnoreCase(resp.result())
                || resp.rates() == null || resp.rates().get("USD") == null) {
            throw new ServicioExternoException("La API de tasas de cambio devolvió una respuesta inválida");
        }

        BigDecimal copUsd = resp.rates().get("USD");
        BigDecimal usdCop = BigDecimal.ONE.divide(copUsd, 2, RoundingMode.HALF_UP);

        TasaCambioDTO nueva = new TasaCambioDTO("COP", "USD", copUsd, usdCop,
                resp.timeLastUpdateUtc(), FUENTE, LocalDateTime.now());
        cache = nueva;
        return nueva;
    }
}
