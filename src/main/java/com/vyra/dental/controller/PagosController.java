package com.vyra.dental.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.service.PagoDetalle;
import com.vyra.dental.service.PagoService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.TratamientoResumen;
import com.vyra.dental.service.TratamientoService;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Icono;
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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Pagos (docs/08-guia-frontend.md §7.8, CU-10, RN-12). Arriba, las tarjetas del periodo (Hoy,
 * Semana o Mes); a la izquierda, los pagos del periodo; a la derecha, el panel "Registrar pago".
 * Se puede abrir con un {@link TratamientoResumen} para cobrarlo directamente.
 */
public class PagosController implements RecibeParametro {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;

    /** Periodos del selector; cada uno sabe su rango y el anterior con el que se compara. */
    private enum Periodo {
        HOY("Hoy", "ayer"), SEMANA("Semana", "la semana pasada"), MES("Mes", "el mes pasado");

        private final String etiqueta;
        private final String anteriorTexto;

        Periodo(String etiqueta, String anteriorTexto) {
            this.etiqueta = etiqueta;
            this.anteriorTexto = anteriorTexto;
        }

        LocalDate desde(LocalDate hoy) {
            return switch (this) {
                case HOY -> hoy;
                case SEMANA -> hoy.with(DayOfWeek.MONDAY);
                case MES -> hoy.withDayOfMonth(1);
            };
        }

        LocalDate anteriorDesde(LocalDate hoy) {
            return switch (this) {
                case HOY -> hoy.minusDays(1);
                case SEMANA -> desde(hoy).minusWeeks(1);
                case MES -> desde(hoy).minusMonths(1);
            };
        }

        LocalDate anteriorHasta(LocalDate hoy) {
            return desde(hoy).minusDays(1);
        }
    }

    @FXML private HBox periodos;
    @FXML private HBox tarjetas;
    @FXML private TableView<PagoDetalle> tabla;
    @FXML private VBox panel;

    private final PagoService servicio = Servicios.pagos();
    private final TratamientoService tratamientos = Servicios.tratamientos();
    private final ObservableList<PagoDetalle> filas = FXCollections.observableArrayList();
    private final ToggleGroup grupoPeriodo = new ToggleGroup();

    // Panel "Registrar pago"
    private final Map<String, CampoFormulario> campos = new LinkedHashMap<>();
    private final TextField campoBuscar = new TextField();
    private final ListView<TratamientoResumen> pendientes = new ListView<>();
    private final VBox tarjetaTratamiento = new VBox(8);
    private final TextField campoMonto = new TextField();
    private final DatePicker campoFecha = new DatePicker();
    private final TextField campoOperacion = new TextField();
    private final ToggleGroup grupoMetodo = new ToggleGroup();
    private final Label saldoDespues = new Label();
    private final Button botonRegistrar = new Button();
    private final Aviso aviso = new Aviso();
    private TratamientoResumen elegido;

