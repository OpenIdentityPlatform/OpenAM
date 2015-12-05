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

package org.forgerock.openam.selfservice;

import org.forgerock.json.resource.RequestHandler;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;

/**
 * Creates new self service instances.
 *
 * @since 13.0.0
 */
public interface SelfServiceFactory {

    /**
     * Creates a new service instance based on the passed service config.
     *
     * @param realm
     *         the realm
     * @param serviceConfig
     *         service config
     *
     * @return new self service instance
     */
    RequestHandler getService(String realm, ProcessInstanceConfig serviceConfig);

}
