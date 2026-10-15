#!/usr/bin/env bash
# Nombre:                 mutacion.sh
# Descripción:            Ejecuta las pruebas de mutación con PIT; informe en target/pit-reports/index.html.
# Fecha de creación:      2026-10-14
# Fecha de actualización: 2026-10-14
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.1.0
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -B -ntp test-compile org.pitest:pitest-maven:mutationCoverage "$@"
