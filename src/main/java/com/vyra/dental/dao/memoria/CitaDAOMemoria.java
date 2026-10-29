package com.vyra.dental.dao.memoria;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;

/**
 * Implementacion en memoria (ArrayList) de CitaDAO, para las pruebas de CitaService.
 * Guarda copias, como haria una base de datos: cambiar una cita fuera del DAO
 * no cambia la guardada.
 */
public class CitaDAOMemoria implements CitaDAO {

    private static final Comparator<Cita> POR_FECHA_Y_HORA =
            Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHoraInicio);

    private final List<Cita> citas = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Cita crear(Cita cita) {
        cita.setIdCita(siguienteId++);
        citas.add(copia(cita));
        return cita;
    }

    @Override
    public Optional<Cita> buscarPorId(int idCita) {
        return citas.stream().filter(c -> c.getIdCita() == idCita).findFirst().map(CitaDAOMemoria::copia);
    }

    @Override
    public List<Cita> listarPorFecha(LocalDate fecha) {
        return filtrar(c -> c.getFecha().equals(fecha), POR_FECHA_Y_HORA);
    }

    @Override
    public List<Cita> listarPorProfesionalYFecha(int idProfesional, LocalDate fecha) {
        return filtrar(c -> c.getProfesional().getIdProfesional() == idProfesional && c.getFecha().equals(fecha),
                POR_FECHA_Y_HORA);
    }

    @Override
    public List<Cita> listarEntre(LocalDate desde, LocalDate hasta) {
        return filtrar(c -> !c.getFecha().isBefore(desde) && !c.getFecha().isAfter(hasta), POR_FECHA_Y_HORA);
    }

    @Override
    public List<Cita> listarPorPaciente(int idPaciente) {
        return filtrar(c -> c.getPaciente().getIdPaciente() == idPaciente, POR_FECHA_Y_HORA.reversed());
    }

    @Override
    public Optional<Cita> buscarCruce(int idProfesional, LocalDate fecha, LocalTime inicio, LocalTime fin,
                                      int excluirId) {
        // Mismo criterio que CitaDAOSqlite: inicioA < finB y inicioB < finA
        return filtrar(c -> c.getProfesional().getIdProfesional() == idProfesional
                && c.getFecha().equals(fecha)
                && (c.getEstado() == EstadoCita.PENDIENTE || c.getEstado() == EstadoCita.CONFIRMADA)
                && c.getHoraInicio().isBefore(fin)
                && inicio.isBefore(c.getHoraFin())
                && c.getIdCita() != excluirId, POR_FECHA_Y_HORA).stream().findFirst();
    }

    @Override
    public Optional<LocalDate> ultimaVisita(int idPaciente) {
        return citas.stream()
                .filter(c -> c.getPaciente().getIdPaciente() == idPaciente && c.getEstado() == EstadoCita.ATENDIDA)
                .map(Cita::getFecha)
                .max(Comparator.naturalOrder());
    }

    @Override
    public int contarPorProfesional(int idProfesional) {
        return (int) citas.stream().filter(c -> c.getProfesional().getIdProfesional() == idProfesional).count();
    }

    @Override
    public boolean actualizar(Cita cita) {
        for (int i = 0; i < citas.size(); i++) {
            if (citas.get(i).getIdCita() == cita.getIdCita()) {
                citas.set(i, copia(cita));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean actualizarEstado(int idCita, EstadoCita estado) {
        Optional<Cita> guardada = citas.stream().filter(c -> c.getIdCita() == idCita).findFirst();
        guardada.ifPresent(c -> c.setEstado(estado));
        return guardada.isPresent();
    }

    private List<Cita> filtrar(Predicate<Cita> condicion, Comparator<Cita> orden) {
        return citas.stream().filter(condicion).sorted(orden).map(CitaDAOMemoria::copia).toList();
    }

    private static Cita copia(Cita cita) {
        return new Cita(cita.getIdCita(), cita.getPaciente(), cita.getProfesional(), cita.getFecha(),
                cita.getHoraInicio(), cita.getHoraFin(), cita.getMotivo(), cita.getEstado());
    }
}
