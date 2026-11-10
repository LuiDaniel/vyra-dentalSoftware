package com.vyra.dental.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.service.PagoService;
import com.vyra.dental.service.PagoService.Periodo;
import com.vyra.dental.service.PagoService.Rango;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.ExportarPdf;
import com.vyra.dental.ui.Icono;
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
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Pagos (docs/08-guia-frontend.md §7.8, CU-10): indicadores del periodo, pagos
 * registrados en él y el panel "Registrar pago". Solo Administrador y Recepción.
 */
public class PagosController implements RecibeParametro {

    private static final double ALTO_FILA = 52;
    private static final double ALTO_ENCABEZADO = 44;
    private static final int FILAS_MINIMAS = 5;
    private static final int MAX_SUGERENCIAS = 8;
    private static final Map<MetodoPago, String> ICONOS = Map.of(MetodoPago.EFECTIVO, "efectivo",
            MetodoPago.YAPE_PLIN, "celular", MetodoPago.TARJETA, "pagos", MetodoPago.TRANSFERENCIA, "banco");

    @FXML private VBox raiz;
    @FXML private HBox selectorPeriodo;
    @FXML private Label tituloCobrado;
    @FXML private Label valorCobrado;
    @FXML private Label comparacionCobrado;
    @FXML private Label valorSaldo;
    @FXML private Label detalleSaldo;
    @FXML private HBox barraMetodos;
    @FXML private FlowPane leyendaMetodos;
    @FXML private Label tituloTabla;
    @FXML private Button botonExportar;
    @FXML private TableView<PagoResumen> tabla;
    @FXML private Label pie;

    @FXML private CampoFormulario cfTratamiento;
    @FXML private HBox buscadorTratamiento;
    @FXML private TextField campoTratamiento;
    @FXML private VBox tarjetaTratamiento;
    @FXML private Label tarjetaDescripcion;
    @FXML private Label tarjetaPaciente;
    @FXML private Label tarjetaCosto;
    @FXML private ProgressBar tarjetaAvance;
    @FXML private Label tarjetaSaldo;
    @FXML private CampoFormulario cfMonto;
    @FXML private TextField campoMonto;
    @FXML private CampoFormulario cfFecha;
    @FXML private DatePicker campoFecha;
    @FXML private CampoFormulario cfMetodo;
    @FXML private GridPane metodos;
    @FXML private CampoFormulario cfOperacion;
    @FXML private TextField campoOperacion;
    @FXML private Aviso avisoPago;
    @FXML private Label saldoDespues;
    @FXML private Button botonRegistrar;

    private final PagoService servicio = Servicios.pagos();
    private final ObservableList<PagoResumen> filas = FXCollections.observableArrayList();
    private final ToggleGroup grupoMetodos = new ToggleGroup();
    private final Map<MetodoPago, ToggleButton> botonesMetodo = new EnumMap<>(MetodoPago.class);
    private final ContextMenu sugerencias = new ContextMenu();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    /** Nombre del campo en el servicio → campo del formulario, para mostrar los errores. */
    private final Map<String, CampoFormulario> campos = new HashMap<>();

    private Periodo periodo = Periodo.MES;
    /** Tratamiento al que se le registra el pago; null si todavía no se eligió. */
    private TratamientoResumen elegido;

    @FXML
    private void initialize() {
        campos.put("idTratamiento", cfTratamiento);
        campos.put("monto", cfMonto);
        campos.put("fechaPago", cfFecha);
        campos.put("metodoPago", cfMetodo);
        campos.put("numeroOperacion", cfOperacion);

        construirSelectorPeriodo();
        construirTabla();
        construirMetodos();
        configurarFormulario();
        configurarBuscador();
        quitarTratamiento();
        cargarPeriodo();
    }

