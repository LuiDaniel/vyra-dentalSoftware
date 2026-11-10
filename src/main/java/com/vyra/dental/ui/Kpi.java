package com.vyra.dental.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Tarjeta de indicador (docs/08-guia-frontend.md §7.2): título con su ícono
 * en un cuadrado de color suave, la cifra grande y un detalle debajo
 * ("3 pendientes de confirmar"). La usan Inicio y Reportes.
 */
public class Kpi extends VBox {

    private final Label valor = new Label();
    private final Label detalle = new Label();

    /** @param icono nombre de {@link Icono}, o null para no mostrar ícono */
    public Kpi(String titulo, String icono) {
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("kpi-etiqueta");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox encabezado = new HBox(tituloLabel, espacio);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        if (icono != null) {
            StackPane cuadro = new StackPane(new Icono(icono, 18));
            cuadro.getStyleClass().add("kpi-icono");
            encabezado.getChildren().add(cuadro);
        }
        valor.getStyleClass().add("kpi-valor");
        detalle.getStyleClass().add("kpi-detalle");
        detalle.setWrapText(true);
        getChildren().addAll(encabezado, valor, detalle);
        getStyleClass().add("kpi");
        setPrefWidth(220);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    /** {@code tendencia}: positivo = subida (verde), negativo = bajada (rojo), 0 = neutro. */
    public Kpi mostrar(String cifra, String textoDetalle, int tendencia) {
        valor.setText(cifra);
        detalle.setText(textoDetalle);
        detalle.getStyleClass().removeAll("kpi-subida", "kpi-bajada");
        if (tendencia > 0) {
            detalle.getStyleClass().add("kpi-subida");
        } else if (tendencia < 0) {
            detalle.getStyleClass().add("kpi-bajada");
        }
        setAccessibleText(cifra + ". " + textoDetalle);
        return this;
    }

    public Kpi mostrar(String cifra, String textoDetalle) {
        return mostrar(cifra, textoDetalle, 0);
    }
}
