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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2.saml2.core;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Exception;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientCredentials;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * @since 12.0.0
 */
public class Saml2GrantTypeHandler implements GrantTypeHandler<Saml2AccessTokenRequest> {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final ClientAuthenticator clientAuthenticator;
    private final ScopeValidator scopeValidator;
    private final TokenStore tokenStore;

    @Inject
    public Saml2GrantTypeHandler(final ClientAuthenticator clientAuthenticator, final ScopeValidator scopeValidator,
            final TokenStore tokenStore) {
        this.clientAuthenticator = clientAuthenticator;
        this.scopeValidator = scopeValidator;
        this.tokenStore = tokenStore;
    }

    public AccessToken handle(final Saml2AccessTokenRequest accessTokenRequest) throws InvalidGrantException,
            InvalidClientException {

        final ClientCredentials clientCredentials = accessTokenRequest.getClientCredentials();

        final ClientRegistration clientRegistration = clientAuthenticator.authenticate(clientCredentials,
                accessTokenRequest.getContext());

        final String assertion = accessTokenRequest.getAssertion();
        logger.trace("Assertion:\n" + assertion);

        final byte[] decodedAssertion = Base64.decode(assertion.replace(" ", "+"));
        if (decodedAssertion == null) {
            logger.error("Decoding assertion failed\nassertion:" + assertion);
        }
        final String finalAssertion = new String(decodedAssertion);
        logger.trace("Decoded assertion:\n" + finalAssertion);

        final Assertion assertionObject;
        final boolean valid;
        try {
            final AssertionFactory factory = AssertionFactory.getInstance();
            assertionObject = factory.createAssertion(finalAssertion);
            valid = validAssertion(assertionObject, (String) accessTokenRequest.getContext().get("deploymentURL"));
        } catch (SAML2Exception e) {
            logger.error("Error parsing assertion", e);
            throw new InvalidGrantException("Assertion is invalid", e);
        }

        if (!valid) {
            logger.error("Error parsing assertion");
            throw new InvalidGrantException("Assertion is invalid.");
        }

        logger.trace("Assertion is valid");

        final Set<String> scope = accessTokenRequest.getScope();
        final Set<String> validatedScope = scopeValidator.validateAccessTokenScope(clientRegistration, scope,
                accessTokenRequest.getContext());
        logger.trace("Granting scope: " + validatedScope.toString());

        logger.trace("Creating token with data: " + clientRegistration.getAccessTokenType() + "\n"
                + validatedScope.toString() + "\n" + accessTokenRequest.getContext().get("realm") + "\n"
                + assertionObject.getSubject().getNameID().getValue() + "\n" + clientRegistration.getClientId());

        final AccessToken accessToken = tokenStore.createAccessToken(accessTokenRequest.getGrantType(),
                assertionObject.getSubject().getNameID().getValue(), clientRegistration, validatedScope, null,
                accessTokenRequest.getContext());
        logger.trace("Token created: " + accessToken.getCoreToken().toString());

        if (validatedScope != null && !validatedScope.isEmpty()) {
            accessToken.add("scope", joinScope(validatedScope));
        }

        return accessToken;
    }

    private boolean validAssertion(final Assertion assertion, final String deploymentURL) throws SAML2Exception {
        //must contain issuer
        final Issuer issuer = assertion.getIssuer();
        if (issuer == null) {
            logger.error("Issuer does not exist");
            return false;
        }

        /**
         * The Assertion MUST contain <Conditions> element with an
         * <AudienceRestriction> element with an <Audience> element
         * containing a URI reference that identifies the authorization
         * server, or the service provider SAML entity of its controlling
         * domain, as an intended audience.  The token endpoint URL of the
         * authorization server MAY be used as an acceptable value for an
         *       <Audience> element.  The authorization server MUST verify that it
         * is an intended audience for the Assertion.
         *
         */

        final Conditions conditions = assertion.getConditions();
        if (conditions == null) {
            logger.error("Saml2BearerServerResource.validAssertion(): Conditions does not exist");
            return false;
        }
        final List<AudienceRestriction> audienceRestriction = conditions.getAudienceRestrictions();
        if (audienceRestriction == null || audienceRestriction.isEmpty()) {
            logger.error("Saml2BearerServerResource.validAssertion(): Audience Restriction does not exist");
            return false;
        }
        boolean found = false;
        logger.trace("Saml2BearerServerResource.validAssertion(): URL of authorization server: " + deploymentURL);
        for (final AudienceRestriction restriction : audienceRestriction) {
            final List<String> audiences = restriction.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                continue;
            }
            for (final String audience : audiences) {
                String deployURL = deploymentURL;
                String aud = audience;
                //TODO ADD service provider SAML entity of its controlling domain
                //check for the url with and without trailing /
                if (deployURL.endsWith("/")) {
                    deployURL = deploymentURL.substring(0, deployURL.length() - 1);
                }
                if (aud.endsWith("/")) {
                    aud = aud.substring(0, aud.length() - 1);
                }
                if (aud.equalsIgnoreCase(deployURL)) {
                    found = true;
                }
            }
        }
        if (found == false) {
            logger.error("Didn't find the oauth2 provider in audience restrictions");
            return false;
        }

