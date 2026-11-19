package com.vyra.dental.ui;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vyra.dental.util.Formatos;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Ventana del cobro en línea (RF-13): el QR con el enlace de pago, el monto y
 * el estado. Pregunta solo cada pocos segundos si ya se pagó (en otro hilo,
 * para no congelar la pantalla) y se cierra sola cuando el pago se aprueba.
 * No conoce los servicios: recibe qué hacer para verificar y para simular.
 */
public final class DialogoCobroEnLinea {

    private static final Duration CADA = Duration.seconds(4);

    private DialogoCobroEnLinea() {
    }

    /**
     * @param verificar devuelve el resultado si el pago ya se aprobó (y lo
     *                  registró), vacío si sigue pendiente, o lanza el problema
     * @param simular   solo en la demo: "el paciente pagó"; null en Mercado Pago real
     * @return el resultado de {@code verificar}, o vacío si se cerró sin pagar
     */
    public static <T> Optional<T> mostrar(Window duenio, String pasarela, String concepto, double monto,
                                          String enlace, Callable<Optional<T>> verificar, Runnable simular) {
        AtomicReference<T> resultado = new AtomicReference<>();
        AtomicBoolean consultando = new AtomicBoolean(false);
        Stage ventana = new Stage();
        ventana.initOwner(duenio);
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setTitle("Cobro con " + pasarela);

        Label titulo = new Label("Cobro con " + pasarela);
        titulo.getStyleClass().add("card-titulo");
        Label detalle = new Label(concepto);
        detalle.getStyleClass().add("texto-secundario");
        detalle.setWrapText(true);
        Label cifra = new Label(Formatos.dinero(monto));
        cifra.getStyleClass().add("cobro-linea-monto");

        ImageView qr = new ImageView(CodigoQr.imagen(enlace, 240));
        qr.setAccessibleText("Código QR para pagar " + Formatos.dinero(monto));
        StackPane marcoQr = new StackPane(qr);
        marcoQr.getStyleClass().add("cobro-linea-qr");
        marcoQr.setMaxWidth(Region.USE_PREF_SIZE);

        Label instruccion = new Label(simular != null
                ? "Modo demo: no se mueve dinero. Pulsa \"Simular pago aprobado\" para ver qué pasa cuando el paciente paga."
                : "Que el paciente escanee el código con la cámara de su celular (o ábrelo en el navegador) y pague con tarjeta, Yape o su cuenta de Mercado Pago.");
        instruccion.setWrapText(true);
        instruccion.getStyleClass().add("texto-apoyo");

        ProgressIndicator girando = new ProgressIndicator();
        girando.setPrefSize(18, 18);
        Label estado = new Label("Esperando el pago…");
        estado.getStyleClass().add("texto-secundario");
        estado.setWrapText(true);
        HBox filaEstado = new HBox(8, girando, estado);
        filaEstado.setAlignment(Pos.CENTER);

        Button copiar = new Button("Copiar enlace");
        copiar.getStyleClass().add("btn-secundario");
        copiar.setOnAction(e -> {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(enlace);
            Clipboard.getSystemClipboard().setContent(contenido);
            copiar.setText("Enlace copiado");
        });
        HBox enlaces = new HBox(8, copiar);
        enlaces.setAlignment(Pos.CENTER);
        if (simular == null && enlace.startsWith("https://")) {
            Button abrir = new Button("Abrir en el navegador");
            abrir.getStyleClass().add("btn-secundario");
            abrir.setOnAction(e -> abrirEnNavegador(enlace, estado));
            enlaces.getChildren().add(abrir);
        }

        Timeline sondeo = new Timeline();
        Runnable consultar = () -> {
            if (!consultando.compareAndSet(false, true)) {
                return;
            }
            Thread hilo = new Thread(() -> {
                try {
                    Optional<T> pago = verificar.call();
                    Platform.runLater(() -> {
                        if (pago.isPresent()) {
                            resultado.set(pago.get());
                            sondeo.stop();
                            ventana.close();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        estado.setText(ex.getMessage());
                        estado.getStyleClass().add("texto-error");
                    });
                } finally {
                    consultando.set(false);
                }
            }, "vyra-cobro-en-linea");
            hilo.setDaemon(true);
            hilo.start();
        };
        sondeo.getKeyFrames().add(new KeyFrame(CADA, e -> consultar.run()));
        sondeo.setCycleCount(Timeline.INDEFINITE);

        HBox botones = new HBox(8);
        botones.setAlignment(Pos.CENTER_RIGHT);
        Region espacio = new Region();
        HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);
        Button cerrar = new Button("Cerrar sin cobrar");
        cerrar.getStyleClass().add("btn-secundario");
        cerrar.setOnAction(e -> ventana.close());
        Button verificarAhora = new Button("Ya pagó, verificar");
        verificarAhora.getStyleClass().add(simular != null ? "btn-secundario" : "btn-primario");
        verificarAhora.setOnAction(e -> consultar.run());
        botones.getChildren().addAll(cerrar, espacio, verificarAhora);

        VBox raiz = new VBox(titulo, detalle, cifra, marcoQr, instruccion, enlaces, filaEstado, botones);
        if (simular != null) {
            Button simularPago = new Button("Simular pago aprobado");
            simularPago.getStyleClass().add("btn-primario");
            simularPago.setMaxWidth(Double.MAX_VALUE);
            simularPago.setOnAction(e -> {
                simular.run();
                estado.setText("Pago simulado: verificando…");
                consultar.run();
            });
            raiz.getChildren().add(raiz.getChildren().indexOf(botones), simularPago);
        }
        raiz.getStyleClass().add("cobro-linea");
        if (simular != null) {
            Label demo = new Label("MODO DEMO · sin dinero real");
            demo.getStyleClass().add("cobro-linea-demo");
            raiz.getChildren().add(1, demo);
        }
        raiz.setPrefWidth(460);
        Scene escena = new Scene(raiz);
        if (duenio != null && duenio.getScene() != null) {
            escena.getStylesheets().setAll(duenio.getScene().getStylesheets());
            escena.getRoot().getStyleClass().addAll(duenio.getScene().getRoot().getStyleClass());
        }
        ventana.setScene(escena);
        ventana.setResizable(false);
        ventana.setOnHidden(e -> sondeo.stop());
        sondeo.play();
        ventana.showAndWait();
        return Optional.ofNullable(resultado.get());
    }

    private static void abrirEnNavegador(String enlace, Label estado) {
        Thread hilo = new Thread(() -> {
            try {
                Desktop.getDesktop().browse(URI.create(enlace));
            } catch (Exception e) {
                Platform.runLater(() -> estado.setText("No se pudo abrir el navegador: copia el enlace."));
            }
        }, "vyra-abrir-enlace");
        hilo.setDaemon(true);
        hilo.start();
    }
}
