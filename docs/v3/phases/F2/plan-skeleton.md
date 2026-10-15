# F2-1 — Plan del esqueleto 3.0 y de los tests de contrato en rojo

Autor: hytale-architect, 2026-10-15. Guardado tal cual lo devolvió, con las decisiones de
usuario aplicadas en la sección "Respuestas" cuando estén. Las citas apuntan a
`feature/v2-refactor` @ dc9b931 y a hytale-shared-source `release` 0033ae473.

## 1. Ubicación y colisión con v2
- **Tipos de API 3.0:** paquete temporal `esq.phonemod.api.v3`, dentro de `phonemod-api`.
  Incluye todos los tipos nuevos o sustituidos, colisionen o no con v2.
- **ui-kit:** módulo nuevo `phonemod-ui-kit` con paquetes finales `esq.phonemod.ui.kit.*`.
- **Test de R-KIT-3:** en el árbol de tests del runtime, paquete `esq.phonemod.ui.kit.action`,
  porque ahí ya son visibles las clases de security.
- **No se crea `phonemod-core`;** eso es F3.

Por qué:
- Los tipos v2 mantienen sus FQN hasta F4: DeviceApp `api/DeviceApp.java:8`, AppDescriptor
  `:10`, DeviceAction `:9`, DeviceAppContext `:8`.
- Ninguna clase existe dos veces en el classpath. La alternativa de FQN finales en otro módulo
  daba dos `esq.phonemod.api.DeviceApp`.
- Promoción en F4:
  1. Borrar los v2 que colisionan.
  2. `git mv api/v3/*.java api/` (main y test).
  3. `sed -i 's/esq\.phonemod\.api\.v3\b/esq.phonemod.api/g'`.
  4. Gate: `grep -r esq.phonemod.api.v3` debe dar 0.
- Los tipos v2 que sobreviven se referencian en su FQN actual, sin copiarlos:
  - `esq.phonemod.api.DeviceIdentity` (`DeviceIdentity.java:9`)
  - `esq.phonemod.phone.sync.SyncChannelType` (`:19`)
  - `ChannelPersistence` (`:17`)
  - `MemberAccessor` (`:11`)
- El handle de sync se llama `SyncHandle`, no `SyncChannel`, porque ya existe
  `esq.phonemod.phone.sync.SyncChannel` (`SyncChannel.java:21`).

## 2. Esqueleto
Todo cuerpo hace `throw new UnsupportedOperationException("F4")`, también los constructores
compactos de los records. Solo se libran las interfaces y las constantes de enum.

### 2a. `phonemod-api/src/main/java/esq/phonemod/api/v3/`
- **`interface DeviceApp<M extends ViewModel>`**
  - `AppDescriptor descriptor()`
  - `M createModel(DeviceAppContext<M>)`
  - `void onOpen(DeviceAppContext<M>, M)`
  - `void onAction(DeviceAppContext<M>, M, DeviceAction)`
  - `default void onClose(DeviceAppContext<M>, M) {}`
  - Todos los parámetros y retornos llevan `@Nonnull`.
- **`interface DeviceAppContext<M extends ViewModel>`**
  - `PlayerRef player()` (`PlayerRef.java:54`)
  - `DeviceIdentity identity()`
  - `DeviceSessionId sessionId()`
  - `DeviceType deviceType()`
  - `String address()`
  - `void refresh()`: cualquier hilo
  - `void onWorld(Consumer<M>)`: cualquier hilo
  - `<S> SyncChannels<S> sync(SyncChannelType<S>)`
  - `void notify(String title, String body)`: `notify()` sin argumentos es ilegal en Java,
    porque `Object.notify()` es final (comprobado con verify_snippet)
  - `AppState state()`: solo en el hilo del mundo
- **`abstract class ViewModel`**
  - `protected ViewModel()`
  - `protected final <T> Prop<T> prop(String name, T initial)`
  - `protected final <K,I> ListProp<K,I> list(String name, Function<? super I,? extends K> keyOf)`
  - `public final List<ModelField> fields()`: en orden de declaración
  - `public final void attach(ModelGuard)`: una segunda llamada lanza IllegalStateException
  - Sin reflexión: los campos se registran desde los inicializadores.
- **`sealed interface ModelField permits Prop, ListProp`**: `name()`, `dirty()`, `markClean()`.
- **`@FunctionalInterface ModelGuard`**: `void checkAccess()`.
- **`final class Prop<T>`**: `name`, `get`, `set` (no marca sucio si `Objects.equals`), `dirty`,
  `markClean`.
