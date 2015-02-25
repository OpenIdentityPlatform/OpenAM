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
 * $Id: FSAuthnResponse.java,v 1.2 2008/06/25 05:46:43 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */


package com.sun.identity.federation.message;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLVersionMismatchException;
import com.sun.identity.saml.protocol.Response;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>FSAuthnResponse</code> creates and parses the
 * Liberty Response. This class extends the <code>SAML</code>
 * <code>Response</code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSAuthnResponse extends Response {

    private String providerId = null;
    protected String    relayState      = null;
    protected String consentURI = null;
    protected int minorVersion = 0;
    protected String id = null; 
    protected Element domElement = null;
    

   /**
    * Constructor to create <code>FSAuthnResponse</code> object.
    *
    * @param responseID value of the <code>ResponseID</code> attribute.
    * @param inResponseTo value of the <code>inResponseTo</code> attribute.
    * @param status the <code>Status</code> object.
    * @param contents  List of Assertions in the response.
    *                  It could be null when there are no Assertions.
    * @param relayState value of the <code>RelayState</code> attribute.
    * @throws FSMsgException on error.
    * @throws SAMLException on error.
    */
    public FSAuthnResponse(String responseID,String inResponseTo,
                           Status status, List contents, String relayState)
                           throws SAMLException, FSMsgException {
        super( responseID, inResponseTo, status, contents);
        setIssueInstant(new Date());
        this.relayState = relayState;
    }

   /**
    * Creates <code>FSAuthnResponse</code> object from XML Schema.
    *
    * @param xml the XML Schema for this object.
    * @throws <code>SAMLException</code> on error.
    * @throws FSMsgException on error.
    */
    public static FSAuthnResponse parseAuthnResponseXML(
        String xml) throws SAMLException, FSMsgException {
        // parse the xml string
        FSUtils.debug.message("FSAuthnResponse.parseAuthnResponseXML: Called");
        Element root;
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
           if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAuthnResponse.parseXML:Error "
                        + "while parsing input xml string");
                }
           throw new FSMsgException("parseError",null);
        }
        root = doc.getDocumentElement();
        return new FSAuthnResponse(root);
    }

    /**
     * Constructor creates <code>FSAuthnResponse</code> object from
     * Document Element.
     *
     * @param root the Document Element
     * @throws SAMLException on error.
     * @throws FSMsgException on error.
     */
    public FSAuthnResponse(Element root) throws SAMLException, FSMsgException {
        // Make sure this is a Response
        FSUtils.debug.message("FSAuthnResponse(Element): Called");
        if (root == null) {
            FSUtils.debug.message("FSAuthnResponse(Element): "
                + "Input paramenter (root) is null");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
            (!tag.equals(IFSConstants.AUTHN_RESPONSE))) {
            FSUtils.debug.message("FSAuthnResponse(Element): "
                + "Root element name is not AuthnResponse");
            throw new FSMsgException("wrongInput",null);
        }
        domElement = root;
        consentURI = root.getAttribute(IFSConstants.CONSENT);

        // Attribute ResponseID
        id = root.getAttribute(IFSConstants.ID);
        responseID = root.getAttribute(IFSConstants.RESPONSE_ID);
        if ((responseID == null) || (responseID.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponse(Element): "
                    + "AuthnResponse doesn't have ResponseID attribute");
            }
            String[] args = { IFSConstants.RESPONSE_ID };
            throw new FSMsgException("missingAttribute",args);
        }

        inResponseTo = root.getAttribute(IFSConstants.IN_RESPONSE_TO);
        if (inResponseTo == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponse(Element): "
                    + "AuthnResponse doesn't have InResponseTo attribute");
            }            
        }
        
        // Attribute IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);

        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.message("FSAuthnResponse(Element): "
                                  + " missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                FSUtils.debug.message(
                    "FSAuthnResponse(Element): could not parse IssueInstant",e);
                throw new FSMsgException("wrongInput",null);
            }
        }

        parseMajorVersion(root.getAttribute(IFSConstants.MAJOR_VERSION));
        parseMinorVersion(root.getAttribute(IFSConstants.MINOR_VERSION));
        
        setRecipient(root.getAttribute(IFSConstants.RECIPIENT));
 
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals(IFSConstants.STATUS)) {
                    if (status != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAuthnResponse(Element): "
                                + "included more than one <Status>");
                        }
                        throw new FSMsgException("moreElement",null);
                    }
                    status = new Status((Element) child);
                } else if (childName.equals(IFSConstants.ASSERTION)) {
                    if (assertions == Collections.EMPTY_LIST) {
                        assertions = new ArrayList();
                    }
                    assertions.add(new FSAssertion((Element) child));
                } else if (childName.equals(IFSConstants.RELAY_STATE)) {
                    // make sure the providerId is not assigned already
                    if (relayState != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAuthnResponse(Element): "
                                + "should contain only one RelayState.");
                        } 
                        throw new FSMsgException("wrongInput",null);
                    }
                    relayState = XMLUtils.getElementValue((Element) child);
                } else if (childName.equals(IFSConstants.PROVIDER_ID)) {
                    if (providerId != null && providerId.length() != 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAuthnResponse(Element): "
                                + "should contain only one ProviderID.");
                        } 
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                  } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAuthnResponse(Element): "
                            + "included wrong element: " + childName);
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            } // end if childName != null
        } // end for loop

        if (status == null) {
            FSUtils.debug.message("FSAuthnResponse(Element): "
                + "missing element <Status>.");
            throw new FSMsgException("missingElement",null);
        }
        //check for signature
        List signs = XMLUtils.getElementsByTagNameNS1(root,
                                        SAMLConstants.XMLSIG_NAMESPACE_URI,
                                        SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            Element elem = (Element)signs.get(0);
            setSignature(elem);
            xmlString = XMLUtils.print(root);
            signed = true;
        } else if (signsSize != 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponse(Element): "
                    + "included more than one Signature element.");
            }
            throw new FSMsgException("moreElement",null);
        }        
        //end check for signature
    }

   /**
    * Returns the value of the <code>id</code> attribute.
    *
    * @return the value of <code>id</code> attribute.
    * @see #setID(String)
    */
    public String getID() {
        return id;
    }
    
    /**
     * Sets the value of the <code>id</code> attribute.
     *
     * @param id the new value of <code>id</code> attribute.
     * @see #getID
     */
    public void setID(String id) {
        this.id = id;
    }    
    
   /**
    * Returns the <code>ProviderID</code> attribute value.
    *
    * @return value of the <code>ProviderID</code> attribute.
    * @see #setProviderId(String)
    */
    public String getProviderId() {
       return providerId;
    }
   
    /**
     * Sets the <code>ProviderID</code> attribute value.
     *
     * @param provId new value of <code>ProviderID</code> attribute.
     * @see #getProviderId
     */
    public void setProviderId(String provId) {
       providerId = provId;
    }
   
   /**
    * Returns a signed XML Representation of this object.
    *
    * @return a signed XML Representation of this object.
    */
    public String getSignedXMLString(){
       return xmlString;
    }
   
   /**
    * Returns the Signature string.
    *
    * @return the Signature string.
    */
    public String getSignatureString(){
       return signatureString;
    }

   /**
    * Returns the value <code>MinorVersion</code> attribute.
    *
    * @return the value <code>MinorVersion</code> attribute.
    * @see #setMinorVersion(int)
    */
    public int getMinorVersion() {
       return minorVersion;
    }

   /**
    * Returns the value of <code>MajorVersion</code> attribute.
    *
    * @param version the value of <code>MajorVersion</code> attribute.
    * @see #getMinorVersion
    */
    public void setMinorVersion(int version) {
       minorVersion = version;
    }

   /**
    * Returns the value of the <code>consent</code> attribute.
    *
    * @return value of <code>consent</code> attribute.
    * @see #setConsentURI(String)
    */

    public String getConsentURI() {
       return consentURI;
    }

   /**
    * Sets the value of the <code>consent</code> attribute.
    *
    * @param consent new value of <code>consent</code> attribute.
    * @see #getConsentURI
    */
    public void setConsentURI(String consent) {
       this.consentURI = consent;
    }

   /**
    * Returns the Document Element for this object.
    *
    * @return the Document Element for this object.
    */
    public Element getDOMElement() {
        return domElement;
    }

    /**
     * Parses the input and sets the <code>MajorVersion</code>.
     *
     * @param majorVer value of <code>MajorVersion</code> attribute to be set.
     * @throws FSMsgException on error.
     * @throws SAMLException if the version is incorrect.
     */
    private void parseMajorVersion(String majorVer) 
                 throws SAMLException, FSMsgException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponse(Element): invalid "
                    + "MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }

        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAuthnResponse(Element):MajorVersion of"
                        + " the Response is too high.");
                }
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                                                 "responseVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAuthnResponse(Element):MajorVersion of"
                        + " the Response is too low.");
                }
                throw new SAMLVersionMismatchException(
                    FSUtils.BUNDLE_NAME,"responseVersionTooLow",null);
            }
        }
    }

   /**
    * Parses the input and set the <code>MinorVersion</code>.
    *
    * @param minorVer value of <code>MinorVersion</code> attribute to be set.
    * @throws FSMsgException on error.
    * @throws SAMLException if the version is incorrect.
    */
    private void parseMinorVersion(
        String minorVer) throws SAMLException, FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponse(Element): invalid "
                    + "MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }

        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
           if(FSUtils.debug.messageEnabled()) {
              FSUtils.debug.message("FSAuthnResponse.checkMinorVersion:"+
              " Minor Version of the AuthnResponse is too high.");
           }
           throw new FSMsgException("requestVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
           if(FSUtils.debug.messageEnabled()) {
              FSUtils.debug.message("FSAuthnResponse.checkMinorVersion:" +
              " Minor Version of the AuthnResponse is too low.");
           }
           throw new FSMsgException("requestVersionTooLow",null);
        }
    }

    /**
     * Returns the <code>RelayState</code> attribute in the Response.
     *
     * @return the <code>RelayState</code> attribute in the Response.
     */
    public String getRelayState() {
        return relayState;
    }

    /**
     * Returns the string representation of this object.
     * This method translates the response to an XML document string based on
     * the Response schema described above.
     *
     * @return An XML String representing the response. NOTE: this is a
     *                complete SAML response xml string with ResponseID,
     *                MajorVersion, etc.
     */
    public String toXMLString()  throws FSMsgException {
        return this.toXMLString(true, true);
    }

    /**
     * Returns a String representation of the &lt;samlp:Response&gt; element.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *          is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *          within the Element.
     * @return A string containing the valid XML for this element
     */   
    public String toXMLString(boolean includeNS, boolean declareNS) 
           throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }

    /**
     * Returns a String representation of the &lt;samlp:Response&gt; element.
     *
     * @param includeNS  Determines whether or not the namespace qualifier
     *          is prepended to the Element when converted
     * @param declareNS  Determines whether or not the namespace is declared
     *          within the Element.
     * @param includeHeader  Determines whether the output include the xml
     *                declaration header.
     * @return A string containing the valid XML for this element
     */   
    public String toXMLString(boolean includeNS,
                        boolean declareNS,
                        boolean includeHeader)  throws FSMsgException {
        FSUtils.debug.message("FSAuthnResponse.toXMLString(3): Called");
        
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSAuthnResponse.toXMLString: "
                + "providerId is null ");
                throw new FSMsgException("nullProviderID",null);
        }
        
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
               .append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>\n")
               .append(IFSConstants.QUOTE)
               .append(IFSConstants.QUESTION_MARK)
               .append(IFSConstants.RIGHT_ANGLE)
               .append(IFSConstants.NL);
        }
        String prefixSAML = "";
        String prefixLIB = "";
        String prefixSAML_PROTOCOL = "";
        String uriSAML_PROTOCOL = "";
        String uriSAML = "";
        String uriLIB = "";
        String uriDS="";
        String uriXSI="";
        
        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
            prefixSAML = IFSConstants.ASSERTION_PREFIX;
            prefixSAML_PROTOCOL = IFSConstants.PROTOCOL_PREFIX; 
        }
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) { 
               uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
               uriLIB = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uriSAML = IFSConstants.assertionDeclareStr;
            uriSAML_PROTOCOL = IFSConstants.PROTOCOL_NAMESPACE_STRING;
            uriDS = IFSConstants.DSSAMLNameSpace;
            uriXSI = IFSConstants.XSI_NAMESPACE_STRING;
        }

        String instantString = DateUtils.toUTCDateFormat(issueInstant);

        if((responseID != null) && (inResponseTo != null)){
            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefixLIB)
               .append(IFSConstants.AUTHN_RESPONSE)
               .append(uriLIB)
               .append(uriSAML)
               .append(uriSAML_PROTOCOL)
               .append(IFSConstants.SPACE)
               .append(uriDS)
               .append(IFSConstants.SPACE)
               .append(uriXSI)
               .append(IFSConstants.SPACE)
               .append(IFSConstants.RESPONSE_ID)
               .append(IFSConstants.EQUAL_TO)
               .append(IFSConstants.QUOTE)
               .append(responseID)
               .append(IFSConstants.QUOTE)
               .append(IFSConstants.SPACE);
               
                if ((inResponseTo != null) && (inResponseTo.length() != 0)) {
                    xml.append(IFSConstants.SPACE)
                       .append(IFSConstants.IN_RESPONSE_TO)
                       .append(IFSConstants.EQUAL_TO)
                       .append(IFSConstants.QUOTE)
                       .append(inResponseTo)
                       .append(IFSConstants.QUOTE);
                }
                if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION && 
                    id != null && (id.length() > 0)) {
                        xml.append(IFSConstants.SPACE)
                           .append(IFSConstants.ID)
                           .append(IFSConstants.EQUAL_TO)
                           .append(IFSConstants.QUOTE)
                           .append(id)
                           .append(IFSConstants.QUOTE);
                }
                xml.append(IFSConstants.SPACE)
                   .append(IFSConstants.MAJOR_VERSION) 
                   .append(IFSConstants.EQUAL_TO)
                   .append(IFSConstants.QUOTE)
                   .append(majorVersion)
                   .append(IFSConstants.QUOTE)
                   .append(IFSConstants.SPACE)
                   .append(IFSConstants.MINOR_VERSION) 
                   .append(IFSConstants.EQUAL_TO)
                   .append(IFSConstants.QUOTE)
                   .append(minorVersion)
                   .append(IFSConstants.QUOTE)
                   .append(IFSConstants.SPACE)
                   .append(IFSConstants.ISSUE_INSTANT)
                   .append(IFSConstants.EQUAL_TO)
                   .append(IFSConstants.QUOTE)
                   .append(instantString)
                   .append(IFSConstants.QUOTE);

                if (consentURI != null) {
                    xml.append(IFSConstants.SPACE)
                       .append(IFSConstants.CONSENT)
                       .append(IFSConstants.EQUAL_TO)
                       .append(IFSConstants.QUOTE)
                       .append(consentURI)
                       .append(IFSConstants.QUOTE)
                       .append(IFSConstants.SPACE);
                }
                if ((recipient != null) && (recipient.length() != 0)) {
                    xml.append(IFSConstants.SPACE)
                       .append(IFSConstants.RECIPIENT)
                       .append(IFSConstants.EQUAL_TO)
                       .append(IFSConstants.QUOTE)
                       .append(recipient)
                       .append(IFSConstants.QUOTE)
                       .append(IFSConstants.SPACE);
                }
                xml.append(IFSConstants.RIGHT_ANGLE);
        }

        if (signed) {
            if (signatureString != null && signatureString.length() != 0) {
                xml.append(signatureString);
            } else if (signature != null) {
                signatureString = XMLUtils.print(signature);
                xml.append(signatureString);
            }
        }
        
        if (status != null) {
            xml.append(status.toString(includeNS, false));
        }
        
        if ((assertions != null) && (assertions != Collections.EMPTY_LIST)) {
            Iterator j = assertions.iterator();
            while (j.hasNext()) {
                xml.append(((FSAssertion) j.next())
                                .toXMLString(true,declareNS));
            }
        }
        
        xml.append(IFSConstants.LEFT_ANGLE)
           .append(prefixLIB)
           .append(IFSConstants.PROVIDER_ID)
           .append(IFSConstants.RIGHT_ANGLE)
           .append(providerId)
           .append(IFSConstants.START_END_ELEMENT)
           .append(prefixLIB)
           .append(IFSConstants.PROVIDER_ID)
           .append(IFSConstants.RIGHT_ANGLE);
        
        if (relayState != null && relayState.length() != 0) {
            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefixLIB)
               .append(IFSConstants.RELAY_STATE)
               .append(IFSConstants.RIGHT_ANGLE)
               .append(XMLUtils.escapeSpecialCharacters(relayState))
               .append(IFSConstants.START_END_ELEMENT)
               .append(prefixLIB)
               .append(IFSConstants.RELAY_STATE)
               .append(IFSConstants.RIGHT_ANGLE);
        }     

        xml.append(IFSConstants.START_END_ELEMENT)
           .append(prefixLIB)
           .append(IFSConstants.AUTHN_RESPONSE)
           .append(IFSConstants.RIGHT_ANGLE)
           .append(IFSConstants.NL);

        return xml.toString();
    }
    
   /**
    * Returns <code>FSAutnResponse</code> object by parsing a 
    * <code>Base64</code> encoding XML string.
    *
    *
    * @param encodedRes the <code>Base64</code> encoded string.
    * @return <code>FSAuthnResponse</code> object.
    * @throws FSMsgException if there is an error parsing
    *         the <code>Base64</code> encoded string.
    * @throws SAMLException if there is an error creating
    *         the <code>FSAuthnResponse</code> object.
    */    
    public static FSAuthnResponse parseBASE64EncodedString(String encodedRes)
                                  throws FSMsgException, SAMLException {
        FSUtils.debug.message(
            "FSAuthnResponse.parseBASE64EncodedString: Called new");
        if(encodedRes != null){
            String decodedAuthnRes = new String(Base64.decode(encodedRes));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAuthnResponse.parseBASE64EncodedString: "
                    + "Decoded AuthnResponse message: \n"
                    + decodedAuthnRes);
            }
            return parseAuthnResponseXML(decodedAuthnRes);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAuthnResponse.parseBASE64EncodedString: "
                        + "null String passed in as argument.");
                }
                throw new FSMsgException("nullInput",null);
        }
    }
    
   /**
    * Returns a <code>Base64</code> encoded string representing this
    * object.
    *
    * @return a <code>Base64</code> encoded string representing this
    *         object.
    * @throws FSMsgException if there is an error creating
    *         a <code>Base64</code> encoded string.
    */
    public String toBASE64EncodedString() throws FSMsgException  {
        FSUtils.debug.message("FSAuthnResponse.toBASE64EncodedString: Called");
        if ((responseID == null) || (responseID.length() == 0)){
         responseID = FSUtils.generateID();
            if (responseID == null) {
                FSUtils.debug.error("FSAuthnResponse.toBASE64EncodedString: "
                    + "couldn't generate ResponseID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString(true, true).getBytes());
    } 
    
    /**
     * Signs the <code>Response</code>.
     *
     * @param certAlias the Certificate Alias
     * @throws SAMLException if <code>Response</code>
     *         cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSAuthnResponse.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAuthnResponse.signXML: the assertion is "
                    + "already signed.");
            }
            throw new SAMLResponderException(
                                  FSUtils.BUNDLE_NAME,"alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(
                FSUtils.BUNDLE_NAME,"cannotFindCertAlias",null);
        }
        try{
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true), 
                                          certAlias, 
                                          IFSConstants.DEF_SIG_ALGO, 
                                          IFSConstants.ID, 
                                          this.id, false);
            } else if (minorVersion == 
                          IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                  signatureString = manager.signXML(
                                         this.toXMLString(true, true), 
                                         certAlias, IFSConstants.DEF_SIG_ALGO,
                                         IFSConstants.RESPONSE_ID, 
                                         this.getResponseID(), false);
            } else { 
                if (FSUtils.debug.messageEnabled()) { 
                    FSUtils.debug.message("invalid minor version.");                 
                }
            }  
            signature = XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
                                               .getDocumentElement();
            signed = true;
            xmlString = this.toXMLString(true, true);      
        } catch(Exception e) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                                             "signFailed",null);
        }
    }
    
   /**
    * Sets the <code>Element</code> signature.
    *
    * @param elem the <code>Element</code> object
    * @return true if signature is set otherwise false
    */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem); 
        return super.setSignature(elem); 
    }
}
