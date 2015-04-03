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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.session.blacklist;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;

/**
 * Records a blacklist of sessions that have been destroyed/logged out to ensure that they cannot be reused. Used in
 * stateless sessions to provide immediate log out functionality as there is no definitive server-side list of active
 * sessions that could otherwise be used to check validity.
 *
 * @since 13.0.0
 */
public interface SessionBlacklist {
    /**
     * Blacklists the given session until its expiry time.
     *
     * @param session the session to blacklist.
     * @throws SessionException if the session cannot be blacklisted for any reason.
     */
    void blacklist(Session session) throws SessionException;

    /**
     * Determines whether the session has previously been blacklisted. <strong>Note:</strong> sessions are only
     * blacklisted until they expire, so a {@code false} result does not mean the session is valid. Further checks
     * should be made to establish session validity.
     *
     * @param session the session to check for blacklisting.
     * @return {@code true} if the session is currently blacklisted, or {@code false} if the session is not
     * blacklisted or has expired (and therefore been removed from the blacklist).
     * @throws SessionException if an error occurs when checking the blacklist.
     */
    boolean isBlacklisted(Session session) throws SessionException;

    /**
     * Subscribe for notifications when sessions are blacklisted. Depending on the implementation, this may include
     * only sessions blacklisted in the local machine, or also notifications for sessions blacklisted on other nodes
     * in the cluster.
     *
     * @param listener the event listener to call when sessions are blacklisted.
     */
    void subscribe(Listener listener);

    interface Listener {
        /**
         * Indicates that the given session has been blacklisted.
         *
         * @param id the stable id of the session that has been blacklisted.
         * @param expiryTime the time (in milliseconds from UTC epoch) at which the item will be expunged from the
         *                   blacklist.
         */
        void onBlacklisted(String id, long expiryTime);
    }
}
