package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.HistoriaClinicaEntrada;

/**
 * Entradas de la historia clínica. No hay actualizar ni eliminar: cada
 * entrada es una fila nueva y nunca se sobrescribe (RN-09).
 */
public interface HistoriaClinicaDAO {

    HistoriaClinicaEntrada crear(HistoriaClinicaEntrada entrada);

    Optional<HistoriaClinicaEntrada> buscarPorId(int idEntrada);

    /** De la más reciente a la más antigua. */
    List<HistoriaClinicaEntrada> listarPorPaciente(int idPaciente);
}
