package com.vyra.dental.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;

/**
 * Contrato de acceso a datos para Cita. Las citas que devuelve traen el
 * paciente (id, nombres, apellidos, DNI) y el profesional (id, nombres,
 * apellidos, especialidad), porque la agenda siempre los muestra.
 */
public interface CitaDAO {
    Cita crear(Cita cita);
    Optional<Cita> buscarPorId(int idCita);
    List<Cita> listarPorFecha(LocalDate fecha);
    List<Cita> listarPorProfesionalYFecha(int idProfesional, LocalDate fecha);

    /** Citas con fecha entre desde y hasta (ambas incluidas), por fecha y hora. */
    List<Cita> listarEntre(LocalDate desde, LocalDate hasta);

    /** Citas del paciente, de la más reciente a la más antigua. */
    List<Cita> listarPorPaciente(int idPaciente);

    /**
     * Primera cita PENDIENTE o CONFIRMADA del profesional que se traslapa con
     * el rango dado (docs/03-modelo-base-datos.md §5.2). {@code excluirId} es
     * la cita que se está reprogramando (0 si es nueva).
     */
    Optional<Cita> buscarCruce(int idProfesional, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluirId);

    default boolean existeCruce(int idProfesional, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluirId) {
        return buscarCruce(idProfesional, fecha, inicio, fin, excluirId).isPresent();
    }

    /** Fecha de la última cita ATENDIDA del paciente. */
    Optional<LocalDate> ultimaVisita(int idPaciente);

    int contarPorProfesional(int idProfesional);
    boolean actualizar(Cita cita);
    boolean actualizarEstado(int idCita, EstadoCita estado);
}