        /**
         * The Assertion MUST contain a <Subject> element.  The subject MAY
         * identify the resource owner for whom the access token is being
         * requested.  For client authentication, the Subject MUST be the
         * "client_id" of the OAuth client.  When using an Assertion as an
         * authorization grant, the Subject SHOULD identify an authorized
         * accessor for whom the access token is being requested (typically
         * the resource owner, or an authorized delegate).  Additional
         * information identifying the subject/principal of the transaction
         * MAY be included in an <AttributeStatement>.
         */
        final Subject subject = assertion.getSubject();
        if (subject == null) {
            logger.error("Subject does not exist");
            return false;
        }

        final String resourceOwner = subject.getNameID().getValue();

        /**
         * The Assertion MUST have an expiry that limits the time window
         * during which it can be used.  The expiry can be expressed either
         * as the NotOnOrAfter attribute of the <Conditions> element or as
         * the NotOnOrAfter attribute of a suitable <SubjectConfirmationData>
         * element.
         */

        /**
         * The <Subject> element MUST contain at least one
         * <SubjectConfirmation> element that allows the authorization server
         * to confirm it as a Bearer Assertion.  Such a <SubjectConfirmation>
         * element MUST have a Method attribute with a value of
         * "urn:oasis:names:tc:SAML:2.0:cm:bearer".  The
         * <SubjectConfirmation> element MUST contain a
         * <SubjectConfirmationData> element, unless the Assertion has a
         * suitable NotOnOrAfter attribute on the <Conditions> element, in
         * which case the <SubjectConfirmationData> element MAY be omitted.
         * When present, the <SubjectConfirmationData> element MUST have a
         * Recipient attribute with a value indicating the token endpoint URL
         * of the authorization server (or an acceptable alias).  The
         * authorization server MUST verify that the value of the Recipient
         * attribute matches the token endpoint URL (or an acceptable alias)
         * to which the Assertion was delivered.  The
         * <SubjectConfirmationData> element MUST have a NotOnOrAfter
         * attribute that limits the window during which the Assertion can be
         * confirmed.  The <SubjectConfirmationData> element MAY also contain
         * an Address attribute limiting the client address from which the
         * Assertion can be delivered.  Verification of the Address is at the
         * discretion of the authorization server.
         */
        final List<SubjectConfirmation> subjectConfirmations = subject.getSubjectConfirmation();

        found = false;
        if (subjectConfirmations == null || subjectConfirmations.isEmpty()) {
            logger.error("Subject Confirmations does not exist");
            return false;
        }
        //if conditions is expired assertion is expired
        if (!assertion.isTimeValid()) {
            logger.error("Assertion expired");
            return false;
        } else {
            found = true;
        }
        for (final SubjectConfirmation subjectConfirmation : subjectConfirmations) {
            if (subjectConfirmation.getMethod() == null) {
                continue;
            }
            if (subjectConfirmation.getMethod().equalsIgnoreCase(OAuth2Constants.SAML20.SUBJECT_CONFIRMATION_METHOD)) {
                final SubjectConfirmationData subjectConfirmationData = subjectConfirmation.getSubjectConfirmationData();
                if (subjectConfirmationData == null) {
                    continue;
                } else if (subjectConfirmationData.getNotOnOrAfter().before(new Date())
                        && subjectConfirmationData.getRecipient().equalsIgnoreCase(deploymentURL)) {
                    found = true;
                }
                //TODO check Client Address
            }
        }

        if (!found) {
            logger.error("Assertion expired or subject expired");
            return false;
        }

        /**
         * The authorization server MUST verify that the NotOnOrAfter instant
         * has not passed, subject to allowable clock skew between systems.
         * An invalid NotOnOrAfter instant on the <Conditions> element
         * invalidates the entire Assertion.  An invalid NotOnOrAfter instant
         * on a <SubjectConfirmationData> element only invalidates the
         * individual <SubjectConfirmation>.  The authorization server MAY
         * reject Assertions with a NotOnOrAfter instant that is unreasonably
         * far in the future.  The authorization server MAY ensure that
         * Bearer Assertions are not replayed, by maintaining the set of used
         * ID values for the length of time for which the Assertion would be
         * considered valid based on the applicable NotOnOrAfter instant.
         *
         * If the Assertion issuer authenticated the subject, the Assertion
         * SHOULD contain a single <AuthnStatement> representing that
         * authentication event.
         *
         * If the Assertion was issued with the intention that the presenter
         * act autonomously on behalf of the subject, an <AuthnStatement>
         * SHOULD NOT be included.  The presenter SHOULD be identified in the
         * <NameID> or similar element, the <SubjectConfirmation> element, or
         * by other available means like [OASIS.saml-deleg-cs].
         *
         * Other statements, in particular <AttributeStatement> elements, MAY
         * be included in the Assertion.
         */

        /**
         * The Assertion MUST be digitally signed by the issuer and the
         * authorization server MUST verify the signature.
         */

        if (!assertion.isSigned()) {
            logger.error("Assertion must be signed");
            return false;
        }
        if (!SAMLUtils.checkSignatureValid(assertion.toXMLString(), "ID", issuer.getValue())) {
            logger.error("Assertion signature verification failed");
            return false;
        }
        return true;
    }
}
