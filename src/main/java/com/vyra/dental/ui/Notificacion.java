package com.vyra.dental.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Mensaje breve de éxito (toast) en la esquina inferior derecha de la ventana.
 * Desaparece solo a los 3 segundos: {@code Notificacion.mostrar(ventana, "Paciente registrado")}.
 */
public final class Notificacion {

    private static final double MARGEN = 24;

    private Notificacion() {
    }

    public static void mostrar(Window ventana, String mensaje) {
        Label texto = new Label(mensaje);
        texto.getStyleClass().add("notificacion-texto");
        HBox caja = new HBox(10, new Icono("check", 18), texto);
        caja.getStyleClass().add("notificacion");
        // Un Popup no hereda las hojas de estilo de la ventana
        caja.getStylesheets().addAll(ventana.getScene().getStylesheets());

        Popup popup = new Popup();
        popup.getContent().add(caja);
        popup.setAutoHide(false);
        popup.show(ventana);
        // El tamaño del contenido se conoce recién después de mostrarlo
        popup.setX(ventana.getX() + ventana.getWidth() - caja.getWidth() - MARGEN);
        popup.setY(ventana.getY() + ventana.getHeight() - caja.getHeight() - MARGEN);

        PauseTransition espera = new PauseTransition(Duration.seconds(2.6));
        FadeTransition salida = new FadeTransition(Duration.millis(400), caja);
        salida.setToValue(0);
        SequentialTransition animacion = new SequentialTransition(espera, salida);
        animacion.setOnFinished(e -> popup.hide());
        animacion.play();
    }
}
