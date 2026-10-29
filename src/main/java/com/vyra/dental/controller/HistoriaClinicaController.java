package com.vyra.dental.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.service.HistoriaClinicaService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Pestaña "Historia clínica" de la ficha del paciente (docs/08-guia-frontend.md §7.5, CU-04):
 * alergias y antecedentes arriba y, debajo, la línea de tiempo de entradas. El odontólogo
 * escribe; el administrador solo consulta. Se construye con código porque vive dentro de
 * PacientesView, que la muestra con {@link #mostrar(Paciente)}.
 */
public class HistoriaClinicaController {

    private final HistoriaClinicaService servicio = Servicios.historiaClinica();
    private final VBox contenedor;
    private final boolean puedeEscribir = servicio.puedeEscribir();
    /** Se llama cuando cambian las alergias o antecedentes, para que la ficha y la lista los reflejen. */
    private final Runnable alActualizarPaciente;

    private final TextArea campoAlergias = new TextArea();
    private final TextArea campoAntecedentes = new TextArea();
    private final CampoFormulario cfAlergias = new CampoFormulario();
    private final CampoFormulario cfAntecedentes = new CampoFormulario();
    private final Aviso avisoDatos = new Aviso();
    private final Button botonGuardarDatos = new Button("Guardar datos");

    private final Button botonNueva = new Button("Nueva entrada", new Icono("mas", 16));
    private final VBox formulario = new VBox();
    private final Label fechaEntrada = new Label();
    private final Label citaEntrada = new Label();
    private final CampoFormulario cfDiagnostico = new CampoFormulario();
    private final CampoFormulario cfObservaciones = new CampoFormulario();
    private final TextField campoDiagnostico = new TextField();
    private final TextArea campoObservaciones = new TextArea();
    private final Aviso avisoEntrada = new Aviso();
    private final VBox lineaDeTiempo = new VBox();

    private Paciente paciente;
    private Cita citaDeHoy;

    public HistoriaClinicaController(VBox contenedor, Runnable alActualizarPaciente) {
        this.contenedor = contenedor;
        this.alActualizarPaciente = alActualizarPaciente;
        construir();
    }

    /** Carga la historia del paciente; cualquier cambio sin guardar en esta pestaña se descarta. */
    public void mostrar(Paciente nuevo) {
        paciente = nuevo;
        if (paciente == null) {
            return;
        }
        campoAlergias.setText(paciente.getAlergias());
        campoAntecedentes.setText(paciente.getAntecedentes());
        avisoDatos.ocultar();
        cerrarFormulario();
        cargarEntradas();
    }

    // ===== Construcción =====

    private void construir() {
        Label tituloDatos = new Label("ALERGIAS Y ANTECEDENTES");
        tituloDatos.getStyleClass().add("seccion-titulo");

        campoAlergias.setPromptText("Vacío si no tiene alergias");
        campoAlergias.setPrefRowCount(2);
        campoAlergias.setWrapText(true);
        cfAlergias.setEtiqueta("Alergias");
        cfAlergias.setControl(campoAlergias);

        campoAntecedentes.setPromptText("Enfermedades, cirugías, medicación habitual…");
        campoAntecedentes.setPrefRowCount(3);
        campoAntecedentes.setWrapText(true);
        cfAntecedentes.setEtiqueta("Antecedentes");
        cfAntecedentes.setControl(campoAntecedentes);

        campoAlergias.setEditable(puedeEscribir);
        campoAntecedentes.setEditable(puedeEscribir);
        botonGuardarDatos.getStyleClass().addAll("btn-secundario", "btn-chico");
        botonGuardarDatos.setOnAction(e -> guardarDatosClinicos());
        alternar(botonGuardarDatos, puedeEscribir);

        HBox pieDatos = new HBox(botonGuardarDatos);
        pieDatos.getStyleClass().add("hc-acciones");

        Label tituloLinea = new Label("HISTORIAL");
        tituloLinea.getStyleClass().add("seccion-titulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        botonNueva.getStyleClass().addAll("btn-primario", "btn-chico");
        botonNueva.setOnAction(e -> abrirFormulario());
        alternar(botonNueva, puedeEscribir);
        HBox encabezadoLinea = new HBox(tituloLinea, espacio, botonNueva);
        encabezadoLinea.getStyleClass().add("hc-encabezado");

        construirFormulario();
        lineaDeTiempo.getStyleClass().add("hc-linea-tiempo");

        contenedor.getChildren().setAll(tituloDatos, cfAlergias, cfAntecedentes, avisoDatos, pieDatos,
                encabezadoLinea, formulario, lineaDeTiempo);
    }

    private void construirFormulario() {
        formulario.getStyleClass().add("hc-formulario");
        fechaEntrada.getStyleClass().add("celda-titulo");
        citaEntrada.getStyleClass().add("celda-sub");
        citaEntrada.setWrapText(true);

        campoDiagnostico.setPromptText("Ej. Caries profunda en la pieza 36");
        cfDiagnostico.setEtiqueta("Diagnóstico");
        cfDiagnostico.setControl(campoDiagnostico);

        campoObservaciones.setPromptText("Hallazgos, indicaciones, procedimiento realizado…");
        campoObservaciones.setPrefRowCount(3);
        campoObservaciones.setWrapText(true);
        cfObservaciones.setEtiqueta("Observaciones");
        cfObservaciones.setControl(campoObservaciones);

        campoDiagnostico.textProperty().addListener((obs, anterior, nuevo) -> cfDiagnostico.limpiarError());
        campoObservaciones.textProperty().addListener((obs, anterior, nuevo) -> cfObservaciones.limpiarError());

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().addAll("btn-secundario", "btn-chico");
        cancelar.setOnAction(e -> cerrarFormulario());
        Button guardar = new Button("Guardar entrada");
        guardar.getStyleClass().addAll("btn-primario", "btn-chico");
        guardar.setOnAction(e -> guardarEntrada());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(8, espacio, cancelar, guardar);

        Label aviso = new Label("Las entradas no se pueden editar ni borrar una vez guardadas.");
        aviso.getStyleClass().add("texto-secundario");
        aviso.setWrapText(true);

        formulario.getChildren().setAll(fechaEntrada, citaEntrada, cfDiagnostico, cfObservaciones, avisoEntrada,
                aviso, pie);
        alternar(formulario, false);
    }

    // ===== Acciones =====

    private void guardarDatosClinicos() {
        avisoDatos.ocultar();
        try {
            servicio.actualizarDatosClinicos(paciente.getIdPaciente(), campoAlergias.getText(),
                    campoAntecedentes.getText());
            paciente.setAlergias(campoAlergias.getText().strip().isEmpty() ? null : campoAlergias.getText().strip());
            paciente.setAntecedentes(
                    campoAntecedentes.getText().strip().isEmpty() ? null : campoAntecedentes.getText().strip());
            notificar("Datos clínicos guardados");
            alActualizarPaciente.run();
        } catch (ValidacionException e) {
            avisoDatos.mostrar(Aviso.Tipo.ERROR, String.join(". ", e.getErrores().values()) + ".");
        } catch (RuntimeException e) {
            avisoDatos.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    private void abrirFormulario() {
        cfDiagnostico.limpiarError();
        cfObservaciones.limpiarError();
        avisoEntrada.ocultar();
        campoDiagnostico.clear();
        campoObservaciones.clear();
        fechaEntrada.setText("Hoy · " + Formatos.diaCorto(LocalDate.now()) + " "
                + Formatos.fechaCorta(LocalDate.now()));
        citaDeHoy = servicio.citaDeHoy(paciente.getIdPaciente()).orElse(null);
        citaEntrada.setText(citaDeHoy == null ? "Sin cita asociada"
                : "Cita de hoy: " + Formatos.rangoHoras(citaDeHoy.getHoraInicio(), citaDeHoy.getHoraFin())
                        + (citaDeHoy.getMotivo() == null ? "" : " · " + citaDeHoy.getMotivo()));
        alternar(formulario, true);
        botonNueva.setDisable(true);
        campoDiagnostico.requestFocus();
    }

    private void cerrarFormulario() {
        alternar(formulario, false);
        botonNueva.setDisable(false);
    }

    private void guardarEntrada() {
        cfDiagnostico.limpiarError();
        cfObservaciones.limpiarError();
        avisoEntrada.ocultar();
        HistoriaClinicaEntrada entrada = new HistoriaClinicaEntrada();
        entrada.setIdPaciente(paciente.getIdPaciente());
        entrada.setIdCita(citaDeHoy == null ? null : citaDeHoy.getIdCita());
        entrada.setDiagnostico(campoDiagnostico.getText());
        entrada.setObservaciones(campoObservaciones.getText());
        try {
            servicio.registrarEntrada(entrada);
            cerrarFormulario();
            cargarEntradas();
            notificar("Entrada registrada");
        } catch (ValidacionException e) {
            Map<String, String> errores = e.getErrores();
            avisoEntrada.mostrar(Aviso.Tipo.ERROR, String.join(". ", errores.values()) + ".");
        } catch (ReglaNegocioException e) {
            if (e.getCampo().filter("diagnostico"::equals).isPresent()) {
                cfDiagnostico.mostrarError(e.getMessage());
                campoDiagnostico.requestFocus();
            } else {
                avisoEntrada.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            }
        } catch (RuntimeException e) {
            avisoEntrada.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    // ===== Línea de tiempo =====

    private void cargarEntradas() {
        lineaDeTiempo.getChildren().clear();
        List<HistoriaClinicaEntrada> entradas;
        try {
            entradas = servicio.entradasDe(paciente.getIdPaciente());
        } catch (RuntimeException e) {
            lineaDeTiempo.getChildren().add(new EstadoVacio("No se pudo cargar la historia clínica", e.getMessage()));
            return;
        }
        if (entradas.isEmpty()) {
            lineaDeTiempo.getChildren().add(new EstadoVacio("Sin entradas todavía",
                    puedeEscribir ? "Usa «Nueva entrada» para registrar el primer diagnóstico."
                            : "Las entradas que registre el odontólogo aparecerán aquí."));
            return;
        }
        for (int i = 0; i < entradas.size(); i++) {
            lineaDeTiempo.getChildren().add(filaDe(entradas.get(i), i == entradas.size() - 1));
        }
    }

    private Node filaDe(HistoriaClinicaEntrada entrada, boolean ultima) {
        Region punto = new Region();
        punto.getStyleClass().add("hc-punto");
        Region linea = new Region();
        linea.getStyleClass().add("hc-linea");
        VBox.setVgrow(linea, Priority.ALWAYS);
        linea.setVisible(!ultima);
        VBox riel = new VBox(punto, linea);
        riel.getStyleClass().add("hc-riel");

        Label fecha = new Label(Formatos.diaCorto(entrada.getFecha()) + " " + Formatos.fechaCorta(entrada.getFecha()));
        fecha.getStyleClass().add("celda-sub");
        VBox textos = new VBox(2, fecha);
        Optional<Cita> cita = servicio.citaDe(entrada);
        cita.ifPresent(c -> {
            Label quien = new Label(c.getProfesional().getNombreCompleto()
                    + (c.getMotivo() == null ? "" : " · " + c.getMotivo()));
            quien.getStyleClass().add("celda-sub");
            quien.setWrapText(true);
            textos.getChildren().add(quien);
        });
        if (entrada.getDiagnostico() != null) {
            Label diagnostico = new Label(entrada.getDiagnostico());
            diagnostico.getStyleClass().add("celda-titulo");
            diagnostico.setWrapText(true);
            textos.getChildren().add(diagnostico);
        }
        if (entrada.getObservaciones() != null) {
            Label observaciones = new Label(entrada.getObservaciones());
            observaciones.setWrapText(true);
            textos.getChildren().add(observaciones);
        }
        textos.getStyleClass().add("hc-entrada-texto");
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox fila = new HBox(12, riel, textos);
        fila.getStyleClass().add("hc-entrada");
        return fila;
    }

    // ===== Utilidades =====

    private void notificar(String mensaje) {
        Notificacion.mostrar(contenedor.getScene().getWindow(), mensaje);
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
