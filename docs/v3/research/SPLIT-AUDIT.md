# Auditoría de separación api / core / ui (2.0.0-alpha.13)

Generado 2026-10-15 por un explorador de solo lectura sobre `feature/v2-refactor` (dc9b931).
Materia prima para SPEC §R-API, §R-CORE, §R-SYNC. R = `src/main/java/esq/phonemod`,
A = `phonemod-api/src/main/java/esq/phonemod`.

## Correcciones al plan (rev 19)
- Hay **25** clases de test (no 26), todas en `src/test/java`.
- El prefijo **"PC-" no existe en Phonemod**: solo en consumidores (computers lo genera en
  OpenComputerInteraction.java:174; los checks de trading-app TradingApp.java:127-130 y relay-ops
  RelayOpsApp.java:88-90 son código muerto porque el session id acaba siendo un UUID).
- `DeviceAppContext.sync()` es un stub: escribe en un mapa en memoria que nadie lee.
- El dominio ya toca la UI: `CallRegistry.java:212` hace `instanceof esq.phonemod.device.ui.DevicePage`
  para leer la política de audio; `RuntimeDevicePlatform.openShared` abre una custom page (:45-47).

## 1. Tipos de phonemod-api con UI
Con UI: `api/DeviceApp` (UICommandBuilder :3, UIEventBuilder :4; build :10-11), `phone/api/PhoneApp`
(:3-4), `phone/api/PhoneUi` (CustomUIEventBindingType :3, EventData :5, builders :6-7),
`phone/api/StatefulPhoneApp` (:3), y `api/AppService` indirectamente (`registerLegacyApp(PhoneApp)` :8).
Sin UI: AppDescriptor, AppRegistration, CallingService, DeviceAction, DeviceActionResult,
DeviceAppContext (PlayerRef), DeviceIdentity, DevicePlatform, DeviceService (Ref/Store/EntityStore),
Messaging/Notification/Presence/SyncService, WorldAccess, api/event/* (IEvent), PhoneAppContext
(expone getContentSelector), PhoneAssetPaths, PhoneEventActions, PhoneEvent, phone/sync/*.
`PhoneAppSessionState` (componente ECS persistido) vive en el módulo api.

## 2. Paquetes del runtime
| paquete | clasificación | qué separar |
|---|---|---|
| (root) PhoneMod | raíz de composición | clases de dominio llaman `PhoneMod.config()` (DeviceSession, ConversationHistoryComponent, CallHistoryComponent, OfflineMailboxStore, CallRegistry) → core necesita un puerto de config |
| commands, config, device.api, device.assets, device.components, device.services, identity, migration, phone.animation, phone.components, phone.sync(+room), security | puro | DevicePageHandle (device.api) es el puerto de callback; refreshApp :42 |
| device.core | mixto | DeviceService:20/95 (createDevicePage); DeviceSession:8,28,94-99 (currentApp PhoneApp) |
| device.events | puro respecto al veto (PhoneNotification usa protocol NotificationStyle; PhoneNotifications usa PhoneAppContext) | — |
| device.routing (+guards, handlers, resolvers) | acoplado a UI | RouteResult:3-4, RouterHost:5-10,59, RouteContext:29,72, handlers → todo el paquete a ui-kit |
| device.theme | UI | ThemeService:5 |
| device.ui | UI | DevicePage |
| operations | casi puro | AssetHealthService:60 usa PhoneApp.getSupportedDeviceTypes |
| phone.apps | UI | las 6 apps + helpers/Calls; SettingsApp:480 `instanceof DevicePage` |
| phone.apps.groupchat, helpers/Contacts, helpers/Whatgram | puro | groupchat lo usan los tests: mantener paquete en core |
| phone.appstore | mixto | AppCatalogService:4, AppInstallService:10 usan PhoneApp → versión por AppDescriptor |
| phone.commands | casi puro | AdminUninstallCommand:15,51 |
| phone.core | acoplado | PhoneAppRegistry:3, PhoneService:4 guardan PhoneApp → registro de descriptores en core + registro de apps en ui-kit |
| phone.interactions | UI | OpenPhone:20,135 (openCustomPage) |
| phone.messaging | mixto (1 fuga) | CallRegistry.resolvePolicy :209-215 |
| platform | mixto | DeviceAppAdapter (UI), RuntimeAppService:8,10 + LegacyFacade :67-68, RuntimeDevicePlatform.openShared :45-47 |
| setup | raíz de composición | SetupManager importa las 6 apps; InteractionSetupManager registra OpenPhone |
| telemetry | puro, atado a PhoneMod y RuntimeDevicePlatform | — |

## 3. DeviceAppContext v2 (A/api/DeviceAppContext.java → R/platform/RuntimeDeviceAppContext.java)
| método | impl | estado |
|---|---|---|
| player() :9 | :18 | real |
| identity() :10 | :19-28 | real, pero con fallback inventado no persistido (nameUUIDFromBytes) |
| address() :11 | identity().optionalAddress() | vacío en devices compartidos/PC (IdentityRegistry.java:158-171) |
| executeOnWorld :12 | :29-35 | real |
| state(k) / state(k,v) :13-14 | :36-37 → PhoneAppContext.java:115-143 | real, **sin guardia de hilo** |
| notify :15 | :38 NotificationUtil | real; se salta preferencias y sonido de PhoneNotifications |
| sync() :16 | RuntimeDevicePlatform.sync :76-87 | **STUB** en memoria, sin subs/persistencia/broadcast |

Contexto nuevo en cada llamada (DeviceAppAdapter.java:34,38,43,47).

Por qué un app v2 no puede repintarse tras async (AppService.refreshApp :38 → RuntimeAppService.java:93-101 → DevicePage.refreshApp :629-656):
1. El contexto no expone app id, device id ni hook de refresh.
2. Devices compartidos/PC: session deviceId = identity UUID (RuntimeDevicePlatform.java:40-44) y address() vacío.
3. Devices compartidos nunca se registran online (solo OpenPhone.java:127 llama PhoneRegistry.register) → refreshApp, SyncService.broadcast y CallRegistry.notifyPage no hacen nada.
4. Alias: refreshApp compara con `session.getCurrentAppId()` (:633-636) que guarda el id pedido sin resolver (:414 vs :359).
5. Sin alternativa por señal: sync() no difunde.
6. Riesgo: ctx.state() desde el hilo async = acceso al Store sin guardia.

## 4. Detección de PC
- "PC-": ver correcciones arriba (muerto).
- `__deviceType__`: escrito en DP:426; constante AppStoreApp.java:367; leído AppStoreApp.java:359-364 y computers OpsConsoleApp.java:280-282; persistido en PhoneAppSessionState.
- `getSupportedDeviceTypes`/`supportedDeviceTypes`: A PhoneApp.java:166, AppDescriptor.java:19,35,41; checks DP:362-364,373,441-452, AppOpenAuthorizer.java:44,53,73-77, AppCatalogService.java:156-166, AssetHealthService.java:60-61, DeviceAppAdapter.java:30, RuntimeAppService.java:46. Tipo desde el asset `DeviceType` (DeviceAsset.java:43,151 → DeviceShell.java:38,70): "phone" (Phone.json:3), "pc" (computers PC.json:3).

## 5. Hilos
Saltos al hilo del mundo: PR:108, PR:256, CR:298,309,318, SyncService.java:201, RuntimeAppService.java:99, RuntimeDevicePlatform.java:46, RuntimeDeviceAppContext.java:32, OpenPhone.java:131, PhoneAnimationController.java:78, AppsCommand:67, AdminInstallCommand:73, AdminUninstallCommand:79.
Fuera de hilo: watcher de timeout en HytaleServer.SCHEDULED_EXECUTOR (CR:59); completions IO (SyncService.java:177, IdentityRegistry.java:287, OfflineMailboxStore.java:131); ChannelStore.save (:75-113).
Riesgos: openShared lee el Store antes del salto (:41); devices.contacts() (:51-56) sin salto; ctx.state() sin assert; CallRegistry.resolvePolicy lee estado de página desde cualquier hilo; PhoneRegistry.register (:93) seguro solo por el llamante.

## 6. Persistencia
| qué | dónde | schema |
|---|---|---|
| DeviceSettingsComponent "DeviceSettings" | EntityStore del jugador | v2 |
| PhoneOwnerComponent | jugador | v2 (contactos por device) |
| ConversationHistoryComponent "PhoneConversationHistory" | jugador | v2 |
| CallHistoryComponent "PhoneCallHistory" | jugador | v2 |
| PhoneAppSessionState (clase en api) | jugador | v2 (k/v por device+app, incl. `__deviceType__`) |
| DeviceInstalledAppsComponent | jugador | v2 |
| IdentityRegistry → `<universe>/Phonemod/device-identities.json` | escritura async ordenada IdentityRegistry.java:286-287 | CURRENT_SCHEMA=2 |
| OfflineMailboxStore → `<universe>/Phonemod/mailboxes/<sha256(address)>.json` | :44,139 | v2 |
| ChannelStore → `<pluginData>/channels/<typeId>/<sha256(id)>.json` + .bak | ChannelStore.java:60,67 | por codec (GroupChatState v2, RoomState v2); PERSISTENT rehidratados, EPHEMERAL borrados |
| PhonemodConfig | withConfig PhoneMod.java:34, saveConfig :66-71 | — |
| Metadata del item | OpenPhone.java:114-117 | address + identity UUID |
| `stringChannels` v2 | RuntimeDevicePlatform.java:30 | solo memoria |
Migración: PlayerDeviceDataMigrator.remap (:20-34) re-clava los 6 componentes; llamada en OpenPhone.java:111-112.

## 7. Tests (25 clases, compiladas vía phonemod-runtime/pom.xml:19)
ApiContractTest(2), CallAudioModeTest(2), PhonemodConfigTest(4), DeviceAssetDefaultsTest(1), DeviceSettingsTest(2), DeviceShellReloadTest(3, reflexión :82), IdentityRegistryPersistenceTest(4), IdentityRegistryTest(5, constructores package-private), MigrationComponentTest(5), PhoneEventTest(4), **SettingsUiContractTest(1, lee DustSettings.ui → va con los recursos UI)**, CallRestoreDecisionTest(6), CallSessionTest(9), VoiceRelayDecisionTest(7), VoiceRouteSnapshotTest(5), OfflineMailboxOrderedSaveTest(3, importa PhoneMod), PhoneRegistryPreferencesTest(1), ChannelStorePersistenceTest(8, reflexión sobre SyncService.store), SyncChannelStateTest(4), AppOpenAuthorizerTest(10), TokenBucketActionGuardTest(1), TokenBucketTest(2), UiEventDecoderTest(2), UiEventValidatorTest(2), RequiredRegistrationTest(3).
Restricciones para dejarlos intactos: mantener nombres de paquete en core; `esq.phonemod.PhoneMod` (o `PhoneMod.config()`) alcanzable desde core; SettingsUiContractTest junto a los .ui.
