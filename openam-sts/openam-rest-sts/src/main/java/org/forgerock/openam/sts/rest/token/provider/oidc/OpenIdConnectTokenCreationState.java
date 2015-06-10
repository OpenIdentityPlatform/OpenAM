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

package org.forgerock.openam.sts.rest.token.provider.oidc;

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;

/**
 * Encapsulates the invocation-specific state necessary to create OpenIdConnect tokens.
 */
public class OpenIdConnectTokenCreationState implements TokenTypeId {
    private final String nonce;
    private final long authenticationTimeInSeconds;

    public OpenIdConnectTokenCreationState(String nonce, long authenticationTimeInSeconds) {
        this.nonce = nonce;
        this.authenticationTimeInSeconds = authenticationTimeInSeconds;
    }

    public String getNonce() {
        return nonce;
    }

    public long getAuthenticationTimeInSeconds() {
        return authenticationTimeInSeconds;
    }

    @Override
    public String getId() {
        return TokenType.OPENIDCONNECT.name();
    }
}
