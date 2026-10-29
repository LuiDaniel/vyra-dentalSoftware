package com.vyra.dental.controller;

/**
 * La implementa el controlador de una pantalla que se abre con un dato, por
 * ejemplo el odontograma de un paciente: {@code main.navegar(Pantalla.ODONTOGRAMA, paciente)}.
 */
public interface RecibeParametro {

    void recibir(Object parametro);
}
