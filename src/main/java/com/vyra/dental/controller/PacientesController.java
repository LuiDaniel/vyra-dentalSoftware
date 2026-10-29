package com.vyra.dental.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.service.CitaService;
import com.vyra.dental.service.PacienteService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.service.Sesion;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.CampoFormulario;
import com.vyra.dental.ui.Dialogos;
import com.vyra.dental.ui.EstadoVacio;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Lista de pacientes + ficha con la pestaña Datos (docs/08-guia-frontend.md §7.4, CU-01).
 * Administrador y Recepción editan; el odontólogo solo consulta.
 */
public class PacientesController {

    private static final double ALTO_FILA = 58;
    private static final double ALTO_ENCABEZADO = 44;
    private static final int FILAS_MINIMAS = 5;

    @FXML private Label resumen;
    @FXML private Button botonNuevo;
    @FXML private TextField campoBuscar;
    @FXML private HBox filtros;
    @FXML private TableView<Paciente> tabla;
    @FXML private Label rango;
    @FXML private Button botonAnterior;
    @FXML private Button botonSiguiente;

    @FXML private VBox panel;
    @FXML private Avatar avatarFicha;
    @FXML private Label tituloFicha;
    @FXML private Label subtituloFicha;
    @FXML private CampoFormulario cfNombres;
    @FXML private CampoFormulario cfApellidos;
    @FXML private CampoFormulario cfDni;
    @FXML private CampoFormulario cfFechaNacimiento;
    @FXML private CampoFormulario cfTelefono;
    @FXML private CampoFormulario cfContactoEmergencia;
    @FXML private CampoFormulario cfDireccion;
    @FXML private CampoFormulario cfAlergias;
    @FXML private CampoFormulario cfAntecedentes;
    @FXML private TextField campoNombres;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoDni;
    @FXML private DatePicker campoFechaNacimiento;
    @FXML private TextField campoTelefono;
    @FXML private TextField campoContactoEmergencia;
    @FXML private TextField campoDireccion;
    @FXML private TextArea campoAlergias;
    @FXML private TextArea campoAntecedentes;
    @FXML private Aviso avisoAlergia;
    @FXML private Aviso avisoFormulario;
    @FXML private HBox pie;
    @FXML private Button botonBaja;
    @FXML private Button botonGuardar;
    @FXML private ToggleButton pestanaDatos;
    @FXML private ToggleButton pestanaCitas;
    @FXML private VBox contenidoDatos;
    @FXML private VBox contenidoCitas;

    private final PacienteService servicio = Servicios.pacientes();
    private final CitaService citas = Servicios.citas();
    /** Última visita de cada paciente de la página (se consulta al cargar la lista). */
    private final Map<Integer, LocalDate> ultimasVisitas = new HashMap<>();
    private final ObservableList<Paciente> filas = FXCollections.observableArrayList();
    private final ToggleGroup grupoFiltros = new ToggleGroup();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    /** Nombre del campo en el modelo → campo del formulario, para mostrar los errores. */
    private final Map<String, CampoFormulario> campos = new LinkedHashMap<>();

    private FiltroPaciente filtro = FiltroPaciente.TODOS;
    private int pagina;
    /** Paciente abierto en la ficha; null cuando se está registrando uno nuevo. */
    private Paciente seleccionado;
    private boolean editable;
    private boolean ignorarSeleccion;

    @FXML
    private void initialize() {
        editable = servicio.puedeEditar();
        campos.put("nombres", cfNombres);
        campos.put("apellidos", cfApellidos);
        campos.put("dni", cfDni);
        campos.put("fechaNacimiento", cfFechaNacimiento);
        campos.put("telefono", cfTelefono);
        campos.put("contactoEmergencia", cfContactoEmergencia);
        campos.put("direccion", cfDireccion);
        campos.put("alergias", cfAlergias);
        campos.put("antecedentes", cfAntecedentes);

        construirFiltros();
        construirTabla();
        configurarFecha();
        configurarFormulario();

        // Filtra mientras se escribe, esperando 300 ms desde la última tecla
        esperaBusqueda.setOnFinished(e -> {
            pagina = 0;
            cargar();
        });
        campoBuscar.textProperty().addListener((obs, anterior, nuevo) -> esperaBusqueda.playFromStart());
        // Enter busca de inmediato (y no activa el botón Guardar del formulario)
        campoBuscar.setOnAction(e -> {
            esperaBusqueda.stop();
            pagina = 0;
            cargar();
            e.consume();
        });

        configurarPestanas();
        aplicarPermisos();
        cargar();
        if (!filas.isEmpty()) {
            tabla.getSelectionModel().selectFirst();
        } else if (editable) {
            prepararNuevo();
        } else {
            mostrarPanel(false);
        }
    }

