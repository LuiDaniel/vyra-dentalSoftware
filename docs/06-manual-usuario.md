# Vyra — Manual de Usuario

**Versión:** 1.1 (consulta antes de atender y cobro con Mercado Pago)
**Fecha:** 2026-11-19

Las capturas de este manual son de la aplicación real con los datos de ejemplo (pacientes y profesionales ficticios). Están en [`docs/manual/`](./manual).

## 1. Introducción

Vyra es una aplicación de escritorio para el trabajo diario de un consultorio dental. Reúne en un solo lugar a los pacientes, la agenda de los odontólogos, la historia clínica, el odontograma, los tratamientos y los pagos.

La usan tres tipos de personas, cada una con su propio usuario y su rol:

| Rol | Qué hace en Vyra |
|---|---|
| **Recepción** | Registra pacientes, agenda y confirma citas, y cobra las consultas y los tratamientos (en caja o con Mercado Pago). |
| **Odontólogo/a** | Ve sus citas, escribe la historia clínica, marca el odontograma y registra los tratamientos. |
| **Administrador** | Ve todo, consulta los reportes y gestiona los profesionales y los usuarios. |

El menú de la izquierda solo muestra las pantallas que tu rol puede usar (ver la tabla de la sección 12).

## 2. Instalación y primer ingreso

**Requisitos:** Windows 10 u 11 de 64 bits. No hace falta instalar Java: la versión instalada lo trae incluido.

1. Ejecuta **`Vyra-Setup-1.0.0.exe`** y sigue el asistente (Siguiente → Instalar). No pide permisos de administrador: se instala para tu usuario y crea un acceso directo en el menú Inicio (y en el escritorio, si marcas la casilla). Para quitarlo: *Configuración → Aplicaciones → Vyra → Desinstalar* (tus datos no se borran).
2. Abre **Vyra** desde el menú Inicio o el escritorio.
3. Ingresa con el usuario **`admin`** y la contraseña **`admin123`**.
4. Entra a **Usuarios y roles**, crea un usuario para cada persona del consultorio y, en tu propio usuario, usa **Restablecer contraseña** para cambiar `admin123` por una contraseña propia.

Los datos se guardan en un archivo en `%LOCALAPPDATA%\Vyra\vyra.db` (por ejemplo, `C:\Users\Recepcion\AppData\Local\Vyra\vyra.db`). **Para hacer una copia de seguridad, cierra Vyra y copia ese archivo** a un USB o a la nube.

## 3. Inicio de sesión (CU-08)

![Inicio de sesión](./manual/01-login.png)

1. Escribe tu **usuario** y tu **contraseña**. El ojo de la derecha muestra u oculta la contraseña.
2. Marca **Recordar mi usuario** si siempre usas la misma computadora.
3. Pulsa **Ingresar** (o Enter).

Si el usuario o la contraseña no son correctos, aparece "Usuario o contraseña incorrectos" (por seguridad no se dice cuál de los dos). Si tu usuario fue desactivado, no podrás entrar: pide al administrador que lo active.

## 4. Inicio

![Inicio](./manual/02-inicio.png)

Es la primera pantalla después de ingresar:

- **Aviso de citas próximas:** si hay citas en la próxima hora, aparece una franja verde con los nombres y la hora (RF-12). "Ver agenda" abre la Agenda.
- **Indicadores:** citas de hoy, pacientes registrados, tratamientos en curso y, según tu rol, los ingresos del mes (administrador), los pagos de hoy (recepción) o tus citas atendidas (odontólogo).
- **Agenda de hoy:** haz clic en una cita para abrirla en la Agenda.
- **Accesos rápidos** y, arriba, el **buscador de pacientes**: escribe un nombre o un DNI y elige al paciente para abrir su ficha. Desde cualquier pantalla, **Ctrl+K** te trae a este buscador.

## 5. Pacientes (CU-01)

![Pacientes](./manual/03-pacientes.png)

**Registrar un paciente nuevo:**

1. Pulsa **Nuevo paciente**.
2. Completa los datos. Los campos con `*` son obligatorios: nombres, apellidos y DNI (8 dígitos).
3. Si tiene alergias, escríbelas en **Alergias**: se mostrarán como alerta en su odontograma.
4. Pulsa **Registrar paciente**.

Si algo está mal, el error aparece en rojo debajo del campo (por ejemplo, "El DNI debe tener 8 dígitos" o "Ya existe un paciente con el DNI 72481935").

