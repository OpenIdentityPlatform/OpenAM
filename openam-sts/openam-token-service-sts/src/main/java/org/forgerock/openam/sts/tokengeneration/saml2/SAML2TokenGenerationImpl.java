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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.saml2;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.CTSTokenPersistenceException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.SAML2TokenGenerationState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.CTSTokenPersistence;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.SSOTokenIdentity;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProvider;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration
 */
public class SAML2TokenGenerationImpl implements SAML2TokenGeneration {
    private static final boolean ASSERTION_TO_STRING_INCLUDE_NAMESPACE_PREFIX = true;
    private static final boolean ASSERTION_TO_STRING_DECLARE_NAMESPACE_PREFIX = true;
    private final StatementProvider statementProvider;
    private final SSOTokenIdentity ssoTokenIdentity;
    private final CTSTokenPersistence ctsTokenPersistence;

    @Inject
    SAML2TokenGenerationImpl(StatementProvider statementProvider, SSOTokenIdentity ssoTokenIdentity, CTSTokenPersistence ctsTokenPersistence) {
        this.statementProvider = statementProvider;
        this.ssoTokenIdentity = ssoTokenIdentity;
        this.ctsTokenPersistence = ctsTokenPersistence;
        /*
        Initialize the santuario library context. Multiple calls to this method are idempotent.
         */
        org.apache.xml.security.Init.init();
    }

