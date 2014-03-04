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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;

import javax.servlet.http.HttpServletRequest;

/**
 * This is the top-level interface invoked directly by the REST-STS resource. Each of the methods defined in this interface
 * will correlate to the top-level operations defined in the REST-STS. Currently, the single top-level operation is
 * the TokenTranslateOperation. If we decide that the REST-STS implement issue/cancel/validate/renew operations like
 * the WS-Trust STS, method definitions corresponding to this functionality would be added to this interface.
 */
public interface RestSTS {
    /**
     *
     * @param inputToken The json representation of the input token
     * @param desiredTokenType The specification of the desired token type
     * @param request
     * @return A json representation of the translated token.
     * @throws TokenValidationException TokenCreationException If the token translation could not be completed, due to failed token
     * validation, creation, invalid arguments, etc.

    public JsonValue translateToken(JsonValue inputToken, String desiredTokenType, HttpServletRequest request)
            throws TokenValidationException, TokenCreationException;
    */

    /**
     *
     * @param inputToken
     * @param desiredTokenType
     * @param httpContext
     * @param securityContext
     * @return
     * @throws TokenValidationException
     * @throws TokenCreationException
     */
    public JsonValue translateToken(JsonValue inputToken,
                                    String desiredTokenType,
                                    HttpContext httpContext,
                                    SecurityContext securityContext)
                                    throws TokenValidationException, TokenCreationException;

}
