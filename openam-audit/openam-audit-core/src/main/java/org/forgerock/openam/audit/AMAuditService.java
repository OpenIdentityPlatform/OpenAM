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
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.configuration.AMAuditServiceConfiguration;

/**
 * Extended interface of the commons {@link AuditService} to allow for OpenAM specific configuration.
 *
 * @since 13.0.0
 */
public interface AMAuditService extends AuditService {

    /**
     * Set the new delegate to set on the {@link org.forgerock.audit.AuditServiceProxy} and the configuration for the
     * OpenAM specific settings.
     *
     * @param delegate The audit service delegate.
     * @param configuration OpenAM specific configuration.
     * @throws ServiceUnavailableException If the delegate is unavailable.
     * @see org.forgerock.audit.AuditServiceProxy#setDelegate(AuditService)
     */
    void setDelegate(AuditService delegate, AMAuditServiceConfiguration configuration)
            throws ServiceUnavailableException;

    /**
     * Determines if the audit service is auditing the specified {@literal topic} and {@literal eventName}.  If you
     * do not know the eventName (i.e. any event names is valid) you may pass in {@literal null}.
     *
     * @param topic The auditing topic.
     * @param eventName The auditing event name, may be null for all event names
     * @return {@code true} if Auditing is switched on and if the topic and eventName should be audited.
     */
    boolean isAuditEnabled(String topic, EventName eventName);

}
