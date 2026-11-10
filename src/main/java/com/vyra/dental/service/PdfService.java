package com.vyra.dental.service;

import java.awt.Color;
import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.service.PagoService.Rango;
import com.vyra.dental.service.ReporteService.ReporteCitas;
import com.vyra.dental.service.ReporteService.ReporteHistorial;
import com.vyra.dental.service.ReporteService.ReporteIngresos;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.Formatos;

/**
 * Exportación a PDF (RF-11, CU-11): historia clínica, odontograma, reportes y
 * pagos del periodo. Usa OpenPDF y escribe en el OutputStream que recibe; el
 * controlador elige el archivo y lo ejecuta en un Task. Los permisos son los
 * de cada dato: la parte clínica la exportan Administrador y Odontólogo, los
 * reportes el Administrador y los pagos Administrador y Recepción.
 */
public class PdfService {

    // Colores de vyra.css (docs/08-guia-frontend.md §2), en el formato de OpenPDF
    private static final Color PRIMARIO = new Color(0x0F766E);
    private static final Color TINTA = new Color(0x0F172A);
    private static final Color TEXTO_2 = new Color(0x475569);
    private static final Color BORDE = new Color(0xE2E8F0);
    private static final Color FONDO_ENCABEZADO = new Color(0xF1F5F9);
    private static final Color ALERGIA = new Color(0x9A3412);
    private static final Map<EstadoPieza, Color[]> COLORES_PIEZA = new EnumMap<>(Map.of(
            EstadoPieza.SANA, new Color[] { Color.WHITE, new Color(0x64748B) },
            EstadoPieza.CARIADA, new Color[] { new Color(0xFCA5A5), new Color(0xB91C1C) },
            EstadoPieza.OBTURADA, new Color[] { new Color(0x93C5FD), new Color(0x1D4ED8) },
            EstadoPieza.EN_TRATAMIENTO, new Color[] { new Color(0xFCD34D), new Color(0xB45309) },
            EstadoPieza.EXTRAIDA, new Color[] { new Color(0xF1F5F9), new Color(0x64748B) }));

