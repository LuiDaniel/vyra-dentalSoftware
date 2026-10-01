module com.vyra.dental {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;
    requires java.sql.rowset;
    requires jakarta.validation;
    requires org.hibernate.validator;

    opens com.vyra.dental to javafx.fxml;
    // javafx.base lee las propiedades (PropertyValueFactory); hibernate lee los campos anotados
    opens com.vyra.dental.model to javafx.base, org.hibernate.validator;
    exports com.vyra.dental;
    exports com.vyra.dental.model;
    exports com.vyra.dental.dao;
    exports com.vyra.dental.dao.impl;
    exports com.vyra.dental.util;
    exports com.vyra.dental.demo;
}
