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

package org.forgerock.openam.sts.token.validator.wss.url;

import org.forgerock.openam.sts.TokenValidationException;

import java.net.URL;

/**
 * This interface defines the means for the AuthenticationHandler to obtain the URI to pass to the
 * TokenAuthenticationRequestDispatcher. Implementations of this interface will encapsulate the realm and authIndexType
 * and authIndexValue mappings necessary to return the appropriate URI. In other words, each STS instance will be created
 * in the context of a specific realm. Each STS instance will also be created with a mapping which specifies the
 * authIndexType and authIndexValue to be targeted for the validation of a particular token type (e.g. x509Tokens should
 * be validated against an authIndexType=module and authIndexValue=MySpecialCertModule, etc.)
 *
  */
public interface AuthenticationUrlProvider {
    /**
     * Returns the String representing the URL against which the authentication invocation will be made.
     * @param token The to-be-validated Token. These will ultimately be class instances in the org.apache.ws.security.message.token
     *              package, but there is no interface common to all these token classes, so Object must suffice.
     * @return The URL targeted by the authentication request - e.g. with the appropriate
     * ?realm=phill&authIndexType=service&authIndexValue=[SERVICE_NAME] elements appended onto the httpBasePath.
     */
    URL authenticationUrl(Object token) throws TokenValidationException;
}
