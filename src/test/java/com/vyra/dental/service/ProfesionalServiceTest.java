package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.ProfesionalDAOMemoria;
import com.vyra.dental.dao.memoria.UsuarioDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.ValidacionException;

class ProfesionalServiceTest {

    private UsuarioDAOMemoria usuarioDAO;
    private CitaDAOMemoria citaDAO;
    private ProfesionalService servicio;
    private Usuario odontologo;

    @BeforeEach
    void preparar() {
        usuarioDAO = new UsuarioDAOMemoria();
        citaDAO = new CitaDAOMemoria();
        servicio = new ProfesionalService(new ProfesionalDAOMemoria(), usuarioDAO, citaDAO);
        odontologo = usuarioDAO.crear(new Usuario(0, "msalinas", "Marco Salinas", "pbkdf2$1$a$b", Rol.ODONTOLOGO, true));
        usuarioDAO.crear(new Usuario(0, "lramos", "Lucía Ramos", "pbkdf2$1$a$b", Rol.RECEPCION, true));
        Sesion.get().iniciar(new Usuario(99, "admin", "Administrador", "pbkdf2$1$a$b", Rol.ADMIN, true), null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void registraConUsuarioOdontologoYTelefonoConEspacios() {
        Profesional p = servicio.registrar(profesional("Marco", "Salinas", "987 654 321", odontologo.getIdUsuario()));

        assertTrue(p.getIdProfesional() > 0);
        assertEquals("987654321", p.getTelefono());
    }

    @Test
    void validaNombresYTelefono() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> servicio.registrar(profesional("", "Salinas", "123", null)));
        assertEquals("Los nombres son obligatorios", error.getErrores().get("nombres"));
        assertEquals("El teléfono debe tener 9 dígitos", error.getErrores().get("telefono"));
    }

    @Test
    void soloSeAsociaUnUsuarioOdontologoYUnaSolaVez() {
        int recepcion = usuarioDAO.buscarPorNombreUsuario("lramos").orElseThrow().getIdUsuario();
        assertEquals("idUsuario", assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(profesional("Lucía", "Ramos", null, recepcion))).getCampo().orElseThrow());

        servicio.registrar(profesional("Marco", "Salinas", null, odontologo.getIdUsuario()));
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(profesional("Otro", "Profesional", null, odontologo.getIdUsuario())));
        assertEquals("Ese usuario ya está asociado a Marco Salinas.", error.getMessage());
    }

    @Test
    void usuariosDisponiblesExcluyeLosYaAsociados() {
        Profesional salinas = servicio.registrar(profesional("Marco", "Salinas", null, odontologo.getIdUsuario()));

        assertTrue(servicio.usuariosDisponibles(0).isEmpty());
        assertEquals(List.of("msalinas"),
                servicio.usuariosDisponibles(salinas.getIdProfesional()).stream().map(Usuario::getNombreUsuario).toList());
    }

    @Test
    void noSeEliminaUnProfesionalConCitas() {
        Profesional salinas = servicio.registrar(profesional("Marco", "Salinas", null, null));
        Profesional nunez = servicio.registrar(profesional("Patricia", "Núñez", null, null));
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(1);
        citaDAO.crear(new Cita(0, paciente, salinas, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 30),
                null, EstadoCita.PENDIENTE));

        assertThrows(ReglaNegocioException.class, () -> servicio.eliminar(salinas.getIdProfesional()));
        servicio.eliminar(nunez.getIdProfesional());
        assertEquals(1, servicio.listarTodos().size());
    }

    @Test
    void soloElAdministradorEscribe() {
        Sesion.get().iniciar(new Usuario(2, "lramos", "Lucía Ramos", "pbkdf2$1$a$b", Rol.RECEPCION, true), null);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(profesional("Marco", "Salinas", null, null)));
        assertEquals(0, servicio.listarTodos().size());
    }

    private static Profesional profesional(String nombres, String apellidos, String telefono, Integer idUsuario) {
        return new Profesional(0, idUsuario, nombres, apellidos, "Endodoncia", telefono, "Lun-Vie 08:00-14:00");
    }
}
