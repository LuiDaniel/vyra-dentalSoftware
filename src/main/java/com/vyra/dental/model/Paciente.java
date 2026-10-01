package com.vyra.dental.model;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Paciente {
    private int idPaciente;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^$|\\d{8}", message = "El DNI debe tener 8 digitos")
    private String dni;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^$|\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;
    private String direccion;
    private String contactoEmergencia;
    private String alergias;

    public String getNombreCompleto(){
        return nombres + " " + apellidos;
    }
}
