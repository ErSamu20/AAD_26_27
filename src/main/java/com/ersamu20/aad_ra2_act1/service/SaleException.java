package com.ersamu20.aad_ra2_act1.service;

/**
 * Error de negocio al procesar una venta (datos inválidos, tienda inexistente o fallo en la transacción).
 * Extiende RuntimeException: es una excepción no comprobada, así que no obliga a declarar throws.
 */
public class SaleException extends RuntimeException {

    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }
}