#!/usr/bin/env bash
# Nombre:                 verificar_encabezados.sh
# Descripción:            Verifica que cada archivo fuente tenga el encabezado obligatorio (RNF-MAN-02); falla si falta algún campo.
# Fecha de creación:      2026-10-14
# Fecha de actualización: 2026-10-14
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.1.0
set -euo pipefail
cd "$(dirname "$0")/.."
campos=("Nombre:" "Descripción:" "Fecha de creación:" "Fecha de actualización:" "Autores:" "Versión:")
fallos=0
while IFS= read -r archivo; do
  cabeza="$(head -n 15 "$archivo")"
  for campo in "${campos[@]}"; do
    if ! grep -q "$campo" <<<"$cabeza"; then
      echo "Falta '$campo' en $archivo"; fallos=$((fallos + 1))
    fi
  done
done < <(git ls-files --cached --others --exclude-standard -- '*.java' '*.sql' '*.sh' '*.yml' '*.yaml' '*.properties' 'pom.xml' \
         | grep -v -E '^(\.mvn/|mvnw)')
if [[ $fallos -gt 0 ]]; then echo "Encabezados incompletos: $fallos"; exit 1; fi
echo "Encabezados completos."
