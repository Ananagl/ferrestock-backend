-- Nombre:                 01_esquema.sql
-- Descripción:            Crea el esquema completo de FerreStock en PostgreSQL: tablas, restricciones CHECK y UNIQUE,
--                         índices y triggers de inmutabilidad (kardex y auditoría). Fuente: docs/fase-2-diseno/02_modelo_de_datos.md
-- Fecha de creación:      2026-10-14
-- Fecha de actualización: 2026-10-14
-- Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
-- Versión:                0.1.0
--
-- Uso: se aplica una sola vez sobre una base vacía (ver crear_bd.sh). No borra datos existentes.

BEGIN;

-- ---------------------------------------------------------------------------
-- Usuarios y sesión persistente (RF-AUT)
-- ---------------------------------------------------------------------------
CREATE TABLE usuario (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_usuario        VARCHAR(50)  NOT NULL CHECK (nombre_usuario ~ '^[a-z0-9._-]{3,50}$'),
    nombre_completo       VARCHAR(120) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    rol                   VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'OPERATIVO')),
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos     SMALLINT     NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
    bloqueado_hasta       TIMESTAMPTZ,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT TRUE,
    idioma                VARCHAR(5)   NOT NULL DEFAULT 'es' CHECK (idioma IN ('es', 'en')),
    creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por_id         BIGINT REFERENCES usuario (id),
    actualizado_por_id    BIGINT REFERENCES usuario (id)
);
CREATE UNIQUE INDEX ux_usuario_nombre ON usuario (lower(nombre_usuario));

-- Tabla estándar de Spring Security para "Recordarme" (JdbcTokenRepositoryImpl)
CREATE TABLE persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);
CREATE INDEX ix_persistent_logins_usuario ON persistent_logins (username);

-- ---------------------------------------------------------------------------
-- Catálogo (RF-PRO, RF-PRE)
-- ---------------------------------------------------------------------------
CREATE TABLE categoria (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre             VARCHAR(60) NOT NULL CHECK (length(trim(nombre)) >= 2),
    activa             BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    creado_por_id      BIGINT REFERENCES usuario (id),
    actualizado_por_id BIGINT REFERENCES usuario (id)
);
CREATE UNIQUE INDEX ux_categoria_nombre ON categoria (lower(nombre));

CREATE TABLE producto (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo             VARCHAR(30)    NOT NULL CHECK (codigo ~ '^[A-Za-z0-9-]{2,30}$'),
    nombre             VARCHAR(120)   NOT NULL CHECK (length(trim(nombre)) >= 2),
    descripcion        VARCHAR(500),
    categoria_id       BIGINT         NOT NULL REFERENCES categoria (id),
    unidad_medida      VARCHAR(15)    NOT NULL CHECK (unidad_medida IN
                           ('UNIDAD', 'METRO', 'KILOGRAMO', 'LITRO', 'GALON', 'CAJA', 'PAQUETE', 'ROLLO')),
    permite_fraccion   BOOLEAN        NOT NULL DEFAULT FALSE,
    precio_venta       NUMERIC(14, 2) NOT NULL CHECK (precio_venta > 0),
    existencias        NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (existencias >= 0),
    costo_promedio     NUMERIC(16, 4) NOT NULL DEFAULT 0 CHECK (costo_promedio >= 0),
    estado             VARCHAR(10)    NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    version            BIGINT         NOT NULL DEFAULT 0,
    creado_en          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    creado_por_id      BIGINT REFERENCES usuario (id),
    actualizado_por_id BIGINT REFERENCES usuario (id)
);
CREATE UNIQUE INDEX ux_producto_codigo ON producto (upper(codigo));
CREATE INDEX ix_producto_categoria_estado ON producto (categoria_id, estado);
CREATE INDEX ix_producto_nombre ON producto (lower(nombre) varchar_pattern_ops);

