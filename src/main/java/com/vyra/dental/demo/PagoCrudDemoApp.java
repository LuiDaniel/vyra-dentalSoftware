package com.vyra.dental.demo;

import java.util.List;

import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.memoria.PagoDAOMemoria;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * PANEL DE PRUEBA (no forma parte de la app real, App.java no lo usa).
 * Sigue el mismo patron que UsuarioCrudDemoApp.
 */
public class PagoCrudDemoApp extends Application {

    private final PagoDAO pagoDAO = new PagoDAOMemoria();
    private final ObservableList<Pago> datos = FXCollections.observableArrayList();
    private final TableView<Pago> tabla = new TableView<>(datos);

    private final TextField campoId = new TextField();
    private final TextField campoMonto = new TextField();
    private final DatePicker campoFecha = new DatePicker();
    private final ComboBox<MetodoPago> campoMetodo =
            new ComboBox<>(FXCollections.observableArrayList(MetodoPago.values()));

    @Override
    public void start(Stage stage) {
        campoId.setEditable(false);
        campoId.setPromptText("Id (autogenerado)");
        campoMonto.setPromptText("monto (ej. 150.0)");
        campoFecha.setPromptText("AAAA-MM-DD");
        campoMetodo.setPromptText("método de pago");

        tabla.getColumns().addAll(construirColumnas());
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                cargarFormulario(seleccionado);
            }
        });

        GridPane formulario = new GridPane();
        formulario.setHgap(8);
        formulario.setVgap(8);
        formulario.setPadding(new Insets(10));
        formulario.addRow(0, new Label("Id:"), campoId);
        formulario.addRow(1, new Label("Monto:"), campoMonto);
        formulario.addRow(2, new Label("Fecha de pago:"), campoFecha);
        formulario.addRow(3, new Label("Método de pago:"), campoMetodo);

        Button botonCrear = new Button("Crear");
        Button botonActualizar = new Button("Actualizar");
        Button botonEliminar = new Button("Eliminar");
        Button botonLimpiar = new Button("Limpiar");

        botonCrear.setOnAction(e -> crear());
        botonActualizar.setOnAction(e -> actualizar());
        botonEliminar.setOnAction(e -> eliminar());
        botonLimpiar.setOnAction(e -> limpiarFormulario());

        HBox botones = new HBox(8, botonCrear, botonActualizar, botonEliminar, botonLimpiar);
        botones.setPadding(new Insets(10));

        BorderPane raiz = new BorderPane();
        raiz.setTop(formulario);
        raiz.setCenter(tabla);
        raiz.setBottom(botones);

        stage.setTitle("Prueba CRUD - Pago (ejemplo)");
        stage.setScene(new Scene(raiz, 640, 480));
        stage.show();

        recargarTabla();
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Pago, ?>[] construirColumnas() {
        TableColumn<Pago, Integer> colId = new TableColumn<>("Id");
        colId.setCellValueFactory(new PropertyValueFactory<>("idPago"));

        TableColumn<Pago, Double> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        TableColumn<Pago, Object> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaPago"));

        TableColumn<Pago, MetodoPago> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));

        return new TableColumn[] { colId, colMonto, colFecha, colMetodo };
    }

    private void crear() {
        try {
            Pago pago = new Pago();
            pago.setMonto(Double.parseDouble(campoMonto.getText()));
            pago.setFechaPago(campoFecha.getValue());
            pago.setMetodoPago(campoMetodo.getValue());
            pagoDAO.crear(pago);
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void actualizar() {
        Pago seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona una fila de la tabla primero.");
            return;
        }
        try {
            seleccionado.setMonto(Double.parseDouble(campoMonto.getText()));
            seleccionado.setFechaPago(campoFecha.getValue());
            seleccionado.setMetodoPago(campoMetodo.getValue());
            pagoDAO.actualizar(seleccionado);
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void eliminar() {
        Pago seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona una fila de la tabla primero.");
            return;
        }
        try {
            pagoDAO.eliminar(seleccionado.getIdPago());
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void recargarTabla() {
        try {
            List<Pago> pagos = pagoDAO.listarTodos();
            datos.setAll(pagos);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void cargarFormulario(Pago pago) {
        campoId.setText(String.valueOf(pago.getIdPago()));
        campoMonto.setText(String.valueOf(pago.getMonto()));
        campoFecha.setValue(pago.getFechaPago());
        campoMetodo.setValue(pago.getMetodoPago());
    }

    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        campoId.clear();
        campoMonto.clear();
        campoFecha.setValue(null);
        campoMetodo.setValue(null);
    }

    private void mostrarAviso(String mensaje) {
        new Alert(AlertType.INFORMATION, mensaje).showAndWait();
    }

    private void mostrarError(Exception e) {
        new Alert(AlertType.ERROR, e.getMessage()).showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
