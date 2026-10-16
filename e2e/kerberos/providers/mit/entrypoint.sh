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

# Creates the realm, the test user and the service principal, exports the service keytab to
# /keytabs/http.keytab and runs the KDC in the foreground. Logs go to the container output.

set -euo pipefail

: "${KRB_REALM:?}" "${KRB_USER:?}" "${KRB_PASSWORD:?}" "${KRB_SPN:?}" "${KRB_KDC_ADMIN_PASSWORD:?}"

cat > /etc/krb5.conf <<EOF
[libdefaults]
  default_realm = $KRB_REALM
  dns_lookup_kdc = false
  dns_lookup_realm = false

[realms]
  $KRB_REALM = {
    kdc = localhost
  }
EOF

mkdir -p /etc/krb5kdc /var/lib/krb5kdc /keytabs
cat > /etc/krb5kdc/kdc.conf <<EOF
[realms]
  $KRB_REALM = {
    database_name = /var/lib/krb5kdc/principal
    key_stash_file = /etc/krb5kdc/stash
    acl_file = /etc/krb5kdc/kadm5.acl
    supported_enctypes = aes256-cts-hmac-sha1-96:normal aes128-cts-hmac-sha1-96:normal
  }

[logging]
  kdc = STDERR
  default = STDERR
EOF
: > /etc/krb5kdc/kadm5.acl

kdb5_util create -s -r "$KRB_REALM" -P "$KRB_KDC_ADMIN_PASSWORD"
# kadmin.local splits the query into words itself: quote the password, doubling any '"' in it.
kadmin.local -r "$KRB_REALM" -q "addprinc -pw \"${KRB_PASSWORD//\"/\"\"}\" $KRB_USER@$KRB_REALM"
kadmin.local -r "$KRB_REALM" -q "addprinc -randkey $KRB_SPN@$KRB_REALM"
# kadmin.local exits with 0 even when the query fails.
for principal in "$KRB_USER@$KRB_REALM" "$KRB_SPN@$KRB_REALM"; do
    if ! kadmin.local -r "$KRB_REALM" -q "getprinc $principal" 2>/dev/null | grep -Fx "Principal: $principal" >/dev/null; then
        echo "Principal $principal was not created" >&2
        exit 1
    fi
done
# Export under a temporary name so that a reader never sees a partly written keytab.
kadmin.local -r "$KRB_REALM" -q "ktadd -k /keytabs/http.keytab.tmp $KRB_SPN@$KRB_REALM"
mv /keytabs/http.keytab.tmp /keytabs/http.keytab

exec krb5kdc -n