-- ---------------------------------------------------------------------------
-- Terceros: clientes y proveedores (RF-TER)
-- ---------------------------------------------------------------------------
CREATE TABLE tercero (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_persona        VARCHAR(10)  NOT NULL CHECK (tipo_persona IN ('NATURAL', 'JURIDICA')),
    tipo_documento      VARCHAR(5)   NOT NULL CHECK (tipo_documento IN ('CC', 'CE', 'NIT')),
    numero_documento    VARCHAR(20)  NOT NULL CHECK (numero_documento ~ '^[0-9]{5,15}$'),
    digito_verificacion CHAR(1)      CHECK (digito_verificacion ~ '^[0-9]$'),
    nombre              VARCHAR(150) NOT NULL CHECK (length(trim(nombre)) >= 2),
    telefono            VARCHAR(20),
    correo              VARCHAR(120),
    direccion           VARCHAR(200),
    es_cliente          BOOLEAN      NOT NULL DEFAULT FALSE,
    es_proveedor        BOOLEAN      NOT NULL DEFAULT FALSE,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por_id       BIGINT REFERENCES usuario (id),
    actualizado_por_id  BIGINT REFERENCES usuario (id),
    CONSTRAINT uq_tercero_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_tercero_algun_rol CHECK (es_cliente OR es_proveedor),
    CONSTRAINT ck_tercero_juridica_nit CHECK (tipo_persona = 'NATURAL' OR tipo_documento = 'NIT'),
    CONSTRAINT ck_tercero_dv_nit CHECK ((tipo_documento = 'NIT') = (digito_verificacion IS NOT NULL))
);
CREATE INDEX ix_tercero_nombre ON tercero (lower(nombre) varchar_pattern_ops);
CREATE INDEX ix_tercero_clientes ON tercero (lower(nombre)) WHERE es_cliente;
CREATE INDEX ix_tercero_proveedores ON tercero (lower(nombre)) WHERE es_proveedor;

-- ---------------------------------------------------------------------------
-- Compras (RF-COM)
-- ---------------------------------------------------------------------------
CREATE TABLE compra (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consecutivo      VARCHAR(20)    NOT NULL UNIQUE,
    proveedor_id     BIGINT         NOT NULL REFERENCES tercero (id),
    fecha            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    observacion      VARCHAR(300),
    total            NUMERIC(14, 2) NOT NULL CHECK (total >= 0),
    estado           VARCHAR(10)    NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    motivo_anulacion VARCHAR(300),
    anulada_por_id   BIGINT REFERENCES usuario (id),
    anulada_en       TIMESTAMPTZ,
    creado_por_id    BIGINT         NOT NULL REFERENCES usuario (id),
    creado_en        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_compra_anulacion CHECK ((estado = 'ANULADA') = (motivo_anulacion IS NOT NULL))
);
CREATE INDEX ix_compra_proveedor_fecha ON compra (proveedor_id, fecha DESC);
CREATE INDEX ix_compra_fecha ON compra (fecha DESC);

