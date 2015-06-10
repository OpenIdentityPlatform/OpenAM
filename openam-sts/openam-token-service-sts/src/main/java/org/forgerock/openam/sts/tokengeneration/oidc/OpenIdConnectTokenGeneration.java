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

package org.forgerock.openam.sts.tokengeneration.oidc;

import com.iplanet.sso.SSOToken;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;

/**
 * Encapsulates the concerns of generating a OpenIdConnect Id Token.
 */
public interface OpenIdConnectTokenGeneration {
    /**
     *
     * @param subjectToken The SSOToken authenticating the identity of the subject of the token
     * @param stsInstanceState The STS-instance-specific state necessary to generate the token
     * @param invocationState The parameters with which the TokenGenerationService was invoked.
     * @return The base64-encoded String representation of the token
     * @throws TokenCreationException if the token could not be successfully created
     */
    String generate(SSOToken subjectToken, STSInstanceState stsInstanceState,
                    TokenGenerationServiceInvocationState invocationState) throws TokenCreationException;

}
