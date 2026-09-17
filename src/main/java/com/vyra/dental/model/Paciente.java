package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    private int idPaciente;
    private String nombres;
    private String apellidos;
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String direccion;
    private String contactoEmergencia;
    private String alergias;
    private LocalDate fechaRegistro;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}