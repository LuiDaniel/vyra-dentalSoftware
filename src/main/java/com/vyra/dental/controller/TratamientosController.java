package com.vyra.dental.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.service.PagoService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.TratamientoResumen;
import com.vyra.dental.service.TratamientoService;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.Dialogos;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.ui.Pill;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tratamientos (docs/08-guia-frontend.md §7.7, CU-06). Lista a la izquierda y, a la derecha, el
 * formulario "Nuevo tratamiento" (solo odontólogo) o el detalle del tratamiento elegido.
 * Se puede abrir con un {@link Integer} (número de pieza desde el odontograma) o con un
 * {@link Paciente} (filtra por ese paciente).
 */
public class TratamientosController implements RecibeParametro {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;

    @FXML private Label resumen;
    @FXML private Button botonNuevo;
    @FXML private TextField campoBuscar;
    @FXML private HBox filtros;
    @FXML private TableView<TratamientoResumen> tabla;
    @FXML private VBox panel;

    private final TratamientoService servicio = Servicios.tratamientos();
    private final PagoService pagos = Servicios.pagos();
    private final boolean puedeEscribir = servicio.puedeEscribir();
    private final boolean puedeCobrar = pagos.puedeAcceder();
    private final ObservableList<TratamientoResumen> filas = FXCollections.observableArrayList();
    private final ToggleGroup grupoFiltro = new ToggleGroup();

    // Formulario "Nuevo tratamiento"
    private final Map<String, CampoFormulario> campos = new LinkedHashMap<>();
    private final ComboBox<Cita> campoCita = new ComboBox<>();
    private final TextField campoDescripcion = new TextField();
    private final TextField campoPiezas = new TextField();
    private final TextField campoCosto = new TextField();
    private final ToggleGroup grupoEstado = new ToggleGroup();
    private final Aviso avisoFormulario = new Aviso();

    private boolean ignorarSeleccion;

