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

package org.forgerock.openam.sts.soap.token.provider;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.slf4j.Logger;

import java.util.List;

/**
 * This class contains functionality common to both the OpenIdConnect and SAML2 TokenProvider implementations. This
 * functionality is largely concerned with extracting the input token type and input token object from the yield
 * of SecurityPolicy binding traversal, or from the delegated token context. This state is necessary for the pluggable
 * generation of authentication context state, which is common to both SAML2 and OpenIdConnect tokens.
 */
public abstract class SoapTokenProviderBase implements TokenProvider {
    protected final Logger logger;
    protected final XMLUtilities xmlUtilities;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;

    public SoapTokenProviderBase(SoapSTSAccessTokenProvider soapSTSAccessTokenProvider, XMLUtilities xmlUtilities, Logger logger) {
        this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
        this.logger = logger;
        this.xmlUtilities = xmlUtilities;
    }

    protected String getTokenGenerationServiceConsumptionToken() throws TokenCreationException {
        try {
            return soapSTSAccessTokenProvider.getAccessToken();
        } catch (ResourceException e) {
            throw new TokenCreationException(e.getCode(), e.getMessage(), e);
        }
    }

    protected void invalidateTokenGenerationServiceConsumptionToken(String consumptionToken) {
        soapSTSAccessTokenProvider.invalidateAccessToken(consumptionToken);
    }

    protected AuthenticationContextMapperState getAuthenticationContextMapperState(TokenProviderParameters tokenProviderParameters) {
        /*
        Add a sanity check as the TokenProviders could be called in a Renew operation, if bound. This will not be the case for
        the 13 release, but might be implemented in a release thereafter.
         */
        if (tokenProviderParameters.getTokenRequirements().getRenewTarget() != null) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, "Illegal state in " +
                    "SoapTokenProviderBase#getAuthenticationContextMapperState: the renewTarget ReceivedToken in the " +
                    "TokenProviderParameters is not null. Is a Renew operation bound?");
        }
        final List<WSHandlerResult> handlerResults = getHandlerResults(tokenProviderParameters);
        if (isDelegatedIssueOperation(tokenProviderParameters)) {
            return new AuthenticationContextMapperState(handlerResults, getDelegatedToken(tokenProviderParameters));
        } else {
            return new AuthenticationContextMapperState(handlerResults);
        }
    }

    private ReceivedToken getDelegatedToken(TokenProviderParameters tokenProviderParameters) {
        if (tokenProviderParameters.getTokenRequirements().getActAs() != null) {
            return tokenProviderParameters.getTokenRequirements().getActAs();
        } else if (tokenProviderParameters.getTokenRequirements().getOnBehalfOf() != null) {
            return tokenProviderParameters.getTokenRequirements().getOnBehalfOf();
        } else {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "IllegalState in SoapTokenProviderBase#getDelegatedToken - neither ActAs nor " +
                            "OnBehalfOf tokens found!");
        }
    }

    private List<WSHandlerResult> getHandlerResults(TokenProviderParameters tokenProviderParameters) {
        return CastUtils.cast((List<?>)
                tokenProviderParameters.getWebServiceContext().getMessageContext().get(WSHandlerConstants.RECV_RESULTS));
    }

    /*
    Method called by the SoapOpenIdConnectTokenProvider. Used to determine if the OpenAM token cached in the
    ThreadLocalAMTokenCache according the ValidationInvocationContext.SOAP_TOKEN_DELEGATION or
    ValidationInvocationContext.SOAP_SECURITY_POLICY context should be used as the token of the subject identity
    asserted in the issued OIDC token. In other words, we will support issuing OIDC tokens in a delegated context, where
    the credentials corresponding to the subject asserted in the issued OIDC token corresponds to those in the ActAs/OnBehalfOf element
    sent in the issue RequestSecurityToken request (and thus cached in the ThreadLocalAMTokenCache in the
    ValidationInvocationContext.SOAP_TOKEN_DELEGATION context), as opposed to the credentials used to traverse the
    SecurityPolicy bindings protecting the sts, whose corresponding OpenAM session token will be cached in the
    ThreadLocalAMTokenCache in the ValidationInvocationContext.SOAP_SECURITY_POLICY context. This method allows the
    SoapOpenIdConnectTokenProvider to determine which ValidationInvocationContext enum to pass to the ThreadLocalAMTokenCache
    to obtain the OpenAM session token which corresponds to the subject identity asserted in the issued OIDC token.

    Method also called from this class to determine state in created AuthenticationContextMapperState.
     */
    protected boolean isDelegatedIssueOperation(TokenProviderParameters tokenProviderParameters) {
        final TokenRequirements tokenRequirements = tokenProviderParameters.getTokenRequirements();
        return isIssueOperation(tokenRequirements) &&
                ((tokenProviderParameters.getTokenRequirements().getOnBehalfOf() != null) ||
                (tokenProviderParameters.getTokenRequirements().getActAs() != null));
    }

    private boolean isIssueOperation(TokenRequirements tokenRequirements) {
        return (tokenRequirements.getRenewTarget() == null) && (tokenRequirements.getValidateTarget() == null)
                && (tokenRequirements.getCancelTarget() == null);
    }

    protected static class AuthenticationContextMapperState {
        private final List<WSHandlerResult> securityPolicyBindingTraversalYield;
        private final ReceivedToken delegatedToken;
        private final boolean isDelegatedContext;

        private AuthenticationContextMapperState(List<WSHandlerResult> securityPolicyBindingTraversalYield, ReceivedToken delegatedToken) {
            this.securityPolicyBindingTraversalYield = securityPolicyBindingTraversalYield;
            this.delegatedToken = delegatedToken;
            this.isDelegatedContext = (delegatedToken != null);
        }

        private AuthenticationContextMapperState(List<WSHandlerResult> securityPolicyBindingTraversalYield) {
            this(securityPolicyBindingTraversalYield, null);
        }

        public List<WSHandlerResult> getSecurityPolicyBindingTraversalYield() {
            return securityPolicyBindingTraversalYield;
        }

        public ReceivedToken getDelegatedToken() {
            return delegatedToken;
        }

        public boolean isDelegatedContext() {
            return isDelegatedContext;
        }
    }
}
