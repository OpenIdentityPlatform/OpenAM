/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LogoutResponseImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.assertion.AssertionFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class implements the <code>LogoutResponse</code> element in
 * SAML protocol schema.
 * It provides all the methods required by <code>LogoutResponse</code>
 */

public class LogoutResponseImpl extends StatusResponseImpl
implements LogoutResponse {
    
    /**
     * Constructs the <code>LogoutResponse</code> Object.
     *
     */
    public LogoutResponseImpl() {
        isMutable=true;
    }
    
    /**
     * Constructs the <code>LogoutResponse</code> Object.
     *
     * @param element the Document Element of <code>LogoutResponse</code> object.
     * @throws SAML2Exception if <code>LogoutResponse</code> cannot be created.
     */
    
    public LogoutResponseImpl(Element element) throws SAML2Exception {
        parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }
    
    /**
     * Constructs the <code>LogoutResponse</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>LogoutResponse</code> cannot be created.
     */
    public LogoutResponseImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
        XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }
    
    /**
     * Returns the <code>LogoutResponse</code> in an XML document String format
     * based on the <code>LogoutResponse</code> schema described above.
     *
     * @return An XML String representing the <code>LogoutResponse</code>.
     * @exception SAML2Exception if some error occurs during conversion to
     *           <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }
    
    /**
     * Returns the <code>LogoutResponse</code> in an XML document String format
     * based on the <code>LogoutResponse</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>LogoutResponse</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix,
    boolean declareNS) throws SAML2Exception {
        if (isSigned && signedXMLString != null) {
            return signedXMLString;
        }

        validateData();
        StringBuffer xmlString = new StringBuffer(1000);
        xmlString.append(SAML2Constants.START_TAG);
        if (includeNSPrefix) {
            xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
        }
        xmlString.append(SAML2Constants.LOGOUT_RESPONSE)
        .append(SAML2Constants.SPACE);
        
        xmlString.append(super.toXMLString(includeNSPrefix,declareNS));
        
        xmlString.append(SAML2Constants.NEWLINE)
        .append(SAML2Constants.SAML2_END_TAG)
        .append(SAML2Constants.LOGOUT_RESPONSE)
        .append(SAML2Constants.END_TAG);
        
        return xmlString.toString();
    }
    
    /**
     * Makes this object immutable.
     *
     */
    public void makeImmutable() {
        super.makeImmutable();
    }
    
    /**
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }
    
    /**
     * Parses the Docuemnt Element for this object.
     *
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    private void parseElement(Element element) throws SAML2Exception {
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        responseId = element.getAttribute(SAML2Constants.ID);
        validateID(responseId);
        
        version = element.getAttribute(SAML2Constants.VERSION);
        validateVersion(version);
        
        String issueInstantStr = element.getAttribute(
        SAML2Constants.ISSUE_INSTANT);
        validateIssueInstant(issueInstantStr);
        
        destination = element.getAttribute(SAML2Constants.DESTINATION);
        consent = element.getAttribute(SAML2Constants.CONSENT);
        inResponseTo = element.getAttribute(SAML2Constants.INRESPONSETO);
        
        NodeList nList = element.getChildNodes();
        
        if ((nList !=null) && (nList.getLength() >0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
                    if (cName.equals(SAML2Constants.ISSUER)) {
                        issuer =
                        assertionFactory.createIssuer((Element)childNode);
                    } else if (cName.equals(SAML2Constants.SIGNATURE)) {
                        signatureString=
                        XMLUtils.getElementString((Element)childNode);
                        isSigned = true; 
                    } else if (cName.equals(SAML2Constants .EXTENSIONS)) {
                        extensions =
                        protoFactory.createExtensions((Element)childNode);
                    } else if (cName.equals(SAML2Constants.STATUS)) {
                        status =
                        protoFactory.createStatus((Element)childNode);
                        validateStatus();
                    }
                }
            }
        }
    }
    
}
