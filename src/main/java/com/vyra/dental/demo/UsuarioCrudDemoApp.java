package com.vyra.dental.demo;

import java.util.List;

import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.dao.impl.UsuarioDAOImpl;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

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
 *
 * Sirve como ejemplo de como probar un CRUD de forma aislada, hablando
 * directo con el DAO (sin pasar por Service ni Controller). Cada quien
 * puede copiar este mismo patron cambiando Usuario por su propia entidad
 * (Paciente, Cita, Tratamiento, Pago) para probar su DAO por separado.
 *
 * Se ejecuta con su propio main, independiente de com.vyra.dental.App.
 */
public class UsuarioCrudDemoApp extends Application {

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();
    private final TableView<Usuario> tabla = new TableView<>(datos);

    private final TextField campoId = new TextField();
    private final TextField campoNombreUsuario = new TextField();
    private final TextField campoContrasena = new TextField();
    private final ComboBox<Rol> campoRol = new ComboBox<>(FXCollections.observableArrayList(Rol.values()));

    @Override
    public void start(Stage stage) {
        campoId.setEditable(false);
        campoId.setPromptText("Id (autogenerado)");
        campoNombreUsuario.setPromptText("nombre_usuario");
        campoContrasena.setPromptText("contrasena (texto plano, se guarda tal cual en esta prueba)");
        campoRol.setPromptText("rol");

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
        formulario.addRow(1, new Label("Usuario:"), campoNombreUsuario);
        formulario.addRow(2, new Label("Contrasena:"), campoContrasena);
        formulario.addRow(3, new Label("Rol:"), campoRol);

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

        stage.setTitle("Prueba CRUD - Usuario (ejemplo)");
        stage.setScene(new Scene(raiz, 640, 480));
        stage.show();

        recargarTabla();
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Usuario, ?>[] construirColumnas() {
        TableColumn<Usuario, Integer> colId = new TableColumn<>("Id");
        colId.setCellValueFactory(new PropertyValueFactory<>("idUsuario"));

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Usuario");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));

        TableColumn<Usuario, Rol> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));

        return new TableColumn[] { colId, colNombre, colRol };
    }

    private void crear() {
        try {
            Usuario usuario = new Usuario();
            usuario.setNombreUsuario(campoNombreUsuario.getText());
            usuario.setContrasenaHash(campoContrasena.getText());
            usuario.setRol(campoRol.getValue());
            usuarioDAO.crear(usuario);
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void actualizar() {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona una fila de la tabla primero.");
            return;
        }
        try {
            seleccionado.setNombreUsuario(campoNombreUsuario.getText());
            seleccionado.setContrasenaHash(campoContrasena.getText());
            seleccionado.setRol(campoRol.getValue());
            usuarioDAO.actualizar(seleccionado);
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void eliminar() {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona una fila de la tabla primero.");
            return;
        }
        try {
            usuarioDAO.eliminar(seleccionado.getIdUsuario());
            limpiarFormulario();
            recargarTabla();
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void recargarTabla() {
        try {
            List<Usuario> usuarios = usuarioDAO.listarTodos();
            datos.setAll(usuarios);
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private void cargarFormulario(Usuario usuario) {
        campoId.setText(String.valueOf(usuario.getIdUsuario()));
        campoNombreUsuario.setText(usuario.getNombreUsuario());
        campoContrasena.setText(usuario.getContrasenaHash());
        campoRol.setValue(usuario.getRol());
    }

    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        campoId.clear();
        campoNombreUsuario.clear();
        campoContrasena.clear();
        campoRol.setValue(null);
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
