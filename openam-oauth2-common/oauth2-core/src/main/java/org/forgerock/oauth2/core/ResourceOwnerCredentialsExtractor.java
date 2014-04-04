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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

/**
 * Interface for providing the ability to extract the Resource Owner's credentials from a OAuth2 request.
 * <br/>
 * Is typed to allow for different implementation of the Http layer of the OAuth2 spec. (i.e. Restlet Request,
 * HttpServletRequest, ...)
 *
 * @param <T> The type of the OAuth2 Request.
 *
 * @since 12.0.0
 */
public interface ResourceOwnerCredentialsExtractor<T> {

    /**
     * Extracts the resource owner's credentials from the OAuth2 request and returns an {@link AuthenticationHandler}
     * that will be later used to perform the actual authentication of the resource owner.
     *
     * @param request The OAuth2 request.
     * @return An instance of an AuthenticationHandler.
     */
    AuthenticationHandler extract(final T request);
}
