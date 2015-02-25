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
 * $Id: FSAuthnRequest.java,v 1.4 2008/07/08 06:03:37 exu Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.Extension;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.RequestAuthnContext;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.AbstractRequest;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>FSAuthnRequest</code> is used to create , parse
 * <code>AuthnRequest</code> object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSAuthnRequest extends AbstractRequest {
    private List extensions = null;
    private boolean isPassive = false;
    private boolean forceAuthn = false;
    private boolean federate = false;
    private String nameIDPolicy = null;
    private String protocolProfile = null;
    private String providerId = null;
    private RequestAuthnContext authnContext = null;
    private String relayState = null;
    protected String xmlString = null;
    protected String signatureString = null;
    protected String authContextCompType = null;
    protected String id = null;
    protected String assertionConsumerServiceID = null;
    protected String consentURI = null;
    protected String affiliationID = null;
    protected int minorVersion = 0;
    protected FSScoping scoping = null;
    private static final String QUERY_STRING_EXTENSION_PREFIX = "AE_";    
    /**
     * Default AuthnRequest construtor
     */
    public FSAuthnRequest() {
        setIssueInstant(new Date());
    }
    
    /**
     * Constructor to create <code>FSAuthnRequest</code> object.
     *
     * @param requestId the request identifier.
     * @param respondWiths List of respond withs attributes.
     * @param providerID provider id of the requesting provider.
     * @param forceAuthn Force Authentication boolean value.
     * @param isPassive attribute for IDP to be passive or active.
     * @param fed attribute to distingush this request for Federation or SSO
     * @param nameIDPolicy Name ID Policy for this request, possible values
     *                     are "none", "onetime", "federated", "any".
     * @param protocolProf ProtocolProfile used for the SSO.
     * @param authnCxt Authentication Context used for the SSO.
     * @param relaySt Relay State i.e. original URL to be redirected after SSO.
     * @param authContextCompType AuthContext comparison type.
     * @throws <code>FSMsgException</code> on error.
     */
    public FSAuthnRequest(String requestId,
            List respondWiths,
            String providerID,
            boolean forceAuthn,
            boolean isPassive,
            boolean fed,
            String nameIDPolicy,
            String protocolProf,
            RequestAuthnContext authnCxt,
            String relaySt,
            String authContextCompType)
            throws FSMsgException {
        
        setIssueInstant(new Date());
        if((respondWiths != null) && (respondWiths != Collections.EMPTY_LIST)) {
            int length = respondWiths.size();
            for(int i = 0; i < length; i++) {
                Object temp = respondWiths.get(i);
                if(!(temp instanceof String)) {
                    FSUtils.debug.error("FSAuthnRequest: wrong input for " +
                            "RespondWith");
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
                FSUtils.debug.error("FSAuthnRequest: couldn't gen RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        this.isPassive = isPassive;
        this.forceAuthn = forceAuthn;
        this.providerId = providerID;
        this.federate = fed;
        this.nameIDPolicy = nameIDPolicy;
        this.protocolProfile = protocolProf;
        this.relayState = relaySt;
        this.authnContext = authnCxt;
        this.authContextCompType = authContextCompType;
        id = requestID;
    }
    
    /**
     * Constructor to create <code>FSAuthnRequest</code> object.
     *
     * @param root the Document Element object.
     * @throws <code>FSMsgException</code> on error.
     */
    public FSAuthnRequest(Element root) throws FSMsgException {
        String tag = null;
        if (root == null) {
            FSUtils.debug.error("FSAuthnRequest(Element): null input.");
            throw new FSMsgException("nullInput",null);
        }
        if(((tag = root.getLocalName()) == null) ||
                (!tag.equals(IFSConstants.AUTHN_REQUEST))) {
            FSUtils.debug.error("FSAuthnRequest(Element): wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        // Attribute IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.error("FSAuthnRequest(Element): " 
                                 + "missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                FSUtils.debug.error("FSAuthnRequest(Element): "
                 + "could not parse IssueInstant", e);
                throw new FSMsgException("wrongInput", null);
            }
        }
        // Consent attribute
        consentURI = root.getAttribute(IFSConstants.CONSENT);
        
        id = root.getAttribute(IFSConstants.ID);
        requestID = root.getAttribute(IFSConstants.AUTH_REQUEST_ID);
        parseMajorVersion(root.getAttribute(IFSConstants.MAJOR_VERSION));
        parseMinorVersion(root.getAttribute(IFSConstants.MINOR_VERSION));
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        int length = contentnl.getLength();
        for(int i = 0; i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals(IFSConstants.RESPONDWITH)) {
                    if (respondWiths == Collections.EMPTY_LIST) {
                        respondWiths = new ArrayList();
                    }
                    respondWiths.add(XMLUtils.getElementValue((Element) child));
                } else if (nodeName.equals(IFSConstants.PROVIDER_ID)) {
                    if(providerId != null && providerId.length() != 0) {
                        FSUtils.debug.error("FSAuthnRequest(Element): should"
                                + "contain only one ProviderID.");
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                } else if(nodeName.equals(IFSConstants.NAMEID_POLICY_ELEMENT)) {
                    nameIDPolicy=XMLUtils.getElementValue((Element) child);
                    
                    if (nameIDPolicy != null &&
                            (nameIDPolicy.equals(
                            IFSConstants.NAME_ID_POLICY_FEDERATED) ||
                            nameIDPolicy.equals(
                            IFSConstants.NAME_ID_POLICY_ONETIME))
                            ) {
                        federate = true;
                    }
                } else if (nodeName.equals(IFSConstants.FEDERATE)) {
                    String strFederate = 
                             XMLUtils.getElementValue((Element)child);
                    if(strFederate != null && strFederate.length() != 0 &&
                            strFederate.equals(IFSConstants.TRUE)
                                    || strFederate.equals(IFSConstants.ONE)) {
                        federate = true;
                    }
                } else if (nodeName.equals(IFSConstants.IS_PASSIVE_ELEM)) {
                    String strIsPassive =
                            XMLUtils.getElementValue((Element) child);
                    if(strIsPassive != null && strIsPassive.length() != 0 &&
                            strIsPassive.equals(IFSConstants.TRUE)) {
                        isPassive = true;
                    } else {
                        isPassive = false;
                    }
                } else if (nodeName.equals(IFSConstants.FORCE_AUTHN_ELEM)) {
                    String strForceAuthn =
                            XMLUtils.getElementValue((Element) child);
                    if(strForceAuthn != null && strForceAuthn.length() != 0 &&
                            strForceAuthn.equals(IFSConstants.TRUE)) {
                        forceAuthn = true;
                    } else {
                        forceAuthn = false;
                    }
                } else if (nodeName.equals(IFSConstants.PROTOCOL_PROFILE)) {
                    if(protocolProfile != null 
                            && protocolProfile.length() != 0) {
                        FSUtils.debug.error("FSAuthnRequest(Element): "
                                + "should contain only one ProtocolProfile.");
                        throw new FSMsgException("wrongInput",null);
                    }
                    protocolProfile = XMLUtils.getElementValue((Element) child);
                    
                } else if (nodeName.equals(IFSConstants.AUTHN_CONTEXT)) {
                    authnContext = new RequestAuthnContext((Element) child);
                    
                } else if (nodeName.equals(
                                   IFSConstants.REQUEST_AUTHN_CONTEXT)) {
                    authnContext = new RequestAuthnContext((Element) child);
                    
                } else if (nodeName.equals(IFSConstants.RELAY_STATE)) {
                    relayState = XMLUtils.getElementValue((Element) child);
                    
                } else if (nodeName.equals(
                                 IFSConstants.AUTHN_CONTEXT_COMPARISON)) {
                    authContextCompType =
                            XMLUtils.getElementValue((Element) child);
                    if(!(authContextCompType.equals(IFSConstants.MINIMUM) ||
                            authContextCompType.equals(IFSConstants.EXACT) ||
                            authContextCompType.equals(IFSConstants.MAXIMUM) ||
                            authContextCompType.equals(IFSConstants.BETTER)) ) {
                        throw new FSMsgException("wrongInput",null);
                    }
                } else if (nodeName.equals(
                             IFSConstants.ASSERTION_CONSUMER_SVC_ID)) {
                    assertionConsumerServiceID =
                            XMLUtils.getElementValue((Element) child);
                } else if(nodeName.equals(IFSConstants.AFFILIATIONID)) {
                    affiliationID = XMLUtils.getElementValue((Element) child);
                } else if(nodeName.equals(IFSConstants.EXTENSION)) {
                    if (extensions == null) {
                        extensions = new ArrayList();
                    }
                    extensions.add(new Extension((Element)child));
                } else if(nodeName.equals(IFSConstants.SCOPING)) {
                    scoping = new FSScoping((Element)child);
                } else {
                    FSUtils.debug.error("FSAuthnRequest(Element): invalid"
                            + " node" + nodeName);
                    throw new FSMsgException("wrongInput",null);
                }
            }
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
            FSUtils.debug.error("FSAuthnRequest(Element): "
                    + "included more than one Signature element.");
            throw new FSMsgException("moreElement",null);
        }
        //end check for signature
    }
    
    /**
     * This method translates the request to an XML document String based on
     * the Request schema described above.
     * NOTE: this is a complete AuthnRequest xml string with RequestID,
     * MajorVersion, etc.
     *
     * @return XML String representing the request.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Creates a String representation of the &lt;lib:AuthnRequest&gt; element.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *          is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *          within the Element.
     * @return string containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(
            boolean includeNS, boolean declareNS
            ) throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Creates a String representation of the &lt;lib:AuthnRequest&gt; element.
     *
     * @param includeNS  Determines whether or not the namespace qualifier
     *          is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *          within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return A string containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(boolean includeNS,
            boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        if (xmlString != null) {
            return xmlString;
        }
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSAuthnRequest.toXMLString: "
                    + "providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSAuthnRequest.toXMLString: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                    append(IFSConstants.DEFAULT_ENCODING).append("\" ?>");
        }
        String prefix = "";
        String samlpPrefix = "";
        String uri = "";
        String samlpUri = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
            samlpPrefix = IFSConstants.PROTOCOL_PREFIX;
        }
        if (declareNS) {
            if(minorVersion ==  IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
            samlpUri = IFSConstants.PROTOCOL_NAMESPACE_STRING;
        }
        
        String instantString = DateUtils.toUTCDateFormat(issueInstant);
        
        if (requestID != null) {
            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefix)
               .append(IFSConstants.AUTHN_REQUEST)
               .append(uri)
               .append(IFSConstants.SPACE)
               .append(samlpUri);
 
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION &&
                    id != null && !(id.length() == 0)){
                xml.append(IFSConstants.SPACE)
                   .append(IFSConstants.ID)
                   .append(IFSConstants.EQUAL_TO)
                   .append(IFSConstants.QUOTE)
                   .append(id)
                   .append(IFSConstants.QUOTE);
            }
            xml.append(IFSConstants.SPACE)
               .append(IFSConstants.REQUEST_ID)
               .append(IFSConstants.EQUAL_TO)
               .append(IFSConstants.QUOTE)
               .append(requestID)
               .append(IFSConstants.QUOTE)
               .append(IFSConstants.SPACE)
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
                   .append(IFSConstants.QUOTE);
            }
            xml.append(IFSConstants.RIGHT_ANGLE);
            
            if((respondWiths != null) &&
                    (respondWiths != Collections.EMPTY_LIST)) {
                Iterator i = respondWiths.iterator();
                while (i.hasNext()) {
                    xml.append(IFSConstants.LEFT_ANGLE)
                       .append(samlpPrefix)
                       .append(IFSConstants.RESPONDWITH)
                       .append(IFSConstants.RIGHT_ANGLE)
                       .append((String) i.next())
                       .append(IFSConstants.START_END_ELEMENT)
                       .append(samlpPrefix)
                       .append(IFSConstants.RESPONDWITH)
                       .append(IFSConstants.RIGHT_ANGLE);
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

            if ((extensions != null) && (!extensions.isEmpty())) {
                for(Iterator iter = extensions.iterator(); iter.hasNext();) {
                    Extension extension = (Extension)iter.next();
                    extension.setMinorVersion(minorVersion);
                    xml.append(extension.toXMLString());
                }

            }

            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefix)
               .append(IFSConstants.PROVIDER_ID)
               .append(IFSConstants.RIGHT_ANGLE)
               .append(providerId)
               .append(IFSConstants.START_END_ELEMENT)
               .append(prefix)
               .append(IFSConstants.PROVIDER_ID)
               .append(IFSConstants.RIGHT_ANGLE);
            
            if (affiliationID != null) {
                xml.append(IFSConstants.LEFT_ANGLE)
                   .append(prefix)
                   .append(IFSConstants.AFFILIATIONID)
                   .append(IFSConstants.RIGHT_ANGLE)
                   .append(affiliationID)
                   .append(IFSConstants.START_END_ELEMENT)
                   .append(prefix)
                   .append(IFSConstants.AFFILIATIONID)
                   .append(IFSConstants.RIGHT_ANGLE);
            }
            
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                String strFederate = IFSConstants.NAME_ID_POLICY_NONE;
                if (federate) {
                    strFederate = IFSConstants.NAME_ID_POLICY_FEDERATED;
                    if (nameIDPolicy != null && nameIDPolicy.length()>0) {
                        strFederate = nameIDPolicy;
                    }
                }
                xml.append(IFSConstants.LEFT_ANGLE)
                   .append(prefix)
                   .append(IFSConstants.NAMEID_POLICY_ELEMENT)
                   .append(IFSConstants.RIGHT_ANGLE)
                   .append(strFederate)
                   .append(IFSConstants.START_END_ELEMENT)
                   .append(prefix)
                   .append(IFSConstants.NAMEID_POLICY_ELEMENT)
                   .append(IFSConstants.RIGHT_ANGLE);
            } else {
                String strFederate = IFSConstants.FALSE;
                if (federate) {
                    strFederate = IFSConstants.TRUE;
                }
                xml.append(IFSConstants.LEFT_ANGLE)
                   .append(prefix)
                   .append(IFSConstants.FEDERATE)
                   .append(IFSConstants.RIGHT_ANGLE)
                   .append(strFederate)
                   .append(IFSConstants.START_END_ELEMENT)
                   .append(prefix)
                   .append(IFSConstants.FEDERATE)
                   .append(IFSConstants.RIGHT_ANGLE);
            }
            
            String strForceAuthn = IFSConstants.FALSE;
            if (forceAuthn) {
                strForceAuthn = IFSConstants.TRUE;
            }
            
            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefix)
               .append(IFSConstants.FORCE_AUTHN_ELEM)
               .append(IFSConstants.RIGHT_ANGLE)
               .append(strForceAuthn)
               .append(IFSConstants.START_END_ELEMENT)
               .append(prefix)
               .append(IFSConstants.FORCE_AUTHN_ELEM)
               .append(IFSConstants.RIGHT_ANGLE);
            
            String strIsPassive = IFSConstants.FALSE;
            if (isPassive) {
                strIsPassive = IFSConstants.TRUE;
            }
            
            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefix)
               .append(IFSConstants.IS_PASSIVE_ELEM)
               .append(IFSConstants.RIGHT_ANGLE)
               .append(strIsPassive)
               .append(IFSConstants.START_END_ELEMENT)
               .append(prefix)
               .append(IFSConstants.IS_PASSIVE_ELEM)
               .append(IFSConstants.RIGHT_ANGLE);
            
            if(protocolProfile != null && protocolProfile.length() != 0) {
                xml.append(IFSConstants.LEFT_ANGLE)
                   .append(prefix)
                   .append(IFSConstants.PROTOCOL_PROFILE)
                   .append(IFSConstants.RIGHT_ANGLE)
                   .append(protocolProfile)
                   .append(IFSConstants.START_END_ELEMENT)
                   .append(prefix)
                   .append(IFSConstants.PROTOCOL_PROFILE)
                   .append(IFSConstants.RIGHT_ANGLE);
            }
            
            if(assertionConsumerServiceID != null) {
                xml.append(IFSConstants.LEFT_ANGLE)
                   .append(prefix)
                   .append(IFSConstants.ASSERTION_CONSUMER_SVC_ID)
                   .append(IFSConstants.RIGHT_ANGLE)
                   .append(assertionConsumerServiceID)
                   .append(IFSConstants.START_END_ELEMENT)
                   .append(prefix)
                   .append(IFSConstants.ASSERTION_CONSUMER_SVC_ID)
                   .append(IFSConstants.RIGHT_ANGLE);
            }
            
            if(authnContext != null){
                authnContext.setMinorVersion(minorVersion);
                xml.append(authnContext.toXMLString());
            }
            
            if(relayState != null && relayState.length() != 0){
                xml.append(IFSConstants.LEFT_ANGLE)
                   .append(prefix)
                   .append(IFSConstants.RELAY_STATE)
                   .append(IFSConstants.RIGHT_ANGLE)
                   .append(XMLUtils.escapeSpecialCharacters(relayState))
                   .append(IFSConstants.START_END_ELEMENT)
                   .append(prefix)
                   .append(IFSConstants.RELAY_STATE)
                   .append(IFSConstants.RIGHT_ANGLE);
            }
            
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION)  {
                if (scoping != null) {
                    xml.append(scoping.toXMLString(true, false));
                }
            }
            
            if(minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                if(authContextCompType != null &&
                        authContextCompType.length() != 0) {
                    xml.append(IFSConstants.LEFT_ANGLE)
                       .append(prefix)
                       .append(IFSConstants.AUTHN_CONTEXT_COMPARISON)
                       .append(IFSConstants.RIGHT_ANGLE)
                       .append(authContextCompType)
                       .append(IFSConstants.START_END_ELEMENT)
                       .append(prefix)
                       .append(IFSConstants.AUTHN_CONTEXT_COMPARISON)
                       .append(IFSConstants.RIGHT_ANGLE);
                }
            }
            
            xml.append(IFSConstants.START_END_ELEMENT)
               .append(prefix)
               .append(IFSConstants.AUTHN_REQUEST)
               .append(IFSConstants.RIGHT_ANGLE);
        } else{
            FSUtils.debug.error("FSAuthnRequest.toString: requestID is null ");
            throw new FSMsgException("nullAuthnRequestID",null);
        }
        return xml.toString();
    }
    
    /**
     * Returns the <code>FSAuthnRequest</code> object.
     *
     * @param xml the XML string.
     * @return <code>FSAuthnRequest</code> object.
     * @throws FSMsgException if there is 
     *         error creating the object.
     */
    public static FSAuthnRequest parseXML(String xml) throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            FSUtils.debug.error("FSAuthnRequest.parseXML:Error "
                    + "while parsing input xml string");
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSAuthnRequest(root);
    }
    
    /**
     * Returns Signed XML String representation of this object.
     *
     * @return signed XML String.
     */
    public String getSignedXMLString(){
        return xmlString;
    }
    
    /**
     * Returns the signature string.
     *
     * @return the signature string.
     */
    public String getSignatureString(){
        return signatureString;
    }
    
    /**
     * Returns a list of <code>Extension</code> objects.
     * Each entry of the list is a <code>Extension</code> object.
     *
     * @return a list of <code>Extension</code> elements.
     * @see #setExtensions(List)
     */
    
    public List getExtensions() {
        return extensions;
    }
    
    /**
     * Sets <code>Extension</code> objects.
     * Each entry of the list is a <code>Extension</code> object.
     *
     * @param extensions a list of <code>Extension</code> objects.
     * @see #getExtensions
     */
    public void setExtensions(List extensions) {
        this.extensions = extensions;
    }

    /**
     * Returns the value of Force Authentication attribute.
     *
     * @return the value of Force Authentication attribute.
     */
    public boolean getForceAuthn() {
        return forceAuthn;
    }
    
    /**
     * Sets the value of Force Authentication attribute.
     *
     * @param forceAuthn value of Force Authentication attribute.
     */
    public void setForceAuthn(boolean forceAuthn) {
        this.forceAuthn = forceAuthn;
    }
    
    /**
     * Returns the value of the <code>isPassive</code> attribute.
     *
     * @return value of <code>isPassive</code> attribute.
     */
    public boolean getIsPassive() {
        return isPassive;
    }
    
    /**
     * Sets the value of the <code>IsPassive</code> attribute.
     *
     * @param isPassive value of <code>isPassive</code> attribute.
     */
    public void setIsPassive(boolean isPassive) {
        this.isPassive = isPassive;
    }
    
    /**
     * Returns the value of the <code>Federate</code> attribute.
     *
     * @return the value fo the <code>Federate</code> attribute.
     */
    public boolean getFederate() {
        return federate;
    }
    
    /**
     * Sets the value of the <code>Federate</code> attribute.
     *
     * @param fed the value of the <code>Federate</code> attribute.
     */
    public void setFederate(boolean fed) {
        federate = fed;
    }
    
    /**
     * Returns the <code>NameIDPolicy</code> object.
     *
     * @return the <code>NameIDPolicy</code> object.
     * @see #setNameIDPolicy(String)
     */
    
    public String getNameIDPolicy() {
        return nameIDPolicy;
    }
    
    /**
     * Sets the <code>NameIDPolicy</code> object.
     *
     * @param nameIDPolicy the new <code>NameIDPolicy</code> object.
     * @see #getNameIDPolicy
     */
    public void setNameIDPolicy(String nameIDPolicy) {
        this.nameIDPolicy = nameIDPolicy;
    }
    
    /**
     * Returns the value of <code>ProtocolProfile<code> attribute.
     *
     * @return the value of <code>ProtocolProfile<code> attribute.
     * @see #setProtocolProfile(String)
     */
    public String getProtocolProfile() {
        return protocolProfile;
    }
    
    /**
     * Sets the value of <code>ProtocolProfile<code> attribute.
     *
     * @param protocolProf the value of <code>ProtocolProfile<code> attribute.
     * @see #getProtocolProfile()
     */
    public void setProtocolProfile(String protocolProf) {
        protocolProfile = protocolProf;
    }
    
    /**
     * Returns the value of RelayState attribute.
     *
     * @return the value of RelayState attribute.
     * @see #setRelayState(String)
     */
    public String getRelayState() {
        return relayState;
    }
    
    /**
     * Set the value of RelayState attribute.
     *
     * @param relaySt the value of RelayState attribute.
     * @see #getRelayState()
     */
    public void setRelayState(String relaySt) {
        relayState = relaySt;
    }

    /**
     * Returns the <code>RequestedAuthnContext</code> object.
     *
     * @return the <code>RequestedAuthnContext</code> object.
     * @see #setAuthnContext(RequestAuthnContext)
     */
    public RequestAuthnContext getAuthnContext() {
        return authnContext;
    }
    
    /**
     * Sets the <code>RequestedAuthnContext</code> object.
     *
     * @param authnCxt the <code>RequestAuthnContext</code> object.
     * @see #getAuthnContext()
     */
    public void setAuthnContext(RequestAuthnContext authnCxt) {
        authnContext = authnCxt;
    }
    
    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     * @see #setProviderId(String).
     */
    public String getProviderId() {
        return providerId;
    }
    
    /**
     * Sets the value of <code>ProviderID</code> attribute.
     *
     * @param provId the value of <code>ProviderID</code> attribute.
     * @see #getProviderId()
     */
    public void setProviderId(String provId) {
        providerId = provId;
    }
    
    /**
     * Returns the value of AuthContext Comparison attribute.
     *
     * @return he value of AuthContext Comparison attribute.
     * @see #setAuthContextCompType(String)
     */
    public String getAuthContextCompType() {
        return authContextCompType;
    }
    
    /**
     * Sets the value of AuthContext Comparison attribute.
     *
     * @param authType he value of AuthContext Comparison attribute.
     * @see #getAuthContextCompType()
     */
    public void setAuthContextCompType(String authType) {
        authContextCompType = authType;
    }
    
    /**
     * Returns the value of <code>id</code> attribute.
     *
     * @return the value of <code>id</code> attribute.
     * @see #setID(String)
     */
    public String getID() {
        return id;
    }
    
    /**
     * Sets the value of <code>id</code> attribute.
     *
     * @param id the value of <code>id</code> attribute.
     * @see #getID()
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * Returns the value of the <code>MinorVersion</code> attribute.
     *
     * @return the value of the <code>MinorVersion</code> attribute.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the value of the <code>MinorVersion</code> attribute.
     *
     * @param version the value of the <code>MinorVersion</code> attribute.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    
    /**
     * Returns the Affliation Identifier.
     *
     * @return the Affliation Identifier.
     * @see #setAffiliationID(String)
     */
    public String getAffiliationID() {
        return affiliationID;
    }
    
    /**
     * Sets the Affiliation Identifier.
     *
     * @param affiliationID the Affiliation Identifier.
     * @see #getAffiliationID()
     */
    public void setAffiliationID(String affiliationID) {
        this.affiliationID = affiliationID;
    }
    
    /**
     * Returns the Assertion Consumer Service Identifier.
     *
     * @return the  Assertion Consumer Service Identifier.
     * @see #setAssertionConsumerServiceID(String)
     */
    public String getAssertionConsumerServiceID() {
        return assertionConsumerServiceID;
    }
    
    /**
     * Sets the Assertion Consumer Service Identifier.
     *
     * @param assertionConsumerServiceID the Assertion Consumer 
     *        Service Identifier.
     * @see #getAssertionConsumerServiceID
     */
    public void setAssertionConsumerServiceID(
                       String assertionConsumerServiceID) {
        this.assertionConsumerServiceID = assertionConsumerServiceID;
    }
    
    /** 
     * Returns the value of <code>consent</code> attribute.
     *
     * @return the value of <code>consent</code> attribute.
     * @see #setConsent(String)
     */
    public String getConsent() {
        return consentURI;
    }
    
    /**
     * Sets the value of <code>consent</code> attribute.
     *
     * @param consentURI the value of <code>consent</code> attribute.
     * @see #getConsent()
     */
    public void setConsent(String consentURI) {
        this.consentURI = consentURI;
    }
    
    /**
     * Sets the <code>FSScoping</code> object.
     *
     * @param scoping the <code>FSScoping</code> object.
     * @see #getScoping()
     */
    public void setScoping(FSScoping scoping) {
        this.scoping = scoping;
    }
    
    /**
     * Returns the <code>FSScoping</code> object.
     *
     * @return the <code>FSScoping</code> object.
     * @see #setScoping(FSScoping)
     */
    public FSScoping getScoping() {
        return scoping;
    }
    
    /**
     * Validates the the <code>MajorVersion</code> property in the 
     * <code>AuthnRequest</code>.
     * 
     * @param majorVer the value of <code>MajorVersion</code> property
     * @throws FSMsgException if the <code>MajoorVersion</code>
     *         is null or is invalid.
     */
    private void parseMajorVersion(String majorVer) throws FSMsgException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest(Element): invalid "
                        + "MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != IFSConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > IFSConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAuthnRequest(Element): "
                            + "MajorVersion of the AuthnRequest is too high.");
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAuthnRequest(Element): "
                            + "MajorVersion of the AuthnRequest is too low.");
                }
                throw new FSMsgException("requestVersionTooLow",null);
            }
        }
        
    }
    /**
     * Validates the the <code>MinorVersion</code> property in the 
     * <code>AuthnRequest</code>.
     * 
     * @param minorVer the value of <code>MinorVersion</code> property
     * @throws FSMsgException if the <code>MinorVersion</code>
     *         is null or is invalid.
     */
    private void parseMinorVersion(String minorVer) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest(Element): invalid "
                        + "MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        if(minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.checkMinorVersion:"+
                        " Minor Version of the AuthnRequest is too high.");
            }
            throw new FSMsgException("requestVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.checkMinorVersion:" +
                        " Minor Version of the AuthnRequest is too low.");
            }
            throw new FSMsgException("requestVersionTooLow",null);
        }
        
    }

    /**
     * Checks the value of the <code>MajorVersion</code> property
     *  in the <code>AuthnRequest</code>.
     *
     * @param minorVer the value of <code>MajorVersion</code> property
     * @return integer value of <code>MajorVersion</code> property
     * @throws FSMsgException if the <code>MajorVersion</code>
     *         is null or invalid.
     */
    private static int checkMajorVersion(String majorVer) 
                        throws FSMsgException {
        int majorVersion;
        if (majorVer == null){
            throw new FSMsgException("nullMajorVersion",null);
        }
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.checkMajorVersion: "
                        + "invalid MajorVersion: " + e.getMessage());
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAuthnRequest.checkMajorVersion: "
                            + "MajorVersion of the AuthnRequest is too high"
                            + majorVersion);
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                            "FSAuthnRequest.checkMajorVersion:MajorVersion of "
                            + "the AuthnRequest is too low. " + majorVersion);
                }
                throw new FSMsgException("requestVersionTooLow",null);
            }
        }
        return majorVersion;
    }
    
    /**
     * Checks the value of the <code>MinorVersion</code> property
     *  in the <code>AuthnRequest</code>.
     *
     * @param minorVer the value of <code>MinorVersion</code> property
     * @return integer value of <code>MinorVersion</code> property
     * @throws FSMsgException if the <code>MinorVersion</code>
     *         is null or invalid.
     */
    private static int checkMinorVersion(String minorVer)
    throws FSMsgException {
        int minorVersion;
        if (minorVer == null){
            throw new FSMsgException("nullMinorVersion",null);
        }
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.checkMinorVersion: "
                        + "invalid MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION ||
                minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            return minorVersion;
        }
        if(minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            if(FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.checkMinorVersion:"+
                        " Minor Version of the AuthnRequest is too high.");
            }
            throw new FSMsgException("requestVersionTooHigh",null);
        } else {
            if(FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.checkMinorVersion:" +
                        " Minor Version of the AuthnRequest is too low.");
            }
            throw new FSMsgException("requestVersionTooLow",null);
        }
    }
    
    /**
     * Returns an URL Encoded Query String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        if ((providerId == null) || (providerId.length() == 0)) {
            FSUtils.debug.error("FSAuthnRequest.toURLEncodedQueryString: "
                    + "providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID }; 
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSAuthnRequest.toURLEncodedQueryString: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        
        StringBuffer urlEncodedAuthnReq = new StringBuffer(300);
        
        urlEncodedAuthnReq.append(IFSConstants.AUTH_REQUEST_ID)
                          .append(IFSConstants.EQUAL_TO)
                          .append(URLEncDec.encode(requestID))
                          .append(IFSConstants.AMPERSAND)
                          .append(IFSConstants.MAJOR_VERSION)
                          .append(IFSConstants.EQUAL_TO)
                          .append(majorVersion)
                          .append(IFSConstants.AMPERSAND)
                          .append(IFSConstants.MINOR_VERSION)
                          .append(IFSConstants.EQUAL_TO)
                          .append(minorVersion)
                          .append(IFSConstants.AMPERSAND);
        if ((extensions != null) && (!extensions.isEmpty())) {
            Extension extension = (Extension)extensions.get(0);
            urlEncodedAuthnReq.append(extension.toURLEncodedQueryString(
                QUERY_STRING_EXTENSION_PREFIX)).append(IFSConstants.AMPERSAND);

            if (extensions.size() > 1) {
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning(
                        "FSAuthnRequest.toURLEncodedQueryString: " +
                        "only one Extension element is allowed and extras " +
                        " will be removed");
                }
            }
        }

        urlEncodedAuthnReq.append(IFSConstants.PROVIDER_ID)
                          .append(IFSConstants.EQUAL_TO)
                          .append(URLEncDec.encode(providerId))
                          .append(IFSConstants.AMPERSAND);

        if (consentURI != null) {
            urlEncodedAuthnReq.append(IFSConstants.CONSENT)
                              .append(IFSConstants.EQUAL_TO)
                              .append(URLEncDec.encode(consentURI))
                              .append(IFSConstants.AMPERSAND);
        }

        if(affiliationID != null) {
            urlEncodedAuthnReq.append(IFSConstants.AFFILIATIONID)
                              .append(IFSConstants.EQUAL_TO)
                              .append(URLEncDec.encode(affiliationID))
                              .append(IFSConstants.AMPERSAND);
        }
        
        if (issueInstant != null){
            urlEncodedAuthnReq.append(IFSConstants.ISSUE_INSTANT)
                              .append(IFSConstants.EQUAL_TO)
                              .append(URLEncDec.encode(
                                  DateUtils.toUTCDateFormat(issueInstant)))
                              .append(IFSConstants.AMPERSAND);
        } else {
            FSUtils.debug.error("FSAuthnRequest.toURLEncodedQueryString: "
                    + "issueInstant missing");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        
        String strForceAuthn = IFSConstants.FALSE;
        if (forceAuthn) {
            strForceAuthn = IFSConstants.TRUE;
        }
        
        urlEncodedAuthnReq.append(IFSConstants.FORCE_AUTHN_ELEM)
                          .append(IFSConstants.EQUAL_TO)
                          .append(strForceAuthn)
                          .append(IFSConstants.AMPERSAND);
        
        String strIsPassive =  IFSConstants.FALSE;
        if (isPassive) {
            strIsPassive = IFSConstants.TRUE;
        }
   
        urlEncodedAuthnReq.append(IFSConstants.IS_PASSIVE_ELEM)
                          .append(IFSConstants.EQUAL_TO)
                          .append(strIsPassive)
                          .append(IFSConstants.AMPERSAND);
        
        if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            String strFederate = IFSConstants.NAME_ID_POLICY_NONE;
            if (federate) {
                strFederate = IFSConstants.NAME_ID_POLICY_FEDERATED;
                if (nameIDPolicy != null && nameIDPolicy.length() > 0) {
                    strFederate = nameIDPolicy;
                }
            }
            urlEncodedAuthnReq.append(IFSConstants.NAMEID_POLICY_ELEMENT)
                              .append(IFSConstants.EQUAL_TO)
                              .append(strFederate)
                              .append(IFSConstants.AMPERSAND);
        } else {
            String strFederate = IFSConstants.FALSE;
            if (federate) {
                strFederate = IFSConstants.TRUE;
            }
            urlEncodedAuthnReq.append(IFSConstants.FEDERATE)
                              .append(IFSConstants.EQUAL_TO)
                              .append(strFederate)
                              .append(IFSConstants.AMPERSAND);
        }
        
        if (protocolProfile != null && protocolProfile.length() != 0) {
            urlEncodedAuthnReq.append(IFSConstants.PROTOCOL_PROFILE)
                              .append(IFSConstants.EQUAL_TO)
                              .append(URLEncDec.encode(protocolProfile))
                              .append(IFSConstants.AMPERSAND);
        }
        
        if (authnContext != null) {
            authnContext.setMinorVersion(minorVersion);
            urlEncodedAuthnReq.append(authnContext.toURLEncodedQueryString());
        }
        
        if (relayState != null && relayState.length() != 0) {
            urlEncodedAuthnReq.append(IFSConstants.RELAY_STATE)
                              .append(IFSConstants.EQUAL_TO)
                              .append(URLEncDec.encode(relayState))
                              .append(IFSConstants.AMPERSAND);
        }
        
        if (scoping != null) {
            urlEncodedAuthnReq.append(scoping.toURLEncodedQueryString());
        }
        
        if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            if (authContextCompType != null 
                       && authContextCompType.length() != 0) {
                urlEncodedAuthnReq.append(IFSConstants.AUTHN_CONTEXT_COMPARISON)
                                  .append(IFSConstants.EQUAL_TO)
                                  .append(URLEncDec.encode(authContextCompType))
                                  .append(IFSConstants.AMPERSAND);
            }
        }
        
        int len = urlEncodedAuthnReq.length() - 1;
        if (urlEncodedAuthnReq.charAt(len) == '&') {
            urlEncodedAuthnReq = urlEncodedAuthnReq.deleteCharAt(len);
        }
        
        return urlEncodedAuthnReq.toString();
    }
    
    /**
     * Returns a Base64 Encoded String.
     *
     * @return a Base64 Encoded String.
     * @throws FSMsgException if there is an error encoding 
     *         the string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSAuthnRequest.toBASE64EncodedString: "
                    + "providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)) {
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSAuthnRequest.toBASE64EncodedString: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());
    }
    
    /**
     * Returns <code>FSAuthnRequest</code> object. The
     * object is creating by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @throws FSMsgException if there is an error
     *         creating <code>FSAuthnRequest</code> object.
     */
    public static FSAuthnRequest parseURLEncodedRequest(
                          HttpServletRequest request) throws FSMsgException {
        FSAuthnRequest retAuthnRequest = new FSAuthnRequest();
        String authReqID = request.getParameter(IFSConstants.AUTH_REQUEST_ID);
        if (authReqID == null || authReqID.length() == 0) {
            throw new FSMsgException("nullAuthnRequestID",null);
        }
        retAuthnRequest.requestID = authReqID;
        
        String instantString = 
            request.getParameter(IFSConstants.ISSUE_INSTANT);
        if (instantString == null || instantString.length() == 0) {
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        try{
            retAuthnRequest.issueInstant =
                    DateUtils.stringToDate(instantString);
        } catch (ParseException e){
            throw new FSMsgException("parseError",null);
        }
        
        retAuthnRequest.majorVersion =
                checkMajorVersion(request.getParameter(
                                          IFSConstants.MAJOR_VERSION));
        
        retAuthnRequest.minorVersion =
                checkMinorVersion(request.getParameter(
                                          IFSConstants.MINOR_VERSION));
        
        String providerId = request.getParameter(IFSConstants.PROVIDER_ID);
        if (providerId == null || providerId.length() == 0) {
            throw new FSMsgException("nullProviderIdInRequest",null);
        } else{
            FSUtils.debug.message("ProviderID of the sender: " + providerId);
            retAuthnRequest.providerId = providerId;
        }
        
        retAuthnRequest.affiliationID = 
            request.getParameter(IFSConstants.AFFILIATIONID);
        
        String forceAuthn = request.getParameter(IFSConstants.FORCE_AUTHN_ELEM);
        if ( forceAuthn != null && forceAuthn.length() != 0 
             && (forceAuthn.equals(IFSConstants.TRUE) 
             || forceAuthn.equals(IFSConstants.ONE))) {
            retAuthnRequest.forceAuthn = true;
        } else {
            retAuthnRequest.forceAuthn = false;
        }
        
        String isPassive = request.getParameter(IFSConstants.IS_PASSIVE_ELEM);
        if (isPassive != null && isPassive.length() != 0 &&
                (isPassive.equals(IFSConstants.TRUE) ||
                isPassive.equals(IFSConstants.ONE))) 
        {
            retAuthnRequest.isPassive = true;
        } else {
            retAuthnRequest.isPassive = false;
        }

        if (retAuthnRequest.minorVersion
                == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            String nameIDPolicy = 
                request.getParameter(IFSConstants.NAMEID_POLICY_ELEMENT);
            
            if (nameIDPolicy != null &&
                    (nameIDPolicy.equals(
                          IFSConstants.NAME_ID_POLICY_FEDERATED) ||
                    nameIDPolicy.equals(
                          IFSConstants.NAME_ID_POLICY_ONETIME))
                    ) {
                retAuthnRequest.federate = true;
            }
            retAuthnRequest.nameIDPolicy = nameIDPolicy;
        } else {
            String federate = request.getParameter(IFSConstants.FEDERATE);
            if (federate != null &&
                    federate.length() != 0 &&
                    (federate.equals(IFSConstants.TRUE)||
                    federate.equals(IFSConstants.ONE))) {
                retAuthnRequest.federate = true;
            } else {
                retAuthnRequest.federate = false;
            }
        }
        
        String protocolProfile = 
            request.getParameter(IFSConstants.PROTOCOL_PROFILE);
        if (protocolProfile != null && protocolProfile.length() != 0) {
            retAuthnRequest.protocolProfile = protocolProfile;
        }
        
        String relayState = request.getParameter(IFSConstants.RELAY_STATE);
        if(relayState != null && relayState.length() != 0) {
            retAuthnRequest.setRelayState(relayState);
        }
        
        String authnContextComparison = 
            request.getParameter(IFSConstants.AUTHN_CONTEXT_COMPARISON);
        if(authnContextComparison != null && 
                          authnContextComparison.length() != 0) {
            retAuthnRequest.setAuthContextCompType(authnContextComparison);
            String authType = retAuthnRequest.getAuthContextCompType();
            if(! (authType.equals(IFSConstants.MINIMUM) ||
                    authType.equals(IFSConstants.EXACT) ||
                    authType.equals(IFSConstants.MAXIMUM) ||
                    authType.equals(IFSConstants.BETTER)) ) {
                throw new FSMsgException("wrongInput",null);
            }
        }
        
        retAuthnRequest.authnContext =
                RequestAuthnContext.parseURLEncodedRequest(
                request, retAuthnRequest.getMinorVersion());
        
        retAuthnRequest.scoping = FSScoping.parseURLEncodedRequest(request);

        Extension extension = Extension.parseURLEncodedRequest(request,
            QUERY_STRING_EXTENSION_PREFIX, retAuthnRequest.getMinorVersion());
        if (extension != null) {
            retAuthnRequest.extensions = new ArrayList();
            retAuthnRequest.extensions.add(extension);
        }

        return retAuthnRequest;
    }
    
    /**
     * Returns <code>FSAuthnRequest</code> object. The object
     * is created by parsing an Base64 encode authentication
     * request string.
     *
     * @param encodedReq the encode string
     * @throws FSMsgException if there is an error
     *         creating <code>FSAuthnRequest</code> object.
     */
    public static FSAuthnRequest parseBASE64EncodedString(String encodedReq) 
                                     throws FSMsgException {
        if (encodedReq != null && encodedReq.length() != 0) {
            String decodedAuthnReq = new String(Base64.decode(encodedReq));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAuthnRequest.parseBASE64EncodedString: "
                    + "decoded input string: " + decodedAuthnReq);
            }
            return parseXML(decodedAuthnReq);
        } else{
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSAuthnRequest.parseBASE64EncodedString: "
                        + "null String passed in as argument.");
            }
            throw new FSMsgException("nullInput",null);
        }
    }
    
    /**
     * Signs the Request.
     *
     * @param certAlias the Certificate Alias.
     * @throws XMLSignatureException if <code>FSAuthnRequest</code>
     *         cannot be signed.
     */

    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSAuthnRequest.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequest.signXML: "
                        + "the assertion is "
                        + "already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(
                    FSUtils.BUNDLE_NAME,"cannotFindCertAlias",null);
        }
        try{
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true),
                        certAlias, (String) null, IFSConstants.ID,
                        this.id, false);
            } else if(minorVersion == 
                            IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                signatureString = 
                        manager.signXML(this.toXMLString(true, true),
                        certAlias, (String) null, IFSConstants.REQUEST_ID,
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
     * Unsupported Method.
     */
    public void signXML() throws SAMLException {
        throw new SAMLException(
                 FSUtils.BUNDLE_NAME,"unsupportedOperation",null);
    }
    
    /**
     * Sets the Signature of the Element passed.
     *
     * @param elem the Document Element.
     * @return true if success otherwise false.
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem);
        return super.setSignature(elem);
    }
}
