package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.PagoDAOMemoria;
import com.vyra.dental.dao.memoria.TratamientoDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.ValidacionException;

class TratamientoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2030, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private final Profesional salinas = new Profesional(1, null, "Marco", "Salinas", "Endodoncia", null, null);
    private final Profesional nunez = new Profesional(2, null, "Patricia", "Núñez", "Ortodoncia", null, null);
    private final Paciente maria = paciente(1, "María", "Quispe Huamán", "72481935");
    private final Paciente jorge = paciente(2, "Jorge", "Mamani", "45812307");

    private CitaDAOMemoria citaDAO;
    private TratamientoDAOMemoria tratamientoDAO;
    private TratamientoService servicio;

    @BeforeEach
    void preparar() {
        citaDAO = new CitaDAOMemoria();
        tratamientoDAO = new TratamientoDAOMemoria(citaDAO, new PagoDAOMemoria());
        servicio = new TratamientoService(tratamientoDAO, citaDAO, RELOJ);
        iniciarSesion(Rol.ODONTOLOGO, salinas);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void registraEnLaCitaConSuFechaYPiezasOrdenadasSinRepetir() {
        Cita cita = cita(maria, salinas, HOY.minusDays(3), EstadoCita.ATENDIDA);

        Tratamiento creado = servicio.registrar(tratamiento(cita, "  Endodoncia pieza 36 ", 650,
                new ArrayList<>(List.of(47, 36, 47))));

        Tratamiento guardado = tratamientoDAO.buscarPorId(creado.getIdTratamiento()).orElseThrow();
        assertEquals("Endodoncia pieza 36", guardado.getDescripcion());
        assertEquals(HOY.minusDays(3), guardado.getFecha());
        assertEquals(EstadoTratamiento.EN_CURSO, guardado.getEstado());
        assertEquals(List.of(36, 47), guardado.getPiezasDentales());
    }

    @Test
    void rechazaPiezasFueraDeLaNotacionFdi() {
        Cita cita = cita(maria, salinas, HOY, EstadoCita.CONFIRMADA);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(tratamiento(cita, "Resina", 100, List.of(36, 19))));

        assertEquals("piezasDentales", error.getCampo().orElseThrow());
        assertEquals(0, tratamientoDAO.listarTodos().size());
    }

    @Test
    void elCostoDebeSerMayorACero() {
        Cita cita = cita(maria, salinas, HOY, EstadoCita.CONFIRMADA);

        ValidacionException error = assertThrows(ValidacionException.class,
                () -> servicio.registrar(tratamiento(cita, "Resina", 0, List.of())));

        assertEquals("El costo debe ser mayor a 0", error.getErrores().get("costo"));
    }

    @Test
    void laCitaNoPuedeFaltarNiEstarCanceladaNiSerFuturaNiDeOtroOdontologo() {
        Cita cancelada = cita(maria, salinas, HOY, EstadoCita.CANCELADA);
        Cita futura = cita(maria, salinas, HOY.plusDays(1), EstadoCita.PENDIENTE);
        Cita deNunez = cita(maria, nunez, HOY, EstadoCita.CONFIRMADA);
        Tratamiento sinCita = tratamiento(cancelada, "Resina", 100, List.of());
        sinCita.setIdCita(0);

        for (Tratamiento invalido : List.of(sinCita, tratamiento(cancelada, "Resina", 100, List.of()),
                tratamiento(futura, "Resina", 100, List.of()), tratamiento(deNunez, "Resina", 100, List.of()))) {
            ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(invalido));
            assertEquals("idCita", error.getCampo().orElseThrow());
        }
        assertEquals(0, tratamientoDAO.listarTodos().size());
    }

    @Test
    void soloElOdontologoRegistraYFinaliza() {
        Cita cita = cita(maria, salinas, HOY, EstadoCita.CONFIRMADA);
        Tratamiento creado = servicio.registrar(tratamiento(cita, "Resina", 100, List.of(14)));

        for (Rol rol : new Rol[] { Rol.ADMIN, Rol.RECEPCION }) {
            iniciarSesion(rol, null);
            assertThrows(ReglaNegocioException.class,
                    () -> servicio.registrar(tratamiento(cita, "Resina", 100, List.of())));
            assertThrows(ReglaNegocioException.class, () -> servicio.finalizar(creado.getIdTratamiento()));
        }
    }

    @Test
    void finalizarCambiaElEstadoUnaSolaVez() {
        Cita cita = cita(maria, salinas, HOY, EstadoCita.CONFIRMADA);
        Tratamiento creado = servicio.registrar(tratamiento(cita, "Resina", 100, List.of(14)));

        servicio.finalizar(creado.getIdTratamiento());

        assertEquals(EstadoTratamiento.FINALIZADO,
                tratamientoDAO.buscarPorId(creado.getIdTratamiento()).orElseThrow().getEstado());
        assertThrows(ReglaNegocioException.class, () -> servicio.finalizar(creado.getIdTratamiento()));
    }

    @Test
    void citasParaRegistrarSonLasDeHoyYLasAtendidasRecientesDelOdontologo() {
        Cita hoy = cita(maria, salinas, HOY, EstadoCita.PENDIENTE);
        Cita atendida = cita(jorge, salinas, HOY.minusDays(5), EstadoCita.ATENDIDA);
        cita(jorge, salinas, HOY, EstadoCita.CANCELADA);
        cita(maria, salinas, HOY.minusDays(5), EstadoCita.CONFIRMADA);
        cita(maria, salinas, HOY.minusDays(TratamientoService.DIAS_CITAS_RECIENTES + 1), EstadoCita.ATENDIDA);
        cita(maria, salinas, HOY.plusDays(1), EstadoCita.CONFIRMADA);
        Cita deNunez = cita(jorge, nunez, HOY, EstadoCita.CONFIRMADA);

        assertEquals(List.of(hoy.getIdCita(), atendida.getIdCita()),
                servicio.citasParaRegistrar().stream().map(Cita::getIdCita).toList());

        iniciarSesion(Rol.ODONTOLOGO, null);
        // Sin profesional asociado ve todas (las dos de hoy son a la misma hora: quedan en el orden en que se crearon)
        assertEquals(List.of(hoy.getIdCita(), deNunez.getIdCita(), atendida.getIdCita()),
                servicio.citasParaRegistrar().stream().map(Cita::getIdCita).toList());
    }

    @Test
    void buscaPorPacienteSinTildesYPorEstado() {
        Cita deMaria = cita(maria, salinas, HOY, EstadoCita.CONFIRMADA);
        Cita deJorge = cita(jorge, salinas, HOY, EstadoCita.CONFIRMADA);
        Tratamiento endodoncia = servicio.registrar(tratamiento(deMaria, "Endodoncia", 650, List.of(36)));
        servicio.registrar(tratamiento(deJorge, "Limpieza", 120, List.of()));
        servicio.finalizar(endodoncia.getIdTratamiento());

        assertEquals(List.of("Endodoncia"), descripciones(servicio.buscar("huaman", null)));
        assertEquals(List.of("Limpieza"), descripciones(servicio.buscar("", EstadoTratamiento.EN_CURSO)));
        assertEquals(List.of("Endodoncia"), descripciones(servicio.dePaciente(maria.getIdPaciente())));
        assertEquals("Quispe Huamán", servicio.resumen(endodoncia.getIdTratamiento()).orElseThrow()
                .getPaciente().getApellidos());
    }

    @Test
    void leePiezasEscritas() {
        assertEquals(List.of(36, 47), TratamientoService.leerPiezas(" 47, 36 ,47 "));
        assertEquals(List.of(11, 21), TratamientoService.leerPiezas("21 11"));
        assertEquals(List.of(), TratamientoService.leerPiezas("  "));
        assertEquals("piezasDentales", assertThrows(ReglaNegocioException.class,
                () -> TratamientoService.leerPiezas("36, abc")).getCampo().orElseThrow());
        assertEquals("piezasDentales", assertThrows(ReglaNegocioException.class,
                () -> TratamientoService.leerPiezas("55")).getCampo().orElseThrow());
    }

    private static List<String> descripciones(List<TratamientoResumen> resumenes) {
        return resumenes.stream().map(r -> r.getTratamiento().getDescripcion()).toList();
    }

    private Cita cita(Paciente paciente, Profesional profesional, LocalDate fecha, EstadoCita estado) {
        return citaDAO.crear(new Cita(0, paciente, profesional, fecha, LocalTime.of(9, 0), LocalTime.of(9, 30),
                "Control", estado));
    }

    private static Tratamiento tratamiento(Cita cita, String descripcion, double costo, List<Integer> piezas) {
        return new Tratamiento(0, cita.getIdCita(), descripcion, costo, null, null, piezas);
    }

    private static Paciente paciente(int id, String nombres, String apellidos, String dni) {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(id);
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setDni(dni);
        return paciente;
    }

    private static void iniciarSesion(Rol rol, Profesional profesional) {
        Sesion.get().iniciar(new Usuario(3, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }
}
