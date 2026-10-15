#!/usr/bin/env bash
# Nombre:                 compilar.sh
# Descripción:            Compila el backend y genera el JAR sin ejecutar pruebas.
# Fecha de creación:      2026-10-14
# Fecha de actualización: 2026-10-14
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.1.0
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -B -ntp clean package -DskipTests
