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
package com.iplanet.dpro.session.monitoring;

import com.sun.identity.monitoring.MonitoringUtil;

/**
 * Non-static wrapper for calls to Session-Monitoring related information. Specifically,
 * this class wraps calls to the {@link MonitoringUtil} class.
 */
public class SessionMonitoringService {

    /**
     * Returns the maximum size of the policy window, as read from {@link MonitoringUtil}.
     *
     * @return the max size of the policy window
     */
    public int getSessionWindowSize() {
        return MonitoringUtil.getSessionWindowSize();
    }

}
