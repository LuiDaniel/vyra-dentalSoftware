package com.vyra.dental.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.ReporteService;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.TratamientoResumen;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Formatos;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Reportes (docs/08-guia-frontend.md §7.9, CU-09). Solo administrador. Tres tipos: Citas,
 * Historial de paciente e Ingresos; el resultado se vuelve a generar al cambiar un filtro.
 */
public class ReportesController {

    private static final double ALTO_FILA = 52;
    private static final double ALTO_ENCABEZADO = 44;

    private enum Tipo { CITAS, HISTORIAL, INGRESOS }

    @FXML private HBox tipos;
    @FXML private Button botonExportar;
    @FXML private VBox filtros;
    @FXML private Label mensaje;
    @FXML private VBox resultado;

    private final ReporteService servicio = Servicios.reportes();
    private final ToggleGroup grupoTipo = new ToggleGroup();
    private final DatePicker campoDesde = new DatePicker();
    private final DatePicker campoHasta = new DatePicker();
    private final ComboBox<Profesional> campoProfesional = new ComboBox<>();
    private final TextField campoPaciente = new TextField();
    private final ListView<Paciente> listaPacientes = new ListView<>();
    private Paciente paciente;
    private boolean ajustando;

    @FXML
    private void initialize() {
        for (Tipo tipo : Tipo.values()) {
            ToggleButton boton = new ToggleButton(switch (tipo) {
                case CITAS -> "Citas";
                case HISTORIAL -> "Historial de paciente";
                case INGRESOS -> "Ingresos";
            });
            boton.setUserData(tipo);
            boton.setToggleGroup(grupoTipo);
            tipos.getChildren().add(boton);
        }
        grupoTipo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else {
                construirFiltros();
                generar();
            }
        });
        LocalDate hoy = LocalDate.now();
        campoDesde.setValue(hoy.withDayOfMonth(1));
        campoHasta.setValue(hoy);
        campoDesde.valueProperty().addListener((obs, antes, ahora) -> generarSiNoAjusta());
        campoHasta.valueProperty().addListener((obs, antes, ahora) -> generarSiNoAjusta());

        campoProfesional.setMaxWidth(Double.MAX_VALUE);
        campoProfesional.setCellFactory(l -> new CeldaProfesional());
        campoProfesional.setButtonCell(new CeldaProfesional());
        Profesional todos = new Profesional();
        todos.setNombres("Todos los profesionales");
        todos.setApellidos("");
        try {
            campoProfesional.getItems().add(todos);
            campoProfesional.getItems().addAll(Servicios.profesionales().listarTodos());
        } catch (RuntimeException e) {
            // Sin lista de profesionales el reporte sigue funcionando para "todos"
        }
        campoProfesional.getSelectionModel().selectFirst();
        campoProfesional.valueProperty().addListener((obs, antes, ahora) -> generarSiNoAjusta());

        campoPaciente.setPromptText("Buscar paciente por nombre o DNI");
        campoPaciente.textProperty().addListener((obs, antes, ahora) -> buscarPacientes());
        listaPacientes.setPrefHeight(150);
        listaPacientes.setMinHeight(150);
        listaPacientes.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null : p.getNombreCompleto() + " · DNI " + p.getDni());
            }
        });
        listaPacientes.getSelectionModel().selectedItemProperty().addListener((obs, antes, p) -> {
            if (p != null) {
                paciente = p;
                generar();
            }
        });

        grupoTipo.selectToggle(grupoTipo.getToggles().get(0));
    }

    private Tipo tipo() {
        return (Tipo) grupoTipo.getSelectedToggle().getUserData();
    }

    // ===== Filtros =====

    private void construirFiltros() {
        filtros.getChildren().clear();
        if (tipo() == Tipo.HISTORIAL) {
            Label etiqueta = new Label("Paciente");
            etiqueta.getStyleClass().add("campo-etiqueta");
            buscarPacientes();
            filtros.getChildren().addAll(etiqueta, campoPaciente, listaPacientes);
            return;
        }
        HBox fechas = new HBox(12, grupo("Desde", campoDesde), grupo("Hasta", campoHasta));
        fechas.setAlignment(Pos.BOTTOM_LEFT);
        HBox atajos = new HBox(6, atajo("Hoy", 0), atajo("Esta semana", 1), atajo("Este mes", 2));
        atajos.getStyleClass().add("filtros");
        fechas.getChildren().add(atajos);
        if (tipo() == Tipo.CITAS) {
            fechas.getChildren().add(grupo("Profesional", campoProfesional));
        }
        filtros.getChildren().add(fechas);
    }

    private static VBox grupo(String etiqueta, Node control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("campo-etiqueta");
        return new VBox(4, l, control);
    }

    private Button atajo(String texto, int clase) {
        Button boton = new Button(texto);
        boton.getStyleClass().addAll("btn-secundario", "btn-chico");
        boton.setOnAction(e -> {
            LocalDate hoy = LocalDate.now();
            ajustando = true;
            campoDesde.setValue(switch (clase) {
                case 0 -> hoy;
                case 1 -> hoy.with(DayOfWeek.MONDAY);
                default -> hoy.withDayOfMonth(1);
            });
            campoHasta.setValue(hoy);
            ajustando = false;
            generar();
        });
        return boton;
    }

    private void buscarPacientes() {
        try {
            listaPacientes.getItems().setAll(Servicios.pacientes().buscar(campoPaciente.getText(),
                    FiltroPaciente.TODOS, 0));
        } catch (RuntimeException e) {
            listaPacientes.getItems().clear();
        }
    }

    private void generarSiNoAjusta() {
        if (!ajustando && grupoTipo.getSelectedToggle() != null) {
            generar();
        }
    }

    // ===== Resultado =====

    private void generar() {
        mensaje.setText("");
        resultado.getChildren().clear();
        try {
            switch (tipo()) {
                case CITAS -> reporteCitas();
                case HISTORIAL -> reporteHistorial();
                case INGRESOS -> reporteIngresos();
            }
        } catch (ReglaNegocioException e) {
            mensaje.setText(e.getMessage());
        } catch (RuntimeException e) {
            mensaje.setText("No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private void reporteCitas() {
        Profesional elegido = campoProfesional.getValue();
        Integer id = elegido == null || elegido.getIdProfesional() == 0 ? null : elegido.getIdProfesional();
        List<Cita> citas = servicio.citas(campoDesde.getValue(), campoHasta.getValue(), id);
        ReporteService.ResumenCitas r = servicio.resumir(citas);
        resultado.getChildren().add(fila(
                kpi("Total de citas", String.valueOf(r.total()), r.pendientes() + " por atender"),
                kpi("Atendidas", String.valueOf(r.atendidas()), "citas completadas"),
                kpi("Canceladas", String.valueOf(r.canceladas()), "no cuentan para la asistencia"),
                kpi("Asistencia", r.asistencia() + "%", "atendidas sobre las no canceladas")));

        TableColumn<Cita, Cita> paciente = columnaNodo("PACIENTE", 190, c -> avatarYNombre(
                c.getPaciente().getIniciales(), c.getPaciente().getIdPaciente(), c.getPaciente().getNombreCompleto()));
        resultado.getChildren().add(tarjetaTabla(citas, List.of(
                columna("FECHA", 90, (Cita c) -> Formatos.fechaCorta(c.getFecha())),
                columna("HORA", 100, c -> Formatos.rangoHoras(c.getHoraInicio(), c.getHoraFin())),
                paciente,
                columna("PROFESIONAL", 150, c -> c.getProfesional().getNombreCompleto()),
                columna("MOTIVO", 150, c -> c.getMotivo() == null ? "—" : c.getMotivo()),
                columnaNodo("ESTADO", 110, c -> new Pill(c.getEstado()))),
                "Sin citas en el periodo", "Prueba con otro rango de fechas o con otro profesional."));
    }

    private void reporteHistorial() {
        if (paciente == null) {
            resultado.getChildren().add(new EstadoVacio("Elige un paciente",
                    "Verás sus tratamientos con el costo, lo pagado y el saldo."));
            return;
        }
        List<TratamientoResumen> historial = servicio.historialPaciente(paciente.getIdPaciente());
        double costo = historial.stream().mapToDouble(h -> h.tratamiento().getCosto()).sum();
        double pagado = historial.stream().mapToDouble(TratamientoResumen::pagado).sum();
        double saldo = historial.stream().mapToDouble(TratamientoResumen::saldo).sum();
        resultado.getChildren().add(fila(
                kpi("Paciente", paciente.getNombreCompleto(), "DNI " + paciente.getDni()),
                kpi("Costo total", Formatos.dinero(costo), historial.size() + " tratamientos"),
                kpi("Pagado", Formatos.dinero(pagado), "suma de sus pagos"),
                kpi("Saldo pendiente", Formatos.dinero(saldo), saldo > 0 ? "por cobrar" : "al día")));
        resultado.getChildren().add(tarjetaTabla(historial, List.of(
                columna("FECHA", 90, (TratamientoResumen t) -> Formatos.fechaCorta(t.tratamiento().getFecha())),
                columna("TRATAMIENTO", 200, t -> t.tratamiento().getDescripcion()),
                columnaNodo("ESTADO", 110, t -> new Pill(t.tratamiento().getEstado())),
                columna("COSTO", 100, t -> Formatos.dinero(t.tratamiento().getCosto())),
                columna("PAGADO", 100, t -> Formatos.dinero(t.pagado())),
                columna("SALDO", 100, t -> Formatos.dinero(t.saldo()))),
                "Sin tratamientos", "Este paciente aún no tiene tratamientos registrados."));
    }

    private void reporteIngresos() {
        ReporteService.Ingresos ingresos = servicio.ingresos(campoDesde.getValue(), campoHasta.getValue());
        int cantidad = ingresos.pagos().size();
        resultado.getChildren().add(fila(
                kpi("Total cobrado", Formatos.dinero(ingresos.total()), cantidad + (cantidad == 1 ? " pago" : " pagos")),
                kpi("Promedio por pago", Formatos.dinero(cantidad == 0 ? 0 : ingresos.total() / cantidad),
                        "en el periodo"),
                kpi("Días con cobros", String.valueOf(ingresos.porDia().size()), "de " + dias() + " del periodo")));

        List<Map.Entry<MetodoPago, Double>> metodos = List.copyOf(ingresos.porMetodo().entrySet());
        Node tablaMetodos = tarjetaTabla(metodos, List.of(
                columnaNodo("MÉTODO", 150, (Map.Entry<MetodoPago, Double> e) -> new Pill(e.getKey())),
                columna("MONTO", 110, e -> Formatos.dinero(e.getValue())),
                columna("%", 70, e -> ingresos.total() <= 0 ? "0%"
                        : Math.round(e.getValue() / ingresos.total() * 100) + "%")),
                "Sin cobros", "No hay pagos en el periodo.");
        HBox.setHgrow(tablaMetodos, Priority.ALWAYS);
        Node barras = barrasPorDia(ingresos.porDia());
        HBox.setHgrow(barras, Priority.ALWAYS);
        resultado.getChildren().add(new HBox(16, tablaMetodos, barras));
    }

    private long dias() {
        return campoDesde.getValue().until(campoHasta.getValue(), java.time.temporal.ChronoUnit.DAYS) + 1;
    }

    private Node barrasPorDia(Map<LocalDate, Double> porDia) {
        Label titulo = new Label("Cobrado por día");
        titulo.getStyleClass().add("card-titulo");
        VBox tarjeta = new VBox(12, titulo);
        tarjeta.getStyleClass().addAll("card", "inicio-tarjeta");
        if (porDia.isEmpty()) {
            Label vacio = new Label("Sin cobros en el periodo.");
            vacio.getStyleClass().add("texto-tenue");
            tarjeta.getChildren().add(vacio);
            return tarjeta;
        }
        double maximo = porDia.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        HBox barras = new HBox(8);
        barras.setAlignment(Pos.BOTTOM_CENTER);
        // Si el periodo es largo se muestran los últimos 14 días con cobros
        List<Map.Entry<LocalDate, Double>> dias = porDia.entrySet().stream()
                .skip(Math.max(0, porDia.size() - 14)).toList();
        for (Map.Entry<LocalDate, Double> dia : dias) {
            Label cifra = new Label(String.format(Locale.US, "%,.0f", dia.getValue()));
            cifra.getStyleClass().add("celda-sub");
            Region barra = new Region();
            barra.getStyleClass().add("inicio-barra");
            double alto = Math.max(4, dia.getValue() / maximo * 120);
            barra.setPrefHeight(alto);
            barra.setMinHeight(alto);
            barra.setMaxHeight(alto);
            Label fecha = new Label(Formatos.fechaCorta(dia.getKey()).substring(0, 6));
            fecha.getStyleClass().add("texto-apoyo");
            VBox columna = new VBox(4, cifra, barra, fecha);
            columna.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(columna, Priority.ALWAYS);
            barras.getChildren().add(columna);
        }
        tarjeta.getChildren().add(barras);
        return tarjeta;
    }

    // ===== Piezas de pantalla =====

    private static HBox fila(Node... tarjetas) {
        HBox fila = new HBox(16, tarjetas);
        for (Node t : tarjetas) {
            HBox.setHgrow(t, Priority.ALWAYS);
            ((Region) t).setMaxWidth(Double.MAX_VALUE);
        }
        return fila;
    }

    private static Node kpi(String titulo, String cifra, String detalle) {
        Label t = new Label(titulo);
        t.getStyleClass().add("texto-secundario");
        Label valor = new Label(cifra);
        valor.getStyleClass().add("kpi-valor");
        valor.setWrapText(true);
        Label d = new Label(detalle);
        d.getStyleClass().add("celda-sub");
        d.setWrapText(true);
        VBox tarjeta = new VBox(6, t, valor, d);
        tarjeta.getStyleClass().add("kpi");
        return tarjeta;
    }

    private static HBox avatarYNombre(String iniciales, int id, String nombre) {
        Label l = new Label(nombre);
        l.getStyleClass().add("celda-titulo");
        HBox caja = new HBox(new Avatar(iniciales, id), l);
        caja.getStyleClass().add("celda-paciente");
        return caja;
    }

    private static <T> Node tarjetaTabla(List<T> filas, List<TableColumn<T, T>> columnas, String vacioTitulo,
                                         String vacioDetalle) {
        TableView<T> tabla = new TableView<>(FXCollections.observableArrayList(filas));
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(3, Bindings.size(tabla.getItems())).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.setPlaceholder(new EstadoVacio(vacioTitulo, vacioDetalle));
        tabla.getColumns().setAll(columnas);
        VBox tarjeta = new VBox(tabla);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private static <T> TableColumn<T, T> base(String titulo, double ancho) {
        TableColumn<T, T> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static <T> TableColumn<T, T> columna(String titulo, double ancho, Function<T, String> texto) {
        TableColumn<T, T> columna = base(titulo, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setText(vacio || fila == null ? null : texto.apply(fila));
            }
        });
        return columna;
    }

    private static <T> TableColumn<T, T> columnaNodo(String titulo, double ancho, Function<T, Node> nodo) {
        TableColumn<T, T> columna = base(titulo, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setText(null);
                setGraphic(vacio || fila == null ? null : nodo.apply(fila));
            }
        });
        return columna;
    }

    private static class CeldaProfesional extends ListCell<Profesional> {
        @Override
        protected void updateItem(Profesional p, boolean vacio) {
            super.updateItem(p, vacio);
            setText(vacio || p == null ? null : p.getNombreCompleto().strip());
        }
    }
}
