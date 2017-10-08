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

package org.forgerock.openam.blacklist;

/**
 * Records a blacklist of entries that have been destroyed/logged out to ensure that they cannot be reused.
 *
 * @param <T> The blacklist type.
 * @since 13.0.0
 */
public interface Blacklist<T extends Blacklistable> {
    /**
     * Blacklists the given entry until its expiry time.
     *
     * @param entry The blacklist entry.
     * @throws BlacklistException if the entry cannot be blacklisted for any reason.
     */
    void blacklist(T entry) throws BlacklistException;

    /**
     * Determines whether the entry has previously been blacklisted. <strong>Note:</strong> entries are only
     * blacklisted until they expire, so a {@code false} result does not mean the entry is valid. Further checks
     * should be made to establish entry validity.
     *
     * @param entry The entry to check for blacklisting.
     * @return {@code true} if the entry is currently blacklisted, or {@code false} if the entry is not
     * blacklisted or has expired (and therefore been removed from the blacklist).
     * @throws BlacklistException if an error occurs when checking the blacklist.
     */
    boolean isBlacklisted(T entry) throws BlacklistException;

    /**
     * Subscribe for notifications when entries are blacklisted. Depending on the implementation, this may include
     * only entries blacklisted in the local machine, or also notifications for entries blacklisted on other nodes
     * in the cluster.
     *
     * @param listener the event listener to call when entries are blacklisted.
     */
    void subscribe(Listener listener);

    interface Listener {
        /**
         * Indicates that the given entry has been blacklisted.
         *
         * @param id the stable id of the entry that has been blacklisted.
         * @param expiryTime the time (in milliseconds from UTC epoch) at which the item will be expunged from the
         *                   blacklist.
         */
        void onBlacklisted(String id, long expiryTime);
    }
}
