package com.vyra.dental.dao.memoria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de TratamientoDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 * Guarda copias, como haria una base de datos. Para los resumenes (paciente y
 * lo pagado) necesita los DAO de citas y pagos; sin ellos, no devuelve resumenes.
 */
public class TratamientoDAOMemoria implements TratamientoDAO {

    private static final Comparator<Tratamiento> MAS_RECIENTE_PRIMERO =
            // El demo puede crear tratamientos sin fecha
            Comparator.comparing(Tratamiento::getFecha, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
                    .thenComparingInt(Tratamiento::getIdTratamiento).reversed();

    private final List<Tratamiento> tratamientos = new ArrayList<>();
    private final CitaDAO citaDAO;
    private final PagoDAO pagoDAO;
    private int siguienteId = 1;

    public TratamientoDAOMemoria() {
        this(null, null);
    }

    public TratamientoDAOMemoria(CitaDAO citaDAO, PagoDAO pagoDAO) {
        this.citaDAO = citaDAO;
        this.pagoDAO = pagoDAO;
    }

    @Override
    public Tratamiento crear(Tratamiento tratamiento) {
        Validador.validar(tratamiento);
        tratamiento.setIdTratamiento(siguienteId++);
        tratamientos.add(copia(tratamiento));
        return tratamiento;
    }

    @Override
    public Optional<Tratamiento> buscarPorId(int idTratamiento) {
        return tratamientos.stream().filter(t -> t.getIdTratamiento() == idTratamiento).findFirst()
                .map(TratamientoDAOMemoria::copia);
    }

    @Override
    public List<Tratamiento> listarTodos() {
        return filtrar(t -> true);
    }

    @Override
    public List<Tratamiento> listarPorPaciente(int idPaciente) {
        return filtrar(t -> cita(t).map(c -> c.getPaciente().getIdPaciente() == idPaciente).orElse(false));
    }

    @Override
    public List<Tratamiento> listarPorEstado(EstadoTratamiento estado) {
        return filtrar(t -> t.getEstado() == estado);
    }

    @Override
    public boolean actualizar(Tratamiento tratamiento) {
        Validador.validar(tratamiento);
        for (int i = 0; i < tratamientos.size(); i++) {
            if (tratamientos.get(i).getIdTratamiento() == tratamiento.getIdTratamiento()) {
                tratamientos.set(i, copia(tratamiento));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean actualizarEstado(int idTratamiento, EstadoTratamiento estado) {
        Optional<Tratamiento> guardado = tratamientos.stream()
                .filter(t -> t.getIdTratamiento() == idTratamiento).findFirst();
        guardado.ifPresent(t -> t.setEstado(estado));
        return guardado.isPresent();
    }

    @Override
    public boolean eliminar(int idTratamiento) {
        return tratamientos.removeIf(t -> t.getIdTratamiento() == idTratamiento);
    }

    @Override
    public List<TratamientoResumen> buscarResumenes(String textoPaciente, EstadoTratamiento estado) {
        String texto = Busqueda.normalizar(textoPaciente);
        return resumenes(t -> estado == null || t.getEstado() == estado).stream()
                .filter(r -> texto.isEmpty() || r.getPaciente().getDni().contains(texto)
                        || Busqueda.normalizar(r.getPaciente().getNombreCompleto()).contains(texto))
                .toList();
    }

    @Override
    public Optional<TratamientoResumen> buscarResumen(int idTratamiento) {
        return resumenes(t -> t.getIdTratamiento() == idTratamiento).stream().findFirst();
    }

    @Override
    public List<TratamientoResumen> resumenesDePaciente(int idPaciente) {
        return resumenes(t -> true).stream()
                .filter(r -> r.getPaciente().getIdPaciente() == idPaciente)
                .toList();
    }

    /** Igual que el JOIN de TratamientoDAOSqlite: sin cita no hay resumen. */
    private List<TratamientoResumen> resumenes(Predicate<Tratamiento> condicion) {
        if (citaDAO == null) {
            return List.of();
        }
        List<TratamientoResumen> resumenes = new ArrayList<>();
        for (Tratamiento tratamiento : filtrar(condicion)) {
            cita(tratamiento).ifPresent(c -> resumenes.add(new TratamientoResumen(tratamiento, c.getPaciente(),
                    c.getProfesional(), pagoDAO == null ? 0 : pagoDAO.totalPagado(tratamiento.getIdTratamiento()))));
        }
        return resumenes;
    }

    private Optional<Cita> cita(Tratamiento tratamiento) {
        return citaDAO == null ? Optional.empty() : citaDAO.buscarPorId(tratamiento.getIdCita());
    }

    private List<Tratamiento> filtrar(Predicate<Tratamiento> condicion) {
        return tratamientos.stream().filter(condicion).sorted(MAS_RECIENTE_PRIMERO)
                .map(TratamientoDAOMemoria::copia).toList();
    }

    private static Tratamiento copia(Tratamiento t) {
        return new Tratamiento(t.getIdTratamiento(), t.getIdCita(), t.getDescripcion(), t.getCosto(), t.getEstado(),
                t.getFecha(), t.getPiezasDentales() == null ? null : new ArrayList<>(t.getPiezasDentales()));
    }
}