- **`final class ListProp<K,I>`**
  - `items()`: inmodificable
  - `find(K)`
  - `put(I)`: upsert por clave; una clave nueva se añade al final
  - `insert(int, I)`: lanza IAE si la clave ya existe
  - `remove(K)`
  - `setAll(List)`: lanza IAE con claves duplicadas; no marca sucio si es igual
  - `keyOf`, `size`, `dirty`, `markClean`
- **`record DeviceAction(String command, Map<String,Object> params)`**
  - Constante `ALLOWED_TYPES` = String, Integer, Long, Double y Boolean.
  - `of(...)` y accesores estrictos que devuelven Optional: `string`, `integer`, `longValue`,
    `decimal`, `bool`.
  - Validación del constructor (F4):
    - command con el patrón `[a-z0-9_.:-]{1,64}` (v2 `DeviceAction.java:11`)
    - como máximo 32 params
    - claves no vacías de hasta 64 caracteres
    - Strings de hasta 1024 caracteres
    - copia inmutable
- **`record AppDescriptor`**
  - Campos: `id`, `aliases`, `name`, `iconPath`, `permissions`, `visibility`, `deviceTypes`,
    `system`, `sortOrder` y `Map<Backend,ViewRef> views`.
  - `builder(id)`
  - `supports(DeviceType)`: un conjunto vacío equivale a todos (`AppDescriptor.java:40-42`)
  - `viewFor(Backend)`: gana la vista explícita; si no, LEGACY; si no, vacío
  - El patrón del id viene de `AppDescriptor.java:22`.
  - El Builder tiene un setter por campo, más `permissions(Collection)` y `view(Backend, ViewRef)`.
- **`enum AppVisibility { LOCKED, HIDDEN }`**: los strings "locked"/"hidden" de v2
  (`PhoneApp.java:146-154`).
- **`enum Backend { LEGACY, NOESIS }`**
- **`interface ViewRef`**: `document()` y `static of(String)`.
- **`record DeviceType(String id)`**: constantes PHONE y PC; `of()` exige minúsculas con el
  patrón `[a-z0-9_.-]{1,32}` (`DeviceAsset.java:43,151`).
- **`record DeviceSessionId(UUID value)`**: `random()`, `parse()`, `toString()`.
- **`interface AppState`**: `get`, `put`, `remove`. Solo en el hilo del mundo.
- **`interface SyncChannels<S>`**: `type`, `channel(id)`, `createRoom`, `joinRoom`,
  `leaveRoom`. Sustituye a LegacyRooms (`LegacyRooms.java:32-47`).
- **`interface SyncHandle<S>`**: `id`, `state`, `update(Consumer<S>)`, `subscribe`,
  `unsubscribe`. Misma semántica que `SyncChannelHandle.java:35-54`, pero indexado por sesión.

### 2b. `phonemod-ui-kit/src/main/java/esq/phonemod/ui/kit/`
- **`diff`**
  - `sealed DiffOp { String field(); }` con los records `SetProp(field, value)`,
    `InsertItem(field, index, key, item)`, `RemoveItem(field, key)` y
    `UpdateItem(field, key, item)`.
  - `DiffEngine.diff(ViewModel)`:
    - El primer diff emite el estado completo; después, solo cambios por clave.
    - Llama a `markClean()` al terminar.
    - Orden canónico: campos en orden de declaración; dentro de una lista, Remove (orden antiguo),
      luego Insert (índice ascendente), luego Update.
    - Un reordenamiento se expresa como remove + insert.
  - `DiffEngine.reset()`
- **`host`**
  - `WorldThread { isInWorldThread(); execute(Runnable); }`. El legacy usará
    `World.isInThread()` (`TickingThread.java:314`) y `World.execute` (`World.java:849`;
    `World implements Executor` en `:81`).
  - `RenderBackend { backend(); open(s, app, view); apply(s, ops); close(s); }`
  - `DeviceSessionInfo(id, owner, Supplier<PlayerRef>, identity, deviceType, address)`. El
    PlayerRef va detrás de un Supplier porque no se puede construir en tests (`PlayerRef.java:199`).
  - Puertos: `Notifier`, `SyncGateway`.
  - `HostServices(backend, world, state, notifier, sync)`
  - `AppHost<M>`:
    - `open(...)`, `context()`, `model()`
    - `deliver(action)`: en el hilo del mundo; llama a `onAction` y luego vuelca el diff
    - `refresh()`: desde cualquier hilo; se coalesce en un único `world.execute(flush)`
    - `close()`, `isOpen()`, `sessionId()`, `appId()`
