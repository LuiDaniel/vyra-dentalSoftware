package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Profesional {

    private int idProfesional;
    private Integer idUsuario;
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String telefono;
    private String horarioAtencion;
}