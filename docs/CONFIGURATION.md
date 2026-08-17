# Framework configuration

The phone's **Settings** app has two separate views:

- **Player** contains personal/device preferences such as theme, wallpaper, home layout,
  sounds, message privacy, incoming-call availability, and the current phone number.
- **Plugin** contains development-oriented, server-wide framework configuration. Its tab
  is visible only to players with `phonemod.admin.deviceframework`.

The permission is checked again for navigation and every plugin-setting action; hiding
the tab is not the authorization boundary.

Accepted changes are validated, applied to the running framework, and written in order
through Hytale's plugin configuration service. A successful click refreshes the labels
from authoritative state and sends a confirmation notification.

## Settings UI controls

| Control | Options | Default | Effect |
|---|---|---|---|
| Number binding | Per Device, Per Player | Per Device | Selects whether a newly resolved number follows a durable device UUID or the player's primary personal identity. |
| New calls | Enabled, Disabled | Enabled | Allows or rejects creation of new call sessions. Existing calls are not force-ended. |
| Call privacy | Device Asset, Private Endpoints, Always Speaker, Player Toggle | Device Asset | Overrides the `CallAudioPolicy` declared by each device asset. |
| Cross-world calls | Allow, Same World Only | Allow | Restricts new calls to endpoints in the same world when disabled. |
| Incoming calls | Connected Device, Device UI Open | Connected Device | Chooses whether player presence is sufficient or the target device page must be open. |
| Ring timeout | 15, 30, 60 seconds | 30 seconds | Controls when a newly ringing call becomes missed. |
| Speaker radius | 4, 8, 16 blocks | 8 blocks | Sets the voice broadcast radius around each endpoint in speaker mode. |
| Offline mail | Queue, Online Only | Queue | Enables bounded mailbox delivery when an address is offline. |
| Message limit | 128, 512, 1024 characters | 512 characters | Changes the server-enforced maximum length for new messages. |

Player settings are functional rather than cosmetic placeholders:

| Player control | Runtime behavior |
|---|---|
| Theme | Recolors the live shell and active app from validated theme assets. |
| Wallpaper | Changes the home-screen background between the device logo, a dark surface, and a plain surface. |
| App layout | Changes the home launcher between a wrapped grid and a scrolling list. |
| Sound profile | `Nonkia` uses the device asset's message/ringtone sounds; `Silent` suppresses them. |
| Message alerts | Enables or suppresses message toast notifications without dropping message history. |
| Message preview | Replaces the body in a toast with `New message`; the stored conversation remains unchanged. |
| Incoming calls | `Do Not Disturb` rejects new calls to this device while leaving messaging available. |
| Restore defaults | Resets only the current device's player preferences. It does not alter identity, ownership, apps, contacts, or histories. |

These preferences are stored on the player's persistent component under the stable device
address. In per-player address mode, that address is shared by the player's personal
phones; in per-device mode, each phone keeps its own preferences. They are not server-wide
plugin policy.

## Number ownership modes

`DEVICE` is the compatibility default. Each durable personal device UUID has its own
human-facing address. Copying item metadata does not transfer ownership; explicit
framework transfer remains the ownership mechanism.

`PLAYER` resolves every personal phone opened by one player to that player's primary
personal identity and address. The phone item is stamped on its next legitimate open,
and the player's phone components are remapped if the resolved address changes. Existing
device identities are retained, so switching back to `DEVICE` is non-destructive.

Shared devices remain address-optional identities. This switch does not convert shared
terminals into personal phones.

## Voice-call modes

- `ASSET` respects each device asset's `CallAudioPolicy`.
- `PRIVATE` routes voice only to the two active endpoints.
- `SPEAKER` additionally routes voice to nearby connected players within the configured
  speaker radius.
- `TOGGLEABLE` starts private. Either active endpoint can use **Speaker On** or
  **Private Audio** on the active-call page; forged requests from non-endpoints are
  rejected.

The resolved audio policy is captured when a call session is created. Changing the mode
does not rewrite an existing call, while changing speaker radius affects subsequent
speaker routing immediately.

Calls survive phone-page dismissal. The open-page requirement applies only when a new
incoming call is attempted, not after the call has connected.

## Persisted configuration

The codec-backed JSON keys and full accepted ranges are:

| Key | Accepted value |
|---|---|
| `AddressBindingMode` | `DEVICE`, `PLAYER` |
| `CallsEnabled` | boolean |
| `CallAudioMode` | `ASSET`, `PRIVATE`, `SPEAKER`, `TOGGLEABLE` |
| `AllowCrossWorldCalls` | boolean |
| `RequireOpenDeviceForIncomingCalls` | boolean |
| `CallRingTimeoutSeconds` | 5–300 |
| `SpeakerRadiusBlocks` | 1–64 |
| `OfflineMessagingEnabled` | boolean |
| `MaxMessageCharacters` | 1–4096 |

The UI intentionally offers a smaller set of safe presets than the codec. Operators can
use values elsewhere in the accepted range by editing the generated plugin config while
the server is stopped, then starting the server so the codec validates the document.

Security boundaries are deliberately not exposed as disable switches. UI payload size,
parameter count, identifier validation, action rate limits, permission checks, ownership
checks, bounded retention, and world-thread dispatch remain server-enforced. Their
advanced numeric limits stay codec-validated in the config document.

## Development test recipes

For phone-number behavior, select a binding mode, close the phone, then open each test
phone and compare its displayed number. Use copied disposable items: production owner
transfer should still go through `/deviceframework transfer`.

For voice behavior, use two endpoint players and one nearby observer. Start a fresh call
after selecting the desired mode. In `PRIVATE`, only endpoints should hear relayed voice;
in `SPEAKER`, the observer should hear it only inside the configured radius; in
`TOGGLEABLE`, verify the call begins private and both buttons update both endpoint pages.

For presence behavior, compare a connected player with their phone page closed against
the same player with it open. For world isolation, place the endpoints in separate worlds
before initiating a fresh call.
