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
 * $Id: SessionConstraint.java,v 1.6 2009/11/21 01:13:24 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */
package com.iplanet.dpro.session.service;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.session.service.DestroyOldestAction;

/**
 * <code>SessionConstraint</code> represents the session quota for a given user
 * and performs the necessary actions based on the session quota limit, If the 
 * user reaches maximum allowed session limit , the activation request of 
 * new session should be rejected.
 * <p>
 *  If this is the "session upgarde" case, the session quota
 *  checking is bypassed when activating the new session. This 
 *  is due to the fact that during the session upgrade process
 *  the old session will not be destroyed until this new
 *  session is successfully activated. 
 * <p>
 *  The session count however still has to be incremented 
 *  because there is indeed a new valid session being created.
 *  As a result, there is a very small window where both old 
 *  session and the new session are both valid and if there is
 *  another client trying to create a new session for the same
 *  user in the meantime, it might not be allowed to do so due
 *  to session quota being exceeded. This conservative approach
 *  is considered as acceptable here.
 *
 *
 */
 
public class SessionConstraint {

    public static final String DESTROY_OLDEST_SESSION_CLASS =
            "org.forgerock.openam.session.service.DestroyOldestAction";

    private static final int DEFAULT_QUOTA = Integer.MAX_VALUE;

    private static final String AM_SESSION_SERVICE = "iPlanetAMSessionService";

    private static final String SESSION_QUOTA_ATTR_NAME = 
        "iplanet-am-session-quota-limit";

    private static Debug debug = SessionService.sessionDebug;

    private static QuotaExhaustionAction quotaExhaustionAction = null;

    /*
     * Get the session service
     */
    static {
        // FIXME Is this initialization necessary?
        getSS();
    }

    static SessionService getSS() {
        return SessionCount.getSS();
    }

    private static QuotaExhaustionAction getQuotaExhaustionAction() {
        String clazzName = SessionService.getConstraintHandler();
        if (quotaExhaustionAction != null
                && quotaExhaustionAction.getClass().getName().equals(clazzName)) {
            return quotaExhaustionAction;
        } else {
            try {
                quotaExhaustionAction = Class.forName(clazzName).asSubclass(
                        QuotaExhaustionAction.class).newInstance();
            } catch (Exception ex) {
                debug.error("Unable to load the Session Quota Exhaustion Action "
                        + "class: " + clazzName
                        + "\nFalling back to DESTROY_OLDEST_SESSION mode", ex);
                quotaExhaustionAction = new DestroyOldestAction();
            }
            return quotaExhaustionAction;
        }
    }

    /**
     * Check if the session quota for a given user has been exhausted and
     * perform necessary actions in such as case.
     * 
     * @param is
     * @return true if the session activation request should be rejected, false
     *         otherwise
     */
    protected static boolean checkQuotaAndPerformAction(InternalSession is) {
        boolean reject = false;
        int sessionCount = -1;

        // Disable the session quota constraint checking if in
        // MULTI_SERVER_MODE mode.
        if (SessionCount.getDeploymentMode() == SessionCount.MULTI_SERVER_MODE) {
            // Override default behaviour if using local sessions in MULTI_SERVER_MODE
            if (!SessionCount.useLocalSessionsInMultiServerMode()) {
                return false;
            }
        }

        // Check if it is upgrade scenario
        if (is.getIsSessionUpgrade()) {
            SessionCount.incrementSessionCount(is);
            return false;
        }

        // Step 1: get constraints for the given user via IDRepo
        int quota = getSessionQuota(is);

	// Step 2: get the information (session id and expiration
	// time) of all sessions for the given user from all
	// AM servers and/or session repository
	Map sessions = null;
	try {
	    sessions  =
		SessionCount.getAllSessionsByUUID(is.getUUID());
	} catch (Exception e) {
	    if (getSS().denyLoginIfDBIsDown()) {
		if (debug.messageEnabled()) {
		    debug.message("SessionConstraint." +
                        "checkQuotaAndPerformAction: " +
                        "denyLoginIfDBIsDown=true => "+
			"The session repository is down and "+
			"the login request will be rejected. ");
		}
                return true;
            } else {
		if (debug.messageEnabled()) {
	            debug.message("SessionConstraint." +
                        "checkQuotaAndPerformAction: " +
                        "denyLoginIfDBIsDown=false => "+
	   		"The session repository is down and "+
	                          "there will be no constraint checking.");
	   	}
	   	return false;
            }
	 }

	 if (sessions != null) {
	    sessionCount = sessions.size();
	}

	// Step 3: checking the constraints
	if (sessionCount >= quota) {
	    // If the session quota is exhausted, invoke the
	    // pluggin to determine the desired behavior.
	    reject = getQuotaExhaustionAction().action(is, sessions);
	    if (debug.messageEnabled()) {
			debug.message("SessionConstraint." +
                        "checkQuotaAndPerformAction: " +
                        "Session quota exhausted.");
            }
	}
        if (!reject) {
	    SessionCount.incrementSessionCount(is);
	}
	return reject;
    }

    /*
     * Get the sessionQuota
     * @param InternalSession 
     * @return session quota
     */
    private static int getSessionQuota(InternalSession is) {

        // Note: this method may have to be further enhanced to
        // retrieve the user based session quota settings using
        // the latest IDRepo interfaces.

        int quota = getDefaultSessionQuota();

        try {
            AMIdentity iden = IdUtils.getIdentity(SessionCount.getAdminToken(),
                    is.getUUID());
            Map serviceAttrs = 
                iden.getServiceAttributesAscending(AM_SESSION_SERVICE);
            Set s = (Set) serviceAttrs.get(SESSION_QUOTA_ATTR_NAME);
            Iterator attrs = s.iterator();
            if (attrs.hasNext()) {
                String attr = (String) attrs.next();
                quota = (Integer.valueOf(attr)).intValue();
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Failed to get the session quota via the "
                        + "IDRepo interfaces, => Use the default "
                        + "value from the dynamic schema instead.", e);
            }

        }
        return quota;
    }

    /*
     * Gets the default allowed sessions quota
     */
    public static int getDefaultSessionQuota() {

        int quota = DEFAULT_QUOTA;
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                    AM_SESSION_SERVICE, SessionCount.getAdminToken());
            ServiceSchema schema = ssm.getDynamicSchema();
            Map attrs = schema.getAttributeDefaults();
            quota = CollectionHelper.getIntMapAttr(
                attrs, SESSION_QUOTA_ATTR_NAME, String.valueOf(DEFAULT_QUOTA),
                debug);
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Failed to get the default session quota "
                        + "setting. => Set user session quota to "
                        + "Integer.MAX_VALUE.", e);
            }
        }
        return quota;
    }
}
