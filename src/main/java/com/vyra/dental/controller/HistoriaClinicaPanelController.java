package com.vyra.dental.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.service.HistoriaClinicaService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.LineaDeTiempo;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Historia clínica de un paciente (docs/08-guia-frontend.md §7.5, CU-04):
 * alergias y antecedentes, la línea de tiempo de entradas y el formulario de
 * nueva entrada. Es el controlador de HistoriaClinicaPanel.fxml, que se incluye
 * en la ficha de Pacientes y en la pantalla Historia clínica. El odontólogo
 * escribe; el administrador solo consulta.
 */
public class HistoriaClinicaPanelController {

    /** Opción "Sin cita" del combo (un ComboBox no maneja bien los null). */
    private static final Cita SIN_CITA = new Cita();

    @FXML private VBox raiz;
    @FXML private CampoFormulario cfAlergias;
    @FXML private CampoFormulario cfAntecedentes;
    @FXML private TextArea campoAlergias;
    @FXML private TextArea campoAntecedentes;
    @FXML private Aviso avisoDatos;
    @FXML private HBox accionesDatos;
    @FXML private Button botonGuardarDatos;
    @FXML private Button botonNuevaEntrada;
    @FXML private VBox formularioEntrada;
    @FXML private TextField campoFecha;
    @FXML private CampoFormulario cfCita;
    @FXML private ComboBox<Cita> campoCita;
    @FXML private CampoFormulario cfDiagnostico;
    @FXML private TextField campoDiagnostico;
    @FXML private CampoFormulario cfObservaciones;
    @FXML private TextArea campoObservaciones;
    @FXML private Aviso avisoEntrada;
    @FXML private VBox contenedorHistorial;
    @FXML private LineaDeTiempo lineaTiempo;

    private final HistoriaClinicaService servicio = Servicios.historiaClinica();
    /** Nombre del campo en el servicio → campo del formulario, para mostrar los errores. */
    private final Map<String, CampoFormulario> camposEntrada = new HashMap<>();

    private Paciente paciente;
    private boolean editable;
    private Consumer<Paciente> alActualizarPaciente = p -> { };

    @FXML
    private void initialize() {
        editable = servicio.puedeEditar();
        camposEntrada.put("idCita", cfCita);
        camposEntrada.put("diagnostico", cfDiagnostico);
        camposEntrada.put("observaciones", cfObservaciones);

        campoCita.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cita cita) {
                if (cita == null || cita == SIN_CITA) {
                    return "Sin cita";
                }
                return Formatos.rangoHoras(cita.getHoraInicio(), cita.getHoraFin())
                        + (cita.getMotivo() == null ? "" : " · " + cita.getMotivo());
            }

