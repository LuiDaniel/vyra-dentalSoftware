package com.vyra.dental;

import javafx.application.Application;

/**
 * Punto de entrada de la versión instalada (jpackage, docs/09-plan-implementacion.md
 * fase 8). Las librerías van en el classpath porque jlink no acepta los módulos
 * automáticos (SQLite, Hibernate Validator, OpenPDF), y JavaFX exige que la
 * clase principal no herede de Application cuando corre desde el classpath.
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
