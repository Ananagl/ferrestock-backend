// Nombre:                 picos.js
// Descripción:            Prueba de picos: ráfaga corta de usuarios sobre la salud, el ingreso y los listados para observar la recuperación.
// Fecha de creación:      2026-10-15
// Fecha de actualización: 2026-10-15
// Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
// Versión:                0.11.0
import http from 'k6/http';
import { check, sleep } from 'k6';
import { API, iniciarSesion } from './sesion.js';

export const options = {
  stages: [{ duration: '10s', target: 5 }, { duration: '10s', target: 100 }, { duration: '30s', target: 100 },
    { duration: '10s', target: 5 }, { duration: '20s', target: 5 }],
  thresholds: { http_req_failed: ['rate<0.05'], 'http_req_duration{fase:recuperacion}': ['p(95)<1000'] },
};

export default function () {
  if (__ITER === 0) iniciarSesion();
  check(http.get(`${(__ENV.FS_URL || 'http://localhost:8080')}/actuator/health`), { 'health 200': (r) => r.status === 200 });
  check(http.get(`${API}/productos?tamano=50`), { 'productos 200': (r) => r.status === 200 });
  sleep(0.5);
}
