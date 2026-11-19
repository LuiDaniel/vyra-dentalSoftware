package com.vyra.dental.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Profesional {
    private int idProfesional;

    /** Cuenta de acceso asociada; null si el profesional no inicia sesión. */
    private Integer idUsuario;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    private String especialidad;

    @Pattern(regexp = "^$|\\d{9}", message = "El teléfono debe tener 9 dígitos")
    private String telefono;

    private String horarioAtencion;

    /** Costo de su consulta, que se copia a cada cita nueva (RN-16). 0 = sin costo. */
    @PositiveOrZero(message = "El costo de la consulta no puede ser negativo")
    private double costoConsulta;

    /** Sin costo de consulta. */
    public Profesional(int idProfesional, Integer idUsuario, String nombres, String apellidos, String especialidad,
                       String telefono, String horarioAtencion) {
        this(idProfesional, idUsuario, nombres, apellidos, especialidad, telefono, horarioAtencion, 0);
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
