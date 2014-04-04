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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.Map;

/**
 * This interface provides the functions that need to be implemented to create a response type for the authorize
 * endpoint.
 *
 * @supported.all.api //TODO what state is the api in? (final/evolving?)
 */
public interface ResponseType {

    /**
     * Creates a token for a response type.
     *
     * @param data The data needed to create the token.
     * @return The created token.
     */
    public CoreToken createToken(Map<String, Object> data);

    /**
     * Returns the location in the HTTP response the token should be returned.
     *
     * @return A string of either FRAGMENT or QUERY.
     */
    public String getReturnLocation();

    /**
     * The parameter in the URI to return the token as.
     *
     * @return The URI to return the token as.
     */
    public String getURIParamValue();
}
