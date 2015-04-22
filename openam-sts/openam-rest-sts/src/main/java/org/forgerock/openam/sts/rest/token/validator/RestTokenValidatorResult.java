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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.validator;

import java.security.Principal;

/**
 * Class defining state returned a successful invocation of RestTokenValidator#validateToken (failed validation will
 * throw a TokenValidationException). Encapsulates both the Principal corresponding to the successful token validation,
 * and the OpenAM session id corresponding to this validation.
 *
 * Note that ultimately, RestTokenProviders will obtain the OpenAM session id from the ThreadLocalAMTokenCache, to support
 * token operations where the OpenAM session cannot be passed, as a parameter, from producers to consumers (as is necessary
 * in the soap-sts context). The sessionId is included in the result with an eye on supporting user-provided, custom
 * RestTokenValidator invocations, so that the caching of the session id does not have to be the responsibility of these
 * custom implementations.
 *
 */
public class RestTokenValidatorResult {
    private final Principal principal;
    private final String amSessionId;

    public RestTokenValidatorResult(Principal principal, String amSessionId) {
        this.principal = principal;
        this.amSessionId = amSessionId;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getAMSessionId() {
        return amSessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RestTokenValidatorResult that = (RestTokenValidatorResult) o;
        return principal.equals(that.principal) && amSessionId.equals(that.amSessionId);
    }

    @Override
    public int hashCode() {
        return amSessionId.hashCode();
    }
}
