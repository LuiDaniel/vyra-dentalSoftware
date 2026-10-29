package com.vyra.dental.controller;

import java.util.prefs.Preferences;

import com.vyra.dental.App;
import com.vyra.dental.service.AuthService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.Icono;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Pantalla de inicio de sesión (docs/08-guia-frontend.md §7.1, CU-08).
 */
public class LoginController {

    private static final String PREF_USUARIO = "usuarioRecordado";

    @FXML private StackPane panelMarca;
    @FXML private Pane capaDecorativa;
    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private TextField campoContrasenaVisible;
    @FXML private Button botonVer;
    @FXML private CheckBox recordar;
    @FXML private Aviso aviso;

    private final AuthService authService = Servicios.auth();
    private final Preferences preferencias = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    private void initialize() {
        // El diente decorativo se sale por la esquina inferior derecha del panel
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(panelMarca.widthProperty());
        recorte.heightProperty().bind(panelMarca.heightProperty());
        panelMarca.setClip(recorte);
        capaDecorativa.getChildren().get(0).layoutXProperty().bind(panelMarca.widthProperty().subtract(380));
        capaDecorativa.getChildren().get(0).layoutYProperty().bind(panelMarca.heightProperty().subtract(400));

        campoContrasenaVisible.textProperty().bindBidirectional(campoContrasena.textProperty());
        mostrarContrasena(false);

        // Al volver a escribir se oculta el error anterior
        campoUsuario.textProperty().addListener((obs, anterior, nuevo) -> aviso.ocultar());
        campoContrasena.textProperty().addListener((obs, anterior, nuevo) -> aviso.ocultar());

        String recordado = preferencias.get(PREF_USUARIO, "");
        if (!recordado.isEmpty()) {
            campoUsuario.setText(recordado);
            recordar.setSelected(true);
            Platform.runLater(this::enfocarContrasena);
        } else {
            Platform.runLater(campoUsuario::requestFocus);
        }
    }

    @FXML
    private void ingresar() {
        String usuario = campoUsuario.getText().strip();
        String contrasena = campoContrasena.getText();
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            aviso.mostrar(Aviso.Tipo.ERROR, "Ingresa tu usuario y tu contraseña.");
            if (usuario.isEmpty()) {
                campoUsuario.requestFocus();
            } else {
                enfocarContrasena();
            }
            return;
        }
        try {
            authService.iniciarSesion(usuario, contrasena);
            recordarUsuario(usuario);
            App.mostrarPrincipal();
        } catch (ReglaNegocioException e) {
            // Primero se limpia (eso oculta el aviso) y después se muestra el error
            campoContrasena.clear();
            aviso.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            enfocarContrasena();
        } catch (RuntimeException e) {
            aviso.mostrar(Aviso.Tipo.ERROR, "No se pudo iniciar sesión. " + e.getMessage() + ".");
        }
    }

    @FXML
    private void alternarContrasena() {
        mostrarContrasena(!campoContrasenaVisible.isVisible());
        enfocarContrasena();
    }

    @FXML
    private void olvideContrasena() {
        aviso.mostrar(Aviso.Tipo.INFO, "Pide al administrador del consultorio que restablezca tu contraseña.");
    }

    private void mostrarContrasena(boolean visible) {
        campoContrasenaVisible.setVisible(visible);
        campoContrasena.setVisible(!visible);
        String texto = visible ? "Ocultar contraseña" : "Mostrar contraseña";
        botonVer.setGraphic(new Icono(visible ? "ojo-cerrado" : "ojo", 18));
        botonVer.setTooltip(new Tooltip(texto));
        botonVer.setAccessibleText(texto);
    }

    private void enfocarContrasena() {
        TextField activo = campoContrasenaVisible.isVisible() ? campoContrasenaVisible : campoContrasena;
        activo.requestFocus();
        activo.end();
    }

    /** Solo se guarda el nombre de usuario, nunca la contraseña. */
    private void recordarUsuario(String usuario) {
        if (recordar.isSelected()) {
            preferencias.put(PREF_USUARIO, usuario);
        } else {
            preferencias.remove(PREF_USUARIO);
        }
    }
}
