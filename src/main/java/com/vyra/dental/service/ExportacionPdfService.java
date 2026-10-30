package com.vyra.dental.service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.util.Formatos;

/**
 * Exporta la historia clínica y el odontograma de un paciente a PDF listo para imprimir
 * (RF-11, CU-11). Pueden usarlo quienes ven esas pantallas: odontólogo y administrador. No usa
 * JavaFX; los controladores lo llaman dentro de un {@code Task}.
 */
public class ExportacionPdfService {

    private static final int[] SUPERIOR = {18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28};
    private static final int[] INFERIOR = {48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38};

    private static final Color TINTA = new Color(0x0F, 0x17, 0x2A);
    private static final Color SECUNDARIO = new Color(0x47, 0x55, 0x69);
    private static final Color PRIMARIO = new Color(0x0F, 0x76, 0x6E);
    private static final Color BORDE = new Color(0xCB, 0xD5, 0xE1);

    private final HistoriaClinicaService historia;
    private final OdontogramaService odontograma;

    public ExportacionPdfService(HistoriaClinicaService historia, OdontogramaService odontograma) {
        this.historia = historia;
        this.odontograma = odontograma;
    }

    public boolean puedeExportar() {
        return historia.puedeVer() && odontograma.puedeVer();
    }

    /** Nombre sugerido para el archivo, sin caracteres raros: "historia-clinica-72481935.pdf". */
    public static String nombreDeArchivo(String tipo, Paciente paciente) {
        return tipo + "-" + paciente.getDni() + ".pdf";
    }

    /** Datos del paciente, alergias y antecedentes, y todas las entradas de la historia (de la más reciente). */
    public void exportarHistoria(Paciente paciente, OutputStream salida) {
        verificarAcceso();
        List<HistoriaClinicaEntrada> entradas = historia.entradasDe(paciente.getIdPaciente());
        escribir(salida, "Historia clínica", paciente, documento -> {
            documento.add(titulo("Alergias y antecedentes"));
            documento.add(par("Alergias", paciente.tieneAlergias() ? paciente.getAlergias() : "Ninguna registrada"));
            documento.add(par("Antecedentes", paciente.getAntecedentes() == null || paciente.getAntecedentes().isBlank()
                    ? "Ninguno registrado" : paciente.getAntecedentes()));
            documento.add(titulo("Historial (" + entradas.size() + (entradas.size() == 1 ? " entrada)" : " entradas)")));
            if (entradas.isEmpty()) {
                documento.add(new Paragraph("Sin entradas registradas.", fuente(10, Font.ITALIC, SECUNDARIO)));
            }
            for (HistoriaClinicaEntrada entrada : entradas) {
                documento.add(entrada(entrada));
            }
        });
    }

    /** Arcada con el estado actual de las 32 piezas y el detalle de las que no están sanas. */
    public void exportarOdontograma(Paciente paciente, OutputStream salida) {
        verificarAcceso();
        Map<Integer, EstadoPieza> estados = odontograma.estadoActual(paciente.getIdPaciente());
        escribir(salida, "Odontograma", paciente, documento -> {
            documento.add(titulo("Estado actual (notación FDI)"));
            documento.add(arcada("Maxilar superior", SUPERIOR, estados));
            documento.add(arcada("Mandíbula inferior", INFERIOR, estados));
            documento.add(leyenda());
            documento.add(titulo("Piezas con cambios"));
            boolean alguna = false;
            for (int[] fila : new int[][] {SUPERIOR, INFERIOR}) {
                for (int pieza : fila) {
                    EstadoPieza estado = estados.getOrDefault(pieza, EstadoPieza.SANA);
                    if (estado == EstadoPieza.SANA && odontograma.historialPieza(paciente.getIdPaciente(), pieza).isEmpty()) {
                        continue;
                    }
                    alguna = true;
                    documento.add(detallePieza(paciente, pieza, estado));
                }
            }
            if (!alguna) {
                documento.add(new Paragraph("Todas las piezas figuran como sanas.", fuente(10, Font.ITALIC, SECUNDARIO)));
            }
        });
    }

    // ===== Estructura común =====

