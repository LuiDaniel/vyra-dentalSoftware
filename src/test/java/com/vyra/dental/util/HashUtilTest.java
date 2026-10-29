package com.vyra.dental.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashUtilTest {

    @Test
    void verificaLaContrasenaCorrecta() {
        String hash = HashUtil.hashear("admin123");
        assertTrue(HashUtil.verificar("admin123", hash));
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        String hash = HashUtil.hashear("admin123");
        assertFalse(HashUtil.verificar("admin124", hash));
        assertFalse(HashUtil.verificar("", hash));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintos() {
        String primero = HashUtil.hashear("admin123");
        String segundo = HashUtil.hashear("admin123");
        assertNotEquals(primero, segundo);
        assertTrue(HashUtil.verificar("admin123", primero));
        assertTrue(HashUtil.verificar("admin123", segundo));
    }

    @Test
    void elHashTieneElFormatoEsperado() {
        String[] partes = HashUtil.hashear("admin123").split("\\$");
        assertEquals(4, partes.length);
        assertEquals("pbkdf2", partes[0]);
        assertEquals("120000", partes[1]);
    }

    @Test
    void unHashInvalidoNuncaCoincide() {
        assertFalse(HashUtil.verificar("admin123", null));
        assertFalse(HashUtil.verificar("admin123", ""));
        assertFalse(HashUtil.verificar("admin123", "admin123"));
        assertFalse(HashUtil.verificar("admin123", "pbkdf2$abc$sal$hash"));
        assertFalse(HashUtil.verificar(null, HashUtil.hashear("admin123")));
    }
}