    private static final Font MARCA = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARIO);
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, TINTA);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXTO_2);
    private static final Font SECCION = new Font(Font.HELVETICA, 12, Font.BOLD, TINTA);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL, TINTA);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 9, Font.BOLD, TINTA);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 8, Font.BOLD, TEXTO_2);
    private static final Font CIFRA = new Font(Font.HELVETICA, 14, Font.BOLD, TINTA);
    private static final Font ALERTA = new Font(Font.HELVETICA, 9, Font.BOLD, ALERGIA);
    private static final Font PIE = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXTO_2);

    private final PacienteDAO pacienteDAO;
    private final ProfesionalDAO profesionalDAO;
    private final HistoriaClinicaService historiaClinica;
    private final OdontogramaService odontograma;
    private final ReporteService reportes;
    private final PagoService pagos;
    /** Fecha y hora de "Generado el ..."; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public PdfService(PacienteDAO pacienteDAO, ProfesionalDAO profesionalDAO, HistoriaClinicaService historiaClinica,
                      OdontogramaService odontograma, ReporteService reportes, PagoService pagos) {
        this(pacienteDAO, profesionalDAO, historiaClinica, odontograma, reportes, pagos, Clock.systemDefaultZone());
    }

    public PdfService(PacienteDAO pacienteDAO, ProfesionalDAO profesionalDAO, HistoriaClinicaService historiaClinica,
                      OdontogramaService odontograma, ReporteService reportes, PagoService pagos, Clock reloj) {
        this.pacienteDAO = pacienteDAO;
        this.profesionalDAO = profesionalDAO;
        this.historiaClinica = historiaClinica;
        this.odontograma = odontograma;
        this.reportes = reportes;
        this.pagos = pagos;
        this.reloj = reloj;
    }

    // ===== Parte clínica (RF-11) =====

    /** Datos del paciente, alergias, antecedentes y todas las entradas de su historia clínica. */
    public void historiaClinica(int idPaciente, OutputStream salida) {
        verificarClinica();
        Paciente paciente = paciente(idPaciente);
        Map<Integer, String> profesionales = nombresProfesionales();
        try (Documento doc = new Documento(salida, "Historia clínica", paciente.getNombreCompleto())) {
            doc.datosPaciente(paciente);
            doc.seccion("Entradas");
            List<HistoriaClinicaEntrada> entradas = historiaClinica.historial(idPaciente);
            if (entradas.isEmpty()) {
                doc.texto("Sin entradas registradas.");
                return;
            }
            PdfPTable tabla = doc.tabla(new float[] { 14, 20, 26, 40 }, "Fecha", "Profesional", "Diagnóstico", "Observaciones");
            for (HistoriaClinicaEntrada entrada : entradas) {
                celdas(tabla, Formatos.fechaCorta(entrada.getFecha()), nombre(profesionales, entrada.getIdProfesional()),
                        guion(entrada.getDiagnostico()), guion(entrada.getObservaciones()));
            }
            doc.agregar(tabla);
        }
    }

    /** Arcada con el estado actual de cada pieza, las piezas con hallazgos y el historial completo de cambios. */
    public void odontograma(int idPaciente, OutputStream salida) {
        verificarClinica();
        Paciente paciente = paciente(idPaciente);
        Map<Integer, String> profesionales = nombresProfesionales();
        Map<Integer, EstadoPieza> estados = odontograma.estadoActual(idPaciente);
        List<OdontogramaPieza> registros = new ArrayList<>();
        for (int pieza : estados.keySet()) {
            registros.addAll(odontograma.historialPieza(idPaciente, pieza));
        }
        registros.sort(Comparator.comparing(OdontogramaPieza::getFechaRegistro)
                .thenComparingInt(OdontogramaPieza::getIdRegistro).reversed());

        try (Documento doc = new Documento(salida, "Odontograma", paciente.getNombreCompleto())) {
            doc.datosPaciente(paciente);
            doc.seccion("Estado actual (notación FDI)");
            doc.agregar(arcada(estados));
            doc.agregar(leyenda(estados));

            doc.seccion("Piezas con hallazgos");
            Map<Integer, OdontogramaPieza> ultimo = new HashMap<>();
            registros.forEach(r -> ultimo.putIfAbsent(r.getNumeroPieza(), r));
            List<Integer> conHallazgos = estados.entrySet().stream()
                    .filter(e -> e.getValue() != EstadoPieza.SANA).map(Map.Entry::getKey).toList();
            if (conHallazgos.isEmpty()) {
                doc.texto("Todas las piezas están sanas.");
            } else {
                PdfPTable tabla = doc.tabla(new float[] { 8, 32, 16, 14, 30 }, "Pieza", "Nombre", "Estado", "Desde", "Observación");
                for (int pieza : conHallazgos) {
                    OdontogramaPieza registro = ultimo.get(pieza);
                    celdas(tabla, String.valueOf(pieza), OdontogramaService.nombrePieza(pieza),
                            estados.get(pieza).getEtiqueta(),
                            registro == null ? "—" : Formatos.fechaCorta(registro.getFechaRegistro()),
                            registro == null ? "—" : guion(registro.getObservacion()));
                }
                doc.agregar(tabla);
            }

            doc.seccion("Historial de cambios");
            if (registros.isEmpty()) {
                doc.texto("Sin cambios registrados.");
                return;
            }
            PdfPTable historial = doc.tabla(new float[] { 14, 8, 16, 24, 38 }, "Fecha", "Pieza", "Estado", "Profesional", "Observación");
            for (OdontogramaPieza registro : registros) {
                celdas(historial, Formatos.fechaCorta(registro.getFechaRegistro()), String.valueOf(registro.getNumeroPieza()),
                        registro.getEstado().getEtiqueta(), nombre(profesionales, registro.getIdProfesional()),
                        guion(registro.getObservacion()));
            }
            doc.agregar(historial);
        }
    }

    public boolean puedeExportarClinica() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.ODONTOLOGO);
    }

    // ===== Reportes (RF-09) =====

    public void reporteCitas(LocalDate desde, LocalDate hasta, Profesional profesional, OutputStream salida) {
        ReporteCitas reporte = reportes.citas(desde, hasta, profesional == null ? null : profesional.getIdProfesional());
        String detalle = rango(desde, hasta) + " · " + (profesional == null ? "Todos los profesionales" : profesional.getNombreCompleto());
        try (Documento doc = new Documento(salida, "Reporte de citas", detalle)) {
            doc.indicadores("Citas", String.valueOf(reporte.total()), "Atendidas", String.valueOf(reporte.atendidas()),
                    "Canceladas", String.valueOf(reporte.canceladas()), "Asistencia",
                    reporte.asistencia() == null ? "—" : reporte.asistencia() + "%");
            doc.seccion("Citas");
            if (reporte.citas().isEmpty()) {
                doc.texto("Sin citas en este periodo.");
                return;
            }
            PdfPTable tabla = doc.tabla(new float[] { 13, 12, 24, 21, 18, 12 }, "Fecha", "Hora", "Paciente", "Profesional", "Motivo", "Estado");
            for (Cita cita : reporte.citas()) {
                celdas(tabla, Formatos.fechaCorta(cita.getFecha()), Formatos.rangoHoras(cita.getHoraInicio(), cita.getHoraFin()),
                        cita.getPaciente().getNombreCompleto(), cita.getProfesional().getNombreCompleto(),
                        guion(cita.getMotivo()), cita.getEstado().getEtiqueta());
            }
            doc.agregar(tabla);
        }
    }

    public void reporteHistorial(int idPaciente, OutputStream salida) {
        ReporteHistorial reporte = reportes.historialPaciente(idPaciente);
        Paciente paciente = paciente(idPaciente);
        try (Documento doc = new Documento(salida, "Historial de tratamientos", paciente.getNombreCompleto() + " · DNI " + paciente.getDni())) {
            doc.indicadores("Tratamientos", String.valueOf(reporte.tratamientos().size()), "Costo total",
                    Formatos.dinero(reporte.costo()), "Pagado", Formatos.dinero(reporte.pagado()), "Saldo",
                    Formatos.dinero(reporte.saldo()));
            doc.seccion("Tratamientos");
            if (reporte.tratamientos().isEmpty()) {
                doc.texto("Sin tratamientos registrados.");
                return;
            }
            PdfPTable tabla = doc.tabla(new float[] { 13, 27, 10, 13, 13, 13, 11 },
                    "Fecha", "Tratamiento", "Piezas", "Costo", "Pagado", "Saldo", "Estado");
            for (TratamientoResumen t : reporte.tratamientos()) {
                List<Integer> piezas = t.getTratamiento().getPiezasDentales();
                celdas(tabla, Formatos.fechaCorta(t.getTratamiento().getFecha()), t.getTratamiento().getDescripcion(),
                        piezas == null || piezas.isEmpty() ? "—" : String.join(", ", piezas.stream().map(String::valueOf).toList()),
                        Formatos.dinero(t.getTratamiento().getCosto()), Formatos.dinero(t.getPagado()),
                        Formatos.dinero(t.getSaldo()), t.getTratamiento().getEstado().getEtiqueta());
            }
            doc.agregar(tabla);
        }
    }

    public void reporteIngresos(LocalDate desde, LocalDate hasta, OutputStream salida) {
        ReporteIngresos reporte = reportes.ingresos(desde, hasta);
        try (Documento doc = new Documento(salida, "Reporte de ingresos", rango(desde, hasta))) {
            doc.indicadores("Total cobrado", Formatos.dinero(reporte.total()), "Pagos", String.valueOf(reporte.pagos()),
                    "Promedio por pago", reporte.pagos() == 0 ? "—" : Formatos.dinero(reporte.total() / reporte.pagos()));
            doc.seccion("Por método de pago");
            PdfPTable metodos = doc.tabla(new float[] { 40, 20, 25, 15 }, "Método", "Pagos", "Total", "% del total");
            for (MetodoPago metodo : MetodoPago.values()) {
                double total = reporte.porMetodo().getOrDefault(metodo, 0.0);
                celdas(metodos, metodo.getEtiqueta(), String.valueOf(reporte.pagosPorMetodo().getOrDefault(metodo, 0)),
                        Formatos.dinero(total), reporte.total() == 0 ? "0%" : Math.round(total / reporte.total() * 100) + "%");
            }
            doc.agregar(metodos);
            doc.seccion("Por día");
            PdfPTable dias = doc.tabla(new float[] { 50, 50 }, "Fecha", "Cobrado");
            reporte.porDia().forEach((dia, total) -> {
                if (total > 0) {
                    celdas(dias, Formatos.fechaCorta(dia), Formatos.dinero(total));
                }
            });
            if (reporte.total() == 0) {
                doc.texto("Sin pagos en este periodo.");
            } else {
                doc.agregar(dias);
            }
        }
    }

    // ===== Pagos =====

    /** Los pagos del periodo de la pantalla Pagos ("Exportar"). */
    public void pagos(Rango rango, String nombrePeriodo, OutputStream salida) {
        if (!pagos.puedeRegistrar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
        List<PagoResumen> lista = pagos.pagosEntre(rango);
        double total = pagos.totalCobrado(rango);
        try (Documento doc = new Documento(salida, "Pagos " + nombrePeriodo, rango(rango.desde(), rango.hasta()))) {
            doc.indicadores("Total cobrado", Formatos.dinero(total), "Pagos", String.valueOf(lista.size()));
            doc.seccion("Pagos");
            if (lista.isEmpty()) {
                doc.texto("Sin pagos en este periodo.");
                return;
            }
            PdfPTable tabla = doc.tabla(new float[] { 13, 26, 26, 17, 18 }, "Fecha", "Paciente", "Concepto", "Método", "Monto");
            for (PagoResumen r : lista) {
                celdas(tabla, Formatos.fechaCorta(r.getPago().getFechaPago()), r.getPaciente().getNombreCompleto(),
                        r.getConcepto(), r.getPago().getMetodoPago().getEtiqueta(), Formatos.dinero(r.getPago().getMonto()));
            }
            doc.agregar(tabla);
        }
    }

    /** "historia-clinica-72481935-2026-11-10.pdf": tipo, detalle (sin espacios ni tildes) y fecha de hoy. */
    public String nombreArchivo(String tipo, String detalle) {
        String base = tipo + (detalle == null || detalle.isBlank() ? "" : "-" + detalle);
        String limpio = Busqueda.normalizar(base).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return limpio + "-" + LocalDate.now(reloj) + ".pdf";
    }

    // ===== Piezas del odontograma =====

    /** Dos filas de 16 piezas con la línea media; número, color del estado y su letra. */
    private static PdfPTable arcada(Map<Integer, EstadoPieza> estados) {
        float[] anchos = new float[17];
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = i == 8 ? 0.4f : 1;
        }
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        filaNumeros(tabla, OdontogramaService.PIEZAS_SUPERIORES);
        filaPiezas(tabla, OdontogramaService.PIEZAS_SUPERIORES, estados);
        for (int i = 0; i < 17; i++) {
            PdfPCell separador = new PdfPCell(new Phrase(" ", PIE));
            separador.setBorder(Rectangle.NO_BORDER);
            separador.setFixedHeight(8);
            tabla.addCell(separador);
        }
        filaPiezas(tabla, OdontogramaService.PIEZAS_INFERIORES, estados);
        filaNumeros(tabla, OdontogramaService.PIEZAS_INFERIORES);
        return tabla;
    }

    private static void filaNumeros(PdfPTable tabla, List<Integer> piezas) {
        for (int i = 0; i < piezas.size(); i++) {
            if (i == 8) {
                tabla.addCell(lineaMedia());
            }
            PdfPCell celda = new PdfPCell(new Phrase(String.valueOf(piezas.get(i)), ETIQUETA));
            celda.setBorder(Rectangle.NO_BORDER);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }
    }

    private static void filaPiezas(PdfPTable tabla, List<Integer> piezas, Map<Integer, EstadoPieza> estados) {
        for (int i = 0; i < piezas.size(); i++) {
            if (i == 8) {
                tabla.addCell(lineaMedia());
            }
            EstadoPieza estado = estados.getOrDefault(piezas.get(i), EstadoPieza.SANA);
            Color[] colores = COLORES_PIEZA.get(estado);
            PdfPCell celda = new PdfPCell(new Phrase(estado.getCodigo(), new Font(Font.HELVETICA, 10, Font.BOLD, colores[1])));
            celda.setBackgroundColor(colores[0]);
            celda.setBorderColor(colores[1]);
            celda.setBorderWidth(1);
            celda.setFixedHeight(24);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(celda);
        }
    }

    private static PdfPCell lineaMedia() {
        PdfPCell celda = new PdfPCell(new Phrase(""));
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    /** "C Cariada (2)  O Obturada (3) ...": el estado se lee con letra, no solo con color. */
    private static Paragraph leyenda(Map<Integer, EstadoPieza> estados) {
        Paragraph leyenda = new Paragraph();
        leyenda.setSpacingBefore(6);
        for (EstadoPieza estado : EstadoPieza.values()) {
            long cantidad = estados.values().stream().filter(e -> e == estado).count();
            String letra = estado.getCodigo().isEmpty() ? "–" : estado.getCodigo();
            leyenda.add(new Chunk(letra + " ", new Font(Font.HELVETICA, 9, Font.BOLD, COLORES_PIEZA.get(estado)[1])));
            leyenda.add(new Chunk(estado.getEtiqueta() + " (" + cantidad + ")      ", NORMAL));
        }
        return leyenda;
    }

    // ===== Utilidades =====

    private void verificarClinica() {
        if (!puedeExportarClinica()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private Paciente paciente(int idPaciente) {
        return pacienteDAO.buscarPorId(idPaciente).orElseThrow(() -> new ReglaNegocioException("El paciente no existe."));
    }

    private Map<Integer, String> nombresProfesionales() {
        Map<Integer, String> nombres = new HashMap<>();
        for (Profesional profesional : profesionalDAO.listarTodos()) {
            nombres.put(profesional.getIdProfesional(), profesional.getNombreCompleto());
        }
        return nombres;
    }

    private static String nombre(Map<Integer, String> profesionales, Integer idProfesional) {
        return idProfesional == null ? "—" : profesionales.getOrDefault(idProfesional, "—");
    }

    private static String rango(LocalDate desde, LocalDate hasta) {
        return desde.equals(hasta) ? Formatos.fechaCorta(desde) : "Del " + Formatos.fechaCorta(desde) + " al " + Formatos.fechaCorta(hasta);
    }

    private static String guion(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    private static void celdas(PdfPTable tabla, String... textos) {
        for (String texto : textos) {
            PdfPCell celda = new PdfPCell(new Phrase(texto, NORMAL));
            celda.setBorderColor(BORDE);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
    }

    /**
     * Un documento A4 con el encabezado de Vyra (título, detalle y "Generado
     * el ... por ...") y el número de página al pie. Se cierra con
     * try-with-resources; cerrar el documento no cierra el OutputStream.
     */
    private final class Documento implements AutoCloseable {

        private final Document documento = new Document(PageSize.A4, 40, 40, 40, 50);

        Documento(OutputStream salida, String titulo, String detalle) {
            try {
                PdfWriter escritor = PdfWriter.getInstance(documento, salida);
                escritor.setCloseStream(false);
                escritor.setPageEvent(new PieDePagina(titulo));
                documento.addTitle(titulo + " · " + detalle);
                documento.addCreator("Vyra");
                documento.open();
                documento.add(new Paragraph("Vyra · Consultorio dental", MARCA));
                Paragraph encabezado = new Paragraph(titulo, TITULO);
                encabezado.setSpacingBefore(4);
                documento.add(encabezado);
                documento.add(new Paragraph(detalle, SECCION));
                String autor = Sesion.get().estaIniciada() ? " por " + Sesion.get().getUsuario().getNombreCompleto() : "";
                LocalDateTime ahora = LocalDateTime.now(reloj);
                Paragraph generado = new Paragraph("Generado el " + Formatos.fechaCorta(ahora.toLocalDate()) + " a las "
                        + Formatos.hora(ahora.toLocalTime()) + autor, SUBTITULO);
                generado.setSpacingAfter(10);
                documento.add(generado);
            } catch (DocumentException e) {
                throw new RuntimeException("No se pudo crear el PDF", e);
            }
        }

        void datosPaciente(Paciente paciente) {
            PdfPTable tabla = new PdfPTable(new float[] { 22, 78 });
            tabla.setWidthPercentage(100);
            fila(tabla, "Paciente", paciente.getNombreCompleto(), NEGRITA);
            fila(tabla, "DNI", paciente.getDni(), NORMAL);
            fila(tabla, "Fecha de nacimiento", paciente.getFechaNacimiento() == null ? "—"
                    : Formatos.fechaCorta(paciente.getFechaNacimiento()) + " (" + Formatos.edad(paciente.getFechaNacimiento()) + ")", NORMAL);
            fila(tabla, "Teléfono", paciente.getTelefono() == null ? "—" : Formatos.telefono(paciente.getTelefono()), NORMAL);
            fila(tabla, "Alergias", paciente.tieneAlergias() ? paciente.getAlergias() : "Ninguna",
                    paciente.tieneAlergias() ? ALERTA : NORMAL);
            fila(tabla, "Antecedentes", guion(paciente.getAntecedentes()), NORMAL);
            agregar(tabla);
        }

        /** Pares título/cifra en una fila de recuadros. */
        void indicadores(String... pares) {
            PdfPTable tabla = new PdfPTable(pares.length / 2);
            tabla.setWidthPercentage(100);
            tabla.setSpacingAfter(4);
            for (int i = 0; i < pares.length; i += 2) {
                PdfPCell celda = new PdfPCell();
                celda.setBorderColor(BORDE);
                celda.setPadding(8);
                celda.addElement(new Paragraph(pares[i], ETIQUETA));
                celda.addElement(new Paragraph(pares[i + 1], CIFRA));
                tabla.addCell(celda);
            }
            agregar(tabla);
        }

        void seccion(String titulo) {
            Paragraph seccion = new Paragraph(titulo, SECCION);
            seccion.setSpacingBefore(14);
            seccion.setSpacingAfter(6);
            agregar(seccion);
        }

        void texto(String texto) {
            agregar(new Paragraph(texto, SUBTITULO));
        }

        PdfPTable tabla(float[] anchos, String... encabezados) {
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String encabezado : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(encabezado.toUpperCase(Formatos.ES_PE), ETIQUETA));
                celda.setBackgroundColor(FONDO_ENCABEZADO);
                celda.setBorderColor(BORDE);
                celda.setPadding(5);
                tabla.addCell(celda);
            }
            return tabla;
        }

        void agregar(Element elemento) {
            try {
                documento.add(elemento);
            } catch (DocumentException e) {
                throw new RuntimeException("No se pudo escribir el PDF", e);
            }
        }

        private void fila(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
            PdfPCell nombre = new PdfPCell(new Phrase(etiqueta, ETIQUETA));
            PdfPCell dato = new PdfPCell(new Phrase(valor, fuente));
            for (PdfPCell celda : List.of(nombre, dato)) {
                celda.setBorderColor(BORDE);
                celda.setPadding(5);
                tabla.addCell(celda);
            }
        }

        @Override
        public void close() {
            documento.close();
        }
    }

    /** "Vyra · Odontograma · Página 2" al pie de cada página. */
    private static final class PieDePagina extends PdfPageEventHelper {

        private final String titulo;

        PieDePagina(String titulo) {
            this.titulo = titulo;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Vyra · " + titulo + " · Página " + escritor.getPageNumber(), PIE),
                    (documento.left() + documento.right()) / 2, documento.bottom() - 20, 0);
        }
    }
}
