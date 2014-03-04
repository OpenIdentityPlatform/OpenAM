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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPrincipal;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.token.provider.OpenAMSessionIdElementBuilder;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;

/**
 */
public class TokenRequestMarshallerImpl implements TokenRequestMarshaller {
    private final OpenAMSessionIdElementBuilder openAMSessionIdElementBuilder;
    private final Logger logger;

    @Inject
    TokenRequestMarshallerImpl(OpenAMSessionIdElementBuilder openAMSessionIdElementBuilder, Logger logger) {
        this.openAMSessionIdElementBuilder = openAMSessionIdElementBuilder;
        this.logger = logger;
    }

    @Override
    public ReceivedToken marshallTokenRequest(JsonValue receivedToken) throws TokenMarshalException {
        Map<String,Object> tokenAsMap = receivedToken.asMap();
        String tokenType = (String)tokenAsMap.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (tokenType == null) {
            String message = "The to-be-translated token does not contain a " + AMSTSConstants.TOKEN_TYPE_KEY +
                    " entry. The token: " + receivedToken;
            logger.error(message);
            throw new TokenMarshalException(message);
        }
        if (TokenType.USERNAME.name().equals(tokenType)) {
            return marshallUsernameToken(tokenAsMap);
        } else if (TokenType.OPENAM.name().equals(tokenType)) {
            return marshallAMSessionToken(tokenAsMap);
        }

        throw new TokenMarshalException("Unsupported token translation operation for token: " + receivedToken);

    }

    @Override
    public TokenType getTokenType(JsonValue receivedToken) throws TokenMarshalException {
        Map<String,Object> responseAsMap = receivedToken.asMap();
        String tokenType = (String)responseAsMap.get(AMSTSConstants.TOKEN_TYPE_KEY);
        if (tokenType == null) {
            String message = "REST authN response does not contain " + AMSTSConstants.TOKEN_TYPE_KEY + " entry. The response map: " + responseAsMap;
            logger.error(message);
            throw new TokenMarshalException(message);
        }
        return TokenType.valueOf(tokenType);
    }

    private ReceivedToken marshallUsernameToken(Map<String, Object> tokenAsMap) throws TokenMarshalException {
        String tokenUserName = (String)tokenAsMap.get(AMSTSConstants.USERNAME_TOKEN_USERNAME);
        if (tokenUserName == null) {
            String message = "Exception: json representation of UNT does not contain a username field. The representation: " + tokenAsMap;
            logger.error(message);
            throw new TokenMarshalException(message);
        }
        String password = (String)tokenAsMap.get(AMSTSConstants.USERNAME_TOKEN_PASSWORD);
        if (password == null) {
            String message = "Exception: json representation of UNT does not contain a password field. The representation: " + tokenAsMap;
            logger.error(message);
            throw new TokenMarshalException(message);
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
            logger.error(message);
            throw new TokenMarshalException(message);
        } else {
            ReceivedToken token = new ReceivedToken(openAMSessionIdElementBuilder.buildOpenAMSessionIdElement(sessionId));
            token.setState(ReceivedToken.STATE.NONE);
            token.setUsernameToken(false);
            return token;
        }
    }
}
