package com.vyra.dental.ui;

import com.vyra.dental.model.Paciente;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

/** Celda de tabla con la píldora naranja de la alergia, o "Ninguna" en gris. */
public class CeldaAlergias extends TableCell<Paciente, Paciente> {

    @Override
    protected void updateItem(Paciente paciente, boolean vacio) {
        super.updateItem(paciente, vacio);
        setText(null);
        if (vacio || paciente == null) {
            setGraphic(null);
        } else if (paciente.tieneAlergias()) {
            Label pill = pillAlergia(paciente.getAlergias().lines().findFirst().orElse(""));
            pill.setTooltip(new Tooltip(paciente.getAlergias()));
            setGraphic(pill);
        } else {
            Label ninguna = new Label("Ninguna");
            ninguna.getStyleClass().add("texto-tenue");
            setGraphic(ninguna);
        }
    }

    /** Píldora naranja con el ícono de alerta, también para encabezados ("Alergia: Penicilina"). */
    public static Label pillAlergia(String texto) {
        Label pill = new Label(texto, new Icono("alerta", 12));
        pill.getStyleClass().addAll("pill", "pill-alergia");
        pill.setMinWidth(Label.USE_PREF_SIZE);
        return pill;
    }
}
