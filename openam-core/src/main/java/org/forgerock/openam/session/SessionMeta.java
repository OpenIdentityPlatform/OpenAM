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
 */

package org.forgerock.openam.session;

import java.util.concurrent.TimeUnit;

import com.iplanet.am.util.SystemProperties;

public class SessionMeta {

    private static final String PURGE_DELAY_PROPERTY = "com.iplanet.am.session.purgedelay";
    private static final String REFRESH_TIME_PROPERTY = "com.iplanet.am.client.appssotoken.refreshtime";

    private static long purgeDelayInMinutes;
    private static long appSSOTokenRefreshTime;

    /**
     * The maximum extra time for which timed out sessions should be retained.
     * <p>
     * Keeping these timed out sessions (in an invalid state) allows user agents attempting
     * to make use of the session to be informed that their session has timed out.
     * <p>
     * Time value is returned in the requested unit (accurate to minute).
     *
     * @param timeUnit the time unit to return the result in.
     * @return purge delay in the requested units.
     */
    public static long getPurgeDelay(final TimeUnit timeUnit) {
        return timeUnit.convert(purgeDelayInMinutes, TimeUnit.MINUTES);
    }

    /**
     * How long before an appSSOToken must refresh (in minutes).
     */
    public static long getAppSSOTokenRefreshTime() {
        return appSSOTokenRefreshTime;
    }

    static {
        purgeDelayInMinutes = SystemProperties.getAsLong(PURGE_DELAY_PROPERTY, 120);
        appSSOTokenRefreshTime = SystemProperties.getAsLong(REFRESH_TIME_PROPERTY, 3);
    }

}