    @FXML
    private void initialize() {
        alternar(botonNuevo, puedeEscribir);
        construirFiltros();
        construirTabla();
        campoBuscar.textProperty().addListener((obs, antes, ahora) -> cargar());
        construirFormulario();
        cargar();
        if (puedeEscribir) {
            mostrarFormulario(null);
        } else {
            mostrarVacio();
        }
    }

    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof Integer pieza && puedeEscribir) {
            mostrarFormulario(pieza);
        } else if (parametro instanceof Paciente paciente) {
            seleccionarFiltro(null);
            campoBuscar.setText(paciente.getDni());
        }
    }

    // ===== Lista =====

    private void construirFiltros() {
        filtros.getChildren().setAll(
                filtro("En curso", EstadoTratamiento.EN_CURSO),
                filtro("Finalizados", EstadoTratamiento.FINALIZADO),
                filtro("Todos", null));
        grupoFiltro.selectToggle(grupoFiltro.getToggles().get(0));
        grupoFiltro.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else {
                cargar();
            }
        });
    }

    private ToggleButton filtro(String texto, EstadoTratamiento estado) {
        ToggleButton boton = new ToggleButton(texto);
        boton.setUserData(estado);
        boton.setToggleGroup(grupoFiltro);
        return boton;
    }

    private void seleccionarFiltro(EstadoTratamiento estado) {
        grupoFiltro.getToggles().stream().filter(t -> t.getUserData() == estado).findFirst()
                .ifPresent(grupoFiltro::selectToggle);
    }

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(4, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.setPlaceholder(new EstadoVacio("Sin tratamientos",
                "Los tratamientos que registre el odontólogo aparecerán aquí."));

        TableColumn<TratamientoResumen, TratamientoResumen> paciente = columna("PACIENTE", 190);
        paciente.setCellFactory(c -> new CeldaPaciente());
        TableColumn<TratamientoResumen, TratamientoResumen> pagado = columna("PAGADO", 130);
        pagado.setCellFactory(c -> new CeldaPagado());
        TableColumn<TratamientoResumen, TratamientoResumen> estado = columna("ESTADO", 110);
        estado.setCellFactory(c -> new CeldaEstado());
        tabla.getColumns().setAll(List.of(
                columnaTexto("FECHA", 90, r -> Formatos.fechaCorta(r.tratamiento().getFecha())),
                paciente,
                columnaTexto("TRATAMIENTO", 170, r -> r.tratamiento().getDescripcion() + piezas(r.tratamiento())),
                columnaTexto("COSTO", 90, r -> Formatos.dinero(r.tratamiento().getCosto())),
                pagado,
                estado));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion) {
                mostrarDetalle(nuevo);
            }
        });
    }

    private void cargar() {
        TratamientoResumen elegido = tabla.getSelectionModel().getSelectedItem();
        EstadoTratamiento estado = (EstadoTratamiento) grupoFiltro.getSelectedToggle().getUserData();
        try {
            filas.setAll(servicio.listar(estado, campoBuscar.getText()));
            resumen.setText(filas.size() + (filas.size() == 1 ? " tratamiento" : " tratamientos")
                    + (puedeEscribir ? " · registra uno nuevo desde una cita." : " · solo consulta."));
        } catch (RuntimeException e) {
            filas.clear();
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar los tratamientos", e.getMessage()));
        }
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            if (elegido != null) {
                filas.stream().filter(r -> r.tratamiento().getIdTratamiento() == elegido.tratamiento().getIdTratamiento())
                        .findFirst().ifPresent(r -> tabla.getSelectionModel().select(r));
            }
        } finally {
            ignorarSeleccion = false;
        }
    }

    @FXML
    private void nuevo() {
        mostrarFormulario(null);
    }

    // ===== Formulario =====

    private void construirFormulario() {
        campoCita.setMaxWidth(Double.MAX_VALUE);
        campoCita.setPromptText("Elige la cita");
        campoCita.setCellFactory(lista -> new CeldaCita());
        campoCita.setButtonCell(new CeldaCita());
        campoDescripcion.setPromptText("Ej. Endodoncia pieza 36");
        campoPiezas.setPromptText("Ej. 36, 47 (opcional)");
        campoCosto.setPromptText("Ej. 650.00");

        campos.put("cita", campo("Cita", true, campoCita));
        campos.put("descripcion", campo("Descripción", true, campoDescripcion));
        campos.put("piezas", campo("Piezas dentales (notación FDI)", false, campoPiezas));
        campos.put("costo", campo("Costo (S/)", true, campoCosto));

        HBox estado = new HBox(filtroEstado("En curso", EstadoTratamiento.EN_CURSO),
                filtroEstado("Finalizado", EstadoTratamiento.FINALIZADO));
        estado.getStyleClass().add("segmentado");
        estado.setMaxWidth(Region.USE_PREF_SIZE);
        grupoEstado.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            }
        });
        CampoFormulario cfEstado = campo("Estado", false, estado);
        campos.put("estado", cfEstado);

        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextField texto) {
                texto.textProperty().addListener((obs, antes, ahora) -> campo.limpiarError());
            }
        }
        campoCita.valueProperty().addListener((obs, antes, ahora) -> campos.get("cita").limpiarError());
    }

    private ToggleButton filtroEstado(String texto, EstadoTratamiento estado) {
        ToggleButton boton = new ToggleButton(texto);
        boton.setUserData(estado);
        boton.setToggleGroup(grupoEstado);
        return boton;
    }

    private static CampoFormulario campo(String etiqueta, boolean obligatorio, Node control) {
        CampoFormulario campo = new CampoFormulario();
        campo.setEtiqueta(etiqueta);
        campo.setObligatorio(obligatorio);
        campo.setControl(control);
        return campo;
    }

    private void mostrarFormulario(Integer piezaInicial) {
        if (!puedeEscribir) {
            return;
        }
        ignorarSeleccion = true;
        tabla.getSelectionModel().clearSelection();
        ignorarSeleccion = false;
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoFormulario.ocultar();
        List<Cita> citas = servicio.citasParaRegistrar();
        campoCita.getItems().setAll(citas);
        campoCita.setValue(citas.isEmpty() ? null : citas.get(0));
        campoDescripcion.clear();
        campoPiezas.setText(piezaInicial == null ? "" : String.valueOf(piezaInicial));
        campoCosto.clear();
        grupoEstado.selectToggle(grupoEstado.getToggles().get(0));

        Label titulo = new Label("Nuevo tratamiento");
        titulo.getStyleClass().add("panel-titulo");
        Label subtitulo = new Label(citas.isEmpty()
                ? "No hay citas de hoy ni de los últimos " + TratamientoService.DIAS_ATRAS + " días para vincular."
                : "Se vincula a una cita de hoy o de los últimos " + TratamientoService.DIAS_ATRAS + " días.");
        subtitulo.getStyleClass().add("texto-secundario");
        subtitulo.setWrapText(true);

        Button cancelar = new Button("Limpiar");
        cancelar.getStyleClass().add("btn-secundario");
        cancelar.setOnAction(e -> mostrarFormulario(null));
        Button guardar = new Button("Registrar tratamiento");
        guardar.getStyleClass().add("btn-primario");
        guardar.setDefaultButton(true);
        guardar.setDisable(citas.isEmpty());
        guardar.setOnAction(e -> registrar());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(espacio, cancelar, guardar);
        pie.getStyleClass().add("panel-pie");

        VBox formulario = new VBox(12);
        formulario.getChildren().addAll(campos.values());
        formulario.getChildren().add(avisoFormulario);
        formulario.getStyleClass().add("ficha-formulario");
        panel.getChildren().setAll(new VBox(4, titulo, subtitulo), formulario, pie);
        campoDescripcion.requestFocus();
    }

    private void registrar() {
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoFormulario.ocultar();
        Cita cita = campoCita.getValue();
        if (cita == null) {
            campos.get("cita").mostrarError("Elige la cita del tratamiento.");
            return;
        }
        List<Integer> piezas = leerPiezas();
        Double costo = Formatos.parsearMonto(campoCosto.getText());
        if (piezas == null || costo == null) {
            if (piezas == null) {
                campos.get("piezas").mostrarError("Escribe números separados por coma, ej. 36, 47.");
            }
            if (costo == null) {
                campos.get("costo").mostrarError(campoCosto.getText().isBlank() ? "El costo es obligatorio."
                        : "Escribe un monto válido, ej. 650.00.");
            }
            return;
        }
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setIdCita(cita.getIdCita());
        tratamiento.setDescripcion(campoDescripcion.getText());
        tratamiento.setCosto(costo);
        tratamiento.setEstado((EstadoTratamiento) grupoEstado.getSelectedToggle().getUserData());
        tratamiento.setPiezasDentales(piezas);
        try {
            Tratamiento guardado = servicio.registrar(tratamiento);
            notificar("Tratamiento registrado");
            seleccionarFiltro(null);
            campoBuscar.clear();
            cargar();
            servicio.buscarPorId(guardado.getIdTratamiento()).ifPresent(r -> {
                filas.stream().filter(f -> f.tratamiento().getIdTratamiento() == r.tratamiento().getIdTratamiento())
                        .findFirst().ifPresent(f -> tabla.getSelectionModel().select(f));
            });
        } catch (ValidacionException e) {
            e.getErrores().forEach((nombre, mensaje) -> mostrarError(nombre.startsWith("piezas") ? "piezas" : nombre,
                    mensaje));
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(null), e.getMessage());
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    /** "36, 47" → [36, 47]; vacío → []; null si algún valor no es un número. */
    private List<Integer> leerPiezas() {
        List<Integer> piezas = new ArrayList<>();
        String texto = campoPiezas.getText() == null ? "" : campoPiezas.getText().strip();
        if (texto.isEmpty()) {
            return piezas;
        }
        for (String valor : texto.split("[,\\s]+")) {
            if (!valor.matches("\\d{1,3}")) {
                return null;
            }
            piezas.add(Integer.parseInt(valor));
        }
        return piezas;
    }

    private void mostrarError(String campo, String mensaje) {
        CampoFormulario destino = campo == null ? null : campos.get(campo);
        if (destino != null) {
            destino.mostrarError(mensaje);
        } else {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, mensaje);
        }
    }

    // ===== Detalle =====

    private void mostrarVacio() {
        panel.getChildren().setAll(new EstadoVacio("Selecciona un tratamiento",
                "Verás su detalle, lo pagado y el saldo pendiente."));
    }

    private void mostrarDetalle(TratamientoResumen resumen) {
        Tratamiento t = resumen.tratamiento();
        Avatar avatar = new Avatar(resumen.paciente().getIniciales(), resumen.paciente().getIdPaciente());
        avatar.getStyleClass().add("avatar-grande");
        Label titulo = new Label(t.getDescripcion());
        titulo.getStyleClass().add("panel-titulo");
        titulo.setWrapText(true);
        Label paciente = new Label(resumen.paciente().getNombreCompleto());
        paciente.getStyleClass().add("texto-secundario");
        VBox titulos = new VBox(4, titulo, paciente);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        HBox encabezado = new HBox(14, avatar, titulos, new Pill(t.getEstado()));
        encabezado.setAlignment(Pos.CENTER_LEFT);

        VBox datos = new VBox(8,
                fila("Fecha", Formatos.fechaCorta(t.getFecha())),
                fila("Profesional", resumen.cita().getProfesional().getNombreCompleto()),
                fila("Piezas", t.getPiezasDentales() == null || t.getPiezasDentales().isEmpty() ? "—"
                        : String.join(", ", t.getPiezasDentales().stream().map(String::valueOf).toList())),
                fila("Costo", Formatos.dinero(t.getCosto())),
                fila("Pagado", Formatos.dinero(resumen.pagado())),
                fila("Saldo", Formatos.dinero(resumen.saldo())));
        ProgressBar barra = new ProgressBar(t.getCosto() <= 0 ? 0 : Math.min(1, resumen.pagado() / t.getCosto()));
        barra.setMaxWidth(Double.MAX_VALUE);
        datos.getChildren().add(barra);

        VBox contenido = new VBox(16, encabezado, datos);
        contenido.getStyleClass().add("ficha-formulario");
        if (puedeCobrar) {
            contenido.getChildren().add(listaDePagos(t.getIdTratamiento()));
        }

        HBox pie = new HBox(8);
        pie.getStyleClass().add("panel-pie");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        pie.getChildren().add(espacio);
        if (puedeCobrar && !resumen.estaPagado()) {
            Button cobrar = new Button("Registrar pago");
            cobrar.getStyleClass().add(puedeEscribir ? "btn-secundario" : "btn-primario");
            cobrar.setOnAction(e -> MainController.actual().navegar(Pantalla.PAGOS, resumen));
            pie.getChildren().add(cobrar);
        }
        if (puedeEscribir && t.getEstado() == EstadoTratamiento.EN_CURSO) {
            Button finalizar = new Button("Marcar como finalizado");
            finalizar.getStyleClass().add("btn-primario");
            finalizar.setOnAction(e -> finalizar(resumen));
            pie.getChildren().add(finalizar);
        }
        panel.getChildren().setAll(contenido, pie);
    }

    private Node listaDePagos(int idTratamiento) {
        Label titulo = new Label("PAGOS");
        titulo.getStyleClass().add("seccion-titulo");
        VBox lista = new VBox(6, titulo);
        List<Pago> lista1;
        try {
            lista1 = pagos.pagosDe(idTratamiento);
        } catch (RuntimeException e) {
            lista.getChildren().add(new Label(e.getMessage()));
            return lista;
        }
        if (lista1.isEmpty()) {
            Label ninguno = new Label("Aún no tiene pagos.");
            ninguno.getStyleClass().add("texto-tenue");
            lista.getChildren().add(ninguno);
        }
        for (Pago pago : lista1) {
            lista.getChildren().add(fila(Formatos.fechaCorta(pago.getFechaPago()) + " · "
                    + pago.getMetodoPago().getEtiqueta(), Formatos.dinero(pago.getMonto())));
        }
        return lista;
    }

    private void finalizar(TratamientoResumen resumen) {
        if (!Dialogos.confirmar(tabla.getScene().getWindow(), "Finalizar tratamiento",
                "¿Marcar \"" + resumen.tratamiento().getDescripcion() + "\" como finalizado?",
                resumen.saldo() > 0 ? "Aún tiene un saldo de " + Formatos.dinero(resumen.saldo()) + " por cobrar."
                        : "El tratamiento está pagado por completo.", "Finalizar")) {
            return;
        }
        try {
            servicio.finalizar(resumen.tratamiento().getIdTratamiento());
            notificar("Tratamiento finalizado");
            cargar();
            servicio.buscarPorId(resumen.tratamiento().getIdTratamiento()).ifPresent(this::mostrarDetalle);
        } catch (RuntimeException e) {
            Notificacion.mostrar(tabla.getScene().getWindow(), e.getMessage());
        }
    }

    private static Node fila(String etiqueta, String valor) {
        Label izquierda = new Label(etiqueta);
        izquierda.getStyleClass().add("texto-secundario");
        Label derecha = new Label(valor);
        derecha.getStyleClass().add("celda-titulo");
        derecha.setWrapText(true);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return new HBox(8, izquierda, espacio, derecha);
    }

    // ===== Utilidades y celdas =====

    private static String piezas(Tratamiento t) {
        if (t.getPiezasDentales() == null || t.getPiezasDentales().isEmpty()) {
            return "";
        }
        return " · " + String.join(", ", t.getPiezasDentales().stream().map(String::valueOf).toList());
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(tabla.getScene().getWindow(), mensaje);
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    private static TableColumn<TratamientoResumen, TratamientoResumen> columna(String titulo, double ancho) {
        TableColumn<TratamientoResumen, TratamientoResumen> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static TableColumn<TratamientoResumen, TratamientoResumen> columnaTexto(String titulo, double ancho,
            Function<TratamientoResumen, String> texto) {
        TableColumn<TratamientoResumen, TratamientoResumen> columna = columna(titulo, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(TratamientoResumen resumen, boolean vacio) {
                super.updateItem(resumen, vacio);
                setText(vacio || resumen == null ? null : texto.apply(resumen));
            }
        });
        return columna;
    }

    /** Avatar + nombre del paciente. */
    private static class CeldaPaciente extends TableCell<TratamientoResumen, TratamientoResumen> {
        private final Avatar avatar = new Avatar();
        private final Label nombre = new Label();
        private final HBox contenido = new HBox(avatar, nombre);

        CeldaPaciente() {
            nombre.getStyleClass().add("celda-titulo");
            contenido.getStyleClass().add("celda-paciente");
        }

        @Override
        protected void updateItem(TratamientoResumen resumen, boolean vacio) {
            super.updateItem(resumen, vacio);
            setText(null);
            if (vacio || resumen == null) {
                setGraphic(null);
                return;
            }
            avatar.mostrar(resumen.paciente().getIniciales(), resumen.paciente().getIdPaciente());
            nombre.setText(resumen.paciente().getNombreCompleto());
            setGraphic(contenido);
        }
    }

    /** Barra de progreso + "S/ 250 de S/ 650". */
    private static class CeldaPagado extends TableCell<TratamientoResumen, TratamientoResumen> {
        private final ProgressBar barra = new ProgressBar();
        private final Label texto = new Label();
        private final VBox contenido = new VBox(4, barra, texto);

        CeldaPagado() {
            barra.setMaxWidth(Double.MAX_VALUE);
            texto.getStyleClass().add("celda-sub");
        }

        @Override
        protected void updateItem(TratamientoResumen resumen, boolean vacio) {
            super.updateItem(resumen, vacio);
            setText(null);
            if (vacio || resumen == null) {
                setGraphic(null);
                return;
            }
            double costo = resumen.tratamiento().getCosto();
            barra.setProgress(costo <= 0 ? 0 : Math.min(1, resumen.pagado() / costo));
            texto.setText(Formatos.dinero(resumen.pagado()) + " de " + Formatos.dinero(costo));
            setGraphic(contenido);
        }
    }

    private static class CeldaEstado extends TableCell<TratamientoResumen, TratamientoResumen> {
        @Override
        protected void updateItem(TratamientoResumen resumen, boolean vacio) {
            super.updateItem(resumen, vacio);
            setText(null);
            setGraphic(vacio || resumen == null ? null : new Pill(resumen.tratamiento().getEstado()));
        }
    }

    /** "29 oct 2026 · María Quispe · Control". */
    private static class CeldaCita extends ListCell<Cita> {
        @Override
        protected void updateItem(Cita cita, boolean vacio) {
            super.updateItem(cita, vacio);
            setText(vacio || cita == null ? null : Formatos.fechaCorta(cita.getFecha()) + " · "
                    + cita.getPaciente().getNombreCompleto()
                    + (cita.getMotivo() == null ? "" : " · " + cita.getMotivo()));
        }
    }
}
