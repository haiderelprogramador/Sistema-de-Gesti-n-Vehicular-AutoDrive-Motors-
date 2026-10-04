package com.autodrive.model.entity;

import com.autodrive.model.enums.EstadoVehiculo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Regla 3: la placa del vehículo debe ser única
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 50)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(length = 30)
    private String color;

    // Regla 2: no se permiten precios negativos (validado en DTO y servicio)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVehiculo estado;

    @OneToOne(mappedBy = "vehiculo", fetch = FetchType.LAZY)
    private Venta venta;

    @OneToMany(mappedBy = "vehiculo", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Mantenimiento> mantenimientos = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (estado == null) {
            estado = EstadoVehiculo.DISPONIBLE;
        }
    }
}
