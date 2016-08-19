/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SessionCount.java,v 1.5 2008/06/25 05:41:31 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import static org.forgerock.openam.session.SessionConstants.*;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;


/**
  * <code>SessionCount</code> represents the session count for a given user.
  * Fetches the sessions for the given user directly from the session repository.
  */
public class SessionCount {

    private static Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));

    private static boolean caseSensitiveUUID =
        SystemProperties.getAsBoolean(Constants.CASE_SENSITIVE_UUID);

    private static final SessionService sessionService = InjectorHolder.getInstance(SessionService.class);

    static {
        try {
            SSOTokenManager.getInstance();
        } catch (Exception ssoe) {
            debug.error("SessionConstraint: Failied to get the "
                    + "SSOTokenManager instance.");
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user
     * (uuid). The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid
     *            User's universal unique ID.
     * @return user Sessions
     * @exception SessionException
     *             if there is any problem with accessing the session
     *             repository.
     */
    public static Map getAllSessionsByUUID(String uuid) throws SessionException {
        if (!caseSensitiveUUID) {
            uuid = uuid.toLowerCase();
        }

        Map<String, Long> sessions;
        try {
            sessions = getSessionsFromRepository(uuid);
        } catch (CoreTokenException e) {
            throw new SessionException(e);
        }

        if (sessions == null) {
            sessions = Collections.emptyMap();
            debug.error("Error: Unable to determine session count for user: " + uuid + 
                    " returning empty map");
        }
        
        return sessions;
    }

    private static Map<String, Long> getSessionsFromRepository(String uuid) throws CoreTokenException {

        CTSPersistentStore repo = sessionService.getRepository();
        try {
            // Filter and Query the CTS
            TokenFilter filter = new TokenFilterBuilder()
                    .returnAttribute(SessionTokenField.SESSION_ID.getField())
                    .returnAttribute(CoreTokenField.EXPIRY_DATE)
                    .and()
                    .withAttribute(CoreTokenField.USER_ID, uuid)
                    .build();
            Collection<PartialToken> partialTokens = repo.attributeQuery(filter);

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "getSessionsFromRepository query success:\n" +
                        "Query: {0}\n" +
                        "Count: {1}",
                        filter,
                        partialTokens.size()));
            }

            // Populate the return Map from the query results.
            Map<String, Long> sessions = new HashMap<String, Long>();
            for (PartialToken partialToken : partialTokens) {
                // Session ID
                String sessionId = partialToken.getValue(SessionTokenField.SESSION_ID.getField());

                // Expiration Date converted to Unix Time
                Calendar timestamp = partialToken.getValue(CoreTokenField.EXPIRY_DATE);
                long unixTime = TimeUtils.toUnixTime(timestamp);

                sessions.put(sessionId, unixTime);
            }

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        "getSessionsFromRepository query results:\n" +
                        "{0}",
                        sessions));
            }

            return sessions;
        } catch (CoreTokenException e) {
            debug.error("SessionCount.getSessionsFromRepository: "+
                "Session repository is not available", e);
            throw e;
        }
    }
}
