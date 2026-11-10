package com.vyra.dental.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Supplier;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.service.CitaService;
import com.vyra.dental.service.PagoService;
import com.vyra.dental.service.PagoService.Periodo;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.GraficoBarras;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.ui.Kpi;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Inicio (docs/08-guia-frontend.md §7.2, RF-12, CU-12): saludo, aviso de citas
 * de la próxima hora, indicadores, agenda de hoy, ingresos de la semana (solo
 * el Administrador) y accesos rápidos según el rol.
 */
public class InicioController {

    /** Minutos del aviso de citas próximas (RF-12). */
    private static final int MINUTOS_AVISO = 60;

    /** Un acceso rápido: se muestra solo si el rol puede abrir esa pantalla. */
    private record Acceso(String texto, String icono, Pantalla pantalla, Object parametro) {
    }

    @FXML private VBox raiz;
    @FXML private Label saludo;
    @FXML private HBox accionesEncabezado;
    @FXML private HBox avisoProximas;
    @FXML private Label proximasTitulo;
    @FXML private Label proximasLista;
    @FXML private HBox kpis;
    @FXML private Label resumenAgenda;
    @FXML private VBox filasAgenda;
    @FXML private VBox tarjetaIngresos;
    @FXML private Label rangoSemana;
    @FXML private Label totalSemana;
    @FXML private GraficoBarras graficoSemana;
    @FXML private GridPane accesos;

    private final CitaService citas = Servicios.citas();
    private final LocalDate hoy = LocalDate.now();
    private Rol rol;

    @FXML
    private void initialize() {
        Usuario usuario = Sesion.get().getUsuario();
        rol = usuario.getRol();
        saludo.setText(saludo(LocalTime.now()) + ", " + primerNombre(usuario.getNombreCompleto()));
        alternar(accionesEncabezado, citas.puedeGestionar());

        List<Cita> deHoy = cargar(() -> citas.citasDelDia(hoy), List.of());
        mostrarProximas();
        construirKpis(deHoy);
        mostrarAgenda(deHoy);
        mostrarIngresosSemana();
        construirAccesos();
    }

    // ===== Aviso de citas próximas =====

    private void mostrarProximas() {
        List<Cita> proximas = cargar(() -> citas.citasProximas(MINUTOS_AVISO), List.of());
        alternar(avisoProximas, !proximas.isEmpty());
        if (proximas.isEmpty()) {
            return;
        }
        proximasTitulo.setText(proximas.size() + (proximas.size() == 1 ? " cita" : " citas") + " en la próxima hora");
        List<String> nombres = new ArrayList<>();
        for (Cita cita : proximas) {
            nombres.add(nombreCorto(cita.getPaciente().getNombres(), cita.getPaciente().getApellidos()) + " "
                    + Formatos.hora(cita.getHoraInicio()));
        }
        proximasLista.setText(String.join(" · ", nombres));
    }

    // ===== Indicadores =====

    private void construirKpis(List<Cita> deHoy) {
        long pendientes = deHoy.stream().filter(c -> c.getEstado() == EstadoCita.PENDIENTE).count();
        kpis.getChildren().add(new Kpi("Citas de hoy", "agenda").mostrar(String.valueOf(deHoy.size()),
                pendientes == 0 ? "Ninguna pendiente de confirmar"
                        : pendientes + (pendientes == 1 ? " pendiente de confirmar" : " pendientes de confirmar")));

        String mes = Formatos.mes(hoy);
        int registrados = cargar(() -> Servicios.pacientes().contar("", FiltroPaciente.TODOS), 0);
        int nuevos = cargar(() -> Servicios.pacientes().nuevosEntre(hoy.withDayOfMonth(1), hoy), 0);
        kpis.getChildren().add(new Kpi("Pacientes registrados", "pacientes").mostrar(String.valueOf(registrados),
                "+" + nuevos + " en " + mes));

        List<TratamientoResumen> enCurso = cargar(
                () -> Servicios.tratamientos().buscar("", EstadoTratamiento.EN_CURSO), List.of());
        long delMes = enCurso.stream()
                .filter(t -> !t.getTratamiento().getFecha().isBefore(hoy.withDayOfMonth(1))).count();
        kpis.getChildren().add(new Kpi("Tratamientos en curso", "tratamientos").mostrar(String.valueOf(enCurso.size()),
                delMes + (delMes == 1 ? " iniciado en " : " iniciados en ") + mes));

        kpis.getChildren().add(switch (rol) {
            case ADMIN -> kpiIngresos();
            case RECEPCION -> kpiPagosDeHoy();
            case ODONTOLOGO -> {
                long atendidas = deHoy.stream().filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count();
                yield new Kpi("Atendidas hoy", "check").mostrar(String.valueOf(atendidas),
                        "de " + deHoy.size() + " citas de hoy");
            }
        });
    }