            @Override
            public Cita fromString(String texto) {
                return null;
            }
        });

        // Al corregir un campo se borra su error
        campoDiagnostico.textProperty().addListener((obs, anterior, nuevo) -> cfDiagnostico.limpiarError());
        campoObservaciones.textProperty().addListener((obs, anterior, nuevo) -> cfDiagnostico.limpiarError());
        campoCita.valueProperty().addListener((obs, anterior, nuevo) -> cfCita.limpiarError());
        // Enter en el diagnóstico guarda la entrada, y no activa otro botón de la pantalla
        campoDiagnostico.setOnAction(e -> {
            guardarEntrada();
            e.consume();
        });
        campoAlergias.textProperty().addListener((obs, anterior, nuevo) -> actualizarBotonDatos());
        campoAntecedentes.textProperty().addListener((obs, anterior, nuevo) -> actualizarBotonDatos());

        aplicarPermisos();
        alternar(formularioEntrada, false);
    }

    /** Muestra la historia clínica del paciente. */
    public void mostrar(Paciente nuevo) {
        paciente = nuevo;
        avisoDatos.ocultar();
        campoAlergias.setText(Objects.requireNonNullElse(paciente.getAlergias(), ""));
        campoAntecedentes.setText(Objects.requireNonNullElse(paciente.getAntecedentes(), ""));
        actualizarBotonDatos();
        cerrarFormulario();
        cargarHistorial();
    }

    /** Se llama cuando el odontólogo guarda las alergias o los antecedentes. */
    public void setAlActualizarPaciente(Consumer<Paciente> accion) {
        alActualizarPaciente = accion;
    }

    // ===== Alergias y antecedentes =====

    @FXML
    private void guardarDatos() {
        avisoDatos.ocultar();
        cfAlergias.limpiarError();
        cfAntecedentes.limpiarError();
        try {
            paciente = servicio.actualizarDatosClinicos(paciente.getIdPaciente(), campoAlergias.getText(),
                    campoAntecedentes.getText());
            actualizarBotonDatos();
            notificar("Alergias y antecedentes guardados");
            alActualizarPaciente.accept(paciente);
        } catch (ValidacionException e) {
            avisoDatos.mostrar(Aviso.Tipo.ERROR, String.join(". ", e.getErrores().values()) + ".");
        } catch (RuntimeException e) {
            avisoDatos.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    /** El botón se habilita solo si las alergias o los antecedentes cambiaron. */
    private void actualizarBotonDatos() {
        if (paciente == null) {
            botonGuardarDatos.setDisable(true);
            return;
        }
        boolean cambio = !igual(campoAlergias.getText(), paciente.getAlergias())
                || !igual(campoAntecedentes.getText(), paciente.getAntecedentes());
        botonGuardarDatos.setDisable(!cambio);
    }

    // ===== Nueva entrada =====

    @FXML
    private void nuevaEntrada() {
        limpiarErroresEntrada();
        campoFecha.setText(Formatos.fechaCorta(LocalDate.now()));
        campoDiagnostico.clear();
        campoObservaciones.clear();
        List<Cita> opciones = new ArrayList<>();
        opciones.add(SIN_CITA);
        try {
            opciones.addAll(Servicios.citas().citasDeHoyDePaciente(paciente.getIdPaciente()));
        } catch (RuntimeException e) {
            avisoEntrada.mostrar(Aviso.Tipo.ADVERTENCIA, "No se pudo consultar las citas de hoy: " + e.getMessage());
        }
        campoCita.getItems().setAll(opciones);
        // Por defecto, la cita de hoy del paciente (la primera, si tiene varias)
        campoCita.setValue(opciones.size() > 1 ? opciones.get(1) : SIN_CITA);

        alternar(formularioEntrada, true);
        alternar(botonNuevaEntrada, false);
        campoDiagnostico.requestFocus();
    }

    @FXML
    private void guardarEntrada() {
        limpiarErroresEntrada();
        Cita cita = campoCita.getValue();
        HistoriaClinicaEntrada entrada = new HistoriaClinicaEntrada();
        entrada.setIdPaciente(paciente.getIdPaciente());
        entrada.setIdCita(cita == null || cita == SIN_CITA ? null : cita.getIdCita());
        entrada.setDiagnostico(campoDiagnostico.getText());
        entrada.setObservaciones(campoObservaciones.getText());
        try {
            servicio.registrarEntrada(entrada);
            notificar("Entrada registrada");
            cerrarFormulario();
            cargarHistorial();
        } catch (ValidacionException e) {
            e.getErrores().forEach(this::mostrarErrorEntrada);
        } catch (ReglaNegocioException e) {
            mostrarErrorEntrada(e.getCampo().orElse(null), e.getMessage());
        } catch (RuntimeException e) {
            avisoEntrada.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    @FXML
    private void cancelarEntrada() {
        cerrarFormulario();
    }

    private void cerrarFormulario() {
        limpiarErroresEntrada();
        alternar(formularioEntrada, false);
        alternar(botonNuevaEntrada, editable);
    }

    private void mostrarErrorEntrada(String campo, String mensaje) {
        CampoFormulario destino = campo == null ? null : camposEntrada.get(campo);
        if (destino == null) {
            avisoEntrada.mostrar(Aviso.Tipo.ERROR, mensaje);
            return;
        }
        destino.mostrarError(mensaje);
        destino.getControl().requestFocus();
    }

    private void limpiarErroresEntrada() {
        camposEntrada.values().forEach(CampoFormulario::limpiarError);
        avisoEntrada.ocultar();
    }

    // ===== Línea de tiempo =====

    private void cargarHistorial() {
        lineaTiempo.limpiar();
        contenedorHistorial.getChildren().setAll(lineaTiempo);
        List<HistoriaClinicaEntrada> entradas;
        Map<Integer, String> profesionales = new HashMap<>();
        try {
            entradas = servicio.historial(paciente.getIdPaciente());
            for (Profesional profesional : Servicios.profesionales().listarTodos()) {
                profesionales.put(profesional.getIdProfesional(), profesional.getNombreCompleto());
            }
        } catch (RuntimeException e) {
            contenedorHistorial.getChildren().setAll(
                    new EstadoVacio("No se pudo cargar la historia clínica", e.getMessage()));
            return;
        }
        if (entradas.isEmpty()) {
            contenedorHistorial.getChildren().setAll(new EstadoVacio("Sin entradas registradas",
                    editable ? "Usa «Nueva entrada» para registrar la primera visita."
                            : "Las entradas que registre el odontólogo aparecerán aquí."));
            return;
        }
        for (HistoriaClinicaEntrada entrada : entradas) {
            String detalle = Formatos.fechaCorta(entrada.getFecha());
            String profesional = entrada.getIdProfesional() == null ? null : profesionales.get(entrada.getIdProfesional());
            if (profesional != null) {
                detalle += " · " + profesional;
            }
            // Sin diagnóstico, las observaciones van como título
            if (entrada.getDiagnostico() != null) {
                lineaTiempo.agregar(entrada.getDiagnostico(), detalle, entrada.getObservaciones(), null);
            } else {
                lineaTiempo.agregar(entrada.getObservaciones(), detalle, null, null);
            }
        }
    }

    // ===== Permisos =====

    /** El administrador ve la historia clínica, pero no puede escribir en ella. */
    private void aplicarPermisos() {
        campoAlergias.setEditable(editable);
        campoAntecedentes.setEditable(editable);
        campoAlergias.setPromptText(editable ? "Vacío si no tiene alergias" : "Sin alergias registradas");
        campoAntecedentes.setPromptText(editable ? "Enfermedades, cirugías, medicación habitual…"
                : "Sin antecedentes registrados");
        alternar(accionesDatos, editable);
        alternar(botonNuevaEntrada, editable);
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(raiz.getScene().getWindow(), mensaje);
    }

    private static boolean igual(String escrito, String guardado) {
        return Objects.requireNonNullElse(escrito, "").strip().equals(Objects.requireNonNullElse(guardado, ""));
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
