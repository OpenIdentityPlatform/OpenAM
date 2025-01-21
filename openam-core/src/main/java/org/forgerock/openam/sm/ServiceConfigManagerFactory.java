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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.sm;

import java.security.AccessController;
import java.security.PrivilegedAction;

import jakarta.inject.Inject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Simple factory for creating {@code ServiceConfigManager} instances.
 */
public class ServiceConfigManagerFactory {

    private final PrivilegedAction<SSOToken> adminTokenAction;

    /**
     * Constructs a new instance of {@code ServiceConfigManagerFactory}, storing a reference to the admin token.
     *
     * @param adminTokenAction the admin token action.
     */
    @Inject
    public ServiceConfigManagerFactory(PrivilegedAction<SSOToken> adminTokenAction) {
        this.adminTokenAction = adminTokenAction;
    }

    /**
     * Creates a new instance of {@code ServiceConfigManager}.
     *
     * @param serviceName    the name of the service.
     * @param serviceVersion the version of the service.
     * @return a new instance of {@code ServiceConfigManager}.
     * @throws SMSException if an error has occurred while performing the operation.
     * @throws SSOException if the user's single sign on token is invalid or expired.
     */
    public ServiceConfigManager create(String serviceName, String serviceVersion) throws SMSException, SSOException {
        final SSOToken token = AccessController.doPrivileged(adminTokenAction);
        return new ServiceConfigManager(token, serviceName, serviceVersion);
    }
}
