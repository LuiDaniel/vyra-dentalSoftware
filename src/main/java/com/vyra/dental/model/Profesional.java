package com.vyra.dental.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
