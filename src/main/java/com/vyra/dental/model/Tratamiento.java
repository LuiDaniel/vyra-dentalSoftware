package com.vyra.dental.model;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tratamiento {
    private int idTratamiento;

    @NotBlank(message = "La descripcion es obligatoria")
    private String descripcion;

    @Positive(message = "El costo debe ser mayor a 0")
    private double costo;

    @NotNull(message = "Debes seleccionar un estado")
    private EstadoTratamiento estado;

    private List<@Min(value = 11, message = "Pieza dental invalida (minimo 11)")
                 @Max(value = 85, message = "Pieza dental invalida (maximo 85)") Integer> piezasDentales;
}
