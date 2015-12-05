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

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;

/**
 * A factory for creating an audit event handler. Implementations of this interface are required to create an instance
 * of {@link AuditEventHandler}, based on the configuration attributes supplied. Instances of
 * {@link AuditEventHandlerFactory} are injected with Guice and implementation class names can be configured in the
 * Audit Logging Configuration Service.
 *
 * @since 13.0.0
 * @see AuditEventHandler
 */
public interface AuditEventHandlerFactory {

    /**
     * Create an instance of {@link AuditEventHandler}. This method will be called every time configuration for audit
     * logging in OpenAM has changed. The returned handler will be added to the appropriate
     * {@link AuditService}, which will in turn call {@link AuditEventHandler#startup()}. If this
     * method returns {@code null} then nothing will be added to the {@link AuditService}.
     *
     * @param configuration
     *              The configuration properties to use when creating the handler.
     *
     * @return An instance of {@link AuditEventHandler} or null if this handler is disabled.
     *
     * @throws AuditException If an error occurred during creation of the handler.
     */
    AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException;
}
