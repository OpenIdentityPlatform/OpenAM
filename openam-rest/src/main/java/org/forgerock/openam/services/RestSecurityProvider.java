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

package org.forgerock.openam.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

/**
 * Guice-injectable singleton to allow access to the current realm's RestSecurity
 * instance. Useful for caching.
 */
@Singleton
public class RestSecurityProvider {

    private final Map<String, RestSecurity> REALM_REST_SECURITY_MAP  = new ConcurrentHashMap<>();

    /**
    * Retrieve cached realm's RestSecurity instance
    **/
    public RestSecurity get(String realm) {
        RestSecurity restSecurity = REALM_REST_SECURITY_MAP.get(realm);
        if (restSecurity == null) {
            synchronized(REALM_REST_SECURITY_MAP) {
                    restSecurity = REALM_REST_SECURITY_MAP.get(realm);
                        if (restSecurity == null) {
                            restSecurity = new RestSecurity(realm);
                                REALM_REST_SECURITY_MAP.put(realm, restSecurity);
                            }
                }
            }
        return restSecurity;
    }

}