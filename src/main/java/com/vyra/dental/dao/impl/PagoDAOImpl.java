package com.vyra.dental.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.model.Pago;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de PagoDAO.
 * Solo para pruebas: los datos se pierden al cerrar la aplicacion.
 */
public class PagoDAOImpl implements PagoDAO {

    private final List<Pago> pagos = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Pago crear(Pago pago) {
        Validador.validar(pago);
        pago.setIdPago(siguienteId++);
        pagos.add(pago);
        return pago;
    }

    @Override
    public Optional<Pago> buscarPorId(int idPago) {
        return pagos.stream().filter(p -> p.getIdPago() == idPago).findFirst();
    }

    @Override
    public List<Pago> listarTodos() {
        return new ArrayList<>(pagos);
    }

    @Override
    public boolean actualizar(Pago pago) {
        Validador.validar(pago);
        for (int i = 0; i < pagos.size(); i++) {
            if (pagos.get(i).getIdPago() == pago.getIdPago()) {
                pagos.set(i, pago);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eliminar(int idPago) {
        return pagos.removeIf(p -> p.getIdPago() == idPago);
    }
}
