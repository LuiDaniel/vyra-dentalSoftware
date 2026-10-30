package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.HistoriaClinicaDAOMemoria;
import com.vyra.dental.dao.memoria.OdontogramaDAOMemoria;
import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class ExportacionPdfServiceTest {

    private HistoriaClinicaService historia;
    private OdontogramaService odontograma;
    private ExportacionPdfService servicio;
    private Paciente maria;

    @BeforeEach
    void preparar() {
        PacienteDAOMemoria pacienteDAO = new PacienteDAOMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneId.of("America/Lima"));
        historia = new HistoriaClinicaService(new HistoriaClinicaDAOMemoria(), pacienteDAO, new CitaDAOMemoria(), reloj);
        odontograma = new OdontogramaService(new OdontogramaDAOMemoria(), pacienteDAO, reloj);
        servicio = new ExportacionPdfService(historia, odontograma);
        maria = pacienteDAO.crear(new Paciente(0, "María", "Quispe Huamán", "72481935", LocalDate.of(1990, 1, 1),
                "987654321", null, null, "Penicilina", "Hipertensión controlada", true, LocalDate.of(2026, 1, 1)));
        iniciarSesionComo(Rol.ODONTOLOGO);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void laHistoriaIncluyeDatosAlergiasYEntradas() throws IOException {
        HistoriaClinicaEntrada entrada = new HistoriaClinicaEntrada();
        entrada.setIdPaciente(maria.getIdPaciente());
        entrada.setDiagnostico("Caries profunda en pieza 36");
        entrada.setObservaciones("Se indicó endodoncia");
        historia.registrarEntrada(entrada);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        servicio.exportarHistoria(maria, salida);
        String texto = textoDe(salida);

        assertTrue(salida.toString("ISO-8859-1").startsWith("%PDF"));
        assertTrue(texto.contains("Historia clínica"), texto);
        assertTrue(texto.contains("María Quispe Huamán"), texto);
        assertTrue(texto.contains("72481935"), texto);
        assertTrue(texto.contains("Penicilina"), texto);
        assertTrue(texto.contains("Hipertensión controlada"), texto);
        assertTrue(texto.contains("Caries profunda en pieza 36"), texto);
        assertTrue(texto.contains("Se indicó endodoncia"), texto);
    }

    @Test
    void laHistoriaSinEntradasLoDice() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        servicio.exportarHistoria(maria, salida);

        assertTrue(textoDe(salida).contains("Sin entradas registradas"));
    }

    @Test
    void elOdontogramaMuestraLasPiezasYSusCambios() throws IOException {
        odontograma.registrarCambios(maria.getIdPaciente(),
                Map.of(36, EstadoPieza.EN_TRATAMIENTO, 18, EstadoPieza.EXTRAIDA), "Endodoncia en curso");

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        servicio.exportarOdontograma(maria, salida);
        String texto = textoDe(salida);

        assertTrue(texto.contains("Odontograma"), texto);
        assertTrue(texto.contains("Pieza 36"), texto);
        assertTrue(texto.contains("En tratamiento"), texto);
        assertTrue(texto.contains("Endodoncia en curso"), texto);
        assertTrue(texto.contains("Pieza 18"), texto);
        assertTrue(texto.contains("Extraída"), texto);
    }

    @Test
    void elOdontogramaSinCambiosDiceQueTodoEstaSano() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        servicio.exportarOdontograma(maria, salida);

        assertTrue(textoDe(salida).contains("Todas las piezas figuran como sanas"));
    }

    @Test
    void elAdministradorTambienPuedeExportar() {
        iniciarSesionComo(Rol.ADMIN);

        servicio.exportarHistoria(maria, new ByteArrayOutputStream());
    }

    @Test
    void recepcionNoPuedeExportar() {
        iniciarSesionComo(Rol.RECEPCION);

        assertThrows(ReglaNegocioException.class, () -> servicio.exportarHistoria(maria, new ByteArrayOutputStream()));
        assertThrows(ReglaNegocioException.class, () -> servicio.exportarOdontograma(maria, new ByteArrayOutputStream()));
    }

    @Test
    void elNombreDeArchivoUsaElDni() {
        assertEquals("odontograma-72481935.pdf", ExportacionPdfService.nombreDeArchivo("odontograma", maria));
    }

    private static String textoDe(ByteArrayOutputStream pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf.toByteArray());
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        lector.close();
        return texto.toString();
    }

    private static void iniciarSesionComo(Rol rol) {
        Sesion.get().iniciar(new Usuario(1, "usuario", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), null);
    }
}
