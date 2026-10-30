package com.vyra.dental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Punto de entrada de la aplicación empaquetada con jpackage. JavaFX no arranca desde el
 * classpath si la clase principal hereda de {@code Application}, por eso existe esta clase aparte.
 * Instalada, la base de datos va en %APPDATA%\Vyra\vyra.db (la carpeta del programa puede no
 * ser escribible); con {@code mvn javafx:run} sigue siendo vyra.db en la carpeta de ejecución.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) throws IOException {
        // jpackage define jpackage.app-path en la aplicación empaquetada
        if (System.getProperty("vyra.bd.url") == null && System.getProperty("jpackage.app-path") != null) {
            String base = System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home");
            Path carpeta = Path.of(base, "Vyra");
            Files.createDirectories(carpeta);
            System.setProperty("vyra.bd.url", "jdbc:sqlite:" + carpeta.resolve("vyra.db"));
        }
        App.main(args);
    }
}
