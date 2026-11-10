package com.vyra.dental.controller;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.service.OdontogramaService;
import com.vyra.dental.service.PacienteService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.BuscadorPacientes;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.CeldaAlergias;
import com.vyra.dental.ui.Dialogos;
import com.vyra.dental.ui.ExportarPdf;
import com.vyra.dental.ui.LineaDeTiempo;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.ui.PiezaDental;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Odontograma del paciente (docs/08-guia-frontend.md §7.6, CU-05). Un clic en
 * una pieza la selecciona y le aplica el estado elegido en la barra. Los
 * cambios quedan pendientes hasta pulsar "Guardar" (cada uno es un registro
 * nuevo, RN-09). El odontólogo registra cambios; el administrador solo consulta.
 */
public class OdontogramaController implements RecibeParametro, ConfirmaSalida {

    /** Orden de los botones de la barra (guía §7.6). */
    private static final List<EstadoPieza> HERRAMIENTAS = List.of(EstadoPieza.SANA, EstadoPieza.CARIADA,
            EstadoPieza.OBTURADA, EstadoPieza.EN_TRATAMIENTO, EstadoPieza.EXTRAIDA);
    private static final int PIEZAS_POR_LADO = 8;

    @FXML private VBox raiz;
    @FXML private VBox vistaSeleccion;
    @FXML private BuscadorPacientes buscador;
    @FXML private VBox vistaOdontograma;
    @FXML private Avatar avatarPaciente;
    @FXML private Label nombrePaciente;
    @FXML private Label detallePaciente;
    @FXML private HBox contenedorAlergia;
    @FXML private Button botonGuardar;
    @FXML private Button botonPdf;
    @FXML private Aviso avisoGuardar;
    @FXML private Label etiquetaHerramientas;
    @FXML private HBox herramientas;
    @FXML private Label ayuda;
    @FXML private VBox arcada;
    @FXML private Label cuadroPieza;
    @FXML private Label tituloPieza;
    @FXML private Label estadoPieza;
    @FXML private CampoFormulario cfObservacion;
    @FXML private TextArea campoObservacion;
    @FXML private Label ayudaObservacion;
    @FXML private Button enlaceTratamiento;
    @FXML private Label tituloHistorial;
    @FXML private LineaDeTiempo historial;
    @FXML private Label historialVacio;

    private final OdontogramaService servicio = Servicios.odontograma();
    private final PacienteService pacientes = Servicios.pacientes();
    private final Map<Integer, PiezaDental> piezas = new HashMap<>();
    private final Map<EstadoPieza, Label> conteos = new EnumMap<>(EstadoPieza.class);
    private final ToggleGroup grupoPiezas = new ToggleGroup();
    private final ToggleGroup grupoHerramientas = new ToggleGroup();
    /** Nombre de cada profesional por id, para el historial. */
    private final Map<Integer, String> profesionales = new HashMap<>();

    /** Estado guardado de cada pieza. */
    private Map<Integer, EstadoPieza> guardado = new TreeMap<>();
    /** Cambios sin guardar: pieza → estado nuevo. */
    private final Map<Integer, EstadoPieza> pendientes = new TreeMap<>();
    /** Observación de cada cambio sin guardar. */
    private final Map<Integer, String> observaciones = new HashMap<>();
    private List<OdontogramaPieza> historialSeleccionada = List.of();

    private Paciente paciente;
    private Integer seleccionada;
    private boolean editable;
    /** true mientras se escribe la observación por código (no es un cambio del usuario). */
    private boolean cargandoObservacion;

