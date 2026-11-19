package com.vyra.dental.pasarela;

/** Un problema al hablar con la pasarela, con un mensaje listo para mostrar. */
public class PasarelaException extends RuntimeException {

    public PasarelaException(String mensaje) {
        super(mensaje);
    }

    public PasarelaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
