package com.vyra.dental.controller;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.vyra.dental.model.Rol;

/**
 * Pantallas del menú lateral, en el orden en que aparecen. Los roles salen de
 * la matriz de permisos de docs/02-casos-de-uso.md §3: el menú muestra solo
 * las pantallas permitidas para el rol del usuario (RN-15).
 */
public enum Pantalla {
    INICIO("Inicio", "inicio", Grupo.PRINCIPAL, "InicioView", Rol.ADMIN, Rol.RECEPCION, Rol.ODONTOLOGO),
    AGENDA("Agenda", "agenda", Grupo.PRINCIPAL, "AgendaView", Rol.ADMIN, Rol.RECEPCION, Rol.ODONTOLOGO),
    PACIENTES("Pacientes", "pacientes", Grupo.PRINCIPAL, "PacientesView", Rol.ADMIN, Rol.RECEPCION, Rol.ODONTOLOGO),
    ODONTOGRAMA("Odontograma", "diente", Grupo.CLINICA, "OdontogramaView", Rol.ADMIN, Rol.ODONTOLOGO),
    HISTORIA_CLINICA("Historia clínica", "historia", Grupo.CLINICA, "HistoriaClinicaView", Rol.ADMIN, Rol.ODONTOLOGO),
    TRATAMIENTOS("Tratamientos", "tratamientos", Grupo.CLINICA, "TratamientosView", Rol.ADMIN, Rol.RECEPCION, Rol.ODONTOLOGO),
    PAGOS("Pagos", "pagos", Grupo.ADMINISTRACION, "PagosView", Rol.ADMIN, Rol.RECEPCION),
    REPORTES("Reportes", "reportes", Grupo.ADMINISTRACION, "ReportesView", Rol.ADMIN),
    PROFESIONALES("Profesionales", "profesionales", Grupo.ADMINISTRACION, "ProfesionalesView", Rol.ADMIN),
    USUARIOS("Usuarios y roles", "usuarios", Grupo.ADMINISTRACION, "UsuariosView", Rol.ADMIN);

    public enum Grupo {
        PRINCIPAL("Principal"),
        CLINICA("Clínica"),
        ADMINISTRACION("Administración");

        private final String titulo;

        Grupo(String titulo) {
            this.titulo = titulo;
        }

        public String getTitulo() {
            return titulo;
        }
    }

    private final String titulo;
    private final String icono;
    private final Grupo grupo;
    private final String vista;
    private final Set<Rol> roles;

    Pantalla(String titulo, String icono, Grupo grupo, String vista, Rol... roles) {
        this.titulo = titulo;
        this.icono = icono;
        this.grupo = grupo;
        this.vista = vista;
        this.roles = EnumSet.copyOf(List.of(roles));
    }

    public String getTitulo() {
        return titulo;
    }

    public String getIcono() {
        return icono;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    /** Ruta del FXML dentro de los recursos. */
    public String getRutaVista() {
        return "/com/vyra/views/" + vista + ".fxml";
    }

    public boolean permitidaPara(Rol rol) {
        return roles.contains(rol);
    }
}
