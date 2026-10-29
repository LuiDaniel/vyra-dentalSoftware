package com.vyra.dental.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import com.vyra.dental.App;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.util.Formatos;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    @FXML private Label fecha;
    @FXML private StackPane contenido;

    private final ToggleGroup grupoMenu = new ToggleGroup();
    private final Map<Pantalla, ToggleButton> botonesMenu = new EnumMap<>(Pantalla.class);
    private Rol rol;

    @FXML
    private void initialize() {
        Usuario usuario = Sesion.get().getUsuario();
        rol = usuario.getRol();
        nombreUsuario.setText(usuario.getNombreCompleto());
        rolUsuario.setText(rol.getEtiqueta());
        avatar.mostrar(Formatos.iniciales(usuario.getNombreCompleto()), usuario.getIdUsuario());
        fecha.setText(Formatos.fechaLarga(LocalDate.now()));

        construirMenu();
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

    /** Carga la pantalla en el área de contenido; si el rol no la tiene permitida, no hace nada. */
    public void navegar(Pantalla destino, Object parametro) {
        if (!destino.permitidaPara(rol)) {
            return;
        }
        ScrollPane desplazable = new ScrollPane(cargarVista(destino, parametro));
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().add("contenido-scroll");
        contenido.getChildren().setAll(desplazable);

        botonesMenu.get(destino).setSelected(true);
        ruta.setText(destino == Pantalla.INICIO ? "Inicio" : "Inicio / " + destino.getTitulo());
    }

    @FXML
    private void salir() {
        Servicios.auth().cerrarSesion();
        App.mostrarLogin();
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

    private ToggleButton crearItem(Pantalla pantalla) {
        ToggleButton item = new ToggleButton(pantalla.getTitulo(), new Icono(pantalla.getIcono(), 18));
        item.getStyleClass().add("nav-item");
        item.setMaxWidth(Double.MAX_VALUE);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setToggleGroup(grupoMenu);
        item.setOnAction(e -> navegar(pantalla));
        botonesMenu.put(pantalla, item);
        return item;
    }

    private Node cargarVista(Pantalla destino, Object parametro) {
        URL vista = getClass().getResource(destino.getRutaVista());
        if (vista == null) {
            return enConstruccion(destino);
        }
        try {
            FXMLLoader loader = new FXMLLoader(vista);
            Node raiz = loader.load();
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
