package com.vyra.dental;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.vyra.dental.dao.impl.PacienteDAOSqlite;
import com.vyra.dental.dao.impl.UsuarioDAOSqlite;
import com.vyra.dental.util.ConexionBD;
import com.vyra.dental.util.DatosIniciales;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Punto de entrada. Usa una sola ventana: muestra el login y, al iniciar
 * sesión, cambia su escena por la principal (docs/05-arquitectura.md §4).
 */
public class App extends Application {

    private static final String HOJA_ESTILOS = "/com/vyra/styles/vyra.css";
    private static final String[] FUENTES = { "Inter-Regular", "Inter-Medium", "Inter-SemiBold", "Inter-Bold" };

    private static Stage ventana;

    @Override
    public void start(Stage stage) {
        cargarFuentes();
        // Crea vyra.db y sus tablas (schema.sql) si todavía no existen
        ConexionBD.obtenerConexion();
        DatosIniciales.crearAdminSiNoExiste(new UsuarioDAOSqlite());
        if (DatosIniciales.demoActivada()) {
            DatosIniciales.cargarEjemplos(new PacienteDAOSqlite());
        }

        ventana = stage;
        mostrarLogin();
        stage.show();
    }

    /** Ventana de 1100×680 con la pantalla de inicio de sesión. */
    public static void mostrarLogin() {
        Parent raiz = cargarVista("LoginView");
        ventana.setMaximized(false);
        ventana.setMinWidth(Math.min(900, areaPantalla().getWidth()));
        ventana.setMinHeight(Math.min(600, areaPantalla().getHeight()));
        ventana.setScene(crearEscena(raiz, 1100, 680));
        ventana.setTitle("Vyra · Iniciar sesión");
        ventana.sizeToScene();
        ventana.centerOnScreen();
    }

    /** Ventana maximizada (mínimo 1280×800) con el menú y la pantalla de Inicio. */
    public static void mostrarPrincipal() {
        Parent raiz = cargarVista("MainView");
        ventana.setScene(crearEscena(raiz, 1280, 800));
        // En pantallas más chicas que 1280×800 el mínimo es la pantalla
        ventana.setMinWidth(Math.min(1280, areaPantalla().getWidth()));
        ventana.setMinHeight(Math.min(800, areaPantalla().getHeight()));
        ventana.setTitle("Vyra");
        ventana.setMaximized(true);
    }

    public static Stage getVentana() {
        return ventana;
    }

    private static Parent cargarVista(String nombre) {
        try {
            return FXMLLoader.load(App.class.getResource("/com/vyra/views/" + nombre + ".fxml"));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo abrir la pantalla " + nombre, e);
        }
    }

    private static Scene crearEscena(Parent raiz, double ancho, double alto) {
        Scene escena = new Scene(raiz, ancho, alto);
        escena.getStylesheets().add(App.class.getResource(HOJA_ESTILOS).toExternalForm());
        return escena;
    }

    private static Rectangle2D areaPantalla() {
        return Screen.getPrimary().getVisualBounds();
    }

    /**
     * JavaFX registra Medium y SemiBold como familias aparte ("Inter Medium",
     * "Inter SemiBold"); vyra.css las usa por nombre (docs/08-guia-frontend.md §3).
     */
    private static void cargarFuentes() {
        for (String fuente : FUENTES) {
            try (InputStream archivo = App.class.getResourceAsStream("/com/vyra/fonts/" + fuente + ".ttf")) {
                if (archivo == null || Font.loadFont(archivo, 14) == null) {
                    System.err.println("No se pudo cargar la fuente " + fuente + "; se usará la del sistema");
                }
            } catch (IOException e) {
                System.err.println("No se pudo cargar la fuente " + fuente + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
