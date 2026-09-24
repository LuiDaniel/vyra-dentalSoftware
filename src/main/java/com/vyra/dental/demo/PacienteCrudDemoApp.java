package com.vyra.dental.demo;

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.dao.impl.PacienteDAOImpl;
import com.vyra.dental.model.Paciente;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

public class PacienteCrudDemoApp extends Application {

    private final PacienteDAO pacienteDAO = new PacienteDAOImpl();

    private final ObservableList<Paciente> datos =
            FXCollections.observableArrayList();

    private final TableView<Paciente> tabla = new TableView<>(datos);

    private final TextField campoId = new TextField();
    private final TextField campoNombres = new TextField();
    private final TextField campoApellidos = new TextField();
    private final TextField campoDni = new TextField();
    private final DatePicker campoFechaNacimiento = new DatePicker();
    private final TextField campoTelefono = new TextField();
    private final TextField campoDireccion = new TextField();
    private final TextField campoContactoEmergencia = new TextField();
    private final TextField campoAlergias = new TextField();

    @Override
    public void start(Stage stage) {
        campoId.setEditable(false);
        campoId.setPromptText("Autogenerado");
        campoNombres.setPromptText("Ej. Carlos");
        campoApellidos.setPromptText("Ej. Pérez");
        campoDni.setPromptText("Ej. 12345678");
        campoFechaNacimiento.setPromptText("AAAA-MM-DD");
        campoTelefono.setPromptText("Ej. 987654321");
        campoDireccion.setPromptText("Ej. Av. Principal 123");
        campoContactoEmergencia.setPromptText("Nombre y teléfono");
        campoAlergias.setPromptText("Ej. Ninguna");

        tabla.getColumns().addAll(construirColumnas());

        tabla.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        cargarFormulario(seleccionado);
                    }
                }
        );

        GridPane formulario = new GridPane();
        formulario.setHgap(8);
        formulario.setVgap(8);
        formulario.setPadding(new Insets(10));

        formulario.addRow(0, new Label("Id:"), campoId);
        formulario.addRow(1, new Label("Nombres:*"), campoNombres);
        formulario.addRow(2, new Label("Apellidos:*"), campoApellidos);
        formulario.addRow(3, new Label("DNI:*"), campoDni);
        formulario.addRow(4, new Label("Fecha nacimiento:"), campoFechaNacimiento);
        formulario.addRow(5, new Label("Teléfono:"), campoTelefono);
        formulario.addRow(6, new Label("Dirección:"), campoDireccion);
        formulario.addRow(7, new Label("Contacto emergencia:"), campoContactoEmergencia);
        formulario.addRow(8, new Label("Alergias:"), campoAlergias);

        Button botonCrear = new Button("Crear");
        Button botonActualizar = new Button("Actualizar");
        Button botonEliminar = new Button("Eliminar");
        Button botonLimpiar = new Button("Limpiar");

        botonCrear.setOnAction(e -> crear());
        botonActualizar.setOnAction(e -> actualizar());
        botonEliminar.setOnAction(e -> eliminar());
        botonLimpiar.setOnAction(e -> limpiarFormulario());

        HBox botones = new HBox(
                8, botonCrear, botonActualizar, botonEliminar, botonLimpiar
        );
        botones.setPadding(new Insets(10));

        BorderPane raiz = new BorderPane();
        raiz.setTop(formulario);
        raiz.setCenter(tabla);
        raiz.setBottom(botones);

        stage.setTitle("Prueba CRUD - Pacientes");
        stage.setScene(new Scene(raiz, 800, 650));
        stage.show();

        recargarTabla();
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Paciente, ?>[] construirColumnas() {
        TableColumn<Paciente, Integer> colId = new TableColumn<>("Id");
        colId.setCellValueFactory(new PropertyValueFactory<>("idPaciente"));
        colId.setPrefWidth(60);

        TableColumn<Paciente, String> colNombre = new TableColumn<>("Paciente");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNombre.setPrefWidth(200);

        TableColumn<Paciente, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colDni.setPrefWidth(110);

        TableColumn<Paciente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colTelefono.setPrefWidth(130);

        TableColumn<Paciente, Object> colFecha =
                new TableColumn<>("Fecha nacimiento");
        colFecha.setCellValueFactory(
                new PropertyValueFactory<>("fechaNacimiento")
        );
        colFecha.setPrefWidth(130);

        return new TableColumn[] {
                colId, colNombre, colDni, colTelefono, colFecha
        };
    }

    private void crear() {
        if (!validarCamposObligatorios()) {
            return;
        }

        try {
            Paciente paciente = crearPacienteDesdeFormulario();
            pacienteDAO.crear(paciente);

            limpiarFormulario();
            recargarTabla();
            mostrarAviso("Paciente registrado correctamente.");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void actualizar() {
        Paciente seleccionado = tabla.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAviso("Selecciona un paciente de la tabla primero.");
            return;
        }

        if (!validarCamposObligatorios()) {
            return;
        }

        try {
            seleccionado.setNombres(campoNombres.getText().trim());
            seleccionado.setApellidos(campoApellidos.getText().trim());
            seleccionado.setDni(campoDni.getText().trim());
            seleccionado.setFechaNacimiento(campoFechaNacimiento.getValue());
            seleccionado.setTelefono(campoTelefono.getText().trim());
            seleccionado.setDireccion(campoDireccion.getText().trim());
            seleccionado.setContactoEmergencia(
                    campoContactoEmergencia.getText().trim()
            );
            seleccionado.setAlergias(campoAlergias.getText().trim());

            pacienteDAO.actualizar(seleccionado);

            limpiarFormulario();
            recargarTabla();
            mostrarAviso("Paciente actualizado correctamente.");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void eliminar() {
        Paciente seleccionado = tabla.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAviso("Selecciona un paciente de la tabla primero.");
            return;
        }

        try {
            pacienteDAO.eliminar(seleccionado.getIdPaciente());

            limpiarFormulario();
            recargarTabla();
            mostrarAviso("Paciente eliminado correctamente.");

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private Paciente crearPacienteDesdeFormulario() {
        Paciente paciente = new Paciente();

        paciente.setNombres(campoNombres.getText().trim());
        paciente.setApellidos(campoApellidos.getText().trim());
        paciente.setDni(campoDni.getText().trim());
        paciente.setFechaNacimiento(campoFechaNacimiento.getValue());
        paciente.setTelefono(campoTelefono.getText().trim());
        paciente.setDireccion(campoDireccion.getText().trim());
        paciente.setContactoEmergencia(
                campoContactoEmergencia.getText().trim()
        );
        paciente.setAlergias(campoAlergias.getText().trim());

        return paciente;
    }

    private boolean validarCamposObligatorios() {
        if (campoNombres.getText().isBlank()
                || campoApellidos.getText().isBlank()
                || campoDni.getText().isBlank()) {

            mostrarAviso("Completa los campos obligatorios: nombres, apellidos y DNI.");
            return false;
        }

        return true;
    }

    private void recargarTabla() {
        try {
            List<Paciente> pacientes = pacienteDAO.listarTodos();
            datos.setAll(pacientes);

        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void cargarFormulario(Paciente paciente) {
        campoId.setText(String.valueOf(paciente.getIdPaciente()));
        campoNombres.setText(paciente.getNombres());
        campoApellidos.setText(paciente.getApellidos());
        campoDni.setText(paciente.getDni());
        campoFechaNacimiento.setValue(paciente.getFechaNacimiento());
        campoTelefono.setText(paciente.getTelefono());
        campoDireccion.setText(paciente.getDireccion());
        campoContactoEmergencia.setText(paciente.getContactoEmergencia());
        campoAlergias.setText(paciente.getAlergias());
    }

    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();

        campoId.clear();
        campoNombres.clear();
        campoApellidos.clear();
        campoDni.clear();
        campoFechaNacimiento.setValue(null);
        campoTelefono.clear();
        campoDireccion.clear();
        campoContactoEmergencia.clear();
        campoAlergias.clear();
    }

    private void mostrarAviso(String mensaje) {
        new Alert(AlertType.INFORMATION, mensaje).showAndWait();
    }

    private void mostrarError(Exception e) {
        String mensaje = e.getMessage();

        if (e.getCause() != null && e.getCause().getMessage() != null) {
            mensaje += "\n\nDetalle: " + e.getCause().getMessage();
        }

        new Alert(AlertType.ERROR, mensaje).showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
