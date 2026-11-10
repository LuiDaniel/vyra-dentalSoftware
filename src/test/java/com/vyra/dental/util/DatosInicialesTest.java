package com.vyra.dental.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.HistoriaClinicaDAOMemoria;
import com.vyra.dental.dao.memoria.OdontogramaDAOMemoria;
import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.dao.memoria.ProfesionalDAOMemoria;
import com.vyra.dental.dao.memoria.UsuarioDAOMemoria;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class DatosInicialesTest {

    @Test
    void creaElAdminCuandoNoHayUsuarios() {
        UsuarioDAOMemoria dao = new UsuarioDAOMemoria();

        DatosIniciales.crearAdminSiNoExiste(dao);

        Usuario admin = dao.buscarPorNombreUsuario("admin").orElseThrow();
        assertEquals(Rol.ADMIN, admin.getRol());
        assertTrue(admin.isActivo());
        assertTrue(HashUtil.verificar("admin123", admin.getContrasenaHash()));
    }

    @Test
    void noCreaNadaSiYaHayUsuarios() {
        UsuarioDAOMemoria dao = new UsuarioDAOMemoria();
        dao.crear(new Usuario(0, "recepcion1", "Lucía Ramos", HashUtil.hashear("clave123"), Rol.RECEPCION, true));

        DatosIniciales.crearAdminSiNoExiste(dao);
        DatosIniciales.crearAdminSiNoExiste(dao);

        assertEquals(1, dao.contar());
        assertTrue(dao.buscarPorNombreUsuario("admin").isEmpty());
    }

    @Test
    void losUsuariosDeEjemploSeCreanUnaVezYElOdontologoQuedaAsociadoASuProfesional() {
        UsuarioDAOMemoria usuarios = new UsuarioDAOMemoria();
        ProfesionalDAOMemoria profesionales = new ProfesionalDAOMemoria();
        Profesional salinas = profesionales.crear(
                new Profesional(0, null, "Marco", "Salinas Vega", "Endodoncia", null, null));

        DatosIniciales.cargarUsuariosDeEjemplo(usuarios, profesionales);
        DatosIniciales.cargarUsuariosDeEjemplo(usuarios, profesionales);

        assertEquals(2, usuarios.contar());
        Usuario odontologo = usuarios.buscarPorNombreUsuario("msalinas").orElseThrow();
        assertEquals(Rol.ODONTOLOGO, odontologo.getRol());
        assertTrue(HashUtil.verificar(DatosIniciales.CONTRASENA_EJEMPLO, odontologo.getContrasenaHash()));
        assertEquals(odontologo.getIdUsuario(), profesionales.buscarPorId(salinas.getIdProfesional())
                .orElseThrow().getIdUsuario());
        assertEquals(Rol.RECEPCION, usuarios.buscarPorNombreUsuario("recepcion").orElseThrow().getRol());
    }

    @Test
    void elOdontogramaDeEjemploCoincideConElDisenoYSeCargaUnaVez() {
        PacienteDAOMemoria pacientes = new PacienteDAOMemoria();
        ProfesionalDAOMemoria profesionales = new ProfesionalDAOMemoria();
        OdontogramaDAOMemoria odontograma = new OdontogramaDAOMemoria();
        HistoriaClinicaDAOMemoria historia = new HistoriaClinicaDAOMemoria();
        DatosIniciales.cargarEjemplos(pacientes);
        int maria = pacientes.buscarPorDni("72481935").orElseThrow().getIdPaciente();

        DatosIniciales.cargarEjemplosClinicos(pacientes, profesionales, odontograma, historia);
        DatosIniciales.cargarEjemplosClinicos(pacientes, profesionales, odontograma, historia);

        // docs/ui/Odontograma.html: 2 cariadas, 3 obturadas, 1 en tratamiento (36) y 2 extraídas
        Map<EstadoPieza, Long> conteo = odontograma.estadoActual(maria).stream()
                .collect(Collectors.groupingBy(OdontogramaPieza::getEstado, Collectors.counting()));
        assertEquals(Map.of(EstadoPieza.CARIADA, 2L, EstadoPieza.OBTURADA, 3L, EstadoPieza.EN_TRATAMIENTO, 1L,
                EstadoPieza.EXTRAIDA, 2L), conteo);
        assertEquals(2, odontograma.historialPieza(maria, 36).size());
        assertEquals(3, historia.listarPorPaciente(maria).size());
    }
}
