package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.HistoriaClinicaEntrada;

/**
 * Contrato de acceso a datos para la historia clínica. No tiene actualizar ni
 * eliminar: las entradas nunca se sobrescriben ni se borran (RN-09).
 */
public interface HistoriaClinicaDAO {
    HistoriaClinicaEntrada crear(HistoriaClinicaEntrada entrada);

    Optional<HistoriaClinicaEntrada> buscarPorId(int idEntrada);

    /** Entradas del paciente, de la más reciente a la más antigua. */
    List<HistoriaClinicaEntrada> listarPorPaciente(int idPaciente);
}
