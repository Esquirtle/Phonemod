# API Reference

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
Human-readable name shown on the app icon label in `AppMenu`.

```java
String getAppButtonUI()
```
Path to the `.ui` file that defines this app's button in the AppMenu grid (e.g. `"Pages/Phone/Components/WhatgramButton.ui"`). The file must declare a root `Group #APPBUTTONENTRY` containing a `TextButton #APPBUTTON` (with the icon hardcoded in its `Default` style) and a `Label #APPNAME`. AppMenu sets the label text dynamically after appending the file. Third-party plugins should use their own asset namespace (e.g. `"Pages/Playground/AppIcon.ui"`).

```java
String getUIPath()
```
Asset path for the app's root UI file, loaded into `#AppContent` via `appendMainUI()` (e.g. `"Pages/Phone/MyApp.ui"`).

```java
void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb)
```
Builds the current UI state for the given player. Called on initial open and after every event that returns `true` from `handleEvent`. Must be idempotent — rebuilding should produce the same result given the same state.

```java
boolean handleEvent(PhoneAppContext ctx, PhoneEvent event,
                    UICommandBuilder cmd, UIEventBuilder evb)
```
Handles a player interaction. Return `true` if the event was consumed (the framework sends `cmd`/`evb` as the UI update). Return `false` if unhandled (the framework sends a no-op update and logs the unhandled action).

### Optional methods (default no-op)

```java
default int getSortOrder()
```
Controls display order in `AppMenu`. Lower values appear first. Default: `0`.

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
Called by `PhonePage` when a message arrives while this app is open. Return `true` to send `cmd`/`evb` as the UI update directly — no full rebuild. Return `false` to fall back to `build()`. Use the `true` path to append a single message bubble without clearing and re-rendering the whole chat.

```java
default void onIncomingMessage(PhoneAppContext ctx, String fromNumber)
```
Fallback called by `PhonePage` only when the 4-arg overload returns `false`. `build()` is called automatically after this returns. Override this to update state before the rebuild (e.g. mark a message as read). If you override the 4-arg version and return `true`, this method is not called.

```java
default void onIncomingCall(PhoneAppContext ctx, String callerNumber, String callerName)
```
Called by `PhonePage` when an incoming call arrives while this app is open. `build()` is called automatically after this hook returns.

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
protected void appendMainUI(@Nonnull UICommandBuilder cmd)
```
Convenience: calls `cmd.clear(CONTENT_SELECTOR)` then `cmd.append(CONTENT_SELECTOR, getUIPath())`. Call this at the top of every `build()` implementation to reset the content area before populating it.

```java
protected static final String CONTENT_SELECTOR = "#AppContent"
```
The UI selector for the main content area. Use this constant in `cmd.clear()` and `cmd.append()` calls.

---

## PhoneAppContext

`esq.phonemod.phone.api.PhoneAppContext`

Per-player session context. Created by `PhonePage` for each app method invocation. Provides ECS access, identity, and isolated per-player state storage.

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

All values are `String`. Apps cannot read each other's state — keys are namespaced by `appId` inside `PhoneAppSessionState`.

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
Stores `value` under `key`. Persists until the player's entity is unloaded.

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

Immutable event payload created by `PhonePage` from the raw UI codec event.

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

```java
@Nonnull PhonePage createPhonePage(@Nonnull PlayerRef playerRef, @Nonnull String phoneNumber)
```
Creates a new `PhonePage` instance bound to the given player and phone number.

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
