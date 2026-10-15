# Phonemod 3.0 — Plan de construcción
Aprobado: SPEC v0 (D-24) · PLAN (D-29, 2026-10-15)

Todo el trabajo de 3.0 va en la rama `feature/v3`, que sale de `feature/v2-refactor`.
`release/2.x` solo recibe fixes (D-6).

Reglas comunes:
- Cada card cita los R-xx que cubre; si un código no cubre ningún R-xx, no se escribe (D-7).
- Cada card cierra con `hytale-verify`: build verde, boot en ENABLED y los gates aplicables.
- Entre fases hay aprobación del usuario.

## Módulos
| módulo | tipo | contenido | R-xx |
|---|---|---|---|
| phonemod-api | lib (jar) | contrato 3.0, sin UI de Hytale | R-API |
| phonemod-core | lib | dominio actual movido (identity, messaging, calls/voz, sync, persistence, migration, telemetry, security, config, components) | R-CORE, R-SYNC |
| phonemod-ui-kit | lib | diff, FakeBackend, pipeline de acciones sobre las guardas existentes, router neutral, refresh | R-KIT |
| phonemod-ui-legacy | lib | DevicePage sobre ui-kit, LegacyView, LegacyRenderHook, apps nativas | R-LEG, R-APP-1/2 |
| phonemod-ui-noesis | lib, apagado | backend Noesis (solo con go del spike) | R-NOE |
| phonemod-runtime | plugin (jar shaded) | raíz de composición: PhoneMod, setup, interacciones | todos |

Grafo: `api ← core ← ui-kit ← ui-legacy / ui-noesis ← runtime`. El nombre exacto de paquetes
y cómo conviven las clases con las de v2 hasta F4 los fija `phases/F2/plan-skeleton.md`.

## Contratos (primero)
| contrato | proveedor | consumidores |
|---|---|---|
| DeviceApp<M>, ViewModel/Prop/ListProp, DeviceAction, DeviceAppContext, AppDescriptor/views, DeviceType, DeviceSessionId | phonemod-api | ui-kit, backends, apps, tuiter |
| DiffOp (SetProp/InsertItem/RemoveItem/UpdateItem), BackendPort, FakeBackend | phonemod-ui-kit | ui-legacy, ui-noesis, tests de apps |
| ConfigPort, AudioPolicyPort, SessionRegistry (por DeviceSessionId) | phonemod-core | ui-kit, runtime |

## Fases
Habilidades: máximo 3 `hytale-*` por card, más `hytale-verify` al final, que es obligatorio y
no cuenta.

### F2 — Spec + tests de contrato (en curso)
Entrega: SPEC aprobada, esqueleto 3.0 que compila, tests de contrato en rojo y scripts en verde.
| card | objetivo | agente | skills | R-xx | aceptación | paralelo |
|---|---|---|---|---|---|---|
| F2-1 | Diseñar el esqueleto y la disposición de tests | architect | hytale-frameworks, hytale-testing | R-API-3..8, R-KIT-1..5 | `phases/F2/plan-skeleton.md` con citas | — |
| F2-2 | Esqueleto 3.0 (cuerpos que lanzan UnsupportedOperationException) + wiring de surefire y perfil `-Pcontract` | implementer | hytale-testing | R-API, R-KIT | `mvn -B test` verde (96) y `-Pcontract` compila y falla por falta de implementación | — |
| F2-3 | Tests de contrato | implementer | hytale-testing | R-API-3..7, R-KIT-1/2/3/5, R-NOE-3 (se queda en espera) | cada @Test nombrado por su R-xx | con F2-4 |
| F2-4 | `bin/check-api-no-ui-imports.sh` + `bin/check-test-renames.sh` | implementer | hytale-testing | R-API-1, R-CORE-1, R-CORE-6 | ambos en verde hoy | con F2-3 |
| F2-5 | `IN_GAME_EXIT_PASS_V3.md` generado desde los R-PAR G | yo | — | R-PAR (G), D-13 | cada R-PAR G tiene un paso con el comando real | con F2-2..4 |
Gate F2: build verde · `-Pcontract` rojo esperado · scripts verdes · aprobación.

