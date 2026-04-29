# DiscoSoftBack

Backend en Spring Boot + Kotlin para el sistema de gestión de discotecas, bares y salones de billar **DiscoSoft** (parte de la plataforma Kompralo). Expone una API REST y un servidor Socket.IO para los módulos de pedidos, billar, jornadas, inventario, promociones y un panel multi-negocio (rol `SUPER`).

## Stack

- **Kotlin** 1.9.25 sobre **JDK 17**
- **Spring Boot** 3.2 (Web, Security, Data JPA, Validation)
- **PostgreSQL** 15
- **JWT** (jjwt 0.12) para autenticación
- **Netty Socket.IO** para eventos en tiempo real (puerto `3001`)
- **iText 7** para generación de PDFs y **ZXing** para QR
- **Firebase Admin** + Google APIs (Gmail, OAuth) para notificaciones y correo
- **Bucket4j** para rate limiting
- Construcción con **Gradle** (Kotlin DSL); imagen Docker multi-stage con `eclipse-temurin:17`

## Estructura del proyecto

```
src/main/kotlin/com/kompralo/
├── AuthBackendApplication.kt   Punto de entrada Spring Boot
├── config/                     Security, CORS, JWT filter, Socket.IO, TenantContext, RateLimit
├── controller/                 Endpoints REST (Auth, Billar, Management, Pedido, Super, Health)
├── services/                   Lógica de negocio (pedidos, billar, jornadas, JWT, Socket.IO)
├── repository/                 Spring Data JPA
├── model/                      Entidades JPA del dominio Disco/Kompralo
├── dto/                        DTOs de request/response
├── mapper/  domain/  port/  application/  infrastructure/   Capas auxiliares hexagonales
└── exception/                  Manejo global de errores
sql/                            Migraciones SQL (V1..V6) de tablas disco, billar, super, etc.
```

## Módulos principales

| Módulo | Prefijo | Descripción |
|---|---|---|
| Auth | `/api/disco/auth` | Login, logout, JWT, roles (`SUPER`, admin, mesero) |
| Pedidos | `/api/disco/pedidos` | Atender mesa, despachar, cancelar, aplicar promos, pagar, jornada (resumen / historial / cerrar) |
| Billar | `/api/disco/billar` | CRUD mesas, iniciar/finalizar/trasladar partidas, edición y borrado de partidas con recálculo |
| Management | `/api/disco/management` | Productos, meseros, jornadas, inventarios, mesas, comparativos, promociones |
| Super | `/api/disco/super` | Dashboard consolidado multi-negocio (solo rol `SUPER`): totales, pagos por método, tendencia 30d, top productos, top meseros, delta mes |
| Health | `/api/auth/health` | Healthcheck |

## Configuración

Copia el archivo de ejemplo y ajusta los valores:

```bash
cp application.properties.example src/main/resources/application.properties
```

Variables relevantes (también pueden inyectarse por entorno):

| Variable | Default | Uso |
|---|---|---|
| `PORT` | `8081` | Puerto HTTP |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/monastery` | Conexión PostgreSQL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `postgres` / *(definir)* | Credenciales DB |
| `JWT_SECRET` | *(definir, mín. 256 bits)* | Firma de tokens HS256 |
| `JWT_EXPIRATION` | `86400000` (24 h) | Vigencia del token en ms |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:5174` | Orígenes permitidos (incluye `monasteryclub.com` en prod) |
| `socketio.host` / `socketio.port` | `0.0.0.0` / `3001` | Servidor de eventos en tiempo real |

Las migraciones SQL viven en `sql/V*.sql`. La aplicación usa `spring.jpa.hibernate.ddl-auto=none`, así que ejecútalas manualmente o mediante el helper `DatabaseMigration.kt`.

## Ejecución local

### Con Gradle

```bash
# Levantar PostgreSQL local (o ajustar DATABASE_URL)
./gradlew bootRun

# Generar el jar de despliegue
./gradlew bootJar     # produce build/libs/app.jar
```

### Con Docker Compose

```bash
docker-compose up --build
```

Esto arranca PostgreSQL (`kompralo-db`, puerto `5432`) y la app (`kompralo-app`, puertos `8080` y `3001`).

> Nota: el `Dockerfile` expone `8081`, mientras que `docker-compose.yml` mapea `8080`. Ajusta `PORT` o el mapeo según tu entorno.

### Utilidades

- `./gradlew runPasswordGenerator` — ejecuta `PasswordHashGenerator.kt` para generar hashes BCrypt para usuarios sembrados.
- `node seed-local.js` — semilla de datos para desarrollo local (requiere `npm install`).
- `sql/V*.sql` — esquema y datos iniciales (productos, billar, multi-tenant `SUPER`).

## Tiempo real (Socket.IO)

`SocketIOService` y `SocketIOConfig` arrancan un servidor Netty en el puerto `3001`. Los clientes (frontends DiscoSoft) se suscriben para recibir actualizaciones de pedidos, partidas de billar y cierre de jornada.

## Multi-tenant

Cada `Negocio` tiene su propio conjunto de jornadas, productos y meseros. `TenantContext` resuelve el negocio activo a partir del JWT; los endpoints `update`/`delete` validan que el recurso pertenezca al tenant antes de aplicar cambios. El rol `SUPER` puede consultar el consolidado a través de `/api/disco/super/consolidado`.

## Tests

```bash
./gradlew test
```

Usa JUnit Platform + `spring-boot-starter-test` y `spring-security-test`.

## Despliegue

El `bootJar` produce `build/libs/app.jar` con `mainClass = com.kompralo.AuthBackendApplicationKt`. El `Dockerfile` multi-stage compila y empaqueta sobre `eclipse-temurin:17-jre`. Para producción se requiere PostgreSQL accesible y los secretos (`JWT_SECRET`, credenciales DB, claves de Google/Firebase si se usan) inyectados por variables de entorno.