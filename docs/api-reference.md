# API Reference (legacy v1)

For new integrations use [V2_API.md](V2_API.md). This page documents the deprecated v1
compatibility surface retained through 2.x.

## PhoneApp\<S\>

`esq.phonemod.phone.api.PhoneApp`

Interface every phone app must implement. `S` is the app's state enum type.

### Required methods

```java
String getId()
```
Returns a unique identifier string for this app (e.g. `"myapp"`). Used as the key in `PhoneAppRegistry` and in `open_app` event payloads. Must be stable across restarts.

```java
String getDisplayName()
```
Human-readable name shown on the app icon label in the home app grid.

```java
String getAppButtonUI()
```
Path to the `.ui` file that defines this app's button in the home grid (e.g. `"Pages/Phone/Components/WhatgramButton.ui"`). The file must declare a root `Group #APPBUTTONENTRY` containing a `TextButton #APPBUTTON` (with the icon baked into its `Default` style, typically via `@PhoneAppButton`'s `@Icon`) and a `Label #APPNAME`. `DevicePage` sets the label text dynamically after appending the file. Third-party plugins should use their own asset namespace (e.g. `"Pages/Playground/AppIcon.ui"`).

```java
String getUIPath()
```
Asset path for the app's root UI file, loaded into `#AppContent` via `appendMainUI()` (e.g. `"Pages/Phone/MyApp.ui"`).

```java
void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb)
```
Builds the current UI state for the given player. Called on initial open and by
framework refresh paths. A handled `handleEvent` must populate the supplied
builders, normally by calling `build(ctx, cmd, evb)` itself. Builds should be
idempotent: rebuilding should produce the same result given the same state.

```java
boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                    UICommandBuilder cmd, UIEventBuilder evb)
```
Handles a player interaction. Return `true` if the event was consumed (the framework sends `cmd`/`evb` as the UI update). Return `false` if unhandled (the framework sends a no-op update and logs the unhandled action).

### Optional methods (default no-op)

```java
default int getSortOrder()
```
Controls sorted registry/catalog views and the order of installed non-system apps
in the home grid. Lower values appear first. System apps retain their
`DefaultApps` declaration order. Equal sort values are ordered by app ID.
Default: `0`.

```java
default void onOpen(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb)
```
Called once when the app is opened, before `build()`. Use to perform one-time setup or play a sound. The `cmd`/`evb` builders passed here are the same ones passed to the subsequent `build()` call.

```java
default void onClose(PhoneAppContext ctx)
```
Called when the player navigates away from this app (home button, or opening another app). Use for cleanup. No builders are provided — any UI changes here are not sent.

```java
default boolean onIncomingMessage(PhoneAppContext ctx, String fromNumber,
                                  UICommandBuilder cmd, UIEventBuilder evb)
```
Called by `DevicePage` when a message arrives while this app is open. Return `true` to send `cmd`/`evb` as the UI update directly — no full rebuild. Return `false` to fall back to `build()`. Use the `true` path to append a single message bubble without clearing and re-rendering the whole chat.

This hook only runs while the app is the visible top-level state; background
message updates do not replace call overlays or framework screens.

```java
default void onIncomingMessage(PhoneAppContext ctx, String fromNumber)
```
Fallback called by `DevicePage` only when the 4-arg overload returns `false`. `build()` is called automatically after this returns. Override this to update state before the rebuild (e.g. mark a message as read). If you override the 4-arg version and return `true`, this method is not called.

```java
default void onIncomingCall(PhoneAppContext ctx, String callerNumber, String callerName)
```
Called by `DevicePage` when an incoming call arrives while this app is open.
Use it to update app state or perform side effects before the framework replaces
the content area with the incoming-call overlay. The app is not rebuilt.

```java
default String[] getSupportedDeviceTypes()
```
Returns supported device type IDs, such as `"phone"` or `"tablet"`. An empty
array supports every device type. Incompatible apps are excluded from catalog
and home-grid views and rejected if opened through a direct action.
`@Nonnull`; default `new String[0]` (`PhoneApp.java:153`).

```java
@Nullable default String getIconPath()
```
Optional icon texture path for compact list/store rows (e.g. the App Store), as an
absolute UI path under `Common/UI/Custom/` — for example `"Pages/Phone/Whatgram.png"`.
Reference it **without** the `@2x` suffix; the engine adds it. Returns `null` when the app
has no dedicated icon, in which case consumers fall back to a placeholder tile. Distinct
from `getAppButtonUI()`, which is the full home-grid button document. Default: `null`
(`PhoneApp.java:60`).

