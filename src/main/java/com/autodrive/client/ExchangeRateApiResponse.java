package com.autodrive.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Mapea el JSON que devuelve https://open.er-api.com/v6/latest/COP
 * Ejemplo: { "result": "success", "base_code": "COP", "rates": { "USD": 0.00024, ... } }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiResponse(
        String result,
        @JsonProperty("base_code") String baseCode,
        @JsonProperty("time_last_update_utc") String timeLastUpdateUtc,
        Map<String, BigDecimal> rates
) {
}
