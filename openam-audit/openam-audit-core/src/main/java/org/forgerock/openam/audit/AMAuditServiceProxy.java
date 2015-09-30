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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceProxy;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;

/**
 * Extension of the commons {@link AuditServiceProxy} that allows for OpenAM specific configuration to be exposed
 * in a thread-safe way.
 *
 * @since 13.0.0
 */
public class AMAuditServiceProxy extends AuditServiceProxy implements AMAuditService {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    private volatile AMAuditServiceConfiguration auditServiceConfiguration;

    /**
     * Create a new instance of the {@code AMAuditServiceProxy}. Note that the given delegate should be started
     * manually before passed to the proxy.
     *
     * @param delegate The audit service delegate.
     * @param auditServiceConfiguration OpenAM specific configuration.
     */
    public AMAuditServiceProxy(AuditService delegate, AMAuditServiceConfiguration auditServiceConfiguration) {
        super(delegate);
        this.auditServiceConfiguration = auditServiceConfiguration;
    }

    @Override
    public void setDelegate(AuditService delegate, AMAuditServiceConfiguration configuration) throws
            ServiceUnavailableException {

        obtainWriteLock();
        try {
            setDelegate(delegate);
            auditServiceConfiguration = configuration;
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public boolean isAuditEnabled(String topic) {
        obtainReadLock();
        try {
            try {
                return auditServiceConfiguration.isAuditEnabled() && isAuditing(topic);
            } catch (ServiceUnavailableException e) {
                DEBUG.error("Audit service is unavailable", e);
            }
        } finally {
            releaseReadLock();
        }
        return false;
    }

    @Override
    public boolean isAuditFailureSuppressed() {
        obtainReadLock();
        try {
            return auditServiceConfiguration.isAuditFailureSuppressed();
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public boolean isResolveHostNameEnabled() {
        obtainReadLock();
        try {
            return auditServiceConfiguration.isResolveHostNameEnabled();
        } finally {
            releaseReadLock();
        }
    }
}
