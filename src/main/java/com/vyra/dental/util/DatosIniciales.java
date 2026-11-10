package com.vyra.dental.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.dao.OdontogramaDAO;
import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.Usuario;

/**
 * Datos que la aplicación necesita para arrancar (docs/03-modelo-base-datos.md §4).
 */
public final class DatosIniciales {

    public static final String ADMIN_USUARIO = "admin";
    public static final String ADMIN_CONTRASENA_INICIAL = "admin123";

    /** Propiedad que activa los datos de ejemplo: {@code mvn javafx:run -Dvyra.demo=true}. */
    public static final String PROPIEDAD_DEMO = "vyra.demo";

    /** Contraseña de los usuarios de ejemplo (solo con la demo activada). */
    public static final String CONTRASENA_EJEMPLO = "demo1234";

    private DatosIniciales() {
    }

    /**
     * Crea el usuario administrador solo si la tabla usuario está vacía,
     * para que la primera vez se pueda iniciar sesión. Su contraseña
     * debe cambiarse después del primer ingreso.
     */
    public static void crearAdminSiNoExiste(UsuarioDAO usuarioDAO) {
        if (usuarioDAO.contar() > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombreUsuario(ADMIN_USUARIO);
        admin.setNombreCompleto("Administrador");
        admin.setContrasenaHash(HashUtil.hashear(ADMIN_CONTRASENA_INICIAL));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        usuarioDAO.crear(admin);
    }

    public static boolean demoActivada() {
        return Boolean.getBoolean(PROPIEDAD_DEMO);
    }

    /**
     * Pacientes ficticios del diseño (docs/ui/Pacientes.html), solo para
     * probar las pantallas en desarrollo. Se insertan una sola vez: si el DNI
     * ya existe, ese paciente se omite.
     */
    public static void cargarEjemplos(PacienteDAO pacienteDAO) {
        Paciente[] ejemplos = {
            paciente("María Fernanda", "Quispe Huamán", "72481935", LocalDate.of(1992, 3, 14), "987654321",
                    "Penicilina", LocalDate.of(2024, 3, 5)),
            paciente("Jorge", "Mamani Condori", "45812307", LocalDate.of(1974, 5, 20), "951203448",
                    null, LocalDate.of(2023, 8, 11)),
            paciente("Lucía", "Torres Rojas", "70356219", LocalDate.of(1999, 7, 8), "964118502",
                    "Látex", LocalDate.of(2025, 1, 17)),
            paciente("Carlos", "Pérez Salas", "43920185", LocalDate.of(1985, 2, 11), "912776340",
                    null, LocalDate.of(2024, 9, 2)),
            paciente("Ana", "Chávez Flores", "76104352", LocalDate.of(2007, 1, 23), "945330917",
                    null, LocalDate.of(2025, 6, 30)),
            paciente("Rosa", "Apaza Choque", "29874561", LocalDate.of(1963, 6, 30), "973482106",
                    "Ibuprofeno", LocalDate.of(2022, 11, 4)),
            paciente("Diego", "Vargas Lima", "71592846", LocalDate.of(2018, 4, 5), "988210774",
                    null, LocalDate.of(2026, 2, 9)),
        };
        for (Paciente ejemplo : ejemplos) {
            if (pacienteDAO.buscarPorDni(ejemplo.getDni()).isEmpty()) {
                pacienteDAO.crear(ejemplo);
            }
        }
    }

    /**
     * Profesionales y citas ficticios del diseño (docs/ui/Citas.html). Las citas
     * se crean para hoy y mañana, y solo la primera vez (si ningún profesional
     * de ejemplo existía).
     */
    public static void cargarEjemplosAgenda(PacienteDAO pacienteDAO, ProfesionalDAO profesionalDAO, CitaDAO citaDAO) {
        boolean yaExistian = profesionalDAO.listarTodos().stream()
                .anyMatch(p -> p.getApellidos().equals("Núñez Paredes"));
        if (yaExistian) {
            return;
        }
        Profesional nunez = profesionalDAO.crear(new Profesional(0, null, "Patricia", "Núñez Paredes", "Ortodoncia",
                "951444210", "Lun-Sáb 08:00-14:00"));
        Profesional salinas = profesionalDAO.crear(new Profesional(0, null, "Marco", "Salinas Vega", "Endodoncia",
                "987111203", "Lun-Vie 08:00-16:00"));
        Profesional rios = profesionalDAO.crear(new Profesional(0, null, "Claudia", "Ríos Mendoza", "Odontopediatría",
                "912600318", "Lun-Vie 09:00-18:00"));

        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);
        Object[][] citas = {
            { nunez, "29874561", hoy, "08:00", "08:30", "Control de prótesis", EstadoCita.ATENDIDA },
            { nunez, "45812307", hoy, "09:30", "10:00", "Limpieza dental", EstadoCita.CONFIRMADA },
            { nunez, "70356219", hoy, "10:00", "10:30", "Ajuste de brackets", EstadoCita.PENDIENTE },
            { nunez, "76104352", hoy, "11:30", "12:30", "Control de ortodoncia", EstadoCita.CONFIRMADA },
            { salinas, "72481935", hoy, "09:00", "10:00", "Endodoncia pieza 36", EstadoCita.CONFIRMADA },
            { salinas, "43920185", hoy, "11:00", "11:30", "Evaluación general", EstadoCita.CANCELADA },
            { salinas, "29874561", hoy, "12:00", "13:00", "Corona provisional", EstadoCita.PENDIENTE },
            { rios, "71592846", hoy, "08:30", "09:30", "Primera visita", EstadoCita.ATENDIDA },
            { rios, "71592846", hoy, "10:30", "11:00", "Sellantes", EstadoCita.PENDIENTE },
            { salinas, "45812307", manana, "10:00", "10:30", "Control", EstadoCita.PENDIENTE },
            { rios, "76104352", manana, "15:00", "15:30", "Evaluación", EstadoCita.CONFIRMADA },
        };
        for (Object[] datos : citas) {
            pacienteDAO.buscarPorDni((String) datos[1]).ifPresent(paciente -> citaDAO.crear(new Cita(0, paciente,
                    (Profesional) datos[0], (LocalDate) datos[2], LocalTime.parse((String) datos[3]),
                    LocalTime.parse((String) datos[4]), (String) datos[5], (EstadoCita) datos[6])));
        }
    }

