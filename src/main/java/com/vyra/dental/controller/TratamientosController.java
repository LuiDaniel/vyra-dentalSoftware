package com.vyra.dental.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.service.TratamientoService;
import com.vyra.dental.ui.AvancePago;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.CeldaPaciente;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Tratamientos (docs/08-guia-frontend.md §7.7, CU-06): tabla con lo pagado de
 * cada uno, detalle con sus pagos y formulario "Nuevo tratamiento". El
 * odontólogo registra y finaliza; Administrador y Recepción consultan y pasan a
 * Pagos para cobrar.
 */
public class TratamientosController implements RecibeParametro {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;
    private static final int FILAS_MINIMAS = 5;

    /** Opciones del selector: En curso / Finalizados / Todos (null). */
    private static final EstadoTratamiento[] FILTROS = { EstadoTratamiento.EN_CURSO, EstadoTratamiento.FINALIZADO, null };

    private enum Panel { VACIO, DETALLE, FORMULARIO }

    @FXML private VBox raiz;
    @FXML private Label resumen;
    @FXML private Button botonNuevo;
    @FXML private TextField campoBuscar;
    @FXML private HBox selectorEstado;
    @FXML private TableView<TratamientoResumen> tabla;
    @FXML private Label pie;

    @FXML private VBox sinSeleccion;
    @FXML private VBox detalle;
    @FXML private Label detalleTitulo;
    @FXML private HBox detalleEstado;
    @FXML private Label detalleSubtitulo;
    @FXML private FlowPane detallePiezas;
    @FXML private Label detalleCosto;
    @FXML private Label detallePagado;
    @FXML private Label detalleSaldo;
    @FXML private ProgressBar detalleAvance;
    @FXML private VBox detallePagos;
    @FXML private Aviso avisoDetalle;
    @FXML private HBox pieDetalle;
    @FXML private Button botonFinalizar;
    @FXML private Button botonPagar;

    @FXML private VBox formulario;
    @FXML private CampoFormulario cfCita;
    @FXML private ComboBox<Cita> campoCita;
    @FXML private CampoFormulario cfDescripcion;
    @FXML private TextField campoDescripcion;
    @FXML private CampoFormulario cfPiezas;
    @FXML private TextField campoPiezas;
    @FXML private CampoFormulario cfCosto;
    @FXML private TextField campoCosto;
    @FXML private ToggleButton estadoEnCurso;
    @FXML private ToggleButton estadoFinalizado;
    @FXML private Aviso avisoFormulario;
    @FXML private Button botonRegistrar;

    private final TratamientoService servicio = Servicios.tratamientos();
    private final ObservableList<TratamientoResumen> filas = FXCollections.observableArrayList();
    /** Botones del selector, en el orden de FILTROS. */
    private final List<ToggleButton> botonesFiltro = new ArrayList<>();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    /** Nombre del campo en el servicio → campo del formulario, para mostrar los errores. */
    private final Map<String, CampoFormulario> campos = new HashMap<>();

    private EstadoTratamiento filtro = EstadoTratamiento.EN_CURSO;
    private TratamientoResumen seleccionado;
    private boolean editable;
    /** Administrador y Recepción pasan desde aquí a la pantalla Pagos. */
    private boolean puedeCobrar;
    private boolean ignorarSeleccion;

    @FXML
    private void initialize() {
        editable = servicio.puedeEditar();
        puedeCobrar = Pantalla.PAGOS.permitidaPara(Sesion.get().getUsuario().getRol());
        campos.put("idCita", cfCita);
        campos.put("descripcion", cfDescripcion);
        campos.put("piezasDentales", cfPiezas);
        campos.put("costo", cfCosto);

        construirSelectorEstado();
        construirTabla();
        configurarFormulario();
        alternar(botonNuevo, editable);
        resumen.setText(editable
                ? "Registra los tratamientos de tus citas y márcalos como finalizados."
                : "Consulta los tratamientos, lo pagado y el saldo de cada uno.");

        esperaBusqueda.setOnFinished(e -> cargar());
        campoBuscar.textProperty().addListener((obs, anterior, nuevo) -> esperaBusqueda.playFromStart());
        campoBuscar.setOnAction(e -> {
            esperaBusqueda.stop();
            cargar();
            e.consume();
        });

        cargar();
        if (!filas.isEmpty()) {
            tabla.getSelectionModel().selectFirst();
        } else {
            mostrarPanel(Panel.VACIO);
        }
    }