### F3 — Extracción del core (M)
| card | objetivo | agente | skills | R-xx | aceptación |
|---|---|---|---|---|---|
| F3-1 | Plan de movimiento paquete a paquete (SPLIT-AUDIT §2) | architect | hytale-ecs, hytale-frameworks | R-CORE-1..8 | lista de ficheros y orden |
| F3-2 | Mover los paquetes puros a phonemod-core; renombres mecánicos de tests | implementer | hytale-ecs | R-CORE-1, R-CORE-6 | `check-test-renames.sh` y `check-api-no-ui-imports.sh` verdes; 96 tests verdes |
| F3-3 | ConfigPort y AudioPolicyPort (cierran la fuga de CallRegistry:212) | implementer | hytale-ecs | R-CORE-2, R-CORE-3 | grep-gates + tests headless |
| F3-4 | DeviceSessionId y registro online de devices compartidos; normalizar el alias | implementer | hytale-ecs | R-CORE-4, R-CORE-5, R-SYNC-2 | tests headless |
| F3-5 | Asserts de hilo (openShared, contacts, state) | implementer | hytale-ecs | R-CORE-8 | test que lanza fuera de hilo |
| F3-6 | Catálogo e instalación sobre AppDescriptor | implementer | — | R-CORE-7, R-PAR-FIX-5 | tests de catálogo |
Paralelo: F3-3, F3-4 y F3-5 tras F3-2 (tocan ficheros distintos; en worktrees separados).

### F4 — ui-kit (M)
| card | objetivo | R-xx | aceptación |
|---|---|---|---|
| F4-1 | DiffEngine + FakeBackend | R-KIT-1, R-KIT-2, R-API-4 | contract en verde |
| F4-2 | Pipeline de acciones sobre las guardas existentes sin cambios | R-KIT-3 | contract en verde; tests de security intactos |
| F4-3 | Router neutral | R-KIT-4 | tests de rutas |
| F4-4 | refresh() coalescente y onWorld | R-KIT-5, R-API-6 | contract en verde |
| F4-5 | `ctx.sync` sobre phone.sync; quitar LegacyRooms y stringChannels | R-SYNC-1..4 | tests sync verdes + grep-gate |
| F4-6 | Retirar los tipos v1/v2 y el paquete temporal (si F2-1 lo usó) | R-API-2 | ApiContractTest verde |
Agente: implementer; skills: hytale-testing, hytale-ecs.

### F5 — Backend legacy (L)
| card | objetivo | R-xx | aceptación |
|---|---|---|---|
| F5-1 | LegacyView tipado + validación en el boot | R-LEG-2 | test headless |
| F5-2 | DevicePage sobre ui-kit; pushes vía diff | R-LEG-1, R-LEG-5, R-PAR-SHELL-* | exit pass V3 (shell) |
| F5-3 | LegacyRenderHook solo en ui-legacy | R-LEG-3 | check-api-no-ui-imports |
| F5-4 | DeviceType unificado; quitar `__deviceType__` | R-API-8 | grep-gate |
Agente: architect, luego implementer; skills: hytale-ui, hytale-ui-widgets, hytale-ecs.
Gates: check-ui-docs, lang-audit, R-LEG-4 (diff de .ui).

### F6 — Apps nativas + ejemplo (L)
Una card por app: Calls, Whatgram, Contacts, Settings, AppStore, GroupChat y hello-device-app.
Pueden ir en paralelo en worktrees, porque son ficheros disjuntos.
R-xx: R-APP-1..4 y R-PAR de cada app, incluidos FIX-1..4, FIX-6..8.
Skills: hytale-ui-widgets, hytale-testing.
Gate: tests headless verdes + exit pass V3 completo en un server de pruebas.

### F7 — Piloto tuiter-app (M)
Port a 3.0 con ctx.sync y manifest `>=3.0.0-alpha.1`; consumer-smoke 3.0; MIGRATION_V2_TO_V3.md.
R-APP-5, R-APP-6. Skills: hytale-frameworks, hytale-testing.
Gate: desplegar en hytest y prueba multijugador. **El deploy solo se hace si lo pides
explícitamente.**

### F8 — Backend Noesis (solo si el spike da go)
R-NOE-1..6. Skills: hytale-ui, hytale-protocol. Gate: ENABLED con el flag apagado y encendido, y
prueba con cliente.

### F9 — Release 3.0 (M)
Docs (V3_API.md, tutorial), evidencia de release y hash del jar, bump de Version del pack
Phonemod-Assets si cambió el arte (D-10: el pack sigue separado), desplegar en hytest (a
petición tuya) y arrancar la ola 2.

## Riesgos
| riesgo | prob. | mitigación |
|---|---|---|
| Colisión de nombres v2/3.0 durante F2–F4 | cierta | estrategia de F2-1; F4-6 la retira |
| El diff no expresa una pantalla legacy | media | LegacyRenderHook (R-LEG-3) |
| Regresión de comportamiento al mover el core | media | 96 tests intactos salvo renombres (D-21) + R-PAR headless |
| Cambios de API de Noesis en cada sync | alta | todo en ui-noesis apagado; re-ejecutar el spike tras cada sync |
| Varios panes tocando plugins-srcs | media | ramas propias y `git commit -o` con rutas explícitas |