    // ===== Lista =====

    private void construirFiltros() {
        for (FiltroPaciente opcion : FiltroPaciente.values()) {
            ToggleButton chip = new ToggleButton(opcion.getEtiqueta());
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(grupoFiltros);
            chip.setSelected(opcion == filtro);
            chip.setOnAction(e -> {
                // Siempre queda un filtro elegido
                chip.setSelected(true);
                if (filtro != opcion) {
                    filtro = opcion;
                    pagina = 0;
                    cargar();
                }
            });
            filtros.getChildren().add(chip);
        }
    }

    private void construirTabla() {
        tabla.setItems(filas);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        // La tabla muestra todas las filas de la página; el desplazamiento lo hace la pantalla
        var alto = Bindings.max(FILAS_MINIMAS, Bindings.size(filas)).multiply(ALTO_FILA).add(ALTO_ENCABEZADO);
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);

        TableColumn<Paciente, Paciente> paciente = columna("PACIENTE", 250);
        paciente.setCellFactory(c -> new CeldaPaciente());

        TableColumn<Paciente, Paciente> telefono = columnaTexto("TELÉFONO", 110,
                p -> p.getTelefono() == null ? "—" : Formatos.telefono(p.getTelefono()));
        TableColumn<Paciente, Paciente> edad = columnaTexto("EDAD", 80,
                p -> p.getFechaNacimiento() == null ? "—" : Formatos.edad(p.getFechaNacimiento()));
        TableColumn<Paciente, Paciente> ultimaVisita = columnaTexto("ÚLTIMA VISITA", 110,
                p -> Optional.ofNullable(ultimasVisitas.get(p.getIdPaciente())).map(Formatos::fechaCorta).orElse("—"));

        TableColumn<Paciente, Paciente> alergias = columna("ALERGIAS", 130);
        alergias.setCellFactory(c -> new CeldaAlergias());

        TableColumn<Paciente, Paciente> acciones = columna("ACCIONES", 96);
        acciones.setCellFactory(c -> new CeldaAcciones());
        acciones.getStyleClass().add("columna-derecha");

