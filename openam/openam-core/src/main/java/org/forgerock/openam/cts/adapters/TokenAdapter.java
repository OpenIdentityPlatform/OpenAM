/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.cts.adapters;

import org.forgerock.openam.cts.api.tokens.Token;

/**
 * Describes the ability to convert from one type of object into a Token and the
 * reverse operation of converting from a Token into the object of type T.
 *
 * This is a key feature of the Core Token Service, which acts as a generic
 * token storage mechanism.
 *
 * @see org.forgerock.openam.cts.api.tokens.Token
 *
 * @author robert.wapshott@forgerock.com
 */
public interface TokenAdapter<T> {

    /**
     * @param t Object of type T to convert to a Token.
     * @return A non null Token.
     */
    public Token toToken(T t);

    /**
     * @param token Token to be converted back to its original type of T.
     * @return Non null object of type T.
     */
    public T fromToken(Token token);
}
