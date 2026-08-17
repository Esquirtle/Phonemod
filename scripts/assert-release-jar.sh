#!/usr/bin/env bash
set -euo pipefail

jar_path="${1:-phonemod-runtime/target/Phonemod-2.0.0-alpha.1.jar}"

if [[ ! -f "$jar_path" ]]; then
    echo "Release JAR not found: $jar_path" >&2
    exit 1
fi

entries=$(unzip -Z1 "$jar_path")
required_entries=(
    "manifest.json"
    "esq/phonemod/api/DevicePlatform.class"
    "Common/UI/Custom/Pages/Phone/Phone.ui"
    "Common/Sounds/PlayerActions/Playground/message-sent.ogg"
    "Common/Sounds/PlayerActions/Playground/message-received.ogg"
    "Server/Phonemod/Devices/Phone.json"
    "Server/Item/Items/Phone.json"
    "Server/Audio/SoundEvents/Notification_Message_Received.json"
    "Server/Languages/en-US/phonemod.lang"
)

for entry in "${required_entries[@]}"; do
    if ! grep -Fxq "$entry" <<<"$entries"; then
        echo "Release JAR is missing required entry: $entry" >&2
        exit 1
    fi
done

manifest=$(unzip -p "$jar_path" manifest.json)
if ! grep -Fq '"Group": "esq.phonemod"' <<<"$manifest" \
        || ! grep -Fq '"Name": "Phonemod"' <<<"$manifest"; then
    echo "Release JAR manifest does not declare esq.phonemod:Phonemod" >&2
    exit 1
fi

echo "Release JAR assertions passed: $jar_path"
