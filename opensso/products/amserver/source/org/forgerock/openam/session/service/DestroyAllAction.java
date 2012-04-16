/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
import java.util.Set;

/**
 * This action will invalidate all currently existing sessions, but it will
 * let the current session to get created, so this way the user will always have
 * only one session.
 *
 * @author Steve Ferris
 */
public class DestroyAllAction implements QuotaExhaustionAction {

    private static Debug debug = SessionService.sessionDebug;

    @Override
    public boolean action(InternalSession is, Map sessions) {
        Set<String> sids = sessions.keySet();
        debug.message("there are " + sids.size() + " sessions");
        synchronized (sessions) {
            for (String sid : sids) {
                SessionID sessID = new SessionID(sid);

                try {
                    Session s = Session.getSession(sessID);
                    s.destroySession(s);
                    debug.message("Destroy sid " + sessID);
                } catch (SessionException se) {
                    if (debug.messageEnabled()) {
                        debug.message("Failed to destroy the next "
                                + "expiring session.", se);
                    }

                    // deny the session activation request
                    // in this case
                    return true;
                }
            }
        }
        return false;
    }
}