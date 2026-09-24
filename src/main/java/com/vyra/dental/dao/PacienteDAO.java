package com.vyra.dental.dao;

import com.vyra.dental.model.Paciente;

import java.util.List;
import java.util.Optional;

public interface PacienteDAO {
    Paciente crear(Paciente paciente);
    Optional<Paciente> buscarPorId(int idPaciente);
    Optional<Paciente> buscarPorDni(String dni);
    List<Paciente> listarTodos();
    boolean actualizar(Paciente paciente);
    boolean eliminar(int idPaciente);
}
}