CREATE TABLE compra_linea (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compra_id      BIGINT         NOT NULL REFERENCES compra (id),
    producto_id    BIGINT         NOT NULL REFERENCES producto (id),
    cantidad       NUMERIC(14, 3) NOT NULL CHECK (cantidad > 0),
    costo_unitario NUMERIC(16, 4) NOT NULL CHECK (costo_unitario >= 0),
    subtotal       NUMERIC(14, 2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT uq_compra_linea_producto UNIQUE (compra_id, producto_id)
);
CREATE INDEX ix_compra_linea_producto ON compra_linea (producto_id);

-- ---------------------------------------------------------------------------
-- Ventas con pago en efectivo (RF-VEN, RF-PAG; ADR-05)
-- ---------------------------------------------------------------------------
CREATE TABLE venta (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consecutivo      VARCHAR(20)    NOT NULL UNIQUE,
    cliente_id       BIGINT         NOT NULL REFERENCES tercero (id),
    fecha            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    total            NUMERIC(14, 2) NOT NULL CHECK (total > 0),
    medio_pago       VARCHAR(10)    NOT NULL DEFAULT 'EFECTIVO' CHECK (medio_pago = 'EFECTIVO'),
    valor_recibido   NUMERIC(14, 2) NOT NULL,
    cambio           NUMERIC(14, 2) NOT NULL,
    estado           VARCHAR(10)    NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    motivo_anulacion VARCHAR(300),
    anulada_por_id   BIGINT REFERENCES usuario (id),
    anulada_en       TIMESTAMPTZ,
    creado_por_id    BIGINT         NOT NULL REFERENCES usuario (id),
    creado_en        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_venta_recibido CHECK (valor_recibido >= total),
    CONSTRAINT ck_venta_cambio CHECK (cambio = valor_recibido - total),
    CONSTRAINT ck_venta_anulacion CHECK ((estado = 'ANULADA') = (motivo_anulacion IS NOT NULL))
);
CREATE INDEX ix_venta_cliente_fecha ON venta (cliente_id, fecha DESC);
CREATE INDEX ix_venta_fecha ON venta (fecha DESC);

CREATE TABLE venta_linea (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venta_id          BIGINT         NOT NULL REFERENCES venta (id),
    producto_id       BIGINT         NOT NULL REFERENCES producto (id),
    cantidad          NUMERIC(14, 3) NOT NULL CHECK (cantidad > 0),
    precio_unitario   NUMERIC(14, 2) NOT NULL CHECK (precio_unitario > 0),
    costo_unitario    NUMERIC(16, 4) NOT NULL CHECK (costo_unitario >= 0),
    subtotal          NUMERIC(14, 2) NOT NULL CHECK (subtotal > 0),
    cantidad_devuelta NUMERIC(14, 3) NOT NULL DEFAULT 0,
    CONSTRAINT uq_venta_linea_producto UNIQUE (venta_id, producto_id),
    CONSTRAINT ck_venta_linea_devuelta CHECK (cantidad_devuelta BETWEEN 0 AND cantidad)
);
CREATE INDEX ix_venta_linea_producto ON venta_linea (producto_id);

-- ---------------------------------------------------------------------------
-- Devoluciones de clientes (RF-DEV)
-- ---------------------------------------------------------------------------
CREATE TABLE devolucion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consecutivo     VARCHAR(20)  NOT NULL UNIQUE,
    cliente_id      BIGINT       NOT NULL REFERENCES tercero (id),
    venta_id        BIGINT REFERENCES venta (id),
    motivo          VARCHAR(300) NOT NULL CHECK (length(trim(motivo)) >= 5),
    fecha           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    validada_por_id BIGINT       NOT NULL REFERENCES usuario (id),
    creado_por_id   BIGINT       NOT NULL REFERENCES usuario (id),
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_devolucion_cliente_fecha ON devolucion (cliente_id, fecha DESC);
CREATE INDEX ix_devolucion_venta ON devolucion (venta_id);

CREATE TABLE devolucion_linea (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    devolucion_id  BIGINT         NOT NULL REFERENCES devolucion (id),
    producto_id    BIGINT         NOT NULL REFERENCES producto (id),
    venta_linea_id BIGINT REFERENCES venta_linea (id),
    cantidad       NUMERIC(14, 3) NOT NULL CHECK (cantidad > 0),
    resultado      VARCHAR(10)    NOT NULL CHECK (resultado IN ('ACEPTADA', 'RECHAZADA')),
    costo_unitario NUMERIC(16, 4) CHECK (costo_unitario >= 0),
    CONSTRAINT ck_devolucion_linea_costo CHECK ((resultado = 'ACEPTADA') = (costo_unitario IS NOT NULL))
);

-- ---------------------------------------------------------------------------
-- Ajustes de inventario, solo ADMIN (RF-AJU)
-- ---------------------------------------------------------------------------
CREATE TABLE ajuste (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consecutivo    VARCHAR(20)    NOT NULL UNIQUE,
    producto_id    BIGINT         NOT NULL REFERENCES producto (id),
    tipo           VARCHAR(10)    NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    cantidad       NUMERIC(14, 3) NOT NULL CHECK (cantidad > 0),
    costo_unitario NUMERIC(16, 4) NOT NULL CHECK (costo_unitario >= 0),
    motivo         VARCHAR(300)   NOT NULL CHECK (length(trim(motivo)) >= 5),
    fecha          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    creado_por_id  BIGINT         NOT NULL REFERENCES usuario (id),
    creado_en      TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_ajuste_producto_fecha ON ajuste (producto_id, fecha DESC);

-- ---------------------------------------------------------------------------
-- Kardex: movimientos de inventario inmutables (RF-INV-002, RF-INV-003)
-- ---------------------------------------------------------------------------
CREATE TABLE movimiento_inventario (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_id               BIGINT         NOT NULL REFERENCES producto (id),
    tipo                      VARCHAR(25)    NOT NULL CHECK (tipo IN ('COMPRA', 'VENTA', 'DEVOLUCION',
                                  'AJUSTE_ENTRADA', 'AJUSTE_SALIDA', 'ANULACION_COMPRA', 'ANULACION_VENTA')),
    naturaleza                VARCHAR(7)     NOT NULL CHECK (naturaleza IN ('ENTRADA', 'SALIDA')),
    cantidad                  NUMERIC(14, 3) NOT NULL CHECK (cantidad > 0),
    costo_unitario            NUMERIC(16, 4) NOT NULL CHECK (costo_unitario >= 0),
    saldo_resultante          NUMERIC(14, 3) NOT NULL CHECK (saldo_resultante >= 0),
    costo_promedio_anterior   NUMERIC(16, 4) NOT NULL CHECK (costo_promedio_anterior >= 0),
    costo_promedio_resultante NUMERIC(16, 4) NOT NULL CHECK (costo_promedio_resultante >= 0),
    documento_tipo            VARCHAR(12)    NOT NULL CHECK (documento_tipo IN ('COMPRA', 'VENTA', 'DEVOLUCION', 'AJUSTE')),
    documento_id              BIGINT         NOT NULL,
    usuario_id                BIGINT         NOT NULL REFERENCES usuario (id),
    fecha                     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_movimiento_producto ON movimiento_inventario (producto_id, id DESC);
CREATE INDEX ix_movimiento_documento ON movimiento_inventario (documento_tipo, documento_id);

-- ---------------------------------------------------------------------------
-- Consecutivos de documentos sin huecos (RF-DOC-001; ADR-09)
-- ---------------------------------------------------------------------------
CREATE TABLE consecutivo (
    tipo_documento VARCHAR(12) PRIMARY KEY CHECK (tipo_documento IN ('COMPRA', 'VENTA', 'DEVOLUCION', 'AJUSTE')),
    prefijo        VARCHAR(5)  NOT NULL UNIQUE,
    siguiente      BIGINT      NOT NULL DEFAULT 1 CHECK (siguiente >= 1)
);

-- ---------------------------------------------------------------------------
-- Auditoría de solo inserción (RF-AUD; RN-11)
-- ---------------------------------------------------------------------------
CREATE TABLE auditoria (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     BIGINT REFERENCES usuario (id),
    nombre_usuario VARCHAR(50)  NOT NULL,
    fecha          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    accion         VARCHAR(40)  NOT NULL,
    descripcion    VARCHAR(500) NOT NULL
);
CREATE INDEX ix_auditoria_fecha ON auditoria (fecha DESC);
CREATE INDEX ix_auditoria_usuario_fecha ON auditoria (usuario_id, fecha DESC);

-- ---------------------------------------------------------------------------
-- Registro de errores de la aplicación (RF-MON-006)
-- ---------------------------------------------------------------------------
CREATE TABLE registro_error (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correlacion UUID         NOT NULL UNIQUE,
    fecha       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ruta        VARCHAR(200),
    metodo      VARCHAR(7),
    tipo        VARCHAR(150) NOT NULL,
    mensaje     VARCHAR(500),
    usuario_id  BIGINT REFERENCES usuario (id),
    taiga_ref   VARCHAR(30)
);
CREATE INDEX ix_registro_error_fecha ON registro_error (fecha DESC);

-- ---------------------------------------------------------------------------
-- Inmutabilidad del kardex y la auditoría (ADR-15; RF-AUD-003)
-- ---------------------------------------------------------------------------
CREATE FUNCTION fs_rechazar_modificacion() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'La tabla % es de solo inserción: no se permite %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER tg_movimiento_inmutable
    BEFORE UPDATE OR DELETE ON movimiento_inventario
    FOR EACH ROW EXECUTE FUNCTION fs_rechazar_modificacion();

CREATE TRIGGER tg_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fs_rechazar_modificacion();

COMMIT;
