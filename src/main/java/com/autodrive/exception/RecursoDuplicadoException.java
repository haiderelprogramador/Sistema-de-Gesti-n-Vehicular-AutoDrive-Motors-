package com.autodrive.exception;

/** 409 - Se intenta registrar un dato único que ya existe (placa, correo, documento). */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
