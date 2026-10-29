package com.vyra.dental.controller;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.service.CitaService;
import com.vyra.dental.service.PacienteService;
import com.vyra.dental.service.ProfesionalService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.Dialogos;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Agenda de citas: vistas Día, Semana y Lista, panel "Nueva cita" con
 * validación de cruce en vivo y detalle de cada cita (docs/08-guia-frontend.md §7.3,
 * CU-02, CU-03). El odontólogo ve solo sus citas y puede marcarlas como atendidas.
 */
public class AgendaController implements RecibeParametro {

    private enum Vista { DIA, SEMANA, LISTA }

    private static final double ALTO_FILA = 56;
    private static final double ANCHO_HORAS = 64;
    private static final int FILAS = (int) Duration.between(CitaService.APERTURA, CitaService.CIERRE).toMinutes()
            / CitaService.MINUTOS_BLOQUE;
    private static final int MAX_HORARIOS_SUGERIDOS = 6;

    @FXML private Label tituloFecha;
    @FXML private ComboBox<Profesional> profesionalSemana;
    @FXML private HBox selectorVista;
    @FXML private HBox tiraSemana;
    @FXML private StackPane areaVista;
    @FXML private HBox leyenda;

    @FXML private VBox panelFormulario;
    @FXML private Label tituloFormulario;
    @FXML private CampoFormulario cfPaciente;
    @FXML private HBox buscadorPaciente;
    @FXML private TextField campoPaciente;
    @FXML private HBox tarjetaPaciente;
    @FXML private Avatar avatarPaciente;
    @FXML private Label nombrePaciente;
    @FXML private Label dniPaciente;
    @FXML private CampoFormulario cfProfesional;
    @FXML private ComboBox<Profesional> campoProfesional;
    @FXML private CampoFormulario cfFecha;
    @FXML private DatePicker campoFecha;
    @FXML private CampoFormulario cfInicio;
    @FXML private ComboBox<LocalTime> campoInicio;
    @FXML private CampoFormulario cfFin;
    @FXML private ComboBox<LocalTime> campoFin;
    @FXML private Aviso avisoDisponibilidad;
    @FXML private FlowPane horariosLibres;
    @FXML private TextArea campoMotivo;
    @FXML private VBox bloqueEstado;
    @FXML private ToggleButton estadoPendiente;
    @FXML private ToggleButton estadoConfirmada;
    @FXML private Aviso avisoFormulario;
    @FXML private Button botonAgendar;

    @FXML private VBox panelDetalle;
    @FXML private Label detalleFechaHora;
    @FXML private StackPane detalleEstado;
    @FXML private Avatar detalleAvatar;
    @FXML private Label detallePaciente;
    @FXML private Label detalleDni;
    @FXML private Label detalleProfesional;
    @FXML private Label detalleMotivo;
    @FXML private Aviso avisoDetalle;
    @FXML private VBox accionesDetalle;
    @FXML private HBox pieDetalle;
    @FXML private VBox panelVacio;

    private final CitaService citas = Servicios.citas();
    private final PacienteService pacientes = Servicios.pacientes();
    private final ProfesionalService profesionalesServicio = Servicios.profesionales();
    private final ContextMenu sugerencias = new ContextMenu();
    private final PauseTransition esperaBusqueda = new PauseTransition(javafx.util.Duration.millis(250));

    private boolean gestiona;
    private List<Profesional> profesionales = List.of();
    private Vista vista = Vista.DIA;
    private LocalDate fecha = LocalDate.now();
    private List<Cita> citasVisibles = List.of();
    private Cita citaSeleccionada;
    /** Cita que se está reprogramando; null si el formulario es de una cita nueva. */
    private Cita citaEnEdicion;
    private Paciente pacienteElegido;
    private boolean cargandoFormulario;

    @FXML
    private void initialize() {
        gestiona = citas.puedeGestionar();
        profesionales = profesionalesVisibles();

        construirSelectorVista();
        construirLeyenda();
        configurarProfesionalSemana();
        if (gestiona) {
            configurarFormulario();
            limpiarFormulario();
            mostrarFormulario();
        } else {
            mostrarPanel(panelVacio);
        }
        refrescar();
    }

    /** Desde Pacientes: "Agendar cita para este paciente". */
    @Override
    public void recibir(Object parametro) {
        if (gestiona && parametro instanceof Paciente paciente) {
            limpiarFormulario();
            mostrarFormulario();
            elegirPaciente(paciente);
            campoProfesional.requestFocus();
        }
    }

    // ===== Barra y navegación =====

    @FXML
    private void anterior() {
        fecha = vista == Vista.DIA ? diaHabil(fecha, -1) : fecha.minusWeeks(1);
        refrescar();
    }

    @FXML
    private void siguiente() {
        fecha = vista == Vista.DIA ? diaHabil(fecha, 1) : fecha.plusWeeks(1);
        refrescar();
    }

    @FXML
    private void hoy() {
        fecha = LocalDate.now();
        refrescar();
    }

    /** Un día antes o después, saltando el domingo (la tira muestra de lunes a sábado). */
    private static LocalDate diaHabil(LocalDate desde, int paso) {
        LocalDate siguiente = desde.plusDays(paso);
        return siguiente.getDayOfWeek() == DayOfWeek.SUNDAY ? siguiente.plusDays(paso) : siguiente;
    }

