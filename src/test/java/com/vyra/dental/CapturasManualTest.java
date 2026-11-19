package com.vyra.dental;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.vyra.dental.controller.MainController;
import com.vyra.dental.controller.Pantalla;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.service.Servicios;
import com.vyra.dental.util.DatosIniciales;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * No es una prueba: abre Vyra con los datos de ejemplo (en una base en
 * memoria), recorre las pantallas y guarda capturas PNG para el manual y el
 * video de presentación. Solo corre si se pide:
 *
 * <pre>mvn test "-Dtest=CapturasManualTest" "-Dvyra.capturas=docs/manual" "-Dvyra.capturas.escala=1"</pre>
 *
 * Con escala 1.5 las capturas salen a 1920×1200 (para el video).
 */
@EnabledIfSystemProperty(named = "vyra.capturas", matches = ".+")
class CapturasManualTest {

    private static final int ANCHO = 1280;
    private static final int ALTO = 800;

    private Path carpeta;
    private double escala;
    private Stage ventana;

    @Test
    void capturarPantallas() throws Exception {
        carpeta = Path.of(System.getProperty("vyra.capturas")).toAbsolutePath();
        Files.createDirectories(carpeta);
        escala = Double.parseDouble(System.getProperty("vyra.capturas.escala", "1"));
        System.setProperty(DatosIniciales.PROPIEDAD_DEMO, "true");

        CountDownLatch listo = new CountDownLatch(1);
        Platform.startup(listo::countDown);
        listo.await();

        enFx(() -> {
            ventana = new Stage();
            new App().start(ventana);
        });
        esperar(800);
        capturar("01-login");

        // ===== Recepción =====
        entrar("recepcion");
        capturar("02-inicio");

        LocalDate hoy = LocalDate.now();
        Cita porCobrar = Servicios.citas().citasDelDia(hoy).stream()
                .filter(c -> c.getEstado() == EstadoCita.CONFIRMADA && c.getCostoConsulta() > 0
                        && !Servicios.citas().consultaPagada(c))
                .findFirst().orElseThrow();
        enFx(() -> MainController.actual().navegar(Pantalla.AGENDA));
        esperar(500);
        enFx(() -> llamar(controlador(), "seleccionarCita", Cita.class, porCobrar));
        esperar(600);
        capturar("04-agenda-consulta-por-cobrar");

        enFx(() -> MainController.actual().navegar(Pantalla.PAGOS, porCobrar));
        esperar(700);
        capturar("09-pagos-consulta");

        // Cobro con la pasarela demo: la ventana se captura mientras está abierta
        AtomicReference<Window> dialogo = new AtomicReference<>();
        Platform.runLater(() -> llamar(controlador(), "cobrarEnLinea"));
        esperar(1200);
        enFx(() -> Window.getWindows().stream().filter(w -> w != ventana && w.isShowing()).findFirst()
                .ifPresent(dialogo::set));
        capturarEscena("14-cobro-mercado-pago", dialogo.get().getScene());
        enFx(() -> boton(dialogo.get().getScene(), "Simular pago aprobado").fire());
        esperar(5500);
        capturar("15-pago-aprobado");

        enFx(() -> MainController.actual().navegar(Pantalla.AGENDA));
        esperar(500);
        Cita pagada = Servicios.citas().buscarPorId(porCobrar.getIdCita()).orElseThrow();
        enFx(() -> llamar(controlador(), "seleccionarCita", Cita.class, pagada));
        esperar(600);
        capturar("16-agenda-consulta-pagada");

        // Nueva cita: el costo viene del profesional y se avisa si el paciente debe
        enFx(() -> {
            llamar(controlador(), "nuevaCita");
            Object agenda = controlador();
            TextField campo = (TextField) campo(agenda, "campoPaciente");
            campo.setText("Apaza");
        });
        esperar(900);
        enFx(() -> {
            Object agenda = controlador();
            var paciente = Servicios.pacientes().buscar("29874561", com.vyra.dental.model.FiltroPaciente.TODOS, 0).get(0);
            llamar(agenda, "elegirPaciente", com.vyra.dental.model.Paciente.class, paciente);
            @SuppressWarnings("unchecked")
            var combo = (javafx.scene.control.ComboBox<com.vyra.dental.model.Profesional>) campo(agenda, "campoProfesional");
            combo.setValue(combo.getItems().stream().filter(p -> p.getApellidos().startsWith("Salinas"))
                    .findFirst().orElseThrow());
        });
        esperar(600);
        capturar("05-agenda-nueva-cita-deuda");

        // ===== Odontólogo =====
        salir();
        entrar("msalinas");
        TratamientoResumen conSaldo = Servicios.tratamientos().buscar("", EstadoTratamiento.EN_CURSO).stream()
                .filter(r -> r.getSaldo() > 0).findFirst().orElseThrow();
        enFx(() -> MainController.actual().navegar(Pantalla.TRATAMIENTOS));
        esperar(600);
        enFx(() -> llamar(controlador(), "mostrarDetalle", TratamientoResumen.class, conSaldo));
        esperar(500);
        capturar("08-tratamiento-con-saldo");

        // ===== Administrador =====
        salir();
        entrar("admin");
        enFx(() -> MainController.actual().navegar(Pantalla.PROFESIONALES));
        esperar(600);
        enFx(() -> {
            Object profesionales = controlador();
            var lista = Servicios.profesionales().listarTodos();
            llamar(profesionales, "mostrar", com.vyra.dental.model.Profesional.class,
                    lista.stream().filter(p -> p.getApellidos().startsWith("Salinas")).findFirst().orElseThrow());
        });
        esperar(500);
        capturar("12-profesionales");
        for (Pantalla pantalla : List.of(Pantalla.PACIENTES, Pantalla.ODONTOGRAMA, Pantalla.REPORTES, Pantalla.USUARIOS)) {
            enFx(() -> MainController.actual().navegar(pantalla));
            esperar(700);
            capturar("pantalla-" + pantalla.name().toLowerCase());
        }
        enFx(() -> MainController.actual().navegar(Pantalla.PAGOS));
        esperar(700);
        capturar("pantalla-pagos");

        enFx(ventana::close);
        Platform.exit();
    }

