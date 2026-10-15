# Phonemod 3.0 — Estado
Fuente de verdad: rama `feature/v3` (worktree `~/workspace/plugins-srcs/Phonemod-v3`). Las docs de `feature/v2-refactor` quedan congeladas en c661929.
Stage: 3-build
Phase: F3 (extracción del core) — F3-1 architect en curso
Running: —
Last completed (2026-10-15):
- SPEC v0 aprobada (D-24) + D-25..D-28 (respuestas del Architect), R-API-9 añadido
- F2-1 plan del esqueleto (phases/F2/plan-skeleton.md)
- F2-2/3/4 en b3b12e5: api/v3 (18 tipos), módulo phonemod-ui-kit, 62 tests de contrato, bin/check-api-no-ui-imports.sh, bin/check-test-renames.sh
- F2-5 IN_GAME_EXIT_PASS_V3.md
- Verificación (re-ejecutada por el orquestador, mvn clean): default 96/96 verdes; -Pcontract 62 tests = 5 verdes (4 pins de firma + invalidIdRejected) y 57 rojos, todos con marcador F4 (54 UnsupportedOperationException, 3 fail explícitos), 0 por otra causa; scripts 0 + self-tests 0; jar con 20 clases api/v3 y 0 ui/kit; assert-release-jar 0; boot desechable → `Enabled plugin esq.phonemod:Phonemod`, `Hytale Server Booted!`
Desviaciones aceptadas del implementer: DeviceType y GuardVerdict tienen constructor real (constantes estáticas propias; un F4 rompería <clinit>); ActionGuards.authorizeOpen devuelve GuardVerdict (ui-kit no ve security).
Next: aprobación del gate F2 → F3 (extracción del core): F3-1 architect
Waiting on user: Q-11 (R-PAR-ADM); prueba con cliente del spike F1 (Q-1..Q-4)
Updated: 2026-10-15