    public String generate(SSOToken subjectToken, STSInstanceState stsInstanceState,
                               TokenGenerationServiceInvocationState invocationState) throws TokenCreationException {

        final SAML2Config saml2Config = stsInstanceState.getConfig().getSaml2Config();
        if (saml2Config == null) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST,
                    "Invocation targets a SAML2 token, but no SAML2Config was specified in the published sts!");
        }
        final String subjectId = ssoTokenIdentity.validateAndGetTokenPrincipal(subjectToken);
        final Assertion assertion = AssertionFactory.getInstance().createAssertion();
        setVersionAndId(assertion);
        setIssuer(assertion, saml2Config);

        final Date issueInstant = newDate();
        setIssueInstant(assertion, issueInstant);
        final SAML2TokenGenerationState tokenGenerationState = invocationState.getSaml2TokenGenerationState();
        setConditions(assertion, saml2Config, issueInstant, tokenGenerationState.getSaml2SubjectConfirmation());
        setSubject(assertion, subjectId, saml2Config.getSpAcsUrl(), saml2Config,
                invocationState.getSaml2TokenGenerationState().getSaml2SubjectConfirmation(), issueInstant,
                tokenGenerationState.getProofTokenState());
        setAuthenticationStatements(assertion, saml2Config, tokenGenerationState.getAuthnContextClassRef());
        setAttributeStatements(assertion, subjectToken, saml2Config);
        setAuthzDecisionStatements(assertion, subjectToken, saml2Config);
        /*
        entering this branch handles both encryption and signing, as the encryption of the entire assertion must be
        proceeded by signing.
         */
        String assertionString;
        if (saml2Config.encryptAssertion()) {
            EncryptedAssertion encryptedAssertion = handleSingingAndEncryptionOfEntireAssertion(assertion, saml2Config, stsInstanceState);
            try {
                assertionString = encryptedAssertion.toXMLString(ASSERTION_TO_STRING_INCLUDE_NAMESPACE_PREFIX, ASSERTION_TO_STRING_DECLARE_NAMESPACE_PREFIX);
            } catch (SAML2Exception e) {
                throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                        "Exception caught calling Assertion.toXMLString: " + e, e);
            }
        } else {
            if (saml2Config.encryptAttributes()) {
                encryptAttributeStatement(assertion, saml2Config, stsInstanceState);
            }
            if (saml2Config.encryptNameID()) {
                encryptNameID(assertion, saml2Config, stsInstanceState);
            }
            if (saml2Config.signAssertion()) {
                signAssertion(assertion, stsInstanceState);
            }
            try {
                assertionString =
                        assertion.toXMLString(ASSERTION_TO_STRING_INCLUDE_NAMESPACE_PREFIX, ASSERTION_TO_STRING_DECLARE_NAMESPACE_PREFIX);
            } catch (SAML2Exception e) {
                throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                        "Exception caught calling Assertion.toXMLString: " + e, e);
            }
        }
        if (stsInstanceState.getConfig().persistIssuedTokensInCTS()) {
            try {
                ctsTokenPersistence.persistToken(invocationState.getStsInstanceId(), TokenType.SAML2, assertionString,
                        subjectId, issueInstant.getTime(), saml2Config.getTokenLifetimeInSeconds());
            } catch (CTSTokenPersistenceException e) {
                throw new TokenCreationException(e.getCode(), e.getMessage(), e);
            }
        }
        return assertionString;
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

    private void setIssuer(Assertion assertion, SAML2Config config) throws TokenCreationException {
        final Issuer issuer = AssertionFactory.getInstance().createIssuer();
        try {
            issuer.setValue(config.getIdpId());
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

    @SuppressWarnings("unchecked")
    private void setAttributeStatements(Assertion assertion, SSOToken token, SAML2Config saml2Config) throws TokenCreationException {
        assertion.getAttributeStatements().addAll(
                statementProvider.getAttributeStatementsProvider(saml2Config).get(
                        token, saml2Config, statementProvider.getAttributeMapper(saml2Config))
        );
    }

    @SuppressWarnings("unchecked")
    private void setAuthzDecisionStatements(Assertion assertion, SSOToken token, SAML2Config saml2Config) throws TokenCreationException {
        assertion.getAuthzDecisionStatements().addAll(
                statementProvider.getAuthzDecisionStatementsProvider(saml2Config).get(token, saml2Config));
    }

    /*
    Called only if the entire assertion should be encrypted. Also handles signing, as this must be done before encryption.
    Code modeled after IDPSSOUtil#signAndEncryptResponseComponents
     */
    private EncryptedAssertion handleSingingAndEncryptionOfEntireAssertion(Assertion assertion, SAML2Config saml2Config,
                                                             STSInstanceState stsInstanceState) throws TokenCreationException {
        /*
        Section 6.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf states
        that when the entire assertion is encrypted, the signature must be performed prior to the encryption
        */
        if (saml2Config.signAssertion()) {
            signAssertion(assertion, stsInstanceState);
        }
        try {
            return assertion.encrypt(
                    stsInstanceState.getSAML2CryptoProvider().getSPX509Certificate(saml2Config.getEncryptionKeyAlias()).getPublicKey(),
                    saml2Config.getEncryptionAlgorithm(),
                    saml2Config.getEncryptionAlgorithmStrength(),
                    saml2Config.getSpEntityId());
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception thrown encrypting assertion in SAML2TokenGenerationImpl: " + e, e);
        }

    }

    private void encryptNameID(Assertion assertion, SAML2Config saml2Config, STSInstanceState stsInstanceState) throws TokenCreationException {
        /*
        The null checks below model IDPSSOUtil#signAndEncryptResponseComponents. The Subject and NameID will
        never be null when generated by the DefaultSubjectProvider, but when generated by a custom provider, this
        invariant is not assured.
         */
        Subject subject = assertion.getSubject();
        if (subject == null) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "In SAML2TokenGenerationImpl, saml2Config specifies encryption of NameID, but " +
                            "encapsulating subject is null.");
        }
        NameID nameID = subject.getNameID();
        if (nameID == null) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "In SAML2TokenGenerationImpl, saml2Config specifies encryption of NameID, but " +
                            "NameID in subject is null.");
        }
        try {
            EncryptedID encryptedNameID = nameID.encrypt(
                    stsInstanceState.getSAML2CryptoProvider().getSPX509Certificate(saml2Config.getEncryptionKeyAlias()).getPublicKey(),
                    saml2Config.getEncryptionAlgorithm(),
                    saml2Config.getEncryptionAlgorithmStrength(),
                    saml2Config.getSpEntityId());
            if (encryptedNameID == null) {
                throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                        "In SAML2TokenGenerationImpl, the EncryptedID returned from NameID#encrypt is null.");
            }
            subject.setEncryptedID(encryptedNameID);
            subject.setNameID(null); // reset NameID
            assertion.setSubject(subject);
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception thrown encrypting NameID in SAML2TokenGenerationImpl: " + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void encryptAttributeStatement(Assertion assertion, SAML2Config saml2Config, STSInstanceState stsInstanceState)
            throws TokenCreationException {
        final PublicKey keyEncryptionKey = stsInstanceState.getSAML2CryptoProvider().getSPX509Certificate(saml2Config.getEncryptionKeyAlias()).getPublicKey();
        final String encryptionAlgorithm = saml2Config.getEncryptionAlgorithm();
        final int algorithmStrength = saml2Config.getEncryptionAlgorithmStrength();
        final String spEntityID = saml2Config.getSpEntityId();
        try {
            List<AttributeStatement> originalAttributeStatements = assertion.getAttributeStatements();
            if ((originalAttributeStatements != null) && (originalAttributeStatements.size() > 0)) {
                List<AttributeStatement> encryptedAttributeStatements = new ArrayList<>(originalAttributeStatements.size());
                for (AttributeStatement originalStatement : originalAttributeStatements) {
                    List<Attribute> originalAttributes = originalStatement.getAttribute();
                    if ((originalAttributes == null) || (originalAttributes.size() == 0)) {
                        continue;
                    }
                    List<EncryptedAttribute> encryptedAttributes = new ArrayList<>(originalAttributes.size());
                    for (Attribute originalAttribute : originalAttributes) {
                        EncryptedAttribute encryptedAttribute =
                                originalAttribute.encrypt(
                                        keyEncryptionKey,
                                        encryptionAlgorithm,
                                        algorithmStrength,
                                        spEntityID);
                        if (encryptedAttribute == null) {
                            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "In SAML2TokenGenerationImpl, " +
                                    "attribute encryption invocation returned null.");
                        }
                        encryptedAttributes.add(encryptedAttribute);
                    }
                    originalStatement.setEncryptedAttribute(encryptedAttributes);
                    originalStatement.setAttribute(Collections.EMPTY_LIST);
                    encryptedAttributeStatements.add(originalStatement);
                }
                assertion.setAttributeStatements(encryptedAttributeStatements);
            }
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "In SAML2TokenGenerationImpl, exception " +
                    "caught encrypting assertion attributes: " + e, e);
        }
    }

    private void signAssertion(Assertion assertion, STSInstanceState instanceState) throws TokenCreationException {
        final SAML2CryptoProvider saml2CryptoProvider = instanceState.getSAML2CryptoProvider();
        final SAML2Config saml2Config = instanceState.getConfig().getSaml2Config();
        String signatureKeyPassword;
        try {
            signatureKeyPassword = new String(saml2Config.getSignatureKeyPassword(), AMSTSConstants.UTF_8_CHARSET_ID);
        } catch (UnsupportedEncodingException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Could not obtain string representation of signature key password in SAML2TokenGenerationImpl: ");
        }
        /*
        Note: the cert alias and private-key alias are the same. If there is a key entry in the keystore, it seems like
        they are represented by the same alias.
         */
        PrivateKey privateKey = saml2CryptoProvider.getIDPPrivateKey(saml2Config.getSignatureKeyAlias(), signatureKeyPassword);
        try {
            assertion.sign(privateKey, saml2CryptoProvider.getIDPX509Certificate(saml2Config.getSignatureKeyAlias()));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught signing assertion in SAML2TokenGenerationImpl: " + e, e);
        }
    }
}
