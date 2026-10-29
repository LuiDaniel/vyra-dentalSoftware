package com.vyra.dental.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.service.CitaService;
import com.vyra.dental.service.PacienteService;
import com.vyra.dental.service.PagoService;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.service.TratamientoResumen;
import com.vyra.dental.service.TratamientoService;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Formatos;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Inicio (docs/08-guia-frontend.md §7.2, CU-12, RF-12): saludo, aviso de citas de la próxima hora,
 * cuatro cifras, agenda de hoy, ingresos de la semana (solo administrador) y accesos rápidos.
 * El odontólogo ve solo sus citas (lo filtra {@link CitaService}).
 */
public class InicioController {

    private static final double ALTO_BARRA = 120;

    @FXML private Label saludo;
    @FXML private Button botonNuevoPaciente;
    @FXML private Button botonNuevaCita;
    @FXML private Aviso avisoProximas;
    @FXML private HBox kpis;
    @FXML private HBox cuerpo;

    private final CitaService citas = Servicios.citas();
    private final PacienteService pacientes = Servicios.pacientes();
    private final TratamientoService tratamientos = Servicios.tratamientos();
    private final PagoService pagos = Servicios.pagos();
    private final Rol rol = Sesion.get().getUsuario().getRol();

    @FXML
    private void initialize() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        String nombre = Sesion.get().getUsuario().getNombreCompleto().strip().split("\\s+")[0];
        saludo.setText(saludoSegunHora(ahora.toLocalTime()) + ", " + nombre);
        alternar(botonNuevoPaciente, pacientes.puedeEditar());
        alternar(botonNuevaCita, citas.puedeGestionar());

        List<Cita> deHoy = citas.citasDelDia(hoy).stream()
                .sorted(Comparator.comparing(Cita::getHoraInicio).thenComparing(c -> c.getProfesional().getIdProfesional()))
                .toList();
        mostrarProximas(deHoy, ahora);
        kpis.getChildren().setAll(kpiCitas(deHoy), kpiPacientes(hoy), kpiTratamientos(hoy), kpiFinanciero(deHoy, hoy));
        for (Node kpi : kpis.getChildren()) {
            HBox.setHgrow(kpi, Priority.ALWAYS);
        }

