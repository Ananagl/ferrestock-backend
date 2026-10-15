#!/usr/bin/env bash
# Nombre:                 inicializar_remota.sh
# Descripción:            Inicializa una base PostgreSQL ya creada por el proveedor (Neon, Render…): esquema, datos iniciales y
#                         usuario admin con clave temporal. La URL y la clave se escriben sin mostrarse y no quedan en archivos.
# Fecha de creación:      2026-10-15
# Fecha de actualización: 2026-10-15
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.13.0
#
# Uso: ./db/inicializar_remota.sh   (pide la URL postgresql://usuario:clave@host/base?sslmode=require)
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
read -rsp "URL de conexión de PostgreSQL (postgresql://...): " URL; echo
read -rsp "Clave temporal del usuario admin (mín. 8 caracteres): " CLAVE; echo
[[ ${#CLAVE} -ge 8 ]] || { echo "La clave debe tener al menos 8 caracteres." >&2; exit 1; }
if [[ "$(psql "$URL" -Atc "SELECT to_regclass('public.usuario') IS NOT NULL")" == "t" ]]; then
  echo "La base ya tiene el esquema de FerreStock. No se modifica nada." >&2; exit 1
fi
psql "$URL" -v ON_ERROR_STOP=1 -q -c "CREATE EXTENSION IF NOT EXISTS pgcrypto"
psql "$URL" -v ON_ERROR_STOP=1 -q -f "$DIR/01_esquema.sql"
psql "$URL" -v ON_ERROR_STOP=1 -q -f "$DIR/02_datos_iniciales.sql"
psql "$URL" -v ON_ERROR_STOP=1 -q -v clave="$CLAVE" <<'SQL'
INSERT INTO usuario (nombre_usuario, nombre_completo, password_hash, rol, debe_cambiar_password)
VALUES ('admin', 'Administrador', crypt(:'clave', gen_salt('bf', 12)), 'ADMIN', TRUE) ON CONFLICT DO NOTHING;
SQL
unset URL CLAVE
echo "Base inicializada. Ingrese con 'admin' y la clave temporal; el sistema pedirá cambiarla."
