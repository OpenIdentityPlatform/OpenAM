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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.common;

import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 12.0.0
 */
@Singleton
public class SessionManager {

    private final Map<String, String> sessions = new ConcurrentHashMap<String, String>();

    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean isValid(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public String create(String userId) {
        final String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, userId);
        return sessionId;
    }

    public String get(String sessionId) {
        return sessions.get(sessionId);
    }
}
