package com.autodrive.exception;

/** 409 - La operación viola una regla de negocio (ej. vender un vehículo vendido). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
