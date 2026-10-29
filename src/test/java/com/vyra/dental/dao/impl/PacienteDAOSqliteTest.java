package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml), compartida entre
 * pruebas: cada prueba usa apellidos y DNI propios y busca solo por ellos.
 */
class PacienteDAOSqliteTest {

    private final PacienteDAOSqlite dao = new PacienteDAOSqlite();

    @Test
    void guardaYLeeTodosLosCampos() {
        Paciente original = new Paciente(0, "María Fernanda", "Quispe Huamán", "80000001", LocalDate.of(1992, 3, 14),
                "987654321", "Av. Ejemplo 123", "Rosa Huamán 912345678", "Penicilina", "Hipertensión",
                true, LocalDate.of(2024, 3, 5));

        Paciente creado = dao.crear(original);
        Paciente leido = dao.buscarPorId(creado.getIdPaciente()).orElseThrow();

        assertEquals(creado, leido);
        assertEquals(leido, dao.buscarPorDni("80000001").orElseThrow());
    }

    @Test
    void sinFechaDeRegistroUsaHoy() {
        Paciente creado = dao.crear(paciente("Ana", "Registro Hoy", "80000002"));
        assertEquals(LocalDate.now(), dao.buscarPorId(creado.getIdPaciente()).orElseThrow().getFechaRegistro());
    }

    @Test
    void buscaPorNombreCompletoYDniOrdenadoPorApellidos() {
        dao.crear(paciente("Zoe", "Busqueda Beta", "80000011"));
        dao.crear(paciente("Ana", "Busqueda Alfa", "80000012"));

        List<Paciente> encontrados = dao.buscar("Busqueda", FiltroPaciente.TODOS, 10, 0);

        assertEquals(List.of("Busqueda Alfa", "Busqueda Beta"),
                encontrados.stream().map(Paciente::getApellidos).toList());
        assertEquals(1, dao.buscar("Ana Busqueda", FiltroPaciente.TODOS, 10, 0).size());
        assertEquals(1, dao.buscar("80000011", FiltroPaciente.TODOS, 10, 0).size());
        assertEquals(2, dao.contarActivos("Busqueda", FiltroPaciente.TODOS));
    }

    @Test
    void losComodinesDelUsuarioSeBuscanTalCual() {
        dao.crear(paciente("Raro", "Comodin_Test", "80000021"));
        dao.crear(paciente("Raro", "ComodinXTest", "80000022"));

        assertEquals(1, dao.contarActivos("Comodin_Test", FiltroPaciente.TODOS));
        assertEquals(0, dao.contarActivos("Comodin%Nada", FiltroPaciente.TODOS));
    }

    @Test
    void paginaConLimiteYDesde() {
        for (int i = 0; i < 5; i++) {
            dao.crear(paciente("Pag", "Paginado " + i, "8000003" + i));
        }

        assertEquals(List.of("Paginado 2", "Paginado 3"),
                dao.buscar("Paginado", FiltroPaciente.TODOS, 2, 2).stream().map(Paciente::getApellidos).toList());
    }

    @Test
    void filtrosDeAlergiasYMenores() {
        Paciente conAlergia = paciente("Lucía", "Filtro Sql", "80000041");
        conAlergia.setAlergias("Látex");
        Paciente alergiaEnBlanco = paciente("Pedro", "Filtro Sql", "80000042");
        alergiaEnBlanco.setAlergias("   ");
        Paciente menor = paciente("Diego", "Filtro Sql", "80000043");
        menor.setFechaNacimiento(LocalDate.now().minusYears(17));
        Paciente justo18 = paciente("Elena", "Filtro Sql", "80000044");
        justo18.setFechaNacimiento(LocalDate.now().minusYears(18));
        for (Paciente p : List.of(conAlergia, alergiaEnBlanco, menor, justo18)) {
            dao.crear(p);
        }

        assertEquals(List.of("Lucía"), nombres(dao.buscar("Filtro Sql", FiltroPaciente.CON_ALERGIAS, 10, 0)));
        assertEquals(List.of("Diego"), nombres(dao.buscar("Filtro Sql", FiltroPaciente.MENORES_DE_EDAD, 10, 0)));
        assertEquals(4, dao.contarActivos("Filtro Sql", FiltroPaciente.TODOS));
    }

    @Test
    void darDeBajaLoSacaDeLaBusquedaPeroNoLoBorra() {
        Paciente creado = dao.crear(paciente("Baja", "Logica Sql", "80000051"));

        assertTrue(dao.darDeBaja(creado.getIdPaciente()));
        assertFalse(dao.darDeBaja(creado.getIdPaciente()));
        assertEquals(0, dao.contarActivos("Logica Sql", FiltroPaciente.TODOS));
        assertFalse(dao.buscarPorId(creado.getIdPaciente()).orElseThrow().isActivo());
        assertTrue(dao.buscarPorDni("80000051").isPresent());
    }

    @Test
    void actualizaYRespetaElDniUnico() {
        Paciente uno = dao.crear(paciente("Uno", "Unico Sql", "80000061"));
        dao.crear(paciente("Dos", "Unico Sql", "80000062"));

        uno.setTelefono("912345678");
        assertTrue(dao.actualizar(uno));
        assertEquals("912345678", dao.buscarPorId(uno.getIdPaciente()).orElseThrow().getTelefono());

        uno.setDni("80000062");
        assertThrows(RuntimeException.class, () -> dao.actualizar(uno));
    }

    private static List<String> nombres(List<Paciente> pacientes) {
        return pacientes.stream().map(Paciente::getNombres).toList();
    }

    private static Paciente paciente(String nombres, String apellidos, String dni) {
        Paciente paciente = new Paciente();
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setDni(dni);
        return paciente;
    }
}
