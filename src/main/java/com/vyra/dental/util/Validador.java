package com.vyra.dental.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Valida un objeto segun las anotaciones de Jakarta Validation
 * (@NotBlank, @Size, @Positive, ...) puestas en el modelo.
 * Si hay errores lanza ValidacionException (una IllegalArgumentException)
 * con todos los mensajes y el mensaje de cada campo.
 */
public final class Validador {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Validador() {
    }

    public static <T> void validar(T objeto) {
        Set<ConstraintViolation<T>> errores = VALIDATOR.validate(objeto);
        if (errores.isEmpty()) {
            return;
        }
        List<ConstraintViolation<T>> ordenados = errores.stream()
                .sorted((a, b) -> a.getMessage().compareTo(b.getMessage()))
                .toList();
        String mensaje = ordenados.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("\n- ", "- ", ""));
        Map<String, String> porCampo = new LinkedHashMap<>();
        for (ConstraintViolation<T> error : ordenados) {
            // Si un campo tiene varios errores, se muestra el primero
            porCampo.putIfAbsent(error.getPropertyPath().toString(), error.getMessage());
        }
        throw new ValidacionException(mensaje, porCampo);
    }
}
