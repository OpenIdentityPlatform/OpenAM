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

package org.forgerock.oauth2.core.exceptions;

import org.forgerock.oauth2.core.OAuth2Request;

/**
 * Produces exceptions to handle the invalid_client error of the OAuth2 specification, particularly
 * in handling the error code returned.
 *
 * @since 13.0.0
 */
public abstract class ClientAuthenticationFailureFactory {

    /**
     * Produces an InvalidClientException. Used for cases where information about the request is unknown.
     * @return InvalidClientException, an exception reporting that the client cannot be authenticated
     */
    public InvalidClientException getException() {
        return new InvalidClientException();
    }

    /**
     * Produces an InvalidClientException. Used for cases where information about the request is unknown.
     * @param message The message which will be reported
     * @return InvalidClientException, an exception reporting that the client cannot be authenticated
     */
    public InvalidClientException getException(String message) {
        return new InvalidClientException(message);
    }

    /**
     * Produces an InvalidClientException or InvalidClientAuthZHeaderException based on the request provided.
     * Establishes which of the two exceptions is appropriate
     * @param request The request that has failed to authenticate the user
     * @param message The message which will be reported
     * @return InvalidClientException or InvalidClientAuthZHeaderException, dependant on if the request uses
     * and authorization header
     */
    public InvalidClientException getException(OAuth2Request request, String message) {
        if(request != null && hasAuthorizationHeader(request)) {
            return new InvalidClientAuthZHeaderException(message, "Basic", getRealm(request));
        }
        return getException(message);
    }

    /**
     * Determines whether the request makes use of the authorization header
     * @param request The request to examine
     * @return True if the authorization header is set
     */
    protected abstract boolean hasAuthorizationHeader(OAuth2Request request);

    /**
     * Extracts the realm from the request, and normalises it
     * @param request The request to examine
     * @return A normalised realm
     */
    protected abstract String getRealm(OAuth2Request request);
}
