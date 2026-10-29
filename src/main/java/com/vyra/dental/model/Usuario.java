package com.vyra.dental.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuenta de acceso al sistema. La contraseña se guarda solo como hash
 * (util.HashUtil); esta clase no calcula ni verifica hashes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    private int idUsuario;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 30, message = "El nombre de usuario debe tener entre 3 y 30 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasenaHash;

    @NotNull(message = "Debes seleccionar un rol")
    private Rol rol;

    private boolean activo = true;
}
