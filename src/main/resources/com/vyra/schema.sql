-- Script de creacion de tablas de Vyra (SQLite)
-- Copia de docs/03-modelo-base-datos.md, seccion 3. Si cambias uno, cambia el otro.
-- Usa "IF NOT EXISTS" para poder ejecutarse cada vez que arranca la app
-- sin borrar datos existentes. Si cambias una tabla, borra vyra.db.
-- ConexionBD separa las sentencias por punto y coma: no lo uses en comentarios ni textos.

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_usuario TEXT NOT NULL UNIQUE,
    nombre_completo TEXT NOT NULL,
    contrasena_hash TEXT NOT NULL,
    rol TEXT NOT NULL CHECK (rol IN ('ADMIN','RECEPCION','ODONTOLOGO')),
    activo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS profesional (
    id_profesional INTEGER PRIMARY KEY AUTOINCREMENT,
    id_usuario INTEGER REFERENCES usuario(id_usuario),
    nombres TEXT NOT NULL,
    apellidos TEXT NOT NULL,
    especialidad TEXT,
    telefono TEXT,
    horario_atencion TEXT
);

CREATE TABLE IF NOT EXISTS paciente (
    id_paciente INTEGER PRIMARY KEY AUTOINCREMENT,
    nombres TEXT NOT NULL,
    apellidos TEXT NOT NULL,
    dni TEXT NOT NULL UNIQUE,
    fecha_nacimiento TEXT,
    telefono TEXT,
    direccion TEXT,
    contacto_emergencia TEXT,
    alergias TEXT,
    antecedentes TEXT,
    activo INTEGER NOT NULL DEFAULT 1,
    fecha_registro TEXT DEFAULT (date('now'))
);

CREATE TABLE IF NOT EXISTS cita (
    id_cita INTEGER PRIMARY KEY AUTOINCREMENT,
    id_paciente INTEGER NOT NULL REFERENCES paciente(id_paciente),
    id_profesional INTEGER NOT NULL REFERENCES profesional(id_profesional),
    fecha TEXT NOT NULL,
    hora_inicio TEXT NOT NULL,
    hora_fin TEXT NOT NULL,
    motivo TEXT,
    estado TEXT NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE','CONFIRMADA','ATENDIDA','CANCELADA')),
    fecha_creacion TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS historia_clinica_entrada (
    id_entrada INTEGER PRIMARY KEY AUTOINCREMENT,
    id_paciente INTEGER NOT NULL REFERENCES paciente(id_paciente),
    id_cita INTEGER REFERENCES cita(id_cita),
    fecha TEXT NOT NULL,
    observaciones TEXT,
    diagnostico TEXT
);

CREATE TABLE IF NOT EXISTS odontograma_pieza (
    id_registro INTEGER PRIMARY KEY AUTOINCREMENT,
    id_paciente INTEGER NOT NULL REFERENCES paciente(id_paciente),
    numero_pieza INTEGER NOT NULL
        CHECK (numero_pieza / 10 BETWEEN 1 AND 4 AND numero_pieza % 10 BETWEEN 1 AND 8),
    estado TEXT NOT NULL
        CHECK (estado IN ('SANA','CARIADA','OBTURADA','EXTRAIDA','EN_TRATAMIENTO')),
    fecha_registro TEXT NOT NULL,
    observacion TEXT
);

CREATE TABLE IF NOT EXISTS tratamiento (
    id_tratamiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_cita INTEGER NOT NULL REFERENCES cita(id_cita),
    descripcion TEXT NOT NULL,
    costo REAL NOT NULL CHECK (costo > 0),
    estado TEXT NOT NULL DEFAULT 'EN_CURSO' CHECK (estado IN ('EN_CURSO','FINALIZADO')),
    fecha TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS tratamiento_pieza (
    id_tratamiento INTEGER NOT NULL REFERENCES tratamiento(id_tratamiento),
    numero_pieza INTEGER NOT NULL,
    PRIMARY KEY (id_tratamiento, numero_pieza)
);

CREATE TABLE IF NOT EXISTS pago (
    id_pago INTEGER PRIMARY KEY AUTOINCREMENT,
    id_tratamiento INTEGER NOT NULL REFERENCES tratamiento(id_tratamiento),
    monto REAL NOT NULL CHECK (monto > 0),
    fecha_pago TEXT NOT NULL,
    metodo_pago TEXT NOT NULL
        CHECK (metodo_pago IN ('EFECTIVO','YAPE_PLIN','TARJETA','TRANSFERENCIA')),
    numero_operacion TEXT
);

CREATE INDEX IF NOT EXISTS idx_cita_profesional_fecha ON cita(id_profesional, fecha);
CREATE INDEX IF NOT EXISTS idx_cita_fecha ON cita(fecha);
CREATE INDEX IF NOT EXISTS idx_cita_paciente ON cita(id_paciente);
CREATE INDEX IF NOT EXISTS idx_historia_paciente ON historia_clinica_entrada(id_paciente);
CREATE INDEX IF NOT EXISTS idx_odontograma_paciente_pieza ON odontograma_pieza(id_paciente, numero_pieza);
CREATE INDEX IF NOT EXISTS idx_tratamiento_cita ON tratamiento(id_cita);
CREATE INDEX IF NOT EXISTS idx_pago_tratamiento ON pago(id_tratamiento);
CREATE INDEX IF NOT EXISTS idx_pago_fecha ON pago(fecha_pago);
