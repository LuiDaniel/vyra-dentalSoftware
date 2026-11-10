package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;

/**
 * Contrato de acceso a datos para Tratamiento. Los tratamientos se leen con
 * sus piezas dentales (tabla tratamiento_pieza).
 */
public interface TratamientoDAO {
    /** Guarda el tratamiento y sus piezas en una sola transacción. */
    Tratamiento crear(Tratamiento tratamiento);
    Optional<Tratamiento> buscarPorId(int idTratamiento);
    List<Tratamiento> listarTodos();

    /** Tratamientos de las citas del paciente, del más reciente al más antiguo. */
    List<Tratamiento> listarPorPaciente(int idPaciente);
    List<Tratamiento> listarPorEstado(EstadoTratamiento estado);

    /** Actualiza los datos y reemplaza sus piezas, en una sola transacción. */
    boolean actualizar(Tratamiento tratamiento);
    boolean actualizarEstado(int idTratamiento, EstadoTratamiento estado);
    boolean eliminar(int idTratamiento);

    /**
     * Tratamientos con paciente, profesional y lo pagado, del más reciente al
     * más antiguo. {@code textoPaciente} filtra por nombre o DNI (sin tildes ni
     * mayúsculas; vacío = todos) y {@code estado} por estado (null = todos).
     */
    List<TratamientoResumen> buscarResumenes(String textoPaciente, EstadoTratamiento estado);
    Optional<TratamientoResumen> buscarResumen(int idTratamiento);
    List<TratamientoResumen> resumenesDePaciente(int idPaciente);
}
