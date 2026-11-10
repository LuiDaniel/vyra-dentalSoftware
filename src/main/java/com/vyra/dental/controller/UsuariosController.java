package com.vyra.dental.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.service.AuthService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.UsuarioService;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Usuarios y roles (docs/08-guia-frontend.md §7.11, RF-08, RN-13, RN-14). Solo
 * el Administrador. Los usuarios no se borran: se desactivan.
 */
public class UsuariosController {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;
    private static final int FILAS_MINIMAS = 5;

    /** Qué ve cada rol, para ayudar a elegirlo (docs/02-casos-de-uso.md §3). */
    private static final Map<Rol, String> AYUDA_ROL = Map.of(
            Rol.ADMIN, "Ve y gestiona todo, incluidos reportes, profesionales y usuarios.",
            Rol.RECEPCION, "Pacientes, agenda y pagos. Ve los tratamientos para cobrar; no ve la parte clínica.",
            Rol.ODONTOLOGO, "Sus citas, historia clínica, odontograma y tratamientos. Ve los pacientes sin editarlos.");

    @FXML private VBox raiz;
    @FXML private Label resumen;
    @FXML private TextField campoBuscar;
    @FXML private TableView<Usuario> tabla;
    @FXML private Label pie;

    @FXML private Avatar avatarFicha;
    @FXML private Label tituloFicha;
    @FXML private Label subtituloFicha;
    @FXML private CampoFormulario cfNombreCompleto;
    @FXML private TextField campoNombreCompleto;
    @FXML private CampoFormulario cfNombreUsuario;
    @FXML private TextField campoNombreUsuario;
    @FXML private CampoFormulario cfRol;
    @FXML private ToggleButton rolAdmin;
    @FXML private ToggleButton rolRecepcion;
    @FXML private ToggleButton rolOdontologo;
    @FXML private Label ayudaRol;
    @FXML private Button botonRestablecer;
    @FXML private CampoFormulario cfContrasena;
    @FXML private PasswordField campoContrasena;
    @FXML private CheckBox campoActivo;
    @FXML private Label ayudaActivo;
    @FXML private Aviso avisoProfesional;
    @FXML private Aviso avisoFormulario;
    @FXML private Button botonGuardar;

    private final UsuarioService servicio = Servicios.usuarios();
    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> filas = new FilteredList<>(usuarios);
    private final ToggleGroup grupoRol = new ToggleGroup();
    /** Nombre del campo en el modelo o el servicio → campo del formulario. */
    private final Map<String, CampoFormulario> campos = new HashMap<>();

    /** Usuario abierto; null si se está creando uno. */
    private Usuario seleccionado;
    private boolean ignorarSeleccion;

    @FXML
    private void initialize() {
        campos.put("nombreCompleto", cfNombreCompleto);
        campos.put("nombreUsuario", cfNombreUsuario);
        campos.put("rol", cfRol);
        campos.put("contrasena", cfContrasena);
        campos.put("contrasenaHash", cfContrasena);

        construirTabla();
        configurarFormulario();
        campoBuscar.textProperty().addListener((obs, anterior, texto) -> {
            String buscado = Busqueda.normalizar(texto);
            filas.setPredicate(u -> buscado.isEmpty()
                    || Busqueda.normalizar(u.getNombreCompleto()).contains(buscado)
                    || Busqueda.normalizar(u.getNombreUsuario()).contains(buscado));
            actualizarPie();
        });
        cargar();
        if (!filas.isEmpty()) {
            tabla.getSelectionModel().selectFirst();
        } else {
            prepararNuevo();
        }
    }

