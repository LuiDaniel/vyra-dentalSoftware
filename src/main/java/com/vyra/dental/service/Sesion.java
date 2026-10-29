package com.vyra.dental.service;

import java.util.Arrays;
import java.util.Optional;

import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

/**
 * Usuario que inició sesión (singleton). La llena AuthService; cualquier
 * controlador o servicio puede consultarla con {@code Sesion.get()}.
 */
public final class Sesion {

    private static final Sesion INSTANCIA = new Sesion();

    private Usuario usuario;
    private Profesional profesional;

    private Sesion() {
    }

    public static Sesion get() {
        return INSTANCIA;
    }

    void iniciar(Usuario usuario, Profesional profesional) {
        this.usuario = usuario;
        this.profesional = profesional;
    }

    void cerrar() {
        usuario = null;
        profesional = null;
    }

    public boolean estaIniciada() {
        return usuario != null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    /** El profesional asociado, solo si el usuario es odontólogo y tiene uno registrado. */
    public Optional<Profesional> getProfesional() {
        return Optional.ofNullable(profesional);
    }

    public boolean tieneRol(Rol... roles) {
        return usuario != null && Arrays.asList(roles).contains(usuario.getRol());
    }
}
