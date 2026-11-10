package com.vyra.dental.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.vyra.dental.App;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.util.Formatos;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Marco común: menú lateral según el rol, barra superior y el área de
 * contenido donde se carga cada pantalla (docs/08-guia-frontend.md §6).
 */
public class MainController {

    @FXML private VBox menu;
    @FXML private Avatar avatar;
    @FXML private Label nombreUsuario;
    @FXML private Label rolUsuario;
    @FXML private Label ruta;
    @FXML private HBox buscadorGlobal;
    @FXML private TextField campoGlobal;
    @FXML private Label fecha;
    @FXML private StackPane contenido;

    private static MainController actual;

    private final ToggleGroup grupoMenu = new ToggleGroup();
    private final Map<Pantalla, ToggleButton> botonesMenu = new EnumMap<>(Pantalla.class);
    private final Label contadorAgenda = new Label();
    private final ContextMenu sugerencias = new ContextMenu();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    private Rol rol;
    private Pantalla pantallaActual;
    /** Controlador de la pantalla abierta, para preguntarle si se puede salir. */
    private Object controladorActual;

    /**
     * El marco abierto, para que una pantalla abra otra:
     * {@code MainController.actual().navegar(Pantalla.ODONTOGRAMA, paciente)}.
     */
    public static MainController actual() {
        return actual;
    }

