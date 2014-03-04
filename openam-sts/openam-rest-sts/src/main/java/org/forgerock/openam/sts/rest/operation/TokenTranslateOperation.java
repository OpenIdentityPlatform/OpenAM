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

package org.forgerock.openam.sts.rest.operation;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;

import javax.servlet.http.HttpServletRequest;

/**
 * This class is the analogue to the Token{Issue/Validate/Renew/Cancel}Operation instances plugged-into the CXF-STS.
 * It is a 'top-level' operation, invoked directly by the implementation of the RestSTS.validateToken method.
 * It may be that other 'top-level' interfaces will be defined, corresponding to the {Issue/Validate/Renew/Cancel}
 * operations of the WS-Trust STS.
 */
public interface TokenTranslateOperation {
    JsonValue translateToken(JsonValue inputToken,
                             String desiredToken,
                             HttpContext httpContext,
                             SecurityContext securityContext) throws TokenValidationException, TokenCreationException;
}
