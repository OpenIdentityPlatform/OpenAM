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

package org.forgerock.openam.shared.audit.context;

import org.forgerock.util.Reject;

/**
 * Thread-local context for Commons Audit state.
 *
 * @since 13.0.0
 */
public class AuditRequestContext {

    private static final ThreadLocal<AuditRequestContext> instances = new ThreadLocal<AuditRequestContext>() {
        @Override
        protected AuditRequestContext initialValue() {
            return new AuditRequestContext(new TransactionId());
        }
    };

    private final TransactionId transactionId;

    /**
     * Construct a new <code>RequestContext</code>.
     *
     * @param transactionId Non-null, <code>TransactionId</code>.
     */
    public AuditRequestContext(TransactionId transactionId) {
        Reject.ifNull(transactionId, "TransactionId should not be null.");
        this.transactionId = transactionId;
    }

    /**
     * @return Non-null, <code>TransactionId</code>.
     */
    public TransactionId getTransactionId() {
        return transactionId;
    }

    /**
     * @return Non-null, thread local <code>RequestContext</code>.
     */
    public static AuditRequestContext get() {
        return instances.get();
    }

    /**
     * Sets <code>RequestContext</code> of the current thread.
     *
     * @param auditRequestContext Non-null, <code>RequestContext</code>.
     */
    public static void set(AuditRequestContext auditRequestContext) {
        Reject.ifNull(auditRequestContext, "RequestContext should not be null.");
        instances.set(auditRequestContext);
    }

    /**
     * Discards the <code>RequestContext</code> of the current thread.
     */
    public static void clear() {
        instances.remove();
    }

    /**
     * @return Non-null, {@link TransactionId} value of the current thread.
     */
    public static String getTransactionIdValue() {
        return get().getTransactionId().getValue();
    }

    /**
     * @return Non-null, {@link TransactionId} value of the current thread extended for propagation to another process.
     */
    public static String createSubTransactionIdValue() {
        return get().getTransactionId().createSubTransactionId().getValue();
    }

}
