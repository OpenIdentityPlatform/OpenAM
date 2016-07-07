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

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.session.SessionPLLSender;
import org.forgerock.openam.session.SessionServiceURLService;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;


/**
  * <code>SessionCount</code> represents the session count for a given user
  * in 3 different mutually exclusive deployment modes from which the user 
  * sessions can be obtained.
  *
  * <ul>
  * <li> SINGLE_SERVER_MODE : 
  *I    Simply return the local sessions for the given user.
  * <li> MULTI_SERVER_MODE :  
  * Query other AM servers for the sessions for the  given user Add the numbers 
  * up (including the local one) and return the value. If any of the AM servers 
  * is down, simply ignores it since all the sessions maintained by that instance
  * are not available.                        
  * <li> SFO_MODE : Fetch the sessions for the given user directly from the 
  * session repository.
  * </ul>
  */
public class SessionCount {

    // SessionInfoMap: uuid -> Set (list of sids)
    private static Map uuidSessionMap = Collections
            .synchronizedMap(new HashMap());

    private static Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));

    private static SSOToken adminToken = null;

    private static boolean useLocalSessionsInMultiServerMode = false;

    private static final SessionServiceURLService SESSION_SERVICE_URL_SERVICE = InjectorHolder.getInstance(SessionServiceURLService.class);
    private static final SessionPLLSender sessionPLLSender = InjectorHolder.getInstance(SessionPLLSender.class);

    private static boolean caseSensitiveUUID =
        SystemProperties.getAsBoolean(Constants.CASE_SENSITIVE_UUID);

    private static final SessionService sessionService = InjectorHolder.getInstance(SessionService.class);
    private static final SessionServerConfig serverConfig = InjectorHolder.getInstance(SessionServerConfig.class);
    private static final SessionServiceConfig serviceConfig = InjectorHolder.getInstance(SessionServiceConfig.class);

    static {
        try {
            SSOTokenManager.getInstance();
        } catch (Exception ssoe) {
            debug.error("SessionConstraint: Failied to get the "
                    + "SSOTokenManager instance.");
        }

        // Without this property defined the default will be false which is
        // backwards compatable.
        useLocalSessionsInMultiServerMode = 
                SystemProperties.getAsBoolean(Constants.USE_LOCAL_SESSIONS_IN_MULTI_SERVER_MODE);
        if (debug.messageEnabled()) {
            debug.message("SessionCount: useLocalSessionsInMultiServerMode set to " + useLocalSessionsInMultiServerMode);                        
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user
     * (uuid). The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid
     *            User's universal unique ID.
     * @return user Sessions
     * @exception Exception
     *             if there is any problem with accessing the session
     *             repository.
     */
    public static Map getAllSessionsByUUID(String uuid) throws Exception {
        if (!caseSensitiveUUID) {
            uuid = uuid.toLowerCase();
        }

        Map<String, Long> sessions = getSessionsFromRepository(uuid);

        if (sessions == null) {
            sessions = Collections.EMPTY_MAP;
            debug.error("Error: Unable to determine session count for user: " + uuid + 
                    " returning empty map");
        }
        
        return sessions;
    }
        
    /*
     * Return true if the Constants.USE_LOCAL_SESSIONS_IN_MULTI_SERVER_MODE property 
     * has been defined and set to true.
     */
    static boolean useLocalSessionsInMultiServerMode() {
        return useLocalSessionsInMultiServerMode;
    }

    /*
     * Get user sessions from local server
     */
    static Map<String, Long> getSessionsFromLocalServer(String uuid) {
        Set<SessionID> sessions = (Set<SessionID>) uuidSessionMap.get(uuid);
        Map<String, Long> retSessions = new HashMap<String, Long>();

        if (sessions != null) {
            synchronized (sessions) {
                for (SessionID sid : sessions) {
                    InternalSession is = sessionService.getInternalSession(sid);
                    
                    if (is != null) {
                        retSessions.put(sid.toString(), new Long(is.getExpirationTime()));
                    }
                }
            }
        }
        
        return retSessions;
    }

    private static Map<String, Long> getSessionsFromRepository(String uuid) throws Exception {

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
        } catch (Exception e) {
            debug.error("SessionCount.getSessionsFromRepository: "+
                "Session repository is not available", e);            
            throw e;
        }
    }

   /*
    * Gets the admin token for checking the session constraints for the users
    * @return admin <code>SSOTken</code>
    */
    static SSOToken getAdminToken() {

        if (adminToken == null) {
            try {
                adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            } catch (Exception e) {
                debug.error("Failed to get the admin token for Session constraint checking.", e);
            }
        }
        return adminToken;
    }
}
