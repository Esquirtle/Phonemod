# Phonemod 3.0 — Especificación
Estado: **v0 aprobada 2026-10-15 (D-24)** · Actualizado: 2026-10-15 ·
Engine: hytale-shared-source `release` 0033ae473 · Base: Phonemod 2.0.0-alpha.13 (`331939f`)

Cada requisito lleva un id, un criterio de aceptación verificable y una verificación asignada:
- **C**: test de contrato (tag JUnit `contract`, perfil `-Pcontract`).
- **H**: test headless sobre lógica pura o FakeBackend.
- **S**: gate de script.
- **G**: paso in-game en `IN_GAME_EXIT_PASS_V3.md`.

Ningún requisito entra sin una decisión (D-n, en `DECISIONS.md`) o una cita `file:line`. Las
citas de 2.x están en `research/SPLIT-AUDIT.md` y `research/R-PAR-DRAFT.md`.

## 1. Visión
3.0 añade una **capa de abstracción sobre las clases que hoy usan la UI**, para que todo lo que
pinta (shell, apps nativas, apps de terceros) se implemente como app de dispositivo neutral
respecto al backend (D-20).
- **El dominio y el backend existentes se reutilizan, no se reescriben.** Son identidad,
  mensajería, llamadas y voz, sync, persistencia, migración, telemetría y guardas de seguridad.
- **Backend legacy** (CustomUIPage + .ui): es el de producción.
- **Backend Noesis**: experimental, apagado por defecto, y solo se construye si el spike F1 da
  go (D-1).
- **Compatibilidad**: 3.0 rompe v1 y v2, sin adaptadores (D-3).

## 2. Fuera de alcance
- Adaptadores v1/v2 (D-3).
- Los 10 consumidores de las olas 2–4 (D-4); quedan fijados a `<3.0.0` (D-8).
- El design system Dust y el rediseño de temas. Dust queda aparcado y los temas se reorientan al
  explorar Noesis (D-9, D-22).
- Unificar el arte en el jar: Phonemod-Assets sigue siendo un pack separado (D-10).
- Paridad píxel a píxel (D-11).

## 3. Requisitos

### R-API — superficie pública (`phonemod-api`)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-API-1 | La API pública no importa ningún tipo de UI de Hytale | `bin/check-api-no-ui-imports.sh` falla si `phonemod-api` o `phonemod-core` importan `com.hypixel.hytale.server.core.ui.*`, `…server.core.modules.ui.*`, `…protocol.packets.interface_.*`, `InteractiveCustomUIPage` o `PageManager` | S |
| R-API-2 | Se eliminan `PhoneApp`, `StatefulPhoneApp`, `PhoneUi`, `PhoneAppContext`, `LegacyRooms`, `DeviceApp` v2 y `AppService.registerLegacyApp` (D-3) | `ApiContractTest` comprueba por reflexión que esos FQN no existen en el jar de la API | C |
| R-API-3 | Contrato de app: `DeviceApp<M>` con `descriptor()`, `createModel(ctx)`, `onOpen(ctx,m)`, `onAction(ctx,m,action)` y `onClose(ctx,m)` por defecto. La app nunca recibe un builder ni un selector (D-5) | `ApiContractTest` fija las firmas; una app de ejemplo compila solo contra `phonemod-api` | C |
| R-API-4 | ViewModel con campos tipados `Prop<T>` y `ListProp<K,Item>` (clave estable) declarados en la clase del modelo, sin reflexión. El set marca el campo como sucio (D-16) | Tests de contrato: un set con el mismo valor no ensucia; un set distinto produce exactamente un cambio; las listas se identifican por clave | C |
| R-API-5 | `DeviceAction` = id de comando + mapa de parámetros primitivos. La app valida la semántica (D-18) | `ApiContractTest`: DeviceAction solo admite String, Integer, Long, Double y Boolean | C |
| R-API-6 | `DeviceAppContext`: `player()`, `identity()`, `sessionId()`, `deviceType()`, `address()` no Optional (las sesiones compartidas tienen address sintética estable, D-14), `refresh()`, `onWorld(Consumer<M>)`, `sync(SyncChannelType<S>)`, `notify()` y `state()` | `ApiContractTest` fija las firmas | C |
| R-API-7 | `AppDescriptor` lleva id, nombre, permisos (todos), visibility, deviceTypes, isSystem, sortOrder y `views`, un mapa Backend → ViewRef. Una app que solo declara legacy funciona en cualquier backend gracias al fallback | Tests de contrato sobre el builder del descriptor y la resolución del fallback | C |
| R-API-8 | El DeviceType es un tipo de primera clase (`phone`, `pc` o extensible desde el asset). Se elimina la clave de estado `__deviceType__` | grep-gate: `__deviceType__` no aparece en el código 3.0 | S |

