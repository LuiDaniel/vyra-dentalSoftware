# Vyra — Manual de Usuario

**Versión:** 1.0
**Fecha:** 2026-10-30

> **Capturas pendientes.** Cada sección marca con *(Captura: …)* la imagen que conviene insertar. Hay que tomarlas con la aplicación en marcha (se puede cargar datos de ejemplo con `mvn javafx:run -Dvyra.demo=true`); el diseño de referencia está en [`docs/ui`](./ui). Usa siempre datos ficticios.

## 1. Introducción

Vyra es un programa de escritorio para llevar el día a día de un consultorio dental: pacientes, agenda de citas, historia clínica, odontograma, tratamientos y pagos. Reemplaza el cuaderno o la hoja de cálculo y evita errores típicos, como dos citas a la misma hora para un mismo odontólogo.

Lo usan tres tipos de personas, y cada una ve solo lo que le corresponde:

| Pantalla | Administrador | Recepción | Odontólogo |
|---|---|---|---|
| Inicio | Sí | Sí | Sí (solo sus citas) |
| Agenda | Sí | Sí | Solo sus citas; puede marcarlas como atendidas |
| Pacientes | Sí | Sí | Solo consulta |
| Odontograma e Historia clínica | Solo consulta | No | Sí |
| Tratamientos | Solo consulta | Solo consulta | Sí |
| Pagos | Sí | Sí | No |
| Reportes, Profesionales, Usuarios y roles | Sí | No | No |

Las pantallas que tu usuario no puede usar no aparecen en el menú de la izquierda.

## 2. Instalación y primer ingreso

**Requisitos:** Windows 10 u 11. La versión empaquetada ya incluye todo lo necesario (no hay que instalar Java).