    /**
     * Usuarios ficticios para probar los otros roles mientras no exista la
     * pantalla Usuarios (fase 7): "msalinas" (Odontólogo, asociado al
     * profesional Marco Salinas Vega) y "recepcion" (Recepción). Ambos con la
     * contraseña {@link #CONTRASENA_EJEMPLO}. Cada uno se crea solo si no existe.
     */
    public static void cargarUsuariosDeEjemplo(UsuarioDAO usuarioDAO, ProfesionalDAO profesionalDAO) {
        if (usuarioDAO.buscarPorNombreUsuario("msalinas").isEmpty()) {
            Usuario odontologo = usuarioDAO.crear(new Usuario(0, "msalinas", "Marco Salinas Vega",
                    HashUtil.hashear(CONTRASENA_EJEMPLO), Rol.ODONTOLOGO, true));
            profesionalDAO.listarTodos().stream()
                    .filter(p -> p.getApellidos().equals("Salinas Vega") && p.getIdUsuario() == null)
                    .findFirst()
                    .ifPresent(salinas -> {
                        salinas.setIdUsuario(odontologo.getIdUsuario());
                        profesionalDAO.actualizar(salinas);
                    });
        }
        if (usuarioDAO.buscarPorNombreUsuario("recepcion").isEmpty()) {
            usuarioDAO.crear(new Usuario(0, "recepcion", "Rocío Mendoza Paz",
                    HashUtil.hashear(CONTRASENA_EJEMPLO), Rol.RECEPCION, true));
        }
    }

    /**
     * Odontograma e historia clínica ficticios de María Quispe, como en
     * docs/ui/Odontograma.html. Solo si su odontograma todavía está vacío.
     */
    public static void cargarEjemplosClinicos(PacienteDAO pacienteDAO, ProfesionalDAO profesionalDAO,
                                              OdontogramaDAO odontogramaDAO, HistoriaClinicaDAO historiaDAO) {
        Paciente maria = pacienteDAO.buscarPorDni("72481935").orElse(null);
        if (maria == null || !odontogramaDAO.estadoActual(maria.getIdPaciente()).isEmpty()) {
            return;
        }
        Integer salinas = profesionalDAO.listarTodos().stream()
                .filter(p -> p.getApellidos().equals("Salinas Vega"))
                .map(Profesional::getIdProfesional)
                .findFirst()
                .orElse(null);
        int id = maria.getIdPaciente();
        LocalDate hoy = LocalDate.now();
        LocalDate primeraVisita = hoy.minusMonths(8);
        LocalDate control = hoy.minusMonths(2);
        LocalDate inicioEndodoncia = hoy.minusDays(25);

        odontogramaDAO.registrarTodos(List.of(
                new OdontogramaPieza(0, id, 18, EstadoPieza.EXTRAIDA, primeraVisita, "Extraída antes de la primera visita", salinas),
                new OdontogramaPieza(0, id, 38, EstadoPieza.EXTRAIDA, primeraVisita, null, salinas),
                new OdontogramaPieza(0, id, 16, EstadoPieza.OBTURADA, primeraVisita, "Resina oclusal", salinas),
                new OdontogramaPieza(0, id, 26, EstadoPieza.OBTURADA, primeraVisita, null, salinas),
                new OdontogramaPieza(0, id, 45, EstadoPieza.OBTURADA, primeraVisita, null, salinas),
                new OdontogramaPieza(0, id, 24, EstadoPieza.CARIADA, control, "Caries interproximal", salinas),
                new OdontogramaPieza(0, id, 47, EstadoPieza.CARIADA, control, null, salinas),
                new OdontogramaPieza(0, id, 36, EstadoPieza.CARIADA, control, "Caries profunda", salinas),
                new OdontogramaPieza(0, id, 36, EstadoPieza.EN_TRATAMIENTO, inicioEndodoncia, "Inicio de endodoncia", salinas)));

        historiaDAO.crear(new HistoriaClinicaEntrada(0, id, null, salinas, primeraVisita,
                "Paciente refiere sensibilidad al frío. Se realizan obturaciones en 16, 26 y 45.", "Caries múltiples"));
        historiaDAO.crear(new HistoriaClinicaEntrada(0, id, null, salinas, control,
                "Dolor espontáneo en 36. Radiografía periapical: caries profunda cercana a la pulpa.",
                "Caries profunda en pieza 36"));
        historiaDAO.crear(new HistoriaClinicaEntrada(0, id, null, salinas, inicioEndodoncia,
                "Se inicia endodoncia de 36: apertura cameral y medicación intraconducto. Control en 3 semanas.",
                "Pulpitis irreversible en pieza 36"));
    }

