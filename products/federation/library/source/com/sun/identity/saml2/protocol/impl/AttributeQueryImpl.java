/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AttributeQueryImpl.java,v 1.3 2008/06/25 05:47:59 qcheng Exp $
 *
 */

package com.sun.identity.saml2.protocol.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.AttributeQuery;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;

public class AttributeQueryImpl
    extends SubjectQueryAbstractImpl implements AttributeQuery {

    protected List attributes;

    /**
     * Constructor to create <code>AttributeQuery</code> Object .
     */
    public AttributeQueryImpl() {
        elementName = SAML2Constants.ATTRIBUTE_QUERY;
        isMutable = true;
    }

    /**
     * Constructor to create <code>AttributeQuery</code> Object.
     *
     * @param element the Document Element Object.
     * @throws SAML2Exception if error creating <code>AttributeQuery</code> 
     *     Object. 
     */
    public AttributeQueryImpl(Element element) throws SAML2Exception {
        parseDOMElement(element);
        elementName = SAML2Constants.ATTRIBUTE_QUERY;
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Constructor to create <code>AttributeQuery</code> Object.
     *
     * @param xmlString the XML String.
     * @throws SAML2Exception if error creating <code>AttributeQuery</code> 
     *     Object. 
     */
    public AttributeQueryImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument = 
            XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseDOMElement(xmlDocument.getDocumentElement());
        elementName = SAML2Constants.ATTRIBUTE_QUERY;
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /** 
     * Returns <code>Attribute</code> objects. 
     *
     * @return the <code>Attribute</code> objects. 
     * @see #setAttributes(List)
     */
    public List getAttributes() {
        return attributes;
    }
  
    /** 
     * Sets the <code>Attribute</code> objects. 
     *
     * @param attributes the new <code>Attribute</code> objects. 
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributes
     */
    public void setAttributes(List attributes) throws SAML2Exception {
         if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.attributes = attributes;
    }

    protected void getXMLString(Set namespaces, StringBuffer attrs,
        StringBuffer childElements, boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception {

        if (declareNS) {
            namespaces.add(SAML2Constants.PROTOCOL_DECLARE_STR.trim());
            namespaces.add(SAML2Constants.ASSERTION_DECLARE_STR.trim());
        }

        super.getXMLString(namespaces, attrs, childElements, includeNSPrefix,
            declareNS);

        if ((attributes != null) && (!attributes.isEmpty())) {
            for(Iterator iter = attributes.iterator(); iter.hasNext(); ) {
                Attribute attribute = (Attribute)iter.next();
                childElements.append(attribute.toXMLString(includeNSPrefix,
                    declareNS)).append(SAML2Constants.NEWLINE);
            }
        }
    }

    /** 
     * Parses attributes of the Docuemnt Element for this object.
     * 
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMAttributes(Element element) throws SAML2Exception {
        super.parseDOMAttributes(element);
    }

    /** 
     * Parses child elements of the Docuemnt Element for this object.
     * 
     * @param iter the child elements iterator.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMChileElements(ListIterator iter)
        throws SAML2Exception {
        super.parseDOMChileElements(iter);

        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        while(iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName() ;
            if (SAML2Constants.ATTRIBUTE.equals(localName)) {
                if (attributes == null) {
                    attributes = new ArrayList();
                }
                attributes.add(assertionFactory.createAttribute(childElement));
            } else {
                iter.previous();
            }
        }
    }
}
