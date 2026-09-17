package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tratamiento {

    private int idTratamiento;
    private Cita cita;
    private String descripcion;
    private double costo;
    private EstadoTratamiento estado;
    private LocalDate fecha;
    private List<Integer> piezasDentales;
}