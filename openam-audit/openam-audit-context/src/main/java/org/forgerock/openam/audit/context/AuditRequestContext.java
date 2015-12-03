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

import org.forgerock.services.TransactionId;
import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for Commons Audit state.
 *
 * @since 13.0.0
 */
public class AuditRequestContext {

    private static final ThreadLocal<AuditRequestContext> INSTANCES = new ThreadLocal<AuditRequestContext>() {
        @Override
        protected AuditRequestContext initialValue() {
            return new AuditRequestContext(new TransactionId());
        }
    };

    private final TransactionId transactionId;
    private final Map<String, String> properties;

    /**
     * Construct a new <code>RequestContext</code>.
     *
     * @param transactionId Non-null, <code>TransactionId</code>.
     */
    public AuditRequestContext(TransactionId transactionId) {
        this(transactionId, null);
    }

    /**
     * Construct a new <code>RequestContext</code> with a copy of the given properties.
     *
     * @param transactionId Non-null, <code>TransactionId</code>.
     * @param properties Initial properties for this context.
     */
    public AuditRequestContext(TransactionId transactionId, Map<String, String> properties) {
        Reject.ifNull(transactionId, "TransactionId should not be null.");
        this.transactionId = transactionId;
        this.properties = properties == null ? new HashMap<String, String>() : new HashMap<>(properties);
    }

    /**
     * Create a new instance of <code>RequestContext</code>, which will have a reference to the original
     * <code>TransactionId</code> and a copy of the original properties.
     *
     * A copy can be used to hand over the context from one thread to another and therefore we make a copy
     * of the properties in order to keep the map thread-safe. The transactionId is thread-safe
     * and we want to see the sequence of sub-transactionIds shared across threads.
     *
     * @return a copy of this instance
     */
    public AuditRequestContext copy() {
        return new AuditRequestContext(transactionId, properties);
    }

    /**
     * Gets the transaction id.
     *
     * @return Non-null, <code>TransactionId</code>.
     */
    public TransactionId getTransactionId() {
        return transactionId;
    }

    /**
     * Add a property to the request context. Changing a property for the same transaction in separate threads will
     * only affect the property on the current thread. Properties are copied when the context is passed
     * on from one thread to another.
     *
     * @param key the name of the property
     * @param value the value of the property
     */
    public static void putProperty(String key, String value) {
        get().properties.put(key, value);
    }

    /**
     * Get the value for the specified property.
     *
     * @param key the name of the property
     * @return the value of the property or null if it has not been set
     */
    public static String getProperty(String key) {
        return get().properties.get(key);
    }

    /**
     * Remove the value for the specified property.
     *
     * @param key the name of the property
     * @return the value of the property or null if it has not been set
     */
    public static String removeProperty(String key) {
        return get().properties.remove(key);
    }

    /**
     * Gets the thread local {@code RequestContext}.
     *
     * @return Non-null, thread local <code>RequestContext</code>.
     */
    public static AuditRequestContext get() {
        return INSTANCES.get();
    }

    /**
     * Sets <code>RequestContext</code> of the current thread.
     *
     * @param auditRequestContext Non-null, <code>RequestContext</code>.
     */
    public static void set(AuditRequestContext auditRequestContext) {
        Reject.ifNull(auditRequestContext, "RequestContext should not be null.");
        INSTANCES.set(auditRequestContext);
    }

    /**
     * Discards the <code>RequestContext</code> of the current thread.
     */
    public static void clear() {
        INSTANCES.remove();
    }

    /**
     * Gets the transaction value of the current thread.
     *
     * @return Non-null, {@link TransactionId} value.
     */
    public static String getTransactionIdValue() {
        return get().getTransactionId().getValue();
    }

    /**
     * Creates a sub transaction id from the transaction id of the current
     * thread, for propagation to another process.
     *
     * @return Non-null, sub-{@link TransactionId} value.
     */
    public static String createSubTransactionIdValue() {
        return get().getTransactionId().createSubTransactionId().getValue();
    }

}
