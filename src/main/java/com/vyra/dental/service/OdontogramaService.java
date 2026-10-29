package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vyra.dental.dao.OdontogramaDAO;
import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.util.Validador;

/**
 * Odontograma del paciente (RF-05, CU-05, RN-09, RN-10). El odontólogo
 * registra cambios; el administrador solo consulta; recepción no tiene acceso.
 * Los cambios no se sobrescriben: cada uno es un registro nuevo.
 */
public class OdontogramaService {

    private final OdontogramaDAO odontogramaDAO;
    private final PacienteDAO pacienteDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public OdontogramaService(OdontogramaDAO odontogramaDAO, PacienteDAO pacienteDAO) {
        this(odontogramaDAO, pacienteDAO, Clock.systemDefaultZone());
    }

    public OdontogramaService(OdontogramaDAO odontogramaDAO, PacienteDAO pacienteDAO, Clock reloj) {
        this.odontogramaDAO = odontogramaDAO;
        this.pacienteDAO = pacienteDAO;
        this.reloj = reloj;
    }

    /** Solo las piezas con algún registro; las que no aparecen son SANA. */
    public Map<Integer, EstadoPieza> estadoActual(int idPaciente) {
        verificarLectura();
        Map<Integer, EstadoPieza> estados = new TreeMap<>();
        for (OdontogramaPieza registro : odontogramaDAO.estadoActual(idPaciente)) {
            estados.put(registro.getNumeroPieza(), registro.getEstado());
        }
        return estados;
    }

    /**
     * Guarda los cambios pendientes del odontograma. Solo se registran las piezas
     * cuyo estado difiere del actual; devuelve cuántas cambiaron.
     */
    public int registrarCambios(int idPaciente, Map<Integer, EstadoPieza> cambios, String observacion) {
        verificarEscritura();
        pacienteDAO.buscarPorId(idPaciente).filter(Paciente::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El paciente no existe o fue dado de baja."));
        String nota = limpiar(observacion);
        LocalDate hoy = LocalDate.now(reloj);
        Map<Integer, EstadoPieza> actual = estadoActual(idPaciente);

        // Se validan todas las piezas antes de guardar alguna
        List<OdontogramaPieza> nuevos = new ArrayList<>();
        for (Map.Entry<Integer, EstadoPieza> cambio : new TreeMap<>(cambios).entrySet()) {
            OdontogramaPieza registro = new OdontogramaPieza(0, idPaciente, cambio.getKey(), cambio.getValue(), hoy,
                    nota);
            Validador.validar(registro);
            if (!piezaExiste(registro.getNumeroPieza())) {
                throw new ReglaNegocioException("La pieza " + registro.getNumeroPieza() + " no existe.");
            }
            if (actual.getOrDefault(registro.getNumeroPieza(), EstadoPieza.SANA) != registro.getEstado()) {
                nuevos.add(registro);
            }
        }
        nuevos.forEach(odontogramaDAO::registrar);
        return nuevos.size();
    }

    /** De la más reciente a la más antigua. */
    public List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza) {
        verificarLectura();
        return odontogramaDAO.historialPieza(idPaciente, numeroPieza);
    }

    public boolean puedeVer() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.ODONTOLOGO);
    }

    public boolean puedeEscribir() {
        return Sesion.get().tieneRol(Rol.ODONTOLOGO);
    }

    /** Notación FDI (RN-10): cuadrante 1 a 4, diente 1 a 8 (descarta 19, 20, 29...). */
    private static boolean piezaExiste(int numero) {
        return numero / 10 >= 1 && numero / 10 <= 4 && numero % 10 >= 1 && numero % 10 <= 8;
    }

    private void verificarLectura() {
        if (!puedeVer()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private void verificarEscritura() {
        if (!puedeEscribir()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
