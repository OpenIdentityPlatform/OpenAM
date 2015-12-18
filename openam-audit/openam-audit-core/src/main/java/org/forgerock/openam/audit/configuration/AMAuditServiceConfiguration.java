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
package org.forgerock.openam.audit.configuration;

import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.openam.audit.AMAuditService;

import java.util.HashSet;
import java.util.Set;

/**
 * Audit service configuration specific to OpenAM. An instance of the current state can be retrieved from
 * {@link AuditServiceConfigurationProvider}. The instance retrieved from {@link AuditServiceConfigurationProvider}
 * will represent the current settings in the SMS.
 * <p/>
 * Each of the Audit Services has its own configuration, which is stored with the Audit Service and can be retrieved
 * in a thread-safe way via its own accessor methods -
 * {@link AMAuditService#isAuditEnabled(String, org.forgerock.openam.audit.AuditConstants.EventName)} and
 * {@link AMAuditService#isAuditFailureSuppressed()}.
 *
 * @since 13.0.0
 */
public class AMAuditServiceConfiguration extends AuditServiceConfiguration {

    private final Set<String> blacklistedEventNames;
    private final boolean auditEnabled;
    private final boolean auditFailureSuppressed;

    /**
     * Create an instance of {@code AMAuditServiceConfiguration} with the specified values.
     *
     * @param auditEnabled Is audit logging enabled.
     * @param auditFailureSuppressed Is audit failure suppressed.
     */
    public AMAuditServiceConfiguration(boolean auditEnabled, boolean auditFailureSuppressed) {
        this.auditEnabled = auditEnabled;
        this.auditFailureSuppressed = auditFailureSuppressed;
        this.blacklistedEventNames = new HashSet<>();
    }

    /**
     * Create an instance of {@code AMAuditServiceConfiguration} with the specified values.
     *
     * @param auditEnabled Is audit logging enabled.
     * @param auditFailureSuppressed Is audit failure suppressed.
     * @param blacklistedEventNames A set of event names which we DO NOT want to audit.
     */
    public AMAuditServiceConfiguration(boolean auditEnabled, boolean auditFailureSuppressed,
            Set<String> blacklistedEventNames) {

        this.auditEnabled = auditEnabled;
        this.auditFailureSuppressed = auditFailureSuppressed;
        this.blacklistedEventNames = blacklistedEventNames;
    }

    /**
     * Is audit logging enabled.
     * @return true if audit logging is enabled.
     */
    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    /**
     * Stop failure to log an audit message form also failing the operation that is audited.
     * @return true if audit failure should be suppressed.
     */
    public boolean isAuditFailureSuppressed() {
        return auditFailureSuppressed;
    }

    /**
     * Is the specified event name blacklisted, i.e. one which we do NOT want to audit.  Passing in {@literal null}
     * turns the checking off, returning {@literal false}, such that no event name is blacklisted.
     *
     * @param eventName the specified event name as a string, or {@literal null}
     * @return true if blacklisted, i.e. we do NOT want to audit
     */
    public boolean isBlacklisted(String eventName) {
        if (eventName == null) {
            return false;
        }
        return blacklistedEventNames.contains(eventName.toString());
    }
}
