module com.vyra.dental {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;

    opens com.vyra.dental to javafx.fxml;
    exports com.vyra.dental;
}