**Buscar y editar:** escribe en el buscador (nombre o DNI, con o sin tildes) y usa los filtros *Con alergias* y *Menores de edad*. Al elegir un paciente se abre su ficha a la derecha; cambia lo necesario y pulsa **Guardar cambios**.

**Dar de baja:** en la ficha, **Dar de baja** (pide confirmación). El paciente deja de aparecer en las búsquedas, pero su historial se conserva.

La ficha tiene pestañas: **Datos**, **Historia clínica** (administrador y odontólogo), **Citas** y **Pagos** (administrador y recepción).

## 6. Agenda (CU-02, CU-03)

![Agenda](./manual/04-agenda.png)

La agenda muestra una columna por profesional. Arriba eliges el día con las flechas, **Hoy** o la tira de la semana, y la vista: **Día**, **Semana** o **Lista**.

**Agendar una cita:**

1. Haz clic en un espacio libre de la cuadrícula (o completa el panel **Nueva cita**).
2. Elige el **paciente** (búscalo por nombre o DNI), el **profesional**, la **fecha** y la **hora de inicio**. La hora de fin se propone sola (30 minutos después) y se puede cambiar.
3. Escribe el motivo y elige si queda **Pendiente** o **Confirmada**.
4. Pulsa **Agendar cita**.

![Cruce de horario](./manual/05-agenda-cruce.png)

**Costo de la consulta:** al elegir el profesional se copia el costo de su consulta (por ejemplo S/ 80.00). Puedes cambiarlo para esa cita; pon **0** si no se cobra (un control, por ejemplo).

![Aviso de deuda al agendar](./manual/16-agenda-aviso-deuda.png)

**Aviso de deuda:** si el paciente debe saldo de algún tratamiento, al elegirlo aparece un aviso naranja ("Rosa debe S/ 380.00 de 1 tratamiento"). No impide agendar: es para recordarle pasar por caja.

**Cruce de horario:** si el profesional ya tiene una cita a esa hora, Vyra lo avisa antes de guardar y propone **horarios libres**; haz clic en uno para usarlo. Las citas van en bloques de 30 minutos, de 08:00 a 20:00, y no se pueden agendar en fechas u horas que ya pasaron.

**Estados de una cita:**

| Estado | Significado |
|---|---|
| Pendiente | Agendada, falta confirmar con el paciente. |
| Confirmada | El paciente confirmó que vendrá. |
| Atendida | El odontólogo la atendió (lo marca el odontólogo o recepción). Solo se puede si la consulta ya está pagada. |
| Cancelada | No se realizará. No se borra: queda en el historial y libera el horario. |

Al elegir una cita se ven sus datos, el estado de la **consulta** y los botones para **confirmar**, **reprogramar**, **cancelar** o **marcar como atendida**.

**Primero se paga, después se atiende (RN-16):** si la consulta tiene costo y todavía no se cobró, el detalle dice "Falta cobrar S/ 60.00", aparece el botón **Cobrar consulta** (abre Pagos con esa consulta ya elegida) y **Marcar atendida** no aparece hasta que se pague. Cuando está pagada, el detalle dice "Pagada" y ya se puede atender:

![Consulta pagada](./manual/15-agenda-consulta-pagada.png)

Si cancelas una cita cuya consulta ya se cobró, Vyra te recuerda devolver el dinero al paciente (o mejor, reprogramarla).

## 7. Historia clínica (CU-04)

![Historia clínica](./manual/06-historia-clinica.png)

Se abre desde el menú (eliges al paciente en el buscador) o desde la pestaña **Historia clínica** de su ficha.

- **Alergias y antecedentes:** el odontólogo los actualiza aquí y pulsa **Guardar alergias y antecedentes**.
- **Nueva entrada:** escribe el diagnóstico y/o las observaciones de la visita. La fecha es la de hoy y la cita de hoy del paciente se elige sola. Pulsa **Guardar entrada**.
- Las entradas **no se pueden editar ni borrar** después de guardarlas: se agrega una nueva si hace falta corregir algo.
- **Exportar PDF** guarda la historia clínica completa para imprimirla (CU-11).

## 8. Odontograma (CU-05)

![Odontograma](./manual/07-odontograma.png)

Muestra las 32 piezas en notación FDI. Cada estado tiene un color y una letra: **C** cariada, **O** obturada, **T** en tratamiento, **X** extraída (sin letra = sana).

**Registrar cambios:**

