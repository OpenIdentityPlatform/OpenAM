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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.exceptions;

import com.sun.identity.authentication.service.AMAuthErrorCode;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * This exception is designed to be thrown from RESTful authentication calls when authentication fails with a
 * AMAuthErrorCode.
 */
public class RestAuthErrorCodeException extends RestAuthException {

    /**
     * Constructs a RestAuthException.
     *
     * @param errorCode The AMAuthErrorCode for the reason of authentication failure.
     * @param errorMessage The error message relating to the exception.
     */
    public RestAuthErrorCodeException(String errorCode, String errorMessage) {
        super(getResponseStatus(errorCode), errorMessage);
    }

    /**
     * Translates the AMAuthErrorCode into a HTTP error code.
     *
     * @param errorCode The AMAuthErrorCode for the reason of authentication failure.
     * @return The HTTP error code for the AMAuthErrorCode.
     */
    private static Response.Status getResponseStatus(String errorCode) {

        if (get401AuthErrorCodes().contains(errorCode)) {
            return Response.Status.UNAUTHORIZED;
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Returns a List of the AMAuthErrorCodes which map to the HTTP 401 Unauthorized error code.
     *
     * @return A List of HTTP 401 AMAuthErrorCodes.
     */
    private static List get401AuthErrorCodes() {

        String[] amAuth401ErrorCodes = new String[]{AMAuthErrorCode.AUTH_INVALID_PASSWORD,
                AMAuthErrorCode.AUTH_PROFILE_ERROR, AMAuthErrorCode.AUTH_USER_NOT_FOUND,
                AMAuthErrorCode.AUTH_USER_INACTIVE, AMAuthErrorCode.AUTH_USER_LOCKED,
                AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED, AMAuthErrorCode.AUTH_LOGIN_FAILED,
                AMAuthErrorCode.AUTH_MAX_SESSION_REACHED, AMAuthErrorCode.AUTH_ERROR};

        return Arrays.asList(amAuth401ErrorCodes);
    }
}
