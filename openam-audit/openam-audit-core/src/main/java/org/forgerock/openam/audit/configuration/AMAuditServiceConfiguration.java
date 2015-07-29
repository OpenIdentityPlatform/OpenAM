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

/**
 * Audit service configuration specific to OpenAM. An instance of the current state can be retrieved from
 * {@link org.forgerock.openam.audit.configuration.AuditServiceConfigurator}. The instance will be updated with
 * any changes in configuration and should be consulted before for every log event where necessary.
 *
 * @since 13.0.0
 */
public class AMAuditServiceConfiguration extends AuditServiceConfiguration {

    private volatile boolean auditEnabled = false;
    private volatile boolean auditFailureSuppressed = true;
    private volatile boolean resolveHostNameEnabled = false;

    /**
     * Is audit logging is enabled.
     * @param auditEnabled true if audit logging is enabled.
     */
    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    /**
     * Is audit logging is enabled.
     * @return true if audit logging is enabled.
     */
    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    /**
     * Stop failure to log an audit message form also failing the operation that is audited.
     * @param auditFailureSuppressed true if audit failure should be suppressed.
     */
    public void setAuditFailureSuppressed(boolean auditFailureSuppressed) {
        this.auditFailureSuppressed = auditFailureSuppressed;
    }

    /**
     * Stop failure to log an audit message form also failing the operation that is audited.
     * @return true if audit failure should be suppressed.
     */
    public boolean isAuditFailureSuppressed() {
        return auditFailureSuppressed;
    }

    /**
     * Set access event reverse DNS lookup enabled.
     * @param enabled true to enable
     */
    public void setResolveHostNameEnabled(boolean enabled) {
        this.resolveHostNameEnabled = enabled;
    }

    /**
     * Is access event reverse DNS lookup enabled.
     * @return true if enabled
     */
    public boolean isResolveHostNameEnabled() {
        return resolveHostNameEnabled;
    }
}
