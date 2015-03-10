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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.entitlement.service;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;

/**
 * Application service to handle all things relating to applications.
 *
 * @see ApplicationServiceFactory
 * @since 13.0.0
 */
public interface ApplicationService {

    /**
     * Retrieves an application instance for the passed name.
     *
     * @param applicationName
     *         the application name
     *
     * @return an application instance, null if the application doesn't exist
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    Application getApplication(String applicationName) throws EntitlementException;

}
