package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de TratamientoDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 */
public class TratamientoDAOMemoria implements TratamientoDAO {

    private final List<Tratamiento> tratamientos = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Tratamiento crear(Tratamiento tratamiento) {
        Validador.validar(tratamiento);
        tratamiento.setIdTratamiento(siguienteId++);
        tratamientos.add(tratamiento);
        return tratamiento;
    }

    @Override
    public Optional<Tratamiento> buscarPorId(int idTratamiento) {
        return tratamientos.stream().filter(t -> t.getIdTratamiento() == idTratamiento).findFirst();
    }

    @Override
    public List<Tratamiento> listarTodos() {
        return new ArrayList<>(tratamientos);
    }

    @Override
    public boolean actualizar(Tratamiento tratamiento) {
        Validador.validar(tratamiento);
        for (int i = 0; i < tratamientos.size(); i++) {
            if (tratamientos.get(i).getIdTratamiento() == tratamiento.getIdTratamiento()) {
                tratamientos.set(i, tratamiento);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eliminar(int idTratamiento) {
        return tratamientos.removeIf(t -> t.getIdTratamiento() == idTratamiento);
    }
}
