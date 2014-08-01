/**
 * Copyright 2014 ForgeRock AS.
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
import com.iplanet.dpro.session.share.SessionInfo;

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
    public SessionInfo refresh(Session session, boolean reset) throws SessionException;

    /**
     * Performs the logout operation on the Session.
     *
     * This operation is intended to destroy the Session and perform any appropriate
     * Session related logic.
     *
     * @param session SessionID to logout.
     */
    public void logout(Session session) throws SessionException;

    /**
     * Destroys the Session by removing it and moving it to the DESTROY state.
     *
     * This operation is similar to the logout and uses similar behaviour.
     *
     * @param requester The requester's non null session used to authorize the destroy operation.
     * @param session The non null session to destroy.
     * @throws SessionException If there was an error while deleting the token.
     */
    public void destroy(Session requester, Session session) throws SessionException;

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
    public void setProperty(Session session, String name, String value) throws SessionException;
}