```java
default Map<String, String> getThemeableSelectors()
```
Themeable selectors this app exposes, as *role → selector*. Roles correspond to keys in a
theme palette's `Colors` map (see `DeviceThemeAsset`); the framework sets the `Background`
of each selector to the palette color when a theme is applied. Selectors should name
elements inside this app's own `getUIPath()` document. Default: empty map
(`PhoneApp.java:73`). See [theming.md](theming.md).

```java
@Nullable default String getRequiredPermission()
```
Permission node required to install *and* open this app (a plain dot-separated string such
as `"apps.police.database"`), or `null` when the app is freely accessible. Evaluated by
`AppCatalogService` at install time and again at every open via `DevicePage`. Default:
`null` (`PhoneApp.java:122`).

```java
@Nonnull default String getPermissionVisibility()
```
How the app is presented to a player who lacks `getRequiredPermission()`:

- `"locked"` (default) — visible in the App Store with a locked badge; cannot be installed
  or opened.
- `"hidden"` — suppressed from every UI surface.

Only consulted when `getRequiredPermission()` is non-null. Default: `"locked"`
(`PhoneApp.java:139`).

```java
default boolean isSystemApp()
```
`true` marks a system app: force-present on every device at every open and not
uninstallable by the player. Apps returning `true` must also appear in the device asset's
`DefaultApps` array. The bundled Whatgram, Contacts, Calls, Settings and AppStore apps
return `true`. Default: `false` (`PhoneApp.java:167`).

---

## StatefulPhoneApp\<S\>

`esq.phonemod.phone.api.StatefulPhoneApp`

Abstract base class that adds enum-backed per-player state helpers. Extend this instead of implementing `PhoneApp` directly.

```java
protected StatefulPhoneApp(@Nonnull S initialState)
```
Provide the enum value that represents the app's initial (default) state.

```java
protected S getState(@Nonnull PhoneAppContext ctx)
```
Returns the current state enum value for the player identified by `ctx`. Falls back to `initialState` if no state has been set yet, or if the stored name is no longer a valid enum constant (e.g. after a rename).

```java
protected void setState(@Nonnull PhoneAppContext ctx, @Nonnull S state)
```
Persists `state` for the player identified by `ctx`. Stored under the reserved key `__state__` in `ctx`'s per-player state map.

```java
protected void appendMainUI(@Nonnull PhoneAppContext ctx, @Nonnull UICommandBuilder cmd)
```
Clears the shell content area (`ctx.getContentSelector()`, sourced from `DeviceShell` — never hardcode `"#AppContent"`) and appends this app's `getUIPath()` into it. Call it once at the top of `build()`; it is the only place an app should touch the content area (see the set/append/clear contract on `PhoneUi`).

---

## PhoneAppContext

`esq.phonemod.phone.api.PhoneAppContext`

Per-player session context. Created by `DevicePage` for each app method invocation. Provides ECS access, identity, and isolated per-player state storage.

### Identity

```java
@Nonnull Ref<EntityStore> getRef()
```
The entity reference for this player.

```java
@Nonnull Store<EntityStore> getStore()
```
The ECS store. Use this to read/write other components on the player entity.

```java
@Nonnull PlayerRef getPlayerRef()
```
Hytale `PlayerRef`. Use for sending packets and notifications.

```java
@Nonnull String getPhoneNumber()
```
The player's assigned phone number.

```java
@Nonnull String getAppId()
```
The ID of the app this context was created for (matches `getId()`).

### Per-player state

All values are `String`. State is namespaced by `phoneNumber + "|" + appId` inside
`PhoneAppSessionState` (`PhoneAppContext.java:103`), so apps and separate phone numbers
cannot read each other's state through the context API.

```java
@Nullable String getState(String key)
```
Returns the stored value for `key`, or `null` if not set.

```java
@Nonnull String getState(String key, String defaultValue)
```
Returns the stored value for `key`, or `defaultValue` if not set.

```java
void setState(String key, String value)
```
Stores `value` under `key`. Persists in the player's ECS component until
explicitly cleared.

```java
void clearState(String key)
```
Removes `key` from this app's state.

```java
void clearAllState()
```
Removes all state keys for this app from the player's state map.

### Convenience

```java
void sendNotification(String title, String body)
```
Sends an in-game notification toast to this player via `NotificationUtil`.

---

## PhoneEvent

`esq.phonemod.phone.api.PhoneEvent`

