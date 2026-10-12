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

# MIT Kerberos KDC for the Kerberos e2e test. The realm, principals and passwords are not baked
# into the image: entrypoint.sh creates them from the environment on every start.
FROM debian:bookworm-slim

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends krb5-kdc krb5-admin-server \
 && rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh /entrypoint.sh

EXPOSE 88/tcp
ENTRYPOINT ["/bin/bash", "/entrypoint.sh"]
