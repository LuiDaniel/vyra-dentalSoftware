package com.vyra.dental.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Aviso en línea (dentro de un formulario o tarjeta), con ícono y texto.
 * Se oculta con {@link #ocultar()} sin dejar el hueco en el diseño.
 * En FXML: {@code <Aviso fx:id="avisoError" tipo="ERROR"/>}.
 */
public class Aviso extends HBox {

    public enum Tipo {
        INFO("aviso-info", "info"),
        EXITO("aviso-exito", "check"),
        ADVERTENCIA("aviso-advertencia", "alerta"),
        ERROR("aviso-error", "error");

        private final String claseCss;
        private final String icono;

        Tipo(String claseCss, String icono) {
            this.claseCss = claseCss;
            this.icono = icono;
        }
    }

    private final Label texto = new Label();
    private Tipo tipo;

    public Aviso() {
        this(Tipo.INFO, "");
        ocultar();
    }

    public Aviso(Tipo tipo, String mensaje) {
        getStyleClass().add("aviso");
        setAlignment(Pos.TOP_LEFT);
        texto.setWrapText(true);
        texto.getStyleClass().add("aviso-texto");
        texto.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(texto, Priority.ALWAYS);
        setTipo(tipo);
        setTexto(mensaje);
    }

    public final void setTipo(Tipo nuevo) {
        if (tipo != null) {
            getStyleClass().remove(tipo.claseCss);
        }
        tipo = nuevo;
        getStyleClass().add(nuevo.claseCss);
        getChildren().setAll(new Icono(nuevo.icono, 18), texto);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public final void setTexto(String mensaje) {
        texto.setText(mensaje);
    }

    public String getTexto() {
        return texto.getText();
    }

    /** Muestra el aviso con el tipo y el mensaje dados. */
    public void mostrar(Tipo nuevoTipo, String mensaje) {
        setTipo(nuevoTipo);
        setTexto(mensaje);
        setVisible(true);
        setManaged(true);
    }

    public void ocultar() {
        setVisible(false);
        setManaged(false);
    }
}
