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
package org.forgerock.openam.entitlement.monitoring;

import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.PrivilegeManager;

/**
 * Non-static wrapper for the {@link EntitlementConfiguration}.
 *
 * Contains only the methods necessary for the entitlement monitoring system to operate.
 */
public class EntitlementConfigurationWrapper {

    private final EntitlementConfiguration entitlementConfiguration;

    public EntitlementConfigurationWrapper() {
        entitlementConfiguration = EntitlementConfiguration.getInstance(PrivilegeManager.superAdminSubject, "/");
    }

    /**
     * Whether or not the monitoring system is running.
     *
     * @return true if the system is available and running
     */
    public boolean isMonitoringRunning() {
        return entitlementConfiguration.isMonitoringRunning();
    }

    /**
     * The number of samples to retain for average/slowest calculation via the monitoring service.
     *
     * @return user-configured policy window size
     */
    public int getPolicyWindowSize() {
        return entitlementConfiguration.getPolicyWindowSize();
    }

}
