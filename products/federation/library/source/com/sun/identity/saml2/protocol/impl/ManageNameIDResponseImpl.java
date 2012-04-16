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
 * $Id: ManageNameIDResponseImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.ManageNameIDResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;

/**
 * Java content class for ManageNameIDResponse element declaration.
 * <p>The following schema fragment specifies the expected
 * 	content contained within this java content object.
 * <p>
 * <pre>
 * &lt;element name="ManageNameIDResponse" 
 *   type="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType"/>
 * </pre>
 */
public class ManageNameIDResponseImpl 
extends StatusResponseImpl implements ManageNameIDResponse {
    public final String elementName = "ManageNameIDResponse";

    /**
     * Constructor to create <code>ManageNameIDResponse</code> Object. 
     */
    public ManageNameIDResponseImpl() {
    	isMutable = true;
    }

    /**
     * Constructor to create <code>ManageNameIDResponse</code> Object. 
     *
     * @param element Document Element of 
     *         <code>ManageNameIDRequest<code> object.
     * @throws SAML2Exception 
     *         if <code>ManageNameIDRequest<code> cannot be created.
     */
    public ManageNameIDResponseImpl(Element element) throws SAML2Exception {
    	parseElement(element);
    	if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    	makeImmutable();
    }

    /**
     * Constructor to create <code>ManageNameIDResponse</code> Object. 
     *
     * @param xmlString XML Representation of 
     *        the <code>ManageNameIDRequest<code> object.
     * @throws SAML2Exception 
     *        if <code>ManageNameIDRequest<code> cannot be created.
     */
    public ManageNameIDResponseImpl(String xmlString) throws SAML2Exception {
    	Document doc = XMLUtils.toDOMDocument(xmlString, SAMLUtils.debug);
		if (doc == null) {
		    throw new SAML2Exception("errorObtainingElement");
		}
	parseElement(doc.getDocumentElement());
	if (isSigned) {
            signedXMLString = xmlString;
        }
    	makeImmutable();
    }

    private void parseElement(Element element) throws SAML2Exception {
        AssertionFactory af = AssertionFactory.getInstance();
    	ProtocolFactory pf = ProtocolFactory.getInstance();
        
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message(
                	"ManageNameIDResponseImpl.parseElement: Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an EncryptedAssertion.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals(elementName))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message(
                	"ManageNameIDResponseImpl.parseElement:"
                    + "not ManageNameIDResponse.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        responseId = element.getAttribute("ID");
        validateID(responseId);
            
        version = element.getAttribute("Version");
    	validateVersion(version);

        String issueInstantStr = element.getAttribute("IssueInstant");
    	validateIssueInstant(issueInstantStr);
            
        destination = element.getAttribute("Destination");
        consent = element.getAttribute("Consent");
        inResponseTo = element.getAttribute("InResponseTo");

    	NodeList nList = element.getChildNodes();
    	
    	if ((nList !=null) && (nList.getLength() >0)) {
    	    for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
    		        if (cName.equals("Issuer")) {
    	                issuer = af.createIssuer((Element)childNode);
    		        } else if (cName.equals("Signature")) {
    		            signatureString =
    			        XMLUtils.getElementString((Element)childNode);
    		            isSigned = true; 
    		        } else if (cName.equals("Extensions")) {
    			        extensions = pf.createExtensions((Element)childNode);
    		        } else if (cName.equals("Status")) {
    			        status = pf.createStatus((Element)childNode);
    		        } 
                }
    	    }
    	}
    }

    /**
     * Returns the <code>ManageNameIDResponse</code> in an XML 
     * document String format based on the <code>ManageNameIDResponse</code> 
     * schema described above.
     * @return An XML String representing the <code>ManageNameIDResponse</code>.
     * @throws SAML2Exception ,if it could not create String object.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true, false);
    }
    
    /**
     * Returns the <code>ManageNameIDResponse</code> in an XML document 
     * String format based on the <code>ManageNameIDResponse</code> schema 
     * described above.
     * @param includeNSPrefix Determines whether the namespace qualifier is
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>ManageNameIDResponse</code>.
    *  @throws SAML2Exception if it could not create String object.
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception {
        if (isSigned && signedXMLString != null) {
            return signedXMLString;
        }

        StringBuffer xml = new StringBuffer();
    	String NS="";
    	String NSP="";
    	
    	if (declareNS) {
    	    NS = SAML2Constants.PROTOCOL_DECLARE_STR;
    	}
    	
    	if (includeNSPrefix) {
    	    NSP = SAML2Constants.PROTOCOL_PREFIX;
    	}

        xml.append("<").append(NSP).append(elementName).append(" ");

        xml.append(super.toXMLString(includeNSPrefix, includeNSPrefix));
        
        xml.append("</").append(NSP).append(elementName).append(">");
        
        return xml.toString();    
    }
}