    @FXML
    private void initialize() {
        actual = this;
        Usuario usuario = Sesion.get().getUsuario();
        rol = usuario.getRol();
        nombreUsuario.setText(usuario.getNombreCompleto());
        rolUsuario.setText(rol.getEtiqueta());
        avatar.mostrar(Formatos.iniciales(usuario.getNombreCompleto()), usuario.getIdUsuario());
        fecha.setText(Formatos.fechaLarga(LocalDate.now()));

        construirMenu();
        configurarBuscadorGlobal();
        // Un clic sobre el ítem ya activo no debe dejar el menú sin selección
        grupoMenu.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
        });
        navegar(Pantalla.INICIO);
    }

    public void navegar(Pantalla destino) {
        navegar(destino, null);
    }

    /**
     * Carga la pantalla en el área de contenido; si el rol no la tiene permitida,
     * o la pantalla abierta tiene cambios que el usuario no quiere perder, no hace nada.
     */
    public void navegar(Pantalla destino, Object parametro) {
        if (!destino.permitidaPara(rol)) {
            return;
        }
        if (!puedeSalirDeLaPantallaActual()) {
            // El ToggleButton ya se marcó con el clic: se vuelve a marcar la pantalla abierta
            botonesMenu.get(pantallaActual).setSelected(true);
            return;
        }
        ScrollPane desplazable = new ScrollPane(cargarVista(destino, parametro));
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().add("contenido-scroll");
        contenido.getChildren().setAll(desplazable);

        pantallaActual = destino;
        botonesMenu.get(destino).setSelected(true);
        ruta.setText(destino == Pantalla.INICIO ? "Inicio" : "Inicio / " + destino.getTitulo());
        // En Inicio la barra superior muestra el buscador en lugar de la ruta (docs/ui/Inicio.html)
        boolean enInicio = destino == Pantalla.INICIO;
        buscadorGlobal.setVisible(enInicio);
        buscadorGlobal.setManaged(enInicio);
        ruta.setVisible(!enInicio);
        ruta.setManaged(!enInicio);
        campoGlobal.clear();
        sugerencias.hide();
    }

    @FXML
    private void salir() {
        if (!puedeSalirDeLaPantallaActual()) {
            return;
        }
        Servicios.auth().cerrarSesion();
        App.mostrarLogin();
    }

    // ===== Buscador global =====

    private void configurarBuscadorGlobal() {
        sugerencias.getStyleClass().add("sugerencias");
        esperaBusqueda.setOnFinished(e -> mostrarSugerencias());
        campoGlobal.textProperty().addListener((obs, anterior, nuevo) -> esperaBusqueda.playFromStart());
        campoGlobal.setOnAction(e -> {
            esperaBusqueda.stop();
            mostrarSugerencias();
            e.consume();
        });
        // Ctrl+K (Cmd+K en Mac) lleva al buscador desde cualquier pantalla
        menu.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null) {
                escena.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), () -> {
                    if (pantallaActual != Pantalla.INICIO) {
                        navegar(Pantalla.INICIO);
                    }
                    if (pantallaActual == Pantalla.INICIO) {
                        campoGlobal.requestFocus();
                    }
                });
            }
        });
    }

    /** Pacientes que coinciden; al elegir uno se abre su ficha en Pacientes. */
    private void mostrarSugerencias() {
        String texto = campoGlobal.getText();
        if (texto == null || texto.isBlank()) {
            sugerencias.hide();
            return;
        }
        List<Paciente> encontrados;
        try {
            encontrados = Servicios.pacientes().buscar(texto, FiltroPaciente.TODOS, 0).stream().limit(8).toList();
        } catch (RuntimeException e) {
            encontrados = List.of();
        }
        sugerencias.getItems().clear();
        if (encontrados.isEmpty()) {
            Label nada = new Label("No se encontraron pacientes");
            nada.getStyleClass().add("texto-tenue");
            CustomMenuItem item = new CustomMenuItem(nada, false);
            item.setDisable(true);
            sugerencias.getItems().add(item);
        }
        for (Paciente paciente : encontrados) {
            Avatar mini = new Avatar(paciente.getIniciales(), paciente.getIdPaciente());
            mini.getStyleClass().add("avatar-mini");
            Label nombre = new Label(paciente.getNombreCompleto());
            nombre.getStyleClass().add("celda-titulo");
            Label dni = new Label("DNI " + paciente.getDni());
            dni.getStyleClass().add("celda-sub");
            HBox fila = new HBox(10, mini, new VBox(nombre, dni));
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPrefWidth(buscadorGlobal.getWidth() - 24);
            CustomMenuItem item = new CustomMenuItem(fila, true);
            item.setOnAction(e -> navegar(Pantalla.PACIENTES, paciente));
            sugerencias.getItems().add(item);
        }
        if (!sugerencias.isShowing()) {
            sugerencias.show(buscadorGlobal, Side.BOTTOM, 0, 4);
        }
    }

    private boolean puedeSalirDeLaPantallaActual() {
        return !(controladorActual instanceof ConfirmaSalida pantalla) || pantalla.confirmarSalida();
    }

    private void construirMenu() {
        for (Pantalla.Grupo grupo : Pantalla.Grupo.values()) {
            VBox bloque = new VBox();
            bloque.getStyleClass().add("nav-bloque");
            for (Pantalla pantalla : Pantalla.values()) {
                if (pantalla.getGrupo() == grupo && pantalla.permitidaPara(rol)) {
                    bloque.getChildren().add(crearItem(pantalla));
                }
            }
            // Se oculta el grupo entero si el rol no ve ninguna de sus pantallas
            if (!bloque.getChildren().isEmpty()) {
                Label titulo = new Label(grupo.getTitulo().toUpperCase(Formatos.ES_PE));
                titulo.getStyleClass().add("nav-grupo");
                bloque.getChildren().add(0, titulo);
                menu.getChildren().add(bloque);
            }
        }
    }

    private Node crearItem(Pantalla pantalla) {
        ToggleButton item = new ToggleButton(pantalla.getTitulo(), new Icono(pantalla.getIcono(), 18));
        item.getStyleClass().add("nav-item");
        item.setMaxWidth(Double.MAX_VALUE);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setToggleGroup(grupoMenu);
        item.setOnAction(e -> navegar(pantalla));
        botonesMenu.put(pantalla, item);
        if (pantalla != Pantalla.AGENDA) {
            return item;
        }
        // Contador de citas de hoy, encima del ítem y sin capturar los clics
        contadorAgenda.getStyleClass().add("nav-badge");
        contadorAgenda.setMouseTransparent(true);
        StackPane.setAlignment(contadorAgenda, Pos.CENTER_RIGHT);
        StackPane.setMargin(contadorAgenda, new Insets(0, 12, 0, 0));
        actualizarContadorAgenda();
        return new StackPane(item, contadorAgenda);
    }

    /** Cantidad de citas de hoy (sin las canceladas) junto a "Agenda"; se oculta si es 0. */
    public void actualizarContadorAgenda() {
        int citasDeHoy;
        try {
            citasDeHoy = Servicios.citas().citasActivasDeHoy();
        } catch (RuntimeException e) {
            citasDeHoy = 0;
        }
        contadorAgenda.setText(String.valueOf(citasDeHoy));
        contadorAgenda.setVisible(citasDeHoy > 0);
        botonesMenu.get(Pantalla.AGENDA).setAccessibleText("Agenda, " + citasDeHoy + " citas hoy");
    }

    private Node cargarVista(Pantalla destino, Object parametro) {
        URL vista = getClass().getResource(destino.getRutaVista());
        controladorActual = null;
        if (vista == null) {
            return enConstruccion(destino);
        }
        try {
            FXMLLoader loader = new FXMLLoader(vista);
            Node raiz = loader.load();
            controladorActual = loader.getController();
            if (loader.getController() instanceof RecibeParametro receptor) {
                receptor.recibir(parametro);
            }
            return raiz;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo abrir la pantalla " + destino.getTitulo(), e);
        }
    }

    /** Marcador para las pantallas que todavía no tienen su FXML. */
    private Node enConstruccion(Pantalla pantalla) {
        Label titulo = new Label(pantalla.getTitulo());
        titulo.getStyleClass().add("titulo-pagina");

        StackPane circulo = new StackPane(new Icono(pantalla.getIcono(), 28));
        circulo.getStyleClass().add("estado-vacio-icono");
        Label encabezado = new Label("En construcción");
        encabezado.getStyleClass().add("card-titulo");
        Label detalle = new Label("Esta pantalla estará disponible en una próxima fase del proyecto.");
        detalle.getStyleClass().add("texto-secundario");

        VBox tarjeta = new VBox(circulo, encabezado, detalle);
        tarjeta.getStyleClass().addAll("card", "estado-vacio");
        tarjeta.setAlignment(Pos.CENTER);

        VBox pagina = new VBox(titulo, tarjeta);
        pagina.getStyleClass().add("pagina");
        return pagina;
    }
}
