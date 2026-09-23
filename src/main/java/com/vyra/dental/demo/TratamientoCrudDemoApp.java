package com.vyra.dental.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.dao.impl.TratamientoDAOImpl;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Tratamiento;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
 * Se ejecuta con su propio main, independiente de com.vyra.dental.App.
 */
public class TratamientoCrudDemoApp extends Application {

    private final TratamientoDAO tratamientoDAO = new TratamientoDAOImpl();
    private final ObservableList<Tratamiento> datos = FXCollections.observableArrayList();
    private final TableView<Tratamiento> tabla = new TableView<>(datos);

    private final TextField campoId = new TextField();
    private final TextField campoDescripcion = new TextField();
    private final TextField campoCosto = new TextField();
    private final ComboBox<EstadoTratamiento> campoEstado =
            new ComboBox<>(FXCollections.observableArrayList(EstadoTratamiento.values()));
    private final TextField campoPiezas = new TextField();

    @Override
    public void start(Stage stage) {
        campoId.setEditable(false);
        campoId.setPromptText("Id (autogenerado)");
        campoDescripcion.setPromptText("descripcion");
        campoCosto.setPromptText("costo (ej. 150.0)");
        campoEstado.setPromptText("estado");
        campoPiezas.setPromptText("piezas dentales separadas por coma (ej. 11,12,21)");

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
        formulario.addRow(1, new Label("Descripcion:"), campoDescripcion);
        formulario.addRow(2, new Label("Costo:"), campoCosto);
        formulario.addRow(3, new Label("Estado:"), campoEstado);
        formulario.addRow(4, new Label("Piezas:"), campoPiezas);

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

        stage.setTitle("Prueba CRUD - Tratamiento (ejemplo)");
        stage.setScene(new Scene(raiz, 640, 480));
        stage.show();

        recargarTabla();
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Tratamiento, ?>[] construirColumnas() {
        TableColumn<Tratamiento, Integer> colId = new TableColumn<>("Id");
        colId.setCellValueFactory(new PropertyValueFactory<>("idTratamiento"));

        TableColumn<Tratamiento, String> colDescripcion = new TableColumn<>("Descripcion");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        TableColumn<Tratamiento, Double> colCosto = new TableColumn<>("Costo");
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costo"));

        TableColumn<Tratamiento, EstadoTratamiento> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        TableColumn<Tratamiento, List<Integer>> colPiezas = new TableColumn<>("Piezas");
        colPiezas.setCellValueFactory(new PropertyValueFactory<>("piezasDentales"));

        return new TableColumn[] { colId, colDescripcion, colCosto, colEstado, colPiezas };
    }

    private void crear() {
        try {
            Tratamiento tratamiento = new Tratamiento();
            tratamiento.setDescripcion(campoDescripcion.getText());
            tratamiento.setCosto(Double.parseDouble(campoCosto.getText()));
            tratamiento.setEstado(campoEstado.getValue());
            tratamiento.setPiezasDentales(textoAPiezas(campoPiezas.getText()));
            tratamientoDAO.crear(tratamiento);
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void actualizar() {
        Tratamiento seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona una fila de la tabla primero.");
            return;
        }
        try {
            seleccionado.setDescripcion(campoDescripcion.getText());
            seleccionado.setCosto(Double.parseDouble(campoCosto.getText()));
            seleccionado.setEstado(campoEstado.getValue());
            seleccionado.setPiezasDentales(textoAPiezas(campoPiezas.getText()));
            tratamientoDAO.actualizar(seleccionado);
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void eliminar() {
        Tratamiento seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona una fila de la tabla primero.");
            return;
        }
        try {
            tratamientoDAO.eliminar(seleccionado.getIdTratamiento());
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void recargarTabla() {
        try {
            List<Tratamiento> tratamientos = tratamientoDAO.listarTodos();
            datos.setAll(tratamientos);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void cargarFormulario(Tratamiento tratamiento) {
        campoId.setText(String.valueOf(tratamiento.getIdTratamiento()));
        campoDescripcion.setText(tratamiento.getDescripcion());
        campoCosto.setText(String.valueOf(tratamiento.getCosto()));
        campoEstado.setValue(tratamiento.getEstado());
        campoPiezas.setText(piezasATexto(tratamiento.getPiezasDentales()));
    }

    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        campoId.clear();
        campoDescripcion.clear();
        campoCosto.clear();
        campoEstado.setValue(null);
        campoPiezas.clear();
    }

    private String piezasATexto(List<Integer> piezas) {
        if (piezas == null || piezas.isEmpty()) {
            return "";
        }
        return piezas.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> textoAPiezas(String texto) {
        List<Integer> piezas = new ArrayList<>();
        if (texto != null && !texto.isBlank()) {
            for (String parte : texto.split(",")) {
                piezas.add(Integer.parseInt(parte.trim()));
            }
        }
        return piezas;
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