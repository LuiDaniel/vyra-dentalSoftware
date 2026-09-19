package com.vyra.dental.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    private int idUsuario;
    private String nombreUsuario;
    private String contrasenaHash;
    private Rol rol;

    public boolean autenticar(String intento) {
        return contrasenaHash.equals(hashear(intento));
    }

    private String hashear(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte b : bytes) {
                hexadecimal.append(String.format("%02x", b));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