    // ===== Lista =====

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(FILAS_MINIMAS, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.setPlaceholder(new EstadoVacio("No se encontraron usuarios", "Prueba con otro nombre."));

        TableColumn<Usuario, Usuario> usuario = columna("USUARIO", 260);
        usuario.setCellFactory(c -> new TableCell<>() {
            private final Avatar avatar = new Avatar();
            private final Label nombre = new Label();
            private final Label cuenta = new Label();
            private final HBox contenido = new HBox(avatar, new VBox(nombre, cuenta));

            {
                nombre.getStyleClass().add("celda-titulo");
                cuenta.getStyleClass().add("celda-sub");
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
                nombre.setText(u.getNombreCompleto() + (servicio.esUsuarioDeLaSesion(u.getIdUsuario()) ? " (tú)" : ""));
                cuenta.setText("@" + u.getNombreUsuario());
                setGraphic(contenido);
            }
        });

        TableColumn<Usuario, Usuario> rol = columna("ROL", 130);
        rol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                setText(null);
                setGraphic(vacio || u == null ? null : new Pill(u.getRol().getEtiqueta(), u.getRol().getClaseCss()));
            }
        });

        TableColumn<Usuario, Usuario> estado = columna("ESTADO", 100);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                setText(null);
                setGraphic(vacio || u == null ? null
                        : u.isActivo() ? new Pill("Activo", "pill-atendida") : new Pill("Inactivo", "pill-neutra"));
            }
        });

        tabla.getColumns().setAll(List.of(usuario, rol, estado));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion) {
                mostrar(nuevo);
            }
        });
    }

    private void cargar() {
        try {
            usuarios.setAll(servicio.listarTodos());
        } catch (RuntimeException e) {
            usuarios.clear();
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar los usuarios", e.getMessage()));
        }
        long activos = usuarios.stream().filter(Usuario::isActivo).count();
        resumen.setText(usuarios.size() + (usuarios.size() == 1 ? " usuario · " : " usuarios · ") + activos
                + (activos == 1 ? " activo" : " activos") + ". Los usuarios no se borran: se desactivan.");
        actualizarPie();
        reseleccionar();
    }

    private void actualizarPie() {
        pie.setText(filas.size() == usuarios.size() ? filas.size() + " usuarios"
                : "Mostrando " + filas.size() + " de " + usuarios.size());
    }

    private void reseleccionar() {
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            if (seleccionado != null) {
                filas.stream().filter(u -> u.getIdUsuario() == seleccionado.getIdUsuario()).findFirst()
                        .ifPresent(u -> tabla.getSelectionModel().select(u));
            }
        } finally {
            ignorarSeleccion = false;
        }
    }

    // ===== Ficha =====

    @FXML
    private void nuevo() {
        prepararNuevo();
    }

    private void prepararNuevo() {
        seleccionado = null;
        reseleccionar();
        limpiarErrores();
        campoNombreCompleto.clear();
        campoNombreUsuario.clear();
        campoContrasena.clear();
        rolRecepcion.setSelected(true);
        campoActivo.setSelected(true);
        avatarFicha.mostrar("", 0);
        tituloFicha.setText("Nuevo usuario");
        subtituloFicha.setText("Completa sus datos. Los campos con * son obligatorios.");
        botonGuardar.setText("Crear usuario");
        cfContrasena.setEtiqueta("Contraseña");
        cfContrasena.setObligatorio(true);
        campoContrasena.setPromptText("Al menos " + AuthService.LARGO_MINIMO_CONTRASENA + " caracteres");
        alternar(cfContrasena, true);
        alternar(botonRestablecer, false);
        // Un usuario nuevo siempre empieza activo
        campoActivo.setDisable(true);
        ayudaActivo.setText("Empieza activo. Podrás desactivarlo después.");
        habilitarRol(true);
        avisoProfesional.ocultar();
        campoNombreCompleto.requestFocus();
    }

    private void mostrar(Usuario usuario) {
        seleccionado = usuario;
        limpiarErrores();
        campoNombreCompleto.setText(usuario.getNombreCompleto());
        campoNombreUsuario.setText(usuario.getNombreUsuario());
        botonRol(usuario.getRol()).setSelected(true);
        campoActivo.setSelected(usuario.isActivo());
        campoContrasena.clear();
        avatarFicha.mostrar(Formatos.iniciales(usuario.getNombreCompleto()), usuario.getIdUsuario());
        tituloFicha.setText(usuario.getNombreCompleto());
        subtituloFicha.setText("@" + usuario.getNombreUsuario() + " · " + usuario.getRol().getEtiqueta()
                + (usuario.isActivo() ? "" : " · Inactivo"));
        botonGuardar.setText("Guardar cambios");
        alternar(cfContrasena, false);
        alternar(botonRestablecer, true);

        boolean esYo = servicio.esUsuarioDeLaSesion(usuario.getIdUsuario());
        campoActivo.setDisable(esYo);
        habilitarRol(!esYo);
        ayudaActivo.setText(esYo ? "No puedes desactivar tu propio usuario ni quitarte el rol de Administrador."
                : "Un usuario inactivo no puede iniciar sesión (RN-14).");
        mostrarProfesional(usuario);
    }

    /** El odontólogo necesita un profesional asociado para ver su agenda. */
    private void mostrarProfesional(Usuario usuario) {
        avisoProfesional.ocultar();
        if (usuario.getRol() != Rol.ODONTOLOGO) {
            return;
        }
        Optional<Profesional> profesional;
        try {
            profesional = servicio.profesionalDe(usuario.getIdUsuario());
        } catch (RuntimeException e) {
            return;
        }
        profesional.ifPresentOrElse(
                p -> avisoProfesional.mostrar(Aviso.Tipo.INFO, "Asociado al profesional " + p.getNombreCompleto() + "."),
                () -> avisoProfesional.mostrar(Aviso.Tipo.ADVERTENCIA,
                        "No está asociado a ningún profesional: no verá citas. Asócialo en Profesionales."));
    }

    @FXML
    private void mostrarRestablecer() {
        cfContrasena.setEtiqueta("Nueva contraseña");
        cfContrasena.setObligatorio(false);
        campoContrasena.setPromptText("Vacía para conservar la actual");
        alternar(cfContrasena, true);
        alternar(botonRestablecer, false);
        campoContrasena.requestFocus();
    }

    @FXML
    private void guardar() {
        limpiarErrores();
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(campoNombreCompleto.getText());
        usuario.setNombreUsuario(campoNombreUsuario.getText());
        usuario.setRol(rolElegido());
        usuario.setActivo(campoActivo.isSelected());
        try {
            if (seleccionado == null) {
                Usuario creado = servicio.crear(usuario, campoContrasena.getText());
                notificar("Usuario creado");
                seleccionado = creado;
            } else {
                usuario.setIdUsuario(seleccionado.getIdUsuario());
                servicio.actualizar(usuario);
                String nueva = campoContrasena.getText();
                if (nueva != null && !nueva.isEmpty()) {
                    servicio.restablecerContrasena(usuario.getIdUsuario(), nueva);
                    notificar("Cambios y contraseña guardados");
                } else {
                    notificar("Cambios guardados");
                }
            }
            int id = seleccionado.getIdUsuario();
            cargar();
            usuarios.stream().filter(u -> u.getIdUsuario() == id).findFirst().ifPresent(this::mostrar);
            reseleccionar();
        } catch (ValidacionException e) {
            e.getErrores().forEach(this::mostrarError);
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(""), e.getMessage());
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

    private void configurarFormulario() {
        rolAdmin.setToggleGroup(grupoRol);
        rolRecepcion.setToggleGroup(grupoRol);
        rolOdontologo.setToggleGroup(grupoRol);
        grupoRol.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                // Siempre queda un rol elegido
                anterior.setSelected(true);
                return;
            }
            cfRol.limpiarError();
            ayudaRol.setText(AYUDA_ROL.get(rolElegido()));
        });
        for (CampoFormulario campo : List.of(cfNombreCompleto, cfNombreUsuario, cfContrasena)) {
            ((TextField) campo.getControl()).textProperty().addListener((obs, anterior, nuevo) -> campo.limpiarError());
        }
        // Enter en un campo de texto guarda
        for (TextField campo : List.of(campoNombreCompleto, campoNombreUsuario, campoContrasena)) {
            campo.setOnAction(e -> {
                guardar();
                e.consume();
            });
        }
    }

    private Rol rolElegido() {
        if (rolAdmin.isSelected()) {
            return Rol.ADMIN;
        }
        return rolOdontologo.isSelected() ? Rol.ODONTOLOGO : Rol.RECEPCION;
    }

    private ToggleButton botonRol(Rol rol) {
        return switch (rol) {
            case ADMIN -> rolAdmin;
            case RECEPCION -> rolRecepcion;
            case ODONTOLOGO -> rolOdontologo;
        };
    }

    private void habilitarRol(boolean habilitado) {
        rolAdmin.setDisable(!habilitado);
        rolRecepcion.setDisable(!habilitado);
        rolOdontologo.setDisable(!habilitado);
    }

    private void mostrarError(String campo, String mensaje) {
        CampoFormulario destino = campos.get(campo);
        if (destino == null || !destino.isVisible()) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, mensaje);
            return;
        }
        destino.mostrarError(mensaje);
        if (destino.getControl() instanceof TextField texto) {
            texto.requestFocus();
        }
    }

    private void limpiarErrores() {
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoFormulario.ocultar();
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(raiz.getScene().getWindow(), mensaje);
    }

    private static TableColumn<Usuario, Usuario> columna(String titulo, double ancho) {
        TableColumn<Usuario, Usuario> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
