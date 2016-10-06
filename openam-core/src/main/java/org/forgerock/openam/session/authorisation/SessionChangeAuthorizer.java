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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.session.authorisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.utils.CollectionUtils;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * Session Change Authorizer acts as a mini authorisation manager for changes to a session that need to be
 * permission checked.
 */
@Singleton
public class SessionChangeAuthorizer {

    private static final String PERMISSION_READ = "READ";
    private static final String PERMISSION_MODIFY = "MODIFY";
    private static final String PERMISSION_DELEGATE = "DELEGATE";

    private final Debug debug;
    private final SSOTokenManager ssoTokenManager;

    /**
     * Creates an instance of a SessionChangeAuthorizer.
     * @param debug The session debug instance.
     * @param ssoTokenManager The SSOTokenManager.
     */
    @Inject
    SessionChangeAuthorizer(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                            final SSOTokenManager ssoTokenManager) {
        this.debug = debug;
        this.ssoTokenManager = ssoTokenManager;
    }

    /**
     * Filter out any sessions from the collection that the actor does not have permission to access.
     * @param actorsSessionID The id of the acting session.
     * @param sessionsActedUpon The list of sessions that the actor is acting upon.
     * @return The filtered collection.
     * @throws SessionException If authorization fails.
     */
    public Collection<InternalSession> filterPermissionToAccess(
            final SessionID actorsSessionID,
            final Collection<InternalSession> sessionsActedUpon) throws SessionException {
        Collection<InternalSession> toReturn = new ArrayList<>();
        try {
            if (hasTopLevelAdminRole(actorsSessionID)) {
                toReturn.addAll(sessionsActedUpon);
                return toReturn;
            }
            AMIdentity user = getUser(actorsSessionID);
            Set orgList = user.getAttribute("iplanet-am-session-get-valid-sessions");
            if (orgList == null) {
                return Collections.emptySet();
            }
            for(InternalSession session : sessionsActedUpon) {
                if (orgList.contains(session.getClientDomain())) {
                    toReturn.add(session);
                }
            }
        } catch (SSOException | IdRepoException e) {
            throw new SessionException(e);
        }

        return toReturn;
    }

    /**
     * Returns true if the subject has top level admin role
     *
     * @param actorsSessionID SessionID of the current acting subject.
     * @throws SessionException
     * @throws SSOException
     */
    private boolean hasTopLevelAdminRole(final SessionID actorsSessionID) throws SessionException, SSOException {
        SSOToken ssoSession = ssoTokenManager.createSSOToken(actorsSessionID.toString());

        boolean topLevelAdmin = false;
        Set actions = CollectionUtils.asSet(PERMISSION_READ, PERMISSION_MODIFY, PERMISSION_DELEGATE);
        try {
            DelegationPermission perm = new DelegationPermission(
                    "/", "*", "*", "*", "*", actions, Collections.EMPTY_MAP);
            DelegationEvaluator evaluator = new DelegationEvaluatorImpl();
            topLevelAdmin = evaluator.isAllowed(ssoSession, perm, Collections.EMPTY_MAP);
        } catch (DelegationException de) {
            debug.error("SessionService.hasTopLevelAdminRole: failed to check the delegation permission.", de);
        }
        return topLevelAdmin;
    }

    /**
     * Checks if the requester has the necessary permission to destroy the provided session. The user has the necessary
     * privileges if one of these conditions is fulfilled:
     * <ul>
     *  <li>The requester attempts to destroy its own session.</li>
     *  <li>The requester has top level admin role (having read/write access to any service configuration in the top
     *  level realm).</li>
     *  <li>The session's client domain is listed in the requester's profile under the
     *  <code>iplanet-am-session-destroy-sessions service</code> service attribute.</li>
     * </ul>
     *
     * @param requester The requester's session.
     * @param sessionId The session to destroy.
     * @throws SessionException If none of the conditions above is fulfilled, i.e. when the requester does not have the
     * necessary permissions to destroy the session.
     */
    public void checkPermissionToDestroySession(final Session requester,
                                                final SessionID sessionId) throws SessionException {
        if (!hasPermissionToDestroySession(requester, sessionId)) {
            throw new SessionException(SessionBundle.rbName, "noPrivilege", null);
        }
    }

    /**
     * Checks if the requester has the necessary permission to destroy the provided session. The user has the necessary
     * privileges if one of these conditions is fulfilled:
     * <ul>
     *  <li>The requester attempts to destroy its own session.</li>
     *  <li>The requester has top level admin role (having read/write access to any service configuration in the top
     *  level realm).</li>
     *  <li>The session's client domain is listed in the requester's profile under the
     *  <code>iplanet-am-session-destroy-sessions service</code> service attribute.</li>
     * </ul>
     *
     * @param requester The requester's session.
     * @param sessionId The session to destroy.
     * @throws SessionException If none of the conditions above is fulfilled, i.e. when the requester does not have the
     * necessary permissions to destroy the session.
     */
    public boolean hasPermissionToDestroySession(final Session requester,
                                                 final SessionID sessionId) throws SessionException {
        if (requester.getState(false) != SessionState.VALID) {
            throw new SessionException(SessionBundle.getString("invalidSessionState") + sessionId.toString());
        }

        try {
            if (hasTopLevelAdminRole(requester.getSessionID())){
                return true;
            }
            // a session can destroy itself
            if (requester.getID().equals(sessionId)) {
                return true;
            }

            AMIdentity user = getUser(requester.getSessionID());
            Set<String> orgList = user.getAttribute("iplanet-am-session-destroy-sessions");
            if (!orgList.contains(requester.getClientDomain())) {
                return false;
            }
        } catch (Exception e) {
            throw new SessionException(e);
        }
        return true;
    }

    /**
     * Returns the User of the Session
     *
     * @param sessionID SessionID of the session to retrieve the user of.
     * @throws SessionException If something went wrong with the operation.
     * @throws SSOException If SSOToken creation failed.
     */
    private AMIdentity getUser(final SessionID sessionID) throws SessionException, SSOException {
        SSOToken ssoSession = ssoTokenManager.createSSOToken(sessionID.toString());
        AMIdentity user = null;
        try {
            user = IdUtils.getIdentity(ssoSession);
        } catch (IdRepoException e) {
            debug.error("SessionService: failed to get the user's identity object", e);
        }
        return user;
    }
}