    /** Solo el Administrador ve los ingresos del mes. */
    private Kpi kpiIngresos() {
        PagoService pagos = Servicios.pagos();
        double total = cargar(() -> pagos.totalCobrado(pagos.rango(Periodo.MES)), 0.0);
        double anterior = cargar(() -> pagos.totalCobrado(pagos.rangoAnterior(Periodo.MES)), 0.0);
        String mesAnterior = Formatos.mes(hoy.minusMonths(1));
        Kpi tarjeta = new Kpi("Ingresos de " + Formatos.mes(hoy), "dinero");
        if (anterior == 0) {
            return tarjeta.mostrar(Formatos.dinero(total), "Sin cobros en " + mesAnterior + " para comparar");
        }
        long porcentaje = Math.round((total - anterior) / anterior * 100);
        return tarjeta.mostrar(Formatos.dinero(total),
                (porcentaje >= 0 ? "+" : "") + porcentaje + "% respecto a " + mesAnterior, porcentaje >= 0 ? 1 : -1);
    }

    /** La recepción ve lo cobrado hoy, sin el total del mes. */
    private Kpi kpiPagosDeHoy() {
        PagoService pagos = Servicios.pagos();
        int cantidad = cargar(() -> pagos.pagosEntre(pagos.rango(Periodo.HOY)).size(), 0);
        double total = cargar(() -> pagos.totalCobrado(pagos.rango(Periodo.HOY)), 0.0);
        return new Kpi("Pagos registrados hoy", "pagos").mostrar(String.valueOf(cantidad),
                Formatos.dinero(total) + " cobrados");
    }

    // ===== Agenda de hoy =====

    private void mostrarAgenda(List<Cita> deHoy) {
        long profesionales = deHoy.stream().map(c -> c.getProfesional().getIdProfesional()).distinct().count();
        resumenAgenda.setText(deHoy.size() + (deHoy.size() == 1 ? " cita" : " citas")
                + (rol == Rol.ODONTOLOGO ? "" : " · " + profesionales
                        + (profesionales == 1 ? " profesional" : " profesionales")));
        if (deHoy.isEmpty()) {
            filasAgenda.getChildren().setAll(new EstadoVacio("No hay citas para hoy",
                    rol == Rol.ODONTOLOGO ? "Tus citas de hoy aparecerán aquí." : "Las citas agendadas para hoy aparecerán aquí."));
            return;
        }
        for (Cita cita : deHoy) {
            filasAgenda.getChildren().add(filaCita(cita));
        }
    }

    /** Hora, avatar, paciente, "motivo · profesional" y estado; un clic abre la cita en la Agenda. */
    private Button filaCita(Cita cita) {
        Label inicio = new Label(Formatos.hora(cita.getHoraInicio()));
        inicio.getStyleClass().add("celda-titulo");
        Label fin = new Label(Formatos.hora(cita.getHoraFin()));
        fin.getStyleClass().add("celda-sub");
        VBox horas = new VBox(inicio, fin);
        horas.getStyleClass().add("fila-agenda-horas");

        Avatar avatar = new Avatar(cita.getPaciente().getIniciales(), cita.getPaciente().getIdPaciente());
        Label paciente = new Label(cita.getPaciente().getNombreCompleto());
        paciente.getStyleClass().add("celda-titulo");
        Label detalle = new Label((cita.getMotivo() == null ? "" : cita.getMotivo() + " · ")
                + cita.getProfesional().getNombreCompleto());
        detalle.getStyleClass().add("celda-sub");
        VBox textos = new VBox(2, paciente, detalle);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox contenido = new HBox(16, horas, avatar, textos, new Pill(cita.getEstado()));
        contenido.setAlignment(Pos.CENTER_LEFT);
        Button fila = new Button(null, contenido);
        fila.getStyleClass().add("fila-agenda-hoy");
        fila.setMaxWidth(Double.MAX_VALUE);
        fila.setAccessibleText(Formatos.hora(cita.getHoraInicio()) + ", " + cita.getPaciente().getNombreCompleto()
                + ", " + cita.getEstado().getEtiqueta());
        fila.setOnAction(e -> MainController.actual().navegar(Pantalla.AGENDA, cita));
        return fila;
    }

    // ===== Ingresos de la semana =====

