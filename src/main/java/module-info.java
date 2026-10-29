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

    // Java no permite exportar ni abrir un paquete sin clases: descomentar cada línea
    // al crear la primera clase del paquete (ver docs/09-plan-implementacion.md).
    // Fase 1:
    // exports com.vyra.dental.dao.memoria;
    // exports com.vyra.dental.service;
    // Fase 2 (FXMLLoader crea los controladores y componentes por reflexión):
    // opens com.vyra.dental.controller to javafx.fxml;
    // opens com.vyra.dental.ui to javafx.fxml;
    // exports com.vyra.dental.controller;
    // exports com.vyra.dental.ui;
}
