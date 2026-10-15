#!/usr/bin/env bash
# Nombre:                 taiga_reportar.sh
# Descripción:            Reporta el resultado de un job de CI en Taiga: si falla crea (o comenta) un issue y una tarea en la historia
#                         "Fallos de CI" y los asigna al autor del commit; si pasa, comenta y cierra lo que estaba abierto (RF-MON-003, 004).
# Fecha de creación:      2026-10-15
# Fecha de actualización: 2026-10-15
# Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
# Versión:                0.12.0
#
# Uso: taiga_reportar.sh fallo|exito
# Variables: TAIGA_USUARIO, TAIGA_CLAVE (secretos), TAIGA_PROYECTO_ID, REPO, RAMA, COMMIT, JOB, AUTOR_GITHUB, URL_LOG
# Opcionales: TAIGA_URL (https://api.taiga.io), TAIGA_MIEMBROS (.taiga/miembros.json), TAIGA_ASIGNADO_POR_DEFECTO,
#             DETALLE (últimas líneas del error), TAIGA_SIMULAR=1 (muestra lo que haría sin llamar a Taiga)
set -euo pipefail

RESULTADO="${1:?Indique fallo o exito}"
API="${TAIGA_URL:-https://api.taiga.io}/api/v1"
: "${TAIGA_PROYECTO_ID:?}" "${REPO:?}" "${RAMA:?}" "${COMMIT:?}" "${JOB:?}"
AUTOR_GITHUB="${AUTOR_GITHUB:-desconocido}"
URL_LOG="${URL_LOG:-}"
MIEMBROS="${TAIGA_MIEMBROS:-.taiga/miembros.json}"
ASUNTO="[CI] ${REPO} · ${JOB} falló en ${RAMA}"
PREFIJO="[CI] ${REPO} · ${JOB} "
HISTORIA="Fallos de CI"

if [[ "${TAIGA_SIMULAR:-0}" == "1" ]]; then
  echo "SIMULACIÓN: resultado=${RESULTADO} asunto='${ASUNTO}' autor=${AUTOR_GITHUB} log=${URL_LOG}"
  exit 0
fi
: "${TAIGA_USUARIO:?}" "${TAIGA_CLAVE:?}"

TOKEN=$(curl -sf -X POST "$API/auth" -H 'Content-Type: application/json' \
  -d "$(jq -n --arg u "$TAIGA_USUARIO" --arg p "$TAIGA_CLAVE" '{type: "normal", username: $u, password: $p}')" | jq -r '.auth_token')
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || { echo "No fue posible autenticarse en Taiga" >&2; exit 1; }

