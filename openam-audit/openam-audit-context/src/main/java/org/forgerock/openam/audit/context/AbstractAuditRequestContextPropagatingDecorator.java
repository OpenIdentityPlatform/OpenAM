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

package org.forgerock.openam.audit.context;

/**
 * Base class for Decorators that propagate thread local {@link AuditRequestContext} to worker thread.
 *
 * @since 13.0.0
 */
public abstract class AbstractAuditRequestContextPropagatingDecorator {

    private final AuditRequestContext publisherContext;
    private AuditRequestContext consumerContext = null;

    /**
     * Constructor that will set the value of the publisher context.
     */
    public AbstractAuditRequestContextPropagatingDecorator() {
        publisherContext = AuditRequestContext.get().copy();
    }

    /**
     * Sets the current thread's {@link AuditRequestContext} to that of the publisher thread.
     *
     * This method should be called from the consumer thread.
     */
    protected void setContext() {
        consumerContext = AuditRequestContext.get();
        AuditRequestContext.set(publisherContext);
    }

    /**
     * Reverts the consumer threads' {@link AuditRequestContext} to the value
     * it had when {@link #setContext()} was called.
     *
     * This method should be called from the consumer thread after {@link #setContext()} has been called.
     */
    protected void revertContext() {
        if (consumerContext != null) {
            AuditRequestContext.set(consumerContext);
        } else {
            AuditRequestContext.clear();
        }
    }

}
