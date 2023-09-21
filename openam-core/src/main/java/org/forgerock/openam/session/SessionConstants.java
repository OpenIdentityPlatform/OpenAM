/*
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
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 * Portions copyright 2023 3A Systems LLC
*/
package org.forgerock.openam.session;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

import java.util.concurrent.TimeUnit;

public class SessionConstants {

    public static final String SESSION_DEBUG = "amSession";

    /**
     * Session Tracking Cookie Name
     */
    public static final String HTTP_SESSION_TRACKING_COOKIE_NAME =
            SystemProperties.get(Constants.AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME, "JSESSIONID");

    /**
     * Session Handle Property reference
     */
    public static final String SESSION_HANDLE_PROP = "SessionHandle";

    /**
     * Session Master Stats table reference (for Guice injection)
     */
    public static final String STATS_MASTER_TABLE = "amMasterSessionTableStats";

    /**
     * Token Restriction Property reference
     */
    public static final String TOKEN_RESTRICTION_PROP = "TokenRestriction";

    /**
     * Session service reference
     */
    public static final String SESSION_SERVICE = "session";

    /**
     * Indicator used to reset the loadbalancer cookie for a client.
     */
    public static final boolean RESET_LB_COOKIE_NAME =
            SystemProperties.getAsBoolean("com.sun.identity.session.resetLBCookie", false);

    public static final String ENABLE_POLLING_PROPERTY =
            "com.iplanet.am.session.client.polling.enable";

    /**
     * The name of the JSON field which describes the session's username.
     */
    public static final String JSON_SESSION_USERNAME = "username";

    /**
     * The name of the JSON field which describes the session's universal ID.
     */
    public static final String JSON_SESSION_UNIVERSAL_ID = "universalId";

    /**
     * The name of the JSON field which describes the session's realm.
     */
    public static final String JSON_SESSION_REALM = "realm";

    /**
     * The name of the JSON field which describes the session's handle.
     */
    public static final String JSON_SESSION_HANDLE = "sessionHandle";

    /**
     * The name of the JSON Field which describes the session's latest access time.
     */
    public static final String JSON_SESSION_LATEST_ACCESS_TIME = "latestAccessTime";

    /**
     * The name of the JSON field which describes the session's max idle expiration time.
     */
    public static final String JSON_SESSION_MAX_IDLE_EXPIRATION_TIME = "maxIdleExpirationTime";

    /**
     * The name of the JSON field which describes the session's max expiration time.
     */
    public static final String JSON_SESSION_MAX_SESSION_EXPIRATION_TIME = "maxSessionExpirationTime";

    /**
     * Defaults for the polling threadpool.
     */
    public static final int DEFAULT_POOL_SIZE = 5;

    public static final int DEFAULT_THRESHOLD = 10000;

    /**
     * Expiry time which is long enough to make sessions functionally non expiring.
     */
    public static final long NON_EXPIRING_SESSION_LENGTH_MINUTES = 42 * TimeUnit.DAYS.toMinutes(365);
}
