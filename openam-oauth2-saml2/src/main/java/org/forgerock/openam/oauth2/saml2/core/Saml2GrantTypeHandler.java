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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.oauth2.saml2.core;

import static org.forgerock.oauth2.core.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.SCOPE;
import static org.forgerock.oauth2.core.Utils.*;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaManager;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.util.Reject;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * @since 12.0.0
 */
public class Saml2GrantTypeHandler extends GrantTypeHandler {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final ClientRegistrationStore clientRegistrationStore;
    private final TokenStore tokenStore;

    @Inject
    public Saml2GrantTypeHandler(ClientRegistrationStore clientRegistrationStore, TokenStore tokenStore,
            OAuth2UrisFactory urisFactory, OAuth2ProviderSettingsFactory providerSettingsFactory,
            ClientAuthenticator clientAuthenticator) {
        super(providerSettingsFactory, urisFactory, clientAuthenticator);
        this.clientRegistrationStore = clientRegistrationStore;
        this.tokenStore = tokenStore;
    }

    public AccessToken handle(OAuth2Request request) throws InvalidGrantException, InvalidClientException,
            InvalidRequestException, ServerException, InvalidScopeException, NotFoundException {
        String clientId = request.getParameter(OAuth2Constants.Params.CLIENT_ID);
        Reject.ifTrue(isEmpty(clientId), "Missing parameter, 'client_id'");

        ClientRegistration clientRegistration = null;
        if (request.<String>getParameter(OAuth2Constants.Params.CLIENT_SECRET) != null) {
            final OAuth2Uris uris = urisFactory.get(request);
            clientRegistration = clientAuthenticator.authenticate(request, uris.getTokenEndpoint());
        }

        final String assertionString = request.getParameter(OAuth2Constants.SAML20.ASSERTION);
        Reject.ifTrue(isEmpty(assertionString), "Missing parameter, 'assertion'");
        logger.trace("Assertion:\n{}", assertionString);
        final byte[] decodedAssertion = Base64url.decode(assertionString);
        if (decodedAssertion == null) {
            logger.error("Decoding assertion failed\nassertion:{}", assertionString);
        }
        final String finalAssertion = new String(decodedAssertion, StandardCharsets.UTF_8);
        logger.trace("Decoded assertion:\n{}", finalAssertion);

        String realm = normaliseRealm(request.<String>getParameter(OAuth2Constants.Params.REALM));
        if (clientRegistration == null) {
            clientRegistration = clientRegistrationStore.get(clientId, request);
        }

        final Assertion assertion;
        try {
            final AssertionFactory factory = AssertionFactory.getInstance();
            assertion = factory.createAssertion(finalAssertion);
            validateAssertion(assertion, clientRegistration, realm);
        } catch (SAML2Exception e) {
            logger.error("An error occurred while validating the assertion", e);
            throw new InvalidGrantException("Assertion is invalid.");
        }

        logger.trace("Assertion is valid");

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String validatedClaims = providerSettings.validateRequestedClaims(
                (String) request.getParameter(OAuth2Constants.Custom.CLAIMS));
        final String grantType = request.getParameter(OAuth2Constants.Params.GRANT_TYPE);
        final Set<String> scope = splitScope(request.<String>getParameter(OAuth2Constants.Params.SCOPE));
        final Set<String> validatedScope = providerSettings.validateAccessTokenScope(clientRegistration, scope,
                request);
        logger.trace("Granting scope: {}", validatedScope.toString());

        logger.trace("Creating token with data: {}\n{}\n{}\n{}\n{}", clientRegistration.getAccessTokenType(),
                validatedScope.toString(), realm, assertion.getSubject().getNameID().getValue(),
                clientRegistration.getClientId());

        final AccessToken accessToken = tokenStore.createAccessToken(grantType, BEARER, null,
                assertion.getSubject().getNameID().getValue(), clientRegistration.getClientId(), null,
                validatedScope, null, null, validatedClaims, request);
        logger.trace("Token created: {}", accessToken.toString());

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.addExtraData(SCOPE, joinScope(validatedScope));
        }

        tokenStore.updateAccessToken(accessToken);

