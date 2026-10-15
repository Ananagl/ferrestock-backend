# Pruebas de carga, estrés y límite (k6)

| Nombre | Descripción |
|---|---|
| Autores | Ana Sofía Nagles García · María Mercedes Rincón Valencia · Juan David Camacho Tapiero |
| Fecha de creación / actualización | 2026-10-15 / 2026-10-15 |
| Versión | 0.11.0 |

| Script | Tipo | Qué verifica |
|---|---|---|
| `listado_carga.js` | Carga | 20 usuarios listando productos y existencias: errores < 1 % y p95 < 800 ms |
| `ventas_estres.js` | Estrés y límite | 200 ventas de 1 unidad con 50 usuarios sobre el mismo producto: solo 201 o 409, nunca 500, y kardex consistente al final |
| `picos.js` | Picos | Ráfaga de 5 a 100 usuarios y regreso: errores < 5 % |

```bash
k6 run -e FS_USUARIO=admin.demo -e FS_CLAVE=... pruebas-carga/listado_carga.js
k6 run -e FS_USUARIO=... -e FS_CLAVE=... -e FS_PRODUCTO_ID=... -e FS_CLIENTE_ID=... pruebas-carga/ventas_estres.js
```

En el pipeline se ejecutan manualmente (`workflow_dispatch`), contra el entorno de demostración. La garantía de concurrencia también está cubierta en las pruebas de integración (`InventarioIT`, `VentasIT`, `AjustesIT`) con PostgreSQL real.
