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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.shared.jaxrpc.golden;

import com.sun.identity.shared.jaxrpc.SOAPClient;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * One recorded JAX-RPC exchange: the request the client SDK sends for an operation and
 * what the endpoint implementation does with it.
 */
public final class GoldenCase {

    private final Class<?> iface;
    private final String id;
    private final String operation;
    private final Object[] clientArgs;
    private final Object result;
    private final Exception thrown;
    private final boolean clientObservableOnly;

    /**
     * @param iface the endpoint interface.
     * @param id {@code <operation>__<kind>}, unique per interface; the base name of the recorded files.
     * @param operation the SOAP operation, normally a method of {@code iface}.
     * @param clientArgs the arguments passed to {@code SOAPClient.encodeMessage}.
     * @param result what the target returns; {@code null} for void.
     * @param thrown if non-null, the target throws it instead of returning {@code result}.
     * @param clientObservableOnly true when a replay compares the client-observable outcome only.
     */
    public GoldenCase(Class<?> iface, String id, String operation, Object[] clientArgs,
                      Object result, Exception thrown, boolean clientObservableOnly) {
        this.iface = iface;
        this.id = id;
        this.operation = operation;
        this.clientArgs = clientArgs;
        this.result = result;
        this.thrown = thrown;
        this.clientObservableOnly = clientObservableOnly;
    }

    public String id() {
        return id;
    }

    public Class<?> iface() {
        return iface;
    }

    public String operation() {
        return operation;
    }

    public Object[] clientArgs() {
        return clientArgs;
    }

    public Object result() {
        return result;
    }

    public Exception thrown() {
        return thrown;
    }

    public boolean clientObservableOnly() {
        return clientObservableOnly;
    }

    /**
     * @return the request the client SDK sends for this case.
     */
    public String request() {
        try {
            return new SOAPClient().encodeMessage(operation, clientArgs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return iface.getSimpleName() + "/" + id;
    }
}
