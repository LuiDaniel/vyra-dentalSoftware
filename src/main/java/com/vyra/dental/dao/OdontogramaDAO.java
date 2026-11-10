package com.vyra.dental.dao;

import java.util.List;

import com.vyra.dental.model.OdontogramaPieza;

/**
 * Contrato de acceso a datos para el odontograma. Cada cambio de estado es una
 * fila nueva (RN-09): no hay actualizar ni eliminar.
 */
public interface OdontogramaDAO {
    OdontogramaPieza registrar(OdontogramaPieza registro);

    /** Registra todos los cambios o ninguno (una sola transacción). */
    void registrarTodos(List<OdontogramaPieza> registros);

    /**
     * El registro más reciente de cada pieza que tiene alguno
     * (docs/03-modelo-base-datos.md §5.1). Las piezas que no aparecen están sanas.
     */
    List<OdontogramaPieza> estadoActual(int idPaciente);

    /** Cambios de la pieza, del más reciente al más antiguo. */
    List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza);
}
