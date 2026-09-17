package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profesional {

    private int idProfesional;
    private Integer idUsuario;
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String telefono;
    private String horarioAtencion;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}