    /** Desde Tratamientos o la ficha del paciente: "Registrar pago" de un tratamiento. */
    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof TratamientoResumen resumen) {
            Servicios.tratamientos().resumen(resumen.getTratamiento().getIdTratamiento()).ifPresent(this::elegirTratamiento);
        }
    }

    // ===== Periodo e indicadores =====

    private void construirSelectorPeriodo() {
        ToggleGroup grupo = new ToggleGroup();
        for (Periodo opcion : Periodo.values()) {
            String texto = switch (opcion) {
                case HOY -> "Hoy";
                case SEMANA -> "Semana";
                case MES -> capitalizar(Formatos.mes(LocalDate.now()));
            };
            ToggleButton boton = new ToggleButton(texto);
            boton.setToggleGroup(grupo);
            boton.setSelected(opcion == periodo);
            boton.setOnAction(e -> {
                boton.setSelected(true);
                if (periodo != opcion) {
                    periodo = opcion;
                    cargarPeriodo();
                }
            });
            selectorPeriodo.getChildren().add(boton);
        }
    }

    private void cargarPeriodo() {
        Rango rango = servicio.rango(periodo);
        try {
            mostrarCobrado(rango);
            mostrarSaldoPendiente();
            mostrarMetodos(servicio.totalesPorMetodo(rango));
            filas.setAll(servicio.pagosEntre(rango));
            double total = filas.stream().mapToDouble(r -> r.getPago().getMonto()).sum();
            pie.setText(filas.isEmpty() ? "Sin pagos"
                    : filas.size() + (filas.size() == 1 ? " pago · " : " pagos · ") + Formatos.dinero(total));
            tabla.setPlaceholder(new EstadoVacio("Sin pagos " + nombrePeriodo(),
                    "Los pagos que registres aparecerán aquí."));
        } catch (RuntimeException e) {
            filas.clear();
            pie.setText("");
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar los pagos", e.getMessage()));
        }
        tituloTabla.setText("Pagos " + nombrePeriodo());
    }

    /** "S/ 18,450.00" y "+8% respecto a octubre". */
    private void mostrarCobrado(Rango rango) {
        tituloCobrado.setText("Cobrado " + nombrePeriodo());
        double total = servicio.totalCobrado(rango);
        double anterior = servicio.totalCobrado(servicio.rangoAnterior(periodo));
        valorCobrado.setText(Formatos.dinero(total));
        comparacionCobrado.getStyleClass().removeAll("kpi-subida", "kpi-bajada");
        if (anterior == 0) {
            comparacionCobrado.setText("Sin cobros " + nombrePeriodoAnterior() + " para comparar");
            return;
        }
        long porcentaje = Math.round((total - anterior) / anterior * 100);
        comparacionCobrado.setText((porcentaje >= 0 ? "+" : "") + porcentaje + "% respecto a " + nombrePeriodoAnteriorCorto());
        comparacionCobrado.getStyleClass().add(porcentaje >= 0 ? "kpi-subida" : "kpi-bajada");
    }

    private void mostrarSaldoPendiente() {
        PagoService.SaldoPendiente saldo = servicio.saldoPendiente();
        valorSaldo.setText(Formatos.dinero(saldo.total()));
        detalleSaldo.setText(switch (saldo.tratamientos()) {
            case 0 -> "Ningún tratamiento con saldo";
            case 1 -> "En 1 tratamiento";
            default -> "En " + saldo.tratamientos() + " tratamientos";
        });
    }

    /** Barra apilada con el % de cada método y su leyenda. */
    private void mostrarMetodos(Map<MetodoPago, Double> totales) {
        barraMetodos.getChildren().clear();
        leyendaMetodos.getChildren().clear();
        double total = totales.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) {
            Region vacio = new Region();
            vacio.getStyleClass().add("segmento-vacio");
            vacio.prefWidthProperty().bind(barraMetodos.widthProperty());
            barraMetodos.getChildren().add(vacio);
            Label nada = new Label("Sin pagos " + nombrePeriodo());
            nada.getStyleClass().add("leyenda-metodo");
            leyendaMetodos.getChildren().add(nada);
            barraMetodos.setAccessibleText("Sin pagos");
            return;
        }
        StringBuilder accesible = new StringBuilder();
        for (MetodoPago metodo : MetodoPago.values()) {
            double monto = totales.getOrDefault(metodo, 0.0);
            if (monto == 0) {
                continue;
            }
            double fraccion = monto / total;
            Region segmento = new Region();
            segmento.getStyleClass().addAll("segmento-metodo", metodo.getClaseCss());
            segmento.setMinWidth(0);
            // Restan los 2 px de separación entre segmentos
            segmento.prefWidthProperty().bind(Bindings.max(0, barraMetodos.widthProperty()
                    .subtract(2 * (totales.size() - 1)).multiply(fraccion)));
            barraMetodos.getChildren().add(segmento);

            Region cuadro = new Region();
            cuadro.getStyleClass().addAll("leyenda-cuadro", metodo.getClaseCss());
            String texto = metodo.getEtiqueta() + " " + Math.round(fraccion * 100) + "%";
            Label item = new Label(texto, cuadro);
            item.getStyleClass().add("leyenda-metodo");
            leyendaMetodos.getChildren().add(item);
            accesible.append(texto).append(". ");
        }
        barraMetodos.setAccessibleText(accesible.toString().strip());
    }

    /** "hoy", "esta semana", "en noviembre". */
    private String nombrePeriodo() {
        return switch (periodo) {
            case HOY -> "hoy";
            case SEMANA -> "esta semana";
            case MES -> "en " + Formatos.mes(LocalDate.now());
        };
    }

    private String nombrePeriodoAnterior() {
        return switch (periodo) {
            case HOY -> "ayer";
            case SEMANA -> "la semana pasada";
            case MES -> "en " + nombrePeriodoAnteriorCorto();
        };
    }

    private String nombrePeriodoAnteriorCorto() {
        return switch (periodo) {
            case HOY -> "ayer";
            case SEMANA -> "la semana pasada";
            case MES -> Formatos.mes(LocalDate.now().minusMonths(1));
        };
    }

    /** Los pagos del periodo elegido, en PDF. */
    @FXML
    private void exportar() {
        Rango rango = servicio.rango(periodo);
        String nombre = nombrePeriodo();
        ExportarPdf.exportar(botonExportar, Servicios.pdf().nombreArchivo("Pagos", rango.desde() + "-" + rango.hasta()),
                salida -> Servicios.pdf().pagos(rango, nombre, salida),
                mensaje -> avisoPago.mostrar(Aviso.Tipo.ERROR, mensaje));
    }

    // ===== Tabla =====

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var alto = Bindings.max(FILAS_MINIMAS, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);

        TableColumn<PagoResumen, PagoResumen> fecha = columnaTexto("FECHA", 70,
                r -> Formatos.diaMes(r.getPago().getFechaPago()), null);
        fecha.setMinWidth(80);
        TableColumn<PagoResumen, PagoResumen> paciente = columnaTexto("PACIENTE", 160,
                r -> r.getPaciente().getNombreCompleto(), "celda-titulo");
        TableColumn<PagoResumen, PagoResumen> concepto = columnaTexto("CONCEPTO", 150, PagoResumen::getConcepto, null);

        TableColumn<PagoResumen, PagoResumen> metodo = columna("MÉTODO", 120);
        // El método y el monto se leen completos; se acortan el paciente y el concepto
        metodo.setMinWidth(118);
        metodo.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PagoResumen r, boolean vacio) {
                super.updateItem(r, vacio);
                setText(null);
                setGraphic(vacio || r == null ? null : new Pill(r.getPago().getMetodoPago()));
            }
        });

        TableColumn<PagoResumen, PagoResumen> monto = columnaTexto("MONTO", 100,
                r -> Formatos.dinero(r.getPago().getMonto()), "celda-monto");
        monto.getStyleClass().add("columna-derecha");
        monto.setMinWidth(96);

        tabla.getColumns().setAll(List.of(fecha, paciente, concepto, metodo, monto));
    }

    // ===== Registrar pago =====

    private void construirMetodos() {
        int i = 0;
        for (MetodoPago metodo : MetodoPago.values()) {
            ToggleButton boton = new ToggleButton(metodo.getEtiqueta(), new Icono(ICONOS.get(metodo), 18));
            boton.getStyleClass().add("boton-metodo");
            boton.setToggleGroup(grupoMetodos);
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setUserData(metodo);
            botonesMetodo.put(metodo, boton);
            metodos.add(boton, i % 2, i / 2);
            i++;
        }
        grupoMetodos.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                // Siempre queda un método elegido
                anterior.setSelected(true);
            }
            cfMetodo.limpiarError();
        });
        botonesMetodo.get(MetodoPago.EFECTIVO).setSelected(true);
    }

    private void configurarFormulario() {
        campoFecha.setValue(LocalDate.now());
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
                    // Se deja el texto tal cual; registrar() muestra el error bajo el campo
                    return campoFecha.getValue();
                }
            }
        });
        // RN-12: no se puede elegir una fecha futura
        campoFecha.setDayCellFactory(selector -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha.isAfter(LocalDate.now()));
            }
        });
        campoMonto.textProperty().addListener((obs, anterior, nuevo) -> {
            cfMonto.limpiarError();
            actualizarPie();
        });
        campoFecha.getEditor().textProperty().addListener((obs, anterior, nuevo) -> cfFecha.limpiarError());
        campoOperacion.textProperty().addListener((obs, anterior, nuevo) -> cfOperacion.limpiarError());
        for (TextField campo : List.of(campoMonto, campoOperacion)) {
            campo.setOnAction(e -> {
                registrar();
                e.consume();
            });
        }
    }

    private void configurarBuscador() {
        sugerencias.getStyleClass().add("sugerencias");
        esperaBusqueda.setOnFinished(e -> mostrarSugerencias());
        campoTratamiento.textProperty().addListener((obs, anterior, nuevo) -> {
            cfTratamiento.limpiarError();
            esperaBusqueda.playFromStart();
        });
        campoTratamiento.setOnAction(e -> {
            esperaBusqueda.stop();
            mostrarSugerencias();
            e.consume();
        });
        // Al entrar al campo se ofrecen los tratamientos con saldo, sin tener que escribir
        campoTratamiento.focusedProperty().addListener((obs, antes, ahora) -> {
            if (ahora && elegido == null) {
                mostrarSugerencias();
            }
        });
    }

    /** Tratamientos que aún deben algo y coinciden con el paciente escrito. */
    private void mostrarSugerencias() {
        List<TratamientoResumen> encontrados;
        try {
            encontrados = servicio.tratamientosConSaldo(campoTratamiento.getText()).stream()
                    .limit(MAX_SUGERENCIAS).toList();
        } catch (RuntimeException e) {
            avisoPago.mostrar(Aviso.Tipo.ERROR, "No se pudo buscar los tratamientos: " + e.getMessage());
            return;
        }
        sugerencias.getItems().clear();
        if (encontrados.isEmpty()) {
            Label nada = new Label("No hay tratamientos con saldo pendiente para esa búsqueda");
            nada.getStyleClass().add("texto-tenue");
            CustomMenuItem item = new CustomMenuItem(nada, false);
            item.setDisable(true);
            sugerencias.getItems().add(item);
        }
        for (TratamientoResumen r : encontrados) {
            Label descripcion = new Label(r.getTratamiento().getDescripcion());
            descripcion.getStyleClass().add("celda-titulo");
            Label detalle = new Label(r.getPaciente().getNombreCompleto() + " · Saldo " + Formatos.dinero(r.getSaldo()));
            detalle.getStyleClass().add("celda-sub");
            VBox fila = new VBox(descripcion, detalle);
            fila.setPrefWidth(buscadorTratamiento.getWidth() - 24);
            CustomMenuItem item = new CustomMenuItem(fila, true);
            item.setOnAction(e -> elegirTratamiento(r));
            sugerencias.getItems().add(item);
        }
        if (!sugerencias.isShowing()) {
            sugerencias.show(buscadorTratamiento, Side.BOTTOM, 0, 4);
        }
    }

    private void elegirTratamiento(TratamientoResumen r) {
        elegido = r;
        sugerencias.hide();
        cfTratamiento.limpiarError();
        tarjetaDescripcion.setText(r.getTratamiento().getDescripcion());
        tarjetaPaciente.setText(r.getPaciente().getNombreCompleto() + " · " + r.getTratamiento().getEstado().getEtiqueta());
        tarjetaCosto.setText(Formatos.dinero(r.getTratamiento().getCosto()));
        tarjetaAvance.setProgress(r.getAvance());
        tarjetaSaldo.setText("Pagado " + Formatos.dinero(r.getPagado()) + " · Saldo " + Formatos.dinero(r.getSaldo()));
        alternar(buscadorTratamiento, false);
        alternar(tarjetaTratamiento, true);
        actualizarPie();
        campoMonto.requestFocus();
    }

    @FXML
    private void quitarTratamiento() {
        elegido = null;
        campoTratamiento.clear();
        sugerencias.hide();
        alternar(tarjetaTratamiento, false);
        alternar(buscadorTratamiento, true);
        actualizarPie();
    }

    /** "Saldo después de este pago: S/ 200.00" y el texto del botón, mientras se escribe el monto. */
    private void actualizarPie() {
        Double monto = montoEscrito();
        botonRegistrar.setText(monto == null ? "Registrar pago" : "Registrar pago de " + Formatos.dinero(monto));
        saldoDespues.getStyleClass().remove("texto-error");
        if (elegido == null) {
            saldoDespues.setText("Elige un tratamiento para ver su saldo.");
        } else if (monto == null) {
            saldoDespues.setText("Saldo pendiente: " + Formatos.dinero(elegido.getSaldo()));
        } else if (Math.round(monto * 100) > Math.round(elegido.getSaldo() * 100)) {
            saldoDespues.setText("El monto supera el saldo pendiente (" + Formatos.dinero(elegido.getSaldo()) + ").");
            saldoDespues.getStyleClass().add("texto-error");
        } else {
            saldoDespues.setText("Saldo después de este pago: " + Formatos.dinero(elegido.getSaldo() - monto));
        }
    }

    /** El monto escrito, o null si está vacío o no es un número. */
    private Double montoEscrito() {
        try {
            return Formatos.leerMonto(campoMonto.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FXML
    private void registrar() {
        limpiarErrores();
        if (elegido == null) {
            cfTratamiento.mostrarError("Elige el tratamiento que se paga.");
            campoTratamiento.requestFocus();
            return;
        }
        Double monto = montoEscrito();
        if (monto == null) {
            cfMonto.mostrarError("Escribe el monto como 200 o 200.50");
            campoMonto.requestFocus();
            return;
        }
        LocalDate fecha;
        String textoFecha = campoFecha.getEditor().getText().strip();
        try {
            fecha = textoFecha.isEmpty() ? null : LocalDate.parse(textoFecha, Formatos.formatoFechaCampo());
        } catch (DateTimeParseException e) {
            cfFecha.mostrarError("Escribe la fecha como dd/mm/aaaa");
            campoFecha.requestFocus();
            return;
        }
        MetodoPago metodo = grupoMetodos.getSelectedToggle() == null ? null
                : (MetodoPago) grupoMetodos.getSelectedToggle().getUserData();
        Pago pago = new Pago(0, elegido.getTratamiento().getIdTratamiento(), monto, fecha, metodo,
                campoOperacion.getText());
        try {
            servicio.registrar(pago);
            Notificacion.mostrar(raiz.getScene().getWindow(), "Pago de " + Formatos.dinero(monto) + " registrado");
            campoMonto.clear();
            campoOperacion.clear();
            campoFecha.setValue(LocalDate.now());
            cargarPeriodo();
            // Se vuelve a leer el tratamiento: si ya no debe nada, se suelta
            TratamientoResumen actualizado = Servicios.tratamientos().resumen(pago.getIdTratamiento()).orElse(null);
            if (actualizado == null || actualizado.getSaldo() <= 0) {
                quitarTratamiento();
            } else {
                elegirTratamiento(actualizado);
            }
        } catch (ValidacionException e) {
            e.getErrores().forEach(this::mostrarError);
        } catch (ReglaNegocioException e) {
            mostrarError(e.getCampo().orElse(""), e.getMessage());
        } catch (RuntimeException e) {
            avisoPago.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    private void mostrarError(String campo, String mensaje) {
        CampoFormulario destino = campos.get(campo);
        if (destino == null) {
            avisoPago.mostrar(Aviso.Tipo.ERROR, mensaje);
            return;
        }
        destino.mostrarError(mensaje);
    }

    private void limpiarErrores() {
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoPago.ocultar();
    }

    // ===== Utilidades =====

    private static TableColumn<PagoResumen, PagoResumen> columna(String titulo, double ancho) {
        TableColumn<PagoResumen, PagoResumen> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static TableColumn<PagoResumen, PagoResumen> columnaTexto(String titulo, double ancho,
                                                                     Function<PagoResumen, String> texto,
                                                                     String claseCss) {
        TableColumn<PagoResumen, PagoResumen> columna = columna(titulo, ancho);
        columna.setCellFactory(c -> {
            TableCell<PagoResumen, PagoResumen> celda = new TableCell<>() {
                @Override
                protected void updateItem(PagoResumen r, boolean vacio) {
                    super.updateItem(r, vacio);
                    setText(vacio || r == null ? null : texto.apply(r));
                }
            };
            if (claseCss != null) {
                celda.getStyleClass().add(claseCss);
            }
            return celda;
        });
        return columna;
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
