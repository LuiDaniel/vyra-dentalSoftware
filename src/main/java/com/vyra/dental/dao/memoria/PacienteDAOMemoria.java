package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de PacienteDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 */
public class PacienteDAOMemoria implements PacienteDAO {

    private final List<Paciente> pacientes = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Paciente crear(Paciente paciente) {
        Validador.validar(paciente);
        paciente.setIdPaciente(siguienteId++);
        pacientes.add(paciente);
        return paciente;
    }

    @Override
    public Optional<Paciente> buscarPorId(int idPaciente) {
        return pacientes.stream().filter(p -> p.getIdPaciente() == idPaciente).findFirst();
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        return pacientes.stream().filter(p -> p.getDni() != null && p.getDni().equals(dni)).findFirst();
    }

    @Override
    public List<Paciente> listarTodos() {
        return new ArrayList<>(pacientes);
    }

    @Override
    public boolean actualizar(Paciente paciente) {
        Validador.validar(paciente);
        for (int i = 0; i < pacientes.size(); i++) {
            if (pacientes.get(i).getIdPaciente() == paciente.getIdPaciente()) {
                pacientes.set(i, paciente);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eliminar(int idPaciente) {
        return pacientes.removeIf(p -> p.getIdPaciente() == idPaciente);
    }
}
