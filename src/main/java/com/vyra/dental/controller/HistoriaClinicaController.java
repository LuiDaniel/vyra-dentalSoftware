package com.vyra.dental.controller;

import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.service.PacienteService;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.ui.Avatar;
import com.vyra.dental.ui.BuscadorPacientes;
import com.vyra.dental.util.Formatos;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/**
 * Pantalla Historia clínica (docs/08-guia-frontend.md §7.5): se elige un
 * paciente en el buscador y se muestra su historia clínica. Si se abre con un
 * paciente ({@code navegar(Pantalla.HISTORIA_CLINICA, paciente)}), lo muestra
 * directamente.
 */
public class HistoriaClinicaController implements RecibeParametro {

    @FXML private Label resumen;
    @FXML private BuscadorPacientes buscador;
    @FXML private VBox sinPaciente;
    @FXML private VBox panel;
    @FXML private Avatar avatarFicha;
    @FXML private Label tituloFicha;
    @FXML private Label subtituloFicha;
    @FXML private Button botonOdontograma;
    @FXML private HistoriaClinicaPanelController historiaController;

    private final PacienteService pacientes = Servicios.pacientes();
    private Paciente abierto;

    @FXML
    private void initialize() {
        resumen.setText(Servicios.historiaClinica().puedeEditar()
                ? "Elige un paciente para ver su historia clínica o registrar una nueva entrada."
                : "Elige un paciente para consultar su historia clínica.");
        botonOdontograma.setTooltip(new Tooltip("Ver odontograma"));
        historiaController.setAlActualizarPaciente(this::mostrarEncabezado);
        mostrarPanel(false);
        buscador.configurar(texto -> pacientes.buscar(texto, FiltroPaciente.TODOS, 0),
                texto -> pacientes.contar(texto, FiltroPaciente.TODOS),
                this::abrir);
        buscador.enfocar();
    }

    @Override
    public void recibir(Object parametro) {
        if (parametro instanceof Paciente paciente) {
            buscador.marcar(paciente.getIdPaciente());
            abrir(paciente);
        }
    }

    private void abrir(Paciente elegido) {
        // La fila de la tabla puede venir de otra pantalla con datos incompletos: se lee de nuevo
        Paciente paciente = pacientes.buscarPorId(elegido.getIdPaciente()).orElse(elegido);
        mostrarEncabezado(paciente);
        historiaController.mostrar(paciente);
        mostrarPanel(true);
    }

    private void mostrarEncabezado(Paciente paciente) {
        abierto = paciente;
        avatarFicha.mostrar(paciente.getIniciales(), paciente.getIdPaciente());
        tituloFicha.setText(paciente.getNombreCompleto());
        String edad = paciente.getFechaNacimiento() == null ? "" : " · " + Formatos.edad(paciente.getFechaNacimiento());
        subtituloFicha.setText("DNI " + paciente.getDni() + edad);
    }

    @FXML
    private void abrirOdontograma() {
        MainController.actual().navegar(Pantalla.ODONTOGRAMA, abierto);
    }

    private void mostrarPanel(boolean conPaciente) {
        alternar(panel, conPaciente);
        alternar(sinPaciente, !conPaciente);
    }

    private static void alternar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
