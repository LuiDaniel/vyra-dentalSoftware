package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.model.HistoriaClinicaEntrada;

/**
 * Implementacion en memoria (ArrayList) de HistoriaClinicaDAO, para las pruebas
 * de HistoriaClinicaService. Guarda copias, como haria una base de datos.
 */
public class HistoriaClinicaDAOMemoria implements HistoriaClinicaDAO {

    private static final Comparator<HistoriaClinicaEntrada> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(HistoriaClinicaEntrada::getFecha)
                    .thenComparingInt(HistoriaClinicaEntrada::getIdEntrada)
                    .reversed();

    private final List<HistoriaClinicaEntrada> entradas = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public HistoriaClinicaEntrada crear(HistoriaClinicaEntrada entrada) {
        entrada.setIdEntrada(siguienteId++);
        entradas.add(copia(entrada));
        return entrada;
    }

    @Override
    public List<HistoriaClinicaEntrada> listarPorPaciente(int idPaciente) {
        return entradas.stream()
                .filter(e -> e.getIdPaciente() == idPaciente)
                .sorted(MAS_RECIENTE_PRIMERO)
                .map(HistoriaClinicaDAOMemoria::copia)
                .toList();
    }

    private static HistoriaClinicaEntrada copia(HistoriaClinicaEntrada e) {
        return new HistoriaClinicaEntrada(e.getIdEntrada(), e.getIdPaciente(), e.getIdCita(), e.getIdProfesional(),
                e.getFecha(), e.getObservaciones(), e.getDiagnostico());
    }
}
