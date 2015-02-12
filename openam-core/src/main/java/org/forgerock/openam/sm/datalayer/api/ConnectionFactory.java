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

package org.forgerock.openam.sm.datalayer.api;

import java.io.Closeable;

import org.forgerock.util.promise.Promise;

/**
 * Creates connections of type T either asynchronously or synchronously.
 * @param <T>
 */
public interface ConnectionFactory<T> extends Closeable {

    /**
     * Creates a connection asynchronously. The returned promise can be used to obtain and act on the eventually
     * created connnection, or handle failure.
     * @return A promise that will be resolved with a connection.
     */
    Promise<T, DataLayerException> createAsync();

    /**
     * Create a connection synchronously.
     * @return The created connection.
     */
    T create() throws DataLayerException;

    /**
     * If the underlying factory logic needs any specific close behaviour.
     */
    void close();

    /**
     * Checks if a (possibly null) connection is valid for use.
     */
    boolean isValid(T connection);
}
