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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.STSInitializationException;

import java.security.AccessController;

/**
 * @see org.forgerock.openam.sts.rest.ServiceListenerRegistration
 */
public class ServiceListenerRegistrationImpl implements ServiceListenerRegistration {

    public void registerServiceListener(String serviceName, String serviceVersion, ServiceListener listener)
            throws STSInitializationException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token,
                serviceName, serviceVersion);
            if (serviceConfigManager.addListener(listener) == null) {
                String message = "In ServiceListenerRegistrationImpl#registerServiceListener, could not add ServiceListener " +
                        "for service " + serviceName + " and version " + serviceVersion;
                throw new STSInitializationException(ResourceException.INTERNAL_ERROR, message);
            }
        } catch (Exception e) {
            final String message = "In ServiceListenerRegistrationImpl#registerServiceListener, could not add ServiceListener " +
                    "for service " + serviceName + " and version " + serviceVersion;
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, message, e);
        }
    }
}
