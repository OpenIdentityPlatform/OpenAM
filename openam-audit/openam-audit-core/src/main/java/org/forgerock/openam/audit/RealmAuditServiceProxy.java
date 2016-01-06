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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.audit;

import static org.forgerock.openam.audit.AuditConstants.EventName;

import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceProxy;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.util.Reject;

/**
 * Extension of the commons {@link AuditServiceProxy} that allows for OpenAM specific configuration to be exposed
 * in a thread-safe way.
 *
 * @since 13.0.0
 */
public class RealmAuditServiceProxy extends AuditServiceProxy implements AMAuditService {

    private final AMAuditService defaultDelegate;

    private volatile AMAuditServiceConfiguration auditServiceConfiguration;

    /**
     * Create a new instance of the {@code AMAuditServiceProxy}. Note that the given delegate should be started
     * manually before or after it's passed to the proxy.
     *
     * @param delegate The audit service delegate.
     * @param defaultDelegate The default audit service delegate.
     * @param auditServiceConfiguration OpenAM specific configuration.
     * @throws NullPointerException If any of the given parameters are null.
     */
    public RealmAuditServiceProxy(AuditService delegate, AMAuditService defaultDelegate,
            AMAuditServiceConfiguration auditServiceConfiguration) {

        super(delegate);
        Reject.ifNull(defaultDelegate);
        Reject.ifNull(auditServiceConfiguration);
        this.defaultDelegate = defaultDelegate;
        this.auditServiceConfiguration = auditServiceConfiguration;
    }

    @Override
    public void setDelegate(AuditService delegate, AMAuditServiceConfiguration configuration) throws
            ServiceUnavailableException {

        Reject.ifNull(configuration);
        obtainWriteLock();
        try {
            setDelegate(delegate);
            auditServiceConfiguration = configuration;
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public boolean isAuditEnabled(String topic, EventName eventName) {
        obtainReadLock();
        try {
            String eventNameString = null;
            if (eventName != null) {
                eventNameString = eventName.toString();
            }
            return auditServiceConfiguration.isAuditEnabled()
                    && isAuditing(topic)
                    && !auditServiceConfiguration.isBlacklisted(eventNameString);
        } catch (ServiceUnavailableException e) {
            return defaultDelegate.isAuditEnabled(topic, eventName);
        } finally {
            releaseReadLock();
        }
    }

}