    /**
     * Tratamientos y pagos ficticios, como en docs/ui/Pagos.html. Cada uno va
     * en una cita atendida que se crea para eso. Solo si no hay tratamientos.
     */
    public static void cargarEjemplosPagos(PacienteDAO pacienteDAO, ProfesionalDAO profesionalDAO, CitaDAO citaDAO,
                                           TratamientoDAO tratamientoDAO, PagoDAO pagoDAO) {
        if (!tratamientoDAO.listarTodos().isEmpty()) {
            return;
        }
        List<Profesional> profesionales = profesionalDAO.listarTodos();
        LocalDate hoy = LocalDate.now();
        // { DNI, apellidos del profesional, días atrás, motivo, descripción, costo, estado, piezas, pagos }
        Object[][] ejemplos = {
            { "72481935", "Salinas Vega", 25, "Inicio de endodoncia", "Endodoncia pieza 36", 650.0,
                EstadoTratamiento.EN_CURSO, List.of(36), new Object[][] { { 25, 250.0, MetodoPago.YAPE_PLIN } } },
            { "70356219", "Núñez Paredes", 60, "Instalación de brackets", "Ortodoncia", 4500.0,
                EstadoTratamiento.EN_CURSO, List.of(), new Object[][] {
                    { 60, 250.0, MetodoPago.TARJETA }, { 30, 250.0, MetodoPago.TARJETA }, { 1, 250.0, MetodoPago.TARJETA } } },
            { "76104352", "Núñez Paredes", 40, "Control de ortodoncia", "Ortodoncia", 3600.0,
                EstadoTratamiento.EN_CURSO, List.of(), new Object[][] { { 2, 250.0, MetodoPago.TRANSFERENCIA } } },
            { "45812307", "Núñez Paredes", 3, "Limpieza dental", "Limpieza dental", 120.0,
                EstadoTratamiento.FINALIZADO, List.of(), new Object[][] { { 3, 120.0, MetodoPago.YAPE_PLIN } } },
            { "43920185", "Salinas Vega", 4, "Resina pieza 14", "Resina pieza 14", 180.0,
                EstadoTratamiento.FINALIZADO, List.of(14), new Object[][] { { 4, 180.0, MetodoPago.EFECTIVO } } },
            { "29874561", "Núñez Paredes", 0, "Control de prótesis", "Control de prótesis", 80.0,
                EstadoTratamiento.FINALIZADO, List.of(), new Object[][] { { 0, 80.0, MetodoPago.EFECTIVO } } },
            { "29874561", "Salinas Vega", 15, "Evaluación de corona", "Corona provisional pieza 26", 380.0,
                EstadoTratamiento.EN_CURSO, List.of(26), new Object[][] {} },
        };
        for (Object[] datos : ejemplos) {
            Paciente paciente = pacienteDAO.buscarPorDni((String) datos[0]).orElse(null);
            Profesional profesional = profesionales.stream()
                    .filter(p -> p.getApellidos().equals(datos[1])).findFirst().orElse(null);
            if (paciente == null || profesional == null) {
                continue;
            }
            LocalDate fecha = hoy.minusDays((Integer) datos[2]);
            // Una hora fuera del horario de la demo de la agenda, para no cruzarse con sus citas de hoy
            Cita cita = citaDAO.crear(new Cita(0, paciente, profesional, fecha, LocalTime.of(18, 0),
                    LocalTime.of(18, 30), (String) datos[3], EstadoCita.ATENDIDA));
            @SuppressWarnings("unchecked")
            List<Integer> piezas = (List<Integer>) datos[7];
            Tratamiento tratamiento = tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), (String) datos[4],
                    (Double) datos[5], (EstadoTratamiento) datos[6], fecha, piezas));
            for (Object[] pago : (Object[][]) datos[8]) {
                pagoDAO.crear(new Pago(0, tratamiento.getIdTratamiento(), (Double) pago[1],
                        hoy.minusDays((Integer) pago[0]), (MetodoPago) pago[2], null));
            }
        }
    }

    private static Paciente paciente(String nombres, String apellidos, String dni, LocalDate nacimiento,
                                     String telefono, String alergias, LocalDate registro) {
        return new Paciente(0, nombres, apellidos, dni, nacimiento, telefono, "Av. Ejemplo 123, Tacna",
                null, alergias, null, true, registro);
    }
}
