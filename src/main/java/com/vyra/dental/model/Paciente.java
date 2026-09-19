package com.vyra.dental.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 

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

    public String getNombreCompleto(){
        return nombres + " " + apellidos;
    }
}
