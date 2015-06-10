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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.token.provider;

import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;

import java.util.Set;

/**
 * This interface defines the consumption of the TokenGenerationService. It is currently only consumed by the
 * AMSAMLTokenProvider.
 */
public interface TokenGenerationServiceConsumer {
    /**
     * Invoke the TokenGenerationService to produce a SAML2 Bearer assertion
     * @param ssoTokenString The session id corresponding to the to-be-asserted subject
     * @param stsInstanceId  The instance id of the STS making the invocation
     * @param realm The realm of the STS making the invocation
     * @param authnContextClassRef The SAML2 AuthnContext class ref to be included in the SAML2 assertion
     * @param callerSSOTokenString The session id corresponding to the caller. The TokenGenerationService will be protected
     *                             by an authz module, which, in the first release, will require an admin token.
     * @return The string representation of the issued token.
     * @throws TokenCreationException if the token could not be created.
     */
    String getSAML2BearerAssertion(String ssoTokenString, String stsInstanceId, String realm,
                                   String authnContextClassRef, String callerSSOTokenString) throws TokenCreationException;


    /**
     * Invoke the TokenGenerationService to produce a SAML2 Bearer assertion
     * @param ssoTokenString The session id corresponding to the to-be-asserted subject
     * @param stsInstanceId  The instance id of the STS making the invocation
     * @param realm The realm of the STS making the invocation
     * @param authnContextClassRef The SAML2 AuthnContext class ref to be included in the SAML2 assertion
     * @param callerSSOTokenString The session id corresponding to the caller. The TokenGenerationService will be protected
     *                             by an authz module, which, in the first release, will require an admin token.
     * @return The string representation of the issued token.
     * @throws TokenCreationException if the token could not be created.
     */
    String getSAML2SenderVouchesAssertion(String ssoTokenString, String stsInstanceId, String realm,
                                          String authnContextClassRef, String callerSSOTokenString) throws TokenCreationException;

    /**
     * Invoke the TokenGenerationService to produce a SAML2 Bearer assertion
     * @param ssoTokenString The session id corresponding to the to-be-asserted subject
     * @param stsInstanceId  The instance id of the STS making the invocation
     * @param realm The realm of the STS making the invocation
     * @param authnContextClassRef The SAML2 AuthnContext class ref to be included in the SAML2 assertion
     * @param proofTokenState The ProofTokenState used as the proof token in the HoK assertion.
     * @param callerSSOTokenString The session id corresponding to the caller. The TokenGenerationService will be protected
     *                             by an authz module, which, in the first release, will require an admin token.
     * @return The string representation of the issued token.
     * @throws TokenCreationException if the token could not be created.
     */
    String getSAML2HolderOfKeyAssertion(String ssoTokenString, String stsInstanceId, String realm,
                                        String authnContextClassRef, ProofTokenState proofTokenState,
                                        String callerSSOTokenString) throws TokenCreationException;

    /**
     * Invoke the TokenGenerationService to produce a OpenId Connect Token
     * @param ssoTokenString The session id corresponding to the to-be-asserted subject
     * @param stsInstanceId  The instance id of the STS making the invocation
     * @param realm The realm of the STS making the invocation
     * @param authnContextClassRef The OpenIdConnect AuthnContext class ref to be included in the OIDC token. Corresponds
     *                             to the acr claim specified here http://openid.net/specs/openid-connect-core-1_0.html#IDToken
     *                             Can be null.
     * @param authnMethodReferences State corresponding to the amr claim included in the OIDC token as defined here:
     *                            http://openid.net/specs/openid-connect-core-1_0.html#IDToken. Can be null
     * @param authnTimeInSeconds  used to set the auth_time claim as specified here:
     *                            http://openid.net/specs/openid-connect-core-1_0.html#IDToken.
     * @param nonce Used to set the nonce claim as specified here:
     *              http://openid.net/specs/openid-connect-core-1_0.html#IDToken. This value will be taken from the original
     *              sts invocation.
     * @param callerSSOTokenString The session id corresponding to the caller. The TokenGenerationService will be protected
     *                             by an authz module, which, in the first release, will require an admin token.
     * @return The string representation of the issued token.
     * @throws TokenCreationException if the token could not be created.
     */
    String getOpenIdConnectToken(String ssoTokenString, String stsInstanceId, String realm,
                                        String authnContextClassRef, Set<String> authnMethodReferences,
                                        long authnTimeInSeconds, String nonce,
                                        String callerSSOTokenString) throws TokenCreationException;
}
