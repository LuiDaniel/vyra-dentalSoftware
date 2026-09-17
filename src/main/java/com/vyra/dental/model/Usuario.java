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
public class Usuario {

    private int idUsuario;
    private String nombreUsuario;
    private String contrasenaHash;
    private Rol rol;
    private boolean activo;
}