    /** Desde el odontograma: "Registrar tratamiento para esta pieza". */
    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof NuevoTratamiento pedido && editable) {
            prepararNuevo();
            campoPiezas.setText(String.valueOf(pedido.pieza()));
            Paciente paciente = pedido.paciente();
            Optional<Cita> suCita = campoCita.getItems().stream()
                    .filter(c -> c.getPaciente().getIdPaciente() == paciente.getIdPaciente())
                    .findFirst();
            suCita.ifPresentOrElse(campoCita::setValue, () -> avisoFormulario.mostrar(Aviso.Tipo.ADVERTENCIA,
                    paciente.getNombreCompleto() + " no tiene citas de hoy ni atendidas en los últimos "
                            + TratamientoService.DIAS_CITAS_RECIENTES + " días. Elige otra cita o agenda una."));
            campoDescripcion.requestFocus();
        }
    }

    // ===== Lista =====

    private void construirSelectorEstado() {
        ToggleGroup grupo = new ToggleGroup();
        for (EstadoTratamiento opcion : FILTROS) {
            ToggleButton boton = new ToggleButton(opcion == null ? "Todos"
                    : opcion == EstadoTratamiento.EN_CURSO ? "En curso" : "Finalizados");
            boton.setToggleGroup(grupo);
            boton.setSelected(opcion == filtro);
            boton.setOnAction(e -> {
                // Siempre queda una opción elegida
                boton.setSelected(true);
                if (filtro != opcion) {
                    filtro = opcion;
                    cargar();
                }
            });
            botonesFiltro.add(boton);
            selectorEstado.getChildren().add(boton);
        }
    }

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(FILAS_MINIMAS, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);

        TableColumn<TratamientoResumen, TratamientoResumen> tratamiento = columna("TRATAMIENTO", 190);
        tratamiento.setCellFactory(c -> new TableCell<>() {
            private final Label descripcion = new Label();
            private final Label detalle = new Label();
            private final VBox contenido = new VBox(descripcion, detalle);

            {
                descripcion.getStyleClass().add("celda-titulo");
                detalle.getStyleClass().add("celda-sub");
            }

            @Override
            protected void updateItem(TratamientoResumen r, boolean vacio) {
                super.updateItem(r, vacio);
                setText(null);
                if (vacio || r == null) {
                    setGraphic(null);
                    return;
                }
                descripcion.setText(r.getTratamiento().getDescripcion());
                detalle.setText(Formatos.fechaCorta(r.getTratamiento().getFecha()) + textoPiezas(r.getTratamiento()));
                setGraphic(contenido);
            }
        });

        TableColumn<TratamientoResumen, Paciente> paciente = new TableColumn<>("PACIENTE");
        paciente.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPaciente()));
        paciente.setCellFactory(c -> new CeldaPaciente<>());
        paciente.setPrefWidth(180);
        paciente.setSortable(false);
        paciente.setReorderable(false);

        TableColumn<TratamientoResumen, TratamientoResumen> pagado = columna("PAGADO", 170);
        // "S/ 1,250.00 de S/ 4,500.00" se lee completo; se acortan la descripción y el paciente
        pagado.setMinWidth(190);
        pagado.setCellFactory(c -> new TableCell<>() {
            private final AvancePago avance = new AvancePago();

            @Override
            protected void updateItem(TratamientoResumen r, boolean vacio) {
                super.updateItem(r, vacio);
                setText(null);
                if (vacio || r == null) {
                    setGraphic(null);
                    return;
                }
                avance.mostrar(r.getPagado(), r.getTratamiento().getCosto());
                setGraphic(avance);
            }
        });

        TableColumn<TratamientoResumen, TratamientoResumen> estado = columna("ESTADO", 100);
        estado.setMinWidth(100);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(TratamientoResumen r, boolean vacio) {
                super.updateItem(r, vacio);
                setText(null);
                setGraphic(vacio || r == null ? null : new Pill(r.getTratamiento().getEstado()));
            }
        });

        tabla.getColumns().setAll(List.of(tratamiento, paciente, pagado, estado));
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion) {
                mostrarDetalle(nuevo);
            }
        });
    }

    private void cargar() {
        try {
            filas.setAll(servicio.buscar(campoBuscar.getText(), filtro));
            pie.setText(filas.isEmpty() ? "Sin resultados"
                    : filas.size() + (filas.size() == 1 ? " tratamiento" : " tratamientos"));
            boolean sinTexto = campoBuscar.getText() == null || campoBuscar.getText().isBlank();
            tabla.setPlaceholder(sinTexto && filtro == null
                    ? new EstadoVacio("Aún no hay tratamientos registrados", editable
                            ? "Usa «Nuevo tratamiento» para registrar el primero." : "Los registra el odontólogo.")
                    : new EstadoVacio("No se encontraron tratamientos", "Prueba con otro paciente o cambia el filtro."));
            reseleccionar();
        } catch (RuntimeException e) {
            filas.clear();
            pie.setText("");
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar los tratamientos", e.getMessage()));
        }
    }

    private void reseleccionar() {
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            if (seleccionado != null) {
                filas.stream()
                        .filter(r -> r.getTratamiento().getIdTratamiento() == seleccionado.getTratamiento().getIdTratamiento())
                        .findFirst()
                        .ifPresent(r -> tabla.getSelectionModel().select(r));
            }
        } finally {
            ignorarSeleccion = false;
        }
    }

    // ===== Detalle =====

    private void mostrarDetalle(TratamientoResumen r) {
        seleccionado = r;
        Tratamiento t = r.getTratamiento();
        avisoDetalle.ocultar();
        detalleTitulo.setText(t.getDescripcion());
        detalleEstado.getChildren().setAll(new Pill(t.getEstado()));
        detalleSubtitulo.setText(r.getPaciente().getNombreCompleto() + " · " + Formatos.fechaCorta(t.getFecha())
                + " · " + r.getProfesional().getNombreCompleto());

        detallePiezas.getChildren().clear();
        if (t.getPiezasDentales() == null || t.getPiezasDentales().isEmpty()) {
            Label ninguna = new Label("Sin piezas asociadas");
            ninguna.getStyleClass().add("texto-tenue");
            detallePiezas.getChildren().add(ninguna);
        } else {
            for (int pieza : t.getPiezasDentales()) {
                Label chip = new Label(String.valueOf(pieza));
                chip.getStyleClass().add("chip-pieza");
                detallePiezas.getChildren().add(chip);
            }
        }
        detalleCosto.setText(Formatos.dinero(t.getCosto()));
        detallePagado.setText(Formatos.dinero(r.getPagado()));
        detalleSaldo.setText(Formatos.dinero(r.getSaldo()));
        detalleAvance.setProgress(r.getAvance());
        cargarPagos(t.getIdTratamiento());

        boolean finalizable = editable && t.getEstado() == EstadoTratamiento.EN_CURSO;
        boolean cobrable = puedeCobrar && r.getSaldo() > 0;
        alternar(botonFinalizar, finalizable);
        alternar(botonPagar, cobrable);
        alternar(pieDetalle, finalizable || cobrable);
        mostrarPanel(Panel.DETALLE);
    }

    /** Fecha · método · monto de cada pago, del más reciente al más antiguo. */
    private void cargarPagos(int idTratamiento) {
        detallePagos.getChildren().clear();
        List<Pago> pagos;
        try {
            pagos = Servicios.pagos().pagosDe(idTratamiento);
        } catch (RuntimeException e) {
            avisoDetalle.mostrar(Aviso.Tipo.ERROR, "No se pudo cargar los pagos: " + e.getMessage());
            return;
        }
        if (pagos.isEmpty()) {
            Label ninguno = new Label("Sin pagos registrados.");
            ninguno.getStyleClass().add("texto-tenue");
            detallePagos.getChildren().add(ninguno);
            return;
        }
        for (Pago pago : pagos) {
            Label fecha = new Label(Formatos.fechaCorta(pago.getFechaPago()));
            fecha.getStyleClass().add("celda-sub");
            fecha.setMinWidth(Region.USE_PREF_SIZE);
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            Label monto = new Label(Formatos.dinero(pago.getMonto()));
            monto.getStyleClass().add("monto");
            HBox fila = new HBox(12, fecha, new Pill(pago.getMetodoPago()), espacio, monto);
            fila.getStyleClass().add("fila-pago");
            detallePagos.getChildren().add(fila);
        }
    }

    @FXML
    private void finalizar() {
        if (seleccionado == null) {
            return;
        }
        try {
            servicio.finalizar(seleccionado.getTratamiento().getIdTratamiento());
            notificar("Tratamiento finalizado");
            recargarSeleccionado();
        } catch (RuntimeException e) {
            avisoDetalle.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    @FXML
    private void registrarPago() {
        if (seleccionado != null) {
            MainController.actual().navegar(Pantalla.PAGOS, seleccionado);
        }
    }

    /** Vuelve a leer el tratamiento abierto (cambió su estado) y la lista. */
    private void recargarSeleccionado() {
        int id = seleccionado.getTratamiento().getIdTratamiento();
        cargar();
        servicio.resumen(id).ifPresent(this::mostrarDetalle);
        reseleccionar();
    }

    // ===== Nuevo tratamiento =====

    @FXML
    private void nuevo() {
        prepararNuevo();
    }

    private void prepararNuevo() {
        limpiarErrores();
        campoDescripcion.clear();
        campoPiezas.clear();
        campoCosto.clear();
        estadoEnCurso.setSelected(true);
        List<Cita> citas;
        try {
            citas = servicio.citasParaRegistrar();
        } catch (RuntimeException e) {
            citas = List.of();
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, "No se pudo cargar las citas: " + e.getMessage());
        }
        campoCita.getItems().setAll(citas);
        campoCita.setValue(citas.isEmpty() ? null : citas.get(0));
        if (citas.isEmpty()) {
            avisoFormulario.mostrar(Aviso.Tipo.INFO, "No tienes citas de hoy ni atendidas en los últimos "
                    + TratamientoService.DIAS_CITAS_RECIENTES + " días. El tratamiento se registra en una cita.");
        }
        botonRegistrar.setDisable(citas.isEmpty());
        ignorarSeleccion = true;
        tabla.getSelectionModel().clearSelection();
        ignorarSeleccion = false;
        mostrarPanel(Panel.FORMULARIO);
        campoCita.requestFocus();
    }

    @FXML
    private void registrar() {
        limpiarErrores();
        double costo;
        try {
            costo = Formatos.leerMonto(campoCosto.getText());
        } catch (NumberFormatException e) {
            cfCosto.mostrarError("Escribe el costo como 650 o 650.50");
            campoCosto.requestFocus();
            return;
        }
        try {
            Cita cita = campoCita.getValue();
            Tratamiento nuevo = new Tratamiento(0, cita == null ? 0 : cita.getIdCita(), campoDescripcion.getText(),
                    costo, estadoFinalizado.isSelected() ? EstadoTratamiento.FINALIZADO : EstadoTratamiento.EN_CURSO,
                    null, TratamientoService.leerPiezas(campoPiezas.getText()));
            servicio.registrar(nuevo);
            notificar("Tratamiento registrado");
            // Se muestra en la lista aunque el filtro elegido no lo incluya
            filtro = null;
            botonesFiltro.get(Arrays.asList(FILTROS).indexOf(null)).setSelected(true);
            campoBuscar.clear();
            esperaBusqueda.stop();
            seleccionado = servicio.resumen(nuevo.getIdTratamiento()).orElse(null);
            cargar();
            if (seleccionado != null) {
                mostrarDetalle(seleccionado);
            }
        } catch (ValidacionException e) {
            e.getErrores().forEach(this::mostrarError);
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(""), e.getMessage());
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    @FXML
    private void cancelar() {
        if (seleccionado != null) {
            mostrarDetalle(seleccionado);
            reseleccionar();
        } else {
            mostrarPanel(Panel.VACIO);
        }
    }

    private void configurarFormulario() {
        ToggleGroup estados = new ToggleGroup();
        estadoEnCurso.setToggleGroup(estados);
        estadoFinalizado.setToggleGroup(estados);
        estados.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            }
        });
        campoCita.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cita cita) {
                return cita == null ? "" : Formatos.fechaCorta(cita.getFecha()) + " · "
                        + cita.getPaciente().getNombreCompleto() + (cita.getMotivo() == null ? "" : " · " + cita.getMotivo());
            }

            @Override
            public Cita fromString(String texto) {
                return null;
            }
        });
        // Al corregir un campo se borra su error
        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextInputControl texto) {
                texto.textProperty().addListener((obs, anterior, nuevo) -> campo.limpiarError());
            }
        }
        campoCita.valueProperty().addListener((obs, anterior, nuevo) -> cfCita.limpiarError());
        // Enter en un campo de texto registra
        for (TextField campo : List.of(campoDescripcion, campoPiezas, campoCosto)) {
            campo.setOnAction(e -> {
                registrar();
                e.consume();
            });
        }
    }

    private void mostrarError(String campo, String mensaje) {
        // Los errores de Jakarta de la lista de piezas llegan como "piezasDentales[0].<list element>"
        String clave = campo.startsWith("piezasDentales") ? "piezasDentales" : campo;
        CampoFormulario destino = campos.get(clave);
        if (destino == null) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, mensaje);
            return;
        }
        destino.mostrarError(mensaje);
        destino.getControl().requestFocus();
    }

    private void limpiarErrores() {
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoFormulario.ocultar();
    }

    // ===== Utilidades =====

    private void mostrarPanel(Panel panel) {
        alternar(sinSeleccion, panel == Panel.VACIO);
        alternar(detalle, panel == Panel.DETALLE);
        alternar(formulario, panel == Panel.FORMULARIO);
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(raiz.getScene().getWindow(), mensaje);
    }

    /** " · piezas 36, 47", o vacío si no tiene. */
    private static String textoPiezas(Tratamiento t) {
        List<Integer> piezas = t.getPiezasDentales();
        if (piezas == null || piezas.isEmpty()) {
            return "";
        }
        return (piezas.size() == 1 ? " · pieza " : " · piezas ")
                + String.join(", ", piezas.stream().map(String::valueOf).toList());
    }

    private static TableColumn<TratamientoResumen, TratamientoResumen> columna(String titulo, double ancho) {
        TableColumn<TratamientoResumen, TratamientoResumen> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
