/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthnQueryImpl.java,v 1.3 2008/06/25 05:47:59 qcheng Exp $
 *
 */

package com.sun.identity.saml2.protocol.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.AuthnQuery;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext ;
import com.sun.identity.shared.xml.XMLUtils;

public class AuthnQueryImpl extends SubjectQueryAbstractImpl
    implements AuthnQuery {

    protected RequestedAuthnContext requestedAuthnContext;
    protected String sessionIndex;

    /**
     * Constructor to create <code>AuthnQuery</code> Object .
     */
    public AuthnQueryImpl() {
        elementName = SAML2Constants.AUTHN_QUERY;
        isMutable = true;
    }

    /**
     * Constructor to create <code>AuthnQuery</code> Object.
     *
     * @param element the Document Element Object.
     * @throws SAML2Exception if error creating <code>AuthnQuery</code> 
     *     Object. 
     */
    public AuthnQueryImpl(Element element) throws SAML2Exception {
        parseDOMElement(element);
        elementName = SAML2Constants.AUTHN_QUERY;
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Constructor to create <code>AuthnQuery</code> Object.
     *
     * @param xmlString the XML String.
     * @throws SAML2Exception if error creating <code>AuthnQuery</code> 
     *     Object. 
     */
    public AuthnQueryImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument = 
            XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseDOMElement(xmlDocument.getDocumentElement());
        elementName = SAML2Constants.AUTHN_QUERY;
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Returns the <code>RequestedAuthnContext</code> object.
     *
     * @return the <code>RequestedAuthnContext</code> object.
     * @see #setRequestedAuthnContext(RequestedAuthnContext)
     */
    public RequestedAuthnContext getRequestedAuthnContext()
    {
        return requestedAuthnContext;
    }
  
    /**
     * Sets the <code>RequestedAuthnContext</code> object.
     *
     * @param requestedAuthnContext the new <code>RequestedAuthnContext</code>
     *     object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequestedAuthnContext
     */
    public void setRequestedAuthnContext(
        RequestedAuthnContext requestedAuthnContext) throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.requestedAuthnContext = requestedAuthnContext;
    }

    /**
     * Returns the value of the <code>SessionIndex</code> attribute.
     *
     * @return value of <code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex() {
        return sessionIndex;
    }

    /**
     * Sets the value of <code>SessionIndex</code> attribute.
     *
     * @param sessionIndex new value of the <code>SessionIndex</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionIndex
     */
    public void setSessionIndex(String sessionIndex) throws SAML2Exception{
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.sessionIndex = sessionIndex;
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

	if ((sessionIndex != null) && (sessionIndex.length() > 0)) {
	    attrs.append(SAML2Constants.SPACE)
                 .append(SAML2Constants.SESSION_INDEX)
	         .append(SAML2Constants.EQUAL).append(SAML2Constants.QUOTE)
	         .append(sessionIndex).append(SAML2Constants.QUOTE);
	}

	if (requestedAuthnContext != null) {
            childElements.append(requestedAuthnContext.toXMLString(
                includeNSPrefix, declareNS)).append(SAML2Constants.NEWLINE);
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
        sessionIndex = element.getAttribute(SAML2Constants.SESSION_INDEX);
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

        ProtocolFactory pFactory = ProtocolFactory.getInstance();
        if(iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName() ;
            if (SAML2Constants.REQ_AUTHN_CONTEXT.equals(localName)) {
                requestedAuthnContext =
                    pFactory.createRequestedAuthnContext(childElement);
            } else {
                iter.previous();
            }
        }
    }
}
