# Plan de trabajo: Trello y GitHub

Propuesta de tablero para la pareja. Crea en Trello las listas **Por hacer**, **En progreso**, **En revisión** y **Hecho**, y una tarjeta por cada fila. Asigna cada tarjeta a su responsable y muévela a medida que avanzan.

## Reparto sugerido

| # | Tarjeta | Responsable | Checklist |
|---|---|---|---|
| 1 | Configurar repositorio GitHub y ramas | Integrante 1 | Repo público, invitar al docente, rama `main` y `develop` |
| 2 | Microservicio `usuarios-service` | Integrante 1 | Entidades Usuario y Empresa, CRUD, login con hash, manejo de errores |
| 3 | Microservicio `pagos-service` | Integrante 2 | Entidades Compromiso, Pago y Reporte, CRUD, regla "no pagar dos veces" |
| 4 | Base de datos MySQL | Integrante 2 | XAMPP, bases `pagoflex_usuarios` y `pagoflex_pagos`, prueba con `/api/salud` |
| 5 | Pruebas del backend | Integrante 1 | `UsuarioServiceTest`, `PagoServiceTest` con `mvn test` |
| 6 | Capa Retrofit de la app | Integrante 2 | DTOs, `UsuariosApi`, `PagosApi`, `ClienteHttp`, IP configurable |
| 7 | Sincronización y modo sin conexión | Integrante 2 | `SincronizadorRemoto`, carga inicial, cola de cambios pendientes |
| 8 | Login contra el servidor | Integrante 1 | `POST /api/auth/login`, mensajes de error, bloqueo por intentos |
| 9 | CRUD de reportes desde la app | Integrante 1 | Crear, ver, tomar o resolver (agente), eliminar (usuario) |
| 10 | API externa mindicador.cl | Integrante 2 | `IndicadoresApi`, caché, tarjeta "Indicadores de hoy" |
| 11 | Pruebas unitarias de la app (80%) | Ambos | Tests de Sincronizador e Indicadores (Integrante 2), LoginViewModel e IndicadoresViewModel (Integrante 1), reporte de cobertura |
| 12 | Diseño estilo banco | Integrante 1 | Login, inicio, comprobante, ícono |
| 13 | APK firmado | Integrante 2 | Crear `.jks`, `keystore.properties`, `assembleRelease`, copiar APK a `apk/` |
| 14 | README y capturas | Ambos | Integrantes, links, capturas del APK y del `.jks` |
| 15 | Ensayo de la defensa | Ambos | Probar en el emulador y en el celular, con el servidor encendido y apagado |

## Commits

La pauta revisa que **cada integrante** tenga commits propios. Algunas recomendaciones:

- Cada uno hace commit desde **su propia cuenta** de GitHub, con la parte que le tocó en el tablero, a medida que la revisa, la prueba y la entiende.
- Usen mensajes claros, por ejemplo: `feat(pagos-service): registrar pago y marcar compromiso como Pagado` o `test(app): pruebas de LoginViewModel con MockK`.
- Trabajen en ramas (`feature/usuarios-service`, `feature/retrofit`, …) y únanlas con *Pull Request*. El otro integrante revisa el PR, y así queda evidencia de colaboración.
- Asocien cada tarjeta de Trello con su commit o PR (Trello tiene el Power-Up de GitHub).
- **No hagan commits después de la fecha límite:** la pauta asigna nota 1.0 si detecta cambios posteriores.
