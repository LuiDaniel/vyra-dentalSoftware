package com.vyra.dental.ui;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Etiqueta + control + mensaje de error bajo el campo (docs/08-guia-frontend.md §1).
 * En FXML:
 * <pre>{@code
 * <CampoFormulario etiqueta="DNI" obligatorio="true">
 *     <TextField fx:id="campoDni"/>
 * </CampoFormulario>
 * }</pre>
 */
@DefaultProperty("control")
public class CampoFormulario extends VBox {

    private final Label etiquetaLabel = new Label();
    private final Label mensajeError = new Label();

    private final StringProperty etiqueta = new SimpleStringProperty(this, "etiqueta", "");
    private final BooleanProperty obligatorio = new SimpleBooleanProperty(this, "obligatorio", false);
    private final ObjectProperty<Node> control = new SimpleObjectProperty<>(this, "control");

    public CampoFormulario() {
        getStyleClass().add("campo-formulario");
        etiquetaLabel.getStyleClass().add("campo-etiqueta");
        mensajeError.getStyleClass().add("mensaje-error");
        mensajeError.setWrapText(true);
        mostrarMensaje(false);

        etiqueta.addListener((obs, anterior, nuevo) -> actualizarEtiqueta());
        obligatorio.addListener((obs, anterior, nuevo) -> actualizarEtiqueta());
        control.addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                getChildren().setAll(etiquetaLabel, nuevo, mensajeError);
                etiquetaLabel.setLabelFor(nuevo);
            } else {
                getChildren().setAll(etiquetaLabel, mensajeError);
            }
        });
        getChildren().setAll(etiquetaLabel, mensajeError);
    }

    public void mostrarError(String mensaje) {
        mensajeError.setText(mensaje);
        mostrarMensaje(true);
        Node actual = control.get();
        if (actual != null && !actual.getStyleClass().contains("campo-error")) {
            actual.getStyleClass().add("campo-error");
        }
    }

    public void limpiarError() {
        mostrarMensaje(false);
        Node actual = control.get();
        if (actual != null) {
            actual.getStyleClass().remove("campo-error");
        }
    }

    private void mostrarMensaje(boolean visible) {
        mensajeError.setVisible(visible);
        mensajeError.setManaged(visible);
    }

    private void actualizarEtiqueta() {
        etiquetaLabel.setText(etiqueta.get() + (obligatorio.get() ? " *" : ""));
    }

    public String getEtiqueta() {
        return etiqueta.get();
    }

    public void setEtiqueta(String valor) {
        etiqueta.set(valor);
    }

    public StringProperty etiquetaProperty() {
        return etiqueta;
    }

    public boolean isObligatorio() {
        return obligatorio.get();
    }

    public void setObligatorio(boolean valor) {
        obligatorio.set(valor);
    }

    public BooleanProperty obligatorioProperty() {
        return obligatorio;
    }

    public Node getControl() {
        return control.get();
    }

    public void setControl(Node valor) {
        control.set(valor);
    }

    public ObjectProperty<Node> controlProperty() {
        return control;
    }
}