### R-CORE — dominio neutral (`phonemod-core`)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-CORE-1 | El core contiene los paquetes que hoy son puros (identity, messaging, calls y voz, sync, persistence, migration, telemetry, security, config, components) y compila sin la UI de Hytale | `check-api-no-ui-imports.sh` en verde sobre core | S |
| R-CORE-2 | Se cierra la fuga `CallRegistry.resolvePolicy` (`instanceof DevicePage`, CallRegistry.java:212): la política de audio se obtiene de la sesión o el shell a través de un puerto del core | grep-gate más un test headless de resolución de política | S+H |
| R-CORE-3 | `PhoneMod.config()` se sustituye por un puerto de configuración inyectado en el core | Ninguna clase del core referencia `esq.phonemod.PhoneMod` | S |
| R-CORE-4 | Las sesiones se identifican por `DeviceSessionId`, no por número de teléfono. Los devices compartidos se registran online igual que los phone (D-14) | Test headless: abrir una sesión compartida la registra y refresh, sync y push la alcanzan | H |
| R-CORE-5 | El app id se normaliza (alias → id canónico) antes de guardarse en la sesión. Así se corrige la comparación de refresh (DevicePage.java:359 vs :414) | Test headless: abrir por alias y refrescar por id canónico repinta | H |
| R-CORE-6 | Los 25 tests de 2.x pasan tras la extracción, con renombres mecánicos como mucho (D-15) | `bin/check-test-renames.sh` (D-21) más la suite en verde | S+H |
| R-CORE-7 | `AppCatalogService` y `AppInstallService` trabajan sobre `AppDescriptor`, no sobre `PhoneApp` | Compilan en core; tests headless de catálogo (R-PAR-AS-*) | H |
| R-CORE-8 | No hay acceso al Store fuera del hilo del mundo. Se corrigen `openShared` (:41), `devices.contacts()` (:51-56) y `ctx.state()` | Assert de hilo en los accesos; un test headless lanza fuera de hilo | H |

### R-KIT — capa de abstracción compartida (`phonemod-ui-kit`)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-KIT-1 | El motor de diff convierte los cambios del ViewModel en operaciones neutrales (SetProp, InsertItem, RemoveItem, UpdateItem) por clave estable | Tests de contrato: secuencias de cambios producen operaciones mínimas y ordenadas | C |
| R-KIT-2 | `FakeBackend` registra las operaciones y permite testear apps sin servidor | Las apps nativas tienen tests sobre FakeBackend (R-APP-2) | C |
| R-KIT-3 | Las guardas existentes se reutilizan sin cambios en el pipeline de acciones: UiEventDecoder, UiEventValidator, TokenBucket, AppOpenAuthorizer y las comprobaciones de sesión y página (D-20). Las acciones rechazadas no llegan a la app | Los tests existentes de security siguen verdes; un test de contrato demuestra que una acción forjada, una de sesión ajena o una que excede el rate no llega a `onAction` | C |
| R-KIT-4 | Router neutral: los handlers devuelven intenciones (abrir app, home, open_chat…), no builders | Tests headless de rutas (R-PAR-SHELL-15/16) | H |
| R-KIT-5 | `refresh()` es seguro desde cualquier hilo: encola un salto al hilo del mundo y coalesce los refrescos repetidos. Modelo y estado lanzan una excepción fuera del hilo del mundo (D-19) | Tests de contrato: refresh desde otro hilo produce un único diff en el hilo del mundo; un set fuera de hilo lanza | C |

