# Vyra
## Documento de Requerimientos — Sistema de Gestión de Citas y Expedientes (Consultorio Dental)

**Versión:** 0.2
**Fecha:** 2026-10-29
**Equipo:** Daniel, Evelyn, Smith y Johan (ver sección 1.1)
**Repositorio:** `vyra`

---

## 1. Descripción general

**Vyra** es una aplicación de escritorio desarrollada en JavaFX, de uso **interno** para el personal de un consultorio dental (recepcionista y odontólogo/a), que permite gestionar pacientes, citas, historias clínicas, odontogramas y tratamientos. Reemplaza el registro manual en papel o en hojas de cálculo que actualmente usan la mayoría de consultorios pequeños.

### 1.1 Equipo de trabajo

| Nombre | Módulo(s) a cargo |
|---|---|
| Daniel | _(por definir)_ |
| Evelyn | _(por definir)_ |
| Smith | _(por definir)_ |
| Johan | _(por definir)_ |

> Los módulos se reparten según la sección 9 de [`05-arquitectura.md`](./05-arquitectura.md#9-división-de-trabajo-sugerida-para-el-equipo). Esta tabla también sirve como matriz de responsabilidades para la sustentación del curso y, más adelante, para dejar claro quiénes son los coautores si el proyecto se registra en INDECOPI (el registro de derecho de autor permite, y pide, listar a todos los autores reales de la obra).

## 2. Alcance

### 2.1 Incluye (versión 1)
- Gestión de pacientes (datos personales y de contacto).
- Gestión de citas (agendamiento interno, no agendamiento remoto por el paciente).
- Historia clínica general por paciente.
- Odontograma por paciente, con historial de cambios por fecha.
- Registro de tratamientos asociados a piezas dentales y citas.
- Gestión de profesionales (odontólogos).
- Control de acceso con usuario, contraseña y roles.
- Reportes básicos (citas del día, historial de un paciente, tratamientos por periodo).
- Registro simple de pagos por tratamiento/cita.

### 2.2 No incluye (fuera de alcance por ahora)
- Agendamiento de citas por parte del paciente (portal web o app móvil).
- Facturación electrónica integrada con SUNAT.
- Recordatorios automáticos por SMS.
- Telemedicina o videoconsulta.

Estas exclusiones pueden convertirse en una "versión 2" del proyecto más adelante; se documentan aquí para dejar explícito qué NO se va a evaluar en esta primera versión.

## 3. Actores del sistema

| Actor | Descripción |
|---|---|
| Administrador | Gestiona usuarios, profesionales y tiene acceso a todos los reportes. Puede ser el mismo dueño del consultorio. |
| Recepcionista | Registra pacientes, agenda citas, registra pagos. No edita historias clínicas ni odontogramas. |
| Odontólogo/a | Atiende citas, registra diagnósticos, actualiza historia clínica, odontograma y tratamientos. |

## 4. Requerimientos funcionales (RF)

| Código | Descripción | Actor principal | Prioridad |
|---|---|---|---|
| RF-01 | El sistema debe permitir registrar, editar, buscar y dar de baja pacientes (nombres, DNI, fecha de nacimiento, teléfono, dirección, contacto de emergencia, alergias). "Dar de baja" es una baja lógica: el paciente deja de aparecer en las búsquedas, pero su historial se conserva. | Recepcionista | Alta |
| RF-02 | El sistema debe permitir registrar, reprogramar y cancelar citas (fecha, hora, paciente, odontólogo, motivo, estado). | Recepcionista | Alta |
| RF-03 | El sistema debe impedir que un mismo odontólogo tenga dos citas en el mismo horario (validación de cruce). | Sistema | Alta |
| RF-04 | El sistema debe permitir crear y actualizar la historia clínica del paciente (antecedentes médicos, alergias, observaciones). | Odontólogo/a | Alta |
| RF-05 | El sistema debe permitir registrar y visualizar el odontograma del paciente, marcando el estado de cada pieza dental (sana, cariada, obturada, extraída, en tratamiento) con fecha de registro. | Odontólogo/a | Alta |
| RF-06 | El sistema debe permitir registrar tratamientos realizados, asociados a una cita y a una o más piezas dentales, con costo y estado (en curso / finalizado). | Odontólogo/a | Alta |
| RF-07 | El sistema debe permitir gestionar los datos de los odontólogos (especialidad, horario de atención). | Administrador | Media |
| RF-08 | El sistema debe controlar el acceso mediante usuario y contraseña, con al menos los roles Administrador, Recepcionista y Odontólogo, cada uno con permisos distintos sobre las pantallas. | Administrador | Alta |
| RF-09 | El sistema debe generar reportes de: citas del día/semana, historial de tratamientos de un paciente, e ingresos por periodo. | Administrador | Media |
| RF-10 | El sistema debe permitir registrar pagos asociados a un tratamiento o cita (monto, fecha, método de pago). | Recepcionista | Media |
| RF-11 | El sistema debe permitir exportar la historia clínica o el odontograma de un paciente en PDF para impresión. | Odontólogo/a | Baja |
| RF-12 | El sistema debe mostrar una alerta interna (no necesariamente correo) de las citas próximas del día al iniciar sesión. | Recepcionista | Baja |


## 5. Requerimientos no funcionales (RNF)

| Código | Descripción |
|---|---|
| RNF-01 | La aplicación debe ser de escritorio, desarrollada en JavaFX, ejecutable en Windows. |
| RNF-02 | Los datos deben persistir en una base de datos relacional (SQLite para esta versión, con diseño que permita migrar a MySQL/PostgreSQL a futuro). |
| RNF-03 | La interfaz debe ser simple e intuitiva para personal no técnico (recepcionista), con validación de formularios y mensajes de error claros. |
| RNF-04 | Las operaciones CRUD deben responder en menos de 2 segundos en condiciones normales de uso local. |
| RNF-05 | Las contraseñas de usuario deben almacenarse cifradas (hash), nunca en texto plano. |
| RNF-06 | El código debe seguir principios de POO, separando el proyecto en capas (modelo, acceso a datos/DAO, lógica de negocio, interfaz), tanto por buena práctica como para sustentarlo académicamente. |
| RNF-07 | El proyecto debe mantenerse bajo control de versiones en Git desde el inicio, con commits descriptivos. |

## 6. Reglas de negocio (RN)

Son las reglas concretas que el código debe hacer cumplir. Cada una indica en qué capa se valida, para que no se dupliquen ni se olviden.

| Código | Regla | Dónde se valida |
|---|---|---|
| RN-01 | El DNI de un paciente es obligatorio, tiene exactamente 8 dígitos y no puede repetirse. | Modelo (`@Pattern`) + `PacienteService` (duplicado) + `UNIQUE` en BD |
| RN-02 | El teléfono, si se ingresa, tiene exactamente 9 dígitos. | Modelo (`@Pattern`) |
| RN-03 | La fecha de nacimiento debe ser anterior a hoy. | Modelo (`@Past`) |
| RN-04 | Dar de baja a un paciente no lo borra: se marca `activo = 0`. | `PacienteService` |
| RN-05 | Una cita debe tener `horaFin` posterior a `horaInicio`. Los horarios van en bloques de 30 minutos entre 08:00 y 20:00. | `CitaService` |
| RN-06 | Un odontólogo no puede tener dos citas que se traslapen el mismo día. Solo cuentan las citas en estado `PENDIENTE` o `CONFIRMADA` (las canceladas y atendidas liberan el horario). | `CitaService.existeCruceDeHorario()` (CU-03) |
| RN-07 | Una cita nunca se elimina: se cancela (`CANCELADA`) para mantener el historial. | `CitaService` |
| RN-08 | No se puede agendar una cita en una fecha pasada ni, si es hoy, en una hora que ya pasó. | `CitaService` |
| RN-09 | Las entradas de historia clínica y los cambios del odontograma nunca se sobrescriben ni se borran: cada cambio es una fila nueva con su fecha. | `HistoriaClinicaService`, `OdontogramaService` |
| RN-10 | El número de pieza dental usa la notación FDI: cuadrantes 1–4 y posiciones 1–8 (11–18, 21–28, 31–38, 41–48). Las piezas temporales (51–85) quedan para una versión futura. | Modelo + `OdontogramaService` |
| RN-11 | El costo de un tratamiento debe ser mayor a 0. | Modelo (`@Positive`) |
| RN-12 | Un pago debe tener monto mayor a 0, fecha no futura, y no puede superar el saldo pendiente del tratamiento (saldo = costo − suma de pagos). | Modelo + `PagoService` |
| RN-13 | La contraseña se guarda como hash con sal (PBKDF2), nunca en texto plano. El mensaje de error del login no dice cuál dato falló. | `util.HashUtil` + `AuthService` |
| RN-14 | Un usuario desactivado (`activo = 0`) no puede iniciar sesión. | `AuthService` |
| RN-15 | Cada rol solo ve las pantallas que le corresponden (ver la matriz de permisos en [`02-casos-de-uso.md`](./02-casos-de-uso.md#3-matriz-de-permisos-por-rol)). | `MainController` (menú) + cada Service (defensa en profundidad) |

## 7. Glosario

- **Odontograma:** representación gráfica de las piezas dentales de un paciente, usada para registrar su estado (sano, cariado, tratado, ausente, etc.) a lo largo del tiempo.
- **Historia clínica / expediente:** conjunto de datos médicos y antecedentes de un paciente, acumulado a lo largo de sus visitas.
- **Tratamiento:** procedimiento realizado a un paciente (ej. limpieza, extracción, obturación), asociado a una o más piezas dentales.

---


