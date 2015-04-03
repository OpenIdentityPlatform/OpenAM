/**
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
 * Copyright 2013-2015 ForgeRock AS.
 */
package com.iplanet.dpro.session.service;

/**
 * Responsible for tracking shared constants for the Session Service.
 */
public class SessionConstants {
    public static final String SESSION_DEBUG = "amSession";
    public static final String PRIMARY_SERVER_URL = "primaryServerURL";
    public static final String STATS_MASTER_TABLE = "amMasterSessionTableStats";
    public static final String PURGE_DELAY_PROPERTY = "com.iplanet.am.session.purgedelay";

    /**
     * Session service attribute to enable/disable session blacklisting.
     */
    public static final String SESSION_BLACKLIST_ENABLED_ATTR = "openam-session-stateless-enable-session-blacklisting";

    /**
     * Session service attribute for the size of the cache to maintain for session blacklisting.
     */
    public static final String SESSION_BLACKLIST_CACHE_SIZE_ATTR = "openam-session-stateless-blacklist-cache-size";

    /**
     * Session service attribute for the interval (in seconds) at which to poll the CTS for session blacklist changes.
     */
    public static final String SESSION_BLACKLIST_POLL_INTERVAL_ATTR =
            "openam-session-stateless-blacklist-poll-interval";

    /**
     * Session service attribute for delay (in minutes) before purging elements from the session blacklist, after
     * expiry.
     */
    public static final String SESSION_BLACKLIST_PURGE_DELAY_ATTR = "openam-session-stateless-blacklist-purge-delay";


}