1. En **Marcar como**, elige el estado (por ejemplo, *Cariada*).
2. Haz clic en las piezas que quieres marcar. Las piezas cambiadas quedan con borde punteado: todavía no están guardadas.
3. Elige una pieza para escribir la **observación del cambio** y ver su **historial**.
4. Pulsa **Guardar n cambios**.

Si sales de la pantalla con cambios sin guardar, Vyra pregunta antes de descartarlos. El historial de cada pieza conserva todos sus cambios con la fecha y el profesional.

Desde una pieza, **Registrar tratamiento para esta pieza** abre Tratamientos con la pieza ya escrita. **Exportar PDF** genera el odontograma impreso.

## 9. Tratamientos y pagos (CU-06, CU-10, CU-13)

![Tratamientos](./manual/08-tratamientos.png)

**Registrar un tratamiento (odontólogo):**

1. Pulsa **Nuevo tratamiento**.
2. Elige la **cita** (de hoy o atendida en los últimos 30 días), escribe la **descripción**, las **piezas** separadas por comas (por ejemplo `36, 47`) y el **costo**.
3. Pulsa **Registrar tratamiento**. Cuando termine y esté pagado, usa **Marcar como finalizado**.

La barra de cada tratamiento muestra cuánto se pagó de su costo. **Un tratamiento con saldo no se puede finalizar (RN-17):** el botón queda desactivado con el aviso "Para finalizarlo, recepción debe cobrar el saldo de S/ 380.00". Los tratamientos largos (ortodoncia, por ejemplo) se pueden ir pagando en partes mientras están en curso.

![Pagos](./manual/09-pagos.png)

**Registrar un pago (recepción o administrador):**

1. En **Pagos**, busca por el nombre o DNI del paciente y elige **la consulta** de una cita o **el tratamiento** que se paga (o pulsa **Cobrar consulta** en la Agenda, **Registrar pago** en Tratamientos o **Cobrar** en la pestaña Pagos de la ficha). Primero aparecen las consultas por cobrar más cercanas a hoy y después los tratamientos con saldo.
2. Escribe el **monto**, revisa la **fecha** (no puede ser futura) y elige el **método**: efectivo, Yape/Plin, tarjeta o transferencia. El número de operación es opcional.
3. Abajo verás el saldo que quedará. Pulsa **Registrar pago**.

No se puede cobrar más que el saldo pendiente: "El monto supera el saldo pendiente (S/ 400.00)". Arriba, los indicadores muestran lo cobrado en el periodo (hoy, semana o mes), el saldo pendiente de los tratamientos y el reparto por método. En la tabla, los pagos de consulta dicen **Consulta** en la columna *Concepto*. **Exportar** guarda los pagos del periodo en PDF.

### Cobrar con Mercado Pago (CU-13)

![Cobro con Mercado Pago](./manual/14-cobro-mercado-pago.png)

1. Elige la consulta o el tratamiento y escribe el monto, como en un pago normal.
2. Pulsa **Cobrar S/ … con Mercado Pago**. Se abre una ventana con un **código QR**.
3. El paciente lo escanea con la cámara de su celular y paga con tarjeta, Yape o su cuenta de Mercado Pago. También puedes **copiar el enlace** y mandárselo por WhatsApp, o abrirlo en el navegador.
4. Vyra pregunta solo cada pocos segundos. Cuando Mercado Pago aprueba el pago, la ventana se cierra y el pago queda registrado con el método **Mercado Pago** y su número de operación. Si el paciente ya pagó y no quieres esperar, pulsa **Ya pagó, verificar**.

Si cierras la ventana sin que el paciente pague, no se registra nada. Si Mercado Pago rechaza la tarjeta, la ventana lo dice y el paciente puede intentarlo otra vez con el mismo QR.

**Modo demo:** si Vyra no tiene credenciales de Mercado Pago, la ventana dice *MODO DEMO · sin dinero real* y tiene el botón **Simular pago aprobado**, para practicar o exponer sin internet.

**Activar Mercado Pago de verdad (administrador):** crea una cuenta en Mercado Pago, entra a *Tus integraciones* en mercadopago.com.pe/developers, crea una aplicación (Checkout Pro) y copia su **Access Token** (el de *Credenciales de prueba* para probar sin dinero real; el de *producción* para cobrar de verdad, con la comisión de Mercado Pago). Luego crea el archivo `%LOCALAPPDATA%\Vyra\vyra.properties` con esta línea y vuelve a abrir Vyra:

```
mercadopago.access_token=PEGA-AQUI-TU-ACCESS-TOKEN
```

El token es secreto: no lo compartas ni lo subas a GitHub.

