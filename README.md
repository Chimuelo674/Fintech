# PagoFlex: app de pagos y compromisos

Aplicación móvil en **Kotlin + Jetpack Compose** para la empresa PagoFlex ([pagoflex.com.ar](https://pagoflex.com.ar)). Las personas pagan sus compromisos (cuotas, arriendos, créditos), las empresas registran cobros y los agentes de soporte atienden los reportes. La app se conecta a **dos microservicios en Spring Boot con MySQL** y consume la **API externa mindicador.cl**.

> Evaluación Parcial 4: DSY1105 Desarrollo de Aplicaciones Móviles.

---

## 1. Nombre del proyecto

**PagoFlex**: billetera de pagos y cobranza con estilo de app bancaria.

## 2. Integrantes

| Integrante | Usuario GitHub | Responsabilidades principales |
|---|---|---|
| _Nombre Apellido 1_ | _@usuario1_ | _Completar_ |
| _Nombre Apellido 2_ | _@usuario2_ | _Completar_ |

- **Tablero Trello:** _pegar link aquí_ (ver [`docs/TRELLO.md`](docs/TRELLO.md) para el plan de tareas).
- **Repositorio:** _pegar link aquí_

## 3. Funcionalidades

**Cliente (usuario final)**
- Ingreso con RUT y clave de 4 dígitos, validado en el microservicio `usuarios-service`. Incluye bloqueo tras 3 intentos fallidos, opción "Recordar mi RUT" e ingreso con huella.
- Inicio estilo banco con:
  - el total por pagar del mes, que se puede ocultar;
  - accesos rápidos;
  - los compromisos vigentes y los últimos movimientos;
  - el estado de la conexión con el servidor.
- **Indicadores económicos del día** (UF, dólar, euro y UTM) desde la API externa **mindicador.cl**.
- Pago de compromisos con comprobante tipo banco (N° de operación, compartir).
- Historial de pagos, detalle de cada compromiso y configuración de avisos.
- Reportes de problemas: **crear, ver y eliminar** mientras ningún agente los haya tomado.
- QR y link de pago para que otra persona pague un compromiso.
- Simulador de costos: PagoFlex (3% fijo) frente a un banco tradicional.
- Recomendar organizaciones.

**Agente PagoFlex**
- Ve los reportes de todos los usuarios, los toma y los resuelve. Cada cambio se **actualiza** en `pagos-service`.
- Gestiona las recomendaciones de organizaciones.

**Ejecutivo de empresa**
- **Registra** compromisos nuevos para sus clientes y crea al cliente si no existe.
- **Anula** compromisos.
- Ve los indicadores de cobranza de su empresa.

**Funcionamiento sin conexión:** la app guarda una copia en el teléfono (Room). Si el servidor no responde, sigue funcionando con esa copia, y los cambios hechos sin conexión se envían al servidor apenas vuelve la conexión.

### Arquitectura

```
 App Android (Kotlin + Compose, MVVM)
 ├── Room (copia local)
 ├── Retrofit ──► usuarios-service :8081 ──► MySQL pagoflex_usuarios
 ├── Retrofit ──► pagos-service    :8082 ──► MySQL pagoflex_pagos
 └── Retrofit ──► https://mindicador.cl/api  (API externa)
```

## 4. Endpoints utilizados

### Microservicio `usuarios-service` (puerto 8081)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/salud` | Estado del servicio |
| POST | `/api/auth/login` | Ingreso con `{ "rut", "clave" }`: 200 con el usuario, 401 si los datos son incorrectos |
| GET | `/api/usuarios?rol=` | Lista los usuarios (el filtro por rol es opcional) |
| GET | `/api/usuarios/{id}` | Obtiene un usuario |
| POST | `/api/usuarios` | Crea un usuario |
| POST | `/api/usuarios/lote` | Carga inicial de usuarios |
| PUT | `/api/usuarios/{id}` | Actualiza un usuario (por ejemplo, la configuración de avisos) |
| DELETE | `/api/usuarios/{id}` | Elimina un usuario |
| GET / POST / PUT / DELETE | `/api/empresas`, `/api/empresas/{id}`, `/api/empresas/lote` | CRUD de empresas |

### Microservicio `pagos-service` (puerto 8082)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/salud` | Estado del servicio |
| GET | `/api/compromisos?usuario=&empresa=` | Lista los compromisos (filtros opcionales) |
| GET / POST / PUT / DELETE | `/api/compromisos/{id}` | CRUD de compromisos (no se elimina un compromiso pagado) |
| POST | `/api/compromisos/lote` | Carga inicial |
| GET | `/api/pagos?usuario=` | Pagos de un usuario |
| POST | `/api/pagos` | Registra un pago y deja el compromiso como **Pagado**. Rechaza pagos dobles |
| GET / PUT / DELETE | `/api/pagos/{id}` | Consulta, actualiza o elimina un pago |
| GET | `/api/reportes?usuario=&estado=` | Lista los reportes |
| POST / PUT / DELETE | `/api/reportes`, `/api/reportes/{id}` | Crea, actualiza (toma o resuelve) y elimina reportes |

Los errores responden con JSON `{ "mensaje": "..." }` y los códigos 400, 401, 404 o 409.

### API externa

| Método | Endpoint | Uso en la app |
|---|---|---|
| GET | `https://mindicador.cl/api` | Tarjeta "Indicadores de hoy" en el inicio (UF, dólar, euro, UTM). Se guarda 10 minutos en caché |

## 5. Pasos para ejecutar

### 5.1 Base de datos (MySQL)
1. Abre **XAMPP** (o Laragon) e inicia **MySQL**.
2. No hace falta crear nada: cada microservicio crea su base (`pagoflex_usuarios` y `pagoflex_pagos`) y sus tablas al arrancar.
   Por defecto se conectan como `root` sin contraseña. Si tu MySQL tiene contraseña, cámbiala en `src/main/resources/application.properties` de cada servicio.

### 5.2 Microservicios (Spring Boot, Java 17)
1. Abre `backend/usuarios-service` en IntelliJ IDEA o VS Code (con el Extension Pack for Java) y ejecuta `UsuariosServiceApplication`.
2. Haz lo mismo con `backend/pagos-service` (`PagosServiceApplication`).
   - Con Maven instalado, también puedes usar `mvn spring-boot:run` dentro de cada carpeta.
3. Comprueba en el navegador que respondan:
   - http://localhost:8081/api/salud
   - http://localhost:8082/api/salud
4. La primera vez que la app se conecta, envía sus datos de prueba a los microservicios (usuarios, empresas, compromisos, pagos y reportes).

### 5.3 App móvil
1. Abre la carpeta del proyecto en **Android Studio** y espera el *Gradle Sync*.
2. **Emulador:** ejecuta con ▶. La app usa `10.0.2.2`, que es la dirección del PC vista desde el emulador.
3. **Celular físico:**
   - el PC y el celular deben estar en la **misma red WiFi**;
   - busca la IP del PC (en Windows, con el comando `ipconfig`; por ejemplo `192.168.1.10`);
   - en la app, pantalla de ingreso, toca el ícono de **servidor** (arriba a la derecha), escribe la IP y toca **Guardar y probar**;
   - si no conecta, permite Java en el **Firewall de Windows** (puertos 8081 y 8082).
4. Ingresa con un usuario de prueba (botón "Usuarios de prueba"). La clave de todos es **1234**. Por ejemplo, Camila Rojas: `15.834.207-3`.

### 5.4 Pruebas unitarias
- **App:** en Android Studio, clic derecho en `app/src/test` > *Run Tests*, o en la terminal:
  ```
  ./gradlew testDebugUnitTest
  ./gradlew createDebugUnitTestCoverageReport   # reporte de cobertura en app/build/reports/coverage
  ```
- **Microservicios:** `mvn test` dentro de cada servicio.

| Archivo de pruebas | Qué cubre |
|---|---|
| `SincronizadorRemotoTest` | Conexión con los microservicios: carga inicial, descarga, CRUD, cola sin conexión y login (JUnit con APIs falsas) |
| `IndicadoresRepositoryTest` | Consumo de mindicador.cl, caché y errores |
| `LoginViewModelTest` | Ingreso con el servidor, rechazo, modo sin conexión y bloqueo (MockK) |
| `IndicadoresViewModelTest` | Estados de carga, error y reintento (MockK) |
| `FuncionesBancoTest`, `ReglasNegocioTest`, `AvisosTest`, `RecomendacionTest`, `RolesInternosTest` | Reglas de negocio (RUT, clave, simulador, links de pago, atrasos, avisos, roles) |
| `UsuarioServiceTest`, `PagoServiceTest` (backend) | Login, claves, duplicados y pagos (JUnit 5 + Mockito) |

## 6. APK firmado y llave (.jks)

### Generar la llave
1. En Android Studio: **Build > Generate Signed App Bundle / APK > APK > Next**.
2. En *Key store path*, elige **Create new…** y guárdala en `keystore/pagoflex.jks`, dentro de este proyecto.
3. Elige tus contraseñas, usa el alias `pagoflex` y completa tu nombre.
4. Copia `keystore.properties.ejemplo` como `keystore.properties` y escribe las mismas contraseñas.

### Generar el APK
- Desde el mismo asistente elige **release** y **Create**, o en la terminal:
  ```
  ./gradlew assembleRelease
  ```
- El APK queda en `app/build/outputs/apk/release/app-release.apk`. Cópialo a la carpeta `apk/` del repositorio.
- `app/build.gradle.kts` lee `keystore.properties` y firma la versión *release* automáticamente.

> El encargo pide dejar el `.jks` y su configuración en el repositorio. En un proyecto real, la llave y sus contraseñas **nunca** se suben a un repositorio público.

### Capturas
_Agregar aquí las capturas del APK firmado y del archivo .jks:_

![APK firmado](docs/capturas/apk-firmado.png)
![Llave .jks](docs/capturas/jks.png)
