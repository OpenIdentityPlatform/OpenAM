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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap;

import static org.forgerock.openam.utils.Time.*;

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.soap.policy.am.OpenAMSessionAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This class provides means to create TokenSpecification instances which encapsulate the state necessary to issue
 * tokens of various types, currently limited to SAML2 Bearer, HolderOfKey, and SenderVouches assertions. This state
 * is referenced by the SoapSTSConsumer to set the appropriate state in the STSClient so that RequestSecurityToken
 * requests can be generated for specific token types. Ultimately, the token-type desired in a RST is a combination of
 * the TokenType and KeyType strings, though if a SAML2 SenderVouches assertion is desired, the ActAs or OnBehalfOf
 * element  must also be set.
 */
public class TokenSpecification {
    private static final Element NULL_ON_BEHALF_OF = null;
    private static final X509Certificate NULL_HOLDER_OF_KEY_CERTIFICATE = null;
    private static final String NULL_KEY_TYPE = null;

    public static final String SAML2_TOKEN_TYPE =
            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    public static final String PUBLIC_KEY_KEYTYPE =
            "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    public static final String BEARER_KEYTYPE =
            "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";

    final String tokenType;
    final String keyType;
    final Element onBehalfOf;
    final X509Certificate holderOfKeyCertificate;

    public TokenSpecification(String tokenType, String keyType, Element onBehalfOf, X509Certificate holderOfKeyCertificate) {
        this.keyType = keyType;
        this.tokenType = tokenType;
        this.onBehalfOf = onBehalfOf;
        this.holderOfKeyCertificate = holderOfKeyCertificate;
    }

    public TokenSpecification(String tokenType, String keyType, Element onBehalfOf) {
        this(tokenType, keyType, onBehalfOf, NULL_HOLDER_OF_KEY_CERTIFICATE);
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getKeyType() {
        return keyType;
    }

    public Element getOnBehalfOf() {
        return onBehalfOf;
    }

    public X509Certificate getHolderOfKeyCertificate() {
        return holderOfKeyCertificate;
    }

    /**
     * @return A TokenSpecification instance with the state necessary to generate a SAML2 bearer assertion
     */
    public static TokenSpecification saml2Bearer() {
        return new TokenSpecification(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, NULL_ON_BEHALF_OF);
    }

    /**
     * @param username the username in the UsernameToken included as the OnBehalfOf element
     * @param password the password in the UsernameToken included as the OnBehalfOf element
     * @return A TokenSpecification instance with the state necessary to generate a SAML2 assertion with SenderVouches
     * subject confirmation. The OnBehalfOf element in this TokenSpecification will be a UsernameToken.
     */
    public static TokenSpecification usernameTokenSaml2SenderVouches(String username, String password) {
        return new TokenSpecification(SAML2_TOKEN_TYPE, PUBLIC_KEY_KEYTYPE, usernameTokenOnBehalfOfElement(username, password));
    }

    /**
     * @param sessionId the OpenAM session id which will be used as the value in the OpenAMSessionToken element
     * @return A TokenSpecification instance with the state necessary to generate a SAML2 assertion with SenderVouches
     * subject confirmation. The OnBehalfOf element in this TokenSpecification will be a OpenAMSessionToken.
     */
    public static TokenSpecification openAMSessionTokenSaml2SenderVouches(String sessionId) {
        return new TokenSpecification(SAML2_TOKEN_TYPE, PUBLIC_KEY_KEYTYPE, openAMSessionTokenOnBehalfOfElement(sessionId));
    }

    /**
     *
     * @param x509Certificate The sts client's x509-certificate to be included in the SAML2 HoK certificate to provide
     *                        proof of assertion ownership
     * @return A TokenSpecification instance with the state necessary to generate a SAML2 assertion with HolderOfKey
     * subject confirmation.
     */
    public static TokenSpecification saml2HolderOfKey(X509Certificate x509Certificate) {
        return new TokenSpecification(SAML2_TOKEN_TYPE, PUBLIC_KEY_KEYTYPE, NULL_ON_BEHALF_OF,
                x509Certificate);
    }

    /**
     *
     * @return A TokenSpecification instance which will result in the STS generating an OpenIdConnect token.
     */
    public static TokenSpecification openIdConnectToken() {
        return new TokenSpecification(AMSTSConstants.AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE, NULL_KEY_TYPE,
                NULL_ON_BEHALF_OF);
    }

    /**
     *
     * @return  A TokenSpecification instance which will result in the STS generating an OpenIdConnect token.
     * The subject asserted by this token will correspond to the {username, password} credentials.
     */
    public static TokenSpecification openIdConnectTokenUsernameTokenDelegation(String username, String password) {
        return new TokenSpecification(AMSTSConstants.AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE, NULL_KEY_TYPE,
                usernameTokenOnBehalfOfElement(username, password));
    }

    /**
     *
     * @return  A TokenSpecification instance which will result in the STS generating an OpenIdConnect token.
     * The subject asserted by this token will correspond to the principal associated with the OpenAM session id.
     */
    public static TokenSpecification openIdConnectTokenOpenAMSessionTokenDelegation(String sessionId) {
        return new TokenSpecification(AMSTSConstants.AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE, NULL_KEY_TYPE,
                openAMSessionTokenOnBehalfOfElement(sessionId), NULL_HOLDER_OF_KEY_CERTIFICATE);
    }

    /**
     * @param username the UsernameToken username
     * @param password the UsernameToken password
     * @return A UsernameToken element to be used as the OnBehalfOf Element in a RequestSecurityToken defining the ISSUE
     * operation invocation.
     */
    public static Element usernameTokenOnBehalfOfElement(String username, String password) {
        WSSecUsernameToken unt = new WSSecUsernameToken();
        unt.setUserInfo(username, password);
        unt.setPasswordType(WSConstants.PASSWORD_TEXT);
        unt.addCreated();
        Date expirationDate = newDate();
        expirationDate.setTime(currentTimeMillis() + (1000 * 60));
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            unt.prepare(document);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return unt.getUsernameTokenElement();
    }

    /**
     * @param sessionId the OpenAM session id to be used as the content of the BinarySecurityToken containing the
     *                  OpenAMSessionToken assertion
     * @return The element corresponding to a OpenAMSessionToken assertion to be used as the OnBehalfOf Element in the
     * RequestSecurityToken defining the ISSUE operation invocation.
     */
    public static Element openAMSessionTokenOnBehalfOfElement(String sessionId) {
        /*
        The NestedPolicy element is not relevant when Element representation of a BST with OpenAMSessionToken value
        type is generated
         */
        Element nestedPolicyElement = null;
        return new OpenAMSessionAssertion(SP12Constants.INSTANCE, SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT,
                nestedPolicyElement, sessionId).getTokenElement();
    }
}
