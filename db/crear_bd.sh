#!/usr/bin/env bash
# Nombre:                 crear_bd.sh
# Descripción:            Crea la base de datos y el rol de aplicación de FerreStock, aplica el esquema y los datos
#                         iniciales, y registra el usuario administrador inicial con una clave tomada del entorno.
# Fecha de creación:      2026-10-14
# Fecha de actualización: 2026-10-14
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.1.0
#
# Uso:
#   FS_DB_CLAVE='...' FS_ADMIN_CLAVE_INICIAL='...' ./db/crear_bd.sh
# Variables opcionales: PGHOST (localhost), PGPORT (5432), PG_SUPERUSUARIO (usuario actual),
#                       FS_DB_NOMBRE (ferrestock), FS_DB_USUARIO (ferrestock_app), FS_DEMO=1 para cargar datos demo.
# El script NO borra nada: si la base ya existe, se detiene.

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PG_SUPERUSUARIO="${PG_SUPERUSUARIO:-$(whoami)}"
FS_DB_NOMBRE="${FS_DB_NOMBRE:-ferrestock}"
FS_DB_USUARIO="${FS_DB_USUARIO:-ferrestock_app}"
: "${FS_DB_CLAVE:?Defina FS_DB_CLAVE (clave del rol de aplicación)}"
: "${FS_ADMIN_CLAVE_INICIAL:?Defina FS_ADMIN_CLAVE_INICIAL (clave temporal del usuario admin)}"

if [[ ${#FS_ADMIN_CLAVE_INICIAL} -lt 8 ]]; then
  echo "FS_ADMIN_CLAVE_INICIAL debe tener al menos 8 caracteres." >&2
  exit 1
fi

# Ejecuta psql como superusuario sobre la base indicada en $1
psql_super() { psql -h "$PGHOST" -p "$PGPORT" -U "$PG_SUPERUSUARIO" -v ON_ERROR_STOP=1 -q -d "$1" "${@:2}"; }
# Ejecuta psql como rol de aplicación, para que las tablas le pertenezcan
psql_app() { PGPASSWORD="$FS_DB_CLAVE" psql -h "$PGHOST" -p "$PGPORT" -U "$FS_DB_USUARIO" -v ON_ERROR_STOP=1 -q -d "$FS_DB_NOMBRE" "$@"; }

existe_bd=$(psql_super postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '$FS_DB_NOMBRE'")
if [[ "$existe_bd" == "1" ]]; then
  echo "La base '$FS_DB_NOMBRE' ya existe en $PGHOST:$PGPORT. No se modifica nada." >&2
  exit 1
fi

echo "1/5 Creando rol '$FS_DB_USUARIO' (si no existe)..."
psql_super postgres -v usuario="$FS_DB_USUARIO" -v clave="$FS_DB_CLAVE" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'usuario', :'clave')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'usuario') \gexec
SQL

echo "2/5 Creando base '$FS_DB_NOMBRE'..."
psql_super postgres -c "CREATE DATABASE \"$FS_DB_NOMBRE\" OWNER \"$FS_DB_USUARIO\" ENCODING 'UTF8' TEMPLATE template0"
psql_super "$FS_DB_NOMBRE" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto"

echo "3/5 Aplicando esquema..."
psql_app -f "$DIR/01_esquema.sql"

echo "4/5 Cargando datos iniciales..."
psql_app -f "$DIR/02_datos_iniciales.sql"

echo "5/5 Creando usuario administrador inicial (debe cambiar la clave al ingresar)..."
psql_app -v clave="$FS_ADMIN_CLAVE_INICIAL" <<'SQL'
INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, debe_cambiar_password)
VALUES ('admin', 'Administrador', crypt(:'clave', gen_salt('bf', 12)), 'ADMIN', TRUE)
ON CONFLICT DO NOTHING;
SQL

if [[ "${FS_DEMO:-0}" == "1" && -f "$DIR/90_datos_demo.sql" ]]; then
  echo "Cargando datos de demostración..."
  psql_app -f "$DIR/90_datos_demo.sql"
fi

echo "Listo: base '$FS_DB_NOMBRE' en $PGHOST:$PGPORT, rol '$FS_DB_USUARIO', usuario 'admin'."
