package com.vyra.dental.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.vyra.dental.model.Paciente;
import com.vyra.dental.util.Formatos;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Tarjeta para elegir un paciente: buscador por nombre o DNI (filtra mientras
 * se escribe, con 300 ms de espera) y una tabla con los resultados. La usan
 * las pantallas que se abren desde el menú sin un paciente (Historia clínica,
 * Odontograma). No consulta la base de datos por su cuenta: el controlador le
 * pasa cómo buscar con {@link #configurar}.
 */
public class BuscadorPacientes extends VBox {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;
    private static final int FILAS_MINIMAS = 5;

    private final TextField campo = new TextField();
    private final TableView<Paciente> tabla = new TableView<>();
    private final Label pie = new Label();
    private final ObservableList<Paciente> filas = FXCollections.observableArrayList();
    private final PauseTransition espera = new PauseTransition(Duration.millis(300));

    private Function<String, List<Paciente>> buscar;
    private ToIntFunction<String> contar;
    private Consumer<Paciente> alElegir;
    private int marcado;
    private boolean ignorarSeleccion;

    public BuscadorPacientes() {
        getStyleClass().addAll("card", "lista");
        campo.setPromptText("Buscar por nombre o DNI");
        campo.setAccessibleText("Buscar pacientes");
        HBox.setHgrow(campo, Priority.ALWAYS);
        HBox buscador = new HBox(new Icono("buscar", 16), campo);
        buscador.getStyleClass().add("buscador");
        HBox.setHgrow(buscador, Priority.ALWAYS);
        HBox barra = new HBox(buscador);
        barra.getStyleClass().add("lista-barra");

        construirTabla();

        pie.getStyleClass().add("texto-secundario");
        HBox piePagina = new HBox(pie);
        piePagina.getStyleClass().add("lista-pie");
        getChildren().addAll(barra, tabla, piePagina);

        espera.setOnFinished(e -> cargar());
        campo.textProperty().addListener((obs, anterior, nuevo) -> espera.playFromStart());
        // Enter busca de inmediato y no activa el botón por defecto de la pantalla
        campo.setOnAction(e -> {
            espera.stop();
            cargar();
            e.consume();
        });
    }

    /**
     * @param buscar   pacientes que coinciden con el texto (una página)
     * @param contar   cuántos pacientes coinciden en total
     * @param alElegir qué hacer al elegir uno de la tabla
     */
    public void configurar(Function<String, List<Paciente>> buscar, ToIntFunction<String> contar,
                           Consumer<Paciente> alElegir) {
        this.buscar = buscar;
        this.contar = contar;
        this.alElegir = alElegir;
        cargar();
    }

    /** Marca en la tabla el paciente abierto, sin volver a elegirlo. */
    public void marcar(int idPaciente) {
        marcado = idPaciente;
        reseleccionar();
    }

    public void enfocar() {
        campo.requestFocus();
    }

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        // La tabla muestra todos los resultados; el desplazamiento lo hace la pantalla
        var alto = Bindings.max(FILAS_MINIMAS, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);

        TableColumn<Paciente, Paciente> paciente = columna("PACIENTE", 250);
        paciente.setCellFactory(c -> new CeldaPaciente<>());
        TableColumn<Paciente, Paciente> edad = columna("EDAD", 80);
        edad.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null
                        : p.getFechaNacimiento() == null ? "—" : Formatos.edad(p.getFechaNacimiento()));
            }
        });
        TableColumn<Paciente, Paciente> alergias = columna("ALERGIAS", 130);
        alergias.setCellFactory(c -> new CeldaAlergias());
        tabla.getColumns().setAll(List.of(paciente, edad, alergias));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion && alElegir != null) {
                marcado = nuevo.getIdPaciente();
                alElegir.accept(nuevo);
            }
        });
    }

    private void cargar() {
        if (buscar == null) {
            return;
        }
        String texto = campo.getText();
        try {
            List<Paciente> encontrados = buscar.apply(texto);
            int total = contar.applyAsInt(texto);
            filas.setAll(encontrados);
            if (total == 0) {
                pie.setText("Sin resultados");
            } else if (encontrados.size() < total) {
                pie.setText("Mostrando " + encontrados.size() + " de " + total + " · escribe para filtrar");
            } else {
                pie.setText(total + (total == 1 ? " paciente" : " pacientes"));
            }
            boolean sinTexto = texto == null || texto.isBlank();
            tabla.setPlaceholder(sinTexto
                    ? new EstadoVacio("Aún no hay pacientes registrados", "Regístralos desde la pantalla Pacientes.")
                    : new EstadoVacio("No se encontraron pacientes", "Prueba con otro nombre o DNI."));
            reseleccionar();
        } catch (RuntimeException e) {
            filas.clear();
            pie.setText("");
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar la lista de pacientes", e.getMessage()));
        }
    }

    private void reseleccionar() {
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            filas.stream()
                    .filter(p -> p.getIdPaciente() == marcado)
                    .findFirst()
                    .ifPresent(p -> tabla.getSelectionModel().select(p));
        } finally {
            ignorarSeleccion = false;
        }
    }

    private static TableColumn<Paciente, Paciente> columna(String titulo, double ancho) {
        TableColumn<Paciente, Paciente> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }
}
