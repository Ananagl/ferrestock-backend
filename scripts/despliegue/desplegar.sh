#!/usr/bin/env bash
# Nombre:                 desplegar.sh
# Descripción:            Construye y publica la imagen Docker en GHCR, dispara el despliegue en la plataforma de demostración
#                         (hook) y espera a que responda la URL de salud (RF-MON-002). Sin hook, solo publica la imagen.
# Fecha de creación:      2026-10-15
# Fecha de actualización: 2026-10-15
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.12.0
#
# Variables: IMAGEN (ghcr.io/usuario/repo:sha), GHCR_TOKEN; opcionales FS_DEPLOY_HOOK y FS_URL_SALUD.
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${IMAGEN:?}"
docker build -t "$IMAGEN" .
if [[ -n "${GHCR_TOKEN:-}" ]]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "${GITHUB_ACTOR:-ci}" --password-stdin
  docker push "$IMAGEN"
fi
if [[ -z "${FS_DEPLOY_HOOK:-}" ]]; then
  echo "Sin FS_DEPLOY_HOOK: la imagen quedó publicada y no se desplegó (DP-14)."
  exit 0
fi
curl -sf -X POST "$FS_DEPLOY_HOOK" >/dev/null
echo "Despliegue solicitado."
if [[ -n "${FS_URL_SALUD:-}" ]]; then
  for intento in $(seq 1 30); do
    if curl -sf "$FS_URL_SALUD" >/dev/null; then echo "Salud verificada (intento $intento)."; exit 0; fi
    sleep 10
  done
  echo "La aplicación no respondió en $FS_URL_SALUD" >&2
  exit 1
fi
