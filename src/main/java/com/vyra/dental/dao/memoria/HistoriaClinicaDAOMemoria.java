package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.model.HistoriaClinicaEntrada;

/**
 * Implementacion en memoria (ArrayList) de HistoriaClinicaDAO, para las pruebas de
 * HistoriaClinicaService. Guarda copias, como haria una base de datos.
 */
public class HistoriaClinicaDAOMemoria implements HistoriaClinicaDAO {

    private final List<HistoriaClinicaEntrada> entradas = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public HistoriaClinicaEntrada crear(HistoriaClinicaEntrada entrada) {
        entrada.setIdEntrada(siguienteId++);
        entradas.add(copia(entrada));
        return entrada;
    }

    @Override
    public Optional<HistoriaClinicaEntrada> buscarPorId(int idEntrada) {
        return entradas.stream().filter(e -> e.getIdEntrada() == idEntrada).findFirst()
                .map(HistoriaClinicaDAOMemoria::copia);
    }

    @Override
    public List<HistoriaClinicaEntrada> listarPorPaciente(int idPaciente) {
        // Misma fecha: gana la registrada despues (id mayor), como en SQLite
        return entradas.stream()
                .filter(e -> e.getIdPaciente() == idPaciente)
                .sorted(Comparator.comparing(HistoriaClinicaEntrada::getFecha)
                        .thenComparing(HistoriaClinicaEntrada::getIdEntrada).reversed())
                .map(HistoriaClinicaDAOMemoria::copia)
                .toList();
    }

    private static HistoriaClinicaEntrada copia(HistoriaClinicaEntrada e) {
        return new HistoriaClinicaEntrada(e.getIdEntrada(), e.getIdPaciente(), e.getIdCita(), e.getFecha(),
                e.getObservaciones(), e.getDiagnostico());
    }
}