![Pagos en la ficha del paciente](./manual/10-ficha-pagos.png)

## 10. Reportes (CU-09) — solo Administrador

![Reportes](./manual/11-reportes.png)

Elige el tipo de reporte arriba a la derecha:

- **Citas:** citas del periodo (y de un profesional, si quieres), con atendidas, canceladas y el % de asistencia.
- **Historial de paciente:** busca al paciente y verás sus tratamientos con el costo, lo pagado y el saldo.
- **Ingresos:** lo cobrado por método de pago y por día.

Cambia las fechas o usa **Hoy**, **Esta semana** o **Este mes**; el reporte se actualiza solo. **Exportar PDF** guarda el reporte que estás viendo.

## 11. Profesionales y usuarios — solo Administrador

![Profesionales](./manual/12-profesionales.png)

**Profesionales (CU-07):** registra a cada odontólogo con su especialidad, teléfono, horario y **costo de consulta** (se copia a cada cita nueva; 0 = sin costo). Cada uno tiene su columna en la agenda. Para que un odontólogo vea sus citas al ingresar, asócialo a su **usuario** (con rol Odontólogo) en el campo *Usuario para iniciar sesión*. Un profesional con citas no se puede eliminar.

![Usuarios y roles](./manual/13-usuarios.png)

**Usuarios y roles (RF-08):** crea un usuario por persona, con su nombre completo, el usuario para ingresar (sin espacios), el rol y una contraseña de al menos 6 caracteres. Para que alguien deje de entrar, desmarca **Usuario activo**: los usuarios no se borran. Si alguien olvidó su contraseña, usa **Restablecer contraseña**. No puedes desactivarte ni quitarte el rol de administrador a ti mismo.

## 12. Qué ve cada rol

| Pantalla | Administrador | Recepción | Odontólogo/a |
|---|---|---|---|
| Inicio | Sí | Sí | Sí (sus citas) |
| Agenda | Sí | Sí (y cobrar la consulta) | Sus citas + marcar "Atendida" (con la consulta pagada) |
| Pacientes | Sí | Sí | Solo lectura |
| Historia clínica | Solo lectura | No | Sí |
| Odontograma | Solo lectura | No | Sí |
| Tratamientos | Solo lectura | Solo lectura (para cobrar) | Sí |
| Pagos | Sí | Sí | No |
| Reportes, Profesionales, Usuarios | Sí | No | No |

## 13. Preguntas frecuentes

**Olvidé mi contraseña.** Pide al administrador que use *Restablecer contraseña* en Usuarios y roles. Si el administrador olvidó la suya y no hay otro administrador, hay que recuperar una copia de seguridad de `vyra.db`.

**Soy odontólogo y no veo ninguna cita.** Tu usuario no está asociado a un profesional. El administrador debe hacerlo en **Profesionales**, campo *Usuario para iniciar sesión*.

**No me deja agendar a esa hora.** El profesional ya tiene otra cita que se cruza, la hora ya pasó o está fuera del horario (08:00 a 20:00 en bloques de 30 minutos). Usa uno de los horarios libres que propone el formulario.

**No me deja marcar la cita como atendida.** Falta cobrar la consulta: pulsa **Cobrar consulta** en el detalle de la cita (recepción) o pide a recepción que la cobre.

**No puedo finalizar un tratamiento.** Todavía tiene saldo. Recepción debe registrar el pago que falta en **Pagos**.

**La ventana de Mercado Pago dice "MODO DEMO".** Vyra no encontró el Access Token. Revisa el archivo `vyra.properties` (sección 9).

**No aparece un paciente que registré antes.** Revisa que el filtro sea *Todos* y que el texto del buscador esté vacío. Si se dio de baja, ya no aparece en las búsquedas (su historial se conserva).

**Marqué piezas en el odontograma y no se guardaron.** Los cambios quedan pendientes (borde punteado) hasta pulsar **Guardar n cambios**.

**El PDF no se guarda.** Si ya existe un archivo con ese nombre y está abierto en el visor de PDF, ciérralo y vuelve a exportar.

**¿Cómo hago una copia de seguridad?** Cierra Vyra y copia `%LOCALAPPDATA%\Vyra\vyra.db` (escribe esa ruta en la barra del Explorador de archivos). Para restaurarla, cierra Vyra y vuelve a poner el archivo en esa carpeta.

---

*Este manual, con sus capturas, es también el anexo de la memoria descriptiva ([`07-memoria-descriptiva.md`](./07-memoria-descriptiva.md)).*
