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

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

import java.security.Principal;

/**
 * Class defining state returned a successful invocation of RestTokenTransformValidator#validateToken (failed validation will
 * throw a TokenValidationException). Encapsulates both the Principal corresponding to the successful token validation,
 * and the OpenAM session id corresponding to this validation.
 *
 * Note that ultimately, RestTokenProviders will obtain the OpenAM session id from the ThreadLocalAMTokenCache, to support
 * token operations where the OpenAM session cannot be passed, as a parameter, from producers to consumers (as is necessary
 * in the soap-sts context). The sessionId is included in the result with an eye on supporting user-provided, custom
 * RestTokenTransformValidator invocations, so that the caching of the session id does not have to be the responsibility of these
 * custom implementations.
 *
 */
public class RestTokenTransformValidatorResult {
    private final Principal principal;
    private final String amSessionId;
    private final JsonValue additionalState;

    public RestTokenTransformValidatorResult(Principal principal, String amSessionId) {
        this(principal, amSessionId, null);
    }

    public RestTokenTransformValidatorResult(Principal principal, String amSessionId, JsonValue additionalState) {
        this.principal = principal;
        this.amSessionId = amSessionId;
        this.additionalState = additionalState;
        Reject.ifNull(amSessionId, "All RestTokenTransformValidatorResult instances must reference a non-null amSessionId.");
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getAMSessionId() {
        return amSessionId;
    }

    public JsonValue getAdditionalState() {
        return additionalState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RestTokenTransformValidatorResult that = (RestTokenTransformValidatorResult) o;
        return Objects.equal(principal, that.principal) && amSessionId.equals(that.amSessionId)
                && additionalStateEqual(that);
    }

    private boolean additionalStateEqual(RestTokenTransformValidatorResult that) {
        if ((this.additionalState != null) && (that.additionalState != null)) {
            return this.additionalState.toString().equals(that.additionalState.toString());
        } else if ((this.additionalState == null) && (that.additionalState == null)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return amSessionId.hashCode();
    }
}
