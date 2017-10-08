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
package org.forgerock.openam.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;

/**
 * Simple factory for throwing out ServiceSchemaManagers, used for decoupling test code.
 */
public class ServiceSchemaManagerFactory {

    private final SSOToken adminToken;

    /**
     * Constrcuts a new ServiceSchemaManagerFactory, storing a reference to the admin token.
     */
    public ServiceSchemaManagerFactory() {
        this.adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());

    }

    /**
     * Builds a new ServiceSchemaManager using the provided service name and the held admin token.
     *
     * @param service Service instanceid to generate.
     * @return A ServiceSchemaManager appropriate to that service.
     * @throws SMSException if an error occurred while trying to perform the operation
     * @throws SSOException if the single sign on token is invalid or expired
     */
    public ServiceSchemaManager build(String service) throws SSOException, SMSException {
        return new ServiceSchemaManager(service, adminToken);
    }

}
