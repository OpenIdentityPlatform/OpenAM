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

import org.apache.cxf.helpers.XMLUtils;
import org.apache.ws.security.WSConstants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.JsonMarshaller;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XmlMarshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.fluent.JsonValue.json;

/**
 Implements the json and xml marshalling for OpenAMSessionToken instances.
 */
public class OpenAMSessionTokenMarshaller implements JsonMarshaller<OpenAMSessionToken>, XmlMarshaller<OpenAMSessionToken> {
    public OpenAMSessionToken fromJson(JsonValue jsonValue) throws TokenMarshalException {
        if (jsonValue.get(AMSTSConstants.TOKEN_TYPE_KEY).isNull()) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "passed-in jsonValue does not have " +
                    AMSTSConstants.TOKEN_TYPE_KEY + " field: " + jsonValue);
        }
        final JsonValue jsonSessionId = jsonValue.get(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID);
        if (jsonSessionId.isNull()) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "passed-in jsonValue does not have " +
                    AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID + " field: " + jsonValue);
        }
        final String sessionId = jsonSessionId.asString();
        if (sessionId.isEmpty()) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "passed-in jsonValue does not have a non-empty " +
                    AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID + " field: " + jsonValue);

        }
        return new OpenAMSessionToken(sessionId);
    }

    public JsonValue toJson(OpenAMSessionToken instance) throws TokenMarshalException {
        return json(object(
                field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.OPENAM.name()),
                field(AMSTSConstants.AM_SESSION_TOKEN_SESSION_ID, instance.getSessionId())));
    }

    public OpenAMSessionToken fromXml(Element element) throws TokenMarshalException {
        if (AMSTSConstants.AM_SESSION_ID_ELEMENT_NAMESPACE.equals(element.getNamespaceURI())) {
            final String sessionId = element.getFirstChild().getTextContent();
            if ((sessionId == null) || sessionId.isEmpty()) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "XML element did not include a non-null sessionid. ");
            }
            return new OpenAMSessionToken(sessionId);
        } else {
            String tokenString;
            try {
                Transformer transformer = XMLUtils.newTransformer();
                StreamResult res =  new StreamResult(new ByteArrayOutputStream());
                transformer.transform(new DOMSource(element), res);
                tokenString = new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
            } catch (Exception e) {
                throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                        "Not dealing with an OpenAM session token");
            }
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Not dealing with an OpenAM session token: "  + tokenString);
        }
    }

    public Element toXml(OpenAMSessionToken instance) throws TokenMarshalException {
        Document document;
        final String sessionId = instance.getSessionId();
        try {
            document = XMLUtils.newDocument();
        } catch (ParserConfigurationException e) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
        Element rootElement = document.createElementNS(AMSTSConstants.AM_SESSION_ID_ELEMENT_NAMESPACE,
                AMSTSConstants.AM_SESSION_ID_ELEMENT_NAME);
        rootElement.setTextContent(sessionId);
        /*
        For custom tokens, the cxf.ws.security.trust.STSClient class expects to find a KeyIdentifier element
        below the root element defining the token. See the validateSecurityToken (line 1079) of the STSClient class
        for details.
         */
        Element idElement = document.createElementNS(WSConstants.WSSE_NS, "KeyIdentifier");
        idElement.setTextContent(sessionId);
        rootElement.appendChild(idElement);
        return rootElement;
    }
}
