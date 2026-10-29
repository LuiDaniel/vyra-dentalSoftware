package com.vyra.dental.ui;

import com.vyra.dental.model.EstadoPieza;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;

/**
 * Una pieza del odontograma (docs/08-guia-frontend.md §7.6): número, diente de 40×48
 * y letra del estado. Los colores salen de vyra.css con la clase {@code .pieza-<estado>}.
 * Las piezas del maxilar superior (cuadrantes 1 y 2) se dibujan con la raíz hacia arriba.
 */
public class PiezaDental extends ToggleButton {

    private static final String TRAZO = "M12 5.5C10.5 4 8.5 3 6.5 3.5 3.5 4.3 3 7.5 3.8 10.5c.6 2.2 1.2 4 1.5 6.5.3 2.3.8 4 2 4 "
            + "1.7 0 1.7-5 4.7-5s3 5 4.7 5c1.2 0 1.7-1.7 2-4 .3-2.5.9-4.3 1.5-6.5.8-3 .3-6.2-2.7-7C15.5 3 13.5 4 12 5.5z";
    private static final double ANCHO = 40;
    private static final double ALTO = 48;
    private static final double LADO = 24;

    private final int numero;
    private final Label letra = new Label();
    private final StackPane cruz = new StackPane();
    private String claseEstado;

    public PiezaDental(int numero) {
        this.numero = numero;
        getStyleClass().add("pieza");

        Label etiquetaNumero = new Label(String.valueOf(numero));
        etiquetaNumero.getStyleClass().add("pieza-numero");
        letra.getStyleClass().add("pieza-letra");

        SVGPath diente = new SVGPath();
        diente.setContent(TRAZO);
        diente.getStyleClass().add("pieza-diente");
        diente.setScaleX(ANCHO / LADO);
        diente.setScaleY(ALTO / LADO);
        boolean superior = numero / 10 <= 2;
        Group dibujo = new Group(diente);
        if (superior) {
            diente.setRotate(180);
        }

        Line diagonal1 = new Line(8, 10, ANCHO - 8, ALTO - 10);
        Line diagonal2 = new Line(ANCHO - 8, 10, 8, ALTO - 10);
        diagonal1.getStyleClass().add("pieza-cruz");
        diagonal2.getStyleClass().add("pieza-cruz");
        cruz.getChildren().addAll(new Group(diagonal1, diagonal2));
        cruz.setMouseTransparent(true);
        cruz.setVisible(false);

        StackPane zonaDiente = new StackPane(dibujo, cruz);
        zonaDiente.setMinSize(ANCHO, ALTO);
        zonaDiente.setPrefSize(ANCHO, ALTO);
        zonaDiente.setMaxSize(ANCHO, ALTO);

        VBox contenido = superior
                ? new VBox(4, etiquetaNumero, zonaDiente, letra)
                : new VBox(4, letra, zonaDiente, etiquetaNumero);
        contenido.setAlignment(Pos.CENTER);
        setGraphic(contenido);
        mostrar(EstadoPieza.SANA, false);
    }

    public int getNumero() {
        return numero;
    }

    /** Pinta el estado; {@code pendiente} marca los cambios que todavía no se guardaron. */
    public final void mostrar(EstadoPieza estado, boolean pendiente) {
        if (claseEstado != null) {
            getStyleClass().remove(claseEstado);
        }
        claseEstado = "pieza-" + estado.name().toLowerCase().replace('_', '-');
        getStyleClass().add(claseEstado);
        if (pendiente) {
            if (!getStyleClass().contains("pieza-pendiente")) {
                getStyleClass().add("pieza-pendiente");
            }
        } else {
            getStyleClass().remove("pieza-pendiente");
        }
        letra.setText(estado.getCodigo());
        cruz.setVisible(estado == EstadoPieza.EXTRAIDA);
        setAccessibleText("Pieza " + numero + ", " + estado.getEtiqueta() + (pendiente ? ", sin guardar" : ""));
    }
}
