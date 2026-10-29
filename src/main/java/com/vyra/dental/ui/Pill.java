package com.vyra.dental.ui;

import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.MetodoPago;

import javafx.scene.control.Label;

/**
 * Píldora de estado: siempre texto + color (docs/08-guia-frontend.md §1).
 */
public class Pill extends Label {

    public Pill(String texto, String claseCss) {
        super(texto);
        getStyleClass().addAll("pill", claseCss);
        // El estado se lee completo: la píldora nunca se recorta con "..."
        setMinWidth(USE_PREF_SIZE);
    }

    public Pill(EstadoCita estado) {
        this(estado.getEtiqueta(), estado.getClaseCss());
    }

    public Pill(EstadoTratamiento estado) {
        this(estado.getEtiqueta(), estado.getClaseCss());
    }

    public Pill(MetodoPago metodo) {
        this(metodo.getEtiqueta(), "pill-neutra");
    }
}
