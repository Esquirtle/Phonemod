# Compatibility and versioning

The runtime plugin identifier remains `esq.phonemod:Phonemod`. `phonemod-api` follows
semantic versioning: patch releases fix compatible behavior; minor releases add compatible
API; major releases may remove or change contracts.

| Consumer | v2 API build | Runtime manifest | Canonical ID / legacy alias |
|---|---:|---:|---|
| AmazonMod | yes | yes | `amazonmod:amazon`, `amazonmod:auctionhouse` |
| Banking | yes | yes | `banking:banking` / `banking` |
| Boilerplate App | yes | yes | `myapp:example` / `myapp` (typed v2) |
| Computers | yes | yes | shared `PC` device API |
| Pocket Tasks | yes | yes | `pockettasks:pocket-tasks` / `pocket-tasks` |
| Relay Ops | yes | yes | `relayops:relay-ops` / `relay-ops` |
| Signal Sweep | yes | yes | `signalsweep:signal-sweep` / `signal-sweep` |
| SocialCore | yes | yes | dynamic `socialcore:org_*` / `org_*` |
| Trading App | yes | yes | `tradingapp:trading` / `trading` |

The v1 phone API is deprecated but supported and tested for every 2.x release. Removal is
reserved for 3.0 and will be called out in release notes at least one minor release before
removal. Security enforcement, bounds, and thread-affinity fixes may become stricter in a
minor/patch release because permissive client behavior is not a compatibility guarantee.
