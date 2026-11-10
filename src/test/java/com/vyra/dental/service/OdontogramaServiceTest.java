package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.OdontogramaDAOMemoria;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class OdontogramaServiceTest {

    private static final int PACIENTE = 1;
    private static final LocalDate HOY = LocalDate.of(2030, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private final Profesional salinas = new Profesional(7, 3, "Marco", "Salinas", "Endodoncia", null, null);

    private OdontogramaDAOMemoria dao;
    private OdontogramaService servicio;

    @BeforeEach
    void preparar() {
        dao = new OdontogramaDAOMemoria();
        servicio = new OdontogramaService(dao, RELOJ);
        iniciarSesion(Rol.ODONTOLOGO, salinas);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    // Pruebas obligatorias de docs/09-plan-implementacion.md, fase 5

    @Test
    void unaPiezaSinRegistrosEstaSana() {
        Map<Integer, EstadoPieza> estado = servicio.estadoActual(PACIENTE);

        assertEquals(32, estado.size());
        assertTrue(estado.values().stream().allMatch(e -> e == EstadoPieza.SANA));
    }

    @Test
    void elEstadoActualEsElUltimoRegistro() {
        servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.CARIADA), Map.of());
        servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.EN_TRATAMIENTO), Map.of());

        assertEquals(EstadoPieza.EN_TRATAMIENTO, servicio.estadoActual(PACIENTE).get(36));
        assertEquals(EstadoPieza.SANA, servicio.estadoActual(PACIENTE).get(35));
    }

    @Test
    void guardarNoBorraElHistorial() {
        servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.CARIADA), Map.of(36, "Caries profunda"));
        servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.OBTURADA, 24, EstadoPieza.CARIADA), Map.of());

        List<OdontogramaPieza> historial = servicio.historialPieza(PACIENTE, 36);

        assertEquals(List.of(EstadoPieza.OBTURADA, EstadoPieza.CARIADA),
                historial.stream().map(OdontogramaPieza::getEstado).toList());
        assertEquals("Caries profunda", historial.get(1).getObservacion());
        assertEquals(1, servicio.historialPieza(PACIENTE, 24).size());
    }

    // Más reglas

    @Test
    void cadaRegistroLlevaLaFechaDeHoyElProfesionalYLaObservacion() {
        int guardados = servicio.registrarCambios(PACIENTE, Map.of(18, EstadoPieza.EXTRAIDA),
                Map.of(18, "  Extracción por fractura  "));

        OdontogramaPieza registro = servicio.historialPieza(PACIENTE, 18).get(0);
        assertEquals(1, guardados);
        assertEquals(HOY, registro.getFechaRegistro());
        assertEquals(salinas.getIdProfesional(), registro.getIdProfesional());
        assertEquals("Extracción por fractura", registro.getObservacion());
    }

    @Test
    void unaObservacionVaciaSeGuardaComoNull() {
        servicio.registrarCambios(PACIENTE, Map.of(11, EstadoPieza.CARIADA), Map.of(11, "   "));

        assertNull(servicio.historialPieza(PACIENTE, 11).get(0).getObservacion());
    }

    @Test
    void unCambioAlMismoEstadoNoSeRegistra() {
        servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.CARIADA), Map.of());

        int guardados = servicio.registrarCambios(PACIENTE,
                Map.of(36, EstadoPieza.CARIADA, 11, EstadoPieza.SANA), Map.of());

        assertEquals(0, guardados);
        assertEquals(1, servicio.historialPieza(PACIENTE, 36).size());
        assertTrue(servicio.historialPieza(PACIENTE, 11).isEmpty());
    }

    @Test
    void rechazaPiezasFueraDeLaNotacionFdi() {
        for (int pieza : new int[] { 10, 19, 20, 29, 49, 51, 85 }) {
            ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                    () -> servicio.registrarCambios(PACIENTE, Map.of(pieza, EstadoPieza.CARIADA), Map.of()));
            assertTrue(error.getMessage().contains(String.valueOf(pieza)));
        }
    }

    @Test
    void siUnaPiezaEsInvalidaNoSeGuardaNingunCambio() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCambios(PACIENTE,
                Map.of(11, EstadoPieza.CARIADA, 19, EstadoPieza.CARIADA), Map.of()));

        assertTrue(servicio.historialPieza(PACIENTE, 11).isEmpty());
    }

    @Test
    void sinCambiosNoHayNadaQueGuardar() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCambios(PACIENTE, Map.of(), Map.of()));
    }

    @Test
    void elAdministradorYRecepcionSoloConsultan() {
        for (Rol rol : new Rol[] { Rol.ADMIN, Rol.RECEPCION }) {
            iniciarSesion(rol, null);
            assertThrows(ReglaNegocioException.class,
                    () -> servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.CARIADA), Map.of()));
        }
        assertEquals(EstadoPieza.SANA, servicio.estadoActual(PACIENTE).get(36));
    }

    @Test
    void unOdontologoSinProfesionalAsociadoRegistraSinProfesional() {
        iniciarSesion(Rol.ODONTOLOGO, null);

        servicio.registrarCambios(PACIENTE, Map.of(36, EstadoPieza.CARIADA), Map.of());

        assertNull(servicio.historialPieza(PACIENTE, 36).get(0).getIdProfesional());
    }

    @Test
    void nombreDeLasPiezas() {
        assertEquals("Primer molar inferior izquierdo", OdontogramaService.nombrePieza(36));
        assertEquals("Incisivo central superior derecho", OdontogramaService.nombrePieza(11));
        assertEquals("Tercer molar inferior derecho", OdontogramaService.nombrePieza(48));
        assertEquals("Canino superior izquierdo", OdontogramaService.nombrePieza(23));
    }

    @Test
    void lasArcadasTienenLas32Piezas() {
        assertEquals(16, OdontogramaService.PIEZAS_SUPERIORES.size());
        assertEquals(16, OdontogramaService.PIEZAS_INFERIORES.size());
        assertTrue(OdontogramaService.PIEZAS_SUPERIORES.stream().allMatch(OdontogramaService::esPiezaValida));
        assertTrue(OdontogramaService.PIEZAS_INFERIORES.stream().allMatch(OdontogramaService::esPiezaValida));
    }

    private static void iniciarSesion(Rol rol, Profesional profesional) {
        Sesion.get().iniciar(new Usuario(3, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }
}
