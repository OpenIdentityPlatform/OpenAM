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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.model;

import org.forgerock.util.Reject;

/**
 * This class represents an OpenID Connect ID Token. The tokenValue is the base64Url encoded representation of the
 * jws or jwt ID Token. This class will serve as a type specifier for the generic types required in the AuthenticationHandler
 * and TokenAuthenticationRequestDispatcher, and contain classes to marshal to/from json and XML, as required by the
 * SOAP and REST STS.
 */
public class OpenIdConnectIdToken {
    private final String tokenValue;

    public OpenIdConnectIdToken(String tokenValue) {
        this.tokenValue = tokenValue;
        Reject.ifNull(this.tokenValue, "A non-null token value must be specified.");
    }

    public String getTokenValue() {
        return tokenValue;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OpenIdConnectIdToken) {
            OpenIdConnectIdToken otherIdToken = (OpenIdConnectIdToken)other;
            return tokenValue.equals(otherIdToken.tokenValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return tokenValue;
    }

    @Override
    public int hashCode() {
        return tokenValue.hashCode();
    }
}
