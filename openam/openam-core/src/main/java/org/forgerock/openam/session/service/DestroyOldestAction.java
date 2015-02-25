/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html See the License for the specific
 * language governing permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://forgerock.org/license/CDDLv1.0.html If
 * applicable, add the following below the CDDL Header, with the fields enclosed
 * by brackets [] replaced by your own identifying information: "Portions
 * Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.session.service;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.QuotaExhaustionAction;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;

/**
 * This action retrieves all the sessions using the Session service and
 * refreshes them in the local cache (so they have up-to-date session expiration
 * information). The session with the lowest max session time will be destroyed.
 *
 * @author Peter Major
 */
public class DestroyOldestAction implements QuotaExhaustionAction {

    private static Debug debug = SessionService.sessionDebug;

    @Override
    public boolean action(InternalSession is, Map<String, Long> sessions) {
        long smallestExpTime = Long.MAX_VALUE;
        String oldestSessionID = null;
        for (Map.Entry<String, Long> entry : sessions.entrySet()) {
            try {
                Session session = Session.getSession(new SessionID(entry.getKey()));
                session.refresh(false);
                long expTime = session.getTimeLeft();
                if (expTime < smallestExpTime) {
                    smallestExpTime = expTime;
                    oldestSessionID = entry.getKey();
                }
            } catch (SessionException ssoe) {
                if (debug.warningEnabled()) {
                    debug.warning("Failed to create SSOToken", ssoe);
                }
                // deny the session activation request
                // in this case
                return true;
            }
        }

        if (oldestSessionID != null) {
            SessionID sessID = new SessionID(oldestSessionID);
            try {
                Session s = Session.getSession(sessID);
                s.destroySession(s);
            } catch (SessionException e) {
                if (debug.messageEnabled()) {
                    debug.message("Failed to destroy the next expiring session.", e);
                }
                // deny the session activation request
                // in this case
                return true;
            }
        }
        return false;
    }
}
