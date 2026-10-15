# Phonemod 3.0 — Exit pass in-game (V3)

Generado 2026-10-15 desde SPEC §R-PAR (D-13). Cubre **solo** los requisitos que se verifican
in-game (G) o en parte in-game (H+G). Lo headless lo cubren los tests. Cada fila cita su R-xx;
una fila sin R-xx no pertenece a este documento.

Sustituye para 3.0 a `docs/IN_GAME_EXIT_PASS.md` (2.x), que se queda en `release/2.x` con sus
erratas conocidas (research/R-PAR-DRAFT.md §"Pasos del exit pass que no cuadran").

Build bajo prueba: `3.0.0-alpha.<n>` @ `<commit>`, a rellenar en cada pasada.
Servidor: uno de pruebas desechable o hytest. **El deploy a hytest solo se hace a petición
explícita.**

## Parte 0 — Preparación
- Dos clientes (P1, P2) y un tercero (P3) para el audio de speaker.
- Un admin con `phonemod.admin.deviceframework` y P2 sin ese permiso.
- El boot muestra `Enabled plugin esq.phonemod:Phonemod`.
- `/deviceframework status` responde con la versión 3.0. En 2.x imprime "Phonemod API
  2.0.0-alpha.1" fijo (DeviceFrameworkCommand.java:36); en 3.0 debe salir la versión real.
- Para SHELL-11 hace falta una app con permiso: ninguna app nativa declara uno. Se usa
  hello-device-app (R-APP-3) con un permiso declarado.
- Para SHELL-10 (dispositivo incompatible) hace falta un segundo DeviceType. Phonemod solo trae
  Phone.json, así que se usa un asset `pc` de prueba o computers. Si no hay ninguno, la fila
  queda N/A y la cubre AppOpenAuthorizerTest.

Leyenda: PASS / FAIL (adjuntar consola o captura) / N/A (con motivo).

## Parte 1 — Shell
| # | paso | esperado | R-xx |
|---|---|---|---|
| 1.1 | P1 usa el item Phone por primera vez | Se abre el device page; el item queda con address e identity | R-PAR-SHELL-1 |
| 1.2 | Navegar a una app y pulsar Home | Vuelve a la rejilla | SHELL-3 |
| 1.3 | Mirar la rejilla | Tiles con nombre; click abre la app | SHELL-5, SHELL-7 |
| 1.4 | Settings → Layout List y volver a Home | Rejilla en lista con scroll | SHELL-8 |
| 1.5 | Admin revoca el permiso de hello-device-app; P1 intenta abrirla | Chat "You do not have permission to open this app."; no se abre | SHELL-11 |
| 1.6 | Abrir app phone-only desde un device `pc` (si existe) | No se abre (silencioso) | SHELL-10 |
| 1.7 | Settings → Theme Dark | La página recolorea en vivo sin reabrir | SHELL-26, SHELL-27 |
| 1.8 | Abrir el teléfono / iniciar llamada / cerrar | Animación UsePhone; PhoneCall durante la llamada; se para al cerrar sin llamada | SHELL-28 |
| 1.9 | P2 escribe a P1 con el teléfono de P1 cerrado | Toast con remitente en verde y cuerpo (o "New message" con preview off), sonido salvo Silent | SHELL-29, WG-10 |
| 1.10 | Repetir 1.9 con Message alerts off | Sin toast | SHELL-29 |