### R-LEG — backend legacy (`phonemod-ui-legacy`)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-LEG-1 | DevicePage se reescribe sobre el ui-kit: traduce operaciones a UICommandBuilder mediante el `LegacyView` de cada app (D-17) | Las apps nativas funcionan con el exit pass V3 | G |
| R-LEG-2 | `LegacyView` es tipado contra los `Prop` y se valida en el boot. Un binding a un selector o plantilla inexistente bloquea el registro de la app con un log claro, sin tumbar el server | Test headless de validación más `bin/check-ui-docs.sh` en verde | H+S |
| R-LEG-3 | `LegacyRenderHook` existe solo en ui-legacy, como vía de escape para pantallas que un binding no puede expresar | `check-api-no-ui-imports.sh` (el hook no está en la API) | S |
| R-LEG-4 | Los .ui actuales (Dust*.ui, DustLib) se reutilizan sin cambios (D-22) | `git diff v2.0.0-alpha.13 -- '*.ui'` solo añade ficheros o cambia los que el binding necesita, y cada cambio se justifica en el PR | S |
| R-LEG-5 | Los pushes (llamada entrante, saliente, respuesta, fin, mensaje entrante, publish de sync) pasan por el diff, no por builders ad hoc | Exit pass V3 (R-PAR-SHELL-19..25) | G |
| R-LEG-6 | `lang-audit` en verde y la Version del pack sube en cada cambio de arte | Gates `lang-audit` y `assert_pack_version_moved` | S |

### R-SYNC — sync tipado (D-23)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-SYNC-1 | `ctx.sync(SyncChannelType<S>)` expone el `phone.sync` existente (codec, PERSISTENT/EPHEMERAL, ChannelStore con .bak, tope) | ChannelStorePersistenceTest y SyncChannelStateTest en verde | H |
| R-SYNC-2 | Publicar llama a `refresh()` de cada suscriptor con la app abierta, phone o compartido (D-14) | Test headless con dos sesiones, una compartida | H |
| R-SYNC-3 | Se eliminan `LegacyRooms` y el `stringChannels` en memoria de v2 (RuntimeDevicePlatform.java:30) | grep-gate | S |
| R-SYNC-4 | El cierre de app, de página o una desconexión desuscribe (R-PAR-GC-5, SHELL-30) | Test headless | H |

### R-PAR — paridad funcional con 2.x (D-11)
La paridad es el inventario de `research/R-PAR-DRAFT.md`, aceptado completo como requisito. Los
ids se conservan: R-PAR-SHELL-1..30, CALLS-1..15, WG-1..12, CON-1..6, SET-1..7, AS-1..9 y
GC-1..5. Cada uno lleva su verificación H/G del borrador. Las excepciones son:
- **R-PAR-SET-7** (estilos DustShellTab): se cumple automáticamente mientras los .ui no cambien (D-22).
- **R-PAR-SHELL-26/27** (temas): se conserva el comportamiento actual (D-22).

