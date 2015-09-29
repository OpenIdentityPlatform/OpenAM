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

import java.util.Collections;
import java.util.List;

/**
 * Audit service configuration specific to OpenAM. An instance of the current state can be retrieved from
 * {@link org.forgerock.openam.audit.configuration.AuditServiceConfigurator}.
 *
 * @since 13.0.0
 */
public class AMAuditServiceConfiguration extends AuditServiceConfiguration {

    private final boolean auditEnabled;
    private final boolean auditFailureSuppressed;
    private final boolean resolveHostNameEnabled;

    /**
     * Create an instance of {@code AMAuditServiceConfiguration} with the specified values.
     *
     * @param auditEnabled
     * @param auditFailureSuppressed
     * @param resolveHostNameEnabled
     */
    public AMAuditServiceConfiguration(boolean auditEnabled, boolean auditFailureSuppressed,
                                        boolean resolveHostNameEnabled) {

        this.auditEnabled = auditEnabled;
        this.auditFailureSuppressed = auditFailureSuppressed;
        this.resolveHostNameEnabled = resolveHostNameEnabled;
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
     * @return true if audit failure should be suppressed.
     */
    public boolean isAuditFailureSuppressed() {
        return auditFailureSuppressed;
    }

    /**
     * Is access event reverse DNS lookup enabled.
     * @return true if enabled
     */
    public boolean isResolveHostNameEnabled() {
        return resolveHostNameEnabled;
    }
}
