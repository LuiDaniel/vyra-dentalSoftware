package com.vyra.dental.model;

import java.time.LocalDate;
import java.time.Period;

import com.vyra.dental.util.Formatos;

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
    @Pattern(regexp = "^$|\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^$|\\d{9}", message = "El teléfono debe tener 9 dígitos")
    private String telefono;
    private String direccion;
    private String contactoEmergencia;

    /** Texto libre; vacío significa que no tiene alergias. */
    private String alergias;
    private String antecedentes;

    /** Baja lógica (RN-04): un paciente dado de baja queda con activo = false. */
    private boolean activo = true;
    private LocalDate fechaRegistro;

    public String getNombreCompleto(){
        return nombres + " " + apellidos;
    }

    /** Años cumplidos; 0 si no se registró la fecha de nacimiento. */
    public int getEdad() {
        if (fechaNacimiento == null) {
            return 0;
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public String getIniciales() {
        return Formatos.iniciales(nombres, apellidos);
    }

    public boolean tieneAlergias() {
        return alergias != null && !alergias.isBlank();
    }
}
