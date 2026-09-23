module com.vyra.dental {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;
    requires java.sql.rowset;

    opens com.vyra.dental to javafx.fxml;
    exports com.vyra.dental;
    exports com.vyra.dental.model;
    exports com.vyra.dental.dao;
    exports com.vyra.dental.dao.impl;
    exports com.vyra.dental.util;
    exports com.vyra.dental.demo;
}
