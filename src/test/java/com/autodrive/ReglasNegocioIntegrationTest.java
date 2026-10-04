package com.autodrive;

import com.autodrive.client.ExchangeRateClient;
import com.autodrive.dto.response.TasaCambioDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReglasNegocioIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockBean
    ExchangeRateClient exchangeRateClient;

    private long crearCliente(String correo, String doc) throws Exception {
        String body = """
                {"nombre":"Ana","apellido":"Pérez","documento":"%s","correo":"%s","telefono":"3001234567"}
                """.formatted(doc, correo);
        String res = mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return om.readTree(res).get("id").asLong();
    }

    private long crearVehiculo(String placa, String precio) throws Exception {
        String body = """
                {"placa":"%s","marca":"Toyota","modelo":"Corolla","anio":2024,"color":"Blanco","precio":%s}
                """.formatted(placa, precio);
        String res = mvc.perform(post("/vehiculos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return om.readTree(res).get("id").asLong();
    }

    private String venta(long cliente, long vehiculo) {
        return "{\"clienteId\":" + cliente + ",\"vehiculoId\":" + vehiculo + ",\"metodoPago\":\"Transferencia\"}";
    }

    @Test
    void ventaConDescuentoYCambioDeEstado() throws Exception {
        long c = crearCliente("ana@mail.com", "1010101010");
        long v = crearVehiculo("ABC123", "120000000");

        mvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(c, v)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descuentoAplicado").value(true))
                .andExpect(jsonPath("$.descuento").value(6000000.00))
                .andExpect(jsonPath("$.total").value(114000000.00))
                .andExpect(jsonPath("$.fechaVenta").exists());

        mvc.perform(get("/vehiculos/" + v)).andExpect(jsonPath("$.estado").value("VENDIDO"));

        // Regla 1: no se puede vender un vehículo vendido (evita ventas duplicadas)
        mvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(c, v)))
                .andExpect(status().isConflict());
    }

    @Test
    void ventaSinDescuentoEnElLimite() throws Exception {
        long c = crearCliente("b@mail.com", "2020202020");
        long v = crearVehiculo("XYZ789", "100000000");
        mvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(c, v)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descuentoAplicado").value(false))
                .andExpect(jsonPath("$.total").value(100000000.00));
    }

    @Test
    void noSeVendeEnMantenimientoYFinalizarLoLibera() throws Exception {
        long c = crearCliente("c@mail.com", "3030303030");
        long v = crearVehiculo("MNO456", "50000000");

        String mant = "{\"vehiculoId\":" + v + ",\"tipo\":\"PREVENTIVO\",\"descripcion\":\"Cambio de aceite\",\"costo\":250000}";
        String res = mvc.perform(post("/mantenimientos").contentType(MediaType.APPLICATION_JSON).content(mant))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoVehiculo").value("EN_MANTENIMIENTO"))
                .andReturn().getResponse().getContentAsString();
        long mantId = om.readTree(res).get("id").asLong();

        mvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(c, v)))
                .andExpect(status().isConflict());
        mvc.perform(get("/vehiculos/disponibles")).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(put("/mantenimientos/" + mantId + "/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.estadoVehiculo").value("DISPONIBLE"));

        mvc.perform(get("/mantenimientos/vehiculo/" + v)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void validacionesYUnicidad() throws Exception {
        crearCliente("d@mail.com", "4040404040");
        crearVehiculo("DEF111", "30000000");

        // Regla 4: correo repetido
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"nombre\":\"Otro\",\"apellido\":\"Cliente\",\"documento\":\"5050505050\",\"correo\":\"D@mail.com\"}"))
                .andExpect(status().isConflict());

        // Regla 3: placa repetida (sin importar mayúsculas o guion)
        mvc.perform(post("/vehiculos").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"placa\":\"def-111\",\"marca\":\"Mazda\",\"modelo\":\"3\",\"anio\":2023,\"precio\":1000}"))
                .andExpect(status().isConflict());

        // Regla 2: precio negativo
        mvc.perform(post("/vehiculos").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"placa\":\"GHI222\",\"marca\":\"Mazda\",\"modelo\":\"3\",\"anio\":2023,\"precio\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());

        mvc.perform(get("/vehiculos/marca/toyota")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/clientes/999")).andExpect(status().isNotFound());
    }

    @Test
    void conversionCopUsd() throws Exception {
        when(exchangeRateClient.obtenerTasaCopUsd()).thenReturn(new TasaCambioDTO(
                "COP", "USD", new BigDecimal("0.00025"), new BigDecimal("4000.00"),
                "hoy", "mock", LocalDateTime.now()));
        long v = crearVehiculo("JKL333", "80000000");

        mvc.perform(get("/vehiculos/" + v + "/precio-usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUsd").value(20000.00));
    }

    @Test
    void noSeEliminaClienteConVentas() throws Exception {
        long c = crearCliente("e@mail.com", "6060606060");
        long v = crearVehiculo("PQR444", "40000000");
        mvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON).content(venta(c, v)));
        mvc.perform(delete("/clientes/" + c)).andExpect(status().isConflict());
        JsonNode reporte = om.readTree(mvc.perform(get("/reportes/resumen"))
                .andReturn().getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(1, reporte.get("totalVentas").asInt());
    }
}
