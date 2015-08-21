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
        /** Access attempt audit event name. */
        AM_ACCESS_ATTEMPT("AM-ACCESS-ATTEMPT"),
        /** Access outcome audit event name. */
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
        /** PLL audit event component. */
        PLL("PLL"),
        /** OAuth2 audit event component. */
        OAUTH2("OAuth2"),
        /** CTS audit event component. */
        CTS("CTS"),
        /** OpenID Connect audit event component. */
        OPENID_CONNECT("OpenID Connect"),
        /** UMA audit event component. */
        UMA("UMA"),
        /** Policy Agent audit event component. */
        POLICY_AGENT("Policy Agent"),
        /** Authentication audit event component. */
        AUTHENTICATION("Authentication"),
        /** CREST audit event component. */
        CREST("CREST"),
        /** Dashboard audit event component. */
        DASHBOARD("Dashboard"),
        /** Server Info audit event component. */
        SERVER_INFO("Server Info"),
        /** Users audit event component. */
        USERS("Users"),
        /** Groups audit event component. */
        GROUPS("Groups"),
        /** Oath audit event component. */
        OATH("Oath"),
        /** Devices audit event component. */
        DEVICES("Devices"),
        /** Policy audit event component. */
        POLICY("Policy"),
        /** Realms audit event component. */
        REALMS("Realms"),
        /** Session audit event component. */
        SESSION("Session"),
        /** Script audit event component. */
        SCRIPT("Script"),
        /** Batch audit event component. */
        BATCH("Batch"),
        /** Configuration audit event component. */
        CONFIG("Config"),
        /** STS audit event component. */
        STS("STS"),
        /** Record audit event component. */
        RECORD("Record"),
        /** Audit audit event component. */
        AUDIT("Audit");

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
     * Name of the {@link com.sun.identity.shared.debug.Debug} instance.
     */
    public static final String DEBUG_NAME = "amAudit";

    /**
     * Names of fields on an audit events.
     */
    public static final String USER_ID = AUTHENTICATION + "." + ID;

    /**
     * The Context ID.
     */
    public static final String CONTEXT_ID = "contextId";

    private AuditConstants() {
        // Prevent instantiation
    }
}
