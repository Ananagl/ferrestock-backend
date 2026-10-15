// Nombre:                 sesion.js
// Descripción:            Inicio de sesión para los scripts de k6: obtiene la cookie XSRF-TOKEN, inicia sesión y devuelve la cabecera CSRF.
// Fecha de creación:      2026-10-15
// Fecha de actualización: 2026-10-15
// Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
// Versión:                0.11.0
import http from 'k6/http';
import { check } from 'k6';

export const API = `${__ENV.FS_URL || 'http://localhost:8080'}/api/v1`;

/**
 * Inicia sesión con el usuario de las variables FS_USUARIO y FS_CLAVE (el jar de cookies de k6 es por VU).
 * @returns {{headers: object}} Parámetros con la cabecera X-XSRF-TOKEN para peticiones que modifican.
 */
export function iniciarSesion() {
  http.get(`${API}/auth/csrf`);
  const xsrf = http.cookieJar().cookiesForURL(API)['XSRF-TOKEN'][0];
  const params = { headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': xsrf } };
  const r = http.post(`${API}/auth/login`, JSON.stringify({ usuario: __ENV.FS_USUARIO, contrasena: __ENV.FS_CLAVE, recordarme: false }), params);
  check(r, { 'login 200': (x) => x.status === 200 });
  return params;
}
