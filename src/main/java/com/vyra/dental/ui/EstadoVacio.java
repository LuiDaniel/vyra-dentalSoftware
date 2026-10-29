package com.vyra.dental.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Mensaje para una lista sin datos ("Aún no hay pacientes registrados" +
 * botón para crear el primero), docs/08-guia-frontend.md §10.
 */
public class EstadoVacio extends VBox {

    public EstadoVacio(String titulo, String detalle) {
        this(titulo, detalle, null);
    }

    /** {@code accion} puede ser null si no hay nada que ofrecer. */
    public EstadoVacio(String titulo, String detalle, Button accion) {
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("card-titulo");
        Label detalleLabel = new Label(detalle);
        detalleLabel.getStyleClass().add("texto-secundario");
        detalleLabel.setWrapText(true);
        getChildren().addAll(tituloLabel, detalleLabel);
        if (accion != null) {
            getChildren().add(accion);
        }
        getStyleClass().add("estado-vacio");
        setAlignment(Pos.CENTER);
    }
}