    // ===== Ayudantes =====

    private void entrar(String usuario) throws Exception {
        String contrasena = usuario.equals("admin") ? DatosIniciales.ADMIN_CONTRASENA_INICIAL
                : DatosIniciales.CONTRASENA_EJEMPLO;
        enFx(() -> {
            Servicios.auth().iniciarSesion(usuario, contrasena);
            App.mostrarPrincipal();
            ventana.setMaximized(false);
            ventana.setWidth(ANCHO);
            ventana.setHeight(ALTO);
            ventana.getScene().getRoot().resize(ANCHO, ALTO);
        });
        esperar(900);
    }

    private void salir() throws Exception {
        enFx(() -> Servicios.auth().cerrarSesion());
    }

    private Object controlador() {
        try {
            Field campo = MainController.class.getDeclaredField("controladorActual");
            campo.setAccessible(true);
            return campo.get(MainController.actual());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object campo(Object objeto, String nombre) {
        try {
            Field campo = objeto.getClass().getDeclaredField(nombre);
            campo.setAccessible(true);
            return campo.get(objeto);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void llamar(Object objeto, String metodo) {
        try {
            Method m = objeto.getClass().getDeclaredMethod(metodo);
            m.setAccessible(true);
            m.invoke(objeto);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(metodo, e);
        }
    }

    private static <T> void llamar(Object objeto, String metodo, Class<T> tipo, T argumento) {
        try {
            Method m = objeto.getClass().getDeclaredMethod(metodo, tipo);
            m.setAccessible(true);
            m.invoke(objeto, argumento);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(metodo, e);
        }
    }

    private static Button boton(Scene escena, String texto) {
        return escena.getRoot().lookupAll(".button").stream()
                .filter(n -> n instanceof Button b && texto.equals(b.getText()))
                .map(Button.class::cast).findFirst().orElseThrow();
    }

    private void capturar(String nombre) throws Exception {
        capturarEscena(nombre, ventana.getScene());
    }

    private void capturarEscena(String nombre, Scene escena) throws Exception {
        AtomicReference<WritableImage> imagen = new AtomicReference<>();
        enFx(() -> {
            SnapshotParameters parametros = new SnapshotParameters();
            parametros.setTransform(Transform.scale(escala, escala));
            imagen.set(escena.getRoot().snapshot(parametros, null));
        });
        guardar(imagen.get(), carpeta.resolve(nombre + ".png").toFile());
        System.out.println("Captura: " + nombre);
    }

    private static void guardar(WritableImage imagen, File archivo) throws IOException {
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        PixelReader pixeles = imagen.getPixelReader();
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                salida.setRGB(x, y, pixeles.getArgb(x, y));
            }
        }
        ImageIO.write(salida, "png", archivo);
    }

    private static void esperar(long milisegundos) throws InterruptedException {
        Thread.sleep(milisegundos);
    }

    /** Corre en el hilo de JavaFX y espera a que termine. */
    private static void enFx(Accion accion) throws Exception {
        CountDownLatch hecho = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                accion.correr();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                hecho.countDown();
            }
        });
        if (!hecho.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX no respondió");
        }
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
    }

    @FunctionalInterface
    private interface Accion {
        void correr() throws Exception;
    }
}
