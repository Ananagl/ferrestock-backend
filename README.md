# FerreStock — Backend

| Campo | Valor |
|---|---|
| **Nombre** | README.md |
| **Descripción** | Requisitos, instalación, ejecución y pruebas del backend de FerreStock. |
| **Fecha de creación** | 2026-10-14 |
| **Fecha de actualización** | 2026-10-15 |
| **Autores** | Ana Sofía Nagles García · María Mercedes Rincón Valencia · Juan David Camacho Tapiero |
| **Versión** | 0.1.0 |

API REST JSON en **Java 21 + Spring Boot 3.5** con Spring Data JPA, Spring Security, Bean Validation, Lombok, Actuator y springdoc-openapi, sobre **PostgreSQL**. El diseño está en `../docs/fase-2-diseno/`.

## Requisitos

| Herramienta | Versión | Uso |
|---|---|---|
| JDK | 21 | Compilar y ejecutar. En macOS: `brew install openjdk@21` |
| PostgreSQL | 14 o superior (en CI se usa 16) | Base de datos de desarrollo |
| Docker | Cualquiera reciente, encendido | Pruebas de integración (Testcontainers) |
| Maven | No hace falta | Se usa el wrapper `./mvnw` |

Si `java -version` no muestra 21, exporte `JAVA_HOME` antes de usar `./mvnw`:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home)
```

## Configuración

1. Copie `.env.example` a `.env` y cambie las claves. `.env` **nunca** se sube al repositorio.
2. Cree la base de datos. El script no borra nada: si la base ya existe, se detiene.

```bash
set -a; source .env; set +a; PGPORT=5432 ./db/crear_bd.sh
```

El script crea el rol `ferrestock_app`, la base `ferrestock`, aplica `db/01_esquema.sql` y `db/02_datos_iniciales.sql`, y crea el usuario `admin` con la clave de `FS_ADMIN_CLAVE_INICIAL`. Esa clave debe cambiarse en el primer ingreso.

## Ejecución

| Acción | Comando |
|---|---|
| Arrancar en desarrollo (puerto 8080) | `./scripts/arrancar.sh` |
| Compilar el JAR | `./scripts/compilar.sh` |
| Pruebas unitarias + integración + cobertura | `./scripts/probar.sh` (requiere Docker) |
| Pruebas de mutación (PIT) | `./scripts/mutacion.sh` |
| Verificar encabezados de archivos | `./scripts/verificar_encabezados.sh` |
| Generar Javadoc | `./mvnw javadoc:javadoc` → `target/reports/apidocs` |

| URL (perfil dev) | Descripción |
|---|---|
| `http://localhost:8080/actuator/health` | Salud: `{"status":"UP"}` |
| `http://localhost:8080/swagger-ui.html` | Documentación OpenAPI (solo pública en dev) |

## Perfiles

`dev` (por defecto), `test` (Testcontainers) y `prod` (exige `FS_DB_URL`, `FS_DB_USUARIO` y `FS_DB_CLAVE`; cookies `Secure`).

## Estructura

Paquete base `com.ferrestock`, organizado por módulo funcional y luego por capa (Controller → Service → Repository). Ver `../docs/fase-2-diseno/08_repositorios_y_entrega.md`.
