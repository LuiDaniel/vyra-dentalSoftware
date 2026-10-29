package com.vyra.dental.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Confirmación de acciones destructivas ("¿Dar de baja a ...?"). Es el único
 * uso de ventanas emergentes: los errores van junto al campo
 * (docs/05-arquitectura.md §7).
 */
public final class Dialogos {

    private Dialogos() {
    }

    /**
     * Devuelve true si el usuario confirma. Enter cancela: la acción
     * destructiva hay que elegirla a propósito.
     */
    public static boolean confirmar(Window ventana, String titulo, String pregunta, String detalle, String accion) {
        ButtonType confirmar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, detalle, confirmar, cancelar);
        alerta.initOwner(ventana);
        alerta.setTitle(titulo);
        alerta.setHeaderText(pregunta);
        alerta.setGraphic(null);
        // Ancho fijo y alto según el texto, para que el mensaje no se corte
        alerta.getDialogPane().setPrefWidth(480);
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alerta.getDialogPane().getStylesheets().addAll(ventana.getScene().getStylesheets());
        alerta.getDialogPane().getStyleClass().add("dialogo");

        Button botonConfirmar = (Button) alerta.getDialogPane().lookupButton(confirmar);
        Button botonCancelar = (Button) alerta.getDialogPane().lookupButton(cancelar);
        botonConfirmar.getStyleClass().add("btn-peligro");
        botonCancelar.getStyleClass().add("btn-secundario");
        botonConfirmar.setDefaultButton(false);
        botonCancelar.setDefaultButton(true);

        return alerta.showAndWait().orElse(cancelar) == confirmar;
    }
}
