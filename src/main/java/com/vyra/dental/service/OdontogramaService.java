package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.vyra.dental.dao.OdontogramaDAO;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.util.Validador;

/**
 * Odontograma del paciente (RF-05, CU-05, RN-09, RN-10). Cada cambio de estado
 * es un registro nuevo: el estado actual de una pieza es su último registro y
 * una pieza sin registros está sana. Solo el odontólogo registra cambios; el
 * administrador lo consulta.
 */
public class OdontogramaService {

    /** Arcada superior en el orden en que se dibuja: de la derecha del paciente a su izquierda. */
    public static final List<Integer> PIEZAS_SUPERIORES = List.of(18, 17, 16, 15, 14, 13, 12, 11,
            21, 22, 23, 24, 25, 26, 27, 28);
    /** Arcada inferior en el orden en que se dibuja. */
    public static final List<Integer> PIEZAS_INFERIORES = List.of(48, 47, 46, 45, 44, 43, 42, 41,
            31, 32, 33, 34, 35, 36, 37, 38);

    private static final String[] POSICIONES = { "Incisivo central", "Incisivo lateral", "Canino",
        "Primer premolar", "Segundo premolar", "Primer molar", "Segundo molar", "Tercer molar" };
    private static final String[] CUADRANTES = { "superior derecho", "superior izquierdo",
        "inferior izquierdo", "inferior derecho" };

    private final OdontogramaDAO odontogramaDAO;
    /** Fecha "actual" de los registros; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public OdontogramaService(OdontogramaDAO odontogramaDAO) {
        this(odontogramaDAO, Clock.systemDefaultZone());
    }

    public OdontogramaService(OdontogramaDAO odontogramaDAO, Clock reloj) {
        this.odontogramaDAO = odontogramaDAO;
        this.reloj = reloj;
    }

    /** Estado actual de las 32 piezas, ordenadas por número; las que no tienen registros están sanas. */
    public Map<Integer, EstadoPieza> estadoActual(int idPaciente) {
        Map<Integer, EstadoPieza> estados = new TreeMap<>();
        todasLasPiezas().forEach(pieza -> estados.put(pieza, EstadoPieza.SANA));
        for (OdontogramaPieza registro : odontogramaDAO.estadoActual(idPaciente)) {
            estados.put(registro.getNumeroPieza(), registro.getEstado());
        }
        return estados;
    }

    /**
     * Guarda los cambios pendientes del odontograma, todos con la fecha de hoy
     * y en una sola transacción. Nunca borra ni modifica registros anteriores
     * (RN-09). Se omiten los cambios que dejan la pieza en el estado que ya
     * tenía. Devuelve cuántos registros se guardaron.
     *
     * @param observaciones nota de cada pieza cambiada (puede no tener todas)
     */
    public int registrarCambios(int idPaciente, Map<Integer, EstadoPieza> cambios, Map<Integer, String> observaciones) {
        verificarPermiso();
        if (cambios.isEmpty()) {
            throw new ReglaNegocioException("No hay cambios para guardar.");
        }
        Map<Integer, EstadoPieza> actual = estadoActual(idPaciente);
        Integer idProfesional = Sesion.get().getProfesional().map(Profesional::getIdProfesional).orElse(null);
        List<OdontogramaPieza> registros = new ArrayList<>();
        for (Map.Entry<Integer, EstadoPieza> cambio : new TreeMap<>(cambios).entrySet()) {
            int pieza = cambio.getKey();
            if (!esPiezaValida(pieza)) {
                // RN-10: el @Min/@Max del modelo dejaría pasar 19, 20, 29...
                throw new ReglaNegocioException("La pieza " + pieza + " no existe en la notación FDI "
                        + "(11–18, 21–28, 31–38, 41–48).");
            }
            if (cambio.getValue() == actual.get(pieza)) {
                continue;
            }
            OdontogramaPieza registro = new OdontogramaPieza(0, idPaciente, pieza, cambio.getValue(),
                    LocalDate.now(reloj), limpiar(observaciones.get(pieza)), idProfesional);
            Validador.validar(registro);
            registros.add(registro);
        }
        if (!registros.isEmpty()) {
            odontogramaDAO.registrarTodos(registros);
        }
        return registros.size();
    }

    /** Cambios de la pieza, del más reciente al más antiguo. */
    public List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza) {
        return odontogramaDAO.historialPieza(idPaciente, numeroPieza);
    }

    public boolean puedeEditar() {
        return Sesion.get().tieneRol(Rol.ODONTOLOGO);
    }

    /** RN-10: cuadrantes 1 a 4 y posiciones 1 a 8 (piezas permanentes). */
    public static boolean esPiezaValida(int numero) {
        int cuadrante = numero / 10;
        int posicion = numero % 10;
        return cuadrante >= 1 && cuadrante <= 4 && posicion >= 1 && posicion <= 8;
    }

    /** "Primer molar inferior izquierdo" para la pieza 36. */
    public static String nombrePieza(int numero) {
        if (!esPiezaValida(numero)) {
            throw new IllegalArgumentException("Pieza dental inválida: " + numero);
        }
        return POSICIONES[numero % 10 - 1] + " " + CUADRANTES[numero / 10 - 1];
    }

    private static Stream<Integer> todasLasPiezas() {
        return Stream.concat(PIEZAS_SUPERIORES.stream(), PIEZAS_INFERIORES.stream());
    }

    private void verificarPermiso() {
        if (!puedeEditar()) {
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
