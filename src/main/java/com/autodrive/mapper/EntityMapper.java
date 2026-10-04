package com.autodrive.mapper;

import com.autodrive.dto.request.ClienteRequestDTO;
import com.autodrive.dto.request.VehiculoRequestDTO;
import com.autodrive.dto.response.ClienteResponseDTO;
import com.autodrive.dto.response.MantenimientoResponseDTO;
import com.autodrive.dto.response.VehiculoResponseDTO;
import com.autodrive.dto.response.VentaResponseDTO;
import com.autodrive.model.entity.Cliente;
import com.autodrive.model.entity.Mantenimiento;
import com.autodrive.model.entity.Vehiculo;
import com.autodrive.model.entity.Venta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Convierte entre entidades JPA y DTOs. Las entidades nunca salen de la capa de servicio.
 */
@Component
public class EntityMapper {

    // ---------- Cliente ----------
    public Cliente toEntity(ClienteRequestDTO dto) {
        Cliente c = new Cliente();
        updateEntity(c, dto);
        return c;
    }

    public void updateEntity(Cliente c, ClienteRequestDTO dto) {
        c.setNombre(dto.nombre().trim());
        c.setApellido(dto.apellido().trim());
        c.setDocumento(dto.documento().trim());
        c.setCorreo(dto.correo().trim().toLowerCase());
        c.setTelefono(dto.telefono());
        c.setDireccion(dto.direccion());
    }

    public ClienteResponseDTO toDto(Cliente c) {
        return new ClienteResponseDTO(c.getId(), c.getNombre(), c.getApellido(), c.getDocumento(),
                c.getCorreo(), c.getTelefono(), c.getDireccion(), c.getFechaRegistro());
    }

    // ---------- Vehículo ----------
    public Vehiculo toEntity(VehiculoRequestDTO dto) {
        Vehiculo v = new Vehiculo();
        updateEntity(v, dto);
        return v;
    }

    public void updateEntity(Vehiculo v, VehiculoRequestDTO dto) {
        v.setPlaca(normalizarPlaca(dto.placa()));
        v.setMarca(capitalizar(dto.marca()));
        v.setModelo(dto.modelo().trim());
        v.setAnio(dto.anio());
        v.setColor(dto.color());
        v.setPrecio(dto.precio());
    }

    public VehiculoResponseDTO toDto(Vehiculo v) {
        return new VehiculoResponseDTO(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(),
                v.getAnio(), v.getColor(), v.getPrecio(), v.getEstado());
    }

    // ---------- Venta ----------
    public VentaResponseDTO toDto(Venta venta) {
        Cliente c = venta.getCliente();
        Vehiculo v = venta.getVehiculo();
        return new VentaResponseDTO(
                venta.getId(),
                venta.getFechaVenta(),
                c.getId(),
                c.getNombre() + " " + c.getApellido(),
                c.getCorreo(),
                v.getId(),
                v.getPlaca(),
                descripcion(v),
                venta.getPrecioBase(),
                venta.getDescuento().compareTo(BigDecimal.ZERO) > 0,
                venta.getDescuento(),
                venta.getTotal(),
                venta.getMetodoPago());
    }

    // ---------- Mantenimiento ----------
    public MantenimientoResponseDTO toDto(Mantenimiento m) {
        Vehiculo v = m.getVehiculo();
        return new MantenimientoResponseDTO(m.getId(), v.getId(), v.getPlaca(), v.getEstado(),
                m.getTipo(), m.getDescripcion(), m.getCosto(), m.getFechaIngreso(),
                m.getFechaSalida(), m.getEstado());
    }

    // ---------- utilidades ----------
    public static String normalizarPlaca(String placa) {
        return placa.trim().replace("-", "").toUpperCase();
    }

    public static String descripcion(Vehiculo v) {
        return v.getMarca() + " " + v.getModelo() + " " + v.getAnio();
    }

    private static String capitalizar(String s) {
        String t = s.trim();
        return t.isEmpty() ? t : t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
    }
}
