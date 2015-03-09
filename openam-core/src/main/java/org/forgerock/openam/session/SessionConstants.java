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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.session;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

public class SessionConstants {

    public static final String SESSION_DEBUG = "amSession";

    /**
     * Session Tracking Cookie Name
     */
    public static final String HTTP_SESSION_TRACKING_COOKIE_NAME =
            SystemProperties.get(Constants.AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME, "JSESSIONID");

    /**
     * Session States
     */
    public static final int INVALID = 0;

    public static final int VALID = 1;

    public static final int INACTIVE = 2;

    public static final int DESTROYED = 3;

    /**
     * Session Types
     */
    public static final int USER_SESSION = 0;

    public static final int APPLICATION_SESSION = 1;

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
     * Primary Servier URL reference (for Guice injection)
     */
    public static final String PRIMARY_SERVER_URL = "primaryServerURL";

    /**
     * Indicator used to reset the loadbalancer cookie for a client.
     */
    public static final boolean RESET_LB_COOKIE_NAME =
            SystemProperties.getAsBoolean("com.sun.identity.session.resetLBCookie", false);

    public static final String ENABLE_POLLING_PROPERTY =
            "com.iplanet.am.session.client.polling.enable";


    /**
     * Defaults for the polling threadpool.
     */
    public static final int DEFAULT_POOL_SIZE = 5;

    public static final int DEFAULT_THRESHOLD = 10000;
}
