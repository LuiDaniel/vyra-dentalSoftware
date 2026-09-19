package com.vyra.dental.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tratamiento {
    private int idTratamiento;
    private String descripcion;
    private double costo;
    private EstadoTratamiento estado;
    private List<Integer> piezasDentales;
}
