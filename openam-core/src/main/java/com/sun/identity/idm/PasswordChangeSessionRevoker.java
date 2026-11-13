/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.idm;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.service.access.SessionQueryManager;

final class PasswordChangeSessionRevoker {
    private static final Debug DEBUG = Debug.getInstance("amIdm");
    private final SessionQueryManager query;
    private final SessionCache cache;

    PasswordChangeSessionRevoker(SessionQueryManager query, SessionCache cache) {
        this.query = query;
        this.cache = cache;
    }

    void revoke(String userId, String currentSessionId) throws SessionException {
        for (String sessionId : query.getAllSessionsByUUID(userId).keySet()) {
            if (sessionId.equals(currentSessionId)) {
                continue;
            }
            try {
                Session session = cache.getSession(new SessionID(sessionId), true, false);
                session.destroySession(session);
            } catch (Exception e) {
                DEBUG.warning("Unable to revoke a session after password change", e);
            }
        }
    }
}
