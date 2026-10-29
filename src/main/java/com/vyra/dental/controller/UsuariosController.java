package com.vyra.dental.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.UsuarioService;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Usuarios y roles (docs/08-guia-frontend.md §7.11, RF-08). Solo administrador. La contraseña se
 * pide al crear; en un usuario existente se cambia con "Restablecer contraseña".
 */
public class UsuariosController {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;

    @FXML private Label resumen;
    @FXML private TableView<Usuario> tabla;
    @FXML private Avatar avatarFicha;
    @FXML private Label tituloFicha;
    @FXML private Label subtituloFicha;
    @FXML private CampoFormulario cfNombreCompleto;
    @FXML private CampoFormulario cfNombreUsuario;
    @FXML private CampoFormulario cfRol;
    @FXML private CampoFormulario cfContrasena;
    @FXML private TextField campoNombreCompleto;
    @FXML private TextField campoNombreUsuario;
    @FXML private HBox selectorRol;
    @FXML private PasswordField campoContrasena;
    @FXML private CheckBox campoActivo;
    @FXML private Aviso avisoFormulario;
    @FXML private Button botonRestablecer;
    @FXML private Button botonGuardar;

    private final UsuarioService servicio = Servicios.usuarios();
    private final ObservableList<Usuario> filas = FXCollections.observableArrayList();
    private final Map<String, CampoFormulario> campos = new LinkedHashMap<>();
    private final ToggleGroup grupoRol = new ToggleGroup();
    /** Usuario abierto; null cuando se crea uno nuevo. */
    private Usuario seleccionado;
    private boolean ignorarSeleccion;