Bugs de 2.x que 3.0 corrige (D-12); cada uno sustituye al comportamiento roto:
| id | requisito correcto | verif. |
|---|---|---|
| R-PAR-FIX-1 | Tras colgar, el teclado y los Call-back de la pantalla Calls funcionan sin volver a Home | H+G |
| R-PAR-FIX-2 | Las pantallas y el historial de llamada muestran el nombre del contacto si existe, nunca "N (N)" | H |
| R-PAR-FIX-3 | Una sola etiqueta "Calling…" en el push y al reabrir | H |
| R-PAR-FIX-4 | Un mensaje entrante con otra app abierta no duplica la raíz de la app (lo garantiza el diff) | H |
| R-PAR-FIX-5 | Install, hidden, Available y Restricted evalúan todos los permisos, igual que open | H |
| R-PAR-FIX-6 | Un nombre de contacto demasiado largo produce un error visible y el formulario conserva lo escrito | H+G |
| R-PAR-FIX-7 | Una llamada entrante con el teléfono cerrado (requireOpenDevice=false) avisa con toast y ringtone | G |
| R-PAR-FIX-8 | Enviar un mensaje reproduce el sonido `MessageSent` salvo con el perfil Silent | G |

Exit pass: `docs/v3/IN_GAME_EXIT_PASS_V3.md` se genera desde los R-PAR marcados G, con los
comandos reales `/deviceframework migration …` (D-13).

### R-APP — apps portadas (D-4)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-APP-1 | Calls, Whatgram, Contacts, Settings, AppStore y GroupChat se portan a `DeviceApp<M>` con `LegacyView` | Cumplen sus R-PAR-* | H+G |
| R-APP-2 | Cada app nativa tiene tests headless sobre FakeBackend para sus R-PAR marcados H | Suite verde | H |
| R-APP-3 | hello-device-app pasa a ser el ejemplo canónico de 3.0 | Compila solo contra phonemod-api y arranca en ENABLED | S |
| R-APP-4 | Un caso async real (por ejemplo, una carga simulada) repinta con `ctx.refresh()` | Test headless más un paso in-game | H+G |
| R-APP-5 | tuiter-app se porta a 3.0 con `ctx.sync` en lugar de LegacyRooms, y su manifest fija `>=3.0.0-alpha.1` (D-8) | Arranca en ENABLED con 3.0; prueba in-game multijugador en hytest | G |
| R-APP-6 | consumer-smoke de 3.0 y `MIGRATION_V2_TO_V3.md` escrito a partir del port de tuiter | El smoke arranca en ENABLED y la guía cubre cada símbolo eliminado en R-API-2 | S |

### R-NOE — backend Noesis (provisional; depende de Q-1..Q-4 del spike F1)
| id | requisito | aceptación | verif. |
|---|---|---|---|
| R-NOE-1 | Todo lo que toca Noesis vive en `phonemod-ui-noesis` y solo se activa con `-Dhytale.serverside_ui_preview` | Boot en ENABLED con el flag apagado y encendido | S |
| R-NOE-2 | Los tipos `Server.Phonemod.*` llevan la versión en el nombre y se registran una sola vez en `setup()` | Test de arranque | S |
| R-NOE-3 | Los comandos Noesis pasan por las mismas guardas de R-KIT-3 antes de llegar a la app | Test de contrato con un FakeBackend Noesis | C |
| R-NOE-4 | El adaptador tiene una aserción propia del hilo del mundo (UISurface no la tiene) | Test headless | H |
| R-NOE-5 | Un backend por sesión de dispositivo, nunca mezclados (un solo slot de página) | Test headless del selector | H |
| R-NOE-6 | El rediseño de temas se aborda aquí (D-22) | Se fija tras el spike | — |

## 4. Matriz de trazabilidad (resumen; se completa en PLAN.md)
| grupo | verificación dominante | fase |
|---|---|---|
| R-API-1..8 | C / S | F2 (rojo) → F4 (verde) |
| R-CORE-1..8 | S / H | F3 |
| R-KIT-1..5 | C / H | F2 (rojo) → F4 |
| R-LEG-1..6 | H / S / G | F5 |
| R-SYNC-1..4 | H / S | F3–F4 |
| R-PAR-* (incl. FIX-1..8) | H / G | F5–F6 |
| R-APP-1..4 | H / G | F6 |
| R-APP-5..6 | G / S | F7 |
| R-NOE-1..6 | S / C / H | F8 (solo con go) |