Immutable event payload created by `DevicePage` from the raw UI codec event.

```java
@Nonnull String getAction()
```
The action name (e.g. `"open_chat"`). Match against `PhoneEventActions` constants.

```java
@Nonnull String getAppId()
```
The app field from the event data. For app-targeted events this is the app ID; for generic events it may be blank.

```java
Map<String, String> getParams()
```
Unmodifiable map of all extra parameters in the event.

```java
@Nullable String getParam(String key)
```
Returns the value for `key`, or `null` if absent. Always check for null.

```java
String getParam(String key, String defaultValue)
```
Returns the value for `key`, or `defaultValue` if absent.

```java
int slotIndex()
```
For an `ItemGrid` slot-click event, the clicked slot index; `-1` if the event carries no `SlotIndex`. Bind slot clicks with `PhoneUi.bindSlot` (see `events.md`).

---

## PhoneUi

`esq.phonemod.phone.api.PhoneUi` — `final`, all methods `static`, no instance state
(`PhoneUi.java:46,48`). `@Deprecated(since = "2.0", forRemoval = false)` (`:45`) along with
the rest of the v1 surface, but it is the most-used helper in every existing app.

These are thin wrappers over `UICommandBuilder` / `UIEventBuilder`; they hide nothing, so
you can freely mix helper calls with direct builder calls.

### set / append / clear ownership contract

From the class javadoc (`PhoneUi.java:22-43`). Violating it breaks theme re-application,
because a themed selector can be torn down while a palette still targets it:

1. **The shell content area** (`ctx.getContentSelector()`, e.g. `#AppContent`) **is owned by
   the framework.** An app clears and re-appends it in exactly one place — `build()`, via
   `StatefulPhoneApp.appendMainUI` or `PhoneUi.appendMain`. Render helpers must never clear
   or append it again.
2. **Switch sub-views inside a single app root, not by swapping roots.** Prefer one root
   document containing every view's elements; toggle with `setVisible` and refill your own
   list container. This keeps every `getThemeableSelectors()` target alive in all views.
3. **An app only clears containers it owns** — its own list/group selectors via `safeClear`.
   To fill a list: `safeClear` it, then `appendListItem` in a loop.
4. **A full-screen takeover** (e.g. a device-driven incoming-call overlay) is the only case
   that replaces the content area outright, via `replace`. Apps should avoid it.

### App entry point

```java
static void appendMain(PhoneAppContext ctx, UICommandBuilder cmd, PhoneApp<?> app)
```
Clears `ctx.getContentSelector()` and appends `app.getUIPath()` into it — the canonical
first two lines of `build()`. Equivalent to `StatefulPhoneApp.appendMainUI`. (`:62`)

### Action binding

```java
static void bindAction(UIEventBuilder evb, String selector, String action)
static void bindAction(UIEventBuilder evb, String selector, String action, EventData extraParams)
```
Binds an `Activating` event carrying `Action = action`. The 4-arg overload merges every
entry of `extraParams` alongside it. Both **lock the interface** while the event is in
flight. Build the payload with `params`. (`:76`, `:89`)

```java
static void bindOpenApp(UIEventBuilder evb, String selector, String appId)
```
Binds a button to open another app. Equivalent to
`EventData.of("Action", "open_app").append("App", appId)`. (`:131`)

```java
static void bindBack(UIEventBuilder evb, String selector)
static void bindHome(UIEventBuilder evb, String selector)
```
Fire the framework `back` / `home` actions (`PhoneEventActions.BACK` / `.HOME`).
(`:143`, `:150`)

```java
static void bindSlot(UIEventBuilder evb, String gridSelector, String action)
static void bindSlot(UIEventBuilder evb, String gridSelector, String action, EventData extraParams)
```
Binds an `ItemGrid`'s slot clicks. The clicked index arrives as the `SlotIndex` param — read
it with `PhoneEvent.slotIndex()`. Unlike `bindAction` these do **not** lock the interface,
because slot selection is navigation rather than mutation. (`:107`, `:115`)

```java
static void bindCapturedText  (UIEventBuilder evb, String selector, String action,
                               String paramName, String inputSelector)
static void bindCapturedNumber(UIEventBuilder evb, String selector, String action,
                               String paramName, String inputSelector)
```
Fire `action` and capture a client-side input value into `paramName`, using the `@`-prefix
convention (`EventData.of("Action", action).append("@" + paramName, inputSelector)`).
`inputSelector` must name an element exposing `.Value`, e.g. `"#MessageInput.Value"`.
`bindCapturedNumber` is an alias of `bindCapturedText` provided purely for semantic clarity
— there is **no** numeric parsing or validation. (`:167`, `:182`)

