package com.vyra.dental.ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.vyra.dental.App;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;

/**
 * Guarda un PDF (docs/08-guia-frontend.md §7.9, RF-11): pide dónde guardarlo,
 * lo genera en un Task para no congelar la ventana (el botón muestra
 * "Exportando…") y al terminar lo abre con el visor de PDF del sistema.
 */
public final class ExportarPdf {

    private ExportarPdf() {
    }

    /**
     * @param boton          botón que se pulsó: se deshabilita mientras se genera
     * @param nombreSugerido nombre de archivo propuesto ("odontograma-72481935-2026-11-10.pdf")
     * @param generar        escribe el PDF en el OutputStream (corre fuera del hilo de JavaFX)
     * @param alFallar       recibe el mensaje de error para mostrarlo en la pantalla
     */
    public static void exportar(Button boton, String nombreSugerido, Consumer<OutputStream> generar,
                                Consumer<String> alFallar) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        selector.setInitialFileName(nombreSugerido);
        File documentos = new File(System.getProperty("user.home"), "Documents");
        if (documentos.isDirectory()) {
            selector.setInitialDirectory(documentos);
        }
        File elegido = selector.showSaveDialog(boton.getScene().getWindow());
        if (elegido == null) {
            return;
        }
        Path destino = elegido.getName().toLowerCase().endsWith(".pdf") ? elegido.toPath()
                : elegido.toPath().resolveSibling(elegido.getName() + ".pdf");

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws IOException {
                try (OutputStream salida = Files.newOutputStream(destino)) {
                    generar.accept(salida);
                }
                return null;
            }
        };
        String texto = boton.getText();
        Node grafico = boton.getGraphic();
        ProgressIndicator progreso = new ProgressIndicator();
        progreso.getStyleClass().add("progreso-boton");
        boton.setDisable(true);
        boton.setText("Exportando…");
        boton.setGraphic(progreso);

        tarea.setOnSucceeded(e -> {
            restaurar(boton, texto, grafico);
            Notificacion.mostrar(boton.getScene().getWindow(), "PDF guardado: " + destino.getFileName());
            App.abrirArchivo(destino);
        });
        tarea.setOnFailed(e -> {
            restaurar(boton, texto, grafico);
            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignorada) {
                // El archivo a medias queda; el error que importa es el original
            }
            Throwable error = tarea.getException();
            if (error instanceof UncheckedIOException || error instanceof IOException) {
                alFallar.accept("No se pudo guardar el PDF. ¿Está abierto en otro programa?");
            } else {
                alFallar.accept(error.getMessage() == null ? "No se pudo generar el PDF." : error.getMessage());
            }
        });
        Thread hilo = new Thread(tarea, "exportar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void restaurar(Button boton, String texto, Node grafico) {
        boton.setText(texto);
        boton.setGraphic(grafico);
        boton.setDisable(false);
    }
}
