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

package org.forgerock.openam.sts.token.model;

import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.util.Reject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class represents an OpenID Connect ID Token. The tokenValue is the base64Url encoded representation of the
 * jws or jwt ID Token. This class will serve as a type specifier for the generic types required in the AuthenticationHandler
 * and TokenAuthenticationRequestDispatcher, and contain classes to marshal to/from json and XML, as required by the
 * SOAP and REST STS.
 */
public class OpenIdConnectIdToken {
    private final String tokenValue;

    public OpenIdConnectIdToken(String tokenValue) {
        this.tokenValue = tokenValue;
        Reject.ifNull(this.tokenValue, "A non-null token value must be specified.");
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public JsonValue toJson() {
        return json(object(field(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY, tokenValue)));
    }

    public static OpenIdConnectIdToken fromJson(JsonValue json) throws TokenMarshalException {
        try {
            return new OpenIdConnectIdToken(json.get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY).asString());
        } catch (NullPointerException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY +
                    " not set in json: " + json.toString(), e);
        }
    }

    public Element toXmlElement() throws TokenMarshalException {
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
        Element rootElement = document.createElementNS(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_ELEMENT_NAMESPACE,
                AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY);
        rootElement.setTextContent(tokenValue);
        return rootElement;
    }

    public static OpenIdConnectIdToken fromXml(Element element) throws TokenMarshalException {
        if (AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_ELEMENT_NAMESPACE.equals(element.getNamespaceURI())) {
            try {
                return new OpenIdConnectIdToken(element.getFirstChild().getTextContent());
            } catch (NullPointerException e) { //thrown by Reject in ctor
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "Exception caught marshalling from xml: " + e, e);
            }
        } else {
            String tokenString = null;
            try {
                Transformer transformer = XMLUtils.getTransformerFactory().newTransformer();
                StreamResult res =  new StreamResult(new ByteArrayOutputStream());
                transformer.transform(new DOMSource(element), res);
                tokenString = new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
            } catch (Exception e) {
                //swallow exception, as it pertains only to generating the tokenString used in the exception.
            }
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Not dealing with an OpenID Connect ID Token: "  + tokenString);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OpenIdConnectIdToken) {
            OpenIdConnectIdToken otherIdToken = (OpenIdConnectIdToken)other;
            return tokenValue.equals(otherIdToken.tokenValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return tokenValue;
    }

    @Override
    public int hashCode() {
        return tokenValue.hashCode();
    }
}
