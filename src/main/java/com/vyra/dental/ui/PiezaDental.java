package com.vyra.dental.ui;

import com.vyra.dental.model.EstadoPieza;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Pieza del odontograma (docs/08-guia-frontend.md §7.6): número, diente y la
 * letra del estado, para que el estado no dependa solo del color. Es un
 * ToggleButton para que la pieza elegida quede marcada ({@code .pieza:selected}).
 * En la arcada superior el diente va con la raíz hacia arriba, el número
 * arriba y la letra abajo; en la inferior, al revés.
 */
public class PiezaDental extends ToggleButton {

    private static final PseudoClass PENDIENTE = PseudoClass.getPseudoClass("pendiente");

    private final int numero;
    private final Label letra = new Label();
    private final Icono diente = new Icono("diente", 40);
    private final Icono cruz = new Icono("cruz-pieza", 40);
    private EstadoPieza estado;
    private boolean pendiente;

    public PiezaDental(int numero, boolean superior) {
        this.numero = numero;
        Label numeroLabel = new Label(String.valueOf(numero));
        numeroLabel.getStyleClass().add("pieza-numero");
        letra.getStyleClass().add("pieza-letra");

        diente.getStyleClass().add("pieza-diente");
        cruz.getStyleClass().add("pieza-cruz");
        if (superior) {
            diente.setRotate(180);
        }
        StackPane dibujo = new StackPane(diente, cruz);
        dibujo.getStyleClass().add("pieza-dibujo");

        VBox contenido = superior ? new VBox(numeroLabel, dibujo, letra) : new VBox(letra, dibujo, numeroLabel);
        contenido.setAlignment(Pos.CENTER);
        contenido.getStyleClass().add("pieza-contenido");
        setGraphic(contenido);
        setText(null);
        getStyleClass().add("pieza");
        setEstado(EstadoPieza.SANA);
    }

    public int getNumero() {
        return numero;
    }

    public EstadoPieza getEstado() {
        return estado;
    }

    /** Cambia los colores, la letra y la X de extraída. */
    public final void setEstado(EstadoPieza nuevo) {
        if (estado != null) {
            getStyleClass().remove(estado.getClaseCss());
        }
        estado = nuevo;
        getStyleClass().add(nuevo.getClaseCss());
        letra.setText(nuevo.getCodigo());
        cruz.setVisible(nuevo == EstadoPieza.EXTRAIDA);
        actualizarTextoAccesible();
    }

    /** Marca la pieza con un cambio sin guardar ({@code .pieza:pendiente}). */
    public void setPendiente(boolean valor) {
        pendiente = valor;
        pseudoClassStateChanged(PENDIENTE, valor);
        actualizarTextoAccesible();
    }

    private void actualizarTextoAccesible() {
        setAccessibleText("Pieza " + numero + ", " + estado.getEtiqueta() + (pendiente ? ", sin guardar" : ""));
    }
}
