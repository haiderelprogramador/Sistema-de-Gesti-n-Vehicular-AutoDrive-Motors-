package com.autodrive.dto.response;

import java.math.BigDecimal;

/**
 * Reporte rápido para la gerencia (problema: "dificultad para generar reportes rápidos").
 */
public record ReporteResumenDTO(
        long totalClientes,
        long totalVehiculos,
        long vehiculosDisponibles,
        long vehiculosVendidos,
        long vehiculosEnMantenimiento,
        long totalVentas,
        BigDecimal ingresosTotales,
        BigDecimal descuentosOtorgados,
        long mantenimientosEnProceso
) {
}
