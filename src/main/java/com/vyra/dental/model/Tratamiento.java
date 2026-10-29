package com.vyra.dental.model;

import java.time.LocalDate;
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

    /** Cita en la que se realizó; el paciente se obtiene a través de ella. */
    private int idCita;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @Positive(message = "El costo debe ser mayor a 0")
    private double costo;

    @NotNull(message = "Debes seleccionar un estado")
    private EstadoTratamiento estado;

    private LocalDate fecha;

    // Notación FDI de piezas permanentes (RN-10); las temporales (51-85) quedan para una versión futura
    private List<@Min(value = 11, message = "Pieza dental inválida (notación FDI de 11 a 48)")
                 @Max(value = 48, message = "Pieza dental inválida (notación FDI de 11 a 48)") Integer> piezasDentales;
}
