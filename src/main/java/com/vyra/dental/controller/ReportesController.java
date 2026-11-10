package com.vyra.dental.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.ReporteService;
import com.vyra.dental.service.ReporteService.ReporteCitas;
import com.vyra.dental.service.ReporteService.ReporteHistorial;
import com.vyra.dental.service.ReporteService.ReporteIngresos;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.ExportarPdf;
import com.vyra.dental.ui.GraficoBarras;
import com.vyra.dental.ui.Kpi;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Formatos;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Reportes (docs/08-guia-frontend.md §7.9, RF-09, CU-09): citas de un periodo,
 * historial de tratamientos de un paciente e ingresos por periodo. Solo el
 * Administrador. Se actualizan al cambiar cualquier filtro.
 */
public class ReportesController {

    private static final double ALTO_FILA = 52;
    private static final double ALTO_ENCABEZADO = 44;
    private static final int FILAS_MINIMAS = 4;
    /** Hasta este largo las barras de ingresos van por día; si no, por mes. */
    private static final int DIAS_BARRAS_DIARIAS = 31;
    /** Opción "Todos los profesionales" del combo (un ComboBox no maneja bien los null). */
    private static final Profesional TODOS = new Profesional();

    private enum Tipo {
        CITAS("Citas"), HISTORIAL("Historial de paciente"), INGRESOS("Ingresos");

        private final String titulo;

        Tipo(String titulo) {
            this.titulo = titulo;
        }
    }

    private enum Atajo {
        HOY("Hoy"), SEMANA("Esta semana"), MES("Este mes");

        private final String titulo;

        Atajo(String titulo) {
            this.titulo = titulo;
        }
    }

    /** Una fila de la tabla de ingresos por método. */
    private record FilaMetodo(MetodoPago metodo, int pagos, double total, double fraccion) {
    }

    @FXML private Label subtitulo;
    @FXML private HBox selectorTipo;
    @FXML private Button botonPdf;
    @FXML private HBox filtroFechas;
    @FXML private CampoFormulario cfDesde;
    @FXML private DatePicker campoDesde;
    @FXML private CampoFormulario cfHasta;
    @FXML private DatePicker campoHasta;
    @FXML private HBox atajos;
    @FXML private CampoFormulario cfProfesional;
    @FXML private ComboBox<Profesional> campoProfesional;
    @FXML private CampoFormulario cfPaciente;
    @FXML private HBox buscadorPaciente;
    @FXML private TextField campoPaciente;
    @FXML private HBox pacienteElegido;
    @FXML private Label nombrePaciente;
    @FXML private Aviso avisoFiltros;
    @FXML private HBox kpis;
    @FXML private Label tituloTabla;
    @FXML private VBox contenedorTabla;
    @FXML private Label pie;
    @FXML private VBox tarjetaBarras;
    @FXML private Label tituloBarras;
    @FXML private GraficoBarras grafico;

