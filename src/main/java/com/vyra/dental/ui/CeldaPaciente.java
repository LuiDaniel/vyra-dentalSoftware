package com.vyra.dental.ui;

import com.vyra.dental.model.Paciente;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Celda de tabla con avatar + nombre + "DNI 72481935" (docs/08-guia-frontend.md §7.4).
 * {@code S} es el tipo de fila: Paciente, o un resumen que trae al paciente.
 */
public class CeldaPaciente<S> extends TableCell<S, Paciente> {

    private final Avatar avatar = new Avatar();
    private final Label nombre = new Label();
    private final Label dni = new Label();
    private final HBox contenido = new HBox(avatar, new VBox(nombre, dni));

    public CeldaPaciente() {
        nombre.getStyleClass().add("celda-titulo");
        dni.getStyleClass().add("celda-sub");
        contenido.getStyleClass().add("celda-paciente");
        setText(null);
    }

    @Override
    protected void updateItem(Paciente paciente, boolean vacio) {
        super.updateItem(paciente, vacio);
        if (vacio || paciente == null) {
            setGraphic(null);
            return;
        }
        avatar.mostrar(paciente.getIniciales(), paciente.getIdPaciente());
        nombre.setText(paciente.getNombreCompleto());
        dni.setText("DNI " + paciente.getDni());
        setGraphic(contenido);
    }
}
