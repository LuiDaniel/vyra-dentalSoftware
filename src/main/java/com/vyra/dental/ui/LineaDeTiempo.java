package com.vyra.dental.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Lista de sucesos con un punto y una línea vertical, del más reciente al más
 * antiguo: historial de una pieza del odontograma y entradas de la historia
 * clínica (docs/08-guia-frontend.md §7.5 y §7.6).
 */
public class LineaDeTiempo extends VBox {

    private final List<Region> trazos = new ArrayList<>();

    public LineaDeTiempo() {
        getStyleClass().add("linea-tiempo");
    }

    public void limpiar() {
        getChildren().clear();
        trazos.clear();
    }

    /**
     * Agrega un suceso al final.
     *
     * @param titulo    texto en negrita ("Cariada", el diagnóstico)
     * @param detalle   texto de apoyo ("02 sep 2026 · Marco Salinas Vega")
     * @param texto     párrafo opcional debajo (observaciones); null si no hay
     * @param claseColor clase del punto: la de un {@code EstadoPieza}, o null para el color primario
     */
    public void agregar(String titulo, String detalle, String texto, String claseColor) {
        Region punto = new Region();
        punto.getStyleClass().add("linea-punto");
        if (claseColor != null) {
            punto.getStyleClass().add(claseColor);
        }
        Region trazo = new Region();
        trazo.getStyleClass().add("linea-trazo");
        VBox.setVgrow(trazo, Priority.ALWAYS);
        VBox marca = new VBox(punto, trazo);
        marca.getStyleClass().add("linea-marca");

        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("linea-titulo");
        tituloLabel.setWrapText(true);
        Label detalleLabel = new Label(detalle);
        detalleLabel.getStyleClass().add("linea-detalle");
        detalleLabel.setWrapText(true);
        VBox textos = new VBox(tituloLabel, detalleLabel);
        textos.getStyleClass().add("linea-textos");
        if (texto != null && !texto.isBlank()) {
            Label parrafo = new Label(texto);
            parrafo.getStyleClass().add("linea-texto");
            parrafo.setWrapText(true);
            textos.getChildren().add(parrafo);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox suceso = new HBox(marca, textos);
        suceso.getStyleClass().add("linea-suceso");
        getChildren().add(suceso);

        // La línea une cada punto con el siguiente: el último suceso no la lleva
        trazos.add(trazo);
        for (int i = 0; i < trazos.size(); i++) {
            trazos.get(i).setVisible(i < trazos.size() - 1);
        }
    }

    public boolean estaVacia() {
        return getChildren().isEmpty();
    }
}