- **`action`** (R-KIT-3; las guardas no cambian)
  - `ActionGuards`, con un método por guarda real:
    - `sessionAuthorized`: `DevicePage.isAuthorizedSession:512-518` y `DeviceSession.isOwnedBy:70`
    - `tryAcquire`: `DeviceSession.tryAcquireAction:74` sobre `TokenBucket.tryAcquire:28`
      (constructor `TokenBucket(double, int)` en `:13`)
    - `validateRaw`: `UiEventValidator.validateRaw:22`
    - `decode`: `UiEventDecoder.decode:21`
    - `validateFields`: `UiEventValidator.validateFields:30`
    - `authorizeOpen`: `AppOpenAuthorizer.authorize:43` (Decision en `:23`)
  - `GuardVerdict(allowed, reason)`, con la forma de `ValidationResult:84`.
  - `InboundEvent(session, actor, rawPayload)`
  - `ActionOutcome`: `Delivered` o `Rejected`.
  - `ActionPipeline(ActionGuards)`: `dispatch`, `authorizeOpen`, `openIfAuthorized`; constantes
    `ENVELOPE_ACTION="Action"` y `ENVELOPE_APP="App"`.
  - Orden fijado en F4 según DevicePage `:176-203`:
    1. sesión → `session_not_authorized`
    2. rate → `rate_limited`
    3. validateRaw
    4. decode → `malformed_event` (string nueva)
    5. validateFields
    6. App distinto del abierto → `stale_app_state`
    7. entrega: `DeviceAction(Action, campos sin Action/App)`
- **`testing`** (en main)
  - `FakeBackend`: `batches`, `ops`, `openedView`, `isOpen`, `applyThreads`, `clear`.
  - `FakeWorldThread`: un hilo llamado `"fake-world"`; `runAndWait`, `drain`, `close`.
  - `FakeHost`: `defaultSession`, `open`, `notifications`. El SyncGateway lanza
    `"F4: sync needs core"`.

## 3. Tests de contrato
Todos llevan `@Tag("contract")`, y todo fallo intencionado contiene el marcador `F4`. Son 60:
3 en verde (fijan firmas) y 57 en rojo.

**ApiContractTest** (api, `esq.phonemod.api.v3`)
| @Test | R-xx | F2 |
|---|---|---|
| removedV2TypesAreAbsent | R-API-2 | ROJO |
| v2DeviceAppAndLegacyRegistrationAreGone | R-API-2 | ROJO |
| deviceAppSignatureIsPinned | R-API-3 | VERDE |
| noApiMethodExposesUiTypes | R-API-3, R-API-1 | VERDE |
| contextSignatureIsPinned | R-API-6 | VERDE |
| actionAcceptsOnlyPrimitiveWrappers | R-API-5 | ROJO |
| actionIsImmutableAndValidated | R-API-5 | ROJO |
| actionTypedAccessorsAreStrict | R-API-5 | ROJO |
| sampleAppCompilesAgainstApiOnly | R-API-3 | ROJO (fixture `HelloContractApp`) |

**ViewModelContractTest** (api): todos ROJO.
- R-API-4: setWithSameValueDoesNotDirty, setWithDifferentValueDirtiesExactlyThatField,
  fieldsAreListedInDeclarationOrder, listPutWithExistingKeyReplacesNotDuplicates,
  listIdentityIsTheKeyNotTheIndex, listSetAllRejectsDuplicateKeys, markCleanClearsDirty
- R-API-4 y R-KIT-5: attachedGuardRunsOnEveryReadAndWrite
- R-KIT-5: guardCanOnlyBeAttachedOnce

**AppDescriptorContractTest** (api): todos ROJO.
- R-API-7: builderRequiresNamespacedId, builderKeepsAllPermissions,
  builderDefensivelyCopiesCollections, defaultsAreLockedNonSystemAllDevices,
  legacyOnlyViewResolvesForEveryBackend, backendSpecificViewWinsOverLegacyFallback,
  noViewsResolvesEmpty
- R-API-7 y R-API-8: supportsUsesDeviceTypeEquality

**DeviceTypeContractTest** (api): todos ROJO.
- R-API-8: builtinConstantsMatchAssetIds, assetDefinedTypeIsValueEqual, invalidIdRejected
- R-API-6: sessionIdRoundTripsThroughString

