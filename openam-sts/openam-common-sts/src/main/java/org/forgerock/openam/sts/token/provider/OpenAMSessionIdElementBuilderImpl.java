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

package org.forgerock.openam.sts.token.provider;

import org.apache.ws.security.WSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

/**
 * This class creates an XML element encapsulating an OpenAM session identifier. It also marshals back from this
 * element to the original session id. This marshalling is necessary as the CXF-STS engine needs all issued tokens
 * to be in XML format, yet this xml format cannot be returned from the rest-sts.
 *
 * TODO: maybe rename to the OpenAMSessionIdMarshaller?
 */
public class OpenAMSessionIdElementBuilderImpl implements OpenAMSessionIdElementBuilder {
    private final Logger logger;

    @Inject
    public OpenAMSessionIdElementBuilderImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Element buildOpenAMSessionIdElement(String sessionId) throws TokenMarshalException {
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new TokenMarshalException(e.getMessage(), e);
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

    public String extractOpenAMSessionId(Element element) throws TokenMarshalException {
        if (AMSTSConstants.AM_SESSION_ID_ELEMENT_NAMESPACE.equals(element.getNamespaceURI())) {
            return element.getFirstChild().getTextContent();
        } else {
            String tokenString = null;
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                StreamResult res =  new StreamResult(new ByteArrayOutputStream());
                transformer.transform(new DOMSource(element), res);
                tokenString = new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
            } catch (Exception e) {
                logger.error("exception caught marshalling unexpected token type to string: " + e);
            }
            throw new TokenMarshalException("Not dealing with an OpenAM session token: "  + tokenString);
        }
    }
}
