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
 * $Id: ManageNameIDRequestImpl.java,v 1.3 2008/06/25 05:47:59 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import java.security.PublicKey;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;
import com.sun.identity.saml2.protocol.NewEncryptedID;
import com.sun.identity.saml2.protocol.NewID;
import com.sun.identity.saml2.protocol.ProtocolFactory;

public class ManageNameIDRequestImpl 
   extends RequestAbstractImpl implements ManageNameIDRequest {
    public final String elementName = "ManageNameIDRequest";
    private NewEncryptedID newEncryptedID = null;
    private EncryptedID encryptedID = null;
    private NewID newID = null;
    private NameID nameid = null;
    private boolean terminate = false;

    /**
     * Constructor to create <code>ManageNameIDRequest</code> Object. 
     */
    public ManageNameIDRequestImpl() {
        isMutable = true;
    }

    /**
     * Constructor to create <code>ManageNameIDRequest</code> Object. 
     *
     * @param element Document Element of 
     *         <code>ManageNameIDRequest<code> object.
     * @throws SAML2Exception 
     *         if <code>ManageNameIDRequest<code> cannot be created.
     */
    public ManageNameIDRequestImpl(Element element) throws SAML2Exception {
    	parseElement(element);
    	if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
        makeImmutable();
    }

    /**
     * Constructor to create <code>ManageNameIDRequest</code> Object. 
     *
     * @param xmlString XML Representation of 
     *        the <code>ManageNameIDRequest<code> object.
     * @throws SAML2Exception 
     *        if <code>ManageNameIDRequest<code> cannot be created.
     */
    public ManageNameIDRequestImpl(String xmlString) throws SAML2Exception {
    	Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
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
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();

        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ManageNameIDRequestImpl.parseElement: "
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an EncryptedAssertion.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals(elementName))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ManageNameIDRequestImpl.parseElement:"
                    + "not ManageNameIDRequest.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        requestId = element.getAttribute("ID");
        validateID(requestId);

        version = element.getAttribute(SAML2Constants.VERSION);
        validateVersion(version);

        String issueInstantStr = element.getAttribute("IssueInstant");
        validateIssueInstant(issueInstantStr);

        destinationURI = element.getAttribute("Destination");
        consent = element.getAttribute("Consent");

        NodeList nList = element.getChildNodes();

        if ((nList !=null) && (nList.getLength() >0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName() ;
                if (cName != null)  {
                    if (cName.equals("Issuer")) {
                        nameID =
                        assertionFactory.createIssuer((Element)childNode);
                    } else if (cName.equals("Signature")) {
                        signatureString=
                            XMLUtils.getElementString((Element)childNode);   
                        isSigned = true;     
                    } else if (cName.equals("Extensions")) {
                        extensions =
                        protocolFactory.createExtensions((Element)childNode);
                    } else if (cName.equals("NameID")) {
                        nameid =
                            assertionFactory.createNameID((Element)childNode);
                    } else if (cName.equals("EncryptedID")) {
                        encryptedID =
                        assertionFactory.createEncryptedID((Element)childNode);
                    } else if (cName.equals("NewID")) {
                        newID =
                        	protocolFactory.createNewID((Element)childNode);
                    } else if (cName.equals("NewEncryptedID")) {
                        newEncryptedID =
                       protocolFactory.createNewEncryptedID((Element)childNode);
                    } else if (cName.equals("Terminate")) {
                        terminate = true;
                    } 
                }
            }
        }
    }

    /**
     * Returns the value of the <code>NewEncryptedID</code> object.
     *
     * @return  the value of <code>NewEncryptedID</code> object.
     * @see #setNewEncryptedID(NewEncryptedID)
     */
    public NewEncryptedID getNewEncryptedID()
    {
    	return newEncryptedID;
    }

    /**
     * Sets the value of the <code>newEncryptedID</code> property.
     * 
     * @param value the value of the <code>newEncryptedID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getNewEncryptedID
     */
    public void setNewEncryptedID(NewEncryptedID value)
    throws SAML2Exception
    {
        if (!isMutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        newEncryptedID = value;

        return;
    }

    /**
     * Returns the value of the <code>encryptedID</code> property.
     * 
     * @return the value of the <code>encryptedID</code> property.
     * @see #setEncryptedID(EncryptedID)
     */
    public EncryptedID getEncryptedID()
    {
    	return encryptedID;
    }

    /**
     * Sets the value of the <code>encryptedID</code> property.
     * 
     * @param value the value of the <code>encryptedID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getEncryptedID
     */
    public void setEncryptedID(EncryptedID value)
    throws SAML2Exception
    {
        if (!isMutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        encryptedID = value;
        return;
    }

    /**
     * Returns the value of the <code>NewID</code> property.
     * 
     * @return the value of the <code>NewID</code> property.
     * @see #setNewID(NewID)
     */
    public NewID getNewID()
    {
    	return newID;
    }

    /**
     * Sets the value of the <code>NewID</code> property.
     * 
     * @param value the value of the <code>NewID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getNewID
     */
    public void setNewID(NewID value)
    throws SAML2Exception
    {
        if (!isMutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        newID = value;
        return;
    }

    /**
     * Returns the value of the <code>nameID</code> property.
     * 
     * @return the value of the <code>nameID</code> property.
     * @see #setNameID(NameID)
     */
    public NameID getNameID()
    {
    	return nameid;
    }

    /**
     * Sets the value of the <code>nameID</code> property.
     * 
     * @param value the value of the <code>nameID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getNameID
     */
    public void setNameID(NameID value)
    throws SAML2Exception
    {
        if (!isMutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        nameid = value;
        return;
    }

    /**
     * Returns true if this is a terminating request.
     *
     * @return true if this is a terminating request.
     * @see #setTerminate(boolean)
     */ 
    public boolean getTerminate()
    {
    	return terminate;
    }

    /**
     * Set this request as terminating request.
     *
     * @param value true to set this request as terminating request.
     * @throws SAML2Exception if this object is immutable.
     * @see #getTerminate
     */
    public void setTerminate(boolean value)
    throws SAML2Exception
    {
        if (!isMutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        terminate = value;
        return;
    }

    /** Returns a String representation of this Object.
    *
    *  @exception SAML2Exception , if it could not create String object
    *  @return a  String representation of this Object.
    */
    public String toXMLString()
    throws SAML2Exception 
    {
        return toXMLString(true, false);
    }
   
    /** Returns a String representation
    *
    *  @param includeNSPrefix determines whether or not the namespace
    *          qualifier is prepended to the Element when converted
    *  @param declareNS determines whether or not the namespace is declared
    *          within the Element.
    *  @exception SAML2Exception ,if it could not create String object.
    *  @return a String , String representation of this Object.
    **/
   
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception
    {   
    	if (isSigned && signedXMLString != null) {
            return signedXMLString;
        }

        StringBuffer xml = new StringBuffer();

        if ((newID != null) && (newEncryptedID != null)) {
        	throw new SAML2Exception("wrongInput");
        }

        if (((newID != null) || (newEncryptedID != null)) && 
                                                 (terminate == true)) {
        	throw new SAML2Exception("wrongInput");
        }

    	String NS="";
    	String NSP="";
    	
    	if (declareNS) {
    	    NS = SAML2Constants.PROTOCOL_DECLARE_STR;
    	}
    	
    	if (includeNSPrefix) {
    	    NSP = SAML2Constants.PROTOCOL_PREFIX;
    	}

        xml.append("<").append(NSP).append(elementName);
        xml.append(NS).append(" ");

        xml.append(getAttributesString());

        xml.append(">");

        xml.append(getElements(includeNSPrefix, declareNS));

        if (nameid != null) {
            xml.append(nameid.toXMLString(includeNSPrefix, declareNS));
        }
                
        if (encryptedID != null) {
            xml.append(encryptedID.toXMLString());
        }
        
        if (newID != null) {
            xml.append(newID.toXMLString(includeNSPrefix, declareNS));
        }
        
        if (newEncryptedID != null) {
            xml.append(newEncryptedID.toXMLString());
        }
        
        if (terminate == true) {
            xml.append("<").append(NSP).append("Terminate/>");
        }
        
        xml.append("</").append(NSP).append(elementName).append(">");

        return xml.toString();    
    }
    
    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            super.makeImmutable();
        	if ((nameid != null) && (nameid.isMutable())) {
                nameid.makeImmutable();
            }

            isMutable=false;
        }
    }
}