        Node agenda = tarjetaAgenda(deHoy);
        HBox.setHgrow(agenda, Priority.ALWAYS);
        VBox derecha = new VBox(20);
        derecha.setPrefWidth(380);
        derecha.setMinWidth(340);
        if (rol == Rol.ADMIN) {
            derecha.getChildren().add(tarjetaIngresosSemana(hoy));
        }
        derecha.getChildren().add(tarjetaAccesos());
        cuerpo.getChildren().setAll(agenda, derecha);
    }

    @FXML
    private void nuevoPaciente() {
        MainController.actual().navegar(Pantalla.PACIENTES, "nuevo");
    }

    @FXML
    private void nuevaCita() {
        MainController.actual().navegar(Pantalla.AGENDA);
    }

    /** Antes de las 12 "Buenos días"; antes de las 19 "Buenas tardes"; después "Buenas noches". */
    static String saludoSegunHora(LocalTime hora) {
        if (hora.isBefore(LocalTime.NOON)) {
            return "Buenos días";
        }
        return hora.isBefore(LocalTime.of(19, 0)) ? "Buenas tardes" : "Buenas noches";
    }

    // ===== Aviso de la próxima hora (RF-12) =====

    private void mostrarProximas(List<Cita> deHoy, LocalDateTime ahora) {
        LocalTime desde = ahora.toLocalTime();
        LocalTime hasta = desde.plusHours(1);
        List<Cita> proximas = deHoy.stream()
                .filter(c -> c.getEstado() == EstadoCita.PENDIENTE || c.getEstado() == EstadoCita.CONFIRMADA)
                .filter(c -> !c.getHoraInicio().isBefore(desde) && !c.getHoraInicio().isAfter(hasta))
                .toList();
        if (desde.isAfter(LocalTime.of(22, 59)) || proximas.isEmpty()) {
            avisoProximas.ocultar();
            return;
        }
        String lista = proximas.stream()
                .map(c -> c.getPaciente().getNombres().split("\\s+")[0] + " " + c.getPaciente().getApellidos().split("\\s+")[0]
                        + " " + Formatos.hora(c.getHoraInicio()))
                .collect(Collectors.joining(" · "));
        avisoProximas.mostrar(Aviso.Tipo.INFO, proximas.size() + (proximas.size() == 1 ? " cita" : " citas")
                + " en la próxima hora: " + lista);
        Button ver = new Button("Ver agenda");
        ver.getStyleClass().add("enlace");
        ver.setOnAction(e -> MainController.actual().navegar(Pantalla.AGENDA));
        avisoProximas.getChildren().add(ver);
    }

    // ===== Cifras =====

    private Node kpiCitas(List<Cita> deHoy) {
        long activas = deHoy.stream().filter(c -> c.getEstado() != EstadoCita.CANCELADA).count();
        long pendientes = deHoy.stream().filter(c -> c.getEstado() == EstadoCita.PENDIENTE).count();
        return kpi("agenda", "Citas de hoy", String.valueOf(activas),
                pendientes + (pendientes == 1 ? " pendiente de confirmar" : " pendientes de confirmar"));
    }

    private Node kpiPacientes(LocalDate hoy) {
        int total = pacientes.contar(null, FiltroPaciente.TODOS);
        int nuevos = pacientes.nuevosDesde(hoy.withDayOfMonth(1));
        return kpi("pacientes", "Pacientes registrados", String.valueOf(total),
                "+" + nuevos + " en " + nombreDelMes(hoy));
    }

    private Node kpiTratamientos(LocalDate hoy) {
        List<TratamientoResumen> enCurso = tratamientos.listar(EstadoTratamiento.EN_CURSO, null);
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        long nuevos = enCurso.stream().filter(t -> !t.tratamiento().getFecha().isBefore(inicioMes)).count();
        return kpi("tratamientos", "Tratamientos en curso", String.valueOf(enCurso.size()),
                nuevos + (nuevos == 1 ? " iniciado este mes" : " iniciados este mes"));
    }

    /** Ingresos del mes (administrador), pagos de hoy (recepción) o citas atendidas hoy (odontólogo). */
    private Node kpiFinanciero(List<Cita> deHoy, LocalDate hoy) {
        if (rol == Rol.ADMIN) {
            LocalDate inicioMes = hoy.withDayOfMonth(1);
            double mes = pagos.cobradoEntre(inicioMes, hoy);
            double anterior = pagos.cobradoEntre(inicioMes.minusMonths(1), inicioMes.minusDays(1));
            String detalle = anterior <= 0 ? "Sin cobros el mes pasado para comparar"
                    : (mes >= anterior ? "+" : "") + Math.round((mes - anterior) / anterior * 100)
                            + "% respecto a " + nombreDelMes(inicioMes.minusMonths(1));
            return kpi("pagos", "Ingresos de " + nombreDelMes(hoy), Formatos.dinero(mes), detalle);
        }
        if (rol == Rol.RECEPCION) {
            int cantidad = pagos.pagosEntre(hoy, hoy).size();
            return kpi("pagos", "Pagos registrados hoy", Formatos.dinero(pagos.cobradoEntre(hoy, hoy)),
                    cantidad + (cantidad == 1 ? " pago" : " pagos"));
        }
        long atendidas = deHoy.stream().filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count();
        long activas = deHoy.stream().filter(c -> c.getEstado() != EstadoCita.CANCELADA).count();
        return kpi("check", "Atendidas hoy", String.valueOf(atendidas), "de " + activas + " citas de hoy");
    }

    private static Node kpi(String icono, String titulo, String cifra, String detalle) {
        StackPane cuadro = new StackPane(new Icono(icono, 20));
        cuadro.getStyleClass().add("kpi-icono");
        Label t = new Label(titulo);
        t.getStyleClass().add("texto-secundario");
        Label valor = new Label(cifra);
        valor.getStyleClass().add("kpi-valor");
        Label d = new Label(detalle);
        d.getStyleClass().add("celda-sub");
        d.setWrapText(true);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(t, espacio, cuadro);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(6, cabecera, valor, d);
        tarjeta.getStyleClass().add("kpi");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    // ===== Agenda de hoy =====

    private Node tarjetaAgenda(List<Cita> deHoy) {
        long profesionales = deHoy.stream().map(c -> c.getProfesional().getIdProfesional()).distinct().count();
        Label titulo = new Label("Agenda de hoy");
        titulo.getStyleClass().add("card-titulo");
        Label subtitulo = new Label(deHoy.size() + (deHoy.size() == 1 ? " cita" : " citas") + " · " + profesionales
                + (profesionales == 1 ? " profesional" : " profesionales"));
        subtitulo.getStyleClass().add("texto-secundario");
        Button verTodas = new Button("Ver todas");
        verTodas.getStyleClass().add("enlace");
        verTodas.setOnAction(e -> MainController.actual().navegar(Pantalla.AGENDA));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(new VBox(2, titulo, subtitulo), espacio, verTodas);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("lista-barra");

        VBox tarjeta = new VBox(cabecera);
        tarjeta.getStyleClass().add("card");
        if (deHoy.isEmpty()) {
            tarjeta.getChildren().add(new EstadoVacio("No hay citas para hoy",
                    "Cuando se agende una cita para hoy, aparecerá aquí."));
        }
        for (Cita cita : deHoy) {
            tarjeta.getChildren().add(filaCita(cita));
        }
        return tarjeta;
    }

    private Node filaCita(Cita cita) {
        Label inicio = new Label(Formatos.hora(cita.getHoraInicio()));
        inicio.getStyleClass().add("celda-titulo");
        Label fin = new Label(Formatos.hora(cita.getHoraFin()));
        fin.getStyleClass().add("celda-sub");
        VBox horas = new VBox(2, inicio, fin);
        horas.setMinWidth(52);
        Avatar avatar = new Avatar(cita.getPaciente().getIniciales(), cita.getPaciente().getIdPaciente());
        Label nombre = new Label(cita.getPaciente().getNombreCompleto());
        nombre.getStyleClass().add("celda-titulo");
        Label detalle = new Label((cita.getMotivo() == null ? "Sin motivo" : cita.getMotivo()) + " · "
                + cita.getProfesional().getNombreCompleto());
        detalle.getStyleClass().add("celda-sub");
        VBox textos = new VBox(2, nombre, detalle);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox fila = new HBox(14, horas, avatar, textos, new Pill(cita.getEstado()));
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("inicio-fila-cita");
        fila.setOnMouseClicked(e -> MainController.actual().navegar(Pantalla.AGENDA));
        return fila;
    }

    // ===== Ingresos de la semana (administrador) =====

    private Node tarjetaIngresosSemana(LocalDate hoy) {
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<Double> montos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate dia = lunes.plusDays(i);
            montos.add(dia.isAfter(hoy) ? 0 : pagos.cobradoEntre(dia, dia));
        }
        double maximo = montos.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double total = montos.stream().mapToDouble(Double::doubleValue).sum();

        Label titulo = new Label("Ingresos de la semana");
        titulo.getStyleClass().add("card-titulo");
        Label rango = new Label(Formatos.diaCorto(lunes) + " " + Formatos.fechaCorta(lunes).substring(0, 6) + " – "
                + Formatos.diaCorto(lunes.plusDays(5)) + " " + Formatos.fechaCorta(lunes.plusDays(5)).substring(0, 6));
        rango.getStyleClass().add("texto-secundario");
        Label totalSemana = new Label(Formatos.dinero(total));
        totalSemana.getStyleClass().add("card-titulo");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(new VBox(2, titulo, rango), espacio, totalSemana);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        HBox barras = new HBox(10);
        barras.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < montos.size(); i++) {
            LocalDate dia = lunes.plusDays(i);
            double monto = montos.get(i);
            Label cifra = new Label(dia.isAfter(hoy) ? "—" : String.format(java.util.Locale.US, "%,.0f", monto));
            cifra.getStyleClass().add("celda-sub");
            Region barra = new Region();
            barra.getStyleClass().add(dia.equals(hoy) ? "inicio-barra-hoy" : "inicio-barra");
            double alto = maximo <= 0 ? 4 : Math.max(4, monto / maximo * ALTO_BARRA);
            barra.setPrefHeight(alto);
            barra.setMinHeight(alto);
            barra.setMaxHeight(alto);
            Label etiqueta = new Label(Formatos.diaCorto(dia));
            etiqueta.getStyleClass().add("texto-apoyo");
            VBox columna = new VBox(4, cifra, barra, etiqueta);
            columna.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(columna, Priority.ALWAYS);
            barra.setMaxWidth(Double.MAX_VALUE);
            barras.getChildren().add(columna);
        }
        VBox tarjeta = new VBox(16, cabecera, barras);
        tarjeta.getStyleClass().addAll("card", "inicio-tarjeta");
        return tarjeta;
    }

    // ===== Accesos rápidos =====

    private Node tarjetaAccesos() {
        Label titulo = new Label("Accesos rápidos");
        titulo.getStyleClass().add("card-titulo");
        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        List<Node> botones = new ArrayList<>();
        if (pacientes.puedeEditar()) {
            botones.add(acceso("pacientes", "Registrar paciente", () ->
                    MainController.actual().navegar(Pantalla.PACIENTES, "nuevo")));
        }
        if (citas.puedeGestionar()) {
            botones.add(acceso("agenda", "Agendar cita", () -> MainController.actual().navegar(Pantalla.AGENDA)));
        }
        if (pagos.puedeAcceder()) {
            botones.add(acceso("pagos", "Registrar pago", () -> MainController.actual().navegar(Pantalla.PAGOS)));
        }
        if (Pantalla.ODONTOGRAMA.permitidaPara(rol)) {
            botones.add(acceso("diente", "Abrir odontograma", () ->
                    MainController.actual().navegar(Pantalla.ODONTOGRAMA)));
        }
        if (rol == Rol.ODONTOLOGO) {
            botones.add(acceso("tratamientos", "Ver tratamientos", () ->
                    MainController.actual().navegar(Pantalla.TRATAMIENTOS)));
        }
        for (int i = 0; i < botones.size(); i++) {
            rejilla.add(botones.get(i), i % 2, i / 2);
        }
        rejilla.getColumnConstraints().addAll(columnaMitad(), columnaMitad());
        VBox tarjeta = new VBox(12, titulo, rejilla);
        tarjeta.getStyleClass().addAll("card", "inicio-tarjeta");
        return tarjeta;
    }

    private static javafx.scene.layout.ColumnConstraints columnaMitad() {
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        c.setPercentWidth(50);
        return c;
    }

    private static Button acceso(String icono, String texto, Runnable accion) {
        Button boton = new Button(texto, new Icono(icono, 18));
        boton.getStyleClass().add("inicio-acceso");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    // ===== Utilidades =====

    /** "octubre": el nombre del mes en minúscula. */
    private static String nombreDelMes(LocalDate fecha) {
        return Formatos.mesAnio(fecha).split(" ")[0];
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