**DiffEngineContractTest** (ui-kit, `diff`): todos ROJO, R-KIT-1.
- firstDiffEmitsFullStateInDeclarationOrder
- secondDiffWithoutChangesIsEmpty
- singlePropChangeEmitsOneSetProp
- samePropValueEmitsNothing
- appendedItemEmitsInsertItemAtIndex
- removedItemEmitsRemoveItemByKey
- changedItemEmitsUpdateItemNotRemoveInsert
- reorderEmitsRemoveThenInsert
- mixedChangesEmitCanonicalOrder
- diffMarksFieldsClean

**FakeBackendContractTest** (ui-kit, `testing`): todos ROJO, R-KIT-2.
- openRecordsViewAndInitialBatch
- actionRecordsOnlyTheDelta
- closeCallsOnCloseAndStopsRecording
- noesisBackendFallsBackToLegacyView (también R-API-7)
- notifyIsRecordedWithoutServer

**ThreadingContractTest** (ui-kit, `host`): todos ROJO, R-KIT-5.
- refreshFromForeignThreadProducesOneBatchOnWorldThread: 10 refresh desde un pool producen
  1 batch, aplicado en el hilo `"fake-world"`
- emptyRefreshAppliesNothing
- modelWriteOffWorldThreadThrows
- modelReadOffWorldThreadThrows
- stateAccessOffWorldThreadThrows
- onWorldMutationRunsOnWorldThreadAndFlushesOnce

**ActionPipelineContractTest** (runtime `src/test/java`, `esq.phonemod.ui.kit.action`): todos
ROJO, R-KIT-3.
- forgedActionNeverReachesApp → `invalid_action`
- oversizedPayloadNeverReachesApp → `payload_too_large`
- foreignSessionNeverReachesApp → `session_not_authorized`
- rateLimitedActionNeverReachesApp → `TokenBucket(0.001, 1)`
- staleAppActionNeverReachesApp → `stale_app_state`
- malformedPayloadNeverReachesApp → `malformed_event`
- guardOrderMatchesDevicePage: sesión ajena + payload enorme → `session_not_authorized`
- validEventDeliveredOnceWithEnvelopeStripped
- unauthorizedOpenNeverCallsCreateModel → `missing_permission`

Usa ActionGuards locales que delegan en las estáticas sin cambios. `UiSafetyLimits`
(`UiSafetyLimits.java:4`) toma los valores por defecto de `PhonemodConfig.java:113-118`.

## 4. Maven
- **Configuración actual de surefire:**
  - `plugins-srcs/pom.xml:51-56`: versión 3.0.0-M5, `forkCount 0`.
  - `phonemod-runtime/pom.xml:84-96`: `forkCount 1`, `reuseForks true`,
    `argLine -Djava.util.logging.manager=com.hypixel.hytale.logger.backend.HytaleLogManager`.
- **`Phonemod/pom.xml`:**
  - Módulos: api, ui-kit, runtime.
  - `pluginManagement` de surefire con `excludedGroups=contract`, más el mismo
    fork/argLine que el runtime.
  - Perfil `contract`: `groups=contract` y `<excludedGroups combine.self="override"/>`.
  - Fallback si M5 no limpia la exclusión: `groups=!contract` por defecto y `groups=contract` en
    el perfil.
- **`phonemod-api/pom.xml`:** JUnit con scope test.
- **`phonemod-ui-kit/pom.xml` (nuevo):**
  - Parent `phonemod-parent`.
  - `hytale.install-dir=${project.basedir}/../../HytaleServer.jar`, igual que
    `phonemod-api/pom.xml:16`.
  - Dependencias: api y JUnit (test).
- **`phonemod-runtime/pom.xml`:** ui-kit con scope **test**. El shade sigue incluyendo solo api
  y telemetry (`:111-116`), así que ui-kit no entra en el jar de F2.

## 5. Scripts (`Phonemod/bin/`)
**`check-api-no-ui-imports.sh`**
- Objetivos: el array `TARGETS`, que los argumentos pueden sustituir. En F2 son `api/v3` (main y
  test) y `phonemod-ui-kit/src/main/java`.
- Patrones prohibidos:
  - `com\.hypixel\.hytale\.server\.core\.ui\.`
  - `…server\.core\.modules\.ui\.`
  - `…protocol\.packets\.interface_\.`
  - `\bInteractiveCustomUIPage\b`: vive en `server.core.entity.entities.player.pages`
    (`InteractiveCustomUIPage.java:26`), no bajo `ui`
  - `\bPageManager\b` (`PageManager.java:24`)
