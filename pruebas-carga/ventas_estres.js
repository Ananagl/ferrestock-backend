// Nombre:                 ventas_estres.js
// Descripción:            Prueba de estrés y límite: muchas ventas simultáneas del mismo producto; nunca debe haber errores 500 ni existencias negativas (RN-01, CA-VEN-03).
// Fecha de creación:      2026-10-15
// Fecha de actualización: 2026-10-15
// Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
// Versión:                0.11.0
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { API, iniciarSesion } from './sesion.js';

const confirmadas = new Counter('ventas_confirmadas');
const rechazadas = new Counter('ventas_rechazadas_por_existencias');

// FS_PRODUCTO_ID y FS_CLIENTE_ID deben existir; el producto se carga antes con N existencias.
export const options = {
  scenarios: { pico: { executor: 'shared-iterations', vus: 50, iterations: 200, maxDuration: '2m' } },
  thresholds: { 'http_req_failed{esperado:no}': ['rate==0'], checks: ['rate==1'] },
};

let params;

export default function () {
  if (!params) params = iniciarSesion();
  const cuerpo = JSON.stringify({ clienteId: Number(__ENV.FS_CLIENTE_ID), lineas: [{ productoId: Number(__ENV.FS_PRODUCTO_ID), cantidad: 1 }],
    pago: { medio: 'EFECTIVO', valorRecibido: '99999999' } });
  const r = http.post(`${API}/ventas`, cuerpo, { ...params, responseCallback: http.expectedStatuses(201, 409) });
  check(r, { 'solo 201 o 409 (nunca 500)': (x) => x.status === 201 || x.status === 409 });
  if (r.status === 201) confirmadas.add(1);
  if (r.status === 409) rechazadas.add(1);
}

export function teardown() {
  const p = iniciarSesion();
  const verificacion = http.get(`${API}/inventario/verificacion`, p);
  check(verificacion, { 'kardex consistente tras el estrés': (r) => r.status === 200 && r.json().length === 0 });
}