### List and selector composition

```java
@Nonnull static String appendListItem(UICommandBuilder cmd, String listSelector,
                                      String uiPath, int index)
```
Appends `uiPath` into `listSelector` and returns the indexed row selector for the entry it
just added — e.g. `"#MyList[2]"`. `index` is the 0-based position you are filling; the
helper does not track it for you. (`:203`)

```java
@Nonnull static String rowSelector(String listSelector, int index)   // "#ContactList[3]"
@Nonnull static String indexed    (String parentSelector, int index) // "#APPHolder[2]"
@Nonnull static String child      (String parentSelector, String childSelector)
                                                       // "#ContactList[0] #ContactName"
```
Pure string composition. `rowSelector` and `indexed` are identical in behavior (`parent +
"[" + index + "]"`); the two names exist to signal list-row versus generic-child intent.
`child` joins with a single space. (`:216`, `:235`, `:226`)

### Text and visibility

```java
static void setText(UICommandBuilder cmd, String selector, Message message)
static void setText(UICommandBuilder cmd, String selector, String rawText)
static void setLocalizedText(UICommandBuilder cmd, String selector, String translationKey)
static void setVisible(UICommandBuilder cmd, String selector, boolean visible)
```
The `Message` overload sets a localized message; the `String` overload sets raw,
unlocalized text — prefer `setLocalizedText` (which wraps `Message.translation(key)`) for
anything user-visible. `setVisible` targets `selector + ".Visible"`.
(`:244`, `:254`, `:263`, `:273`)

### Content replacement and clearing

```java
static void replace  (UICommandBuilder cmd, String selector, String uiPath)
static void safeClear(UICommandBuilder cmd, String selector)
```
`replace` clears `selector` then appends `uiPath` — reserved for full-screen takeovers per
the ownership contract. `safeClear` is a plain clear that is safe on an already-empty or
absent selector. (`:284`, `:295`)

### EventData factories

```java
@Nonnull static EventData params(String key, String value)
@Nonnull static EventData params(Map<String, String> values)
```
Build a payload for the `bindAction` / `bindSlot` overloads. (`:306`, `:314`)

### Selector validation

```java
static void ensureRequiredSelectors(String uiPath, Map<String, String> selectors)
```
Logs a warning for every entry whose selector value is null or blank. **Does not throw** —
the page still builds, and a missing selector silently no-ops in the UI. Pass a map of
logical name → selector, e.g. `Map.of("submit button", "#SubmitBtn")`; `uiPath` is used
only in the warning text. (`:333`)

---

## PhoneService

`esq.phonemod.phone.core.PhoneService`

Singleton. Initialize once during plugin `setup()`. Must be initialized before any app is registered or any phone page is created.

```java
static void initialize()
```
Creates the singleton if it does not exist. Safe to call multiple times (idempotent).

```java
static PhoneService get()
```
Returns the singleton. Throws `IllegalStateException` if `initialize()` has not been called.

```java
void registerApp(@Nonnull PhoneApp<?> app)
```
Registers an app. Delegates to `PhoneAppRegistry.register()`. Throws `IllegalArgumentException` on duplicate ID.

```java
PhoneApp<?> getApp(@Nonnull String appId)
```
Returns the app registered under `appId`, or `null` if not found.

```java
@Nonnull List<PhoneApp<?>> getApps()
```
Returns all registered apps sorted by `getSortOrder()` ascending. Unmodifiable.

> Page creation lives in the device framework: `esq.phonemod.device.core.DeviceService.createDevicePage(session)`
> (a session built from a `DeviceAsset`). `PhoneService` itself only manages apps.

---

## PhoneAppRegistry

`esq.phonemod.phone.core.PhoneAppRegistry`

Internal catalog. Access via `PhoneService`.

```java
void register(@Nonnull PhoneApp<?> app)
```
Throws `IllegalArgumentException` if the same app ID is already registered.

```java
@Nullable PhoneApp<?> get(@Nonnull String appId)
```
Returns the app registered under `appId`, or `null` if not found.

```java
@Nonnull List<PhoneApp<?>> getApps()
```
Sorted by `getSortOrder()`, unmodifiable.

```java
boolean contains(@Nonnull String appId)
```
Returns `true` if an app with that ID is registered.
