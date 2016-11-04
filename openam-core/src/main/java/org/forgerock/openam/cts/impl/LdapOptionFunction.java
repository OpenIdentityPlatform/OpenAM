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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.cts.impl;

import org.forgerock.opendj.ldap.requests.Request;
import org.forgerock.util.Option;
import org.forgerock.util.Options;

/**
 * A function that is invoked for a given {@link Option}.
 *
 * @since 14.0.0
 */
public interface LdapOptionFunction {

    /**
     * Invoked for a given {@code Option} which is contained within {@literal options}.
     * <p>
     * If the required option is not present in {@literal options} then this function should return
     * the original {@literal request} object, unmodified.
     *
     * @param request The CTS LDAP {@link Request}.
     * @param options The {@link Options}.
     * @param <R> The type of the LDAP request.
     * @return The {@link Request} to use in the CTS operation.
     */
    <R extends Request> R apply(R request, Options options);
}
