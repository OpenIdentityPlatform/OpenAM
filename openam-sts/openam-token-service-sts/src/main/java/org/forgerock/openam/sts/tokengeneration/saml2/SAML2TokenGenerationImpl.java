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
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import org.apache.xml.security.signature.XMLSignature;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.invocation.ProofTokenState;
import org.forgerock.openam.sts.tokengeneration.service.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2AssertionSigner;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.forgerock.openam.sts.tokengeneration.service.TokenGenerationServiceInvocationState.SAML2SubjectConfirmation;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.Date;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration
 */
public class SAML2TokenGenerationImpl implements SAML2TokenGeneration {
    private static final String DSA_PRIVATE_KEY_ALGORITHM = "DSA";
    private static final String RSA_PRIVATE_KEY_ALGORITHM = "RSA";
    private static final boolean ASSERTION_TO_STRING_INCLUDE_NAMESPACE_PREFIX = true;
    private static final boolean ASSERTION_TO_STRING_DECLARE_NAMESPACE_PREFIX = true;

    private static final String DSA_DEFAULT_SIGNATURE_ALGORITHM = XMLSignature.ALGO_ID_SIGNATURE_DSA;
    private static final String RSA_DEFAULT_SIGNATURE_ALGORITHM = XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;

    private final SAML2AssertionSigner saml2AssertionSigner;
    private final StatementProvider statementProvider;
    private final XMLUtilities xmlUtilities;
    private final SSOTokenIdentity ssoTokenIdentity;

    @Inject
    SAML2TokenGenerationImpl(SAML2AssertionSigner saml2AssertionSigner, StatementProvider statementProvider,
                             XMLUtilities xmlUtilities, SSOTokenIdentity ssoTokenIdentity) {
        this.saml2AssertionSigner = saml2AssertionSigner;
        this.statementProvider = statementProvider;
        this.xmlUtilities = xmlUtilities;
        this.ssoTokenIdentity = ssoTokenIdentity;
        /*
        Initialize the santuario library context. Multiple calls to this method are idempotent.
         */
        org.apache.xml.security.Init.init();
    }

