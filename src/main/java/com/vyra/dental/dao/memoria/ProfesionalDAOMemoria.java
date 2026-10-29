package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de ProfesionalDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 */
public class ProfesionalDAOMemoria implements ProfesionalDAO {

    private final List<Profesional> profesionales = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Profesional crear(Profesional profesional) {
        Validador.validar(profesional);
        profesional.setIdProfesional(siguienteId++);
        profesionales.add(profesional);
        return profesional;
    }

    @Override
    public Optional<Profesional> buscarPorId(int idProfesional) {
        return profesionales.stream().filter(p -> p.getIdProfesional() == idProfesional).findFirst();
    }

    @Override
    public Optional<Profesional> buscarPorIdUsuario(int idUsuario) {
        return profesionales.stream()
                .filter(p -> p.getIdUsuario() != null && p.getIdUsuario() == idUsuario)
                .findFirst();
    }

    @Override
    public List<Profesional> listarTodos() {
        return new ArrayList<>(profesionales);
    }

    @Override
    public boolean actualizar(Profesional profesional) {
        Validador.validar(profesional);
        for (int i = 0; i < profesionales.size(); i++) {
            if (profesionales.get(i).getIdProfesional() == profesional.getIdProfesional()) {
                profesionales.set(i, profesional);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eliminar(int idProfesional) {
        return profesionales.removeIf(p -> p.getIdProfesional() == idProfesional);
    }
}
