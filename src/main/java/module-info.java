module com.vyra.dental {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;
    requires java.sql.rowset;
    requires java.prefs;
    requires jakarta.validation;
    requires org.hibernate.validator;

    opens com.vyra.dental to javafx.fxml;
    // javafx.base lee las propiedades (PropertyValueFactory); hibernate lee los campos anotados
    opens com.vyra.dental.model to javafx.base, org.hibernate.validator;
    // FXMLLoader crea los controladores y componentes por reflexión
    opens com.vyra.dental.controller to javafx.fxml;
    opens com.vyra.dental.ui to javafx.fxml;
    exports com.vyra.dental;
    exports com.vyra.dental.model;
    exports com.vyra.dental.dao;
    exports com.vyra.dental.dao.impl;
    exports com.vyra.dental.dao.memoria;
    exports com.vyra.dental.service;
    exports com.vyra.dental.controller;
    exports com.vyra.dental.ui;
    exports com.vyra.dental.util;
    exports com.vyra.dental.demo;
}