    @FXML
    private void initialize() {
        campos.put("nombreCompleto", cfNombreCompleto);
        campos.put("nombreUsuario", cfNombreUsuario);
        campos.put("rol", cfRol);
        campos.put("contrasena", cfContrasena);
        campos.put("activo", new CampoFormulario());
        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextInputControl texto) {
                texto.textProperty().addListener((obs, antes, ahora) -> campo.limpiarError());
            }
        }
        for (Rol rol : Rol.values()) {
            ToggleButton boton = new ToggleButton(rol.getEtiqueta());
            boton.setUserData(rol);
            boton.setToggleGroup(grupoRol);
            selectorRol.getChildren().add(boton);
        }
        grupoRol.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else {
                cfRol.limpiarError();
            }
        });
        construirTabla();

        cargar();
        if (!filas.isEmpty()) {
            tabla.getSelectionModel().selectFirst();
        } else {
            prepararNuevo();
        }
    }

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(4, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.setPlaceholder(new EstadoVacio("Aún no hay usuarios", "Crea la primera cuenta de acceso."));

        TableColumn<Usuario, Usuario> nombre = columna("USUARIO", 230);
        nombre.setCellFactory(c -> new CeldaUsuario());
        TableColumn<Usuario, Usuario> rol = columna("ROL", 130);
        rol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario usuario, boolean vacio) {
                super.updateItem(usuario, vacio);
                setText(null);
                if (vacio || usuario == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(usuario.getRol().getEtiqueta());
                pill.getStyleClass().addAll("pill", "pill-neutra");
                setGraphic(pill);
            }
        });
        TableColumn<Usuario, Usuario> estado = columna("ESTADO", 110);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario usuario, boolean vacio) {
                super.updateItem(usuario, vacio);
                setText(null);
                if (vacio || usuario == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(usuario.isActivo() ? "Activo" : "Inactivo");
                pill.getStyleClass().addAll("pill", usuario.isActivo() ? "pill-atendida" : "pill-cancelada");
                setGraphic(pill);
            }
        });
        tabla.getColumns().setAll(List.of(nombre, rol, estado));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion) {
                mostrar(nuevo);
            }
        });
    }

    private void cargar() {
        try {
            filas.setAll(servicio.listarTodos());
            long activos = filas.stream().filter(Usuario::isActivo).count();
            resumen.setText(filas.size() + (filas.size() == 1 ? " usuario" : " usuarios") + " · " + activos
                    + (activos == 1 ? " activo" : " activos") + ". Un usuario desactivado no puede iniciar sesión.");
        } catch (RuntimeException e) {
            filas.clear();
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar los usuarios", e.getMessage()));
        }
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            if (seleccionado != null) {
                filas.stream().filter(u -> u.getIdUsuario() == seleccionado.getIdUsuario())
                        .findFirst().ifPresent(u -> tabla.getSelectionModel().select(u));
            }
        } finally {
            ignorarSeleccion = false;
        }
    }

    @FXML
    private void nuevo() {
        prepararNuevo();
    }

    private void prepararNuevo() {
        seleccionado = null;
        tabla.getSelectionModel().clearSelection();
        limpiarErrores();
        campoNombreCompleto.clear();
        campoNombreUsuario.clear();
        campoContrasena.clear();
        campoActivo.setSelected(true);
        campoActivo.setDisable(true);
        grupoRol.selectToggle(grupoRol.getToggles().get(Rol.RECEPCION.ordinal()));
        avatarFicha.mostrar("", 0);
        tituloFicha.setText("Nuevo usuario");
        subtituloFicha.setText("Los campos con * son obligatorios.");
        botonGuardar.setText("Crear usuario");
        alternar(cfContrasena, true);
        alternar(botonRestablecer, false);
        campoNombreCompleto.requestFocus();
    }

    private void mostrar(Usuario usuario) {
        seleccionado = usuario;
        limpiarErrores();
        campoNombreCompleto.setText(usuario.getNombreCompleto());
        campoNombreUsuario.setText(usuario.getNombreUsuario());
        campoContrasena.clear();
        campoActivo.setDisable(false);
        campoActivo.setSelected(usuario.isActivo());
        grupoRol.selectToggle(grupoRol.getToggles().get(usuario.getRol().ordinal()));
        avatarFicha.mostrar(Formatos.iniciales(usuario.getNombreCompleto()), usuario.getIdUsuario());
        tituloFicha.setText(usuario.getNombreCompleto());
        subtituloFicha.setText(usuario.getNombreUsuario() + " · " + usuario.getRol().getEtiqueta());
        botonGuardar.setText("Guardar cambios");
        // La contraseña se escribe solo para restablecerla: el campo se muestra con ese botón
        alternar(cfContrasena, true);
        cfContrasena.setEtiqueta("Contraseña nueva (para restablecerla)");
        alternar(botonRestablecer, true);
    }

    @FXML
    private void guardar() {
        limpiarErrores();
        Usuario datos = new Usuario();
        datos.setNombreCompleto(campoNombreCompleto.getText());
        datos.setNombreUsuario(campoNombreUsuario.getText());
        datos.setRol((Rol) grupoRol.getSelectedToggle().getUserData());
        datos.setActivo(campoActivo.isSelected());
        try {
            if (seleccionado == null) {
                Usuario creado = servicio.crear(datos, campoContrasena.getText());
                seleccionado = creado;
                notificar("Usuario creado");
            } else {
                datos.setIdUsuario(seleccionado.getIdUsuario());
                servicio.actualizar(datos);
                seleccionado = datos;
                notificar("Cambios guardados");
            }
            cargar();
            servicio.buscarPorId(seleccionado.getIdUsuario()).ifPresent(this::mostrar);
        } catch (ValidacionException e) {
            e.getErrores().forEach(this::mostrarError);
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(null), e.getMessage());
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    @FXML
    private void restablecer() {
        limpiarErrores();
        if (seleccionado == null) {
            return;
        }
        try {
            servicio.restablecerContrasena(seleccionado.getIdUsuario(), campoContrasena.getText());
            campoContrasena.clear();
            notificar("Contraseña restablecida");
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse("contrasena"), e.getMessage());
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    @FXML
    private void cancelar() {
        if (seleccionado == null) {
            prepararNuevo();
        } else {
            mostrar(seleccionado);
        }
    }

    private void mostrarError(String campo, String mensaje) {
        CampoFormulario destino = campo == null ? null : campos.get(campo);
        if (destino != null && destino.getControl() != null) {
            destino.mostrarError(mensaje);
        } else {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, mensaje);
        }
    }

    private void limpiarErrores() {
        campos.values().forEach(CampoFormulario::limpiarError);
        cfContrasena.setEtiqueta("Contraseña");
        avisoFormulario.ocultar();
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(tabla.getScene().getWindow(), mensaje);
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    private static TableColumn<Usuario, Usuario> columna(String titulo, double ancho) {
        TableColumn<Usuario, Usuario> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    /** Avatar + nombre completo + usuario. */
    private static class CeldaUsuario extends TableCell<Usuario, Usuario> {
        private final Avatar avatar = new Avatar();
        private final Label nombre = new Label();
        private final Label usuario = new Label();
        private final Node contenido = new HBox(avatar, new VBox(nombre, usuario));

        CeldaUsuario() {
            nombre.getStyleClass().add("celda-titulo");
            usuario.getStyleClass().add("celda-sub");
            contenido.getStyleClass().add("celda-paciente");
        }

        @Override
        protected void updateItem(Usuario u, boolean vacio) {
            super.updateItem(u, vacio);
            setText(null);
            if (vacio || u == null) {
                setGraphic(null);
                return;
            }
            avatar.mostrar(Formatos.iniciales(u.getNombreCompleto()), u.getIdUsuario());
            nombre.setText(u.getNombreCompleto());
            usuario.setText(u.getNombreUsuario());
            setGraphic(contenido);
        }
    }
}