    @FunctionalInterface
    private interface Contenido {
        void agregar(Document documento) throws DocumentException;
    }

    private void escribir(OutputStream salida, String titulo, Paciente paciente, Contenido contenido) {
        Document documento = new Document(PageSize.A4, 40, 40, 40, 50);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle(titulo + " - " + paciente.getNombreCompleto());
            documento.addAuthor("Vyra · Consultorio dental");
            documento.open();
            documento.add(encabezado(titulo));
            documento.add(datosDelPaciente(paciente));
            contenido.agregar(documento);
            documento.add(new Paragraph("Generado el " + Formatos.fechaCorta(LocalDate.now())
                    + " · Documento confidencial de uso clínico.", fuente(8, Font.NORMAL, SECUNDARIO)));
        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el PDF", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        try {
            salida.flush();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el PDF", e);
        }
    }

    private static Element encabezado(String titulo) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[] {3, 2});
        tabla.setWidthPercentage(100);
        PdfPCell izquierda = new PdfPCell(new Phrase("Vyra", fuente(20, Font.BOLD, PRIMARIO)));
        izquierda.setBorder(Rectangle.NO_BORDER);
        PdfPCell derecha = new PdfPCell(new Phrase(titulo, fuente(16, Font.BOLD, TINTA)));
        derecha.setBorder(Rectangle.NO_BORDER);
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(izquierda);
        tabla.addCell(derecha);
        tabla.setSpacingAfter(10);
        return tabla;
    }

    private static Element datosDelPaciente(Paciente paciente) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[] {1, 1});
        tabla.setWidthPercentage(100);
        tabla.addCell(celdaDato("Paciente", paciente.getNombreCompleto()));
        tabla.addCell(celdaDato("DNI", paciente.getDni()));
        tabla.addCell(celdaDato("Edad", paciente.getFechaNacimiento() == null ? "—"
                : Formatos.edad(paciente.getFechaNacimiento())));
        tabla.addCell(celdaDato("Teléfono", paciente.getTelefono() == null ? "—"
                : Formatos.telefono(paciente.getTelefono())));
        tabla.setSpacingAfter(8);
        return tabla;
    }

    private static PdfPCell celdaDato(String etiqueta, String valor) {
        Phrase frase = new Phrase();
        frase.add(new com.lowagie.text.Chunk(etiqueta + ": ", fuente(10, Font.BOLD, SECUNDARIO)));
        frase.add(new com.lowagie.text.Chunk(valor, fuente(10, Font.NORMAL, TINTA)));
        PdfPCell celda = new PdfPCell(frase);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(BORDE);
        celda.setPadding(5);
        return celda;
    }

    private static Paragraph titulo(String texto) {
        Paragraph p = new Paragraph(texto.toUpperCase(Formatos.ES_PE), fuente(10, Font.BOLD, PRIMARIO));
        p.setSpacingBefore(12);
        p.setSpacingAfter(5);
        return p;
    }

    private static Paragraph par(String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(etiqueta + ": ", fuente(10, Font.BOLD, SECUNDARIO)));
        p.add(new com.lowagie.text.Chunk(valor, fuente(10, Font.NORMAL, TINTA)));
        p.setSpacingAfter(3);
        return p;
    }

    // ===== Historia =====

    private Element entrada(HistoriaClinicaEntrada entrada) throws DocumentException {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setKeepTogether(true);
        StringBuilder cabecera = new StringBuilder(Formatos.fechaCorta(entrada.getFecha()));
        historia.citaDe(entrada).ifPresent(c -> cabecera.append(" · ").append(descripcionDeCita(c)));
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(BORDE);
        celda.setPadding(8);
        celda.addElement(new Paragraph(cabecera.toString(), fuente(9, Font.NORMAL, SECUNDARIO)));
        if (entrada.getDiagnostico() != null) {
            celda.addElement(new Paragraph(entrada.getDiagnostico(), fuente(11, Font.BOLD, TINTA)));
        }
        if (entrada.getObservaciones() != null) {
            celda.addElement(new Paragraph(entrada.getObservaciones(), fuente(10, Font.NORMAL, TINTA)));
        }
        tabla.addCell(celda);
        tabla.setSpacingAfter(6);
        return tabla;
    }

    private static String descripcionDeCita(Cita cita) {
        return cita.getProfesional().getNombreCompleto() + (cita.getMotivo() == null ? "" : " · " + cita.getMotivo());
    }

    // ===== Odontograma =====

    private static Element arcada(String nombre, int[] piezas, Map<Integer, EstadoPieza> estados)
            throws DocumentException {
        PdfPTable tabla = new PdfPTable(piezas.length);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(4);
        for (int pieza : piezas) {
            EstadoPieza estado = estados.getOrDefault(pieza, EstadoPieza.SANA);
            Paragraph numero = new Paragraph(String.valueOf(pieza), fuente(8, Font.BOLD, TINTA));
            numero.setAlignment(Element.ALIGN_CENTER);
            Paragraph letra = new Paragraph(estado.getCodigo().isEmpty() ? " " : estado.getCodigo(),
                    fuente(11, Font.BOLD, colorDeBorde(estado)));
            letra.setAlignment(Element.ALIGN_CENTER);
            PdfPCell celda = new PdfPCell();
            celda.addElement(numero);
            celda.addElement(letra);
            celda.setBackgroundColor(colorDeRelleno(estado));
            celda.setBorderColor(colorDeBorde(estado));
            celda.setPadding(3);
            tabla.addCell(celda);
        }
        Paragraph titulo = new Paragraph(nombre, fuente(9, Font.BOLD, SECUNDARIO));
        PdfPTable contenedor = new PdfPTable(1);
        contenedor.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.addElement(titulo);
        celda.addElement(tabla);
        contenedor.addCell(celda);
        return contenedor;
    }

    private static Element leyenda() {
        Paragraph p = new Paragraph("Leyenda:  C = Cariada   O = Obturada   T = En tratamiento   X = Extraída   "
                + "(sin letra = Sana)", fuente(8, Font.NORMAL, SECUNDARIO));
        p.setSpacingBefore(6);
        return p;
    }

    private Element detallePieza(Paciente paciente, int pieza, EstadoPieza estado) {
        List<OdontogramaPieza> registros = odontograma.historialPieza(paciente.getIdPaciente(), pieza);
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk("Pieza " + pieza + " · " + estado.getEtiqueta(), fuente(10, Font.BOLD, TINTA)));
        if (!registros.isEmpty()) {
            OdontogramaPieza ultimo = registros.get(0);
            p.add(new com.lowagie.text.Chunk("  (" + Formatos.fechaCorta(ultimo.getFechaRegistro()) + ")",
                    fuente(9, Font.NORMAL, SECUNDARIO)));
            if (ultimo.getObservacion() != null) {
                p.add(new com.lowagie.text.Chunk(" — " + ultimo.getObservacion(), fuente(9, Font.NORMAL, TINTA)));
            }
            if (registros.size() > 1) {
                p.add(new com.lowagie.text.Chunk("  · " + registros.size() + " registros", fuente(8, Font.NORMAL,
                        SECUNDARIO)));
            }
        }
        p.setSpacingAfter(2);
        return p;
    }

    private static Color colorDeRelleno(EstadoPieza estado) {
        return switch (estado) {
            case SANA -> Color.WHITE;
            case CARIADA -> new Color(0xFC, 0xA5, 0xA5);
            case OBTURADA -> new Color(0x93, 0xC5, 0xFD);
            case EN_TRATAMIENTO -> new Color(0xFC, 0xD3, 0x4D);
            case EXTRAIDA -> new Color(0xF1, 0xF5, 0xF9);
        };
    }

    private static Color colorDeBorde(EstadoPieza estado) {
        return switch (estado) {
            case SANA, EXTRAIDA -> new Color(0x64, 0x74, 0x8B);
            case CARIADA -> new Color(0xB9, 0x1C, 0x1C);
            case OBTURADA -> new Color(0x1D, 0x4E, 0xD8);
            case EN_TRATAMIENTO -> new Color(0xB4, 0x53, 0x09);
        };
    }

    private static Font fuente(float tamano, int estilo, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamano, estilo, color);
    }

    private void verificarAcceso() {
        if (!puedeExportar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }
}
