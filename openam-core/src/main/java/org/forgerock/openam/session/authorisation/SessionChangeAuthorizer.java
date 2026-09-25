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
 * Portions copyright 2025-2026 3A Systems LLC.
 */
package org.forgerock.openam.session.authorisation;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.Set;

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
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

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
     *
     * Gets the organisations assigned to the session subject
     *
     * @param sessionID The id of the session
     * @return The organisations the assigned to the session subject, or an empty set when the subject's identity or
     * the attribute cannot be resolved. Never null.
     */
    public Set<String> getSessionSubjectOrganisations(SessionID sessionID)
            throws SSOException, SessionException, IdRepoException {
        AMIdentity user = getUser(sessionID);
        if (user == null) {
            debug.warning("SessionChangeAuthorizer: the identity of the session subject could not be resolved, "
                    + "no organisations are granted.");
            return Collections.emptySet();
        }
        Set<String> organisations = user.getAttribute("iplanet-am-session-get-valid-sessions");
        return organisations == null ? Collections.<String>emptySet() : organisations;
    }

    /**
     * Returns true if the subject has top level admin role
     *
     * @param actorsSessionID SessionID of the current acting subject.
     * @throws SessionException if there is a problem accessing the session
     * @throws SSOException if the single sign on token is invalid or expired
     */
    public boolean hasTopLevelAdminRole(final SessionID actorsSessionID) throws SessionException, SSOException {
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
     * @param sessionToDestroy The session to destroy. Both the session id and the realm the permission is evaluated
     * against are derived from it, so that the two can never refer to different sessions.
     * @throws SessionException If none of the conditions above is fulfilled, i.e. when the requester does not have the
     * necessary permissions to destroy the session.
     */
    public void checkPermissionToDestroySession(final Session requester, final Session sessionToDestroy)
            throws SessionException {
        checkPermissionToDestroySession(requester, sessionToDestroy.getID(), sessionToDestroy.getClientDomain());
    }

    /**
     * Checks if the requester has the necessary permission to destroy the provided session.
     *
     * @param requester The requester's session.
     * @param sessionToDestroy The internal session to destroy. Both the session id and the realm the permission is
     * evaluated against are derived from it, so that the two can never refer to different sessions.
     * @throws SessionException If the requester does not have the necessary permissions to destroy the session.
     * @see #checkPermissionToDestroySession(Session, Session)
     */
    public void checkPermissionToDestroySession(final Session requester, final InternalSession sessionToDestroy)
            throws SessionException {
        checkPermissionToDestroySession(requester, sessionToDestroy.getID(), sessionToDestroy.getClientDomain());
    }

    /**
     * Checks the permission against an already resolved session id and realm.
     * <p>
     * Package private on purpose: the realm must always be the realm of the session identified by
     * {@code sessionId}, and passing the requester's own realm here is exactly the defect this class had. Callers
     * outside of this package have to hand over the session object so that the two facts cannot drift apart.
     *
     * @param requester The requester's session.
     * @param sessionId The id of the session to destroy.
     * @param sessionClientDomain The client domain (realm) of the session identified by {@code sessionId}. Never the
     * requester's own client domain.
     * @throws SessionException If the requester does not have the necessary permissions to destroy the session.
     */
    void checkPermissionToDestroySession(final Session requester, final SessionID sessionId,
                                         final String sessionClientDomain) throws SessionException {
        if (!hasPermissionToDestroySession(requester, sessionId, sessionClientDomain)) {
            throw new SessionException(SessionBundle.rbName, SessionConstants.NO_PRIVILEGE_ERROR_CODE, null);
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
     * @param sessionToDestroy The session to destroy. Both the session id and the realm the permission is evaluated
     * against are derived from it, so that the two can never refer to different sessions.
     * @throws SessionException If the state of the requester's session cannot be established.
     */
    public boolean hasPermissionToDestroySession(final Session requester, final Session sessionToDestroy)
            throws SessionException {
        return hasPermissionToDestroySession(requester, sessionToDestroy.getID(), sessionToDestroy.getClientDomain());
    }

    /**
     * Checks if the requester has the necessary permission to destroy the provided session.
     *
     * @param requester The requester's session.
     * @param sessionToDestroy The internal session to destroy. Both the session id and the realm the permission is
     * evaluated against are derived from it, so that the two can never refer to different sessions.
     * @throws SessionException If the state of the requester's session cannot be established.
     * @see #hasPermissionToDestroySession(Session, Session)
     */
    public boolean hasPermissionToDestroySession(final Session requester, final InternalSession sessionToDestroy)
            throws SessionException {
        return hasPermissionToDestroySession(requester, sessionToDestroy.getID(), sessionToDestroy.getClientDomain());
    }

    /**
     * Checks the permission against an already resolved session id and realm.
     * <p>
     * Package private on purpose, see {@link #checkPermissionToDestroySession(Session, SessionID, String)}.
     *
     * @param requester The requester's session.
     * @param sessionId The id of the session to destroy.
     * @param sessionClientDomain The client domain (realm) of the session identified by {@code sessionId}. Never the
     * requester's own client domain.
     * @throws SessionException If the state of the requester's session cannot be established.
     */
    boolean hasPermissionToDestroySession(final Session requester, final SessionID sessionId,
                                          final String sessionClientDomain) throws SessionException {
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

            // The realm restriction has to be evaluated against the session being destroyed, not against the
            // requester's own realm, otherwise the delegation is not scoped to a realm at all.
            if (StringUtils.isBlank(sessionClientDomain)) {
                debug.warning("SessionChangeAuthorizer: refusing to destroy a session, the realm of the session "
                        + "to destroy could not be determined.");
                return false;
            }

            AMIdentity user = getUser(requester.getSessionID());
            if (user == null) {
                debug.warning("SessionChangeAuthorizer: refusing to destroy a session, the identity of the "
                        + "requester could not be resolved.");
                return false;
            }
            Set<String> orgList = user.getAttribute("iplanet-am-session-destroy-sessions");
            if (orgList == null || !orgList.contains(sessionClientDomain)) {
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
    // Package private rather than private so that the authorisation logic can be unit tested.
    AMIdentity getUser(final SessionID sessionID) throws SessionException, SSOException {
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
