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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Value logged with every Commons Audit event.
 *
 * TransactionId value should be unique per request coming from an external agent so that all events occurring in
 * response to the same external stimulus can be tied together.
 *
 * Calls to external systems should propagate the value returned by {@link #createSubTransactionId()} so that Audit
 * events reported by the external system can also be tied back to the original stimulus.
 *
 * Due to the fact that each TransactionId instance creates a sequence of sub-transaction IDs, the same TransactionId
 * object should be used while fulfilling a given request; it is not appropriate to create multiple instances of
 * TransactionId with the same value as this would lead to duplicate sub-transaction ID values. As such, two instances
 * of TransactionId with the same value are not considered equal.
 *
 * @since 13.0.0
 */
public final class TransactionId {

    /**
     * Clients can set this HTTP header in order to specify the transactionId value that will be
     * assigned to every Audit Event.
     *
     * If this HTTP header is not set, then a unique random transactionId value will be automatically generated.
     */
    public static final String HTTP_HEADER = "X-ForgeRock-TransactionId";

    private final String value;
    private final AtomicInteger subTransactionIdCounter;

    /**
     * Construct a <code>TransactionId</code> with a random value.
     */
    public TransactionId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Construct a <code>TransactionId</code> with the specified value.
     */
    public TransactionId(String value) {
        Reject.ifNull(value, "value should not be null.");
        this.value = value;
        this.subTransactionIdCounter = new AtomicInteger(0);
    }

    /**
     * @return Non-null, <code>TransactionId</code> value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return Non-null, <code>TransactionId</code> value that can be passed to an external system.
     */
    public TransactionId createSubTransactionId() {
        final String subTransactionId = value + "/" + subTransactionIdCounter.getAndIncrement();
        return new TransactionId(subTransactionId);
    }
}
