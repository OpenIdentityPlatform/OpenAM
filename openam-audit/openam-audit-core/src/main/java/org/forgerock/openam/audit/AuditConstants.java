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
package org.forgerock.openam.audit;

import static org.forgerock.audit.events.AuditEventBuilder.*;

/**
 * Collection of constants related to auditing.
 *
 * @since 13.0.0
 */
public final class AuditConstants {

    /**
     * Predefined names for audit events.
     */
    public enum EventName {
        AM_ACCESS_ATTEMPT("AM-ACCESS-ATTEMPT"),
        AM_ACCESS_OUTCOME("AM-ACCESS-OUTCOME");

        private final String name;

        EventName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Predefined components for audit events.
     */
    public enum Component {
        PLL("PLL"),
        OAUTH2("OAuth2"),
        CTS("CTS"),
        OPENID_CONNECT("OpenID Connect"),
        UMA("UMA"),
        POLICY_AGENT("Policy Agent"),
        AUTHENTICATION("Authentication"),
        CREST("CREST"),
        DASHBOARD("Dashboard"),
        SERVER_INFO("Server Info"),
        USERS("Users"),
        GROUPS("Groups"),
        OATH("Oath"),
        DEVICES("Devices"),
        POLICY("Policy"),
        REALMS("Realms"),
        SESSION("Session"),
        SCRIPT("Script"),
        BATCH("Batch"),
        CONFIG("Config"),
        RECORD("Record"),
        STS("STS");

        private final String name;

        Component(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * The topic to which events built using {@link AMAccessAuditEventBuilder} should be routed.
     */
    public static final String ACCESS_TOPIC = "access";

    /**
     * SMS service name for the audit service.
     */
    public static final String SERVICE_NAME = "AuditService";

    /**
     * Name of the event handlers registered with the audit service.
     */
    public static final String CSV = "csv";

    /**
     * Names of fields on an audit events.
     */
    public static final String USER_ID = AUTHENTICATION + "." + ID;
    public static final String CONTEXT_ID = "contextId";


    private AuditConstants() {
        // Prevent instantiation
    }

}
