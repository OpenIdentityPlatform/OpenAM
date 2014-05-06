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

package org.forgerock.openam.sts.tokengeneration.saml2;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.assertion.Assertion;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.tokengeneration.service.TokenGenerationServiceInvocationState;

import java.util.List;

/**
 * This interface defines the concerns of SAML2TokenGeneration, as exposed by the TokenGenerationService, and consumed by
 * published STS instances, both REST and SOAP.
 *
 * The implementation of this interface will consult the SAML2Config of the STSInstanceConfig referenced in the invocation
 * to determine if any custom statement providers have been registered with the published STS.
 *
 */
public interface SAML2TokenGeneration {
    /**
     *
     * @param subjectToken The SSOToken authenticating the identity of the subject of the assertion
     * @param stsInstanceState The STS-instance-specific state necessary to generate the assertion
     * @param subjectConfirmation The subject confirmation method for the issued assertion
     * @param invocationClaims TODO: the WS-TRUST STS provides for claims to be specified in the invocation - should this be supported?
     * @param audienceId The ultimate consumer of the assertion.
     * @return The String representation of the assertion
     * @throws TokenCreationException
     */
    String generate(SSOToken subjectToken, STSInstanceState stsInstanceState,
                       TokenGenerationServiceInvocationState.SAML2SubjectConfirmation subjectConfirmation,
                       List<String> invocationClaims, String audienceId) throws TokenCreationException;


}