        return accessToken;
    }

    @Override
    protected AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) {
        throw new UnsupportedOperationException();
    }

    private String normaliseRealm(String realm) {
        if (realm == null) {
            return "/";
        }
        return realm;
    }

    private void validateAssertion(Assertion assertion, ClientRegistration clientRegistration, String realm)
            throws SAML2Exception, InvalidGrantException {
        // The Assertion's <Issuer> element MUST contain a unique identifier for the entity that issued the Assertion.
        final Issuer issuer = assertion.getIssuer();
        if (issuer == null) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Assertion's Issuer field is not specified");
            throw new InvalidGrantException("Issuer is not specified");
        }
        final String idpEntityID = issuer.getValue();

        final Set<X509Certificate> verificationCerts;
        SAML2MetaManager metaManager = new SAML2MetaManager();
        final IDPSSODescriptorElement idpSsoDescriptor = metaManager.getIDPSSODescriptor(realm, idpEntityID);
        verificationCerts = KeyUtil.getVerificationCerts(idpSsoDescriptor, idpEntityID, SAML2Constants.IDP_ROLE);


        // The Assertion MUST be digitally signed or have a Message Authentication Code (MAC) applied by the issuer.
        if (!assertion.isSigned()) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Assertion is not signed");
            throw new InvalidGrantException("Assertion is not signed");
        }

        // The authorization server MUST reject Assertions with an invalid signature or MAC.
        if (!assertion.isSignatureValid(verificationCerts)) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Assertion signature verification failed");
            throw new InvalidGrantException("Assertion signature is not valid");
        }
        logger.trace("Saml2GrantTypeHandler.isValidAssertion(): Assertion signature validation was successful");

        /*
         * The Assertion MUST contain <Conditions> element with an <AudienceRestriction> element with an <Audience>
         * element containing a URI reference that identifies the authorization server, or the service provider SAML
         * entity of its controlling domain, as an intended audience. The token endpoint URL of the authorization
         * server MAY be used as an acceptable value for an <Audience> element. The authorization server MUST verify
         * that it is an intended audience for the Assertion.
         */
        final Conditions conditions = assertion.getConditions();
        if (conditions == null) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Conditions does not exist");
            throw new InvalidGrantException("Conditions element is missing");
        }
        final List<AudienceRestriction> audienceRestriction = conditions.getAudienceRestrictions();
        if (audienceRestriction == null || audienceRestriction.isEmpty()) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Audience Restriction does not exist");
            throw new InvalidGrantException("AudienceRestriction is missing");
        }

        boolean found = false;
        SPSSODescriptorElement spSsoDescriptor = null;

        for (final AudienceRestriction restriction : audienceRestriction) {
            final List<String> audiences = restriction.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                continue;
            }
            for (final String audience : audiences) {
                spSsoDescriptor = metaManager.getSPSSODescriptor(realm, audience);
                if (spSsoDescriptor != null && SAML2Utils.isSourceSiteValid(issuer, realm, audience)) {
                    found = true;
                }
            }
        }
        if (!found) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Didn't find the Oauth2 provider in audience "
                    + "restrictions");
            throw new InvalidGrantException("Audience validation failed");
        }

        /*
         * The Assertion MUST contain a <Subject> element. The subject MAY identify the resource owner for whom the
         * access token is being requested. For client authentication, the Subject MUST be the "client_id" of the OAuth
         * client. When using an Assertion as an authorization grant, the Subject SHOULD identify an authorized accessor
         * for whom the access token is being requested (typically the resource owner, or an authorized delegate).
         * Additional information identifying the subject/principal of the transaction MAY be included in an
         * <AttributeStatement>.
         */
        final Subject subject = assertion.getSubject();
        if (subject == null) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Subject is not specified in the assertion");
            throw new InvalidGrantException("Subject is not specified");
        }

        /*
         * The Assertion MUST have an expiry that limits the time window during which it can be used. The expiry can be
         * expressed either as the NotOnOrAfter attribute of the <Conditions> element or as the NotOnOrAfter attribute
         * of a suitable <SubjectConfirmationData> element.
         *
         * The <Subject> element MUST contain at least one <SubjectConfirmation> element that allows the authorization
         * server to confirm it as a Bearer Assertion. Such a <SubjectConfirmation> element MUST have a Method attribute
         * with a value of "urn:oasis:names:tc:SAML:2.0:cm:bearer". The <SubjectConfirmation> element MUST contain a
         * <SubjectConfirmationData> element, unless the Assertion has a suitable NotOnOrAfter attribute on the
         * <Conditions> element, in which case the <SubjectConfirmationData> element MAY be omitted. When present, the
         * <SubjectConfirmationData> element MUST have a Recipient attribute with a value indicating the token endpoint
         * URL of the authorization server (or an acceptable alias). The authorization server MUST verify that the value
         * of the Recipient attribute matches the token endpoint URL (or an acceptable alias) to which the Assertion was
         * delivered. The <SubjectConfirmationData> element MUST have a NotOnOrAfter attribute that limits the window
         * during which the Assertion can be confirmed. The <SubjectConfirmationData> element MAY also contain an
         * Address attribute limiting the client address from which the Assertion can be delivered. Verification of the
         * Address is at the discretion of the authorization server.
         */
        final List<SubjectConfirmation> subjectConfirmations = subject.getSubjectConfirmation();

        found = false;
        if (subjectConfirmations == null || subjectConfirmations.isEmpty()) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Subject Confirmations is not specified in the "
                    + "assertion");
            throw new InvalidGrantException("SubjectConfirmations element is missing");
        }

        // Check the time Conditions on the Assertion object
        if (!assertion.isTimeValid()) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): Assertion expired");
            throw new InvalidGrantException("Assertion expired");
        }

        for (final SubjectConfirmation subjectConfirmation : subjectConfirmations) {
            if (OAuth2Constants.SAML20.SUBJECT_CONFIRMATION_METHOD.equalsIgnoreCase(subjectConfirmation.getMethod())) {
                final SubjectConfirmationData subjectConfirmationData =
                        subjectConfirmation.getSubjectConfirmationData();
                if (subjectConfirmationData == null) {
                    continue;
                } else {
                    SAML2Utils.validateRecipient(spSsoDescriptor, assertion.getID(), subjectConfirmationData);
                    final Date notOnOrAfter = subjectConfirmationData.getNotOnOrAfter();
                    if (notOnOrAfter == null) {
                        logger.error("Saml2GrantTypeHandler.isValidAssertion(): Required NotOnOrAfter field is "
                                + "missing from the SubjectConfirmationData");
                    } else if (newDate().before(notOnOrAfter)) {
                        found = true;
                    }
                }
                //TODO check Client Address
            }
        }

        if (!found) {
            logger.error("Saml2GrantTypeHandler.isValidAssertion(): The assertion is either expired or had no "
                    + "expiration info");
            throw new InvalidGrantException("Assertion either expired or had no expiration information");
        }
    }
}
