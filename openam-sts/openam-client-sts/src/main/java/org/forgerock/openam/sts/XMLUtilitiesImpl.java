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

package org.forgerock.openam.sts;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.json.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

/**
 * @see org.forgerock.openam.sts.XMLUtilities
 */
public class XMLUtilitiesImpl implements XMLUtilities {
    private static final Debug NULL_DEBUG = null;

    public Document stringToDocumentConversion(String xmlString) {
        return XMLUtils.toDOMDocument(xmlString, NULL_DEBUG);
    }

    public String documentToStringConversion(Node inputNode) {
        return XMLUtils.print(inputNode);
    }

    public Document newSafeDocument(boolean schemaValidation) throws ParserConfigurationException {
        return XMLUtils.getSafeDocumentBuilder(schemaValidation).newDocument();
    }

    public Transformer getNewTransformer() throws TokenMarshalException {
        try {
            return XMLUtils.getTransformerFactory().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }
}
