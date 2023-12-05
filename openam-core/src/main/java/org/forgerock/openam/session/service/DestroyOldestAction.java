/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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
import com.iplanet.dpro.session.service.QuotaExhaustionActionImpl;

import java.util.HashSet;
import java.util.Map;
import org.apache.commons.lang.StringUtils;


/**
 * This action retrieves all the sessions using the Session service and
 * refreshes them in the local cache (so they have up-to-date session expiration
 * information). The session with the lowest max session time will be destroyed.
 *
 * @author Peter Major
 */
public class DestroyOldestAction extends QuotaExhaustionActionImpl {

	@Override
    public boolean action(InternalSession is, Map<String, Long> sessions) {
        long smallestExpTime = Long.MAX_VALUE;
        String oldestSessionID = null;
        for (String sid : new HashSet<String>(sessions.keySet())) 
        	if (!StringUtils.equals(is.getSessionID().toString(), sid)){
	            try {
	                Session session = new Session(new SessionID(sid));
	                session.refresh(false);
	                long expTime = session.getTimeLeft();
	                if (expTime <= smallestExpTime) {
	                    smallestExpTime = expTime;
	                    oldestSessionID = sid;
	                }
	            } catch (SessionException ssoe) {
	            	sessions.remove(sid); //session invalid
	            }
	        }
        if (oldestSessionID != null) { 
        	destroy(oldestSessionID,sessions);
		}
        return false;
    }
}
