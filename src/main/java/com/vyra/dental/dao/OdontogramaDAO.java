package com.vyra.dental.dao;

import java.util.List;

import com.vyra.dental.model.OdontogramaPieza;

/**
 * Registros del odontograma. No hay actualizar ni eliminar: cada cambio de
 * estado es una fila nueva y nunca se sobrescribe (RN-09).
 */
public interface OdontogramaDAO {

    OdontogramaPieza registrar(OdontogramaPieza registro);

    /** El último registro de cada pieza que tiene alguno. Las demás piezas son SANA. */
    List<OdontogramaPieza> estadoActual(int idPaciente);

    /** Todos los registros de la pieza, del más reciente al más antiguo. */
    List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza);
}
