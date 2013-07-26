/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

/**
 * Portions copyright 2012-2013 ForgeRock AS
 */

package org.forgerock.restlet.ext.oauth2.flow;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.*;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements a SAML 2.0 Flow. This is an Extension grant.
 *
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.5">4.5.  Extension Grants</a>
 */
public class SAML20BearerServerResource extends AbstractFlow {

    /*
     * 2.1. Using SAML20BearerServerResource Assertions as Authorization Grants
     * 
     * To use a SAML20BearerServerResource Bearer Assertion as an authorization
     * grant, use the following parameter values and encodings.
     * 
     * The value of "grant_type" parameter MUST be
     * "urn:ietf:params:oauth:grant-type:saml2-bearer"
     * 
     * The value of the "assertion" parameter MUST contain a single
     * SAML20BearerServerResource 2.0 Assertion. The SAML20BearerServerResource
     * Assertion XML data MUST be encoded using base64url, where the encoding
     * adheres to the definition in Section 5 of RFC4648 [RFC4648] and where the
     * padding bits are set to zero. To avoid the need for subsequent encoding
     * steps (by "application/ x-www-form-urlencoded"
     * [W3C.REC-html401-19991224], for example), the base64url encoded data
     * SHOULD NOT be line wrapped and pad characters ("=") SHOULD NOT be
     * included.
     */

    /*
     * 2.2. Using SAML20BearerServerResource Assertions for Client
     * Authentication
     * 
     * To use a SAML20BearerServerResource Bearer Assertion for client
     * authentication grant, use the following parameter values and encodings.
     * 
     * 
     * The value of "client_assertion_type" parameter MUST be
     * "urn:ietf:params:oauth:client-assertion-type:saml2-bearer"
     * 
     * The value of the "client_assertion" parameter MUST contain a single
     * SAML20BearerServerResource 2.0 Assertion. The SAML20BearerServerResource
     * Assertion XML data MUST be encoded using base64url, where the encoding
     * adheres to the definition in Section 5 of RFC4648 [RFC4648] and where the
     * padding bits are set to zero. To avoid the need for subsequent encoding
     * steps (by "application/x-www-form-urlencoded" [W3C.REC-html401-19991224],
     * for example), the base64url encoded data SHOULD NOT be line wrapped and
     * pad characters ("=") SHOULD NOT be included.
     */

    @Post("form:json")
    public Representation represent(Representation entity) {
        client = validateRemoteClient();

        String assertion = OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.SAML20.ASSERTION, String.class);
        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.represent(): Assertion:\n" + assertion);

        byte[] decodedAsertion = Base64.decode(assertion.replace(" ", "+"));
        if (decodedAsertion == null) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.represent(): Decoding assertion failed\nassertion:" + assertion);
        }
        String finalAssertion = new String(decodedAsertion);
        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.represent(): Decoded assertion:\n" + finalAssertion);

        Assertion assertionObject;
        boolean valid = false;
        try {
            AssertionFactory factory = AssertionFactory.getInstance();
            assertionObject = factory.createAssertion(finalAssertion);
            valid = validAssertion(assertionObject);
        } catch (SAML2Exception e) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.represent(): Error parsing assertion", e);
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(),
                    "Assertion is invalid.");
        }

        if (!valid) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.represent(): Error parsing assertion");
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(),
                    "Assertion is invalid.");
        }

        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.represent(): Assertion is valid");

        String scope_before =
                OAuth2Utils
                        .getRequestParameter(getRequest(), OAuth2Constants.Params.SCOPE, String.class);
        Set<String> checkedScope = executeAccessTokenScopePlugin(scope_before);
        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.represent(): Granting scope: " + checkedScope.toString());


        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.represent(): Creating token with data: " +
                client.getClient().getAccessTokenType() + "\n" +
                checkedScope.toString() + "\n" +
                OAuth2Utils.getRealm(getRequest()) + "\n" +
                assertionObject.getSubject().getNameID().getValue() + "\n" +
                client.getClient().getClientId());
        CoreToken token = getTokenStore().createAccessToken(client.getClient().getAccessTokenType(), checkedScope,
                OAuth2Utils.getRealm(getRequest()), assertionObject.getSubject().getNameID().getValue(),
                client.getClient().getClientId(), null, null, null);
        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.represent(): Token created: " + token.toString());

        Map<String, Object> response = token.convertToMap();

        if (checkedScope != null && !checkedScope.isEmpty()) {
            response.put(OAuth2Constants.Params.SCOPE, OAuth2Utils.join(checkedScope,
                    OAuth2Utils.getScopeDelimiter(getContext())));
        }
        return new JacksonRepresentation<Map>(response);
    }

    private boolean validAssertion(Assertion assertion) throws SAML2Exception {
        //must contain issuer
        Issuer issuer = assertion.getIssuer();
        if (issuer == null) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Issuer does not exist");
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

        Conditions conditions = assertion.getConditions();
        if (conditions == null) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Conditions does not exist");
            return false;
        }
        List<AudienceRestriction> audienceRestriction = conditions.getAudienceRestrictions();
        if (audienceRestriction == null || audienceRestriction.isEmpty()) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Audience Restriction does not exist");
            return false;
        }
        boolean found = false;
        //String oauthTokenURL = OAuth2Utils.getDeploymentURL(getRequest());
        String deploymentURL = OAuth2Utils.getDeploymentURL(getRequest());
        OAuth2Utils.DEBUG.message("SAML20BearerServerResource.validAssertion(): URL of authorization server: " + deploymentURL);
        for (AudienceRestriction restriction : audienceRestriction) {
            List<String> audiences = restriction.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                continue;
            }
            for (String audience : audiences) {
                //TODO ADD service provider SAML entity of its controlling domain
                //check for the url with and without trailing /
                if (deploymentURL.endsWith("/")) {
                    deploymentURL = deploymentURL.substring(0, deploymentURL.length() - 1);
                }
                if (audience.endsWith("/")) {
                    audience = audience.substring(0, audience.length() - 1);
                }
                if (audience.equalsIgnoreCase(deploymentURL)) {
                    found = true;
                }
            }
        }
        if (found == false) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Didnt find the oauth2 provider in" +
                    "audience restrictions");
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
        Subject subject = assertion.getSubject();
        if (subject == null) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Subject does not exist");
            return false;
        }

        String resourceOwner = subject.getNameID().getValue();

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
        List<SubjectConfirmation> subjectConfirmations = subject.getSubjectConfirmation();

        found = false;
        if (subjectConfirmations == null || subjectConfirmations.isEmpty()) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Subject Confirmations does not exist");
            return false;
        }
        //if conditions is expired assertion is expired
        if (!assertion.isTimeValid()) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Assertion expired");
            return false;
        } else {
            found = true;
        }
        for (SubjectConfirmation subjectConfirmation : subjectConfirmations) {
            if (subjectConfirmation.getMethod() == null) {
                continue;
            }
            if (subjectConfirmation.getMethod().equalsIgnoreCase(OAuth2Constants.SAML20.SUBJECT_CONFIRMATION_METHOD)) {
                SubjectConfirmationData subjectConfirmationData = subjectConfirmation.getSubjectConfirmationData();
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
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Assertion expired or subject expired");
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
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Assertion must be signed");
            return false;
        }
        if (!SAMLUtils.checkSignatureValid(
                assertion.toXMLString(), "ID", issuer.getValue())) {
            OAuth2Utils.DEBUG.error("SAML20BearerServerResource.validAssertion(): Assertion signature verification failed");
            return false;
        }
        return true;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.SAML20.ASSERTION,
                OAuth2Constants.Params.CLIENT_ID};
    }
}
