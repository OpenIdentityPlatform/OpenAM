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
 * An object which can be placed in a {@code Blacklist}.
 */
public interface Blacklistable {

    /**
     * Returns a stable ID that can be used as a unique identifier when storing this entry.
     * <p>This will be used as a stable reference to this ID which all callers are expected to know
     * and use.</p>
     *
     * @return A unique stable storage id.
     */
    String getStableStorageID();

    /**
     * The expiry time of the object in the blacklist. The blacklist will only store objects until
     * their expiry time elapses. After which the blacklist will at some point purge the entry. It
     * is recommended to set this value to the expiry time of the object, or a timestamp after
     * which the object no longer matters. It is not recommend to set this to a large indefinite
     * point in the future as this will remain stored in the blacklist for this period.
     * Note: time stamp is in ms from epoch UTC.
     *
     * @return The time at which the entry expires (if it has not already) plus a purge delay.
     * @throws BlacklistException If the entry has already expired or an error occurs.
     */
    long getBlacklistExpiryTime() throws BlacklistException;
}