**Obtener el programa**
1. Abre una ventana de PowerShell en la carpeta del proyecto y ejecuta `powershell -ExecutionPolicy Bypass -File scripts/empaquetar.ps1`.
2. Se crea la carpeta `target/instalador/Vyra`. Cópiala donde quieras (por ejemplo, el escritorio o `C:\Vyra`) y abre `Vyra.exe`.
3. Para generar un instalador `.exe` con acceso directo en el menú Inicio, instala antes [WiX Toolset 3](https://wixtoolset.org) y ejecuta el mismo comando con `-Instalador`.

**Dónde quedan los datos.** La primera vez que se abre, Vyra crea su base de datos en `%APPDATA%\Vyra\vyra.db` (escribe esa ruta en la barra del Explorador de archivos para llegar). **Haz copias de seguridad de ese archivo.** Si lo borras, se pierden todos los datos.

**Primer ingreso**
- Usuario: `admin` — Contraseña: `admin123`.
- Cambia esa contraseña de inmediato: entra a **Usuarios y roles**, elige `Administrador`, escribe la nueva contraseña (mínimo 6 caracteres) y pulsa **Restablecer contraseña**.
- Después crea una cuenta para cada persona del consultorio (ver §10).

## 3. Inicio de sesión

*(Captura: pantalla de inicio de sesión)*

1. Escribe tu usuario y tu contraseña. El ojo del campo de contraseña la muestra u oculta.
2. Pulsa **Ingresar** o la tecla Enter.
3. Marca **Recordar mi usuario** si quieres que el programa guarde tu nombre de usuario (nunca guarda la contraseña).

Si algo falla verás "Usuario o contraseña incorrectos." (el mensaje no dice cuál de los dos datos es el equivocado, a propósito). Si ves "Tu usuario está desactivado", consulta con el administrador. Para salir, usa el botón de la parte inferior del menú.

## 4. Inicio

*(Captura: pantalla de inicio)*

Es lo primero que ves al entrar:
- **Aviso azul** cuando hay citas en la próxima hora, con los nombres y las horas. **Ver agenda** te lleva a la agenda.
- **Cuatro cifras**: citas de hoy, pacientes registrados, tratamientos en curso y, según tu rol, los ingresos del mes (administrador), los pagos de hoy (recepción) o las citas atendidas hoy (odontólogo).
- **Agenda de hoy**, con la hora, el paciente, el motivo, el profesional y el estado de cada cita.
- **Ingresos de la semana** (solo administrador) y **Accesos rápidos** a las tareas más comunes.

## 5. Pacientes

*(Captura: lista de pacientes con la ficha abierta)*

**Buscar.** Escribe parte del nombre o del DNI; no importan las mayúsculas ni las tildes ("lucia" encuentra a "Lucía"). Los filtros muestran solo los pacientes con alergias o los menores de edad.

**Registrar un paciente** (administrador y recepción)
1. Pulsa **Nuevo paciente**.
2. Completa los campos con `*` (nombres, apellidos, DNI de 8 dígitos y fecha de nacimiento) y, si lo conoces, teléfono, dirección, contacto de emergencia, alergias y antecedentes.
3. Pulsa **Registrar paciente**. Si algo está mal, el error aparece en rojo debajo del campo.

El DNI no se puede repetir, ni siquiera con un paciente dado de baja.

**Editar y dar de baja.** Elige al paciente de la lista, cambia los datos y pulsa **Guardar cambios**. **Dar de baja** lo oculta de las búsquedas pero conserva todo su historial.

**Pestañas de la ficha:** Datos, Historia clínica, Citas y Pagos (según tu rol). En la lista, los botones de la última columna abren el odontograma del paciente o una cita nueva para él.

## 6. Agenda y citas

*(Captura: agenda del día con varios profesionales)*

La agenda tiene tres vistas: **Día** (una columna por profesional, de 08:00 a 20:00), **Semana** y **Lista**. Los botones de flechas y **Hoy** cambian la fecha. Una línea naranja marca la hora actual.

**Agendar una cita** (administrador y recepción)
1. Pulsa **Nueva cita** o haz clic en un espacio libre.
2. Busca al paciente por nombre o DNI y elígelo.
3. Elige el odontólogo, la fecha y la hora de inicio; el fin se calcula solo (30 minutos) y puedes cambiarlo. Escribe el motivo.
4. Pulsa **Agendar cita** (o Enter).

*(Captura: aviso de cruce de horario con los horarios libres)*

Si el odontólogo ya tiene otra cita a esa hora, Vyra avisa **"Cruce de horario"**, dice con quién se cruza y ofrece **horarios libres**: haz clic en uno para usarlo. El botón no se activa mientras haya cruce. No se pueden agendar citas en el pasado ni fuera del horario de atención.

**Estados de una cita**

| Estado | Qué significa |
|---|---|
| Pendiente | Agendada, aún sin confirmar con el paciente |
| Confirmada | El paciente confirmó su asistencia |
| Atendida | Ya se realizó |
| Cancelada | No se realizará (la cita no se borra: queda en el historial) |

Al hacer clic en una cita ves su detalle y los botones que corresponden: **Confirmar**, **Reprogramar**, **Cancelar** (pide confirmación) y **Marcar atendida**. El odontólogo solo ve sus citas y solo puede marcarlas como atendidas.

## 7. Historia clínica

*(Captura: pestaña Historia clínica de la ficha)*

Se abre desde **Pacientes → ficha del paciente → Historia clínica** (el ítem "Historia clínica" del menú te lleva a Pacientes para elegirlo).

- **Alergias y antecedentes:** el odontólogo los edita y pulsa **Guardar datos**.
- **Historial:** línea de tiempo con cada visita, de la más reciente a la más antigua. Para añadir una, pulsa **Nueva entrada**, escribe el diagnóstico y/o las observaciones, y **Guardar entrada**. Se vincula a la cita de hoy del paciente, si la hay.
- Las entradas **no se pueden editar ni borrar**: si hubo un error, se agrega una entrada nueva que lo aclare. Así el historial siempre es confiable.
- **Exportar PDF** guarda una copia para imprimir (ver §12).

## 8. Odontograma

*(Captura: odontograma con piezas de distintos estados)*

Muestra las 32 piezas en notación FDI: arriba el maxilar superior (18 a 11 y 21 a 28) y abajo la mandíbula inferior (48 a 41 y 31 a 38).

**Registrar el estado de las piezas** (odontólogo)
1. En **Marcar como**, elige un estado: Sana, Cariada (C), Obturada (O), En tratamiento (T) o Extraída (X).
2. Haz clic en las piezas que tengan ese estado. Cada cambio queda **pendiente**: la pieza se marca en naranja y el botón dice "Guardar N cambios".
3. En la tarjeta **Pieza seleccionada** puedes escribir una observación del cambio (por ejemplo, "Resina clase II").
4. Pulsa **Guardar N cambios**. Si sales sin guardar, Vyra te pregunta antes de descartar los cambios.

Cada cambio guardado queda registrado con su fecha y **nunca se sobrescribe**: el **Historial de la pieza** muestra todos los estados por los que pasó. **Registrar tratamiento para esta pieza** abre Tratamientos con la pieza ya escrita. El administrador solo puede consultar.

Para abrir el odontograma de un paciente, usa el botón con el diente en la lista de Pacientes, o elige al paciente con el buscador cuando entres desde el menú.

## 9. Tratamientos y pagos

### Tratamientos (odontólogo registra; los demás consultan)

*(Captura: pantalla de tratamientos con el formulario)*

1. Pulsa **Nuevo tratamiento**.
2. Elige la **cita** (aparecen las tuyas de hoy y de los últimos 30 días, sin las canceladas).
3. Escribe la **descripción**, las **piezas** separadas por coma (por ejemplo, `36, 47`; puedes dejarlas vacías) y el **costo**.
4. Pulsa **Registrar tratamiento**. Queda "En curso".

Al elegir un tratamiento de la lista ves su costo, lo pagado, el saldo y los pagos hechos. **Marcar como finalizado** lo cierra (aunque tenga saldo, que se sigue cobrando). Los filtros **En curso / Finalizados / Todos** y el buscador por paciente ayudan a ubicarlos.

### Pagos (administrador y recepción)

*(Captura: pantalla de pagos)*

Arriba, el selector **Hoy / Semana / mes** cambia las tarjetas: lo cobrado (con el porcentaje frente al periodo anterior), el saldo pendiente de todos los tratamientos y el reparto por método de pago. La tabla lista los pagos del periodo.

**Registrar un pago**
1. En el panel derecho, busca el tratamiento por paciente o DNI y elígelo. Verás su costo, lo pagado y el saldo.
2. Escribe el **monto** (ya viene el saldo completo; puedes cobrar solo una parte), la **fecha** (hoy por defecto, nunca futura) y el **método**: Efectivo, Yape / Plin, Tarjeta o Transferencia. El número de operación es opcional.
3. Abajo ves "Saldo después de este pago". Pulsa **Registrar pago de S/ …**.

Vyra no deja registrar un monto mayor al saldo pendiente ("El monto supera el saldo pendiente"). El saldo nunca se escribe a mano: siempre es el costo menos la suma de los pagos. También puedes cobrar desde la pestaña **Pagos** de la ficha del paciente, con el botón **Cobrar**.

## 10. Usuarios y roles, y Profesionales (administrador)

### Usuarios y roles
*(Captura: lista de usuarios con el formulario)*

Cada persona que use Vyra necesita su propio usuario.
1. **Nuevo usuario**: nombre completo, usuario (3 a 30 caracteres), **rol** y contraseña (mínimo 6).
2. Para cambiar la contraseña de alguien, elige al usuario, escribe la nueva y pulsa **Restablecer contraseña**.
3. Para quitar el acceso a alguien, desmarca **Usuario activo**. Los usuarios no se borran, para conservar el historial.

Reglas de seguridad: no puedes desactivarte ni cambiarte el rol a ti mismo, siempre debe quedar al menos un administrador activo, y no se quita el rol Odontólogo a quien tiene un profesional asociado.

### Profesionales
Registra a los odontólogos (nombres, apellidos, especialidad, teléfono y horario). Cada profesional tiene su columna en la agenda. Si le asocias un **usuario** con rol Odontólogo, esa persona verá solo sus propias citas. Un profesional con citas registradas no se puede eliminar.

## 11. Reportes (administrador)

*(Captura: reporte de citas)*

Hay tres tipos; el resultado se actualiza al cambiar cualquier filtro.
- **Citas:** elige las fechas (o **Hoy / Esta semana / Este mes**) y, si quieres, un profesional. Muestra el total, las atendidas, las canceladas y la asistencia (atendidas sobre las no canceladas), y la tabla de citas.
- **Historial de paciente:** busca al paciente y verás sus tratamientos con costo, pagado y saldo.
- **Ingresos:** total cobrado en el periodo, reparto por método de pago y barras por día.

## 12. Exportar a PDF

Desde el **Odontograma** y desde la pestaña **Historia clínica** hay un botón **Exportar PDF** (para odontólogo y administrador).
1. Pulsa el botón y elige dónde guardar el archivo (el nombre propuesto incluye el DNI del paciente).
2. Mientras se genera, el botón muestra un indicador de progreso; al terminar aparece "PDF guardado".
3. Abre el archivo con cualquier lector de PDF e imprímelo.

El PDF del odontograma refleja lo **ya guardado**: guarda los cambios pendientes antes de exportar. Estos documentos son confidenciales: no los compartas fuera del consultorio.

## 13. Preguntas frecuentes

**Olvidé mi contraseña.** Pídele al administrador que la restablezca en *Usuarios y roles*. Si se olvidó la del administrador y no hay otro administrador activo, hay que borrar `%APPDATA%\Vyra\vyra.db` (se pierden los datos), así que mantén siempre copias de seguridad.

**No veo una pantalla del menú.** Tu rol no la incluye (ver la tabla de la introducción). Consulta con el administrador.

**La cita que acabo de crear no aparece.** Revisa la fecha y el profesional elegidos y que la vista no esté filtrada a otro día; en la vista Lista aparecen todas.

**No me deja agendar: "Cruce de horario".** El odontólogo ya tiene una cita en ese horario. Elige uno de los horarios libres que sugiere Vyra.

**No puedo registrar el pago.** Revisa que el monto no supere el saldo y que la fecha no sea futura. Si el tratamiento ya está pagado por completo, no acepta más pagos.

**El PDF no se guarda.** Elige una carpeta en la que puedas escribir (por ejemplo, Documentos) y verifica que el archivo no esté abierto en otro programa.

**¿Cómo hago una copia de seguridad?** Cierra Vyra y copia el archivo `%APPDATA%\Vyra\vyra.db` a una memoria USB o a la nube. Para restaurarla, vuelve a poner el archivo en esa carpeta.

---

*Este manual, junto con las capturas reales, es también la base del anexo técnico para la memoria descriptiva ([`07-memoria-descriptiva.md`](./07-memoria-descriptiva.md)).*