        tabla.getColumns().setAll(List.of(paciente, telefono, edad, ultimaVisita, alergias, acciones));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null && !ignorarSeleccion) {
                mostrar(nuevo);
            }
        });
    }

    private void cargar() {
        String texto = campoBuscar.getText();
        try {
            int total = servicio.contar(texto, filtro);
            int ultimaPagina = Math.max(0, (total - 1) / PacienteService.TAMANO_PAGINA);
            pagina = Math.min(pagina, ultimaPagina);
            List<Paciente> encontrados = servicio.buscar(texto, filtro, pagina);
            ultimasVisitas.clear();
            for (Paciente paciente : encontrados) {
                citas.ultimaVisita(paciente.getIdPaciente())
                        .ifPresent(fecha -> ultimasVisitas.put(paciente.getIdPaciente(), fecha));
            }
            filas.setAll(encontrados);

            int registrados = servicio.contar("", FiltroPaciente.TODOS);
            String accion = editable ? "ver o editar su ficha" : "ver su ficha";
            resumen.setText(registrados + (registrados == 1 ? " paciente registrado" : " pacientes registrados")
                    + " · selecciona uno para " + accion + ".");

            int desde = pagina * PacienteService.TAMANO_PAGINA;
            rango.setText(total == 0 ? "Sin resultados"
                    : "Mostrando " + (desde + 1) + "–" + (desde + filas.size()) + " de " + total);
            botonAnterior.setDisable(pagina == 0);
            botonSiguiente.setDisable(desde + filas.size() >= total);
            tabla.setPlaceholder(estadoVacio(registrados == 0));
            reseleccionar();
        } catch (RuntimeException e) {
            filas.clear();
            rango.setText("");
            tabla.setPlaceholder(new EstadoVacio("No se pudo cargar la lista de pacientes", e.getMessage()));
        }
    }

    /** Vuelve a marcar en la tabla el paciente abierto, sin recargar el formulario. */
    private void reseleccionar() {
        ignorarSeleccion = true;
        try {
            tabla.getSelectionModel().clearSelection();
            if (seleccionado != null) {
                filas.stream()
                        .filter(p -> p.getIdPaciente() == seleccionado.getIdPaciente())
                        .findFirst()
                        .ifPresent(p -> tabla.getSelectionModel().select(p));
            }
        } finally {
            ignorarSeleccion = false;
        }
    }

    @FXML
    private void paginaAnterior() {
        pagina--;
        cargar();
    }

    @FXML
    private void paginaSiguiente() {
        pagina++;
        cargar();
    }

    private Node estadoVacio(boolean sinPacientes) {
        if (sinPacientes) {
            Button primero = null;
            if (editable) {
                primero = new Button("Registrar el primero", new Icono("mas", 16));
                primero.getStyleClass().add("btn-primario");
                primero.setOnAction(e -> prepararNuevo());
            }
            return new EstadoVacio("Aún no hay pacientes registrados",
                    "Cuando registres uno aparecerá en esta lista.", primero);
        }
        return new EstadoVacio("No se encontraron pacientes", "Prueba con otro nombre o DNI, o cambia el filtro.");
    }

    // ===== Ficha =====

    @FXML
    private void nuevo() {
        prepararNuevo();
    }

    private void prepararNuevo() {
        seleccionado = null;
        reseleccionar();
        mostrarPanel(true);
        limpiarErrores();
        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextInputControl texto) {
                texto.clear();
            }
        }
        campoFechaNacimiento.setValue(null);
        campoFechaNacimiento.getEditor().clear();

        avatarFicha.mostrar("", 0);
        tituloFicha.setText("Nuevo paciente");
        subtituloFicha.setText("Completa sus datos. Los campos con * son obligatorios.");
        botonGuardar.setText("Registrar paciente");
        pestanaCitas.setDisable(true);
        pestanaDatos.setSelected(true);
        mostrarPestana();
        alternar(botonBaja, false);
        campoNombres.requestFocus();
    }

    private void mostrar(Paciente paciente) {
        seleccionado = paciente;
        mostrarPanel(true);
        limpiarErrores();
        campoNombres.setText(paciente.getNombres());
        campoApellidos.setText(paciente.getApellidos());
        campoDni.setText(paciente.getDni());
        campoFechaNacimiento.setValue(paciente.getFechaNacimiento());
        campoTelefono.setText(paciente.getTelefono() == null ? "" : Formatos.telefono(paciente.getTelefono()));
        campoContactoEmergencia.setText(paciente.getContactoEmergencia());
        campoDireccion.setText(paciente.getDireccion());
        campoAlergias.setText(paciente.getAlergias());
        campoAntecedentes.setText(paciente.getAntecedentes());

        avatarFicha.mostrar(paciente.getIniciales(), paciente.getIdPaciente());
        tituloFicha.setText(paciente.getNombreCompleto());
        String desde = paciente.getFechaRegistro() == null ? ""
                : " · Paciente desde " + Formatos.mesAnio(paciente.getFechaRegistro());
        subtituloFicha.setText("DNI " + paciente.getDni() + desde);
        botonGuardar.setText("Guardar cambios");
        alternar(botonBaja, editable);
        pestanaCitas.setDisable(false);
        mostrarPestana();
    }

    @FXML
    private void guardar() {
        limpiarErrores();
        Optional<Paciente> datos = leerFormulario();
        if (datos.isEmpty()) {
            return;
        }
        Paciente paciente = datos.get();
        try {
            if (seleccionado == null) {
                servicio.registrar(paciente);
                notificar("Paciente registrado");
            } else {
                servicio.actualizar(paciente);
                notificar("Cambios guardados");
            }
            seleccionado = paciente;
            cargar();
            mostrar(paciente);
        } catch (ValidacionException e) {
            mostrarErrores(e.getErrores());
        } catch (ReglaNegocioException e) {
            CampoFormulario campo = e.getCampo().map(campos::get).orElse(null);
            if (campo != null) {
                campo.mostrarError(e.getMessage());
                campo.getControl().requestFocus();
            } else {
                avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            }
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage() + ".");
        }
    }

    @FXML
    private void cancelar() {
        if (seleccionado == null) {
            prepararNuevo();
        } else {
            mostrar(seleccionado);
        }
    }

    @FXML
    private void darDeBaja() {
        if (seleccionado == null) {
            return;
        }
        boolean confirmado = Dialogos.confirmar(tabla.getScene().getWindow(), "Dar de baja",
                "¿Dar de baja a " + seleccionado.getNombreCompleto() + "?",
                "Dejará de aparecer en las búsquedas, pero su historial se conserva.", "Dar de baja");
        if (!confirmado) {
            return;
        }
        try {
            servicio.darDeBaja(seleccionado.getIdPaciente());
            notificar("Paciente dado de baja");
            seleccionado = null;
            cargar();
            if (!filas.isEmpty()) {
                tabla.getSelectionModel().selectFirst();
            } else {
                prepararNuevo();
            }
        } catch (RuntimeException e) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, e.getMessage());
        }
    }

    /** Arma el paciente con lo escrito; vacío si la fecha no se puede leer. */
    private Optional<Paciente> leerFormulario() {
        LocalDate nacimiento;
        String textoFecha = campoFechaNacimiento.getEditor().getText().strip();
        try {
            nacimiento = textoFecha.isEmpty() ? null : LocalDate.parse(textoFecha, Formatos.formatoFechaCampo());
        } catch (DateTimeParseException e) {
            cfFechaNacimiento.mostrarError("Escribe la fecha como dd/mm/aaaa");
            campoFechaNacimiento.requestFocus();
            return Optional.empty();
        }
        Paciente paciente = new Paciente();
        if (seleccionado != null) {
            paciente.setIdPaciente(seleccionado.getIdPaciente());
            paciente.setActivo(seleccionado.isActivo());
            paciente.setFechaRegistro(seleccionado.getFechaRegistro());
        }
        paciente.setNombres(campoNombres.getText());
        paciente.setApellidos(campoApellidos.getText());
        paciente.setDni(campoDni.getText());
        paciente.setFechaNacimiento(nacimiento);
        paciente.setTelefono(campoTelefono.getText());
        paciente.setContactoEmergencia(campoContactoEmergencia.getText());
        paciente.setDireccion(campoDireccion.getText());
        paciente.setAlergias(campoAlergias.getText());
        paciente.setAntecedentes(campoAntecedentes.getText());
        return Optional.of(paciente);
    }

    private void mostrarErrores(Map<String, String> errores) {
        Node primero = null;
        StringBuilder otros = new StringBuilder();
        for (Map.Entry<String, String> error : errores.entrySet()) {
            CampoFormulario campo = campos.get(error.getKey());
            if (campo == null) {
                otros.append(error.getValue()).append(". ");
                continue;
            }
            campo.mostrarError(error.getValue());
            if (primero == null) {
                primero = campo.getControl();
            }
        }
        if (!otros.isEmpty()) {
            avisoFormulario.mostrar(Aviso.Tipo.ERROR, otros.toString().strip());
        }
        if (primero != null) {
            primero.requestFocus();
        }
    }

    private void limpiarErrores() {
        campos.values().forEach(CampoFormulario::limpiarError);
        avisoFormulario.ocultar();
    }

    private void configurarFormulario() {
        // Al corregir un campo se borra su error
        for (CampoFormulario campo : campos.values()) {
            if (campo.getControl() instanceof TextInputControl texto) {
                texto.textProperty().addListener((obs, anterior, nuevo) -> campo.limpiarError());
            }
        }
        campoFechaNacimiento.getEditor().textProperty()
                .addListener((obs, anterior, nuevo) -> cfFechaNacimiento.limpiarError());
        campoAlergias.textProperty().addListener((obs, anterior, nuevo) -> actualizarAvisoAlergia());
    }

    private void actualizarAvisoAlergia() {
        String alergias = campoAlergias.getText();
        if (editable && alergias != null && !alergias.isBlank()) {
            avisoAlergia.mostrar(Aviso.Tipo.ADVERTENCIA, "Esta alergia se mostrará como alerta al abrir el odontograma.");
        } else {
            avisoAlergia.ocultar();
        }
    }

    private void configurarFecha() {
        campoFechaNacimiento.setConverter(new StringConverter<>() {
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
                    // Se deja el texto tal cual; guardar() muestra el error bajo el campo
                    return campoFechaNacimiento.getValue();
                }
            }
        });
        // RN-03: no se puede elegir una fecha de nacimiento futura
        campoFechaNacimiento.setDayCellFactory(selector -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha.isAfter(LocalDate.now()));
            }
        });
    }

    /** El odontólogo ve la ficha, pero no puede crear, editar ni dar de baja. */
    private void aplicarPermisos() {
        alternar(botonNuevo, editable);
        alternar(pie, editable);
        // Guardar es el botón por defecto (Enter): oculto no basta, se deshabilita
        botonGuardar.setDisable(!editable);
        if (!editable) {
            for (CampoFormulario campo : campos.values()) {
                if (campo.getControl() instanceof TextInputControl texto) {
                    texto.setEditable(false);
                }
            }
            campoFechaNacimiento.setEditable(false);
            campoFechaNacimiento.setDisable(true);
        }
    }

    private void mostrarPanel(boolean visible) {
        alternar(panel, visible);
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    private void notificar(String mensaje) {
        Notificacion.mostrar(tabla.getScene().getWindow(), mensaje);
    }

    // ===== Pestañas de la ficha =====

    private void configurarPestanas() {
        ToggleGroup pestanas = new ToggleGroup();
        pestanaDatos.setToggleGroup(pestanas);
        pestanaCitas.setToggleGroup(pestanas);
        pestanas.selectedToggleProperty().addListener((obs, anterior, nueva) -> {
            if (nueva == null) {
                anterior.setSelected(true);
            } else {
                mostrarPestana();
            }
        });
        alternar(contenidoCitas, false);
    }

    private void mostrarPestana() {
        boolean datos = pestanaDatos.isSelected();
        alternar(contenidoDatos, datos);
        alternar(pie, datos && editable);
        alternar(contenidoCitas, !datos);
        if (!datos) {
            cargarCitasDelPaciente();
        }
    }

    /** Pestaña Citas: fecha, horario, profesional, motivo y estado de cada cita. */
    private void cargarCitasDelPaciente() {
        contenidoCitas.getChildren().clear();
        if (seleccionado == null) {
            return;
        }
        List<Cita> lista;
        try {
            lista = citas.citasDePaciente(seleccionado.getIdPaciente());
        } catch (RuntimeException e) {
            contenidoCitas.getChildren().add(new EstadoVacio("No se pudo cargar las citas", e.getMessage()));
            return;
        }
        if (lista.isEmpty()) {
            Button agendar = null;
            if (editable) {
                agendar = new Button("Agendar cita", new Icono("agenda", 16));
                agendar.getStyleClass().add("btn-primario");
                Paciente paciente = seleccionado;
                agendar.setOnAction(e -> MainController.actual().navegar(Pantalla.AGENDA, paciente));
            }
            contenidoCitas.getChildren().add(new EstadoVacio("Sin citas registradas",
                    "Las citas de este paciente aparecerán aquí.", agendar));
            return;
        }
        for (Cita cita : lista) {
            Label cuando = new Label(Formatos.diaCorto(cita.getFecha()) + " " + Formatos.fechaCorta(cita.getFecha())
                    + " · " + Formatos.rangoHoras(cita.getHoraInicio(), cita.getHoraFin()));
            cuando.getStyleClass().add("celda-titulo");
            Label quien = new Label(cita.getProfesional().getNombreCompleto()
                    + (cita.getMotivo() == null ? "" : " · " + cita.getMotivo()));
            quien.getStyleClass().add("celda-sub");
            quien.setWrapText(true);
            VBox textos = new VBox(2, cuando, quien);
            HBox.setHgrow(textos, Priority.ALWAYS);
            HBox fila = new HBox(12, textos, new Pill(cita.getEstado()));
            fila.getStyleClass().add("fila-cita");
            contenidoCitas.getChildren().add(fila);
        }
    }

    // ===== Columnas =====

    private static TableColumn<Paciente, Paciente> columna(String titulo, double ancho) {
        TableColumn<Paciente, Paciente> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    private static TableColumn<Paciente, Paciente> columnaTexto(String titulo, double ancho,
                                                               Function<Paciente, String> texto) {
        TableColumn<Paciente, Paciente> columna = columna(titulo, ancho);
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente paciente, boolean vacio) {
                super.updateItem(paciente, vacio);
                setText(vacio || paciente == null ? null : texto.apply(paciente));
            }
        });
        return columna;
    }

    /** Avatar + nombre + "DNI 72481935". */
    private static class CeldaPaciente extends TableCell<Paciente, Paciente> {
        private final Avatar avatar = new Avatar();
        private final Label nombre = new Label();
        private final Label dni = new Label();
        private final HBox contenido = new HBox(avatar, new VBox(nombre, dni));

        CeldaPaciente() {
            nombre.getStyleClass().add("celda-titulo");
            dni.getStyleClass().add("celda-sub");
            contenido.getStyleClass().add("celda-paciente");
            setText(null);
        }

        @Override
        protected void updateItem(Paciente paciente, boolean vacio) {
            super.updateItem(paciente, vacio);
            if (vacio || paciente == null) {
                setGraphic(null);
                return;
            }
            avatar.mostrar(paciente.getIniciales(), paciente.getIdPaciente());
            nombre.setText(paciente.getNombreCompleto());
            dni.setText("DNI " + paciente.getDni());
            setGraphic(contenido);
        }
    }

    /** Píldora naranja con la alergia, o "Ninguna" en gris. */
    private static class CeldaAlergias extends TableCell<Paciente, Paciente> {
        @Override
        protected void updateItem(Paciente paciente, boolean vacio) {
            super.updateItem(paciente, vacio);
            setText(null);
            if (vacio || paciente == null) {
                setGraphic(null);
            } else if (paciente.tieneAlergias()) {
                Label pill = new Label(paciente.getAlergias().lines().findFirst().orElse(""), new Icono("alerta", 12));
                pill.getStyleClass().addAll("pill", "pill-alergia");
                pill.setTooltip(new Tooltip(paciente.getAlergias()));
                setGraphic(pill);
            } else {
                Label ninguna = new Label("Ninguna");
                ninguna.getStyleClass().add("texto-tenue");
                setGraphic(ninguna);
            }
        }
    }

    /** Accesos al odontograma y a una nueva cita, según lo que el rol puede ver. */
    private class CeldaAcciones extends TableCell<Paciente, Paciente> {
        @Override
        protected void updateItem(Paciente paciente, boolean vacio) {
            super.updateItem(paciente, vacio);
            setText(null);
            if (vacio || paciente == null) {
                setGraphic(null);
                return;
            }
            HBox botones = new HBox();
            botones.getStyleClass().add("celda-acciones");
            if (Pantalla.ODONTOGRAMA.permitidaPara(Sesion.get().getUsuario().getRol())) {
                botones.getChildren().add(accion("diente", "Ver odontograma de " + paciente.getNombreCompleto(),
                        () -> MainController.actual().navegar(Pantalla.ODONTOGRAMA, paciente)));
            }
            if (editable) {
                botones.getChildren().add(accion("agenda", "Agendar cita para " + paciente.getNombreCompleto(),
                        () -> MainController.actual().navegar(Pantalla.AGENDA, paciente)));
            }
            setGraphic(botones);
        }

        private Button accion(String icono, String descripcion, Runnable alHacerClic) {
            Button boton = new Button(null, new Icono(icono, 18));
            boton.getStyleClass().add("btn-icono");
            boton.setTooltip(new Tooltip(descripcion));
            boton.setAccessibleText(descripcion);
            boton.setOnAction(e -> alHacerClic.run());
            return boton;
        }
    }
}
