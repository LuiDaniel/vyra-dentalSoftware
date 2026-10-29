package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vyra.dental.dao.OdontogramaDAO;
import com.vyra.dental.model.OdontogramaPieza;

/**
 * Implementacion en memoria (ArrayList) de OdontogramaDAO, para las pruebas de
 * OdontogramaService. Guarda copias, como haria una base de datos.
 */
public class OdontogramaDAOMemoria implements OdontogramaDAO {

    private final List<OdontogramaPieza> registros = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public OdontogramaPieza registrar(OdontogramaPieza registro) {
        registro.setIdRegistro(siguienteId++);
        registros.add(copia(registro));
        return registro;
    }

    @Override
    public List<OdontogramaPieza> estadoActual(int idPaciente) {
        // El ultimo registro (id mayor) de cada pieza, ordenado por numero de pieza
        Map<Integer, OdontogramaPieza> ultimos = new TreeMap<>();
        for (OdontogramaPieza r : registros) {
            if (r.getIdPaciente() == idPaciente) {
                ultimos.merge(r.getNumeroPieza(), r, (a, b) -> a.getIdRegistro() > b.getIdRegistro() ? a : b);
            }
        }
        return ultimos.values().stream().map(OdontogramaDAOMemoria::copia).toList();
    }

    @Override
    public List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza) {
        return registros.stream()
                .filter(r -> r.getIdPaciente() == idPaciente && r.getNumeroPieza() == numeroPieza)
                .sorted(Comparator.comparingInt(OdontogramaPieza::getIdRegistro).reversed())
                .map(OdontogramaDAOMemoria::copia)
                .toList();
    }

    private static OdontogramaPieza copia(OdontogramaPieza r) {
        return new OdontogramaPieza(r.getIdRegistro(), r.getIdPaciente(), r.getNumeroPieza(), r.getEstado(),
                r.getFechaRegistro(), r.getObservacion());
    }
}