taiga() { # método ruta [cuerpo]
  local args=(-sf -X "$1" "$API$2" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'x-disable-pagination: True')
  [[ $# -ge 3 ]] && args+=(-d "$3")
  curl "${args[@]}"
}

# Responsable: el integrante de Taiga que corresponde al autor del commit en GitHub
ASIGNADO=$(jq -r --arg a "$AUTOR_GITHUB" '.[$a] // empty' "$MIEMBROS" 2>/dev/null || true)
ASIGNADO="${ASIGNADO:-${TAIGA_ASIGNADO_POR_DEFECTO:-null}}"

ISSUE=$(taiga GET "/issues?project=$TAIGA_PROYECTO_ID&status__is_closed=false" \
  | jq -c --arg p "$PREFIJO" '[.[] | select(.subject | startswith($p))][0] // empty')
HISTORIA_ID=$(taiga GET "/userstories?project=$TAIGA_PROYECTO_ID" | jq -r --arg h "$HISTORIA" '[.[] | select(.subject == $h)][0].id // empty')
TAREA=""
if [[ -n "$HISTORIA_ID" ]]; then
  TAREA=$(taiga GET "/tasks?project=$TAIGA_PROYECTO_ID&user_story=$HISTORIA_ID&status__is_closed=false" \
    | jq -c --arg p "$PREFIJO" '[.[] | select(.subject | startswith($p))][0] // empty')
fi

if [[ "$RESULTADO" == "fallo" ]]; then
  DESCRIPCION=$(printf '**Repositorio:** %s\n**Rama:** %s\n**Commit:** %s\n**Autor:** %s\n**Etapa fallida:** %s\n**Log:** %s\n\n```\n%s\n```' \
    "$REPO" "$RAMA" "$COMMIT" "$AUTOR_GITHUB" "$JOB" "$URL_LOG" "${DETALLE:-(ver el log)}")
  if [[ -n "$ISSUE" ]]; then
    ID=$(jq -r .id <<<"$ISSUE"); VERSION=$(jq -r .version <<<"$ISSUE")
    taiga PATCH "/issues/$ID" "$(jq -n --arg c "Volvió a fallar en el commit $COMMIT ($AUTOR_GITHUB). Log: $URL_LOG" --argjson v "$VERSION" '{comment: $c, version: $v}')" >/dev/null
    echo "Issue #$(jq -r .ref <<<"$ISSUE") comentado"
  else
    TIPO=$(taiga GET "/issue-types?project=$TAIGA_PROYECTO_ID" | jq -r '([.[] | select(.name == "Fallo CI")][0] // [.[] | select(.name == "Bug")][0] // .[0]).id')
    NUEVO=$(taiga POST "/issues" "$(jq -n --argjson pr "$TAIGA_PROYECTO_ID" --arg s "$ASUNTO" --arg d "$DESCRIPCION" --argjson t "$TIPO" \
      --argjson a "$ASIGNADO" --arg r "$REPO" --arg j "$JOB" \
      '{project: $pr, subject: $s, description: $d, type: $t, assigned_to: $a, tags: ["ci", $r, $j]}')")
    echo "Issue #$(jq -r .ref <<<"$NUEVO") creado"
  fi
  if [[ -z "$HISTORIA_ID" ]]; then
    HISTORIA_ID=$(taiga POST "/userstories" "$(jq -n --argjson pr "$TAIGA_PROYECTO_ID" --arg s "$HISTORIA" \
      '{project: $pr, subject: $s, description: "Tareas creadas automáticamente por el pipeline cuando falla una etapa.", tags: ["ci"]}')" | jq -r .id)
  fi
  if [[ -z "$TAREA" ]]; then
    TAREA_NUEVA=$(taiga POST "/tasks" "$(jq -n --argjson pr "$TAIGA_PROYECTO_ID" --argjson us "$HISTORIA_ID" --arg s "${PREFIJO}corregir en ${RAMA}" \
      --arg d "$DESCRIPCION" --argjson a "$ASIGNADO" '{project: $pr, user_story: $us, subject: $s, description: $d, assigned_to: $a, tags: ["ci"]}')")
    echo "Tarea #$(jq -r .ref <<<"$TAREA_NUEVA") creada"
  fi
elif [[ "$RESULTADO" == "exito" ]]; then
  if [[ -n "$ISSUE" ]]; then
    ID=$(jq -r .id <<<"$ISSUE"); VERSION=$(jq -r .version <<<"$ISSUE")
    CERRADO=$(taiga GET "/issue-statuses?project=$TAIGA_PROYECTO_ID" | jq -r '[.[] | select(.is_closed)][0].id')
    taiga PATCH "/issues/$ID" "$(jq -n --arg c "Resuelto en el commit $COMMIT ($AUTOR_GITHUB)." --argjson v "$VERSION" --argjson st "$CERRADO" \
      '{comment: $c, version: $v, status: $st}')" >/dev/null
    echo "Issue #$(jq -r .ref <<<"$ISSUE") cerrado"
  fi
  if [[ -n "$TAREA" ]]; then
    ID=$(jq -r .id <<<"$TAREA"); VERSION=$(jq -r .version <<<"$TAREA")
    CERRADA=$(taiga GET "/task-statuses?project=$TAIGA_PROYECTO_ID" | jq -r '[.[] | select(.is_closed)][0].id')
    taiga PATCH "/tasks/$ID" "$(jq -n --arg c "Resuelto en el commit $COMMIT." --argjson v "$VERSION" --argjson st "$CERRADA" \
      '{comment: $c, version: $v, status: $st}')" >/dev/null
    echo "Tarea #$(jq -r .ref <<<"$TAREA") cerrada"
  fi
else
  echo "Resultado desconocido: $RESULTADO" >&2; exit 2
fi