- Se ignoran los comentarios.
- Salida: 0 limpio, 1 con violaciones (`file:line:pattern`), 2 si falta un directorio.
- `--self-test`.

**`check-test-renames.sh`**
1. `BASE` por defecto: `v2.0.0-alpha.13`.
2. Exige exactamente 25 `*Test.java` en `src/test/java` del BASE.
3. Mapea cada uno a su ruta actual: la misma ruta → rename de `git diff -M` → basename único
   (incluye ficheros no commiteados). Si no encuentra ninguno, falla.
4. Normaliza con perl (quita líneas `package`/`import` y los prefijos FQN `esq.phonemod.….`
   delante de mayúscula) y compara con `diff -u`. Cualquier diferencia falla.
5. Lista de excepciones: `bin/test-renames.allow`.
6. `--self-test`.

## 6. Orden de ficheros para el implementer
**No tocar:** `security/*`, los ficheros v2 de la API, los 25 tests y `scripts/assert-release-jar.sh`.
1. Poms: raíz, api, ui-kit (nuevo) y runtime.
2. Tipos `api/v3`: Backend, AppVisibility, DeviceType, DeviceSessionId, ViewRef, ModelGuard,
   ModelField, Prop, ListProp, ViewModel, DeviceAction, AppState, SyncHandle, SyncChannels,
   AppDescriptor, DeviceAppContext, DeviceApp.
3. ui-kit: diff, host, action, testing.
4. Tests:
   - api: HelloContractApp, ApiContract, ViewModelContract, AppDescriptorContract, DeviceTypeContract
   - ui-kit: DiffEngine, FakeBackend, Threading
   - runtime: ActionPipeline
5. Scripts y `test-renames.allow` (vacío).
6. La versión no cambia (Q-F2-10).

## 7. Verificación
1. **Suite por defecto:** `mvn -B test` da exactamente 96 tests (todos del runtime). El resultado
   se juzga por `${PIPESTATUS[0]}`.
2. **Perfil contract:** `mvn -B test -Pcontract -Dmaven.test.failure.ignore=true`:
   - 60 tests: 3 pasan y 57 fallan con el marcador F4.
   - 0 errores de compilación y 0 NoClassDefFoundError.
   - Ninguno de los 96 se ejecuta.
3. **Scripts:**
   - Los dos dan 0 y su `--self-test` también.
   - Una inyección temporal de un import de UI da 1.
   - Cambiar una aserción en una copia da 1.
4. **Jar:** contiene `esq/phonemod/api/v3/*` y no contiene `esq/phonemod/ui/kit/*`.
5. **v2 intacto:** sin diff en `src/main/java`, en los ficheros v2 de la API ni en los 25 tests.
6. **hytale-verify:** el boot llega a ENABLED, porque el contenido del jar cambia.

## 8. Preguntas abiertas → OPEN-QUESTIONS Q-F2-1..11

## Respuestas del usuario (2026-10-15) — obligatorias para el implementer
- D-25: `ViewRef` se queda como está en api; `LegacyView` NO se crea en F2 (es F5, ui-legacy).
- D-26: `DeviceSessionId` sin `random()`. Usar `static DeviceSessionId of(DeviceIdentity device, @Nullable UUID player)`: estable; `player` solo para devices compartidos (`identity.shared()`); derivación UUID v3 (`UUID.nameUUIDFromBytes`) sobre "phonemod-session:" + id del device [+ ":" + player]. Más `parse(String)`/`toString()`. Test nuevo en DeviceTypeContractTest: `sessionIdIsStableForSameDeviceAndPlayer` (R-API-6, R-CORE-4).
- D-27: AppDescriptor tal cual (aliases + iconPath; sin capabilities/uiPath/appButtonUiPath).
- D-28: notify(title, body); ui-kit en TARGETS de check-api-no-ui-imports; rooms por DeviceSessionId (SyncChannels.joinRoom/leaveRoom usan ctx.sessionId(), no address); NO crear DeviceActionResult en v3; versión sin cambiar; añadir `interface AppService` en api/v3 con `AppRegistration registerApp(DeviceApp<?> app)` (AppRegistration v2 en su FQN actual si sirve — verificar; si no, `api.v3.AppRegistration` con `void unregister()`) y `void refreshApp(DeviceSessionId session)`, y un @Test `appServiceSignatureIsPinned` (R-API-9, VERDE) en ApiContractTest.
