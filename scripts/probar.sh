#!/usr/bin/env bash
# Nombre:                 probar.sh
# Descripción:            Ejecuta pruebas unitarias e integración (requiere Docker para PostgreSQL de prueba) y genera cobertura JaCoCo en target/site/jacoco.
# Fecha de creación:      2026-10-14
# Fecha de actualización: 2026-10-14
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.1.0
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -B -ntp clean verify "$@"