    /** Barras de lunes a sábado; hoy en el color primario. Solo el Administrador. */
    private void mostrarIngresosSemana() {
        boolean visible = Servicios.reportes().puedeVer();
        alternar(tarjetaIngresos, visible);
        if (!visible) {
            return;
        }
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sabado = lunes.plusDays(5);
        rangoSemana.setText("Lunes " + lunes.getDayOfMonth() + " · sábado " + sabado.getDayOfMonth());
        SortedMap<LocalDate, Double> porDia;
        try {
            porDia = Servicios.reportes().ingresosPorDia(lunes, sabado);
        } catch (RuntimeException e) {
            totalSemana.setText("");
            rangoSemana.setText("No se pudo cargar los ingresos: " + e.getMessage());
            return;
        }
        List<String> etiquetas = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        int resaltada = -1;
        double total = 0;
        for (LocalDate dia : porDia.keySet()) {
            double monto = porDia.get(dia);
            if (dia.equals(hoy)) {
                resaltada = etiquetas.size();
            }
            etiquetas.add(Formatos.diaCorto(dia));
            valores.add(monto);
            // Un día que todavía no llega se muestra con "—"
            textos.add(dia.isAfter(hoy) ? "—" : Formatos.miles(monto));
            total += monto;
        }
        totalSemana.setText(Formatos.dinero(total));
        graficoSemana.mostrar(etiquetas, valores, textos, resaltada);
    }

    // ===== Accesos rápidos =====

    private void construirAccesos() {
        List<Acceso> candidatos = new ArrayList<>();
        if (Servicios.pacientes().puedeEditar()) {
            candidatos.add(new Acceso("Registrar paciente", "pacientes", Pantalla.PACIENTES, Solicitud.NUEVO));
        }
        if (citas.puedeGestionar()) {
            candidatos.add(new Acceso("Agendar cita", "agenda", Pantalla.AGENDA, Solicitud.NUEVO));
        }
        candidatos.add(new Acceso("Registrar pago", "pagos", Pantalla.PAGOS, null));
        candidatos.add(new Acceso("Abrir odontograma", "diente", Pantalla.ODONTOGRAMA, null));
        candidatos.add(new Acceso("Historia clínica", "historia", Pantalla.HISTORIA_CLINICA, null));
        candidatos.add(new Acceso("Tratamientos", "tratamientos", Pantalla.TRATAMIENTOS, null));
        candidatos.add(new Acceso("Ver agenda", "agenda", Pantalla.AGENDA, null));

        int i = 0;
        for (Acceso acceso : candidatos) {
            if (i == 4) {
                break;
            }
            if (!acceso.pantalla().permitidaPara(rol)) {
                continue;
            }
            Label texto = new Label(acceso.texto());
            texto.getStyleClass().add("acceso-texto");
            StackPane cuadro = new StackPane(new Icono(acceso.icono(), 18));
            cuadro.getStyleClass().add("acceso-icono");
            VBox contenido = new VBox(10, cuadro, texto);
            Button boton = new Button(null, contenido);
            boton.getStyleClass().add("acceso-rapido");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setAccessibleText(acceso.texto());
            boton.setOnAction(e -> MainController.actual().navegar(acceso.pantalla(), acceso.parametro()));
            accesos.add(boton, i % 2, i / 2);
            GridPane.setHgrow(boton, Priority.ALWAYS);
            i++;
        }
    }

    // ===== Acciones =====

    @FXML
    private void nuevoPaciente() {
        MainController.actual().navegar(Pantalla.PACIENTES, Solicitud.NUEVO);
    }

    @FXML
    private void nuevaCita() {
        MainController.actual().navegar(Pantalla.AGENDA, Solicitud.NUEVO);
    }

    @FXML
    private void verAgenda() {
        MainController.actual().navegar(Pantalla.AGENDA);
    }

    // ===== Utilidades =====

    /** "Buenos días" antes de las 12, "Buenas tardes" antes de las 19 y "Buenas noches" después. */
    static String saludo(LocalTime hora) {
        if (hora.isBefore(LocalTime.NOON)) {
            return "Buenos días";
        }
        return hora.isBefore(LocalTime.of(19, 0)) ? "Buenas tardes" : "Buenas noches";
    }

    private static String primerNombre(String nombreCompleto) {
        return nombreCompleto == null || nombreCompleto.isBlank() ? "" : nombreCompleto.strip().split("\\s+")[0];
    }

    /** "María Quispe": primer nombre y primer apellido. */
    private static String nombreCorto(String nombres, String apellidos) {
        return primerNombre(nombres) + " " + primerNombre(apellidos);
    }

    /** Un dato que no se pudo cargar no debe impedir ver el resto de Inicio. */
    private static <T> T cargar(Supplier<T> consulta, T siFalla) {
        try {
            return consulta.get();
        } catch (RuntimeException e) {
            return siFalla;
        }
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
