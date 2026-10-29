package com.vyra.dental.controller;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.service.OdontogramaService;
import com.vyra.dental.service.ReglaNegocioException;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.Aviso;
import com.vyra.dental.ui.Dialogos;
import com.vyra.dental.ui.EstadoVacio;
import com.vyra.dental.ui.Icono;
import com.vyra.dental.ui.Notificacion;
import com.vyra.dental.ui.PiezaDental;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.ValidacionException;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Odontograma del paciente (docs/08-guia-frontend.md §7.6, CU-05). Los cambios quedan
 * pendientes en {@link #pendientes} hasta pulsar "Guardar"; cada pieza cambiada se
 * guarda como un registro nuevo (RN-09). El odontólogo edita; el administrador solo consulta.
 * Se abre con un {@link Paciente} como parámetro; sin parámetro muestra un buscador.
 */
public class OdontogramaController implements RecibeParametro, ConfirmaSalida {

    private static final int[] SUPERIOR = {18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28};
    private static final int[] INFERIOR = {48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38};
    private static final String[] POSICIONES = {"", "Incisivo central", "Incisivo lateral", "Canino",
        "Primer premolar", "Segundo premolar", "Primer molar", "Segundo molar", "Tercer molar"};
    private static final String[] CUADRANTES = {"", "superior derecho", "superior izquierdo", "inferior izquierdo",
        "inferior derecho"};
    private static final int TOTAL_PIEZAS = 32;

    @FXML private VBox pagina;
    @FXML private Label subtitulo;
    @FXML private VBox selector;
    @FXML private VBox clinica;

    private final OdontogramaService servicio = Servicios.odontograma();
    private final boolean puedeEscribir = servicio.puedeEscribir();

    /** Estado guardado en la base de datos; las piezas ausentes son SANA. */
    private final Map<Integer, EstadoPieza> guardado = new HashMap<>();
    /** Cambios sin guardar: número de pieza → estado nuevo (solo si difiere del guardado). */
    private final Map<Integer, EstadoPieza> pendientes = new TreeMap<>();
    private final Map<Integer, PiezaDental> piezas = new HashMap<>();
    private final Map<EstadoPieza, Label> conteos = new EnumMap<>(EstadoPieza.class);
    private final ToggleGroup grupoPiezas = new ToggleGroup();
    private final ToggleGroup grupoHerramienta = new ToggleGroup();

    private final Avatar avatar = new Avatar();
    private final Label nombre = new Label();
    private final Label datos = new Label();
    private final HBox alergia = new HBox(6);
    private final Label textoAlergia = new Label();
    private final Button botonGuardar = new Button();
    private final Aviso aviso = new Aviso();
    private final VBox detallePieza = new VBox(10);
    private final VBox historial = new VBox();
    private final TextField campoObservacion = new TextField();

    private Paciente paciente;
    private EstadoPieza herramienta = EstadoPieza.CARIADA;
    private Integer seleccionada;

    @FXML
    private void initialize() {
        if (!servicio.puedeVer()) {
            pagina.getChildren().setAll(new EstadoVacio("Sin acceso", "Tu rol no puede ver el odontograma."));
            return;
        }
        subtitulo.setText(puedeEscribir ? "Selecciona un estado y haz clic en las piezas para aplicarlo."
                : "Solo consulta: el odontólogo es quien registra los cambios.");
        construirClinica();
    }

    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof Paciente p) {
            mostrarPaciente(p);
        } else {
            mostrarSelector();
        }
    }

    @Override
    public boolean puedeSalir() {
        if (pendientes.isEmpty() || pagina.getScene() == null) {
            return true;
        }
        return Dialogos.confirmar(pagina.getScene().getWindow(), "Cambios sin guardar",
                "¿Salir sin guardar los cambios?",
                "Hay " + pendientes.size() + " pieza(s) modificada(s) que se perderán.", "Salir sin guardar");
    }

    // ===== Selector de paciente =====

    private void mostrarSelector() {
        TextField buscador = new TextField();
        buscador.setPromptText("Buscar paciente por nombre o DNI");
        ListView<Paciente> resultados = new ListView<>();
        resultados.setPrefHeight(320);
        resultados.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null : p.getNombreCompleto() + " · DNI " + p.getDni());
            }
        });
        Runnable buscar = () -> resultados.getItems()
                .setAll(Servicios.pacientes().buscar(buscador.getText(), FiltroPaciente.TODOS, 0));
        buscador.textProperty().addListener((obs, antes, ahora) -> buscar.run());
        resultados.getSelectionModel().selectedItemProperty().addListener((obs, antes, p) -> {
            if (p != null) {
                mostrarPaciente(p);
            }
        });
        buscar.run();

        Label titulo = new Label("¿De qué paciente?");
        titulo.getStyleClass().add("card-titulo");
        VBox tarjeta = new VBox(12, titulo, buscador, resultados);
        tarjeta.getStyleClass().addAll("card", "odontograma-selector");
        selector.getChildren().setAll(tarjeta);
        selector.setVisible(true);
        selector.setManaged(true);
        alternar(clinica, false);
    }

    private void mostrarPaciente(Paciente nuevo) {
        paciente = nuevo;
        pendientes.clear();
        seleccionada = null;
        grupoPiezas.selectToggle(null);
        campoObservacion.clear();
        aviso.ocultar();
        recargarGuardado();

        avatar.mostrar(nuevo.getIniciales(), nuevo.getIdPaciente());
        nombre.setText(nuevo.getNombreCompleto());
        String cita = Servicios.historiaClinica().citaDeHoy(nuevo.getIdPaciente())
                .map(c -> c.getMotivo() == null ? null : c.getMotivo()).orElse(null);
        datos.setText("DNI " + nuevo.getDni() + (nuevo.getFechaNacimiento() == null ? ""
                : " · " + Formatos.edad(nuevo.getFechaNacimiento())) + (cita == null ? "" : " · Cita actual: " + cita));
        textoAlergia.setText("Alergia: " + (nuevo.tieneAlergias()
                ? nuevo.getAlergias().lines().findFirst().orElse("") : ""));
        alternar(alergia, nuevo.tieneAlergias());

        alternar(selector, false);
        alternar(clinica, true);
        refrescar();
    }

    private void recargarGuardado() {
        guardado.clear();
        guardado.putAll(servicio.estadoActual(paciente.getIdPaciente()));
    }

    // ===== Construcción =====

    private void construirClinica() {
        clinica.getChildren().setAll(construirTarjetaPaciente(), construirBarraYArcada(), construirDetalle());
    }

    private Node construirTarjetaPaciente() {
        avatar.getStyleClass().add("avatar-grande");
        nombre.getStyleClass().add("card-titulo");
        datos.getStyleClass().add("texto-secundario");
        VBox textos = new VBox(4, nombre, datos);

        alergia.getStyleClass().addAll("pill", "pill-alergia");
        alergia.setAlignment(Pos.CENTER_LEFT);
        alergia.getChildren().addAll(new Icono("alerta", 14), textoAlergia);
        alergia.setMaxHeight(Region.USE_PREF_SIZE);

        Button cambiar = new Button("Cambiar paciente");
        cambiar.getStyleClass().add("btn-secundario");
        cambiar.setOnAction(e -> {
            if (puedeSalir()) {
                mostrarSelector();
            }
        });
        Button exportar = new Button("Exportar PDF", new Icono("descargar", 16));
        exportar.getStyleClass().add("btn-secundario");
        exportar.setDisable(true);
        exportar.setTooltip(new Tooltip("Disponible en la fase 8"));

        botonGuardar.getStyleClass().add("btn-primario");
        botonGuardar.setOnAction(e -> guardar());
        alternar(botonGuardar, puedeEscribir);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox tarjeta = new HBox(16, avatar, textos, alergia, espacio, cambiar, exportar, botonGuardar);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getStyleClass().addAll("card", "odontograma-paciente");
        return tarjeta;
    }

    private Node construirBarraYArcada() {
        HBox barra = new HBox(8);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("odontograma-barra");
        Label marcar = new Label("Marcar como:");
        marcar.getStyleClass().add("campo-etiqueta");
        barra.getChildren().add(marcar);
        for (EstadoPieza estado : EstadoPieza.values()) {
            barra.getChildren().add(crearHerramienta(estado));
        }
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label ayuda = new Label("Notación FDI · haz clic en una pieza para aplicar el estado");
        ayuda.getStyleClass().add("texto-apoyo");
        barra.getChildren().addAll(espacio, ayuda);
        alternar(barra, puedeEscribir);
        grupoHerramienta.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null) {
                antes.setSelected(true);
            } else if (ahora != null) {
                herramienta = (EstadoPieza) ahora.getUserData();
            }
        });

        grupoPiezas.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null && antes != null && seleccionada != null) {
                antes.setSelected(true);   // una pieza seleccionada se queda seleccionada
            }
        });

        VBox arcada = new VBox(10,
                rotulos("Derecha del paciente", "Maxilar superior", "Izquierda del paciente"),
                fila(SUPERIOR), fila(INFERIOR),
                rotulos("Derecha del paciente", "Mandíbula inferior", "Izquierda del paciente"));
        arcada.setAlignment(Pos.CENTER);
        arcada.getStyleClass().add("odontograma-arcada");

        VBox tarjeta = new VBox(barra, arcada, aviso);
        tarjeta.getStyleClass().addAll("card", "odontograma-tarjeta");
        return tarjeta;
    }

    private ToggleButton crearHerramienta(EstadoPieza estado) {
        Label muestra = new Label(estado.getCodigo());
        muestra.getStyleClass().addAll("pieza-muestra", "pieza-" + estado.name().toLowerCase().replace('_', '-'));
        Label cuenta = new Label();
        cuenta.getStyleClass().add("texto-apoyo");
        conteos.put(estado, cuenta);
        Label texto = new Label(estado.getEtiqueta());
        HBox contenido = new HBox(8, muestra, texto, cuenta);
        contenido.setAlignment(Pos.CENTER_LEFT);
        ToggleButton boton = new ToggleButton();
        boton.setGraphic(contenido);
        boton.getStyleClass().add("herramienta");
        boton.setUserData(estado);
        boton.setToggleGroup(grupoHerramienta);
        boton.setSelected(estado == herramienta);
        return boton;
    }

    private Node rotulos(String izquierda, String centro, String derecha) {
        Label a = new Label(izquierda);
        Label b = new Label(centro);
        Label c = new Label(derecha);
        for (Label l : List.of(a, b, c)) {
            l.getStyleClass().add("odontograma-rotulo");
        }
        Region r1 = new Region();
        Region r2 = new Region();
        HBox.setHgrow(r1, Priority.ALWAYS);
        HBox.setHgrow(r2, Priority.ALWAYS);
        HBox fila = new HBox(a, r1, b, r2, c);
        fila.setMaxWidth(980);
        return fila;
    }

    private Node fila(int[] numeros) {
        HBox izquierda = new HBox(4);
        HBox derecha = new HBox(4);
        for (int i = 0; i < numeros.length; i++) {
            PiezaDental pieza = new PiezaDental(numeros[i]);
            pieza.setToggleGroup(grupoPiezas);
            pieza.setOnAction(e -> alHacerClic(pieza.getNumero()));
            piezas.put(numeros[i], pieza);
            (i < numeros.length / 2 ? izquierda : derecha).getChildren().add(pieza);
        }
        Region lineaMedia = new Region();
        lineaMedia.getStyleClass().add("odontograma-linea-media");
        HBox fila = new HBox(18, izquierda, lineaMedia, derecha);
        fila.setAlignment(Pos.CENTER);
        return fila;
    }

    private Node construirDetalle() {
        Label tituloPieza = new Label("PIEZA SELECCIONADA");
        tituloPieza.getStyleClass().add("seccion-titulo");
        VBox tarjetaPieza = new VBox(12, tituloPieza, detallePieza);
        tarjetaPieza.getStyleClass().addAll("card", "odontograma-tarjeta");
        tarjetaPieza.setPrefWidth(420);
        tarjetaPieza.setMinWidth(320);

        Label tituloHistorial = new Label("HISTORIAL DE LA PIEZA");
        tituloHistorial.getStyleClass().add("seccion-titulo");
        VBox tarjetaHistorial = new VBox(12, tituloHistorial, historial);
        tarjetaHistorial.getStyleClass().addAll("card", "odontograma-tarjeta");
        HBox.setHgrow(tarjetaHistorial, Priority.ALWAYS);

        HBox fila = new HBox(18, tarjetaPieza, tarjetaHistorial);
        fila.setAlignment(Pos.TOP_LEFT);
        return fila;
    }

    // ===== Interacción =====

    private void alHacerClic(int numero) {
        seleccionada = numero;
        if (puedeEscribir) {
            if (herramienta == guardado.getOrDefault(numero, EstadoPieza.SANA)) {
                pendientes.remove(numero);
            } else {
                pendientes.put(numero, herramienta);
            }
        }
        aviso.ocultar();
        refrescar();
    }

    private void guardar() {
        if (pendientes.isEmpty()) {
            return;
        }
        try {
            servicio.registrarCambios(paciente.getIdPaciente(), pendientes, campoObservacion.getText());
        } catch (ReglaNegocioException | ValidacionException e) {
            aviso.mostrar(Aviso.Tipo.ERROR, e.getMessage());
            return;
        }
        int cambios = pendientes.size();
        pendientes.clear();
        campoObservacion.clear();
        aviso.ocultar();
        recargarGuardado();
        refrescar();
        Notificacion.mostrar(pagina.getScene().getWindow(),
                cambios == 1 ? "Se guardó 1 cambio en el odontograma" : "Se guardaron " + cambios + " cambios");
    }

    // ===== Pantalla =====

    private EstadoPieza estadoVisible(int numero) {
        return pendientes.getOrDefault(numero, guardado.getOrDefault(numero, EstadoPieza.SANA));
    }

    private void refrescar() {
        Map<EstadoPieza, Integer> cuenta = new EnumMap<>(EstadoPieza.class);
        for (Map.Entry<Integer, PiezaDental> entrada : piezas.entrySet()) {
            int numero = entrada.getKey();
            EstadoPieza estado = estadoVisible(numero);
            entrada.getValue().mostrar(estado, pendientes.containsKey(numero));
            entrada.getValue().setSelected(seleccionada != null && seleccionada == numero);
            cuenta.merge(estado, 1, Integer::sum);
        }
        conteos.forEach((estado, etiqueta) -> etiqueta.setText(String.valueOf(cuenta.getOrDefault(estado, 0))));

        int n = pendientes.size();
        botonGuardar.setDisable(n == 0);
        botonGuardar.setText(n == 0 ? "Guardado" : n == 1 ? "Guardar 1 cambio" : "Guardar " + n + " cambios");
        mostrarDetalle();
    }

    private void mostrarDetalle() {
        detallePieza.getChildren().clear();
        historial.getChildren().clear();
        if (seleccionada == null) {
            detallePieza.getChildren().add(textoTenue("Haz clic en una pieza para ver su estado y su historial."));
            historial.getChildren().add(textoTenue("Sin pieza seleccionada."));
            return;
        }
        int numero = seleccionada;
        EstadoPieza estado = estadoVisible(numero);

        Label cuadro = new Label(String.valueOf(numero));
        cuadro.getStyleClass().addAll("pieza-numero-grande", "pieza-" + estado.name().toLowerCase().replace('_', '-'));
        Label titulo = new Label("Pieza " + numero + " · " + nombrePieza(numero));
        titulo.getStyleClass().add("celda-titulo");
        titulo.setWrapText(true);
        Label actual = new Label("Estado actual: " + estado.getEtiqueta()
                + (pendientes.containsKey(numero) ? " (sin guardar)" : ""));
        actual.getStyleClass().add("celda-sub");
        HBox cabecera = new HBox(12, cuadro, new VBox(2, titulo, actual));
        cabecera.setAlignment(Pos.CENTER_LEFT);
        detallePieza.getChildren().add(cabecera);

        if (puedeEscribir) {
            Label etiqueta = new Label("Observación del cambio");
            etiqueta.getStyleClass().add("campo-etiqueta");
            campoObservacion.setPromptText("Ej. Resina compuesta clase II");
            detallePieza.getChildren().addAll(etiqueta, campoObservacion);
        }
        Button tratamiento = new Button("Registrar tratamiento para esta pieza");
        tratamiento.getStyleClass().add("enlace");
        tratamiento.setOnAction(e -> {
            if (puedeSalir()) {
                MainController.actual().navegar(Pantalla.TRATAMIENTOS, numero);
            }
        });
        alternar(tratamiento, Pantalla.TRATAMIENTOS.permitidaPara(
                com.vyra.dental.service.Sesion.get().getUsuario().getRol()));
        detallePieza.getChildren().add(tratamiento);

        // Historial: lo sin guardar arriba, luego los registros de la base de datos
        List<OdontogramaPieza> registros = servicio.historialPieza(paciente.getIdPaciente(), numero);
        boolean hayPendiente = pendientes.containsKey(numero);
        if (!hayPendiente && registros.isEmpty()) {
            historial.getChildren().add(textoTenue("Sin cambios registrados: la pieza figura como sana."));
            return;
        }
        int total = registros.size() + (hayPendiente ? 1 : 0);
        int indice = 0;
        if (hayPendiente) {
            historial.getChildren().add(entrada(estado.getEtiqueta(), "Hoy · sin guardar", null, ++indice == total));
        }
        for (OdontogramaPieza r : registros) {
            historial.getChildren().add(entrada(r.getEstado().getEtiqueta(), Formatos.diaCorto(r.getFechaRegistro())
                    + " " + Formatos.fechaCorta(r.getFechaRegistro()), r.getObservacion(), ++indice == total));
        }
    }

    private Node entrada(String estado, String fecha, String observacion, boolean ultima) {
        Region punto = new Region();
        punto.getStyleClass().add("hc-punto");
        Region linea = new Region();
        linea.getStyleClass().add("hc-linea");
        VBox.setVgrow(linea, Priority.ALWAYS);
        linea.setVisible(!ultima);
        VBox riel = new VBox(punto, linea);
        riel.getStyleClass().add("hc-riel");

        Label titulo = new Label(estado);
        titulo.getStyleClass().add("celda-titulo");
        Label cuando = new Label(fecha);
        cuando.getStyleClass().add("celda-sub");
        VBox textos = new VBox(2, titulo, cuando);
        if (observacion != null) {
            Label nota = new Label(observacion);
            nota.setWrapText(true);
            textos.getChildren().add(nota);
        }
        textos.getStyleClass().add("hc-entrada-texto");
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox fila = new HBox(12, riel, textos);
        fila.getStyleClass().add("hc-entrada");
        return fila;
    }

    /** "Primer molar inferior izquierdo" a partir de la notación FDI. */
    static String nombrePieza(int numero) {
        return POSICIONES[numero % 10] + " " + CUADRANTES[numero / 10];
    }

    private static Label textoTenue(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("texto-tenue");
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
