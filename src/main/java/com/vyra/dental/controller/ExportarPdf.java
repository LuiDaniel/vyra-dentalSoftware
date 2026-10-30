package com.vyra.dental.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import com.vyra.dental.ui.Notificacion;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Guarda un PDF (RF-11): pide dónde guardarlo y lo genera en un {@link Task}, para no congelar la
 * ventana. Mientras tanto, el botón queda deshabilitado con un indicador de progreso.
 */
final class ExportarPdf {

    /** Lo que escribe el PDF; lo implementa {@code ExportacionPdfService}. */
    @FunctionalInterface
    interface Escritor {
        void escribir(OutputStream salida);
    }

    private ExportarPdf() {
    }

    static void ejecutar(Button boton, String nombreSugerido, Escritor escritor) {
        Window ventana = boton.getScene().getWindow();
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        File archivo = selector.showSaveDialog(ventana);
        if (archivo == null) {
            return;
        }

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws IOException {
                try (OutputStream salida = Files.newOutputStream(archivo.toPath())) {
                    escritor.escribir(salida);
                }
                return null;
            }
        };
        Node graficoOriginal = boton.getGraphic();
        ProgressIndicator progreso = new ProgressIndicator();
        progreso.setPrefSize(16, 16);
        boton.setGraphic(progreso);
        boton.setDisable(true);
        Runnable restaurar = () -> {
            boton.setGraphic(graficoOriginal);
            boton.setDisable(false);
        };
        tarea.setOnSucceeded(e -> {
            restaurar.run();
            Notificacion.mostrar(ventana, "PDF guardado: " + archivo.getName());
        });
        tarea.setOnFailed(e -> {
            restaurar.run();
            Notificacion.mostrar(ventana, "No se pudo guardar el PDF: " + mensaje(tarea.getException()));
        });
        Thread hilo = new Thread(tarea, "exportar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static String mensaje(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getMessage() == null) {
            causa = causa.getCause();
        }
        return causa.getMessage() == null ? error.getClass().getSimpleName() : causa.getMessage();
    }
}
