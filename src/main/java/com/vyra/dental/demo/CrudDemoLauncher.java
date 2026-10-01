package com.vyra.dental.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Panel unico para abrir los 4 CRUD de prueba (Usuario, Paciente,
 * Tratamiento y Pago). Cada boton abre su propia ventana.
 */
public class CrudDemoLauncher extends Application {

    @Override
    public void start(Stage stage) {
        Button botonUsuario = new Button("CRUD Usuario");
        Button botonPaciente = new Button("CRUD Paciente");
        Button botonTratamiento = new Button("CRUD Tratamiento");
        Button botonPago = new Button("CRUD Pago");

        for (Button boton : new Button[] { botonUsuario, botonPaciente, botonTratamiento, botonPago }) {
            boton.setMaxWidth(Double.MAX_VALUE);
        }

        botonUsuario.setOnAction(e -> abrir(new UsuarioCrudDemoApp()));
        botonPaciente.setOnAction(e -> abrir(new PacienteCrudDemoApp()));
        botonTratamiento.setOnAction(e -> abrir(new TratamientoCrudDemoApp()));
        botonPago.setOnAction(e -> abrir(new PagoCrudDemoApp()));

        VBox raiz = new VBox(10, new Label("Pruebas CRUD (datos en memoria)"),
                botonUsuario, botonPaciente, botonTratamiento, botonPago);
        raiz.setPadding(new Insets(20));

        stage.setTitle("Vyra Dental - Pruebas CRUD");
        stage.setScene(new Scene(raiz, 300, 240));
        stage.show();
    }

    private void abrir(Application demo) {
        try {
            demo.start(new Stage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
