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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.audit.AuditConstants.*;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceProxy;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * Extension of the commons {@link AuditServiceProxy} that allows for OpenAM specific configuration to be exposed
 * in a thread-safe way.
 *
 * @since 13.0.0
 */
public class DefaultAuditServiceProxy extends AuditServiceProxy implements AMAuditService {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    private volatile AMAuditServiceConfiguration auditServiceConfiguration;

    /**
     * Create a new instance of the {@code DefaultAuditServiceProxy}. Note that the given delegate should be started
     * manually before or after it's passed to the proxy.
     *
     * @param delegate The audit service delegate.
     * @param auditServiceConfiguration OpenAM specific configuration.
     * @throws NullPointerException If any of the given parameters are null.
     */
    public DefaultAuditServiceProxy(AuditService delegate, AMAuditServiceConfiguration auditServiceConfiguration) {
        super(delegate);
        Reject.ifNull(auditServiceConfiguration);
        this.auditServiceConfiguration = auditServiceConfiguration;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        if (!isAuditEnabled(request)) {
            return newResourceResponse(null, null, json(object())).asPromise();
        }
        return super.handleCreate(context, request);
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
            try {
                String eventNameString = null;
                if (eventName != null) {
                    eventNameString = eventName.toString();
                }
                return auditServiceConfiguration.isAuditEnabled()
                        && isAuditing(topic)
                        && !auditServiceConfiguration.isBlacklisted(eventNameString);
            } catch (ServiceUnavailableException e) {
                DEBUG.warning("Default Audit Service is unavailable.", e);
            }
        } finally {
            releaseReadLock();
        }
        return false;
    }

    private boolean isAuditEnabled(CreateRequest createRequest) {
        JsonValue auditEventValue = createRequest.getContent();
        JsonValue eventNameJson = auditEventValue.get("eventName");

        if (eventNameJson == null) {
            return true;
        }
        return !auditServiceConfiguration.isBlacklisted(eventNameJson.asString());
    }
}