    @FXML
    private void initialize() {
        construirPeriodos();
        construirTabla();
        construirPanel();
        refrescarPeriodo();
        mostrarTratamiento(null);
    }

    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof TratamientoResumen resumen) {
            // Se vuelve a leer: el saldo pudo cambiar desde que se abrió la otra pantalla
            tratamientos.buscarPorId(resumen.tratamiento().getIdTratamiento()).ifPresent(this::mostrarTratamiento);
        }
    }

    // ===== Periodo, tarjetas y tabla =====

    private void construirPeriodos() {
        for (Periodo periodo : Periodo.values()) {
            ToggleButton boton = new ToggleButton(periodo == Periodo.MES ? nombreDelMes() : periodo.etiqueta);
            boton.setUserData(periodo);
            boton.setToggleGroup(grupoPeriodo);
            periodos.getChildren().add(boton);
        }
        grupoPeriodo.selectToggle(grupoPeriodo.getToggles().get(Periodo.MES.ordinal()));
        grupoPeriodo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else {
                refrescarPeriodo();
            }
        });
    }

    /** "Octubre": el mes actual con mayúscula inicial. */
    private static String nombreDelMes() {
        String mes = Formatos.mesAnio(LocalDate.now()).split(" ")[0];
        return Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
    }

    private Periodo periodo() {
        return (Periodo) grupoPeriodo.getSelectedToggle().getUserData();
    }

    private void refrescarPeriodo() {
        Periodo periodo = periodo();
        LocalDate hoy = LocalDate.now();
        LocalDate desde = periodo.desde(hoy);
        try {
            filas.setAll(servicio.pagosEntre(desde, hoy));
            double cobrado = servicio.cobradoEntre(desde, hoy);
            double anterior = servicio.cobradoEntre(periodo.anteriorDesde(hoy), periodo.anteriorHasta(hoy));
            tarjetas.getChildren().setAll(
                    tarjetaCobrado(periodo, cobrado, anterior),
                    tarjetaSaldo(servicio.saldoPendiente()),
                    tarjetaMetodos(servicio.cobradoPorMetodo(desde, hoy), cobrado));
        } catch (RuntimeException e) {
            filas.clear();
            tarjetas.getChildren().setAll(new EstadoVacio("No se pudo cargar los pagos", e.getMessage()));
        }
    }

    private Node tarjetaCobrado(Periodo periodo, double cobrado, double anterior) {
        String titulo = switch (periodo) {
            case HOY -> "Cobrado hoy";
            case SEMANA -> "Cobrado esta semana";
            case MES -> "Cobrado en el mes";
        };
        String comparacion;
        if (anterior <= 0) {
            comparacion = "Sin cobros " + (periodo == Periodo.HOY ? "ayer" : "en el periodo anterior") + " para comparar";
        } else {
            long cambio = Math.round((cobrado - anterior) / anterior * 100);
            comparacion = (cambio >= 0 ? "+" : "") + cambio + "% respecto a " + periodo.anteriorTexto;
        }
        return kpi(titulo, Formatos.dinero(cobrado), comparacion);
    }

    private Node tarjetaSaldo(PagoService.SaldoPendiente saldo) {
        return kpi("Saldo pendiente de tratamientos", Formatos.dinero(saldo.total()),
                "En " + saldo.tratamientos() + (saldo.tratamientos() == 1 ? " tratamiento" : " tratamientos"));
    }

    private Node tarjetaMetodos(Map<MetodoPago, Double> porMetodo, double total) {
        Label titulo = new Label("Métodos de pago");
        titulo.getStyleClass().add("texto-secundario");
        VBox contenido = new VBox(10, titulo);
        if (total <= 0) {
            Label vacio = new Label("Sin cobros en el periodo");
            vacio.getStyleClass().add("texto-tenue");
            contenido.getChildren().add(vacio);
        } else {
            GridPane barra = new GridPane();
            barra.getStyleClass().add("metodos-barra");
            VBox leyenda = new VBox(2);
            int columna = 0;
            for (MetodoPago metodo : MetodoPago.values()) {
                double valor = porMetodo.getOrDefault(metodo, 0.0);
                long porcentaje = Math.round(valor / total * 100);
                if (valor > 0) {
                    Region segmento = new Region();
                    segmento.getStyleClass().addAll("metodo-segmento", "metodo-" + metodo.name().toLowerCase());
                    ColumnConstraints restriccion = new ColumnConstraints();
                    restriccion.setPercentWidth(valor / total * 100);
                    barra.getColumnConstraints().add(restriccion);
                    barra.add(segmento, columna++, 0);
                }
                Label etiqueta = new Label(metodo.getEtiqueta() + " " + porcentaje + "%");
                etiqueta.getStyleClass().add("celda-sub");
                leyenda.getChildren().add(etiqueta);
            }
            contenido.getChildren().addAll(barra, leyenda);
        }
        contenido.getStyleClass().addAll("card", "kpi");
        HBox.setHgrow(contenido, Priority.ALWAYS);
        contenido.setMaxWidth(Double.MAX_VALUE);
        return contenido;
    }

    private static Node kpi(String titulo, String cifra, String detalle) {
        Label t = new Label(titulo);
        t.getStyleClass().add("texto-secundario");
        Label valor = new Label(cifra);
        valor.getStyleClass().add("kpi-valor");
        Label d = new Label(detalle);
        d.getStyleClass().add("celda-sub");
        d.setWrapText(true);
        VBox tarjeta = new VBox(6, t, valor, d);
        tarjeta.getStyleClass().add("kpi");
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(4, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.setPlaceholder(new EstadoVacio("Sin pagos en este periodo",
                "Los pagos que registres aparecerán aquí."));

        TableColumn<PagoDetalle, PagoDetalle> paciente = columna("PACIENTE", 190);
        paciente.setCellFactory(c -> new CeldaPaciente());
        TableColumn<PagoDetalle, PagoDetalle> metodo = columna("MÉTODO", 120);
        metodo.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PagoDetalle detalle, boolean vacio) {
                super.updateItem(detalle, vacio);
                setText(null);
                setGraphic(vacio || detalle == null ? null : new Pill(detalle.pago().getMetodoPago()));
            }
        });
        TableColumn<PagoDetalle, PagoDetalle> monto = columnaTexto("MONTO", 100,
                d -> Formatos.dinero(d.pago().getMonto()));
        monto.getStyleClass().add("columna-derecha");
        tabla.getColumns().setAll(List.of(
                columnaTexto("FECHA", 90, d -> Formatos.fechaCorta(d.pago().getFechaPago())),
                paciente,
                columnaTexto("CONCEPTO", 170, d -> d.tratamiento() == null ? "—" : d.tratamiento().getDescripcion()),
                metodo,
                monto));
    }

    // ===== Panel "Registrar pago" =====

    private void construirPanel() {
        campoBuscar.setPromptText("Buscar tratamiento por paciente o DNI");
        campoBuscar.textProperty().addListener((obs, antes, ahora) -> cargarPendientes());
        pendientes.setPrefHeight(132);
        pendientes.setMinHeight(132);
        pendientes.setPlaceholder(new Label("Sin tratamientos con saldo pendiente"));
        pendientes.setCellFactory(lista -> new CeldaPendiente());
        pendientes.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null && (elegido == null
                    || elegido.tratamiento().getIdTratamiento() != ahora.tratamiento().getIdTratamiento())) {
                mostrarTratamiento(ahora);
            }
        });
        campos.put("tratamiento", campo("Tratamiento", true, new VBox(8, campoBuscar, pendientes, tarjetaTratamiento)));

        campoMonto.setPromptText("0.00");
        campoMonto.textProperty().addListener((obs, antes, ahora) -> {
            campos.get("monto").limpiarError();
            actualizarPie();
        });
        campos.put("monto", campo("Monto (S/)", true, campoMonto));

        campoFecha.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : Formatos.formatoFechaCampo().format(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                try {
                    return texto == null || texto.isBlank() ? null
                            : LocalDate.parse(texto.strip(), Formatos.formatoFechaCampo());
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        campoFecha.setDayCellFactory(selector -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha.isAfter(LocalDate.now()));
            }
        });
        campoFecha.setMaxWidth(Double.MAX_VALUE);
        campoFecha.valueProperty().addListener((obs, antes, ahora) -> campos.get("fechaPago").limpiarError());
        campos.put("fechaPago", campo("Fecha de pago", true, campoFecha));

        GridPane metodos = new GridPane();
        metodos.setHgap(8);
        metodos.setVgap(8);
        metodos.getColumnConstraints().addAll(porcentaje(50), porcentaje(50));
        int i = 0;
        for (MetodoPago metodo : MetodoPago.values()) {
            ToggleButton boton = new ToggleButton(metodo.getEtiqueta(), new Icono(iconoDe(metodo), 16));
            boton.setContentDisplay(ContentDisplay.LEFT);
            boton.getStyleClass().add("metodo-pago");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setUserData(metodo);
            boton.setToggleGroup(grupoMetodo);
            metodos.add(boton, i % 2, i / 2);
            i++;
        }
        grupoMetodo.selectToggle(grupoMetodo.getToggles().get(0));
        grupoMetodo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            }
        });
        campos.put("metodoPago", campo("Método de pago", true, metodos));

        campoOperacion.setPromptText("Yape, tarjeta o transferencia");
        campos.put("numeroOperacion", campo("N.º de operación (opcional)", false, campoOperacion));

        saldoDespues.getStyleClass().add("texto-secundario");
        botonRegistrar.getStyleClass().add("btn-primario");
        botonRegistrar.setMaxWidth(Double.MAX_VALUE);
        botonRegistrar.setDefaultButton(true);
        botonRegistrar.setOnAction(e -> registrar());
    }

    private void mostrarTratamiento(TratamientoResumen resumen) {
        elegido = resumen;
        campos.values().forEach(CampoFormulario::limpiarError);
        aviso.ocultar();
        campoMonto.clear();
        campoOperacion.clear();
        campoFecha.setValue(LocalDate.now());
        grupoMetodo.selectToggle(grupoMetodo.getToggles().get(0));
        cargarPendientes();
        tarjetaTratamiento.getChildren().clear();
        if (resumen != null) {
            pendientes.getSelectionModel().clearSelection();
            tarjetaTratamiento.getChildren().add(detalleTratamiento(resumen));
            campoMonto.setText(String.format(Locale.US, "%.2f", resumen.saldo()));
        }
        reconstruirPanel();
        actualizarPie();
    }

    private Node detalleTratamiento(TratamientoResumen resumen) {
        Avatar avatar = new Avatar(resumen.paciente().getIniciales(), resumen.paciente().getIdPaciente());
        Label descripcion = new Label(resumen.tratamiento().getDescripcion());
        descripcion.getStyleClass().add("celda-titulo");
        descripcion.setWrapText(true);
        Label quien = new Label(resumen.paciente().getNombreCompleto());
        quien.getStyleClass().add("celda-sub");
        VBox textos = new VBox(2, descripcion, quien);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Label costo = new Label(Formatos.dinero(resumen.tratamiento().getCosto()));
        costo.getStyleClass().add("celda-titulo");
        HBox cabecera = new HBox(10, avatar, textos, new Pill(resumen.tratamiento().getEstado()), costo);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        ProgressBar barra = new ProgressBar(Math.min(1, resumen.pagado() / resumen.tratamiento().getCosto()));
        barra.setMaxWidth(Double.MAX_VALUE);
        Label detalle = new Label("Pagado " + Formatos.dinero(resumen.pagado()) + " · Saldo "
                + Formatos.dinero(resumen.saldo()));
        detalle.getStyleClass().add("celda-sub");
        VBox tarjeta = new VBox(8, cabecera, barra, detalle);
        tarjeta.getStyleClass().add("tratamiento-elegido");
        return tarjeta;
    }

    private void cargarPendientes() {
        try {
            pendientes.getItems().setAll(tratamientos.listar(null, campoBuscar.getText()).stream()
                    .filter(r -> !r.estaPagado()).limit(30).toList());
        } catch (RuntimeException e) {
            pendientes.getItems().clear();
        }
    }

    private void reconstruirPanel() {
        Label titulo = new Label("Registrar pago");
        titulo.getStyleClass().add("panel-titulo");
        VBox formulario = new VBox(12);
        formulario.getChildren().addAll(campos.values());
        formulario.getChildren().add(aviso);
        formulario.getStyleClass().add("ficha-formulario");
        VBox pie = new VBox(8, saldoDespues, botonRegistrar);
        pie.getStyleClass().add("panel-pie");
        pie.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().setAll(titulo, formulario, pie);
    }

    /** "Saldo después de este pago" y el texto del botón, según el monto escrito. */
    private void actualizarPie() {
        Double monto = Formatos.parsearMonto(campoMonto.getText());
        boolean valido = elegido != null && monto != null && monto > 0;
        botonRegistrar.setDisable(elegido == null);
        if (!valido) {
            saldoDespues.setText(elegido == null ? "Elige el tratamiento que se paga."
                    : "Saldo actual: " + Formatos.dinero(elegido.saldo()));
            botonRegistrar.setText("Registrar pago");
            return;
        }
        double despues = Math.round((elegido.saldo() - monto) * 100) / 100.0;
        saldoDespues.setText(despues < 0 ? "El monto supera el saldo pendiente (" + Formatos.dinero(elegido.saldo()) + ")"
                : "Saldo después de este pago: " + Formatos.dinero(despues));
        botonRegistrar.setText("Registrar pago de " + Formatos.dinero(monto));
    }

    private void registrar() {
        campos.values().forEach(CampoFormulario::limpiarError);
        aviso.ocultar();
        if (elegido == null) {
            campos.get("tratamiento").mostrarError("Elige el tratamiento que se paga.");
            return;
        }
        Double monto = Formatos.parsearMonto(campoMonto.getText());
        if (monto == null) {
            campos.get("monto").mostrarError(campoMonto.getText().isBlank() ? "El monto es obligatorio."
                    : "Escribe un monto válido, ej. 250.00.");
            return;
        }
        Pago pago = new Pago();
        pago.setIdTratamiento(elegido.tratamiento().getIdTratamiento());
        pago.setMonto(monto);
        pago.setFechaPago(campoFecha.getValue());
        pago.setMetodoPago((MetodoPago) grupoMetodo.getSelectedToggle().getUserData());
        pago.setNumeroOperacion(campoOperacion.getText());
        try {
            servicio.registrar(pago);
            Notificacion.mostrar(tabla.getScene().getWindow(), "Pago de " + Formatos.dinero(monto) + " registrado");
            int id = elegido.tratamiento().getIdTratamiento();
            refrescarPeriodo();
            // Se vuelve a leer el tratamiento para mostrar el saldo nuevo
            tratamientos.buscarPorId(id).ifPresent(r -> {
                if (r.estaPagado()) {
                    mostrarTratamiento(null);
                } else {
                    mostrarTratamiento(r);
                }
            });
        } catch (ValidacionException e) {
            e.getErrores().forEach(this::mostrarError);
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(null), e.getMessage());
        } catch (RuntimeException e) {
            aviso.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    private void mostrarError(String campo, String mensaje) {
        CampoFormulario destino = campo == null ? null : campos.get(campo);
        if (destino != null) {
            destino.mostrarError(mensaje);
        } else {
            aviso.mostrar(Aviso.Tipo.ERROR, mensaje);
        }
    }

    // ===== Utilidades y celdas =====

    private static String iconoDe(MetodoPago metodo) {
        return switch (metodo) {
            case EFECTIVO -> "efectivo";
            case YAPE_PLIN -> "celular";
            case TARJETA -> "pagos";
            case TRANSFERENCIA -> "banco";
        };
    }

    private static ColumnConstraints porcentaje(double valor) {
        ColumnConstraints restriccion = new ColumnConstraints();
        restriccion.setPercentWidth(valor);
        return restriccion;
    }

    private static CampoFormulario campo(String etiqueta, boolean obligatorio, Node control) {
        CampoFormulario campo = new CampoFormulario();
        campo.setEtiqueta(etiqueta);
        campo.setObligatorio(obligatorio);
        campo.setControl(control);
        return campo;
    }

    private static TableColumn<PagoDetalle, PagoDetalle> columna(String titulo, double ancho) {
        TableColumn<PagoDetalle, PagoDetalle> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static TableColumn<PagoDetalle, PagoDetalle> columnaTexto(String titulo, double ancho,
                                                                     Function<PagoDetalle, String> texto) {
        TableColumn<PagoDetalle, PagoDetalle> columna = columna(titulo, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PagoDetalle detalle, boolean vacio) {
                super.updateItem(detalle, vacio);
                setText(vacio || detalle == null ? null : texto.apply(detalle));
                setAlignment(titulo.equals("MONTO") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                getStyleClass().remove("celda-monto");
                if (titulo.equals("MONTO")) {
                    getStyleClass().add("celda-monto");
                }
            }
        });
        return columna;
    }

    /** Avatar + nombre del paciente. */
    private static class CeldaPaciente extends TableCell<PagoDetalle, PagoDetalle> {
        private final Avatar avatar = new Avatar();
        private final Label nombre = new Label();
        private final HBox contenido = new HBox(avatar, nombre);

        CeldaPaciente() {
            nombre.getStyleClass().add("celda-titulo");
            contenido.getStyleClass().add("celda-paciente");
        }

        @Override
        protected void updateItem(PagoDetalle detalle, boolean vacio) {
            super.updateItem(detalle, vacio);
            setText(null);
            if (vacio || detalle == null || detalle.paciente() == null) {
                setGraphic(null);
                return;
            }
            avatar.mostrar(detalle.paciente().getIniciales(), detalle.paciente().getIdPaciente());
            nombre.setText(detalle.paciente().getNombreCompleto());
            setGraphic(contenido);
        }
    }

    /** "Endodoncia pieza 36 · María Quispe — saldo S/ 400.00". */
    private static class CeldaPendiente extends ListCell<TratamientoResumen> {
        @Override
        protected void updateItem(TratamientoResumen resumen, boolean vacio) {
            super.updateItem(resumen, vacio);
            setText(vacio || resumen == null ? null : resumen.tratamiento().getDescripcion() + " · "
                    + resumen.paciente().getNombreCompleto() + " — saldo " + Formatos.dinero(resumen.saldo()));
        }
    }
}
