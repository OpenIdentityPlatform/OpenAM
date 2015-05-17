package org.forgerock.openam.shared.audit.context;

/**
 * Base class for Decorators that propagate thread local {@link AuditRequestContext} to worker thread.
 *
 * @since 13.0.0
 */
public abstract class AbstractAuditRequestContextPropagatingDecorator {

    private final AuditRequestContext publisherContext;
    private AuditRequestContext consumerContext = null;

    public AbstractAuditRequestContextPropagatingDecorator() {
        publisherContext = AuditRequestContext.get();
    }

    /**
     * Sets the current thread's {@link AuditRequestContext} to that of the publisher thread.
     *
     * This method should be called from the consumer thread.
     */
    protected void setContext() {
        if (publisherContext != null) {
            consumerContext = AuditRequestContext.get();
            AuditRequestContext.set(publisherContext);
        }
    }

    /**
     * Reverts the consumer threads' {@link AuditRequestContext} to the value it had when {@link #setContext()} was called.
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