    private void construirSelectorVista() {
        ToggleGroup grupo = new ToggleGroup();
        for (Vista opcion : Vista.values()) {
            ToggleButton boton = new ToggleButton(switch (opcion) {
                case DIA -> "Día";
                case SEMANA -> "Semana";
                case LISTA -> "Lista";
            });
            boton.setToggleGroup(grupo);
            boton.setSelected(opcion == vista);
            boton.setOnAction(e -> {
                boton.setSelected(true);
                vista = opcion;
                refrescar();
            });
            selectorVista.getChildren().add(boton);
        }
    }

    private void configurarProfesionalSemana() {
        profesionalSemana.getItems().setAll(profesionales);
        profesionalSemana.setConverter(convertidorProfesional());
        if (!profesionales.isEmpty()) {
            profesionalSemana.setValue(profesionales.get(0));
        }
        profesionalSemana.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (vista == Vista.SEMANA) {
                refrescar();
            }
        });
    }

    private void construirLeyenda() {
        for (EstadoCita estado : List.of(EstadoCita.ATENDIDA, EstadoCita.CONFIRMADA, EstadoCita.PENDIENTE,
                EstadoCita.CANCELADA)) {
            Region color = new Region();
            color.getStyleClass().addAll("leyenda-color", claseBloque(estado));
            Label texto = new Label(estado.getEtiqueta(), color);
            texto.getStyleClass().add("leyenda-item");
            leyenda.getChildren().add(texto);
        }
    }

    /** Vuelve a leer las citas y dibuja la vista elegida. */
    private void refrescar() {
        LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sabado = lunes.plusDays(5);
        tituloFecha.setText(vista == Vista.DIA ? Formatos.diaYMes(fecha)
                : Formatos.fechaCorta(lunes) + " – " + Formatos.fechaCorta(sabado));
        profesionalSemana.setVisible(vista == Vista.SEMANA);
        profesionalSemana.setManaged(vista == Vista.SEMANA);

        List<Cita> semana;
        try {
            semana = citas.citasEntre(lunes, sabado.plusDays(1));
        } catch (RuntimeException e) {
            areaVista.getChildren().setAll(new EstadoVacio("No se pudo cargar la agenda", e.getMessage()));
            return;
        }
        construirTira(lunes, semana);

        if (profesionales.isEmpty()) {
            areaVista.getChildren().setAll(sinProfesionales());
            return;
        }
        switch (vista) {
            case DIA -> {
                citasVisibles = semana.stream().filter(c -> c.getFecha().equals(fecha)).toList();
                areaVista.getChildren().setAll(vistaDia());
            }
            case SEMANA -> {
                Profesional elegido = profesionalSemana.getValue();
                citasVisibles = semana.stream()
                        .filter(c -> elegido != null
                                && c.getProfesional().getIdProfesional() == elegido.getIdProfesional())
                        .toList();
                areaVista.getChildren().setAll(vistaSemana(lunes));
            }
            case LISTA -> {
                citasVisibles = semana;
                areaVista.getChildren().setAll(vistaLista());
            }
        }
        if (MainController.actual() != null) {
            MainController.actual().actualizarContadorAgenda();
        }
    }

    private Node sinProfesionales() {
        if (Sesion.get().getUsuario() != null && !gestiona) {
            return new EstadoVacio("Tu usuario no tiene un profesional asociado",
                    "Pide al administrador que lo asocie en la pantalla Profesionales para ver tus citas.");
        }
        Button ir = null;
        if (profesionalesServicio.puedeEditar()) {
            ir = new Button("Ir a Profesionales");
            ir.getStyleClass().add("btn-primario");
            ir.setOnAction(e -> MainController.actual().navegar(Pantalla.PROFESIONALES));
        }
        return new EstadoVacio("Aún no hay profesionales registrados",
                "La agenda tiene una columna por profesional. Registra al menos uno para agendar citas.", ir);
    }

    // ===== Tira de la semana =====

    private void construirTira(LocalDate lunes, List<Cita> semana) {
        Map<LocalDate, Long> porDia = semana.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .collect(Collectors.groupingBy(Cita::getFecha, Collectors.counting()));
        tiraSemana.getChildren().clear();
        ToggleGroup grupo = new ToggleGroup();
        for (int i = 0; i < 6; i++) {
            LocalDate dia = lunes.plusDays(i);
            long cantidad = porDia.getOrDefault(dia, 0L);
            Label nombre = new Label(Formatos.diaCorto(dia));
            nombre.getStyleClass().add("dia-nombre");
            Label numero = new Label(String.valueOf(dia.getDayOfMonth()));
            numero.getStyleClass().add("dia-numero");
            Label total = new Label(cantidad == 1 ? "1 cita" : cantidad + " citas");
            total.getStyleClass().add("dia-citas");

            ToggleButton boton = new ToggleButton(null, new VBox(nombre, numero, total));
            boton.getStyleClass().add("dia-boton");
            if (dia.equals(LocalDate.now())) {
                boton.getStyleClass().add("dia-hoy");
            }
            boton.setToggleGroup(grupo);
            boton.setSelected(dia.equals(fecha));
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setAccessibleText(Formatos.diaYMes(dia) + ", " + total.getText());
            HBox.setHgrow(boton, Priority.ALWAYS);
            boton.setOnAction(e -> {
                fecha = dia;
                refrescar();
            });
            tiraSemana.getChildren().add(boton);
        }
    }

    // ===== Vistas Día y Semana: cuadrícula de 30 minutos =====

    /** Una columna por profesional. */
    private Node vistaDia() {
        List<Node> encabezados = profesionales.stream().map(this::encabezadoProfesional).toList();
        return cuadricula(encabezados, c -> indiceProfesional(c.getProfesional()),
                (columna, hora) -> prepararNuevaCita(profesionales.get(columna), fecha, hora),
                fecha.equals(LocalDate.now()));
    }

    /** Una columna por día (lunes a sábado), para el profesional elegido. */
    private Node vistaSemana(LocalDate lunes) {
        List<Node> encabezados = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate dia = lunes.plusDays(i);
            Label titulo = new Label(Formatos.diaCorto(dia) + " " + dia.getDayOfMonth());
            titulo.getStyleClass().add("celda-titulo");
            HBox caja = new HBox(titulo);
            caja.getStyleClass().add("agenda-col-encabezado");
            if (dia.equals(LocalDate.now())) {
                caja.getStyleClass().add("agenda-col-hoy");
            }
            encabezados.add(caja);
        }
        Profesional elegido = profesionalSemana.getValue();
        return cuadricula(encabezados, c -> (int) ChronoUnit.DAYS.between(lunes, c.getFecha()),
                (columna, hora) -> prepararNuevaCita(elegido, lunes.plusDays(columna), hora), false);
    }

    /**
     * Cuadrícula de 08:00 a 20:00 en filas de 30 minutos. Cada cita ocupa
     * tantas filas como dure (GridPane rowSpan). Un clic en un espacio vacío
     * abre "Nueva cita" con esa hora.
     */
    private Node cuadricula(List<Node> encabezados, ToIntFunction<Cita> columnaDe,
                            BiConsumer<Integer, LocalTime> alHacerClicVacio, boolean mostrarHoraActual) {
        int columnas = encabezados.size();

        GridPane cabecera = new GridPane();
        cabecera.getStyleClass().add("agenda-encabezado");
        GridPane cuerpo = new GridPane();
        cuerpo.getStyleClass().add("agenda-cuerpo");
        for (GridPane grid : List.of(cabecera, cuerpo)) {
            ColumnConstraints horas = new ColumnConstraints(ANCHO_HORAS);
            grid.getColumnConstraints().add(horas);
            for (int c = 0; c < columnas; c++) {
                ColumnConstraints columna = new ColumnConstraints();
                columna.setHgrow(Priority.ALWAYS);
                columna.setPercentWidth(-1);
                columna.setMinWidth(120);
                columna.setPrefWidth(200);
                grid.getColumnConstraints().add(columna);
            }
        }
        cabecera.add(new Region(), 0, 0);
        for (int c = 0; c < columnas; c++) {
            cabecera.add(encabezados.get(c), c + 1, 0);
        }

        for (int fila = 0; fila < FILAS; fila++) {
            RowConstraints alto = new RowConstraints(ALTO_FILA);
            alto.setValignment(VPos.TOP);
            cuerpo.getRowConstraints().add(alto);
            LocalTime hora = CitaService.APERTURA.plusMinutes((long) fila * CitaService.MINUTOS_BLOQUE);

            Label etiqueta = new Label(hora.getMinute() == 0 ? Formatos.hora(hora) : "");
            etiqueta.getStyleClass().add("agenda-hora");
            cuerpo.add(etiqueta, 0, fila);

            Region linea = new Region();
            linea.getStyleClass().add(fila == 0 ? "agenda-linea-primera"
                    : hora.getMinute() == 0 ? "agenda-linea-hora" : "agenda-linea");
            linea.setMouseTransparent(true);
            linea.setMaxHeight(Double.MAX_VALUE);
            cuerpo.add(linea, 1, fila, columnas, 1);

            if (gestiona) {
                for (int c = 0; c < columnas; c++) {
                    int columna = c;
                    Region celda = new Region();
                    celda.getStyleClass().add("agenda-celda");
                    celda.setMaxHeight(Double.MAX_VALUE);
                    celda.setOnMouseClicked(e -> alHacerClicVacio.accept(columna, hora));
                    Tooltip.install(celda, new Tooltip("Nueva cita a las " + Formatos.hora(hora)));
                    cuerpo.add(celda, c + 1, fila);
                }
            }
        }
        for (int c = 0; c < columnas; c++) {
            Region separador = new Region();
            separador.getStyleClass().add("agenda-columna");
            separador.setMouseTransparent(true);
            separador.setMaxHeight(Double.MAX_VALUE);
            cuerpo.add(separador, c + 1, 0, 1, FILAS);
        }

        // Las canceladas primero, para que una cita activa en el mismo horario quede encima
        citasVisibles.stream()
                .sorted(Comparator.comparing((Cita c) -> c.getEstado() != EstadoCita.CANCELADA))
                .forEach(cita -> {
                    int columna = columnaDe.applyAsInt(cita);
                    int fila = filaDe(cita.getHoraInicio());
                    int filas = Math.max(1, filaDe(cita.getHoraFin()) - fila);
                    if (columna < 0 || columna >= columnas || fila < 0 || fila >= FILAS) {
                        return;
                    }
                    Node bloque = bloqueCita(cita);
                    GridPane.setMargin(bloque, new Insets(3, 6, 3, 6));
                    cuerpo.add(bloque, columna + 1, fila, 1, Math.min(filas, FILAS - fila));
                });

        if (mostrarHoraActual) {
            agregarHoraActual(cuerpo, columnas);
        }
        VBox contenido = new VBox(cabecera, cuerpo);
        contenido.getStyleClass().add("agenda");
        return contenido;
    }

    private static int filaDe(LocalTime hora) {
        return (int) Duration.between(CitaService.APERTURA, hora).toMinutes() / CitaService.MINUTOS_BLOQUE;
    }

    /** Línea naranja con la hora actual. */
    private static void agregarHoraActual(GridPane cuerpo, int columnas) {
        LocalTime ahora = LocalTime.now();
        if (ahora.isBefore(CitaService.APERTURA) || !ahora.isBefore(CitaService.CIERRE)) {
            return;
        }
        long minutos = Duration.between(CitaService.APERTURA, ahora).toMinutes();
        int fila = (int) minutos / CitaService.MINUTOS_BLOQUE;
        double desplazamiento = (minutos % CitaService.MINUTOS_BLOQUE) * ALTO_FILA / CitaService.MINUTOS_BLOQUE;

        Label etiqueta = new Label(Formatos.hora(ahora));
        etiqueta.getStyleClass().add("hora-actual-etiqueta");
        Region linea = new Region();
        linea.getStyleClass().add("hora-actual-linea");
        HBox.setHgrow(linea, Priority.ALWAYS);
        HBox marca = new HBox(etiqueta, linea);
        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setMouseTransparent(true);
        marca.setMaxHeight(Region.USE_PREF_SIZE);
        // Centra la línea en el minuto exacto dentro de la fila
        marca.setTranslateY(desplazamiento - 9);
        cuerpo.add(marca, 0, fila, columnas + 1, 1);
    }

    private Node encabezadoProfesional(Profesional profesional) {
        Avatar avatar = new Avatar(Formatos.iniciales(profesional.getNombres(), profesional.getApellidos()),
                profesional.getIdProfesional());
        avatar.getStyleClass().add("avatar-mini");
        Label nombre = new Label(profesional.getNombreCompleto());
        nombre.getStyleClass().add("agenda-col-nombre");
        Label especialidad = new Label(profesional.getEspecialidad() == null ? "" : profesional.getEspecialidad());
        especialidad.getStyleClass().add("celda-sub");
        HBox caja = new HBox(avatar, new VBox(nombre, especialidad));
        caja.getStyleClass().add("agenda-col-encabezado");
        return caja;
    }

    /** Bloque de color con paciente, estado, horario y motivo. */
    private Node bloqueCita(Cita cita) {
        Label paciente = new Label(nombreCorto(cita.getPaciente()));
        paciente.getStyleClass().add("cita-nombre");
        paciente.setMinWidth(0);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label estado = new Label(cita.getEstado().getEtiqueta());
        estado.getStyleClass().add("cita-estado");
        estado.setMinWidth(Region.USE_PREF_SIZE);
        HBox arriba = new HBox(6, paciente, espacio, estado);
        arriba.setAlignment(Pos.CENTER_LEFT);

        String detalle = Formatos.rangoHoras(cita.getHoraInicio(), cita.getHoraFin())
                + (cita.getMotivo() == null ? "" : " · " + cita.getMotivo());
        Label abajo = new Label(detalle);
        abajo.getStyleClass().add("cita-detalle");
        abajo.setMinWidth(0);

        VBox bloque = new VBox(arriba, abajo);
        bloque.getStyleClass().addAll("cita-bloque", claseBloque(cita.getEstado()));
        if (citaSeleccionada != null && citaSeleccionada.getIdCita() == cita.getIdCita()) {
            bloque.getStyleClass().add("cita-seleccionada");
        }
        bloque.setMinHeight(0);
        bloque.setMaxHeight(Double.MAX_VALUE);
        bloque.setOnMouseClicked(e -> seleccionarCita(cita));
        Tooltip.install(bloque, new Tooltip(cita.getPaciente().getNombreCompleto() + "\n" + detalle
                + "\n" + cita.getProfesional().getNombreCompleto() + " · " + cita.getEstado().getEtiqueta()));
        return bloque;
    }

    private static String claseBloque(EstadoCita estado) {
        return "cita-" + estado.name().toLowerCase();
    }

    /** "María Quispe": primer nombre y primer apellido. */
    private static String nombreCorto(Paciente paciente) {
        return primeraPalabra(paciente.getNombres()) + " " + primeraPalabra(paciente.getApellidos());
    }

    private static String primeraPalabra(String texto) {
        return texto == null ? "" : texto.strip().split("\\s+")[0];
    }

    private int indiceProfesional(Profesional profesional) {
        for (int i = 0; i < profesionales.size(); i++) {
            if (profesionales.get(i).getIdProfesional() == profesional.getIdProfesional()) {
                return i;
            }
        }
        return -1;
    }

    // ===== Vista Lista =====

    private Node vistaLista() {
        TableView<Cita> tabla = new TableView<>();
        tabla.getStyleClass().add("tabla-agenda");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabla.setFixedCellSize(52);
        tabla.setPrefHeight(Math.max(5, citasVisibles.size()) * 52 + 44);
        tabla.setMinHeight(tabla.getPrefHeight());
        tabla.setPlaceholder(new EstadoVacio("No hay citas esta semana", "Las citas agendadas aparecerán aquí."));
        tabla.getItems().setAll(citasVisibles);
        tabla.getColumns().setAll(List.of(
                columnaTexto("FECHA", 90, c -> Formatos.diaCorto(c.getFecha()) + " " + Formatos.fechaCorta(c.getFecha())),
                columnaTexto("HORA", 100, c -> Formatos.rangoHoras(c.getHoraInicio(), c.getHoraFin())),
                columnaTexto("PACIENTE", 180, c -> c.getPaciente().getNombreCompleto()),
                columnaTexto("PROFESIONAL", 150, c -> c.getProfesional().getNombreCompleto()),
                columnaTexto("MOTIVO", 160, c -> c.getMotivo() == null ? "—" : c.getMotivo()),
                columnaEstado()));
        tabla.setRowFactory(t -> {
            TableRow<Cita> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (!fila.isEmpty()) {
                    seleccionarCita(fila.getItem());
                }
            });
            return fila;
        });
        if (citaSeleccionada != null) {
            tabla.getItems().stream().filter(c -> c.getIdCita() == citaSeleccionada.getIdCita())
                    .findFirst().ifPresent(c -> tabla.getSelectionModel().select(c));
        }
        return tabla;
    }

    private static TableColumn<Cita, Cita> columnaTexto(String titulo, double ancho,
                                                        Function<Cita, String> texto) {
        TableColumn<Cita, Cita> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Cita cita, boolean vacio) {
                super.updateItem(cita, vacio);
                setText(vacio || cita == null ? null : texto.apply(cita));
            }
        });
        return columna;
    }

    private static TableColumn<Cita, Cita> columnaEstado() {
        TableColumn<Cita, Cita> columna = columnaTexto("ESTADO", 110, c -> null);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Cita cita, boolean vacio) {
                super.updateItem(cita, vacio);
                setText(null);
                setGraphic(vacio || cita == null ? null : new Pill(cita.getEstado()));
            }
        });
        return columna;
    }

    // ===== Panel: detalle de una cita =====

    private void seleccionarCita(Cita cita) {
        citaSeleccionada = cita;
        mostrarDetalle(cita);
        refrescar();
    }

    private void mostrarDetalle(Cita cita) {
        mostrarPanel(panelDetalle);
        avisoDetalle.ocultar();
        detalleFechaHora.setText(Formatos.diaYMes(cita.getFecha()) + " · "
                + Formatos.rangoHoras(cita.getHoraInicio(), cita.getHoraFin()));
        detalleEstado.getChildren().setAll(new Pill(cita.getEstado()));
        Paciente paciente = cita.getPaciente();
        detalleAvatar.mostrar(Formatos.iniciales(paciente.getNombres(), paciente.getApellidos()), paciente.getIdPaciente());
        detallePaciente.setText(paciente.getNombreCompleto());
        detalleDni.setText("DNI " + paciente.getDni());
        Profesional profesional = cita.getProfesional();
        detalleProfesional.setText(profesional.getNombreCompleto()
                + (profesional.getEspecialidad() == null ? "" : " · " + profesional.getEspecialidad()));
        detalleMotivo.setText(cita.getMotivo() == null ? "Sin motivo registrado" : cita.getMotivo());

        accionesDetalle.getChildren().clear();
        boolean activa = cita.getEstado() == EstadoCita.PENDIENTE || cita.getEstado() == EstadoCita.CONFIRMADA;
        if (!activa) {
            avisoDetalle.mostrar(Aviso.Tipo.INFO, cita.getEstado() == EstadoCita.ATENDIDA
                    ? "Esta cita ya fue atendida." : "Esta cita fue cancelada; su horario quedó libre.");
        } else {
            boolean puedeAtender = citas.puedeMarcarAtendida(cita) && !cita.getFecha().isAfter(LocalDate.now());
            if (gestiona && cita.getEstado() == EstadoCita.PENDIENTE) {
                accionesDetalle.getChildren().add(boton("Confirmar cita", "btn-primario", "check",
                        () -> ejecutar(() -> citas.confirmar(cita.getIdCita()), "Cita confirmada", cita)));
            }
            if (puedeAtender) {
                accionesDetalle.getChildren().add(boton("Marcar atendida",
                        cita.getEstado() == EstadoCita.CONFIRMADA || !gestiona ? "btn-primario" : "btn-secundario", "check",
                        () -> ejecutar(() -> citas.marcarAtendida(cita.getIdCita()), "Cita marcada como atendida", cita)));
            }
            if (gestiona) {
                accionesDetalle.getChildren().add(boton("Reprogramar", "btn-secundario", "agenda",
                        () -> prepararReprogramacion(cita)));
                accionesDetalle.getChildren().add(boton("Cancelar cita", "btn-peligro", "cerrar", () -> {
                    if (Dialogos.confirmar(areaVista.getScene().getWindow(), "Cancelar cita",
                            "¿Cancelar la cita de " + paciente.getNombreCompleto() + "?",
                            "La cita no se borra: queda como cancelada y el horario se libera.", "Cancelar cita")) {
                        ejecutar(() -> citas.cancelar(cita.getIdCita()), "Cita cancelada", cita);
                    }
                }));
            }
            if (!gestiona && !puedeAtender) {
                avisoDetalle.mostrar(Aviso.Tipo.INFO, "Podrás marcarla como atendida el día de la cita.");
            }
        }
        pieDetalle.setVisible(gestiona);
        pieDetalle.setManaged(gestiona);
    }

    private Button boton(String texto, String clase, String icono, Runnable accion) {
        Button boton = new Button(texto, new Icono(icono, 16));
        boton.getStyleClass().add(clase);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    /** Ejecuta un cambio de estado, avisa y vuelve a mostrar la cita actualizada. */
    private void ejecutar(Runnable cambio, String mensaje, Cita cita) {
        try {
            cambio.run();
            notificar(mensaje);
            citaSeleccionada = citas.buscarPorId(cita.getIdCita()).orElse(null);
            if (citaSeleccionada != null) {
                mostrarDetalle(citaSeleccionada);
            }
            refrescar();
        } catch (RuntimeException e) {
            avisoDetalle.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    @FXML
    private void nuevaCita() {
        limpiarFormulario();
        mostrarFormulario();
    }

    // ===== Panel: nueva cita / reprogramar =====

    private void configurarFormulario() {
        StringConverter<LocalTime> horas = new StringConverter<>() {
            @Override
            public String toString(LocalTime hora) {
                return hora == null ? "" : Formatos.hora(hora);
            }

            @Override
            public LocalTime fromString(String texto) {
                return texto == null || texto.isBlank() ? null : LocalTime.parse(texto);
            }
        };
        campoInicio.setConverter(horas);
        campoFin.setConverter(horas);
        campoInicio.getItems().setAll(CitaService.horasDeInicio());
        campoProfesional.getItems().setAll(profesionales);
        campoProfesional.setConverter(convertidorProfesional());
        configurarFecha();

        ToggleGroup estados = new ToggleGroup();
        estadoPendiente.setToggleGroup(estados);
        estadoConfirmada.setToggleGroup(estados);
        estados.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            }
        });

        // El fin se calcula: por defecto inicio + 30 min, conservando la duración elegida
        campoInicio.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (cargandoFormulario || nuevo == null) {
                return;
            }
            long duracion = anterior != null && campoFin.getValue() != null
                    ? Duration.between(anterior, campoFin.getValue()).toMinutes() : CitaService.MINUTOS_BLOQUE;
            actualizarOpcionesFin(nuevo);
            LocalTime fin = nuevo.plusMinutes(Math.max(duracion, CitaService.MINUTOS_BLOQUE));
            campoFin.setValue(fin.isAfter(CitaService.CIERRE) || fin.isBefore(nuevo)
                    ? nuevo.plusMinutes(CitaService.MINUTOS_BLOQUE) : fin);
        });
        for (var propiedad : List.of(campoProfesional.valueProperty(), campoFecha.valueProperty(),
                campoInicio.valueProperty(), campoFin.valueProperty())) {
            propiedad.addListener((obs, anterior, nuevo) -> {
                if (!cargandoFormulario) {
                    actualizarDisponibilidad();
                }
            });
        }
        campoProfesional.valueProperty().addListener((obs, a, n) -> cfProfesional.limpiarError());
        campoFecha.valueProperty().addListener((obs, a, n) -> cfFecha.limpiarError());

        configurarBuscadorPaciente();
    }

    private void configurarFecha() {
        campoFecha.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate valor) {
                return valor == null ? "" : Formatos.formatoFechaCampo().format(valor);
            }

            @Override
            public LocalDate fromString(String texto) {
                try {
                    return texto == null || texto.isBlank() ? null
                            : LocalDate.parse(texto.strip(), Formatos.formatoFechaCampo());
                } catch (DateTimeParseException e) {
                    return campoFecha.getValue();
                }
            }
        });
        // RN-08: no se agenda en fechas pasadas
        campoFecha.setDayCellFactory(selector -> new DateCell() {
            @Override
            public void updateItem(LocalDate dia, boolean vacio) {
                super.updateItem(dia, vacio);
                setDisable(vacio || dia.isBefore(LocalDate.now()));
            }
        });
    }

    private void actualizarOpcionesFin(LocalTime inicio) {
        List<LocalTime> opciones = new ArrayList<>();
        for (LocalTime hora = inicio.plusMinutes(CitaService.MINUTOS_BLOQUE); !hora.isAfter(CitaService.CIERRE)
                && hora.isAfter(inicio); hora = hora.plusMinutes(CitaService.MINUTOS_BLOQUE)) {
            opciones.add(hora);
        }
        campoFin.getItems().setAll(opciones);
    }

    /** Validación de cruce en vivo (CU-03): aviso rojo con horarios libres, o verde si está disponible. */
    private void actualizarDisponibilidad() {
        horariosLibres.getChildren().clear();
        mostrar(horariosLibres, false);
        cfInicio.limpiarError();
        Profesional profesional = campoProfesional.getValue();
        LocalDate dia = campoFecha.getValue();
        LocalTime inicio = campoInicio.getValue();
        LocalTime fin = campoFin.getValue();
        if (profesional == null || dia == null || inicio == null || fin == null) {
            avisoDisponibilidad.ocultar();
            actualizarBotonAgendar(false);
            return;
        }
        Optional<String> problema = citas.problemaDeFechaHora(dia, inicio);
        if (problema.isPresent()) {
            avisoDisponibilidad.mostrar(Aviso.Tipo.ERROR, problema.get());
            actualizarBotonAgendar(false);
            return;
        }
        Cita borrador = borrador();
        Optional<Cita> cruce;
        try {
            cruce = citas.buscarCruce(borrador);
        } catch (RuntimeException e) {
            avisoDisponibilidad.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            actualizarBotonAgendar(false);
            return;
        }
        if (cruce.isPresent()) {
            cfInicio.mostrarError("Horario ocupado");
            avisoDisponibilidad.mostrar(Aviso.Tipo.ERROR,
                    "Cruce de horario. " + CitaService.mensajeCruce(profesional, cruce.get()));
            mostrarHorariosLibres(profesional, dia, (int) Duration.between(inicio, fin).toMinutes());
            actualizarBotonAgendar(false);
        } else {
            avisoDisponibilidad.mostrar(Aviso.Tipo.EXITO, "Horario disponible para " + profesional.getNombreCompleto() + ".");
            actualizarBotonAgendar(true);
        }
    }

    private void mostrarHorariosLibres(Profesional profesional, LocalDate dia, int duracion) {
        int excluir = citaEnEdicion == null ? 0 : citaEnEdicion.getIdCita();
        List<LocalTime> libres = citas.horariosLibres(profesional.getIdProfesional(), dia, duracion, excluir);
        Label titulo = new Label(libres.isEmpty() ? "No quedan horarios libres este día." : "Horarios libres:");
        titulo.getStyleClass().add("horarios-titulo");
        horariosLibres.getChildren().add(titulo);
        // Los más cercanos a la hora elegida primero
        LocalTime elegida = campoInicio.getValue();
        libres.stream()
                .sorted(Comparator.comparingLong(h -> Math.abs(Duration.between(elegida, h).toMinutes())))
                .limit(MAX_HORARIOS_SUGERIDOS)
                .sorted()
                .forEach(hora -> {
                    Button chip = new Button(Formatos.hora(hora));
                    chip.getStyleClass().add("chip-hora");
                    chip.setOnAction(e -> {
                        long duracionActual = Duration.between(campoInicio.getValue(), campoFin.getValue()).toMinutes();
                        cargandoFormulario = true;
                        campoInicio.setValue(hora);
                        actualizarOpcionesFin(hora);
                        campoFin.setValue(hora.plusMinutes(duracionActual));
                        cargandoFormulario = false;
                        actualizarDisponibilidad();
                    });
                    horariosLibres.getChildren().add(chip);
                });
        mostrar(horariosLibres, true);
    }

    private void actualizarBotonAgendar(boolean horarioLibre) {
        botonAgendar.setDisable(!horarioLibre);
    }

    @FXML
    private void guardarCita() {
        avisoFormulario.ocultar();
        cfPaciente.limpiarError();
        if (pacienteElegido == null) {
            cfPaciente.mostrarError("Elige un paciente");
            campoPaciente.requestFocus();
            return;
        }
        Cita cita = borrador();
        cita.setMotivo(campoMotivo.getText());
        try {
            if (citaEnEdicion == null) {
                cita.setEstado(estadoConfirmada.isSelected() ? EstadoCita.CONFIRMADA : EstadoCita.PENDIENTE);
                citas.agendar(cita);
                notificar("Cita agendada");
            } else {
                citas.reprogramar(cita);
                notificar("Cita reprogramada");
            }
            fecha = cita.getFecha();
            limpiarFormulario();
            citaSeleccionada = null;
            refrescar();
        } catch (ValidacionException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, String.join(" ", e.getErrores().values()));
        } catch (ReglaNegocioException e) {
            String campo = e.getCampo().orElse("");
            switch (campo) {
                case "fecha" -> cfFecha.mostrarError(e.getMessage());
                case "horaInicio", "horaFin" -> {
                    avisoDisponibilidad.mostrar(Aviso.Tipo.ERROR, e.getMessage());
                    cfInicio.mostrarError("Revisa el horario");
                }
                default -> avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            }
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    @FXML
    private void cancelarFormulario() {
        limpiarFormulario();
        if (citaSeleccionada != null) {
            mostrarDetalle(citaSeleccionada);
        }
    }

    /** Clic en un espacio vacío de la cuadrícula. */
    private void prepararNuevaCita(Profesional profesional, LocalDate dia, LocalTime hora) {
        if (!gestiona) {
            return;
        }
        citaSeleccionada = null;
        if (citaEnEdicion != null) {
            limpiarFormulario();
        }
        mostrarFormulario();
        cargandoFormulario = true;
        campoProfesional.setValue(profesional);
        campoFecha.setValue(dia.isBefore(LocalDate.now()) ? LocalDate.now() : dia);
        campoInicio.setValue(hora);
        actualizarOpcionesFin(hora);
        campoFin.setValue(hora.plusMinutes(CitaService.MINUTOS_BLOQUE));
        cargandoFormulario = false;
        actualizarDisponibilidad();
        refrescar();
        if (pacienteElegido == null) {
            campoPaciente.requestFocus();
        }
    }

    private void prepararReprogramacion(Cita cita) {
        limpiarFormulario();
        citaEnEdicion = cita;
        mostrarFormulario();
        tituloFormulario.setText("Reprogramar cita");
        botonAgendar.setText("Guardar cambios");
        mostrar(bloqueEstado, false);
        elegirPaciente(cita.getPaciente());
        cargandoFormulario = true;
        campoProfesional.getItems().stream()
                .filter(p -> p.getIdProfesional() == cita.getProfesional().getIdProfesional())
                .findFirst().ifPresent(campoProfesional::setValue);
        campoFecha.setValue(cita.getFecha());
        campoInicio.setValue(cita.getHoraInicio());
        actualizarOpcionesFin(cita.getHoraInicio());
        campoFin.setValue(cita.getHoraFin());
        campoMotivo.setText(cita.getMotivo());
        cargandoFormulario = false;
        actualizarDisponibilidad();
    }

    private void limpiarFormulario() {
        if (!gestiona) {
            return;
        }
        citaEnEdicion = null;
        cargandoFormulario = true;
        quitarPaciente();
        campoProfesional.setValue(profesionales.size() == 1 ? profesionales.get(0) : null);
        campoFecha.setValue(fecha.isBefore(LocalDate.now()) ? LocalDate.now() : fecha);
        campoInicio.setValue(null);
        campoFin.setValue(null);
        campoFin.getItems().clear();
        campoMotivo.clear();
        estadoPendiente.setSelected(true);
        cargandoFormulario = false;
        tituloFormulario.setText("Nueva cita");
        botonAgendar.setText("Agendar cita");
        mostrar(bloqueEstado, true);
        avisoFormulario.ocultar();
        cfPaciente.limpiarError();
        cfFecha.limpiarError();
        cfInicio.limpiarError();
        actualizarDisponibilidad();
    }

    /** La cita que describe el formulario (sin validar). */
    private Cita borrador() {
        Cita cita = new Cita();
        if (citaEnEdicion != null) {
            cita.setIdCita(citaEnEdicion.getIdCita());
            cita.setEstado(citaEnEdicion.getEstado());
        }
        cita.setPaciente(pacienteElegido);
        cita.setProfesional(campoProfesional.getValue());
        cita.setFecha(campoFecha.getValue());
        cita.setHoraInicio(campoInicio.getValue());
        cita.setHoraFin(campoFin.getValue());
        return cita;
    }

    // ===== Buscador de pacientes con autocompletado =====

    private void configurarBuscadorPaciente() {
        sugerencias.getStyleClass().add("sugerencias");
        esperaBusqueda.setOnFinished(e -> mostrarSugerencias());
        campoPaciente.textProperty().addListener((obs, anterior, nuevo) -> {
            cfPaciente.limpiarError();
            esperaBusqueda.playFromStart();
        });
        campoPaciente.setOnAction(e -> {
            esperaBusqueda.stop();
            mostrarSugerencias();
            e.consume();
        });
        mostrar(tarjetaPaciente, false);
    }

    private void mostrarSugerencias() {
        String texto = campoPaciente.getText();
        if (texto == null || texto.isBlank()) {
            sugerencias.hide();
            return;
        }
        List<Paciente> encontrados = pacientes.buscar(texto, FiltroPaciente.TODOS, 0).stream().limit(8).toList();
        sugerencias.getItems().clear();
        if (encontrados.isEmpty()) {
            Label nada = new Label("No se encontraron pacientes");
            nada.getStyleClass().add("texto-tenue");
            CustomMenuItem item = new CustomMenuItem(nada, false);
            item.setDisable(true);
            sugerencias.getItems().add(item);
        }
        for (Paciente paciente : encontrados) {
            Avatar avatar = new Avatar(paciente.getIniciales(), paciente.getIdPaciente());
            avatar.getStyleClass().add("avatar-mini");
            Label nombre = new Label(paciente.getNombreCompleto());
            nombre.getStyleClass().add("celda-titulo");
            Label dni = new Label("DNI " + paciente.getDni());
            dni.getStyleClass().add("celda-sub");
            HBox fila = new HBox(10, avatar, new VBox(nombre, dni));
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPrefWidth(buscadorPaciente.getWidth() - 24);
            CustomMenuItem item = new CustomMenuItem(fila, true);
            item.setOnAction(e -> elegirPaciente(paciente));
            sugerencias.getItems().add(item);
        }
        if (!sugerencias.isShowing()) {
            sugerencias.show(buscadorPaciente, Side.BOTTOM, 0, 4);
        }
    }

    private void elegirPaciente(Paciente paciente) {
        pacienteElegido = paciente;
        sugerencias.hide();
        cfPaciente.limpiarError();
        avatarPaciente.mostrar(Formatos.iniciales(paciente.getNombres(), paciente.getApellidos()), paciente.getIdPaciente());
        nombrePaciente.setText(paciente.getNombreCompleto());
        dniPaciente.setText("DNI " + paciente.getDni());
        mostrar(buscadorPaciente, false);
        mostrar(tarjetaPaciente, true);
    }

    @FXML
    private void quitarPaciente() {
        pacienteElegido = null;
        campoPaciente.clear();
        sugerencias.hide();
        mostrar(tarjetaPaciente, false);
        mostrar(buscadorPaciente, true);
    }

    // ===== Utilidades =====

    /** El odontólogo solo ve su propia columna. */
    private List<Profesional> profesionalesVisibles() {
        try {
            List<Profesional> todos = profesionalesServicio.listarTodos();
            if (gestiona) {
                return todos;
            }
            int propio = Sesion.get().getProfesional().map(Profesional::getIdProfesional).orElse(-1);
            return todos.stream().filter(p -> p.getIdProfesional() == propio).toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static StringConverter<Profesional> convertidorProfesional() {
        return new StringConverter<>() {
            @Override
            public String toString(Profesional profesional) {
                if (profesional == null) {
                    return "";
                }
                return profesional.getNombreCompleto()
                        + (profesional.getEspecialidad() == null ? "" : " · " + profesional.getEspecialidad());
            }

            @Override
            public Profesional fromString(String texto) {
                return null;
            }
        };
    }

    private void mostrarFormulario() {
        mostrarPanel(panelFormulario);
    }

    private void mostrarPanel(VBox visible) {
        for (VBox modo : List.of(panelFormulario, panelDetalle, panelVacio)) {
            mostrar(modo, modo == visible);
        }
    }

    private static void mostrar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(areaVista.getScene().getWindow(), mensaje);
    }
}
