package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.ValidacionException;

class PacienteServiceTest {

    private PacienteDAOMemoria pacienteDAO;
    private PacienteService servicio;

    @BeforeEach
    void preparar() {
        pacienteDAO = new PacienteDAOMemoria();
        servicio = new PacienteService(pacienteDAO);
        iniciarSesionComo(Rol.RECEPCION);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void registraUnPacienteActivoConFechaDeRegistro() {
        Paciente registrado = servicio.registrar(paciente("María", "Quispe", "72481935"));

        assertTrue(registrado.getIdPaciente() > 0);
        assertTrue(registrado.isActivo());
        assertEquals(LocalDate.now(), registrado.getFechaRegistro());
    }

    @Test
    void limpiaEspaciosYAceptaElTelefonoConEspacios() {
        Paciente nuevo = paciente("  María ", " Quispe ", "72481935");
        nuevo.setTelefono("987 654 321");
        nuevo.setDireccion("   ");

        Paciente registrado = servicio.registrar(nuevo);

        assertEquals("María", registrado.getNombres());
        assertEquals("Quispe", registrado.getApellidos());
        assertEquals("987654321", registrado.getTelefono());
        assertNull(registrado.getDireccion());
    }

    @Test
    void noPermiteUnDniRepetido() {
        servicio.registrar(paciente("María", "Quispe", "72481935"));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(paciente("Otra", "Persona", "72481935")));

        assertEquals("Ya existe un paciente con el DNI 72481935", error.getMessage());
        assertEquals("dni", error.getCampo().orElseThrow());
    }

    @Test
    void elDniDeUnPacienteDadoDeBajaTampocoSeRepite() {
        Paciente antiguo = servicio.registrar(paciente("María", "Quispe", "72481935"));
        servicio.darDeBaja(antiguo.getIdPaciente());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(paciente("Otra", "Persona", "72481935")));

        assertEquals("Ya existe un paciente con el DNI 72481935 (dado de baja)", error.getMessage());
    }

    @Test
    void indicaQueCampoTieneElError() {
        Paciente invalido = paciente("", "Quispe", "7248");
        invalido.setTelefono("12345");

        ValidacionException error = assertThrows(ValidacionException.class, () -> servicio.registrar(invalido));

        assertEquals("Los nombres son obligatorios", error.getErrores().get("nombres"));
        assertEquals("El DNI debe tener 8 dígitos", error.getErrores().get("dni"));
        assertEquals("El teléfono debe tener 9 dígitos", error.getErrores().get("telefono"));
    }

    @Test
    void actualizarConservaSuPropioDniPeroNoAceptaElDeOtro() {
        Paciente maria = servicio.registrar(paciente("María", "Quispe", "72481935"));
        servicio.registrar(paciente("Jorge", "Mamani", "45812307"));

        maria.setAlergias("Penicilina");
        servicio.actualizar(maria);
        assertEquals("Penicilina", pacienteDAO.buscarPorId(maria.getIdPaciente()).orElseThrow().getAlergias());

        // Como en la pantalla: se envía un objeto nuevo con el id del paciente editado
        Paciente conDniAjeno = paciente("María", "Quispe", "45812307");
        conDniAjeno.setIdPaciente(maria.getIdPaciente());
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(conDniAjeno));
        assertEquals("dni", error.getCampo().orElseThrow());
    }

    @Test
    void darDeBajaNoBorraAlPaciente() {
        Paciente maria = servicio.registrar(paciente("María", "Quispe", "72481935"));

        servicio.darDeBaja(maria.getIdPaciente());

        assertEquals(0, servicio.contar("", FiltroPaciente.TODOS));
        assertTrue(servicio.buscar("María", FiltroPaciente.TODOS, 0).isEmpty());
        assertFalse(servicio.buscarPorId(maria.getIdPaciente()).orElseThrow().isActivo());
        assertThrows(ReglaNegocioException.class, () -> servicio.darDeBaja(maria.getIdPaciente()));
    }

    @Test
    void buscaPorNombreCompletoODni() {
        servicio.registrar(paciente("María Fernanda", "Quispe Huamán", "72481935"));
        servicio.registrar(paciente("Jorge", "Mamani Condori", "45812307"));

        assertEquals(1, servicio.buscar("maría fernanda quispe", FiltroPaciente.TODOS, 0).size());
        assertEquals("Jorge", servicio.buscar("4581", FiltroPaciente.TODOS, 0).get(0).getNombres());
        assertEquals(2, servicio.contar("", FiltroPaciente.TODOS));
    }

    @Test
    void paginaDeVeinteEnVeinte() {
        for (int i = 0; i < 25; i++) {
            servicio.registrar(paciente("Paciente", String.format("Prueba %02d", i), String.format("100000%02d", i)));
        }

        List<Paciente> primera = servicio.buscar("", FiltroPaciente.TODOS, 0);
        List<Paciente> segunda = servicio.buscar("", FiltroPaciente.TODOS, 1);

        assertEquals(PacienteService.TAMANO_PAGINA, primera.size());
        assertEquals(5, segunda.size());
        assertEquals("Prueba 00", primera.get(0).getApellidos());
        assertEquals("Prueba 24", segunda.get(4).getApellidos());
    }

    @Test
    void filtraPorAlergiasYMenoresDeEdad() {
        Paciente conAlergia = paciente("Lucía", "Torres", "70356219");
        conAlergia.setAlergias("Látex");
        Paciente menor = paciente("Diego", "Vargas", "71592846");
        menor.setFechaNacimiento(LocalDate.now().minusYears(8));
        Paciente adulto = paciente("Carlos", "Pérez", "43920185");
        adulto.setFechaNacimiento(LocalDate.now().minusYears(41));
        servicio.registrar(conAlergia);
        servicio.registrar(menor);
        servicio.registrar(adulto);

        assertEquals("Lucía", servicio.buscar("", FiltroPaciente.CON_ALERGIAS, 0).get(0).getNombres());
        assertEquals(1, servicio.contar("", FiltroPaciente.CON_ALERGIAS));
        assertEquals("Diego", servicio.buscar("", FiltroPaciente.MENORES_DE_EDAD, 0).get(0).getNombres());
        assertEquals(1, servicio.contar("", FiltroPaciente.MENORES_DE_EDAD));
    }

    @Test
    void elOdontologoSoloPuedeConsultar() {
        Paciente maria = servicio.registrar(paciente("María", "Quispe", "72481935"));
        iniciarSesionComo(Rol.ODONTOLOGO);

        assertFalse(servicio.puedeEditar());
        assertEquals(PacienteService.SIN_PERMISO,
                assertThrows(ReglaNegocioException.class,
                        () -> servicio.registrar(paciente("Jorge", "Mamani", "45812307"))).getMessage());
        assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(maria));
        assertThrows(ReglaNegocioException.class, () -> servicio.darDeBaja(maria.getIdPaciente()));
        assertNotNull(servicio.buscarPorId(maria.getIdPaciente()).orElse(null));
        assertEquals(1, servicio.buscar("", FiltroPaciente.TODOS, 0).size());
    }

    private static Paciente paciente(String nombres, String apellidos, String dni) {
        Paciente paciente = new Paciente();
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setDni(dni);
        return paciente;
    }

    private static void iniciarSesionComo(Rol rol) {
        Sesion.get().iniciar(new Usuario(1, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), null);
    }
}
