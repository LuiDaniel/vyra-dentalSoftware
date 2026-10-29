package com.vyra.dental.dao;

import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;

import java.util.List;
import java.util.Optional;

public interface PacienteDAO {
    Paciente crear(Paciente paciente);
    Optional<Paciente> buscarPorId(int idPaciente);

    /** Busca también entre los dados de baja: el DNI no se puede repetir nunca (RN-01). */
    Optional<Paciente> buscarPorDni(String dni);

    /** Pacientes activos cuyo DNI o nombre completo contiene el texto, ordenados por apellidos. */
    List<Paciente> buscar(String texto, FiltroPaciente filtro, int limite, int desde);
    int contarActivos(String texto, FiltroPaciente filtro);
    boolean actualizar(Paciente paciente);

    /** Baja lógica (RN-04): activo = 0. */
    boolean darDeBaja(int idPaciente);
}
