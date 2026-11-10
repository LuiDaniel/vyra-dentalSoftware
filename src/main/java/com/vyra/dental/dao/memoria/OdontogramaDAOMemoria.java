package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.vyra.dental.dao.OdontogramaDAO;
import com.vyra.dental.model.OdontogramaPieza;

/**
 * Implementacion en memoria (ArrayList) de OdontogramaDAO, para las pruebas
 * de OdontogramaService. Guarda copias, como haria una base de datos.
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
    public void registrarTodos(List<OdontogramaPieza> nuevos) {
        nuevos.forEach(this::registrar);
    }

    @Override
    public List<OdontogramaPieza> estadoActual(int idPaciente) {
        // Mismo criterio que OdontogramaDAOSqlite: el registro con el id más alto de cada pieza
        BinaryOperator<OdontogramaPieza> masReciente = (a, b) -> a.getIdRegistro() > b.getIdRegistro() ? a : b;
        Map<Integer, OdontogramaPieza> porPieza = registros.stream()
                .filter(r -> r.getIdPaciente() == idPaciente)
                .collect(Collectors.toMap(OdontogramaPieza::getNumeroPieza, r -> r, masReciente));
        return porPieza.values().stream()
                .sorted(Comparator.comparingInt(OdontogramaPieza::getNumeroPieza))
                .map(OdontogramaDAOMemoria::copia)
                .toList();
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
                r.getFechaRegistro(), r.getObservacion(), r.getIdProfesional());
    }
}
