# R-PAR — borrador de paridad (2.0.0-alpha.13)

Generado 2026-10-15 por un explorador de solo lectura sobre `feature/v2-refactor` (dc9b931).
Es MATERIA PRIMA para SPEC §R-PAR, no la spec: cada línea se acepta, corrige o descarta en la
entrevista. Paridad = funcional (D-11).

Abreviaturas (bajo `src/main/java/esq/phonemod/`): DP = `device/ui/DevicePage.java`,
CR = `phone/messaging/CallRegistry.java`, PR = `phone/messaging/PhoneRegistry.java`,
Rtr = `device/routing/DefaultPhoneRouter.java`, EP = `docs/IN_GAME_EXIT_PASS.md`.
Verificación: H = headless (lógica pura / FakeBackend), G = solo in-game.

## SHELL
- SHELL-1: Usar el item Phone abre el device page; en el primer uso se estampa address (PhoneNumber) + identity UUID; modo PLAYER = una address por jugador. OpenPhone.java:74-90,107; EP Part 0. H (IdentityRegistryTest) + G.
- SHELL-2: Sin address asignable → "This device does not currently have an address." y no abre. OpenPhone.java:79-81. H.
- SHELL-3: Shell = Phone.ui; Home vuelve a la rejilla desde cualquier estado. DP:117,130-136; HomeHandler.java:20; Phone.json:4-7. G.
- SHELL-4: Cada apertura re-siembra apps: system siempre presentes; defaults no-system solo una vez y si hay permiso (marcador `__seeded__`). DP:122-128; AppInstallService.java:176-208. H.
- SHELL-5: Rejilla = system compatibles en orden DefaultApps (whatgram, contacts, calls, settings, appstore) y luego no-system instaladas por sortOrder, id. DP:286-315; Phone.json:9. H + G.
- SHELL-6: App con permiso que falta y visibility "hidden" no aparece. DP:326-331. H.
- SHELL-7: Tile = displayName, click abre; app sin button UI se omite. DP:332-345. G.
- SHELL-8: Layout "List" → TopScrolling; si no, LeftCenterWrap. DP:260-267. H + G.
- SHELL-9: Wallpaper "Dark"→#0f1724, "None"→#151c26, otro → default del asset (hytel_logo.png). DP:680-686; Phone.json:12. H.
- SHELL-10: Orden de checks al abrir, gana el primer fallo: unknown → wrong device → not owner → not installed → falta cualquier permiso. AppOpenAuthorizer.java:43-66; DP:372-403; EP 1.1/1.3-1.6. H.
- SHELL-11: Falta de permiso → chat `phonemod.appstore.launch.denied`; no abre, sesión intacta. DP:378-385; EP 1.2. H + G.
- SHELL-12: Resto de denegaciones = silenciosas (solo log). DP:386-400; EP 1.3, 1.6. H.
- SHELL-13: Abrir una app cierra la actual (onClose + unsubscribe sync). DP:411,455-471; SetupManager.java:80-84. H.
- SHELL-14: Acciones UI rechazadas en silencio (ack sin cambio): sesión ajena/página stale, rate-limit, payload malformado/grande, campo App ≠ app abierta. DP:176-210; UiEventValidator; DeviceSession.java:74; EP 1.6. H.
- SHELL-15: Acciones sin ruta van a la app solo en estado APP. Rtr:128-137. H.
- SHELL-16: "open_chat" fuera de Whatgram abre Whatgram en ese chat. Rtr:66-71; OpenChatHandler. H.
- SHELL-17: Reabrir restaura UI de llamada: activa > saliente pendiente > entrante pendiente > home. DP:138-165; CallRestoreDecision; EP 2.3. H + G.
- SHELL-18: Cerrar la página NO termina la llamada; presencia se mantiene (toasts siguen). DP:214-228; PR:131-143; EP 2.2. G.
- SHELL-19: Push de llamada entrante: overlay "Incoming Call" "<name> (<number>)" Answer/Decline en devices con capacidad `calls`. DP:562-585; Calls.java:271-282. G.
- SHELL-20: Ringtone_Nonkia salvo perfil "Silent". DP:566-570,713-721; Phone.json:18. H + G.
- SHELL-21: Push saliente: overlay "Calling… <name> (<number>)". DP:588-600. G.
- SHELL-22: Push de respuesta (ambos): "In call with <partner> (<number>)" + Hang Up. DP:603-613; CR:218-235; EP 2.1. G.
- SHELL-23: Push de fin (ambos): pantalla Calls (dialer + historial). DP:616-626; CR:305-312; EP 2.4. G.
- SHELL-24: Mensaje entrante re-renderiza la app abierta solo en estado APP; Whatgram in place, otras rebuild completo. DP:539-559; PR:276-280. G.
- SHELL-25: Publish de sync re-renderiza solo si esa app es la abierta. DP:629-654. H.
- SHELL-26: Colores de shell/área/header/panel desde la paleta (Default/Dark/Compact); el tema guardado gana al default del asset. DP:118,432-435,661-668; ThemeService; Themes/*.json. H.
- SHELL-27: Cambiar tema en Settings recolorea en vivo. SetupManager.java:91-104; DP:689-700. G.
- SHELL-28: UI abierta → animación UsePhone; llamada activa → PhoneCall (gana); cerrar sin llamada la para. PhoneAnimationController; PR:100; CR:233-234,282-283. G.
- SHELL-29: Mensaje recibido → toast (remitente #43D69A, cuerpo o "New message" si preview off, sonido Notification_Message_Received salvo Silent); sin toast si alerts off. PR:258-275,400-407. H + G.
- SHELL-30: Desconexión termina llamadas activas/pendientes, quita subs/rooms y presencia. PhoneEventHandler.java:25-50; EP 2.5. G.

## CALLS (CallsApp.java, helpers/Calls.java, DustCalls.ui)
- CALLS-1: id `calls`; input "Enter number..." + botón Call. CallsApp.java:86-91,120-129. G.
- CALLS-2: Historial más reciente primero: "<name> (<number>)", "Missed|Outgoing|Incoming  MM/dd HH:mm", Call-back. CallsApp.java:94-112. H.
- CALLS-3: App de sistema (no desinstalable). CallsApp.java:73-75. H.
- CALLS-4: Rechazo → registro "missed outgoing" + pantalla Calls si: llamadas off, a sí mismo, alguien offline, callee requiere device abierto y lo tiene cerrado, DND, cross-world prohibido. CR:155-178,295-303; EP 2.11. H.
- CALLS-5: 1 intento por `callAttemptIntervalSeconds` (5); exceso descartado sin historial. CR:179-184; PhonemodConfig.java:123. H.
- CALLS-6: Llamar a alguien ocupado (o estando ocupado) falla sin doble sesión. CR:195-199. H.
- CALLS-7: Sin respuesta termina tras `callRingTimeoutSeconds` (30; check cada 2 s → hasta ~32 s); missed en ambos. CR:52,79-92; EP 2.6. H + G.
- CALLS-8: Answer solo del callee, sonando, y en estado INCOMING_CALL. CallSession.java:35-39; Rtr:75-80. H.
- CALLS-9: Decline/cancel sonando → missed; Hang Up activa → normal; ambos a Calls + historial. CR:252-293. H.
- CALLS-10: Voz privada entre los dos al contestar. CR:94-137; EP 2.1, 2.7. H + G.
- CALLS-11: Speaker/toggled-on → también a cercanos del mismo mundo en `speakerRadiusBlocks` (8). CR:119-135; EP 2.8. G.
- CALLS-12: TOGGLEABLE empieza privada; controles solo en activas TOGGLEABLE, solo participantes, re-render en ambos. Calls.java:299-307; CallSession.java:49-54; CR:238-250; EP 2.9. H + G.
- CALLS-13: Política = `callAudioMode` (ASSET/PRIVATE/SPEAKER/TOGGLEABLE); ASSET → asset del device del caller (Phone.json: Private). CR:209-216; Phone.json:14. H.
- CALLS-14: Hablante muteado por el server no se retransmite; la llamada sigue. CR:94-110; EP 2.10. H + G.
- CALLS-15: Shutdown termina llamadas y limpia rutas; filtro de voz registrado al arrancar. CR:57-77; EventRegistryManager.java:26; EP 2.12. G.

## WHATGRAM (WhatgramApp.java, helpers/Whatgram.java, DustWhatgram.ui)
- WG-1: Lista "Whatgram" sin back/call/avatar/composer; una fila por interlocutor (historial, luego inbox offline), nombre de contacto o número. WhatgramApp.java:114-144. H.
- WG-2: Vacío → "No conversations yet / Start a chat from Contacts." :146-150. H/G.
- WG-3: Fila: inicial (o "?"), preview ("You: " si mío; >40 → 39 + "…"), tiempo relativo now/Nm/Nh/Nd/Nw/Ny, badge no leídos ("9+" >9, oculto a 0). :159-186,199-214. H.
- WG-4: Abrir fila entra al chat y pone no-leídos a 0. :255-267,376-382. H.
- WG-5: Chat: título nombre, inicial, subtítulo número (en blanco si nombre == número), Back, "Call", composer. :216-247. G.
- WG-6: Burbujas en orden; "You" o nombre; enviadas #2f5d44, recibidas #26303f. :391-421; Whatgram.java:453-454. H.
- WG-7: Enviar no-vacío: trim, guarda, entrega, re-render; vacío solo re-render. :297-332. H.
- WG-8: Entrega rechazada en silencio si vacío, > `maxMessageCharacters` (512) o rate-limit. PR:190-206. H.
- WG-9: Destinatario con ese chat abierto → burbuja en vivo y leída; con la lista abierta → lista se refresca. :337-366. G.
- WG-10: Conectado con teléfono cerrado → guardado, no-leídos +1, toast, sin push. PR:231-237,303. G.
- WG-11: Offline → cola (si offline mail on); aparece al abrir; > `offlineMessageRetentionDays` (30) se descarta. PR:105-120,211-216,241; EP 3.1. H + G.
- WG-12: "Call" del header llama al contacto; Back a la lista. :243-246,269-288. G.

## CONTACTS (ContactsApp.java, DustContacts.ui, DustContactsEntry.ui)
- CON-1: Lista con "Add Contact" + filas "<name> (<number>)" con Chat/Call/Del. :403-436. G.
- CON-2: Add → formulario (Phone number, Display name, Save, Cancel). :438-452,461-470. G.
- CON-3: Save con ambos campos guarda (trim, sobrescribe mismo número) y vuelve; con un campo vacío vuelve sin cambios. :471-482. H.
- CON-4: Del elimina y re-renderiza. :483-493. H.
- CON-5: Call llama; Chat abre Whatgram en ese chat (ruta open_chat). :428-433,494-502. G.
- CON-6: Nombres de contacto en filas/títulos/burbujas de Whatgram; Calls usa el nombre del registro. H.

## SETTINGS (SettingsApp.java, DustSettings.ui)
- SET-1: Pestaña Player: número + Theme (Default), Wallpaper (Logo), Layout (Grid), Sound (Nonkia), Message alerts, preview SHOWN/HIDDEN, Incoming calls ALLOWED/DND. :179-218. H.
- SET-2: Solo valores en lista blanca; se guarda por device y re-render. :416-466. H.
- SET-3: "Restore Player Defaults" resetea todo. :217,427-431. H.
- SET-4: Efecto inmediato en toasts, DND, sonido y tema; wallpaper/layout en el siguiente Home. SetupManager.java:92-94; PR:169-177; DP:258-268. H + G.
- SET-5: Pestaña "Plugin" solo con `phonemod.admin.deviceframework`; sin él se fuerza Player. :38,156-170,318-337,385-387. H.
- SET-6: Plugin edita config en vivo (binding, llamadas, privacidad, cross-world, incoming, ring 15/30/60, speaker 4/8/16, offline mail, límite 128/512/1024); toast "Saved <key>"; inválidos rechazados. :220-267,342-377. H.
- SET-7: Estilos de pestaña DustShellTabStyleActive/Inactive. :167-168,288-291. G.

## APP STORE (AppStoreApp.java, AppCatalogService.java, AppInstallService.java)
- AS-1: Pestañas Installed/Available/Restricted/System, por defecto Installed; nombre desconocido mantiene la actual. :42,127-176. H.
- AS-2: Installed: instaladas compatibles no-system (sortOrder, id) con Uninstall; vacío → "No apps installed. Visit 'Available' to install apps." :214-236. H.
- AS-3: Available: no-system, compatibles, no instaladas, con permiso (solo el primero); vacío → "No apps available for installation." :238-261. H.
- AS-4: Restricted: no-system sin permiso con visibility "locked", badge 🔒 sin botón; "hidden" en ningún sitio; vacío → "No restricted apps to show." :263-282. H.
- AS-5: System: apps de sistema salvo la propia App Store, con Open (pasa por open_app). :284-302. H.
- AS-6: Fila: nombre, id, icono. :306-315. G.
- AS-7: Install falla con "You do not have permission to install this app." (desconocida, sin permiso, veto de evento); si no, aparece en Home y re-render. :180-193; AppInstallService.java:63-91. H.
- AS-8: Uninstall de system o no instalada → "This app cannot be uninstalled." :195-207. H.
- AS-9: App Store es system con sortOrder 100. :101-109. H.

## GROUP CHAT (GroupChatApp.java, groupchat/GroupChatState.java)
- GC-1: `groupchat` NO es system ni DefaultApps; se instala desde Available. H.
- GC-2: Sala "global" compartida; abrir suscribe y muestra burbujas Sender/Body. :546-549,599-618. H.
- GC-3: Enviar (trim, no vacío) añade; todos los visores, incluido el emisor, re-renderizan en vivo; remitente = número. :621-635; DP:629-654. G (multi-cliente).
- GC-4: Sobrevive reinicio; tope `persistentChannelMessages` (1000), descarta los más viejos. GroupChatState.java:551-559; EP 3.2. H + G.
- GC-5: Cerrar app/página o desconectar desuscribe. H.

## Comportamiento en código SIN paso en IN_GAME_EXIT_PASS.md
Todo el comportamiento por-app (WG-*, CON-*, SET-*, AS-*, GC-1/2/3/5, CALLS-1/2/12-UI); SHELL-4..9, 14 (salvo open_app forjado), 20, 27-29; WG-3/4/9/10; CALLS-4/5 y registros missed; SET-5/6; AS-*; comandos `/phonemod sendtext` y `/phonemod permissions …`; invalidación de shells al recargar assets (EventRegistryManager.java:28-33; DeviceService.java:80).

## Pasos del exit pass que no cuadran con el código
- EP 1.2/1.5: ninguna app nativa declara permiso; hace falta una app de terceros o v2 de prueba.
- EP 1.4: solo hay un device asset (Phone.json); no hay PC en Phonemod.
- EP 4.2/5.1: los comandos reales son `/deviceframework migration dry-run|status|conflicts|repair` (DeviceFrameworkCommand.java:21-29,52-56); `transfer <identityUUID> <newOwnerUUID>`.
- EP 4.3: no hay comando de migración; es perezosa por item en OpenPhone (:74-85); backup `.pre-v2-<ts>.bak` en el primer save del registro (IdentityRegistry.java:295-308).
- EP 4.2: dry-run solo imprime conteos (DeviceFrameworkCommand.java:57-62).
- EP 4.5: no hay comando de restore; es copia manual.
- EP 4.6: "Schema: v2 …" lo imprime `migration status`, no `status` (:32-49,63-68).
- EP 5.1: no hay audit-log en DeviceFrameworkCommand.
- EP 2.11: llamar a alguien ocupado falla sin registro missed ni UI (CR:195-199 vs CR:295-303).

## Divergencias (posibles bugs) a resolver antes de congelar la paridad
1. Dial pad muerto tras colgar: onCallEnded pone estado CALLS (DP:624), no hay ruta `start_call` y el fallback solo corre en APP (Rtr:132).
2. Nombres de contacto nunca llegan a las pantallas de llamada (CR:204 pasa `caller, caller`; CallsApp.java:126) → "555 (555)".
3. Dos etiquetas "Calling" distintas (DP:155 vs DP:597).
4. Posible raíz de app duplicada al llegar mensaje con otra app abierta (DP:554-556).
5. Permisos incoherentes: install/hidden/Available/Restricted miran solo el primero; open mira todos (DP:368 vs AppCatalogService.java:112,137; AppInstallService.java:74).
6. Nombre de contacto demasiado largo se pierde en silencio (addContact lanza; DP:205-210 lo traga).
7. Sin timbre ni overlay con el teléfono cerrado (requireOpenDevice=false): solo aparece al reabrir (CR:204; DP:158-161).
8. Sonido `MessageSent` declarado (Phone.json:17; DeviceSoundProfile.java:70) pero nunca se reproduce.
