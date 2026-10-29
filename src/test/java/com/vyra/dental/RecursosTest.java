package com.vyra.dental;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecursosTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/com/vyra/schema.sql",
            "/com/vyra/fonts/Inter-Regular.ttf",
            "/com/vyra/fonts/Inter-Medium.ttf",
            "/com/vyra/fonts/Inter-SemiBold.ttf",
            "/com/vyra/fonts/Inter-Bold.ttf"
    })
    void elRecursoEstaEnElClasspath(String ruta) {
        assertNotNull(getClass().getResource(ruta), "Falta el recurso " + ruta);
    }
}
