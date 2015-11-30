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

import org.forgerock.audit.events.AuditEventBuilder;

import org.forgerock.openam.audit.context.AuditRequestContext;

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
        AM_ACCESS_OUTCOME("AM-ACCESS-OUTCOME"),
        /** Session created audit event name. */
        AM_SESSION_CREATED("AM-SESSION-CREATED"),
        /** Session idle timed out audit event name. */
        AM_SESSION_IDLE_TIMED_OUT("AM-SESSION-IDLE_TIMED_OUT"),
        /** Session max timed out audit event name. */
        AM_SESSION_MAX_TIMED_OUT("AM-SESSION-MAX_TIMED_OUT"),
        /** Session logged out audit event name. */
        AM_SESSION_LOGGED_OUT("AM-SESSION-LOGGED_OUT"),
        /** Session reactivated audit event name. */
        AM_SESSION_REACTIVATED("AM-SESSION-REACTIVATED"),
        /** Session destroyed audit event name. */
        AM_SESSION_DESTROYED("AM-SESSION-DESTROYED"),
        /** Session property changed audit event name. */
        AM_SESSION_PROPERTY_CHANGED("AM-SESSION-PROPERTY_CHANGED"),
        /** Event for auth process module completion. */
        AM_LOGIN_MODULE_COMPLETED("AM-LOGIN-MODULE-COMPLETED"),
        /** Event for auth login process completion. */
        AM_LOGIN_COMPLETED("AM-LOGIN-COMPLETED"),
        /** Event for auth process logout. */
        AM_LOGOUT("AM-LOGOUT"),
        /** Event for a change in configuration. */
        AM_CONFIG_CHANGE("AM-CONFIG-CHANGE");

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
        /** OAuth2, OpenID Connect and UMA audit event components. */
        OAUTH("OAuth"),
        /** CTS audit event component. */
        CTS("CTS"),
        /** Policy Agent audit event component. */
        POLICY_AGENT("Policy Agent"),
        /** Authentication audit event component. */
        AUTHENTICATION("Authentication"),
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
        /** Configuration audit event component. */
        CONFIG("Config"),
        /** STS audit event component. */
        STS("STS"),
        /** Record audit event component. */
        RECORD("Record"),
        /** Audit audit event component. */
        AUDIT("Audit"),
        /** RADIUS server component. */
        RADIUS("Radius"),
        /** SSOADM CLI component. */
        SSOADM("ssoadm"),
        /** Jato audit event component. */
        JATO("jato"),
        /** SAML2 audit event component. */
        SAML2("SAML2");

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
     * Predefined operations for configuration events.
     */
    public enum ConfigOperation {
        /** Creation operation.*/
        CREATE("CREATE"),
        /** Modification operation. */
        UPDATE("UPDATE"),
        /** Deletion operation. */
        DELETE("DELETE");

        private final String name;

        ConfigOperation(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Predefined tracking ID types.
     */
    public enum TrackingIdKey {
        /** Tracking ID for authenticated Session. */
        SESSION,
        /** Tracking ID for OAuth2 grant code, for use with OAuth2, OIDC and UMA. */
        OAUTH2_GRANT,
        /** Tracking ID for OAuth2 refresh code, for use with OAuth2, OIDC and UMA. */
        OAUTH2_REFRESH,
        /** Tracking ID for OAuth2 access code, for use with OAuth2, OIDC and UMA. */
        OAUTH2_ACCESS,
        /** Tracking ID for Session used during authentication. */
        AUTH,
        /** Radius Access Request. */
        RADIUS_ACCESS_REQUEST
    }

    /**
     * Predefined keys for the entries in the authentication audit "entries/info" field.
     */
    public enum EntriesInfoFieldKey {
        /** Key for the IP address. */
        IP_ADDRESS("ipAddress"),
        /** Key for the auth level. */
        AUTH_LEVEL("authLevel"),
        /** Key for the auth index. */
        AUTH_INDEX("authIndex"),
        /** Key for the login module control flag. */
        AUTH_CONTROL_FLAG("authControlFlag"),
        /** Key for the module class. */
        MODULE_CLASS("moduleClass"),
        /** Key for the reason of a failure event. */
        FAILURE_REASON("failureReason"),
        /** Key for the device ID. */
        DEVICE_ID("deviceId");

        private final String name;

        EntriesInfoFieldKey(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Predefined values for the entries in the authentication audit "entries/info/failureReason" field.
     */
    public enum AuthenticationFailureReason {
        /** Incorrect/invalid credentials presented. */
        LOGIN_FAILED,
        /** Invalid credentials entered. */
        INVALID_PASSWORD,
        /** Named Configuration (Auth Chain) does not exist. */
        NO_CONFIG,
        /** No user profile found for this user. */
        NO_USER_PROFILE,
        /** This user is not active. */
        USER_INACTIVE,
        /** Max number of failure attempts exceeded. User is Locked out. */
        LOCKED_OUT,
        /** User account has expired. */
        ACCOUNT_EXPIRED,
        /** Login timed out. */
        LOGIN_TIMEOUT,
        /** Authentication module is denied. */
        MODULE_DENIED,
        /** Limit for maximum number of allowed session has been reached. */
        MAX_SESSION_REACHED,
        /** Org/Realm does not exists. */
        INVALID_REALM,
        /** Org/Realm is not active. */
        REALM_INACTIVE,
        /** Role based auth. User does not belong to this role. */
        USER_NOT_FOUND,
        /** Authentication type is denied. */
        AUTH_TYPE_DENIED,
        /** Cannot create a session. */
        SESSION_CREATE_ERROR,
        /** Level based Auth. Invalid Auth Level. */
        INVALID_LEVEL
    }

    /**
     * The topic to which events built using {@link AMAccessAuditEventBuilder} should be routed.
     */
    public static final String ACCESS_TOPIC = "access";

    /**
     * The topic to which events built using {@link AMActivityAuditEventBuilder} should be routed.
     */
    public static final String ACTIVITY_TOPIC = "activity";

    /**
     * The topic to which events built using {@link AMConfigAuditEventBuilder} should be routed.
     */
    public static final String CONFIG_TOPIC = "config";

    /**
     * The topic to which events built using {@link AMAuthenticationAuditEventBuilder} should be routed.
     */
    public static final String AUTHENTICATION_TOPIC = "authentication";

    /**
     * SMS service name for the audit service.
     */
    public static final String SERVICE_NAME = "AuditService";

    /**
     * Name of the {@link com.sun.identity.shared.debug.Debug} instance.
     */
    public static final String DEBUG_NAME = "amAudit";

    /**
     * Names of fields on an audit events.
     */
    public static final String USER_ID = AuditEventBuilder.USER_ID;

    /**
     * Names of the realm field on an audit event.
     */
    public static final String EVENT_REALM = "realm";

    /**
     * To access the default audit service via {@link AuditEventFactory#accessEvent(String)} and
     * {@link AuditEventPublisher#isAuditing(String, String, EventName)} you can provide null or an empty string.
     * We deliberately do not provide a convenience method with no realm to force implementers to consider providing
     * the realm.
     */
    public static final String NO_REALM = null;

    /**
     * The OAuth2 audit context providers, responsible for finding details which can be audit logged from various
     * tokens which may be attached to requests and/or responses.
     */
    public static final String OAUTH2_AUDIT_CONTEXT_PROVIDERS = "oauth2AuditContextProviders";

    /**
     * The field name to use when adding a "reason" string to the /response/detail object of an access event.
     */
    public static final String ACCESS_RESPONSE_DETAIL_REASON = "reason";

    /**
     *  Key for the login module control flag that can be added {@link AuditRequestContext}.
     */
    public static final String LOGIN_MODULE_CONTROL_FLAG = "loginModuleControlFlag";

    private AuditConstants() {
        // Prevent instantiation
    }
}
