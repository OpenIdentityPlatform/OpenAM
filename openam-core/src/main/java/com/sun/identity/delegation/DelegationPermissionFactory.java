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

package com.sun.identity.delegation;

import java.util.Map;
import java.util.Set;

/**
 * Simple factory for creating {@link com.sun.identity.delegation.DelegationPermission} instances.
 *
 * @since 12.0.0
 */
public class DelegationPermissionFactory {

    /**
     * Creates a new {@link com.sun.identity.delegation.DelegationPermission} instance.
     *
     * @param orgName
     *         the org name
     * @param serviceName
     *         the service name
     * @param version
     *         the version
     * @param configType
     *         the configuration type
     * @param subConfigName
     *         the sub configuration name
     * @param actions
     *         the set of actions
     * @param extensions
     *         the defined extension points
     *
     * @return a new instance
     *
     * @throws DelegationException
     *         should some error occur during instantiation
     */
    public DelegationPermission newInstance(String orgName, String serviceName,
                                            String version, String configType,
                                            String subConfigName, Set<String> actions,
                                            Map<String, String> extensions) throws DelegationException {

        return new DelegationPermission(orgName, serviceName, version, configType, subConfigName, actions, extensions);
    }

}
