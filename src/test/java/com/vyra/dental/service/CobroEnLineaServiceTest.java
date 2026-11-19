package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.PagoDAOMemoria;
import com.vyra.dental.dao.memoria.TratamientoDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.Cobro;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.pasarela.PasarelaDemo;
import com.vyra.dental.pasarela.PasarelaPago.OrdenCobro;

/** RF-13: cobro en línea con la pasarela demo (el mismo flujo que con Mercado Pago). */
class CobroEnLineaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2025, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private PagoDAOMemoria pagoDAO;
    private PagoService pagos;
    private CitaService citas;
    private CobroEnLineaService enLinea;
    private Cobro consulta;

    @BeforeEach
    void preparar() {
        CitaDAOMemoria citaDAO = new CitaDAOMemoria();
        pagoDAO = new PagoDAOMemoria();
        pagos = new PagoService(pagoDAO, new TratamientoDAOMemoria(citaDAO, pagoDAO), citaDAO, RELOJ);
        citas = new CitaService(citaDAO, pagoDAO, RELOJ);
        enLinea = new CobroEnLineaService(pagos, new PasarelaDemo(), RELOJ);
        Sesion.get().iniciar(new Usuario(3, "prueba", "Recepción", "pbkdf2$1$a$b", Rol.RECEPCION, true), null);

        Paciente maria = new Paciente();
        maria.setIdPaciente(1);
        maria.setNombres("María");
        maria.setApellidos("Quispe");
        maria.setDni("72481935");
        Profesional salinas = new Profesional(1, null, "Marco", "Salinas", null, null, null, 80);
        Cita cita = citas.agendar(new Cita(0, maria, salinas, HOY, LocalTime.of(11, 0), LocalTime.of(11, 30),
                "Evaluación", EstadoCita.CONFIRMADA, 80));
        consulta = pagos.cobroDeConsulta(cita.getIdCita()).orElseThrow();
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void mientrasNoPagaNoSeRegistraNadaYAlAprobarseSeRegistraUnaVez() {
        OrdenCobro orden = enLinea.iniciar(consulta, 80);
        assertTrue(orden.referencia().startsWith("VYRA-C" + consulta.getId() + "-"));
        assertEquals(Optional.empty(), enLinea.verificar(consulta, orden));
        assertTrue(pagoDAO.listarTodos().isEmpty());

        enLinea.simularPago(orden);
        Pago pago = enLinea.verificar(consulta, orden).orElseThrow();

        assertEquals(MetodoPago.MERCADO_PAGO, pago.getMetodoPago());
        assertEquals(80, pago.getMonto());
        assertEquals(consulta.getId(), pago.getIdCita());
        assertEquals(0.0, pagos.saldoConsulta(consulta.getId()));
        // Verificar otra vez el mismo cobro no lo registra de nuevo
        assertThrows(ReglaNegocioException.class, () -> enLinea.verificar(consulta, orden));
        assertEquals(1, pagoDAO.listarTodos().size());
    }

    @Test
    void noSeCobraEnLineaMasQueElSaldoNiSinPermiso() {
        assertEquals("monto", assertThrows(ReglaNegocioException.class,
                () -> enLinea.iniciar(consulta, 100)).getCampo().orElseThrow());

        Sesion.get().cerrar();
        Sesion.get().iniciar(new Usuario(4, "doc", "Odontólogo", "pbkdf2$1$a$b", Rol.ODONTOLOGO, true), null);
        assertThrows(ReglaNegocioException.class, () -> enLinea.iniciar(consulta, 80));
    }
}
