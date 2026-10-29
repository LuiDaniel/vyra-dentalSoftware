package com.vyra.dental.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.service.ProfesionalService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.Dialogos;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Registro de profesionales (docs/08-guia-frontend.md §7.10, CU-07). Solo Administrador.
 */
public class ProfesionalesController {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;

    @FXML private Label resumen;
    @FXML private TableView<Profesional> tabla;
    @FXML private Avatar avatarFicha;
    @FXML private Label tituloFicha;
    @FXML private Label subtituloFicha;
    @FXML private CampoFormulario cfNombres;
    @FXML private CampoFormulario cfApellidos;
    @FXML private CampoFormulario cfEspecialidad;
    @FXML private CampoFormulario cfTelefono;
    @FXML private CampoFormulario cfHorario;
    @FXML private CampoFormulario cfUsuario;
    @FXML private TextField campoNombres;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoEspecialidad;
    @FXML private TextField campoTelefono;
    @FXML private TextField campoHorario;
    @FXML private ComboBox<Usuario> campoUsuario;
    @FXML private Aviso avisoFormulario;
    @FXML private Button botonEliminar;
    @FXML private Button botonGuardar;

    private final ProfesionalService servicio = Servicios.profesionales();
    private final ObservableList<Profesional> filas = FXCollections.observableArrayList();
    private final Map<String, CampoFormulario> campos = new LinkedHashMap<>();
    /** Profesional abierto; null cuando se registra uno nuevo. */
    private Profesional seleccionado;
    private boolean ignorarSeleccion;

