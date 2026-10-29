package com.vyra.dental.ui;

import java.util.Map;

import javafx.beans.NamedArg;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Ícono de trazo de 24×24 (docs/08-guia-frontend.md §5), escalado al tamaño pedido.
 * El color y el grosor salen de vyra.css con la clase {@code .icono}
 * ({@code -fx-stroke}, {@code -fx-stroke-width}). En FXML:
 * {@code <Icono nombre="agenda" tamano="18"/>}.
 */
public class Icono extends Group {

    private static final double LADO = 24;

    private static final Map<String, String> TRAZOS = Map.ofEntries(
            Map.entry("inicio", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"),
            Map.entry("agenda", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zM16 2v4M8 2v4M3 10h18"),
            Map.entry("pacientes", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"),
            Map.entry("diente", "M12 5.5C10.5 4 8.5 3 6.5 3.5 3.5 4.3 3 7.5 3.8 10.5c.6 2.2 1.2 4 1.5 6.5.3 2.3.8 4 2 4 1.7 0 1.7-5 4.7-5s3 5 4.7 5c1.2 0 1.7-1.7 2-4 .3-2.5.9-4.3 1.5-6.5.8-3 .3-6.2-2.7-7C15.5 3 13.5 4 12 5.5z"),
            Map.entry("historia", "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M16 13H8M16 17H8"),
            Map.entry("tratamientos", "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1zM16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2M9 12h6M9 16h4"),
            Map.entry("pagos", "M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zM2 10h20M6 15h4"),
            Map.entry("reportes", "M3 3v18h18M7 16v-4M12 16V8M17 16v-7"),
            Map.entry("profesionales", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0M16 11l2 2 4-4"),
            Map.entry("usuarios", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"),
            Map.entry("buscar", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0M21 21l-4.3-4.3"),
            Map.entry("campana", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0"),
            Map.entry("mas", "M12 5v14M5 12h14"),
            Map.entry("salir", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"),
            Map.entry("reloj", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0M12 6v6l4 2"),
            Map.entry("alerta", "M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"),
            Map.entry("error", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0M12 8v4M12 16h.01"),
            Map.entry("info", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0M12 16v-4M12 8h.01"),
            Map.entry("check", "M20 6 9 17l-5-5"),
            Map.entry("cerrar", "M18 6 6 18M6 6l12 12"),
            Map.entry("descargar", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"),
            Map.entry("anterior", "m15 18-6-6 6-6"),
            Map.entry("siguiente", "m9 18 6-6-6-6"),
            Map.entry("flecha", "M5 12h14M13 6l6 6-6 6"),
            Map.entry("usuario", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2M8 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0"),
            Map.entry("candado", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2zM7 11V7a5 5 0 0 1 10 0v4"),
            Map.entry("ojo", "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zM9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0"),
            Map.entry("ojo-cerrado", "M9.88 9.88a3 3 0 1 0 4.24 4.24M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68M6.61 6.61A13.53 13.53 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61M2 2l20 20"),
            Map.entry("efectivo", "M2 6h20v12H2zM9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0M6 12h.01M18 12h.01"),
            Map.entry("celular", "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zM12 18h.01"),
            Map.entry("banco", "M3 21h18M3 10h18M5 6l7-3 7 3M4 10v11M20 10v11M8 14v3M12 14v3M16 14v3"),
            Map.entry("dinero", "M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"));

    private final String nombre;
    private final double tamano;

    public Icono(@NamedArg("nombre") String nombre, @NamedArg(value = "tamano", defaultValue = "18") double tamano) {
        String trazo = TRAZOS.get(nombre);
        if (trazo == null) {
            throw new IllegalArgumentException("No existe el ícono '" + nombre + "'");
        }
        this.nombre = nombre;
        this.tamano = tamano;

        SVGPath path = new SVGPath();
        path.setContent(trazo);
        path.setFill(null);
        path.setStroke(Color.web("#64748B"));
        path.setStrokeWidth(2);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.getStyleClass().add("icono");

        // Caja transparente de 24×24: todos los íconos ocupan lo mismo aunque su trazo sea más chico
        Rectangle caja = new Rectangle(LADO, LADO, Color.TRANSPARENT);
        Group lienzo = new Group(caja, path);
        double escala = tamano / LADO;
        lienzo.setScaleX(escala);
        lienzo.setScaleY(escala);
        // La escala se aplica desde el centro; se corrige para que la esquina quede en (0, 0)
        lienzo.setTranslateX((tamano - LADO) / 2);
        lienzo.setTranslateY((tamano - LADO) / 2);

        getChildren().add(lienzo);
        getStyleClass().add("icono-contenedor");
    }

    public Icono(String nombre) {
        this(nombre, 18);
    }

    public String getNombre() {
        return nombre;
    }

    public double getTamano() {
        return tamano;
    }
}
