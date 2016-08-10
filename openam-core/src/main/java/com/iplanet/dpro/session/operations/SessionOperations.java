/**
 * Copyright 2014-2016 ForgeRock AS.
 *
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
 */
package com.iplanet.dpro.session.operations;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOToken;

/**
 * Describes the ability to perform an operation on a Session.
 *
 * These operations are specific to the Session Service, therefore all will be
 * keyed by the SessionID key.
 *
 * This abstraction layer is important as it will be implementation specific as to
 * how this operation will be performed.
 *
 * Note: Session instances are the currency of the
 * {@link com.iplanet.dpro.session.Session} Service. The aptly titled
 * {@link com.iplanet.dpro.session.service.SessionService} deals with InternalSessions.
 * The common currency between them is the SessionID which is suitably generic to use in
 * this interface.
 */
public interface SessionOperations {
    /**
     * Performs a refresh of the Session and return a summary of the Session information.
     *
     * This will optionally update the last modified timestamp of the Session.
     *
     * Note: This method is inconsistent from the other two in that it requires a Session
     * reference. This is because of a dependency which could not be resolved without
     * substantial refactoring in the Remote implementation.
     *
     * @param reset If true, then update the last modified timestamp of the Session.
     * @param session Reference to the Session.
     * @return Null if there was an error locating the Session, otherwise non null.
     */
    SessionInfo refresh(Session session, boolean reset) throws SessionException;

    /**
     * Destroys the Session by removing it and moving it to the DESTROY state.
     *
     * This operation is similar to the logout and uses similar behaviour.
     *
     * @param requester The requester's non null session used to authorize the destroy operation.
     * @param session The non null session to destroy.
     * @throws SessionException If there was an error while deleting the token.
     */
    void destroy(Session requester, Session session) throws SessionException;

    /**
     * Assigns the property to the Session.
     *
     * @param session The session to perform the set on.
     * @param name Non null name of the property.
     * @param value Non null value.
     * @throws SessionException SessionException if the session reached its maximum
     * session time, or the session was destroyed, or there was an error during
     * communication with session service, or if the property name or value was null.
     */
    void setProperty(Session session, String name, String value) throws SessionException;

    /**
     * Get the Session Info Object from the Session ID.
     * @param sessionId the Session Id of the required Session Info.
     * @param reset if true will cause the last access time on the session to be updated.
     * @return a Session Info object for the required session.
     * @throws SessionException if the session could not be accessed.
     */
    SessionInfo getSessionInfo(SessionID sessionId, boolean reset) throws SessionException;

    /**
     * Add a session listener notification url.  The url will receive a notification when session change events occur.
     * @param sessionId the session id to listen to.
     * @param url the listener notifcation url
     * @throws SessionException if the session could not be accessed.
     */
    void addSessionListener(SessionID sessionId, String url) throws SessionException;

    /**
     * Check that a session is a local session.
     * @param sessionId the session ID to check.
     * @return returns true if the session is local
     * @throws SessionException if the session could not be accessed.
     */
    boolean checkSessionLocal(SessionID sessionId) throws SessionException;

    /**
     * Gets the restricted token ID for a session.
     * @param masterSessionId the master session id to get the restricted token id for
     * @param restriction the Token Restriction type to use
     * @return a Restricted token ID as a String
     * @throws SessionException if the session could not be accessed.
     */
    String getRestrictedTokenId(SessionID masterSessionId, TokenRestriction restriction) throws SessionException;

    /**
     * Given a restricted token, returns the SSOTokenID of the master token
     * can only be used if the requester is an app token
     *
     * @param session Must be an app token
     * @param restrictedID The SSOTokenID of the restricted token
     * @return The SSOTokenID string of the master token
     * @throws SessionException If the master token cannot be de-referenced
     */
    String deferenceRestrictedID(Session session, SessionID restrictedID) throws SessionException;

    /**
     * Sets an external property in the session.  If the property is protected then it will throw a SessionException.
     *
     * @param clientToken SSO Token of the client setting external property.
     * @param sessionId The Id of the session to set the property on
     * @param name the name of the property
     * @param value the new value of the property
     * @throws SessionException If the Session could not be accessed or the property is protected.
     */
    void setExternalProperty(SSOToken clientToken, SessionID sessionId, String name, String value) throws SessionException;

    /**
     * Performs the logout operation on the Session.
     *
     * This operation is intended to destroy the Session and perform any appropriate
     * Session related logic.
     *
     * @param session Session to logout.
     */
    void logout(final Session session) throws SessionException;

    /**
     * Retrieves the Session from the Session ID.
     * @param sessionID the ID of the session to resolve
     * @return the Session Object
     * @throws SessionException if the session could not be accessed.
     */
    Session resolveSession(SessionID sessionID) throws SessionException;

    /**
     * Called to notify the relevant code that an InternalSession has been updated.
     * @param session The session that was updated.
     */
    void update(InternalSession session);
}
