package com.vyra.dental.dao.memoria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de PagoDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 * Guarda copias, como haria una base de datos. No arma resumenes (no conoce
 * los tratamientos ni los pacientes): resumenesEntre devuelve una lista vacía.
 */
public class PagoDAOMemoria implements PagoDAO {

    private static final Comparator<Pago> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(Pago::getFechaPago).thenComparingInt(Pago::getIdPago).reversed();

    private final List<Pago> pagos = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Pago crear(Pago pago) {
        Validador.validar(pago);
        pago.setIdPago(siguienteId++);
        pagos.add(copia(pago));
        return pago;
    }

    @Override
    public Optional<Pago> buscarPorId(int idPago) {
        return pagos.stream().filter(p -> p.getIdPago() == idPago).findFirst().map(PagoDAOMemoria::copia);
    }

    @Override
    public List<Pago> listarTodos() {
        return filtrar(p -> true);
    }

    @Override
    public List<Pago> listarPorTratamiento(int idTratamiento) {
        return filtrar(p -> p.getIdTratamiento() == idTratamiento);
    }

    @Override
    public List<Pago> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        return filtrar(p -> !p.getFechaPago().isBefore(desde) && !p.getFechaPago().isAfter(hasta));
    }

    @Override
    public double totalPagado(int idTratamiento) {
        return pagos.stream().filter(p -> p.getIdTratamiento() == idTratamiento).mapToDouble(Pago::getMonto).sum();
    }

    @Override
    public List<PagoResumen> resumenesEntre(LocalDate desde, LocalDate hasta) {
        return List.of();
    }

    @Override
    public Map<MetodoPago, Double> totalesPorMetodo(LocalDate desde, LocalDate hasta) {
        Map<MetodoPago, Double> totales = new EnumMap<>(MetodoPago.class);
        for (Pago pago : listarPorPeriodo(desde, hasta)) {
            totales.merge(pago.getMetodoPago(), pago.getMonto(), Double::sum);
        }
        return totales;
    }

    @Override
    public boolean actualizar(Pago pago) {
        Validador.validar(pago);
        for (int i = 0; i < pagos.size(); i++) {
            if (pagos.get(i).getIdPago() == pago.getIdPago()) {
                pagos.set(i, copia(pago));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eliminar(int idPago) {
        return pagos.removeIf(p -> p.getIdPago() == idPago);
    }

    private List<Pago> filtrar(Predicate<Pago> condicion) {
        return pagos.stream().filter(condicion).sorted(MAS_RECIENTE_PRIMERO).map(PagoDAOMemoria::copia).toList();
    }

    private static Pago copia(Pago p) {
        return new Pago(p.getIdPago(), p.getIdTratamiento(), p.getMonto(), p.getFechaPago(), p.getMetodoPago(),
                p.getNumeroOperacion());
    }
}