    private final ReporteService servicio = Servicios.reportes();
    private final ObservableList<Cita> filasCitas = FXCollections.observableArrayList();
    private final ObservableList<TratamientoResumen> filasHistorial = FXCollections.observableArrayList();
    private final ObservableList<FilaMetodo> filasIngresos = FXCollections.observableArrayList();
    private final TableView<Cita> tablaCitas = new TableView<>(filasCitas);
    private final TableView<TratamientoResumen> tablaHistorial = new TableView<>(filasHistorial);
    private final TableView<FilaMetodo> tablaIngresos = new TableView<>(filasIngresos);
    private final ToggleGroup grupoAtajos = new ToggleGroup();
    private final Map<Atajo, ToggleButton> botonesAtajo = new LinkedHashMap<>();
    private final ContextMenu sugerencias = new ContextMenu();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));

    private Tipo tipo = Tipo.CITAS;
    private Paciente paciente;
    /** true mientras se cambian las fechas por código (no se recalcula en cada cambio). */
    private boolean cambiandoFechas;
    /** Hasta terminar initialize() los filtros cambian sin generar el reporte. */
    private boolean iniciado;

    @FXML
    private void initialize() {
        construirSelectorTipo();
        configurarFechas();
        configurarProfesionales();
        configurarBuscadorPaciente();
        construirTablaCitas();
        construirTablaHistorial();
        construirTablaIngresos();
        quitarPaciente();
        iniciado = true;
        mostrarTipo();
    }

    // ===== Filtros =====

    private void construirSelectorTipo() {
        ToggleGroup grupo = new ToggleGroup();
        for (Tipo opcion : Tipo.values()) {
            ToggleButton boton = new ToggleButton(opcion.titulo);
            boton.setToggleGroup(grupo);
            boton.setSelected(opcion == tipo);
            boton.setOnAction(e -> {
                boton.setSelected(true);
                if (tipo != opcion) {
                    tipo = opcion;
                    mostrarTipo();
                }
            });
            selectorTipo.getChildren().add(boton);
        }
    }

    private void configurarFechas() {
        for (DatePicker campo : List.of(campoDesde, campoHasta)) {
            campo.setConverter(new StringConverter<>() {
                @Override
                public String toString(LocalDate fecha) {
                    return fecha == null ? "" : Formatos.formatoFechaCampo().format(fecha);
                }

                @Override
                public LocalDate fromString(String texto) {
                    try {
                        return texto == null || texto.isBlank() ? null
                                : LocalDate.parse(texto.strip(), Formatos.formatoFechaCampo());
                    } catch (DateTimeParseException e) {
                        return campo.getValue();
                    }
                }
            });
            campo.valueProperty().addListener((obs, anterior, nueva) -> {
                if (!cambiandoFechas) {
                    marcarAtajo();
                    actualizar();
                }
            });
        }
        for (Atajo atajo : Atajo.values()) {
            ToggleButton chip = new ToggleButton(atajo.titulo);
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(grupoAtajos);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                aplicarAtajo(atajo);
            });
            botonesAtajo.put(atajo, chip);
            atajos.getChildren().add(chip);
        }
        aplicarAtajo(Atajo.MES);
    }

    private void aplicarAtajo(Atajo atajo) {
        LocalDate hoy = LocalDate.now();
        cambiandoFechas = true;
        switch (atajo) {
            case HOY -> {
                campoDesde.setValue(hoy);
                campoHasta.setValue(hoy);
            }
            case SEMANA -> {
                campoDesde.setValue(hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                campoHasta.setValue(hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            }
            case MES -> {
                campoDesde.setValue(hoy.withDayOfMonth(1));
                campoHasta.setValue(hoy.with(TemporalAdjusters.lastDayOfMonth()));
            }
        }
        cambiandoFechas = false;
        botonesAtajo.get(atajo).setSelected(true);
        actualizar();
    }

    /** Si las fechas escritas coinciden con un atajo, se marca; si no, ninguno. */
    private void marcarAtajo() {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        Atajo coincide = null;
        if (hoy.equals(desde) && hoy.equals(hasta)) {
            coincide = Atajo.HOY;
        } else if (hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).equals(desde)
                && hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).equals(hasta)) {
            coincide = Atajo.SEMANA;
        } else if (hoy.withDayOfMonth(1).equals(desde) && hoy.with(TemporalAdjusters.lastDayOfMonth()).equals(hasta)) {
            coincide = Atajo.MES;
        }
        if (coincide == null) {
            if (grupoAtajos.getSelectedToggle() != null) {
                grupoAtajos.getSelectedToggle().setSelected(false);
            }
        } else {
            botonesAtajo.get(coincide).setSelected(true);
        }
    }

    private void configurarProfesionales() {
        List<Profesional> opciones = new ArrayList<>();
        opciones.add(TODOS);
        try {
            opciones.addAll(Servicios.profesionales().listarTodos());
        } catch (RuntimeException e) {
            avisoFiltros.mostrar(Aviso.Tipo.ERROR, "No se pudo cargar los profesionales: " + e.getMessage());
        }
        campoProfesional.getItems().setAll(opciones);
        campoProfesional.setConverter(new StringConverter<>() {
            @Override
            public String toString(Profesional p) {
                return p == null || p == TODOS ? "Todos los profesionales" : p.getNombreCompleto();
            }

            @Override
            public Profesional fromString(String texto) {
                return null;
            }
        });
        campoProfesional.setValue(TODOS);
        campoProfesional.valueProperty().addListener((obs, anterior, nuevo) -> actualizar());
    }

    private void configurarBuscadorPaciente() {
        sugerencias.getStyleClass().add("sugerencias");
        esperaBusqueda.setOnFinished(e -> mostrarSugerencias());
        campoPaciente.textProperty().addListener((obs, anterior, nuevo) -> esperaBusqueda.playFromStart());
        campoPaciente.setOnAction(e -> {
            esperaBusqueda.stop();
            mostrarSugerencias();
            e.consume();
        });
    }

    private void mostrarSugerencias() {
        String texto = campoPaciente.getText();
        if (texto == null || texto.isBlank()) {
            sugerencias.hide();
            return;
        }
        List<Paciente> encontrados;
        try {
            encontrados = Servicios.pacientes().buscar(texto, FiltroPaciente.TODOS, 0).stream().limit(8).toList();
        } catch (RuntimeException e) {
            avisoFiltros.mostrar(Aviso.Tipo.ERROR, "No se pudo buscar pacientes: " + e.getMessage());
            return;
        }
        sugerencias.getItems().clear();
        if (encontrados.isEmpty()) {
            Label nada = new Label("No se encontraron pacientes");
            nada.getStyleClass().add("texto-tenue");
            CustomMenuItem item = new CustomMenuItem(nada, false);
            item.setDisable(true);
            sugerencias.getItems().add(item);
        }
        for (Paciente encontrado : encontrados) {
            Label nombre = new Label(encontrado.getNombreCompleto());
            nombre.getStyleClass().add("celda-titulo");
            Label dni = new Label("DNI " + encontrado.getDni());
            dni.getStyleClass().add("celda-sub");
            VBox fila = new VBox(nombre, dni);
            fila.setPrefWidth(buscadorPaciente.getWidth() - 24);
            CustomMenuItem item = new CustomMenuItem(fila, true);
            item.setOnAction(e -> elegirPaciente(encontrado));
            sugerencias.getItems().add(item);
        }
        if (!sugerencias.isShowing()) {
            sugerencias.show(buscadorPaciente, Side.BOTTOM, 0, 4);
        }
    }

    private void elegirPaciente(Paciente elegido) {
        paciente = elegido;
        sugerencias.hide();
        nombrePaciente.setText(elegido.getNombreCompleto() + " · DNI " + elegido.getDni());
        alternar(buscadorPaciente, false);
        alternar(pacienteElegido, true);
        actualizar();
    }

    @FXML
    private void quitarPaciente() {
        paciente = null;
        campoPaciente.clear();
        sugerencias.hide();
        alternar(pacienteElegido, false);
        alternar(buscadorPaciente, true);
        if (tipo == Tipo.HISTORIAL) {
            actualizar();
        }
    }

    /** Cada reporte muestra solo sus filtros, su tabla y (en Ingresos) las barras. */
    private void mostrarTipo() {
        alternar(filtroFechas, tipo != Tipo.HISTORIAL);
        alternar(cfProfesional, tipo == Tipo.CITAS);
        alternar(cfPaciente, tipo == Tipo.HISTORIAL);
        alternar(tarjetaBarras, tipo == Tipo.INGRESOS);
        subtitulo.setText(switch (tipo) {
            case CITAS -> "Citas del periodo, con su asistencia.";
            case HISTORIAL -> "Tratamientos de un paciente, con lo pagado y el saldo.";
            case INGRESOS -> "Lo cobrado en el periodo, por método de pago y por día.";
        });
        contenedorTabla.getChildren().setAll(switch (tipo) {
            case CITAS -> tablaCitas;
            case HISTORIAL -> tablaHistorial;
            case INGRESOS -> tablaIngresos;
        });
        actualizar();
    }

    // ===== Exportar =====

    /** El reporte que está en pantalla, con los mismos filtros, en PDF. */
    @FXML
    private void exportarPdf() {
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        Profesional elegido = campoProfesional.getValue() == TODOS ? null : campoProfesional.getValue();
        Paciente delHistorial = paciente;
        String nombre = switch (tipo) {
            case CITAS -> Servicios.pdf().nombreArchivo("Reporte de citas", null);
            case HISTORIAL -> Servicios.pdf().nombreArchivo("Historial", delHistorial.getDni());
            case INGRESOS -> Servicios.pdf().nombreArchivo("Reporte de ingresos", null);
        };
        Tipo exportado = tipo;
        ExportarPdf.exportar(botonPdf, nombre, salida -> {
            switch (exportado) {
                case CITAS -> Servicios.pdf().reporteCitas(desde, hasta, elegido, salida);
                case HISTORIAL -> Servicios.pdf().reporteHistorial(delHistorial.getIdPaciente(), salida);
                case INGRESOS -> Servicios.pdf().reporteIngresos(desde, hasta, salida);
            }
        }, mensaje -> avisoFiltros.mostrar(Aviso.Tipo.ERROR, mensaje));
    }

    // ===== Reportes =====

    private void actualizar() {
        if (cambiandoFechas || !iniciado) {
            return;
        }
        avisoFiltros.ocultar();
        cfHasta.limpiarError();
        // Sin paciente elegido no hay historial que exportar
        botonPdf.setDisable(tipo == Tipo.HISTORIAL && paciente == null);
        try {
            switch (tipo) {
                case CITAS -> mostrarCitas();
                case HISTORIAL -> mostrarHistorial();
                case INGRESOS -> mostrarIngresos();
            }
        } catch (ReglaNegocioException e) {
            kpis.getChildren().clear();
            vaciarTablas();
            botonPdf.setDisable(true);
            if (e.getCampo().filter(c -> c.equals("hasta") || c.equals("desde")).isPresent()) {
                cfHasta.mostrarError(e.getMessage());
            } else {
                avisoFiltros.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            }
        } catch (RuntimeException e) {
            kpis.getChildren().clear();
            vaciarTablas();
            avisoFiltros.mostrar(Aviso.Tipo.ERROR, "No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private void mostrarCitas() {
        Profesional elegido = campoProfesional.getValue();
        Integer idProfesional = elegido == null || elegido == TODOS ? null : elegido.getIdProfesional();
        ReporteCitas reporte = servicio.citas(campoDesde.getValue(), campoHasta.getValue(), idProfesional);
        filasCitas.setAll(reporte.citas());
        kpis.getChildren().setAll(
                new Kpi("Citas", "agenda").mostrar(String.valueOf(reporte.total()),
                        reporte.porAtender() + " pendientes o confirmadas"),
                new Kpi("Atendidas", "check").mostrar(String.valueOf(reporte.atendidas()),
                        "de " + reporte.total() + " en el periodo"),
                new Kpi("Canceladas", "cerrar").mostrar(String.valueOf(reporte.canceladas()),
                        "Liberaron su horario"),
                new Kpi("Asistencia", "pacientes").mostrar(
                        reporte.asistencia() == null ? "—" : reporte.asistencia() + "%",
                        "Atendidas entre las citas que ya debieron ocurrir"));
        tituloTabla.setText("Citas del " + rangoTexto());
        pie.setText(reporte.total() + (reporte.total() == 1 ? " cita" : " citas"));
        tablaCitas.setPlaceholder(new EstadoVacio("Sin citas en este periodo", "Prueba con otras fechas o con todos los profesionales."));
    }

    private void mostrarHistorial() {
        if (paciente == null) {
            kpis.getChildren().clear();
            filasHistorial.clear();
            tituloTabla.setText("Historial de tratamientos");
            pie.setText("");
            tablaHistorial.setPlaceholder(new EstadoVacio("Elige un paciente", "Búscalo por nombre o DNI en el filtro de arriba."));
            return;
        }
        ReporteHistorial reporte = servicio.historialPaciente(paciente.getIdPaciente());
        filasHistorial.setAll(reporte.tratamientos());
        long enCurso = reporte.tratamientos().stream()
                .filter(t -> t.getTratamiento().getEstado() == EstadoTratamiento.EN_CURSO).count();
        kpis.getChildren().setAll(
                new Kpi("Tratamientos", "tratamientos").mostrar(String.valueOf(reporte.tratamientos().size()),
                        enCurso + " en curso"),
                new Kpi("Costo total", "dinero").mostrar(Formatos.dinero(reporte.costo()), "Suma de los tratamientos"),
                new Kpi("Pagado", "pagos").mostrar(Formatos.dinero(reporte.pagado()), "Suma de sus pagos"),
                new Kpi("Saldo", "alerta").mostrar(Formatos.dinero(reporte.saldo()),
                        reporte.saldo() > 0 ? "Pendiente de cobro" : "Sin deuda"));
        tituloTabla.setText("Tratamientos de " + paciente.getNombreCompleto());
        pie.setText(reporte.tratamientos().size() + (reporte.tratamientos().size() == 1 ? " tratamiento" : " tratamientos"));
        tablaHistorial.setPlaceholder(new EstadoVacio("Sin tratamientos", "Este paciente aún no tiene tratamientos registrados."));
    }

    private void mostrarIngresos() {
        LocalDate desde = campoDesde.getValue();
        LocalDate hasta = campoHasta.getValue();
        ReporteIngresos reporte = servicio.ingresos(desde, hasta);
        List<FilaMetodo> filas = new ArrayList<>();
        for (MetodoPago metodo : MetodoPago.values()) {
            double total = reporte.porMetodo().getOrDefault(metodo, 0.0);
            filas.add(new FilaMetodo(metodo, reporte.pagosPorMetodo().getOrDefault(metodo, 0), total,
                    reporte.total() == 0 ? 0 : total / reporte.total()));
        }
        filasIngresos.setAll(filas);
        kpis.getChildren().setAll(
                new Kpi("Total cobrado", "dinero").mostrar(Formatos.dinero(reporte.total()), "Del " + rangoTexto()),
                new Kpi("Pagos", "pagos").mostrar(String.valueOf(reporte.pagos()), "Registrados en el periodo"),
                new Kpi("Promedio por pago", "reportes").mostrar(
                        reporte.pagos() == 0 ? "—" : Formatos.dinero(reporte.total() / reporte.pagos()),
                        "Total cobrado entre la cantidad de pagos"));
        tituloTabla.setText("Ingresos por método de pago");
        pie.setText(reporte.pagos() + (reporte.pagos() == 1 ? " pago · " : " pagos · ") + Formatos.dinero(reporte.total()));
        mostrarBarras(reporte, desde, hasta);
    }

    /** Barras por día si el periodo tiene hasta 31 días; si no, por mes. */
    private void mostrarBarras(ReporteIngresos reporte, LocalDate desde, LocalDate hasta) {
        boolean porDia = ChronoUnit.DAYS.between(desde, hasta) < DIAS_BARRAS_DIARIAS;
        Map<LocalDate, Double> grupos = new LinkedHashMap<>();
        reporte.porDia().forEach((dia, monto) ->
                grupos.merge(porDia ? dia : dia.withDayOfMonth(1), monto, Double::sum));
        List<String> etiquetas = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        int resaltada = -1;
        LocalDate hoy = LocalDate.now();
        for (Map.Entry<LocalDate, Double> grupo : grupos.entrySet()) {
            LocalDate fecha = grupo.getKey();
            if (porDia ? fecha.equals(hoy) : fecha.equals(hoy.withDayOfMonth(1))) {
                resaltada = etiquetas.size();
            }
            // Con muchas barras no entran las etiquetas ni las cifras: el rango va en el título
            boolean denso = grupos.size() > 12;
            etiquetas.add(denso ? "" : porDia ? String.valueOf(fecha.getDayOfMonth()) : Formatos.mes(fecha).substring(0, 3));
            valores.add(grupo.getValue());
            textos.add(denso ? "" : Formatos.miles(grupo.getValue()));
        }
        String titulo = porDia ? "Ingresos por día" : "Ingresos por mes";
        tituloBarras.setText(grupos.size() > 12
                ? titulo + " (" + Formatos.diaMes(desde) + " – " + Formatos.diaMes(hasta) + ")" : titulo);
        grafico.mostrar(etiquetas, valores, textos, resaltada);
    }

    private String rangoTexto() {
        return Formatos.fechaCorta(campoDesde.getValue()) + " al " + Formatos.fechaCorta(campoHasta.getValue());
    }

    private void vaciarTablas() {
        filasCitas.clear();
        filasHistorial.clear();
        filasIngresos.clear();
        pie.setText("");
    }

    // ===== Tablas =====

    private void construirTablaCitas() {
        configurarTabla(tablaCitas, filasCitas);
        tablaCitas.getColumns().setAll(List.of(
                columnaTexto("FECHA", 100, c -> Formatos.fechaCorta(c.getFecha()), null),
                columnaTexto("HORA", 100, c -> Formatos.rangoHoras(c.getHoraInicio(), c.getHoraFin()), null),
                columnaTexto("PACIENTE", 180, c -> c.getPaciente().getNombreCompleto(), "celda-titulo"),
                columnaTexto("PROFESIONAL", 160, c -> c.getProfesional().getNombreCompleto(), null),
                columnaTexto("MOTIVO", 160, c -> c.getMotivo() == null ? "—" : c.getMotivo(), null),
                columnaGrafico("ESTADO", 110, c -> new Pill(c.getEstado()))));
    }

    private void construirTablaHistorial() {
        configurarTabla(tablaHistorial, filasHistorial);
        tablaHistorial.getColumns().setAll(List.of(
                columnaTexto("FECHA", 100, t -> Formatos.fechaCorta(t.getTratamiento().getFecha()), null),
                columnaTexto("TRATAMIENTO", 200, t -> t.getTratamiento().getDescripcion(), "celda-titulo"),
                columnaTexto("PIEZAS", 90, t -> piezas(t.getTratamiento().getPiezasDentales()), null),
                columnaTexto("COSTO", 100, t -> Formatos.dinero(t.getTratamiento().getCosto()), "celda-monto"),
                columnaTexto("PAGADO", 100, t -> Formatos.dinero(t.getPagado()), "celda-monto"),
                columnaTexto("SALDO", 100, t -> Formatos.dinero(t.getSaldo()), "celda-monto"),
                columnaGrafico("ESTADO", 110, t -> new Pill(t.getTratamiento().getEstado()))));
    }

    private void construirTablaIngresos() {
        configurarTabla(tablaIngresos, filasIngresos);
        tablaIngresos.getColumns().setAll(List.of(
                columnaGrafico("MÉTODO", 160, f -> new Pill(f.metodo())),
                columnaTexto("PAGOS", 80, f -> String.valueOf(f.pagos()), null),
                columnaTexto("TOTAL", 120, f -> Formatos.dinero(f.total()), "celda-monto"),
                columnaTexto("% DEL TOTAL", 110, f -> Math.round(f.fraccion() * 100) + "%", null)));
        tablaIngresos.setPlaceholder(new EstadoVacio("Sin pagos en este periodo", "Prueba con otras fechas."));
    }

    private static <S> void configurarTabla(TableView<S> tabla, ObservableList<S> filas) {
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(FILAS_MINIMAS, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
    }

    private static <S> TableColumn<S, S> columna(String titulo, double ancho) {
        TableColumn<S, S> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static <S> TableColumn<S, S> columnaTexto(String titulo, double ancho, Function<S, String> texto,
                                                      String claseCss) {
        TableColumn<S, S> columna = columna(titulo, ancho);
        columna.setCellFactory(c -> {
            TableCell<S, S> celda = new TableCell<>() {
                @Override
                protected void updateItem(S fila, boolean vacio) {
                    super.updateItem(fila, vacio);
                    setText(vacio || fila == null ? null : texto.apply(fila));
                }
            };
            if (claseCss != null) {
                celda.getStyleClass().add(claseCss);
            }
            return celda;
        });
        if ("celda-monto".equals(claseCss)) {
            columna.getStyleClass().add("columna-derecha");
        }
        return columna;
    }

    private static <S> TableColumn<S, S> columnaGrafico(String titulo, double ancho, Function<S, Node> grafico) {
        TableColumn<S, S> columna = columna(titulo, ancho);
        columna.setMinWidth(ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(S fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setText(null);
                setGraphic(vacio || fila == null ? null : grafico.apply(fila));
            }
        });
        return columna;
    }

    private static String piezas(List<Integer> piezas) {
        return piezas == null || piezas.isEmpty() ? "—"
                : String.join(", ", piezas.stream().map(String::valueOf).toList());
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
