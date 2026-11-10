package com.vyra.dental.ui;

import com.vyra.dental.util.Formatos;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Barra de lo pagado de un tratamiento + "S/ 250.00 de S/ 650.00"
 * (docs/08-guia-frontend.md §7.7 y §7.8).
 */
public class AvancePago extends VBox {

    private final ProgressBar barra = new ProgressBar(0);
    private final Label texto = new Label();

    public AvancePago() {
        getStyleClass().add("avance-pago");
        barra.getStyleClass().add("barra-pago");
        barra.setMaxWidth(Double.MAX_VALUE);
        texto.getStyleClass().add("celda-sub");
        getChildren().addAll(barra, texto);
    }

    public void mostrar(double pagado, double costo) {
        barra.setProgress(costo <= 0 ? 0 : Math.min(1, pagado / costo));
        texto.setText(Formatos.dinero(pagado) + " de " + Formatos.dinero(costo));
        setAccessibleText("Pagado " + texto.getText());
    }
}
