package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.OdontogramaDAOMemoria;
import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class OdontogramaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private OdontogramaDAOMemoria odontogramaDAO;
    private PacienteDAOMemoria pacienteDAO;
    private OdontogramaService servicio;
    private Paciente maria;

    @BeforeEach
    void preparar() {
        odontogramaDAO = new OdontogramaDAOMemoria();
        pacienteDAO = new PacienteDAOMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZONA);
        servicio = new OdontogramaService(odontogramaDAO, pacienteDAO, reloj);
        maria = pacienteDAO.crear(new Paciente(0, "María", "Quispe", "72481935", LocalDate.of(1990, 1, 1), null, null,
                null, null, null, true, LocalDate.of(2026, 1, 1)));
        iniciarSesionComo(Rol.ODONTOLOGO);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void unaPiezaSinRegistrosEsSana() {
        Map<Integer, EstadoPieza> estados = servicio.estadoActual(maria.getIdPaciente());

        assertTrue(estados.isEmpty());
        assertEquals(EstadoPieza.SANA, estados.getOrDefault(36, EstadoPieza.SANA));
    }

    @Test
    void elEstadoActualEsElUltimoRegistro() {
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.CARIADA), null);
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.OBTURADA), "Resina");

        assertEquals(EstadoPieza.OBTURADA, servicio.estadoActual(maria.getIdPaciente()).get(36));
    }

    @Test
    void guardarNoBorraElHistorial() {
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.CARIADA), null);
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.OBTURADA), "Resina");

        List<OdontogramaPieza> historial = servicio.historialPieza(maria.getIdPaciente(), 36);

        assertEquals(2, historial.size());
        assertEquals(EstadoPieza.OBTURADA, historial.get(0).getEstado());
        assertEquals("Resina", historial.get(0).getObservacion());
        assertEquals(EstadoPieza.CARIADA, historial.get(1).getEstado());
        assertEquals(LocalDate.of(2026, 10, 29), historial.get(1).getFechaRegistro());
    }

    @Test
    void soloRegistraLasPiezasQueCambiaron() {
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.CARIADA), null);

        int cambiadas = servicio.registrarCambios(maria.getIdPaciente(),
                Map.of(36, EstadoPieza.CARIADA, 11, EstadoPieza.SANA, 18, EstadoPieza.EXTRAIDA), null);

        assertEquals(1, cambiadas);
        assertEquals(1, servicio.historialPieza(maria.getIdPaciente(), 36).size());
        assertTrue(servicio.historialPieza(maria.getIdPaciente(), 11).isEmpty());
    }

    @Test
    void volverASanaQuedaRegistrado() {
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(46, EstadoPieza.EN_TRATAMIENTO), null);
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(46, EstadoPieza.SANA), null);

        assertEquals(EstadoPieza.SANA, servicio.estadoActual(maria.getIdPaciente()).get(46));
        assertEquals(2, servicio.historialPieza(maria.getIdPaciente(), 46).size());
    }

    @Test
    void laObservacionVaciaSeGuardaComoNull() {
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(21, EstadoPieza.CARIADA), "   ");

        assertNull(servicio.historialPieza(maria.getIdPaciente(), 21).get(0).getObservacion());
    }

    @Test
    void rechazaPiezasQueNoExisten() {
        for (int pieza : new int[] {10, 19, 20, 29, 49, 51}) {
            assertThrows(RuntimeException.class,
                    () -> servicio.registrarCambios(maria.getIdPaciente(), Map.of(pieza, EstadoPieza.CARIADA), null),
                    "pieza " + pieza);
        }
        assertTrue(servicio.estadoActual(maria.getIdPaciente()).isEmpty());
    }

    @Test
    void siUnaPiezaEsInvalidaNoGuardaNinguna() {
        assertThrows(RuntimeException.class, () -> servicio.registrarCambios(maria.getIdPaciente(),
                Map.of(11, EstadoPieza.CARIADA, 19, EstadoPieza.CARIADA), null));

        assertTrue(servicio.estadoActual(maria.getIdPaciente()).isEmpty());
    }

    @Test
    void noRegistraParaUnPacienteDadoDeBaja() {
        maria.setActivo(false);
        pacienteDAO.actualizar(maria);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.CARIADA), null));
    }

    @Test
    void elAdministradorSoloConsulta() {
        servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.CARIADA), null);
        iniciarSesionComo(Rol.ADMIN);

        assertEquals(EstadoPieza.CARIADA, servicio.estadoActual(maria.getIdPaciente()).get(36));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.OBTURADA), null));
    }

    @Test
    void recepcionNoTieneAcceso() {
        iniciarSesionComo(Rol.RECEPCION);

        assertThrows(ReglaNegocioException.class, () -> servicio.estadoActual(maria.getIdPaciente()));
        assertThrows(ReglaNegocioException.class, () -> servicio.historialPieza(maria.getIdPaciente(), 36));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarCambios(maria.getIdPaciente(), Map.of(36, EstadoPieza.CARIADA), null));
    }

    private static void iniciarSesionComo(Rol rol) {
        Sesion.get().iniciar(new Usuario(1, "usuario", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), null);
    }
}
