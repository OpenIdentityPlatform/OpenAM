/*
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
 * $Id: FSNameRegistrationRequest.java,v 1.4 2008/06/25 05:46:44 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.federation.message;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.federation.message.common.OldProvidedNameIdentifier;
import com.sun.identity.federation.message.common.SPProvidedNameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.protocol.AbstractRequest;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.text.ParseException;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;


/**
 * This class contains methods to create <code>NameRegistrationRequest</code>
 * object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSNameRegistrationRequest extends AbstractRequest {
    private String providerId;
    private SPProvidedNameIdentifier spProvidedNameIdentifier;
    private IDPProvidedNameIdentifier idpProvidedNameIdentifier;
    private OldProvidedNameIdentifier oldProvidedNameIdentifier;
    private String relayState = "";
    protected String xmlString;
    protected String signatureString;
    protected String id;
    protected int minorVersion = 0;

    /** 
     * Default Constructor.
     */
    
    public FSNameRegistrationRequest() { 
        setIssueInstant(newDate());
    }
        
    /**
     * Returns the value of <code>id</code> attribute.
     *
     * @return the value of <code>id</code> attribute.
     * @see #setID(String)
     */
    public String getID(){
        return id;
    }
    
    /**
     * Sets the value of <code>id</code> attribute.
     *
     * @param id the value of <code>id</code> attribute.
     * @see #getID()
     */
    public void setID(String id){
        this.id = id;
    }
    /**
     * Returns the value of <code>RelayState</code> attribute.
     *
     * @return the value of <code>RelayState</code> attribute.
     * @see #setRelayState(String)
     */
    
    public String getRelayState(){
        return relayState;
    }
    
    /**
     * Set the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     * @see #getRelayState()
     */
    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

   
    /**
     * Returns the signed <code>XML</code> string.
     *
     * @return the signed <code>XML</code> string.
     */
    public String getSignatureString(){
        return signatureString;
    }


    /**
     * Constructor creates the <code>FSNameRegistrationRequest</code>
     * object.
     *
     * @param requestId the value of <code>RequestID</code> attribute.
     * @param respondWiths the value of <code>RespondWiths</code> attribute.
     * @param providerId the value of <code>ProviderID</code> attribute.
     * @param spProvidedNameIdentifier the Service Provider 
     *        <code>NameIdentifier</code>.
     * @param idpProvidedNameIdentifier the Identity Provider 
     *        <code>NameIdentifier</code>.
     * @param oldProvidedNameIdentifier the Original Provider
     *        <code>NameIdentifier</code>.
     * @param relayState the value of <code>RelayState</code> attribute.
     * @throws FSMsgException if there is an error creating this object.
     */
    public FSNameRegistrationRequest(
        String requestId,
        List respondWiths,
        String providerId, 
        SPProvidedNameIdentifier spProvidedNameIdentifier,
        IDPProvidedNameIdentifier idpProvidedNameIdentifier,
        OldProvidedNameIdentifier oldProvidedNameIdentifier,
        String relayState) throws FSMsgException {
     
        int length = 0;
        setIssueInstant(newDate());
        if ((respondWiths != null) &&
            (respondWiths != Collections.EMPTY_LIST)) {
            length = respondWiths.size();
            for (int i = 0; i < length; i++) {
                Object temp = respondWiths.get(i);
                if (!(temp instanceof String)) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("NameRegistrationRequest: "
                            + "wrong input for RespondWith");
                    }
                    throw new FSMsgException("wrongInput", null);
                }
            }
            this.respondWiths = respondWiths;
        }
     
        if ((requestId != null) && (requestId.length() != 0)) {
            requestID = requestId;
        } else {
            // random generate one
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSNameRegistrationRequest: "
                    + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID", null);
            }
        }
        this.providerId=providerId;
        this.spProvidedNameIdentifier=spProvidedNameIdentifier;
        this.idpProvidedNameIdentifier=idpProvidedNameIdentifier;
        this.oldProvidedNameIdentifier=oldProvidedNameIdentifier;
        this.relayState = relayState;
    }
    
    /**
     * Constructor creates <code>FSNameRegistrationRequest</code>> object
     * from a Document Element.
     *
     * @param root the Document Element.
     * @throws FSMsgException if there is an error creating
     *         this object.
     */
    public FSNameRegistrationRequest(Element root) throws FSMsgException {        
        String tag = null;
        if (root == null) {
            FSUtils.debug.message(
                "FSNameRegistrationRequest(Element): null input.");
            throw new FSMsgException("nullInput",null);
        }
        if (((tag = root.getLocalName()) == null) ||
        (!tag.equals("RegisterNameIdentifierRequest"))) {
            FSUtils.debug.message(
                "FSNameRegistrationRequest(Element): wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        
        // Attribute IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if ((instantString == null) || (instantString.length() == 0)) {
             FSUtils.debug.error("FSNameRegistrationRequest(Element):" +
             "missing IssueInstant");
             String[] args = { IFSConstants.ISSUE_INSTANT };
             throw new FSMsgException("missingAttribute",args);
        } else {
             try {
                 issueInstant = DateUtils.stringToDate(instantString);
             } catch (ParseException e) {
                 FSUtils.debug.error(
                    "FSNameRegistrationRequest(Element): " +
                    "could not parse IssueInstant" , e);
                 throw new FSMsgException("wrongInput",null);
             }
        }
        
        int length = 0;
        id = root.getAttribute("id");
        requestID = root.getAttribute("RequestID");
        parseMajorVersion(root.getAttribute("MajorVersion"));
        parseMinorVersion(root.getAttribute("MinorVersion"));
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        length = contentnl.getLength();
        for (int i = 0; i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals("RespondWith")) {
                    if (respondWiths == Collections.EMPTY_LIST) {
                        respondWiths = new ArrayList();
                    }
                    respondWiths.add(
                    XMLUtils.getElementValue((Element) child));
                } else if (nodeName.equals(IFSConstants.SIGNATURE)) {
                } else if (nodeName.equals("ProviderID")) {
                    if (providerId != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSNameRegistrationRequest(Element): "
                                + "should contain only one ProviderID.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                }  else if (nodeName.equals("SPProvidedNameIdentifier")) {
                    spProvidedNameIdentifier = 
                        new SPProvidedNameIdentifier((Element) child);
                } else if (nodeName.equals("IDPProvidedNameIdentifier")) {
                    idpProvidedNameIdentifier = 
                        new IDPProvidedNameIdentifier((Element) child);
                } else if (nodeName.equals("OldProvidedNameIdentifier")) {
                    oldProvidedNameIdentifier = 
                        new OldProvidedNameIdentifier((Element) child);
                }else if (nodeName.equals("RelayState")) {
                    relayState = XMLUtils.getElementValue((Element) child);
                }else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSNameRegistrationRequest(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            }
        }
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
            FSUtils.debug.error("FSNameRegistrationRequest(Element): " +
            "included more than one Signature element.");
            throw new FSMsgException( "moreElement",null);
        }        
        //end check for signature
    }
    
    /**
     * Returns the <code>MinorVersion</code>.
     *
     * @return the <code>MinorVersion</code>.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
       return minorVersion;
    }
    
    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param version the <code>MinorVersion</code>.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int version) {
       minorVersion = version;
    }

    public static FSNameRegistrationRequest parseXML(String xml)
        throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            FSUtils.debug.error("FSNameRegistrationRequest.parseXML:Error " +
            "while parsing input xml string");
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSNameRegistrationRequest(root);
    }

    /**
     * Returns a String representation of the Logout Response.
     *
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }

    /**
     * Returns a String representation of the Logout Response.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Returns a String representation of the Logout Response.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *        this object ot a string.
     */
    public String toXMLString(boolean includeNS,boolean declareNS,
        boolean includeHeader) throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSNameRegistrationRequest.toXMLString: "
                + "providerId is null in the request with requestId:" 
                + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSNameRegistrationRequest.toXMLString: "
                    + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        
        StringBuffer xml = new StringBuffer(1000);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
            append(IFSConstants.DEFAULT_ENCODING).append("\" ?>\n");
        }
        String prefix = "";
        String uri = "";
        String uriSAML = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uriSAML = IFSConstants.assertionDeclareStr;
        }

        String instantString = DateUtils.toUTCDateFormat(issueInstant);

        if(requestID != null){
           xml.append("<").append(prefix).
           append("RegisterNameIdentifierRequest").
           append(uri).append(uriSAML);
           if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION && 
              id != null && !(id.length() == 0)){
              xml.append(" id=\"").append(id).append("\" ");
           }
           xml.append(" RequestID=\"").append(requestID).append("\" ").
               append(" MajorVersion=\"").append(majorVersion).append("\" ").
               append(" MinorVersion=\"").append(minorVersion).append("\" ").
               append(" IssueInstant=\"").append(instantString).append("\"").
               append(">");
           if((respondWiths != null) && 
               (respondWiths != Collections.EMPTY_LIST)) {
                Iterator i = respondWiths.iterator();
                while (i.hasNext()) {
                    xml.append("<").append(prefix).append("RespondWith>").
                    append((String) i.next()).append("</").append(prefix).
                    append("RespondWith>");
                }
            }
            if (signed) {
                if (signatureString != null) {
                    xml.append(signatureString);
                } else if (signature != null) {
                    signatureString = XMLUtils.print(signature);
                    xml.append(signatureString);
                }
            }
            
            xml.append("<").append(prefix).append("ProviderID").append(">").
            append(providerId).
            append("</").append(prefix).append("ProviderID").append(">");
            if(idpProvidedNameIdentifier != null && 
               idpProvidedNameIdentifier.getName().length() != 0) {
               xml.append(idpProvidedNameIdentifier.toXMLString());            
            }
            if(spProvidedNameIdentifier != null && 
               spProvidedNameIdentifier.getName().length() != 0) {
               xml.append(spProvidedNameIdentifier.toXMLString());
            }
            if(oldProvidedNameIdentifier != null && 
               oldProvidedNameIdentifier.getName().length() != 0) {
                xml.append(oldProvidedNameIdentifier.toXMLString());
            }
            if(relayState != null) {
               xml.append("<").append(prefix).append("RelayState").append(">").
                   append(relayState).append("</").append(prefix).
                   append("RelayState").append(">");
            }
            xml.append("</").append(prefix).
                append("RegisterNameIdentifierRequest>");
        } else {
            FSUtils.debug.error("FSNameRegistrationRequest.toString: " +
            "requestID is null ");
            throw new FSMsgException("nullRequestID",null);
        }
        
        return xml.toString();
    }

    /**
     * Returns the Identity Provider's <code>NameIdentifier</code>.
     *
     * @return the Identity Provider's <code>NameIdentifier</code>.
     */
    public IDPProvidedNameIdentifier getIDPProvidedNameIdentifier() {
        return idpProvidedNameIdentifier;
    }
    
    /**
     * Returns the original <code>NameIdentifier</code>.
     *
     * @return the original <code>NameIdentifier</code>.
     */
    public OldProvidedNameIdentifier getOldProvidedNameIdentifier() {
        return oldProvidedNameIdentifier;
    }
    
    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     * @see #setProviderId(String).
     */
    public String getProviderId() {
        return this.providerId;
    }

    /**
     * Sets the Identity Provider's <code>NameIdentifier</code>.
     *
     * @param nameIdentifier the Identity Provider's
     *        <code>NameIdentifier</code>.
     * @see #getIDPProvidedNameIdentifier
     */
    public void setIDPProvidedNameIdentifier(
        IDPProvidedNameIdentifier nameIdentifier) {
        idpProvidedNameIdentifier=nameIdentifier;
    }
    
    /**
     * Sets the original <code>NameIdentifier</code>.
     *
     * @param nameIdentifier the original provider's
     *        <code>NameIdentifier</code>.
     * @see #getOldProvidedNameIdentifier
     */
    
    public void setOldProvidedNameIdentifier(
        OldProvidedNameIdentifier nameIdentifier) {
        oldProvidedNameIdentifier=nameIdentifier;
    }
    /**
     * Sets the Service Provider's <code>NameIdentifier</code>.
     *
     * @param nameIdentifier the Identity Provider's
     *        <code>NameIdentifier</code>.
     * @see #getSPProvidedNameIdentifier
     */
    public void setSPProvidedNameIdentifier(
            SPProvidedNameIdentifier nameIdentifier) {
        spProvidedNameIdentifier=nameIdentifier;
    }

    /**
     * Sets the value of <code>ProviderID</code> attribute.
     *
     * @param providerId the value of <code>ProviderID</code> attribute.
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * Returns the <code>NameIdentifier</code> provided by
     * the Service Provider.
     *
     * @return the <code>NameIdentifier</code> provided by
     *         the Service Provider.
     * @see #setSPProvidedNameIdentifier(SPProvidedNameIdentifier)
     */
    public SPProvidedNameIdentifier getSPProvidedNameIdentifier() {
        return spProvidedNameIdentifier;
    }

    /**
     * Returns a Base64 Encoded String.
     *
     * @return a Base64 Encoded String.
     * @throws FSMsgException if there is an error encoding the string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if ((providerId == null) || (providerId.length() == 0)) {
            FSUtils.debug.error(
                "FSNameRegistrationRequest.toBASE64EncodedString: "
                + "providerId is null in the request with requestId:" 
                + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error(
                    "FSNameRegistrationRequest.toBASE64EncodedString: "
                    + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());        
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException on error.
     */
    private void parseMajorVersion(String majorVer) throws FSMsgException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameRegistrationRequest(Element): "
                    + "invalid MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationRequest(Element): "
                        + "MajorVersion of the RegisterNameIdentifierRequest"
                        + "is too high.");
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationRequest(Element): "
                        + "MajorVersion of the RegisterNameIdentifierRequest"
                        + "is too low.");
                }
                throw new FSMsgException("requestVersionTooLow",null);
            }
        }
    }
    
    /**
     * Sets the <code>MinorVersion</code> by parsing the version string.
     *
     * @param minorVer a String representing the <code>MinorVersion</code> to
     *        be set.
     * @throws SAMLException when the version mismatchs.
     */
    private void parseMinorVersion(String minorVer) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                "FSNameRegis(Element): "
                + "invalid MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }

        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSNameRegisNot(Element):MinorVersion of"
            + " the Response is too high.");
            throw new FSMsgException("responseVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSNameRegis(Element):MinorVersion of"
            + " the Response is too low.");
            throw new FSMsgException("responseVersionTooLow",null);
        }
    }


    public void signXML() {

    }
    /**
     * Signs the <code>FSNameRegistrationRequest</code> object.
     *
     * @param certAlias the Certificate Alias.
     * @throws SAMLException if this object cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSNameRegistrationRequest.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameRegistrationRequest.signXML: "
                    + "the assertion is already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                                             "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                "cannotFindCertAlias",null);
        }
        try{
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                     signatureString = manager.signXML(
                                         this.toXMLString(true, true), 
                                         certAlias,null,IFSConstants.ID, 
                                         this.id, false);
            } else if (minorVersion == 
                                   IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                    signatureString = manager.signXML(
                                         this.toXMLString(true, true), 
                                         certAlias,null,IFSConstants.REQUEST_ID, 
                                         this.getRequestID(), false);
            } else { 
                    if (FSUtils.debug.messageEnabled()) { 
                        FSUtils.debug.message("invalid minor version.");                 
                    }
            }
                 
            signature = 
                XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
                        .getDocumentElement();
            signed = true;
            xmlString = this.toXMLString(true, true);      
        } catch(Exception e){
            throw new SAMLResponderException(
                                  FSUtils.BUNDLE_NAME,"signFailed",null);
        }
    }

    /**
     * Sets the Signature.
     *
     * @param elem the Document Element.
     * @return true if success otherwise false.
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem); 
        return super.setSignature(elem); 
    } 
        
    /**
     * Returns an URL Encoded String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)) {
            FSUtils.debug.error("FSNameRegistrationRequest." +
            "toURLEncodedQueryString: providerId is null in the request " +
            "with requestId: " + requestID);
            throw new FSMsgException("nullProviderID",null);
         }
         if((requestID == null) || (requestID.length() == 0)){
             requestID = SAMLUtils.generateID();
             if(requestID == null) {
                 FSUtils.debug.error("FSNameRegistrationRequest." +
                       "toURLEncodedQueryString: couldn't generate RequestID.");
                 throw new FSMsgException("errorGenerateID",null);
             }
         }
         StringBuffer urlEncodedAuthnReq = new StringBuffer(300);
         urlEncodedAuthnReq.append("RequestID=").
                 append(URLEncDec.encode(requestID)).
                 append(IFSConstants.AMPERSAND);
         urlEncodedAuthnReq.append("MajorVersion=").
                 append(majorVersion).
                 append(IFSConstants.AMPERSAND);
         urlEncodedAuthnReq.append("MinorVersion=").
                 append(minorVersion).
                 append(IFSConstants.AMPERSAND);
         urlEncodedAuthnReq.append("RelayState=").
                 append(URLEncDec.encode(relayState)).
                 append(IFSConstants.AMPERSAND);
         
         if (issueInstant != null){
             urlEncodedAuthnReq.append("IssueInstant=")
                               .append(URLEncDec.encode(
                                      DateUtils.toUTCDateFormat(issueInstant)))
                               .append(IFSConstants.AMPERSAND);
         } else {
             FSUtils.debug.error("FSNameRegistrationRequest."
                     + "toURLEncodedQueryString: issueInstant missing");
             String[] args = { IFSConstants.ISSUE_INSTANT };
             throw new FSMsgException("missingAttribute",args);
         }
         if (providerId != null && providerId.length() > 0) {
             urlEncodedAuthnReq.append("ProviderID=").
                     append(URLEncDec.encode(providerId)).
                     append(IFSConstants.AMPERSAND);
         }
    
         if(spProvidedNameIdentifier != null) {
             if (spProvidedNameIdentifier.getName() != null &&
                     spProvidedNameIdentifier.getName().length() != 0) {
                 urlEncodedAuthnReq.append("SPProvidedNameIdentifier=").
                         append(URLEncDec.encode(
                         spProvidedNameIdentifier.getName())).
                         append(IFSConstants.AMPERSAND);
             }

             if(spProvidedNameIdentifier.getNameQualifier() != null &&
                    spProvidedNameIdentifier.getNameQualifier().length() != 0) {
                 urlEncodedAuthnReq.append("SPNameQualifier=").
                         append(URLEncDec.encode(
                         spProvidedNameIdentifier.getNameQualifier())).
                         append(IFSConstants.AMPERSAND);
             }
              if (spProvidedNameIdentifier.getFormat() != null && 
                 spProvidedNameIdentifier.getFormat().length() != 0) {
                 urlEncodedAuthnReq.append("SPNameFormat=").
                                    append(URLEncDec.encode(
                                         spProvidedNameIdentifier.getFormat())).
                                    append(IFSConstants.AMPERSAND); 
              }
         } 

         if (oldProvidedNameIdentifier != null) {
            if (oldProvidedNameIdentifier.getName() != null && 
               oldProvidedNameIdentifier.getName().length() != 0) {
               urlEncodedAuthnReq.append("OldProvidedNameIdentifier=").
                                  append(URLEncDec.encode(
                                      oldProvidedNameIdentifier.getName())).
                                  append(IFSConstants.AMPERSAND);
            }
            if (oldProvidedNameIdentifier.getNameQualifier() != null && 
                oldProvidedNameIdentifier.getNameQualifier().length() != 0) { 
                urlEncodedAuthnReq.append("OldNameQualifier=").
                                   append(URLEncDec.encode(
                                 oldProvidedNameIdentifier.getNameQualifier())).
                                   append(IFSConstants.AMPERSAND);  
            }
            if (oldProvidedNameIdentifier.getFormat() != null&& 
                         oldProvidedNameIdentifier.getFormat().length() != 0) {
                urlEncodedAuthnReq.append("OldNameFormat=").
                                   append(URLEncDec.encode(
                                        oldProvidedNameIdentifier.getFormat())).
                                   append(IFSConstants.AMPERSAND); 
            }
         } 

         if (idpProvidedNameIdentifier != null) {
            if (idpProvidedNameIdentifier.getName() != null && 
                idpProvidedNameIdentifier.getName().length() != 0){
                urlEncodedAuthnReq.append("IDPProvidedNameIdentifier=").
                                   append(URLEncDec.encode(
                                         idpProvidedNameIdentifier.getName())).
                                   append(IFSConstants.AMPERSAND);
            }
            if (idpProvidedNameIdentifier.getNameQualifier() != null && 
                idpProvidedNameIdentifier.getNameQualifier().length() != 0) {
                urlEncodedAuthnReq.append("IDPNameQualifier=").
                                   append(URLEncDec.encode(
                                idpProvidedNameIdentifier.getNameQualifier())).
                                   append(IFSConstants.AMPERSAND);  
            }
            if(idpProvidedNameIdentifier.getFormat() != null&& 
                idpProvidedNameIdentifier.getFormat().length() != 0) {
                urlEncodedAuthnReq.append("IDPNameFormat=").
                                   append(URLEncDec.encode(
                                        idpProvidedNameIdentifier.getFormat())).
                                   append(IFSConstants.AMPERSAND); 
            }
         } 
         return urlEncodedAuthnReq.toString();   
   }
   
    /**
     * Returns <code>FSNameRegistrationRequest</code> object. The
     * object is creating by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @throws FSMsgException if there is an error
     *         creating this object.
     * @throws SAMLException if there is an error.
     */
    public static FSNameRegistrationRequest parseURLEncodedRequest(
            HttpServletRequest request) throws FSMsgException, SAMLException {
        FSNameRegistrationRequest retNameRegistrationRequest =
                new FSNameRegistrationRequest();
        try {
            FSUtils.debug.message("checking minor version");
            retNameRegistrationRequest.majorVersion =
                    Integer.parseInt(request.getParameter("MajorVersion"));
            retNameRegistrationRequest.minorVersion =
                    Integer.parseInt(request.getParameter("MinorVersion"));
        } catch(NumberFormatException ex){
            FSUtils.debug.error("FSNameRegistrationRequest.parseURLEncoded" +
                    "Request: Invalid versions", ex);
            throw new FSMsgException("invalidNumber",null);
        }
        FSUtils.debug.message("checking RequestID");
        if(request.getParameter("RequestID")!= null) {
            retNameRegistrationRequest.requestID =
                    request.getParameter("RequestID");
        } else {
            FSUtils.debug.error("FSNameRegistrationRequest.parseURLEncoded" +
                    "Request: RequestID not found");
            String[] args = { IFSConstants.REQUEST_ID };
            throw new FSMsgException("missingAttribute",args);
        }
        FSUtils.debug.message("checking instantString");
        String instantString = request.getParameter("IssueInstant");
        FSUtils.debug.message("instantString : " + instantString);
        if(instantString == null ||
                instantString.length() == 0) {
            FSUtils.debug.error("FSNameRegistrationRequest.parseURLEncoded" +
                    "Request: IssueInstant not found");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        try{
            FSUtils.debug.message(
                    "calling : DateUtils.stringToDate.issueInstant");
            retNameRegistrationRequest.issueInstant =
                    DateUtils.stringToDate(instantString);
        } catch (ParseException e){
            FSUtils.debug.error("FSNameRegistrationRequest.parseURLEncoded" +
                    "Request: Can not parse IssueInstant", e);
            throw new FSMsgException("parseError",null);
        }
        
        if(request.getParameter("ProviderID")!= null){
            retNameRegistrationRequest.providerId =
                    request.getParameter("ProviderID");
        } else {
            FSUtils.debug.error("FSNameRegistrationRequest.parseURLEncoded" +
                    "Request: Can not find ProviderID");
            throw new FSMsgException("missingElement",null);
        }
        FSUtils.debug.message("start identifier processing");
        String spNameFormat = "";
        String spNameQualifier = "";
        String spName = "";
        
        if(request.getParameter("SPNameFormat") != null) {
            spNameFormat = request.getParameter("SPNameFormat");
        }
        
        if(request.getParameter("SPNameQualifier") != null) {
            spNameQualifier = request.getParameter("SPNameQualifier");
        }
        
        if(request.getParameter("SPProvidedNameIdentifier") != null) {
            spName = request.getParameter("SPProvidedNameIdentifier");
        }
        if(spName != null &&  !(spName.length() < 1)) {
            retNameRegistrationRequest.setSPProvidedNameIdentifier(
                new SPProvidedNameIdentifier(spName, spNameQualifier, 
                                             spNameFormat));
        }
        
        String idpNameFormat = null;
        String idpNameQualifier = null;
        String idpName = null;
        
        if (request.getParameter("IDPNameFormat") != null) {
            idpNameFormat = request.getParameter("IDPNameFormat");
        }
        
        if (request.getParameter("IDPNameQualifier") != null) {
            idpNameQualifier = request.getParameter("IDPNameQualifier");
        }
        
        if (request.getParameter("IDPProvidedNameIdentifier") != null) {
            idpName = request.getParameter("IDPProvidedNameIdentifier");
        }
        if (idpName != null && !(idpName.length() < 1)) {
            retNameRegistrationRequest.idpProvidedNameIdentifier =
                    new IDPProvidedNameIdentifier(idpName, idpNameQualifier,
                    idpNameFormat);
        }
        
        String oldNameFormat = null;
        String oldNameQualifier = null;
        String oldName = null;
        
        if (request.getParameter("OldNameFormat") != null) {
            oldNameFormat = request.getParameter("OldNameFormat");
        }
        
        if (request.getParameter("OldNameQualifier") != null) {
            oldNameQualifier = request.getParameter("OldNameQualifier");
        }
        
        if (request.getParameter("OldProvidedNameIdentifier") != null) {
            oldName = request.getParameter("OldProvidedNameIdentifier");
        }
        
        if (oldName != null && !(oldName.length() < 1)) {
            retNameRegistrationRequest.oldProvidedNameIdentifier =
                    new OldProvidedNameIdentifier(oldName, oldNameQualifier,
                    oldNameFormat);
        }
        
        if(request.getParameter("RelayState") != null) {
            retNameRegistrationRequest.relayState =
                    request.getParameter("RelayState");
        }
        return retNameRegistrationRequest;
    }
}
