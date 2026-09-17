module com.vyra.dental {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.vyra.dental to javafx.fxml;
    exports com.vyra.dental;
}