## Parte 2 — Llamadas y voz
| # | paso | esperado | R-xx |
|---|---|---|---|
| 2.1 | P1 llama a P2 | P2 ve "Incoming Call" con **nombre de contacto** y número, Answer/Decline; suena Ringtone_Nonkia | SHELL-19, SHELL-20, FIX-2 |
| 2.2 | En P1 durante el ring | "Calling…" con nombre; la misma etiqueta si P1 cierra y reabre | SHELL-21, FIX-3 |
| 2.3 | P2 contesta | Ambos ven "In call with <nombre> (<número>)" + Hang Up; voz en ambos sentidos | SHELL-22, CALLS-10 |
| 2.4 | P1 cierra el teléfono a mitad de llamada y lo reabre | La llamada sigue; al reabrir se restaura la UI de llamada | SHELL-17, SHELL-18 |
| 2.5 | P1 cuelga | Ambos vuelven a Calls con historial actualizado | SHELL-23, CALLS-9 |
| 2.6 | Justo después, P1 usa el teclado y Call-back sin pasar por Home | Funcionan | FIX-1 |
| 2.7 | P1 llama a P2 y P2 no contesta | Se corta a ~30 s (máx. ~32); missed en ambos | CALLS-7 |
| 2.8 | P2 con el teléfono cerrado (requireOpenDevice=false) recibe llamada | Toast + ringtone en P2 | FIX-7 |
| 2.9 | Device PRIVATE: P3 junto a P1 en llamada | P3 no oye nada | CALLS-10, CALLS-13 |
| 2.10 | callAudioMode SPEAKER: repetir 2.9 | P3 a ≤ 8 bloques oye la llamada | CALLS-11 |
| 2.11 | TOGGLEABLE: P1 pulsa Speaker On y luego Private Audio | Controles solo en la llamada activa; ambos extremos re-renderizan; P3 oye solo con speaker | CALLS-12 |
| 2.12 | Mutear a P1 en el server durante la llamada | No se retransmite su voz; la llamada sigue | CALLS-14 |
| 2.13 | Nueva llamada; P2 se desconecta | P1 sale de la llamada y puede llamar de nuevo | SHELL-30 |
| 2.14 | Reiniciar el server con una llamada activa | Arranque limpio; ninguna llamada colgada al volver | CALLS-15 |

## Parte 3 — Apps
| # | paso | esperado | R-xx |
|---|---|---|---|
| 3.1 | Calls: escribir número y Call | Se inicia la llamada | CALLS-1 |
| 3.2 | Whatgram: abrir un chat | Título, inicial, subtítulo, Back, Call y composer | WG-5 |
| 3.3 | P1 y P2 con el mismo chat abierto; P2 envía | La burbuja aparece en vivo en P1 y queda leída; suena MessageSent en P2 salvo Silent | WG-9, FIX-8 |
| 3.4 | P1 en la lista de Whatgram; P2 envía | La lista se refresca en el sitio (badge, preview) | WG-9, SHELL-24 |
| 3.5 | P1 con Contacts abierto; P2 envía | Contacts no se duplica ni se rompe | FIX-4 |
| 3.6 | P2 offline; P1 le escribe; P2 entra y abre el teléfono | El mensaje aparece en el chat | WG-11 |
| 3.7 | Whatgram: botón Call del header / Back | Llama al contacto / vuelve a la lista | WG-12 |
| 3.8 | Contacts: Add Contact, rellenar y Save; Cancel | Formulario, guarda y vuelve; Cancel vuelve sin cambios | CON-1, CON-2 |
| 3.9 | Contacts: nombre demasiado largo y Save | Error visible; el formulario conserva lo escrito | FIX-6 |
| 3.10 | Contacts: Chat y Call en una fila | Chat abre Whatgram en esa conversación; Call llama | CON-5 |
| 3.11 | Settings (P2 sin permiso admin) | La pestaña Plugin no está; forzar la vista vuelve a Player | SET-5 |
| 3.12 | Settings Player: cambiar wallpaper y volver a Home | Wallpaper aplicado en el siguiente Home | SET-4 |
| 3.13 | App Store: Install de Group Chat | Aparece en Home | AS-6, AS-7, GC-1 |
| 3.14 | Group Chat con P1 y P2 abiertos; P1 envía | Ambos ven la burbuja en vivo, también el emisor | GC-3 |
| 3.15 | Reiniciar el server y abrir Group Chat | El historial sigue ahí | GC-4 |
| 3.16 | Caso async (R-APP-4): app de ejemplo con carga simulada | Repinta sola al terminar, sin reabrir | R-APP-4 |

## Parte 4 — Migración (comandos reales)
Los comandos de administración (`/deviceframework status`, `transfer`, `release`, `/phonemod …`) no tienen R-xx todavía: ver Q-11.
| # | paso | esperado | R-xx |
|---|---|---|---|
| 4.1 | `/deviceframework migration dry-run` | Conteo de identidades y conflictos; "no data changed" | SHELL-1 (identidad) |
| 4.2 | Con datos 2.x copiados: abrir un teléfono antiguo | Migración perezosa en OpenPhone; datos conservados; backup `.pre-v2-<ts>.bak` en el primer save | SHELL-1 |

## Parte 5 — Multijugador tuiter (F7)
Lo define la card F7 a partir de R-APP-5 cuando exista el port.

## Evidencias
Pegar las líneas de consola y las capturas junto a cada fila, y anotar en `phases/F<n>/REPORT.md`
qué filas quedaron en PASS, FAIL o N/A.
