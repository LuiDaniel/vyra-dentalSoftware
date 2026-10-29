package com.vyra.dental.dao.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.util.Validador;

/**
 * Implementacion en memoria (ArrayList) de PacienteDAO.
 * Para las pruebas de los servicios y los demos: los datos se pierden al cerrar la aplicacion.
 */
public class PacienteDAOMemoria implements PacienteDAO {

    private final List<Paciente> pacientes = new ArrayList<>();
    private int siguienteId = 1;

    @Override
    public Paciente crear(Paciente paciente) {
        Validador.validar(paciente);
        paciente.setIdPaciente(siguienteId++);
        pacientes.add(paciente);
        return paciente;
    }

    @Override
    public Optional<Paciente> buscarPorId(int idPaciente) {
        return pacientes.stream().filter(p -> p.getIdPaciente() == idPaciente).findFirst();
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        return pacientes.stream().filter(p -> p.getDni() != null && p.getDni().equals(dni)).findFirst();
    }

    @Override
    public List<Paciente> buscar(String texto, FiltroPaciente filtro, int limite, int desde) {
        return pacientes.stream()
                .filter(coincide(texto, filtro))
                .sorted(Comparator.comparing(Paciente::getApellidos).thenComparing(Paciente::getNombres))
                .skip(desde)
                .limit(limite)
                .toList();
    }

    @Override
    public int contarActivos(String texto, FiltroPaciente filtro) {
        return (int) pacientes.stream().filter(coincide(texto, filtro)).count();
    }

    @Override
    public boolean actualizar(Paciente paciente) {
        Validador.validar(paciente);
        for (int i = 0; i < pacientes.size(); i++) {
            if (pacientes.get(i).getIdPaciente() == paciente.getIdPaciente()) {
                pacientes.set(i, paciente);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean darDeBaja(int idPaciente) {
        Optional<Paciente> paciente = buscarPorId(idPaciente).filter(Paciente::isActivo);
        paciente.ifPresent(p -> p.setActivo(false));
        return paciente.isPresent();
    }

    /** Mismo criterio que PacienteDAOSqlite.buscar(). */
    private Predicate<Paciente> coincide(String texto, FiltroPaciente filtro) {
        String buscado = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        return p -> p.isActivo()
                && (p.getDni().contains(buscado)
                    || p.getNombreCompleto().toLowerCase(Locale.ROOT).contains(buscado))
                && switch (filtro) {
                    case TODOS -> true;
                    case CON_ALERGIAS -> p.tieneAlergias();
                    case MENORES_DE_EDAD -> p.getFechaNacimiento() != null && p.getEdad() < 18;
                };
    }
}
