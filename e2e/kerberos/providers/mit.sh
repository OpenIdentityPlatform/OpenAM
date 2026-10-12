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

# KDC provider "mit": MIT Kerberos KDC in a container built from providers/mit.
#
# A provider sets its defaults and defines the functions bootstrap.sh calls:
#   kdc_start          build the image and start the KDC with the test user and service principal
#   kdc_wait           wait until the KDC is up and the service keytab exists
#   kdc_export_keytab  write the keytab for $KRB_SPN to stdout
#   kdc_logs           print the KDC logs for diagnostics
#   kdc_stop           remove the KDC container, so that the Docker network can be removed

: "${KRB_KDC_IMAGE:=openam-e2e-kdc}"
: "${KRB_KDC_CONTAINER:=openam-kdc}"
: "${KRB_PASSWORD:=changeit}"
: "${KRB_KDC_ADMIN_PASSWORD:=kdc-master-password}"
: "${KRB_KDC_WAIT_SECONDS:=30}"

MIT_PROVIDER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/mit" && pwd)"

kdc_build() {
    # --load: with a docker-container buildx builder (as set up in CI) the image would stay in the build cache.
    if docker buildx version >/dev/null 2>&1; then
        docker buildx build --load -t "$KRB_KDC_IMAGE" "$MIT_PROVIDER_DIR"
    else
        docker build -t "$KRB_KDC_IMAGE" "$MIT_PROVIDER_DIR"
    fi
}

kdc_start() {
    # The build resolves the base image on Docker Hub every time; retry its transient failures.
    local attempt
    for attempt in 1 2 3; do
        if kdc_build; then
            break
        fi
        if ((attempt == 3)); then
            return 1
        fi
        sleep 5
    done
    docker rm -f "$KRB_KDC_CONTAINER" >/dev/null 2>&1 || true
    docker run -d --name "$KRB_KDC_CONTAINER" --network "$KRB_DOCKER_NETWORK" \
        -p "$KRB_KDC_CLIENT_ADDR:88/tcp" \
        -e "KRB_REALM=$KRB_REALM" \
        -e "KRB_USER=$KRB_USER" \
        -e "KRB_PASSWORD=$KRB_PASSWORD" \
        -e "KRB_SPN=$KRB_SPN" \
        -e "KRB_KDC_ADMIN_PASSWORD=$KRB_KDC_ADMIN_PASSWORD" \
        "$KRB_KDC_IMAGE"
}

kdc_wait() {
    local deadline=$((SECONDS + KRB_KDC_WAIT_SECONDS))
    until docker exec "$KRB_KDC_CONTAINER" test -s /keytabs/http.keytab 2>/dev/null; do
        if [[ "$(docker inspect -f '{{.State.Running}}' "$KRB_KDC_CONTAINER" 2>/dev/null)" != "true" ]]; then
            echo "KDC container $KRB_KDC_CONTAINER exited" >&2
            return 1
        fi
        if ((SECONDS >= deadline)); then
            echo "KDC keytab did not appear in ${KRB_KDC_WAIT_SECONDS}s" >&2
            return 1
        fi
        sleep 1
    done
}

kdc_export_keytab() {
    docker exec "$KRB_KDC_CONTAINER" cat /keytabs/http.keytab
}

kdc_logs() {
    docker logs "$KRB_KDC_CONTAINER" 2>&1
}

kdc_stop() {
    docker rm -f "$KRB_KDC_CONTAINER" >/dev/null 2>&1 || true
}
