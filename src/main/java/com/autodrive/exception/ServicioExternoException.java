package com.autodrive.exception;

/** 503 - La API externa de tasas de cambio no respondió correctamente. */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }
}
