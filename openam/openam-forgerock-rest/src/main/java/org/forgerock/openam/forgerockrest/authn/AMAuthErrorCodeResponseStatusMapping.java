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

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.authentication.service.AMAuthErrorCode;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains methods that return a map of AMErrorCodes to Http Response Status codes.
 */
public class AMAuthErrorCodeResponseStatusMapping {

    /**
     * Returns the Http Status code for the given AMAuth error code.
     *
     * @param authErrorCode The AMAuthErrorCode.
     * @return The Http Status code.
     */
    public int getAuthLoginExceptionResponseStatus(String authErrorCode) {

        int statusCode = Response.Status.UNAUTHORIZED.getStatusCode();

        Map<String, Response.Status> authErrorCodeResponseStatuses = getAMAuthErrorCodeResponseStatuses();

        Response.Status responseStatus = authErrorCodeResponseStatuses.get(authErrorCode);
        if (responseStatus == null && AMAuthErrorCode.AUTH_TIMEOUT.equals(authErrorCode)) {
            statusCode = 408;
        } else if (responseStatus != null) {
            statusCode = responseStatus.getStatusCode();
        }

        return statusCode;
    }

    /**
     * Returns a map of AMErrorCodes to Http Response Status codes.
     *
     * @return A Map of AM error codes to Response.Status.
     */
    private Map<String, Response.Status> getAMAuthErrorCodeResponseStatuses() {

        Map<String, Response.Status> authErrorCodeResponseStatuses = new HashMap<String, Response.Status>();

        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_ERROR, Response.Status.INTERNAL_SERVER_ERROR);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_INVALID_PASSWORD, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_USER_INACTIVE, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_INVALID_PCOOKIE, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_LOGIN_FAILED, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_INVALID_DOMAIN, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_ORG_INACTIVE, Response.Status.BAD_REQUEST);
//        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_TIMEOUT, ???); 408
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_MODULE_DENIED, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_USER_LOCKED, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_USER_NOT_FOUND, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_TYPE_DENIED, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_MAX_SESSION_REACHED, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_PROFILE_CREATE, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_SESSION_CREATE_ERROR,
                Response.Status.INTERNAL_SERVER_ERROR);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.INVALID_AUTH_LEVEL, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.MODULE_BASED_AUTH_NOT_ALLOWED, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_TOO_MANY_ATTEMPTS, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.REMOTE_AUTH_INVALID_SSO_TOKEN, Response.Status.BAD_REQUEST);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.AUTH_USER_LOCKED_IN_DS, Response.Status.UNAUTHORIZED);
        authErrorCodeResponseStatuses.put(AMAuthErrorCode.SESSION_UPGRADE_FAILED, Response.Status.BAD_REQUEST);

        return authErrorCodeResponseStatuses;
    }
}
