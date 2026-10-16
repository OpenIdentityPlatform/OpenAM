#!/bin/bash

# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2026 3A Systems, LLC.

# Prepares the Kerberos (WindowsDesktopSSO) e2e test:
#   1. starts a KDC through the provider selected by KRB_PROVIDER (providers/<name>.sh);
#   2. installs the service keytab into the OpenAM container;
#   3. configures the WindowsDesktopSSO module and the chain "module -> DataStore" with ssoadm;
#   4. obtains a TGT for the test user into .work/krb5cc for the browser and curl;
#   5. writes the resolved values to .work/kerberos.json for the spec.
#
# Usage: bootstrap.sh         set everything up; safe to run again
#        bootstrap.sh logs    print the KDC and module logs
#        bootstrap.sh stop    stop the KDC
#
# Every setting is an environment variable; see kerberos.env and providers/*.sh for defaults.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$HERE/.work"

# ─── Settings: environment, then common defaults, then the provider, then derived values ──
: "${KRB_PROVIDER:=mit}"
# shellcheck source=kerberos.env
source "$HERE/kerberos.env"

PROVIDER_FILE="$HERE/providers/$KRB_PROVIDER.sh"
if [[ ! "$KRB_PROVIDER" =~ ^[A-Za-z0-9_-]+$ || ! -f "$PROVIDER_FILE" ]]; then
    echo "Unknown KRB_PROVIDER '$KRB_PROVIDER'. Available providers:" >&2
    for provider in "$HERE"/providers/*.sh; do
        echo "  $(basename "$provider" .sh)" >&2
    done
    exit 2
fi
# shellcheck source=providers/mit.sh
source "$PROVIDER_FILE"

: "${KRB_SERVICE_PRINCIPAL:=$KRB_SPN@$KRB_REALM}"
: "${KRB_KDC_HOST:=$KRB_KDC_CONTAINER}"

OPENAM_DEBUG_DIR="/usr/openam/config/openam/debug"

if [[ "${1:-}" == "logs" ]]; then
    echo "===== KDC ($KRB_PROVIDER) ====="
    kdc_logs || true
    echo "===== OpenAM WindowsDesktopSSO debug log ====="
    docker exec "$KRB_OPENAM_CONTAINER" sh -c 'cat "$1"/amAuthWindowsDesktopSSO 2>/dev/null' sh "$OPENAM_DEBUG_DIR" || true
    exit 0
fi
if [[ "${1:-}" == "stop" ]]; then
    kdc_stop
    exit 0
fi

ssoadm() {
    local subcommand="$1"
    shift
    docker exec -w /usr/openam/ssoadmintools/openam/bin "$KRB_OPENAM_CONTAINER" \
        ./ssoadm "$subcommand" --adminid amadmin --password-file pwd.txt "$@"
}

json_string() {
    local value="${1//\\/\\\\}"
    printf '"%s"' "${value//\"/\\\"}"
}

if ! klist -V 2>/dev/null | grep -q '^Kerberos 5 version'; then
    echo "MIT Kerberos client tools are required (kinit, klist; Debian/Ubuntu package krb5-user)" >&2
    exit 2
fi
if ! command -v envsubst >/dev/null; then
    echo "envsubst is required (Debian/Ubuntu package gettext-base)" >&2
    exit 2
fi

mkdir -p "$WORK"

echo "Starting KDC (provider $KRB_PROVIDER, realm $KRB_REALM)"
kdc_start
kdc_wait

echo "Installing the keytab for $KRB_SPN into $KRB_OPENAM_CONTAINER:$KRB_KEYTAB_PATH"
kdc_export_keytab > "$WORK/http.keytab"
docker exec -i "$KRB_OPENAM_CONTAINER" sh -c 'cat > "$1"' sh "$KRB_KEYTAB_PATH" < "$WORK/http.keytab"

# The JDK accepts only AES keys by default (RC4 and DES are disabled since JDK 17).
keytab_entries="$(klist -ke "$WORK/http.keytab")"
echo "$keytab_entries"
if ! grep -F "$KRB_SPN@$KRB_REALM" <<<"$keytab_entries" | grep -Eq '\(aes(128|256)-cts'; then
    echo "The keytab has no AES key for $KRB_SPN@$KRB_REALM" >&2
    exit 1
fi

echo "Configuring auth module $KRB_MODULE_NAME and chain $KRB_CHAIN_NAME"
instances="$(ssoadm list-auth-instances --realm /)"
if ! grep -Fxq "$KRB_MODULE_NAME, [type=WindowsDesktopSSO]" <<<"$instances"; then
    ssoadm create-auth-instance --realm / --name "$KRB_MODULE_NAME" --authtype WindowsDesktopSSO
fi
ssoadm update-auth-instance --realm / --name "$KRB_MODULE_NAME" --attributevalues \
    "iplanet-am-auth-windowsdesktopsso-principal-name=$KRB_SERVICE_PRINCIPAL" \
    "iplanet-am-auth-windowsdesktopsso-keytab-file=$KRB_KEYTAB_PATH" \
    "iplanet-am-auth-windowsdesktopsso-kerberos-realm=$KRB_REALM" \
    "iplanet-am-auth-windowsdesktopsso-kdc=$KRB_KDC_HOST" \
    "iplanet-am-auth-windowsdesktopsso-returnRealm=false" \
    "iplanet-am-auth-windowsdesktopsso-auth-level=1"

chains="$(ssoadm list-auth-cfgs --realm /)"
if ! grep -Fxq "$KRB_CHAIN_NAME" <<<"$chains"; then
    ssoadm create-auth-cfg --realm / --name "$KRB_CHAIN_NAME"
fi
# Replaces the whole entry list, so a second run leaves the same chain.
ssoadm update-auth-cfg-entr --realm / --name "$KRB_CHAIN_NAME" \
    --entries "$KRB_MODULE_NAME|SUFFICIENT" "DataStore|REQUIRED"

echo "Obtaining a ticket for $KRB_USER@$KRB_REALM"
KRB_REALM="$KRB_REALM" KRB_KDC_CLIENT_ADDR="$KRB_KDC_CLIENT_ADDR" KRB_SPN_HOST="$KRB_SPN_HOST" \
    envsubst '${KRB_REALM} ${KRB_KDC_CLIENT_ADDR} ${KRB_SPN_HOST}' \
    < "$HERE/krb5.conf.template" > "$WORK/krb5.conf"
export KRB5_CONFIG="$WORK/krb5.conf"
export KRB5CCNAME="FILE:$WORK/krb5cc"
for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if printf '%s\n' "$KRB_PASSWORD" | kinit "$KRB_USER@$KRB_REALM" >/dev/null; then
        break
    fi
    if ((attempt == 10)); then
        echo "kinit failed" >&2
        exit 1
    fi
    sleep 2
done
klist

{
    printf '{\n'
    printf '  "provider": %s,\n' "$(json_string "$KRB_PROVIDER")"
    printf '  "realm": %s,\n' "$(json_string "$KRB_REALM")"
    printf '  "user": %s,\n' "$(json_string "$KRB_USER")"
    printf '  "module": %s,\n' "$(json_string "$KRB_MODULE_NAME")"
    printf '  "chain": %s\n' "$(json_string "$KRB_CHAIN_NAME")"
    printf '}\n'
} > "$WORK/kerberos.json"
echo "Kerberos e2e environment is ready"
