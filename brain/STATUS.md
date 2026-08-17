---
kind: plugin-status
id: Phonemod/STATUS
title: Phonemod status
plugin_id: Phonemod
last_verified: 2026-08-15
---

# Status — Phonemod

**Current task:** Separate Player and Plugin Settings tabs plus functional expanded player
preferences are deployed in 2.0.0-alpha.1 on real hytest. The expanded UI passed a real
client reconnect/parser check, and the list-layout choice plus all new preference fields
were persisted. Visual behavior confirmation and two-client behavior acceptance remain.

**Blockers:** Full Hytale asset validation is blocked by an upstream vanilla-instance
validator deadlock in `Gyms/Gym_Movement`. Migration/rollback rehearsals and the
multiplayer acceptance checklist require a populated test universe and players.

**Next action:** Connect multiplayer clients to hytest and execute the phone, ownership,
messaging, calling/audio, shared-device, and administration acceptance checklist. Then
rehearse copied-v1 migration/rollback and re-run full asset validation with a server build
containing the instance-validator fix.

The JDK 25 build, 26 automated tests, nine-consumer API compile matrix, deterministic JAR
inspection, isolated fresh-universe boot, persistent hytest integration boot, bundled
phone asset/creative-catalog smoke test, scalar UI-event compatibility test, Banking UI
pack load/client-join smoke test, ATM-vault inventory grid with a metadata-bearing phone,
live `/deviceframework` status/migration commands, and the updated configuration-pack
client join are green. Character Framework and AuctionHouse are also enabled in the
current hytest combination. See
[verification evidence](../docs/VERIFICATION_EVIDENCE.md).

**Roadmap:** [Phonemod Professionalization Plan](../PROFESSIONALIZATION_PLAN.md) — gated
1.0-to-2.0 implementation, migration, compatibility, test, and release plan.
