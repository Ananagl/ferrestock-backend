// Nombre:                 listado_carga.js
// Descripción:            Prueba de carga: listados concurrentes de productos e inventario con umbrales de latencia (RNF-DES-01).
// Fecha de creación:      2026-10-15
// Fecha de actualización: 2026-10-15
// Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
// Versión:                0.11.0
import http from 'k6/http';
import { check, sleep } from 'k6';
import { API, iniciarSesion } from './sesion.js';

export const options = {
  stages: [{ duration: '30s', target: 20 }, { duration: '1m', target: 20 }, { duration: '15s', target: 0 }],
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<800'] },
};

export function setup() {
  return {};
}

export default function () {
  if (__ITER === 0) iniciarSesion();
  check(http.get(`${API}/productos?tamano=20`), { 'productos 200': (r) => r.status === 200 });
  check(http.get(`${API}/inventario/existencias?tamano=20`), { 'existencias 200': (r) => r.status === 200 });
  sleep(1);
}