    @FXML
    private void initialize() {
        editable = servicio.puedeEditar();
        construirHerramientas();
        construirArcada();
        campoObservacion.textProperty().addListener((obs, anterior, nuevo) -> {
            if (!cargandoObservacion && seleccionada != null && pendientes.containsKey(seleccionada)) {
                observaciones.put(seleccionada, nuevo);
                mostrarHistorial();
            }
        });
        aplicarPermisos();
        cargarProfesionales();

        buscador.configurar(texto -> pacientes.buscar(texto, FiltroPaciente.TODOS, 0),
                texto -> pacientes.contar(texto, FiltroPaciente.TODOS),
                this::abrir);
        mostrarVista(false);
        buscador.enfocar();
    }

    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof Paciente elegido) {
            abrir(elegido);
        }
    }

    @Override
    public boolean confirmarSalida() {
        if (pendientes.isEmpty()) {
            return true;
        }
        int n = pendientes.size();
        return Dialogos.confirmar(raiz.getScene().getWindow(), "Cambios sin guardar",
                "¿Salir sin guardar el odontograma?",
                (n == 1 ? "Hay 1 cambio sin guardar" : "Hay " + n + " cambios sin guardar") + ". Si sales, se perderá"
                        + (n == 1 ? "." : "n."),
                "Salir sin guardar");
    }

    // ===== Paciente =====

    private void abrir(Paciente elegido) {
        // La fila puede venir de otra pantalla con datos incompletos (la agenda): se lee de nuevo
        paciente = pacientes.buscarPorId(elegido.getIdPaciente()).orElse(elegido);
        pendientes.clear();
        observaciones.clear();
        avisoGuardar.ocultar();
        try {
            guardado = servicio.estadoActual(paciente.getIdPaciente());
        } catch (RuntimeException e) {
            guardado = new TreeMap<>();
            avisoGuardar.mostrar(Aviso.Tipo.ERROR, "No se pudo cargar el odontograma: " + e.getMessage());
        }
        mostrarPaciente();
        piezas.keySet().forEach(this::actualizarPieza);
        actualizarConteos();
        actualizarBotonGuardar();
        if (grupoPiezas.getSelectedToggle() != null) {
            grupoPiezas.getSelectedToggle().setSelected(false);
        }
        seleccionar(null);
        mostrarVista(true);
    }

    @FXML
    private void cambiarPaciente() {
        if (!confirmarSalida()) {
            return;
        }
        pendientes.clear();
        observaciones.clear();
        paciente = null;
        buscador.marcar(0);
        mostrarVista(false);
        buscador.enfocar();
    }

    /** "DNI 72481935 · 34 años · Cita actual: Endodoncia pieza 36" y la alergia. */
    private void mostrarPaciente() {
        avatarPaciente.mostrar(paciente.getIniciales(), paciente.getIdPaciente());
        nombrePaciente.setText(paciente.getNombreCompleto());
        StringBuilder detalle = new StringBuilder("DNI " + paciente.getDni());
        if (paciente.getFechaNacimiento() != null) {
            detalle.append(" · ").append(Formatos.edad(paciente.getFechaNacimiento()));
        }
        try {
            List<Cita> deHoy = Servicios.citas().citasDeHoyDePaciente(paciente.getIdPaciente());
            if (!deHoy.isEmpty()) {
                Cita cita = deHoy.get(0);
                detalle.append(" · Cita actual: ").append(cita.getMotivo() != null ? cita.getMotivo()
                        : Formatos.rangoHoras(cita.getHoraInicio(), cita.getHoraFin()));
            }
        } catch (RuntimeException e) {
            // La cita actual es solo informativa: sin ella el odontograma sigue funcionando
        }
        detallePaciente.setText(detalle.toString());

        contenedorAlergia.getChildren().clear();
        if (paciente.tieneAlergias()) {
            Label alergia = CeldaAlergias.pillAlergia("Alergia: " + paciente.getAlergias().lines().findFirst().orElse(""));
            alergia.getStyleClass().add("pill-grande");
            alergia.setTooltip(new Tooltip(paciente.getAlergias()));
            contenedorAlergia.getChildren().add(alergia);
        }
    }

    // ===== Barra de herramientas =====

    private void construirHerramientas() {
        for (EstadoPieza estado : HERRAMIENTAS) {
            Label cuadro = new Label(estado.getCodigo());
            cuadro.getStyleClass().addAll("estado-cuadro", estado.getClaseCss());
            Label nombre = new Label(estado.getEtiqueta());
            nombre.getStyleClass().add("herramienta-nombre");
            Label conteo = new Label("0");
            conteo.getStyleClass().add("herramienta-conteo");
            conteos.put(estado, conteo);
            HBox contenido = new HBox(cuadro, nombre, conteo);
            contenido.getStyleClass().add("herramienta-contenido");

            if (editable) {
                ToggleButton boton = new ToggleButton(null, contenido);
                boton.setMinWidth(Region.USE_PREF_SIZE);
                boton.getStyleClass().add("herramienta");
                boton.setUserData(estado);
                boton.setToggleGroup(grupoHerramientas);
                boton.setAccessibleText("Marcar como " + estado.getEtiqueta());
                boton.setTooltip(new Tooltip("Marcar piezas como " + estado.getEtiqueta().toLowerCase(Formatos.ES_PE)
                        + " · otro clic para solo seleccionar"));
                herramientas.getChildren().add(boton);
            } else {
                // Solo lectura: la barra es una leyenda con los conteos
                Label leyenda = new Label(null, contenido);
                leyenda.setMinWidth(Region.USE_PREF_SIZE);
                leyenda.getStyleClass().addAll("herramienta", "herramienta-lectura");
                herramientas.getChildren().add(leyenda);
            }
        }
        grupoHerramientas.selectedToggleProperty().addListener((obs, anterior, nueva) -> actualizarAyuda());
        actualizarAyuda();
    }

    /** Estado elegido en la barra; null si solo se seleccionan piezas. */
    private EstadoPieza herramienta() {
        return grupoHerramientas.getSelectedToggle() == null ? null
                : (EstadoPieza) grupoHerramientas.getSelectedToggle().getUserData();
    }

    private void actualizarAyuda() {
        EstadoPieza elegido = herramienta();
        if (!editable) {
            ayuda.setText("Notación FDI · solo lectura");
        } else if (elegido == null) {
            ayuda.setText("Notación FDI · elige un estado y haz clic en las piezas");
        } else {
            ayuda.setText("Notación FDI · clic en una pieza para marcarla como "
                    + elegido.getEtiqueta().toLowerCase(Formatos.ES_PE));
        }
    }

    private void actualizarConteos() {
        Map<EstadoPieza, Integer> cantidad = new EnumMap<>(EstadoPieza.class);
        for (int numero : piezas.keySet()) {
            cantidad.merge(estadoVisible(numero), 1, Integer::sum);
        }
        conteos.forEach((estado, label) -> label.setText(String.valueOf(cantidad.getOrDefault(estado, 0))));
    }

    // ===== Arcada =====

    private void construirArcada() {
        HBox superior = fila(OdontogramaService.PIEZAS_SUPERIORES, true);
        HBox inferior = fila(OdontogramaService.PIEZAS_INFERIORES, false);

        HBox rotulos = new HBox(rotulo("Derecha del paciente"), espacio(), rotulo("Maxilar superior"), espacio(),
                rotulo("Izquierda del paciente"));
        rotulos.prefWidthProperty().bind(superior.widthProperty());
        rotulos.maxWidthProperty().bind(superior.widthProperty());
        Label mandibula = rotulo("Mandíbula inferior");

        arcada.getChildren().setAll(rotulos, superior, inferior, mandibula);
    }

    /** Dos grupos de 8 piezas separados por la línea media. */
    private HBox fila(List<Integer> numeros, boolean superiorDeLaArcada) {
        HBox derecha = new HBox();
        HBox izquierda = new HBox();
        for (int i = 0; i < numeros.size(); i++) {
            PiezaDental pieza = new PiezaDental(numeros.get(i), superiorDeLaArcada);
            pieza.setToggleGroup(grupoPiezas);
            pieza.setOnAction(e -> alHacerClic(pieza));
            piezas.put(pieza.getNumero(), pieza);
            (i < PIEZAS_POR_LADO ? derecha : izquierda).getChildren().add(pieza);
        }
        derecha.getStyleClass().add("arcada-grupo");
        izquierda.getStyleClass().add("arcada-grupo");
        Region lineaMedia = new Region();
        lineaMedia.getStyleClass().add("arcada-linea-media");
        HBox fila = new HBox(derecha, lineaMedia, izquierda);
        fila.getStyleClass().addAll("arcada-fila", superiorDeLaArcada ? "arcada-superior" : "arcada-inferior");
        fila.setMaxWidth(Region.USE_PREF_SIZE);
        return fila;
    }

    private static Label rotulo(String texto) {
        Label label = new Label(texto.toUpperCase(Formatos.ES_PE));
        label.getStyleClass().add("arcada-rotulo");
        return label;
    }

    private static Region espacio() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    /** Clic (o Espacio) en una pieza: se selecciona y, si hay un estado elegido, se le aplica. */
    private void alHacerClic(PiezaDental pieza) {
        // Un segundo clic no la deja sin seleccionar
        pieza.setSelected(true);
        EstadoPieza elegido = herramienta();
        if (editable && paciente != null && elegido != null) {
            aplicar(pieza.getNumero(), elegido);
        }
        seleccionar(pieza.getNumero());
    }

    private void aplicar(int numero, EstadoPieza estado) {
        if (estado == guardado.get(numero)) {
            // Volver al estado guardado deshace el cambio pendiente
            pendientes.remove(numero);
            observaciones.remove(numero);
        } else {
            pendientes.put(numero, estado);
        }
        actualizarPieza(numero);
        actualizarConteos();
        actualizarBotonGuardar();
    }

    private void actualizarPieza(int numero) {
        PiezaDental pieza = piezas.get(numero);
        pieza.setEstado(estadoVisible(numero));
        pieza.setPendiente(pendientes.containsKey(numero));
    }

    private EstadoPieza estadoVisible(int numero) {
        return pendientes.getOrDefault(numero, guardado.getOrDefault(numero, EstadoPieza.SANA));
    }

    // ===== Pieza seleccionada e historial =====

    private void seleccionar(Integer numero) {
        seleccionada = numero;
        historialSeleccionada = List.of();
        if (numero != null) {
            try {
                historialSeleccionada = servicio.historialPieza(paciente.getIdPaciente(), numero);
            } catch (RuntimeException e) {
                avisoGuardar.mostrar(Aviso.Tipo.ERROR, "No se pudo cargar el historial de la pieza: " + e.getMessage());
            }
        }
        mostrarDetalle();
    }

    private void mostrarDetalle() {
        for (EstadoPieza estado : EstadoPieza.values()) {
            cuadroPieza.getStyleClass().remove(estado.getClaseCss());
        }
        if (seleccionada == null) {
            alternar(cuadroPieza, false);
            tituloPieza.setText("Ninguna pieza seleccionada");
            estadoPieza.setText("Haz clic en una pieza para ver su estado y su historial.");
            alternar(cfObservacion, false);
            alternar(ayudaObservacion, false);
            alternar(enlaceTratamiento, false);
            mostrarHistorial();
            return;
        }
        int numero = seleccionada;
        EstadoPieza actual = guardado.getOrDefault(numero, EstadoPieza.SANA);
        EstadoPieza nuevo = pendientes.get(numero);
        alternar(cuadroPieza, true);
        cuadroPieza.setText(String.valueOf(numero));
        cuadroPieza.getStyleClass().add(estadoVisible(numero).getClaseCss());
        tituloPieza.setText("Pieza " + numero + " · " + OdontogramaService.nombrePieza(numero));
        estadoPieza.setText("Estado actual: " + actual.getEtiqueta()
                + (nuevo == null ? "" : " · nuevo: " + nuevo.getEtiqueta() + " (sin guardar)"));

        alternar(cfObservacion, editable);
        alternar(ayudaObservacion, editable);
        // Solo el odontólogo registra tratamientos (docs/02-casos-de-uso.md §3)
        alternar(enlaceTratamiento, Servicios.tratamientos().puedeEditar());
        campoObservacion.setDisable(nuevo == null);
        ayudaObservacion.setText(nuevo == null
                ? "Para registrar un cambio, elige un estado en la barra y haz clic en la pieza."
                : "La observación se guarda junto con el cambio de estado.");
        cargandoObservacion = true;
        campoObservacion.setText(observaciones.getOrDefault(numero, ""));
        cargandoObservacion = false;
        mostrarHistorial();
    }

    /** Los cambios sin guardar van arriba como "Hoy · sin guardar". */
    private void mostrarHistorial() {
        historial.limpiar();
        if (seleccionada == null) {
            tituloHistorial.setText("Historial de la pieza");
            historialVacio.setText("Elige una pieza para ver sus cambios de estado.");
            alternar(historialVacio, true);
            return;
        }
        tituloHistorial.setText("Historial de la pieza " + seleccionada);
        EstadoPieza nuevo = pendientes.get(seleccionada);
        if (nuevo != null) {
            historial.agregar(nuevo.getEtiqueta(), unir("Hoy · sin guardar", observaciones.get(seleccionada)),
                    null, nuevo.getClaseCss());
        }
        for (OdontogramaPieza registro : historialSeleccionada) {
            String profesional = registro.getIdProfesional() == null ? null : profesionales.get(registro.getIdProfesional());
            String detalle = unir(unir(Formatos.fechaCorta(registro.getFechaRegistro()), profesional),
                    registro.getObservacion());
            historial.agregar(registro.getEstado().getEtiqueta(), detalle, null, registro.getEstado().getClaseCss());
        }
        historialVacio.setText("Sin cambios registrados: la pieza figura como sana.");
        alternar(historialVacio, historial.estaVacia());
    }

    /** RF-11: el odontograma guardado en PDF (los cambios pendientes no se incluyen). */
    @FXML
    private void exportarPdf() {
        if (paciente == null) {
            return;
        }
        if (!pendientes.isEmpty()) {
            avisoGuardar.mostrar(Aviso.Tipo.INFO, "El PDF incluye solo los cambios guardados. "
                    + "Guarda los pendientes si quieres que aparezcan.");
        }
        int idPaciente = paciente.getIdPaciente();
        ExportarPdf.exportar(botonPdf, Servicios.pdf().nombreArchivo("Odontograma", paciente.getDni()),
                salida -> Servicios.pdf().odontograma(idPaciente, salida),
                mensaje -> avisoGuardar.mostrar(Aviso.Tipo.ERROR, mensaje));
    }

    /** Abre Tratamientos con el formulario dirigido a este paciente y esta pieza (CU-06). */
    @FXML
    private void registrarTratamiento() {
        if (paciente != null && seleccionada != null) {
            MainController.actual().navegar(Pantalla.TRATAMIENTOS, new NuevoTratamiento(paciente, seleccionada));
        }
    }

    // ===== Guardar =====

    @FXML
    private void guardar() {
        avisoGuardar.ocultar();
        try {
            int guardados = servicio.registrarCambios(paciente.getIdPaciente(), pendientes, observaciones);
            Notificacion.mostrar(raiz.getScene().getWindow(), guardados == 1
                    ? "Se guardó 1 cambio del odontograma" : "Se guardaron " + guardados + " cambios del odontograma");
            pendientes.clear();
            observaciones.clear();
            guardado = servicio.estadoActual(paciente.getIdPaciente());
            piezas.keySet().forEach(this::actualizarPieza);
            actualizarConteos();
            actualizarBotonGuardar();
            seleccionar(seleccionada);
        } catch (ValidacionException e) {
            avisoGuardar.mostrar(Aviso.Tipo.ERROR, String.join(". ", e.getErrores().values()) + ".");
        } catch (ReglaNegocioException e) {
            avisoGuardar.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        } catch (RuntimeException e) {
            avisoGuardar.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ". Los cambios siguen sin guardar.");
        }
    }

    /** "Guardar 2 cambios", o "Guardado" (deshabilitado) si no hay nada pendiente. */
    private void actualizarBotonGuardar() {
        int n = pendientes.size();
        botonGuardar.setText(n == 0 ? "Guardado" : n == 1 ? "Guardar 1 cambio" : "Guardar " + n + " cambios");
        botonGuardar.setDisable(n == 0);
    }

    // ===== Otros =====

    /** El administrador ve el odontograma, pero no puede cambiarlo. */
    private void aplicarPermisos() {
        etiquetaHerramientas.setText(editable ? "Marcar como:" : "Estados:");
        alternar(botonPdf, Servicios.pdf().puedeExportarClinica());
        alternar(botonGuardar, editable);
    }

    private void cargarProfesionales() {
        try {
            for (Profesional profesional : Servicios.profesionales().listarTodos()) {
                profesionales.put(profesional.getIdProfesional(), profesional.getNombreCompleto());
            }
        } catch (RuntimeException e) {
            // Sin los nombres, el historial muestra solo la fecha y la nota
        }
    }

    private void mostrarVista(boolean conPaciente) {
        alternar(vistaOdontograma, conPaciente);
        alternar(vistaSeleccion, !conPaciente);
    }

    private static String unir(String texto, String agregado) {
        return agregado == null || agregado.isBlank() ? texto : texto + " · " + agregado.strip();
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
