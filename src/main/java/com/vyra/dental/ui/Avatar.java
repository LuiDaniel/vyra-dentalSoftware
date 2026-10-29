package com.vyra.dental.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Círculo con las iniciales. El color sale del id ({@code id % 7}), así la
 * misma persona siempre tiene el mismo color (clases .avatar-0 a .avatar-6).
 * Para el tamaño grande de los encabezados se agrega la clase .avatar-grande.
 */
public class Avatar extends StackPane {

    private static final int COLORES = 7;

    private final Label iniciales = new Label();
    private String claseColor;

    public Avatar() {
        getStyleClass().add("avatar");
        iniciales.getStyleClass().add("avatar-iniciales");
        getChildren().add(iniciales);
        mostrar("", 0);
    }

    public Avatar(String iniciales, int id) {
        this();
        mostrar(iniciales, id);
    }

    public final void mostrar(String texto, int id) {
        iniciales.setText(texto);
        if (claseColor != null) {
            getStyleClass().remove(claseColor);
        }
        claseColor = "avatar-" + Math.floorMod(id, COLORES);
        getStyleClass().add(claseColor);
    }
}
