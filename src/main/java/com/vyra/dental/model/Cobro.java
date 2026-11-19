package com.vyra.dental.model;

import com.vyra.dental.util.Formatos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Algo que recepción puede cobrar: la consulta de una cita (RN-16) o un
 * tratamiento con saldo (RN-12). No es una tabla: lo arma PagoService para el
 * buscador de la pantalla Pagos.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cobro {

    public enum Tipo {
        CONSULTA, TRATAMIENTO
    }

    private Tipo tipo;
    /** id_cita si es una consulta; id_tratamiento si es un tratamiento. */
    private int id;
    /** "Consulta · 20/11 09:00" o la descripción del tratamiento. */
    private String concepto;
    /** "Pendiente · Marco Salinas" o "En curso". */
    private String detalle;
    /** Id, nombres, apellidos y DNI. */
    private Paciente paciente;
    private double costo;
    private double pagado;

    public static Cobro deConsulta(Cita cita, double pagado) {
        String concepto = "Consulta del " + Formatos.diaMes(cita.getFecha()) + " · " + Formatos.hora(cita.getHoraInicio());
        String detalle = cita.getEstado().getEtiqueta() + " · " + cita.getProfesional().getNombreCompleto();
        return new Cobro(Tipo.CONSULTA, cita.getIdCita(), concepto, detalle, cita.getPaciente(),
                cita.getCostoConsulta(), pagado);
    }

    public static Cobro deTratamiento(TratamientoResumen r) {
        return new Cobro(Tipo.TRATAMIENTO, r.getTratamiento().getIdTratamiento(), r.getTratamiento().getDescripcion(),
                r.getTratamiento().getEstado().getEtiqueta(), r.getPaciente(), r.getTratamiento().getCosto(),
                r.getPagado());
    }

    public boolean esConsulta() {
        return tipo == Tipo.CONSULTA;
    }

    /** Costo − pagado, redondeado a céntimos. */
    public double getSaldo() {
        return Math.round((costo - pagado) * 100) / 100.0;
    }

    /** Fracción pagada, de 0 a 1 (para la barra de progreso). */
    public double getAvance() {
        return costo <= 0 ? 0 : Math.min(1, pagado / costo);
    }
}
