package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml). Cada prueba crea su
 * propio paciente, así los registros de otras pruebas no interfieren.
 */
class OdontogramaYHistoriaDAOSqliteTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 5, 6);
    private static int contador;

    private final OdontogramaDAOSqlite odontograma = new OdontogramaDAOSqlite();
    private final HistoriaClinicaDAOSqlite historia = new HistoriaClinicaDAOSqlite();
    private Paciente paciente;
    private Profesional profesional;

    @BeforeEach
    void preparar() {
        contador++;
        Paciente nuevo = new Paciente();
        nuevo.setNombres("Elena");
        nuevo.setApellidos("Odontograma Sql " + contador);
        nuevo.setDni(String.format("9200%04d", contador));
        paciente = new PacienteDAOSqlite().crear(nuevo);
        profesional = new ProfesionalDAOSqlite().crear(
                new Profesional(0, null, "Prof" + contador, "Odontograma Sql", "Endodoncia", null, null));
    }

    @Test
    void elEstadoActualEsElUltimoRegistroDeCadaPieza() {
        odontograma.registrar(registro(36, EstadoPieza.CARIADA, "Caries profunda"));
        odontograma.registrar(registro(36, EstadoPieza.EN_TRATAMIENTO, null));
        odontograma.registrar(registro(18, EstadoPieza.EXTRAIDA, null));

        List<OdontogramaPieza> actual = odontograma.estadoActual(paciente.getIdPaciente());

        assertEquals(List.of(18, 36), actual.stream().map(OdontogramaPieza::getNumeroPieza).toList());
        assertEquals(EstadoPieza.EN_TRATAMIENTO, actual.get(1).getEstado());
    }

    @Test
    void elHistorialVaDelMasRecienteAlMasAntiguoConSusDatos() {
        odontograma.registrarTodos(List.of(registro(36, EstadoPieza.CARIADA, "Caries profunda"),
                registro(24, EstadoPieza.CARIADA, null)));
        OdontogramaPieza sinProfesional = registro(36, EstadoPieza.OBTURADA, null);
        sinProfesional.setIdProfesional(null);
        odontograma.registrar(sinProfesional);

        List<OdontogramaPieza> historial = odontograma.historialPieza(paciente.getIdPaciente(), 36);

        assertEquals(List.of(EstadoPieza.OBTURADA, EstadoPieza.CARIADA),
                historial.stream().map(OdontogramaPieza::getEstado).toList());
        assertNull(historial.get(0).getIdProfesional());
        assertEquals(profesional.getIdProfesional(), historial.get(1).getIdProfesional());
        assertEquals("Caries profunda", historial.get(1).getObservacion());
        assertEquals(FECHA, historial.get(1).getFechaRegistro());
        assertTrue(historial.get(0).getIdRegistro() > 0);
    }

    @Test
    void siUnRegistroFallaNoSeGuardaNinguno() {
        // La base de datos rechaza la pieza 19 (CHECK de la notación FDI)
        List<OdontogramaPieza> registros = List.of(registro(11, EstadoPieza.CARIADA, null),
                registro(19, EstadoPieza.CARIADA, null));

        assertThrows(RuntimeException.class, () -> odontograma.registrarTodos(registros));

        assertTrue(odontograma.estadoActual(paciente.getIdPaciente()).isEmpty());
        // La conexión compartida vuelve a guardar normalmente
        odontograma.registrar(registro(11, EstadoPieza.CARIADA, null));
        assertEquals(1, odontograma.estadoActual(paciente.getIdPaciente()).size());
    }

    @Test
    void guardaYListaLasEntradasDeHistoriaClinica() {
        historia.crear(new HistoriaClinicaEntrada(0, paciente.getIdPaciente(), null, null, FECHA.minusDays(3),
                "Sin hallazgos", null));
        HistoriaClinicaEntrada creada = historia.crear(new HistoriaClinicaEntrada(0, paciente.getIdPaciente(), null,
                profesional.getIdProfesional(), FECHA, "Se programa endodoncia", "Pulpitis irreversible en 36"));

        List<HistoriaClinicaEntrada> entradas = historia.listarPorPaciente(paciente.getIdPaciente());

        assertEquals(2, entradas.size());
        HistoriaClinicaEntrada primera = entradas.get(0);
        assertEquals(creada.getIdEntrada(), primera.getIdEntrada());
        assertEquals("Pulpitis irreversible en 36", primera.getDiagnostico());
        assertEquals("Se programa endodoncia", primera.getObservaciones());
        assertEquals(profesional.getIdProfesional(), primera.getIdProfesional());
        assertNull(primera.getIdCita());
        assertNull(entradas.get(1).getIdProfesional());
    }

    private OdontogramaPieza registro(int pieza, EstadoPieza estado, String observacion) {
        return new OdontogramaPieza(0, paciente.getIdPaciente(), pieza, estado, FECHA, observacion,
                profesional.getIdProfesional());
    }
}