    @FXML
    private void initialize() {
        campos.put("nombres", cfNombres);
        campos.put("apellidos", cfApellidos);
        campos.put("especialidad", cfEspecialidad);
        campos.put("telefono", cfTelefono);
        campos.put("horarioAtencion", cfHorario);
        campos.put("idUsuario", cfUsuario);
        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextInputControl texto) {
                texto.textProperty().addListener((obs, anterior, nuevo) -> campo.limpiarError());
            }
        }
        campoUsuario.valueProperty().addListener((obs, anterior, nuevo) -> cfUsuario.limpiarError());
        configurarComboUsuario();
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
        tabla.setPlaceholder(new EstadoVacio("Aún no hay profesionales registrados",
                "Registra a los odontólogos del consultorio para poder agendar citas."));

        TableColumn<Profesional, Profesional> nombre = columna("PROFESIONAL", 240);
        nombre.setCellFactory(c -> new CeldaProfesional());
        tabla.getColumns().setAll(List.of(nombre,
                columnaTexto("TELÉFONO", 110, p -> p.getTelefono() == null ? "—" : Formatos.telefono(p.getTelefono())),
                columnaTexto("HORARIO", 160, p -> p.getHorarioAtencion() == null ? "—" : p.getHorarioAtencion()),
                columnaTexto("USUARIO", 120, p -> servicio.usuarioDe(p).map(Usuario::getNombreUsuario).orElse("Sin usuario"))));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion) {
                mostrar(nuevo);
            }
        });
    }

    private void cargar() {
        try {
            filas.setAll(servicio.listarTodos());
            resumen.setText(filas.size() + (filas.size() == 1 ? " profesional registrado" : " profesionales registrados")
                    + " · cada uno tiene su columna en la agenda.");
        } catch (RuntimeException e) {
            filas.clear();
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar la lista de profesionales", e.getMessage()));
        }
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            if (seleccionado != null) {
                filas.stream().filter(p -> p.getIdProfesional() == seleccionado.getIdProfesional())
                        .findFirst().ifPresent(p -> tabla.getSelectionModel().select(p));
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
        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextInputControl texto) {
                texto.clear();
            }
        }
        cargarUsuarios(0, null);
        avatarFicha.mostrar("", 0);
        tituloFicha.setText("Nuevo profesional");
        subtituloFicha.setText("Los campos con * son obligatorios.");
        botonGuardar.setText("Registrar profesional");
        botonEliminar.setVisible(false);
        botonEliminar.setManaged(false);
        campoNombres.requestFocus();
    }

    private void mostrar(Profesional profesional) {
        seleccionado = profesional;
        limpiarErrores();
        campoNombres.setText(profesional.getNombres());
        campoApellidos.setText(profesional.getApellidos());
        campoEspecialidad.setText(profesional.getEspecialidad());
        campoTelefono.setText(profesional.getTelefono() == null ? "" : Formatos.telefono(profesional.getTelefono()));
        campoHorario.setText(profesional.getHorarioAtencion());
        cargarUsuarios(profesional.getIdProfesional(), profesional.getIdUsuario());

        avatarFicha.mostrar(Formatos.iniciales(profesional.getNombres(), profesional.getApellidos()),
                profesional.getIdProfesional());
        tituloFicha.setText(profesional.getNombreCompleto());
        subtituloFicha.setText(profesional.getEspecialidad() == null ? "Sin especialidad registrada"
                : profesional.getEspecialidad());
        botonGuardar.setText("Guardar cambios");
        botonEliminar.setVisible(true);
        botonEliminar.setManaged(true);
    }

    @FXML
    private void guardar() {
        limpiarErrores();
        Profesional profesional = new Profesional();
        if (seleccionado != null) {
            profesional.setIdProfesional(seleccionado.getIdProfesional());
        }
        profesional.setNombres(campoNombres.getText());
        profesional.setApellidos(campoApellidos.getText());
        profesional.setEspecialidad(campoEspecialidad.getText());
        profesional.setTelefono(campoTelefono.getText());
        profesional.setHorarioAtencion(campoHorario.getText());
        Usuario usuario = campoUsuario.getValue();
        profesional.setIdUsuario(usuario == null || usuario.getIdUsuario() == 0 ? null : usuario.getIdUsuario());
        try {
            if (seleccionado == null) {
                servicio.registrar(profesional);
                notificar("Profesional registrado");
            } else {
                servicio.actualizar(profesional);
                notificar("Cambios guardados");
            }
            seleccionado = profesional;
            cargar();
            mostrar(profesional);
        } catch (ValidacionException e) {
            e.getErrores().forEach((campo, mensaje) -> mostrarError(campo, mensaje));
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(null), e.getMessage());
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

    @FXML
    private void eliminar() {
        if (seleccionado == null || !Dialogos.confirmar(tabla.getScene().getWindow(), "Eliminar profesional",
                "¿Eliminar a " + seleccionado.getNombreCompleto() + "?",
                "Solo se puede eliminar si no tiene citas registradas.", "Eliminar")) {
            return;
        }
        try {
            servicio.eliminar(seleccionado.getIdProfesional());
            notificar("Profesional eliminado");
            seleccionado = null;
            cargar();
            if (!filas.isEmpty()) {
                tabla.getSelectionModel().selectFirst();
            } else {
                prepararNuevo();
            }
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    /** "Sin usuario" + los odontólogos libres (y el actual, si tiene). */
    private void cargarUsuarios(int idProfesional, Integer idUsuarioActual) {
        Usuario ninguno = new Usuario();
        ninguno.setNombreCompleto("Sin usuario");
        List<Usuario> opciones = new ArrayList<>();
        opciones.add(ninguno);
        opciones.addAll(servicio.usuariosDisponibles(idProfesional));
        campoUsuario.getItems().setAll(opciones);
        campoUsuario.setValue(opciones.stream()
                .filter(u -> idUsuarioActual != null && u.getIdUsuario() == idUsuarioActual)
                .findFirst().orElse(ninguno));
    }

    private void configurarComboUsuario() {
        campoUsuario.setCellFactory(lista -> new CeldaUsuario());
        campoUsuario.setButtonCell(new CeldaUsuario());
    }

    private void mostrarError(String campo, String mensaje) {
        CampoFormulario destino = campo == null ? null : campos.get(campo);
        if (destino != null) {
            destino.mostrarError(mensaje);
        } else {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, mensaje);
        }
    }

    private void limpiarErrores() {
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoFormulario.ocultar();
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(tabla.getScene().getWindow(), mensaje);
    }

    private static TableColumn<Profesional, Profesional> columna(String titulo, double ancho) {
        TableColumn<Profesional, Profesional> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static TableColumn<Profesional, Profesional> columnaTexto(String titulo, double ancho,
                                                                     Function<Profesional, String> texto) {
        TableColumn<Profesional, Profesional> columna = columna(titulo, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Profesional profesional, boolean vacio) {
                super.updateItem(profesional, vacio);
                setText(vacio || profesional == null ? null : texto.apply(profesional));
            }
        });
        return columna;
    }

    /** Avatar + nombre + especialidad. */
    private static class CeldaProfesional extends TableCell<Profesional, Profesional> {
        private final Avatar avatar = new Avatar();
        private final Label nombre = new Label();
        private final Label especialidad = new Label();
        private final Node contenido = new HBox(avatar, new VBox(nombre, especialidad));

        CeldaProfesional() {
            nombre.getStyleClass().add("celda-titulo");
            especialidad.getStyleClass().add("celda-sub");
            contenido.getStyleClass().add("celda-paciente");
        }

        @Override
        protected void updateItem(Profesional profesional, boolean vacio) {
            super.updateItem(profesional, vacio);
            setText(null);
            if (vacio || profesional == null) {
                setGraphic(null);
                return;
            }
            avatar.mostrar(Formatos.iniciales(profesional.getNombres(), profesional.getApellidos()),
                    profesional.getIdProfesional());
            nombre.setText(profesional.getNombreCompleto());
            especialidad.setText(profesional.getEspecialidad() == null ? "Sin especialidad" : profesional.getEspecialidad());
            setGraphic(contenido);
        }
    }

    /** "Marco Salinas (msalinas)" o "Sin usuario". */
    private static class CeldaUsuario extends ListCell<Usuario> {
        @Override
        protected void updateItem(Usuario usuario, boolean vacio) {
            super.updateItem(usuario, vacio);
            if (vacio || usuario == null) {
                setText(null);
            } else if (usuario.getNombreUsuario() == null) {
                setText(usuario.getNombreCompleto());
            } else {
                setText(usuario.getNombreCompleto() + " (" + usuario.getNombreUsuario() + ")");
            }
        }
    }
}