    /*
    For now, a set of claims specified in the invocation will not be supported. In other words, in the standard WS-Trust
    model, the SecurityPolicy bindings in a web-service wsdl can specify that a token must be obtained from an STS, and,
    furthermore, that a given set of claims should be included in the token. Thus the RequestSecurityToken handled by
    a WS-Trust STS allows for the specification of a set of claims. The CXF-STS handles these claims, rejecting them
    if no handler for a given claim has been registered. If I want to support this model all the way through to the
     TokenGenerationService, then I would have to allow for claims specification in the TokenGenerationInvocationState.
     I will not add this support for now, as the ability to publish a set of STS instances, each with a distinct set
     of attribute mappings, may well accommodate any needs around the flexible inclusion of claims. Furthermore, adding
     invocation parameters supporting the relatively complicated attribute mapping strings (static and binary attr support)
     may make the API a bit unwieldy, and this attribute format does not map to the claims as defined in WS-Trust and
     implemented in the CXF-STS.
     */
    public String generate(SSOToken subjectToken, STSInstanceState stsInstanceState,
                               TokenGenerationServiceInvocationState invocationState) throws TokenCreationException {

        final SAML2Config saml2Config = stsInstanceState.getConfig().getSaml2Config();
        final String subjectId = ssoTokenIdentity.validateAndGetTokenPrincipal(subjectToken);
        final Assertion assertion = AssertionFactory.getInstance().createAssertion();
        setVersionAndId(assertion);
        setIssuer(assertion, stsInstanceState.getConfig());

        final Date issueInstant = new Date();
        setIssueInstant(assertion, issueInstant);
        setConditions(assertion, saml2Config, issueInstant, invocationState.getSaml2SubjectConfirmation());
        setSubject(assertion, subjectId, invocationState.getSpAcsUrl(), saml2Config,
                invocationState.getSaml2SubjectConfirmation(), issueInstant, invocationState.getProofTokenState());
        setAuthenticationStatements(assertion, saml2Config, invocationState.getAuthnContextClassRef());
        setAttributeStatements(assertion, subjectToken, saml2Config);
        setAuthzDecisionStatements(assertion, subjectToken, saml2Config);

        if (invocationState.getSignAssertion()) {
            return signAssertion(assertion, stsInstanceState);
        } else {
            try {
                return assertion.toXMLString(ASSERTION_TO_STRING_INCLUDE_NAMESPACE_PREFIX, ASSERTION_TO_STRING_DECLARE_NAMESPACE_PREFIX);
            } catch (SAML2Exception e) {
                throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                        "Exception caught calling Assertion.toXMLString: " + e, e);
            }
        }
    }

    private void setVersionAndId(Assertion assertion) throws TokenCreationException {
        try {
            assertion.setVersion("2.0");
            assertion.setID(SAML2SDKUtils.generateID());
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting version and/or id in SAML2TokenGenerationImpl: " + e, e);
        }
    }

    private void setIssuer(Assertion assertion, STSInstanceConfig config) throws TokenCreationException {
        final Issuer issuer = AssertionFactory.getInstance().createIssuer();
        try {
            issuer.setValue(config.getIssuerName());
            assertion.setIssuer(issuer);
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting issuer in SAML2TokenGenerationImpl: " + e, e);
        }
    }

    private void setIssueInstant(Assertion assertion, Date issueInstant) throws TokenCreationException {
        try {
            assertion.setIssueInstant(issueInstant);
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting issue instant in SAML2TokenGenerationImpl: " + e, e);
        }
    }

    private void setConditions(Assertion assertion, SAML2Config saml2Config,
                               Date issueInstant,
                               SAML2SubjectConfirmation saml2SubjectConfirmation) throws TokenCreationException {
        try {
            assertion.setConditions(statementProvider.getConditionsProvider(saml2Config).get(saml2Config, issueInstant, saml2SubjectConfirmation));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting conditions in SAML2TokenGenerationImpl: " + e, e);
        }
    }
    private void setSubject(Assertion assertion, String subjectId, String spAcsUrl, SAML2Config saml2Config,
                            SAML2SubjectConfirmation subjectConfirmation,
                            Date assertionIssueInstant, ProofTokenState proofTokenState) throws TokenCreationException {
        try {
            assertion.setSubject(statementProvider.getSubjectProvider(saml2Config).get(subjectId, spAcsUrl, saml2Config,
                    subjectConfirmation, assertionIssueInstant, proofTokenState));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting subject in SAML2TokenGenerationImpl: " + e, e);
        }
    }

    private void setAuthenticationStatements(Assertion assertion, SAML2Config saml2Config, String authnContextClassRef) throws TokenCreationException {
        try {
            assertion.setAuthnStatements(statementProvider.getAuthenticationStatementsProvider(saml2Config).get(saml2Config, authnContextClassRef));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting authentication statement in SAML2TokenGenerationImpl: " + e, e);
        }
    }

    private void setAttributeStatements(Assertion assertion, SSOToken token, SAML2Config saml2Config) throws TokenCreationException {
        assertion.getAttributeStatements().addAll(
                statementProvider.getAttributeStatementsProvider(saml2Config).get(
                        token, saml2Config, statementProvider.getAttributeMapper(saml2Config)));
    }

    private void setAuthzDecisionStatements(Assertion assertion, SSOToken token, SAML2Config saml2Config) throws TokenCreationException {
        assertion.getAuthzDecisionStatements().addAll(
                statementProvider.getAuthzDecisionStatementsProvider(saml2Config).get(token, saml2Config));
    }

    private String signAssertion(Assertion assertion, STSInstanceState instanceState) throws TokenCreationException {
        Document assertionDocument;
        try {
            assertionDocument = xmlUtilities.stringToDocumentConversion(
                    assertion.toXMLString(ASSERTION_TO_STRING_INCLUDE_NAMESPACE_PREFIX, ASSERTION_TO_STRING_DECLARE_NAMESPACE_PREFIX));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught obtaining String representation of Assertion in SAML2TokenGenerationImpl: " + e, e);
        }
        if (assertionDocument == null) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Could not obtain document representation of Assertion in SAML2TokenGenerationImpl: ");
        }
        final STSKeyProvider stsKeyProvider = instanceState.getKeyProvider();
        final KeystoreConfig keystoreConfig = instanceState.getConfig().getKeystoreConfig();
        String signatureKeyPassword;
        try {
            signatureKeyPassword = new String(keystoreConfig.getSignatureKeyPassword(), AMSTSConstants.UTF_8_CHARSET_ID);
        } catch (UnsupportedEncodingException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Could not obtain string representation of signature key password in SAML2TokenGenerationImpl: ");
        }
        /*
        Note: the cert alias and private-key alias are the same. If there is a key entry in the keystore, it seems like
        they are represented by the same alias.
         */
        PrivateKey privateKey = stsKeyProvider.getPrivateKey(keystoreConfig.getSignatureKeyAlias(), signatureKeyPassword);
        String signatureAlgorithm = getSignatureAlgorithm(instanceState.getConfig().getSaml2Config(), privateKey);
        Element signatureElement =
                saml2AssertionSigner.signSAML2Assertion(
                        assertionDocument,
                        assertion.getID(),
                        privateKey,
                        stsKeyProvider.getX509Certificate(keystoreConfig.getSignatureKeyAlias()),
                        signatureAlgorithm,
                        instanceState.getConfig().getSaml2Config().getCanonicalizationAlgorithm());
        return xmlUtilities.documentToStringConversion(signatureElement.getOwnerDocument().getDocumentElement());
    }

    private String getSignatureAlgorithm(SAML2Config sam2Config, PrivateKey privateKey) throws TokenCreationException {
        if (sam2Config.getSignatureAlgorithm() != null) {
            return sam2Config.getSignatureAlgorithm();
        }
        if (DSA_PRIVATE_KEY_ALGORITHM.equals(privateKey.getAlgorithm())) {
            return DSA_DEFAULT_SIGNATURE_ALGORITHM;
        } else if (RSA_PRIVATE_KEY_ALGORITHM.equals(privateKey.getAlgorithm())) {
            return RSA_DEFAULT_SIGNATURE_ALGORITHM;
        }
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Unexpected PrivateKey algorithm encountered in SAML2TokenGenerationImpl: " + privateKey.getAlgorithm());
    }
}
