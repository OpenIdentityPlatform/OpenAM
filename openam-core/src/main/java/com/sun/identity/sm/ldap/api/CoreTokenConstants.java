/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package com.sun.identity.sm.ldap.api;

import org.forgerock.opendj.ldap.DN;

/**
 * Responsible for collecting together all constants used in the Core Token Service.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenConstants {

    /**
     * Debugging header, for all debug messages.
     */
    public static final String DEBUG_HEADER = "CTS: ";

    /**
     * Configuration properties for Cleanup and Health Check periods.
     */
    public static final String CLEANUP_PERIOD = "com.sun.identity.session.repository.cleanupRunPeriod";
    public static final String HEALTH_CHECK_PERIOD = "com.sun.identity.session.repository.healthCheckRunPeriod";
    /**
     * Globals public Constants, so not to pollute entire product.
     */
    public static final String SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX =
            "iplanet-am-session-sfo-store-root-suffix";
    public static final String SYS_PROPERTY_SESSION_HA_REPOSITORY_TYPE =
            "iplanet-am-session-sfo-store-type";
    public static final String SYS_PROPERTY_TOKEN_ROOT_SUFFIX =
            "iplanet-am-config-token-root-suffix";
    public static final String SYS_PROPERTY_TOKEN_SAML2_REPOSITORY_ROOT_SUFFIX =
            "iplanet-am-token-saml2-root-suffix";
    public static final String SYS_PROPERTY_TOKEN_OAUTH2_REPOSITORY_ROOT_SUFFIX = "iplanet-am-token-oauth2-root-suffix";
    public static final String SYS_PROPERTY_EXPIRED_SEARCH_LIMIT =
            "forgerock-openam-session-expired-search-limit";
    public static final String DEBUG_NAME = "amSessionRepository";
    public static final String CTS_REPOSITORY_CLASS_PROPERTY =
            "com.sun.am.session.SessionRepositoryImpl";
    public static final String IS_SFO_ENABLED =
            "iplanet-am-session-sfo-enabled";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String FR_CORE_TOKEN = "frCoreToken";


    /**
     * LDAP Storage location for all Tokens.
     */
    private final DN tokenDN;

    public CoreTokenConstants(String rootSuffix) {
        tokenDN = DN.valueOf(rootSuffix)
                .child("ou=tokens")
                .child("ou=openam-session")
                .child("ou=famrecords");
    }

    public DN getTokenDN() {
        return tokenDN;
    }
}
