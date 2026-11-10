package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.HistoriaClinicaDAOMemoria;
import com.vyra.dental.dao.memoria.OdontogramaDAOMemoria;
import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.dao.memoria.PagoDAOMemoria;
import com.vyra.dental.dao.memoria.ProfesionalDAOMemoria;
import com.vyra.dental.dao.memoria.TratamientoDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.Usuario;

/** Los PDF se generan y traen los datos esperados (se lee su texto con OpenPDF). */
class PdfServiceTest {

    private static final LocalDate HOY = LocalDate.of(2025, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(16, 30).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private PdfService servicio;
    private Paciente maria;
    private Profesional salinas;

    @BeforeEach
    void preparar() {
        PacienteDAOMemoria pacienteDAO = new PacienteDAOMemoria();
        ProfesionalDAOMemoria profesionalDAO = new ProfesionalDAOMemoria();
        CitaDAOMemoria citaDAO = new CitaDAOMemoria();
        PagoDAOMemoria pagoDAO = new PagoDAOMemoria();
        TratamientoDAOMemoria tratamientoDAO = new TratamientoDAOMemoria(citaDAO, pagoDAO);
        HistoriaClinicaDAOMemoria historiaDAO = new HistoriaClinicaDAOMemoria();
        OdontogramaDAOMemoria odontogramaDAO = new OdontogramaDAOMemoria();

        Paciente nueva = new Paciente();
        nueva.setNombres("María Fernanda");
        nueva.setApellidos("Quispe Huamán");
        nueva.setDni("72481935");
        nueva.setAlergias("Penicilina");
        maria = pacienteDAO.crear(nueva);
        salinas = profesionalDAO.crear(new Profesional(0, null, "Marco", "Salinas Vega", "Endodoncia", null, null));

        historiaDAO.crear(new HistoriaClinicaEntrada(0, maria.getIdPaciente(), null, salinas.getIdProfesional(),
                HOY.minusDays(3), "Se inicia endodoncia", "Pulpitis irreversible en pieza 36"));
        odontogramaDAO.registrarTodos(List.of(
                new OdontogramaPieza(0, maria.getIdPaciente(), 36, EstadoPieza.CARIADA, HOY.minusDays(30), "Caries profunda",
                        salinas.getIdProfesional()),
                new OdontogramaPieza(0, maria.getIdPaciente(), 36, EstadoPieza.EN_TRATAMIENTO, HOY.minusDays(3), null,
                        salinas.getIdProfesional()),
                new OdontogramaPieza(0, maria.getIdPaciente(), 18, EstadoPieza.EXTRAIDA, HOY.minusDays(30), null, null)));
        Cita cita = citaDAO.crear(new Cita(0, maria, salinas, HOY.minusDays(3), LocalTime.of(9, 0), LocalTime.of(10, 0),
                "Endodoncia", EstadoCita.ATENDIDA));
        Tratamiento endodoncia = tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Endodoncia pieza 36", 650,
                EstadoTratamiento.EN_CURSO, HOY.minusDays(3), List.of(36)));
        pagoDAO.crear(new Pago(0, endodoncia.getIdTratamiento(), 250, HOY.minusDays(3), MetodoPago.YAPE_PLIN, null));

        HistoriaClinicaService historia = new HistoriaClinicaService(historiaDAO, pacienteDAO, citaDAO, RELOJ);
        OdontogramaService odontograma = new OdontogramaService(odontogramaDAO, RELOJ);
        ReporteService reportes = new ReporteService(citaDAO, pagoDAO, tratamientoDAO, RELOJ);
        PagoService pagos = new PagoService(pagoDAO, tratamientoDAO, RELOJ);
        servicio = new PdfService(pacienteDAO, profesionalDAO, historia, odontograma, reportes, pagos, RELOJ);
        iniciarSesion(Rol.ADMIN);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void historiaClinicaConElPacienteSusAlergiasYSusEntradas() {
        String texto = texto(salida -> servicio.historiaClinica(maria.getIdPaciente(), salida));

        assertTrue(texto.contains("Historia clínica"), texto);
        assertTrue(texto.contains("María Fernanda Quispe Huamán"), texto);
        assertTrue(texto.contains("Penicilina"), texto);
        assertTrue(texto.contains("Pulpitis irreversible"), texto);
        assertTrue(texto.contains("Marco Salinas Vega"), texto);
        assertTrue(texto.contains("Generado el 15 ene 2025 a las 16:30 por Usuario de prueba"), texto);
    }

    @Test
    void odontogramaConElEstadoActualYElHistorial() {
        String texto = texto(salida -> servicio.odontograma(maria.getIdPaciente(), salida));

        assertTrue(texto.contains("Primer molar inferior izquierdo"), texto);
        assertTrue(texto.contains("En tratamiento"), texto);
        assertTrue(texto.contains("Caries profunda"), texto);
        assertTrue(texto.contains("Cariada (0)"), texto);
        assertTrue(texto.contains("Sana (30)"), texto);
    }

    @Test
    void reportesEIngresos() {
        String citas = texto(salida -> servicio.reporteCitas(HOY.minusDays(7), HOY, null, salida));
        String historial = texto(salida -> servicio.reporteHistorial(maria.getIdPaciente(), salida));
        String ingresos = texto(salida -> servicio.reporteIngresos(HOY.minusDays(7), HOY, salida));

        assertTrue(citas.contains("Todos los profesionales"), citas);
        assertTrue(citas.contains("100%"), citas);
        assertTrue(historial.contains("S/ 400.00"), historial);
        assertTrue(ingresos.contains("Yape / Plin"), ingresos);
        assertTrue(ingresos.contains("S/ 250.00"), ingresos);
    }

    @Test
    void respetaLosPermisos() {
        iniciarSesion(Rol.RECEPCION);
        assertThrows(ReglaNegocioException.class,
                () -> servicio.historiaClinica(maria.getIdPaciente(), new ByteArrayOutputStream()));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.reporteIngresos(HOY, HOY, new ByteArrayOutputStream()));

        iniciarSesion(Rol.ODONTOLOGO);
        assertThrows(ReglaNegocioException.class, () -> servicio.pagos(
                new PagoService.Rango(HOY, HOY), "hoy", new ByteArrayOutputStream()));
    }

    @Test
    void nombreDeArchivoSinTildesNiEspacios() {
        assertEquals("historia-clinica-72481935-2025-01-15.pdf", servicio.nombreArchivo("Historia clínica", "72481935"));
        assertEquals("reporte-de-ingresos-2025-01-15.pdf", servicio.nombreArchivo("Reporte de ingresos", null));
    }

    private static String texto(Consumer<ByteArrayOutputStream> generar) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        generar.accept(salida);
        byte[] bytes = salida.toByteArray();
        assertTrue(new String(bytes, 0, 5).startsWith("%PDF"));
        try (PdfReader lector = new PdfReader(bytes)) {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return texto.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void iniciarSesion(Rol rol) {
        Sesion.get().iniciar(new Usuario(1, "prueba", "Usuario de prueba", "x", rol, true), null);
    }
}
