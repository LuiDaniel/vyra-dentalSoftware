package com.vyra.dental.ui;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Barras verticales simples con el valor arriba y la etiqueta abajo: "Ingresos
 * de la semana" de Inicio y los ingresos por día de Reportes
 * (docs/08-guia-frontend.md §7.2: sin BarChart, con Region de alto proporcional).
 * La barra resaltada (por ejemplo, hoy) va en el color primario.
 */
public class GraficoBarras extends HBox {

    /** Alto de la barra más alta. */
    private static final double ALTO_MAXIMO = 110;
    /** Alto mínimo, para que un día sin ingresos se vea como una línea. */
    private static final double ALTO_MINIMO = 4;
    /** Con más barras que esto, el gráfico se compacta (barras más juntas y angostas). */
    private static final int BARRAS_NORMALES = 12;

    public GraficoBarras() {
        getStyleClass().add("grafico-barras");
        setAlignment(Pos.BOTTOM_CENTER);
    }

    /**
     * @param etiquetas  texto bajo cada barra ("Lun", "12")
     * @param valores    alto relativo de cada barra
     * @param textos     texto sobre cada barra ("820", "—")
     * @param resaltada  índice de la barra en color primario, o -1
     */
    public void mostrar(List<String> etiquetas, List<Double> valores, List<String> textos, int resaltada) {
        getChildren().clear();
        getStyleClass().remove("grafico-denso");
        if (valores.size() > BARRAS_NORMALES) {
            getStyleClass().add("grafico-denso");
        }
        double maximo = valores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        StringBuilder accesible = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            Label valor = new Label(textos.get(i));
            valor.getStyleClass().add("barra-valor");
            Region barra = new Region();
            barra.getStyleClass().add(i == resaltada ? "barra-resaltada" : "barra");
            double alto = maximo <= 0 ? ALTO_MINIMO : Math.max(ALTO_MINIMO, valores.get(i) / maximo * ALTO_MAXIMO);
            barra.setMinHeight(alto);
            barra.setPrefHeight(alto);
            barra.setMaxHeight(alto);
            Label etiqueta = new Label(etiquetas.get(i));
            etiqueta.getStyleClass().add(i == resaltada ? "barra-etiqueta-resaltada" : "barra-etiqueta");

            VBox columna = new VBox(valor, barra, etiqueta);
            columna.getStyleClass().add("barra-columna");
            columna.setAlignment(Pos.BOTTOM_CENTER);
            columna.setMinWidth(0);
            HBox.setHgrow(columna, Priority.ALWAYS);
            getChildren().add(columna);
            accesible.append(etiquetas.get(i)).append(": ").append(textos.get(i)).append(". ");
        }
        setAccessibleText(accesible.toString().strip());
    }
}
