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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.marshal;

import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.WSConstants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPrincipal;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.SAML2TokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller
 */
public class TokenRequestMarshallerImpl implements TokenRequestMarshaller {
    private final XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller;
    private final XmlMarshaller<OpenIdConnectIdToken> openIdConnectXmlMarshaller;

    @Inject
    TokenRequestMarshallerImpl(XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller,
                               XmlMarshaller<OpenIdConnectIdToken> openIdConnectXmlMarshaller) {
        this.amSessionTokenXmlMarshaller = amSessionTokenXmlMarshaller;
        this.openIdConnectXmlMarshaller = openIdConnectXmlMarshaller;
    }

    public ReceivedToken marshallInputToken(JsonValue receivedToken) throws TokenMarshalException {
        Map<String,Object> tokenAsMap = receivedToken.asMap();
        String tokenType = (String)tokenAsMap.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (tokenType == null) {
            String message = "The to-be-translated token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        if (TokenType.USERNAME.name().equals(tokenType)) {
            return marshallUsernameToken(tokenAsMap);
        } else if (TokenType.OPENAM.name().equals(tokenType)) {
            return marshallAMSessionToken(tokenAsMap);
        } else if (TokenType.OPENIDCONNECT.name().equals(tokenType)) {
            return marshallOpenIdConnectIdToken(tokenAsMap);
        }

        throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                "Unsupported token translation operation for token: " + receivedToken);

    }

    public TokenType getTokenType(JsonValue receivedToken) throws TokenMarshalException {
        JsonValue jsonTokenType = receivedToken.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (jsonTokenType.isNull() || !jsonTokenType.isString()) {
            String message = "REST STS invocation does not contain " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " String entry. The json token: " + receivedToken;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        try {
            return TokenType.valueOf(jsonTokenType.asString());
        } catch (IllegalArgumentException e) {
            String message = "Error marshalling from " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " value to token-type enum. The json token: " + receivedToken + " The exception: " + e;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } catch (NullPointerException e) {
            String message = "Error marshalling from " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " value to token-type enum. The json token: " + receivedToken + " The exception: " + e;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
    }

    public SAML2SubjectConfirmation getSubjectConfirmation(JsonValue token) throws TokenMarshalException {
        try {
            SAML2TokenState tokenState = SAML2TokenState.fromJson(token);
            return tokenState.getSubjectConfirmation();
        } catch (TokenMarshalException e) {
            /*
            Try to get the value directly
             */
            String subjectConfirmationString = token.get(SAML2TokenState.SUBJECT_CONFIRMATION).asString();
            try {
                return SAML2SubjectConfirmation.valueOf(subjectConfirmationString);
            } catch (IllegalArgumentException iae) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "Invalid subjectConfirmation specified in the JsonValue corresponding to SAML2TokenState. " +
                                "The JsonValue: " + token.toString());
            } catch (NullPointerException npe) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "No subjectConfirmation specified in the JsonValue corresponding to SAML2TokenState. " +
                                "The JsonValue: " + token.toString());
            }
        }
    }

    public String getServiceProviderAssertionConsumerServiceUrl(JsonValue token) throws TokenMarshalException {
        try {
            SAML2TokenState tokenState = SAML2TokenState.fromJson(token);
            return tokenState.getServiceProviderAssertionConsumerServiceUrl();
        } catch (TokenMarshalException e) {
            /*
            Try to get the value directly
             */
            JsonValue acsUrl = token.get(SAML2TokenState.SP_ACS_URL);
            if (acsUrl.isNull()) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "No SP Assertion Consumer Service url specified in the JsonValue corresponding to SAML2TokenState. " +
                                "The JsonValue: " + token.toString());
            }
            return acsUrl.asString();
        }
    }

    public ProofTokenState getProofTokenState(JsonValue token) throws TokenMarshalException {
        final SAML2TokenState tokenState = SAML2TokenState.fromJson(token);
        final ProofTokenState proofTokenState = tokenState.getProofTokenState();
        if (proofTokenState ==  null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "No ProofTokenState specified in the" +
                    " SAML2TokenState. The JsonValue: " + token);
        } else {
            return proofTokenState;
        }
    }

    private ReceivedToken marshallUsernameToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String tokenUserName = (String)tokenAsMap.get(AMSTSConstants.USERNAME_TOKEN_USERNAME);
        if (tokenUserName == null) {
            String message = "Exception: json representation of UNT does not contain a username field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }
        String password = (String)tokenAsMap.get(AMSTSConstants.USERNAME_TOKEN_PASSWORD);
        if (password == null) {
            String message = "Exception: json representation of UNT does not contain a password field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        }

        UsernameTokenType usernameTokenType = new UsernameTokenType();
        AttributedString usernameAttributedString = new AttributedString();
        usernameAttributedString.setValue(tokenUserName);
        usernameTokenType.setUsername(usernameAttributedString);
        PasswordString passwordString = new PasswordString();
        passwordString.setValue(password);
        passwordString.setType(WSConstants.PASSWORD_TEXT);
        JAXBElement <PasswordString> passwordType =
                new JAXBElement<PasswordString>(QNameConstants.PASSWORD, PasswordString.class, passwordString);
        usernameTokenType.getAny().add(passwordType);
        JAXBElement<UsernameTokenType> jaxbUsernameTokenType =
                new JAXBElement<UsernameTokenType>(QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);
        /*
        TODO - setting the nonce? If I don't set it, subsequent requests will fail because my validator will not be called
        (as the token is found in the cache), and thus the interim OpenAM token is not stored to be used to define the
        principal in the issued token. At the moment, I am not setting the TokenStore in the TokenValidatorParameters, so
        this solves the problem. But I have to answer larger questions like:
        1. are there some tokens I don't want to store, so that their validation can be resolved at a caching layer? Caching tokens
        is full of headaches like making sure that the token lifetime in the STS TokenStore matches that of any token/session state
        generated by the ultimate authority for this token. The bottom line is that caching tokens in the STS TokenStore should be
        the exception rather than the rule - i.e. it makes sense for SCT instances, as otherwise, there is no way to validate them,
        but less so for UNTs.
        2. Should the REST-STS accept UNTs without a nonce or origination instant? Without a unique bit of entropy, UNT instances
        with the same <username,password> value will hash to the same entry in the STS TokenStore. But if UNTs are not cached, it
        might not matter. The bottom line is whether the REST-STS should prevent the replay of token transformations - so perhaps each
        input token type in supported transformations should be associated with a token lifetime, but if we are taking UNTs in the clear,
        what's the point (i.e. the timestamp could just be updated). I imagine that we will ultimately just take a UNT in a JWT enveloped
        in JWS/JWE.
         */
        ReceivedToken token = new ReceivedToken(jaxbUsernameTokenType);
        token.setState(ReceivedToken.STATE.NONE);
        token.setPrincipal(new STSPrincipal(tokenUserName));
        token.setUsernameToken(true);
        return token;
    }

    private ReceivedToken marshallAMSessionToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String sessionId = (String)tokenAsMap.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID);
        if (sessionId == null) {
            String message = "Exception: json representation of AM Session Token does not contain a session_id field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            ReceivedToken token = new ReceivedToken(amSessionTokenXmlMarshaller.toXml(new OpenAMSessionToken(sessionId)));
            token.setState(ReceivedToken.STATE.NONE);
            token.setUsernameToken(false);
            return token;
        }
    }

    private ReceivedToken marshallOpenIdConnectIdToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String tokenValue = (String)tokenAsMap.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY);
        if (tokenValue == null) {
            String message = "Exception: json representation of Open ID Connect ID Token does not contain a "
                    + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY + " field. The representation: " + tokenAsMap;
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, message);
        } else {
            ReceivedToken token = new ReceivedToken(openIdConnectXmlMarshaller.toXml(new OpenIdConnectIdToken(tokenValue)));
            token.setState(ReceivedToken.STATE.NONE);
            token.setUsernameToken(false);
            return token;
        }
    }
}
