package com.vyra.dental.util;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Valida un objeto segun las anotaciones de Jakarta Validation
 * (@NotBlank, @Size, @Positive, ...) puestas en el modelo.
 * Si hay errores lanza IllegalArgumentException con todos los mensajes.
 */
public final class Validador {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Validador() {
    }

    public static <T> void validar(T objeto) {
        Set<ConstraintViolation<T>> errores = VALIDATOR.validate(objeto);
        if (!errores.isEmpty()) {
            String mensaje = errores.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("\n- ", "- ", ""));
            throw new IllegalArgumentException(mensaje);
        }
    }
}
