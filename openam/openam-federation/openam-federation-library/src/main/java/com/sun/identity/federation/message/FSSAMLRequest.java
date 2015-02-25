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
 * $Id: FSSAMLRequest.java,v 1.2 2008/06/25 05:46:45 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import java.text.ParseException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.protocol.Request;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLRequestVersionTooHighException;
import com.sun.identity.saml.common.SAMLRequestVersionTooLowException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.federation.common.*;

/**
 * This class had methods to create a <code>SAML</code> Request
 * object from a Document Element and to create Request message
 * from this object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSSAMLRequest extends Request {
    
    /*
     * Default Constructor.
     */
    protected FSSAMLRequest() {}
    
    /**
     * Constructor creates <code>FSSAMLRequest</code> from
     * the Document Element.
     *
     * @param root the Document Element.
     * @throws SAMLException if there is an error creating this object.
     */
    public FSSAMLRequest(Element root) throws SAMLException {
        // Make sure this is a Request
        String tag = null;
        if (root == null) {
            SAMLUtils.debug.message("FSSAMLRequest(Element): null input.");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "nullInput",null);
        }
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("Request"))) {
            SAMLUtils.debug.message("FSSAMLRequest(Element): wrong input");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        // Attribute MajorVersion
        parseMajorVersion(root.getAttribute("MajorVersion"));
        
        // Attribute MinorVersion
        parseMinorVersion(root.getAttribute("MinorVersion"));
        
        List signs = XMLUtils.getElementsByTagNameNS1(root,
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion ==
                    IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
                valid = manager.verifyXMLSignature(root);
            } else {
                valid = manager.verifyXMLSignature(root,
                        IFSConstants.REQUEST_ID, null);
            }
            if (!valid) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("FSSAMLRequest(Element): couldn't"
                            + " verify Request's signature.");
                }
            }
            xmlString = XMLUtils.print(root);
            signed = true;
        } else if (signsSize != 0) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("FSSAMLRequest(Element): included more "
                        + "than one Signature element.");
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "moreElement",null);
        }
        
        // Attribute RequestID
        requestID = root.getAttribute("RequestID");
        if ((requestID == null) || (requestID.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("FSSAMLRequest(Element): Request "
                        + "does not have a RequestID.");
            }
            String[] args = { IFSConstants.REQUEST_ID };
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "missingAttribute",args);
        }
        
        // Attribute IssueInstant
        String instantString = root.getAttribute("IssueInstant");
        if ((instantString == null) || (instantString.length() == 0)) {
            SAMLUtils.debug.message("FSSAMLRequest(Element): "
                    + " missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                SAMLUtils.debug.message(
                        "FSSAMLRequest(Element): could not parse IssueInstant",
                        e);
                throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                        "wrongInput",null);
            }
        }
        
        // get the contents of the request
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        String respondWith;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals("RespondWith")) {
                    respondWith = XMLUtils.getElementValue((Element) child);
                    if (respondWith.length() == 0) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("FSSAMLRequest(Element): "
                                    + "wrong RespondWith value.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    if (respondWiths == Collections.EMPTY_LIST) {
                        respondWiths = new ArrayList();
                    }
                    respondWiths.add(respondWith);
                } else if (nodeName.equals("Signature")) {
                    signature = (Element) child;
                } else if (nodeName.equals("AssertionArtifact")) {
                    // make sure the content has no other elements assigned
                    if ((contentType != NOT_SUPPORTED) &&
                            (contentType != ASSERTION_ARTIFACT)) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("FSSAMLRequest(Element): "
                                    + "contained mixed contents.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = ASSERTION_ARTIFACT;
                    if (artifacts == Collections.EMPTY_LIST) {
                        artifacts = new ArrayList();
                    }
                    try{
                        AssertionArtifact newArt = new FSAssertionArtifact(
                                XMLUtils.getElementValue((Element) child));
                        artifacts.add(newArt);
                    }catch (Exception e){
                        SAMLUtils.debug.error("FSSAMLRequest(Element): ", e);
                    }
                } else {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("FSSAMLRequest(Element):invalid"
                                + " node" + nodeName);
                    }
                    throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                            "wrongInput",null);
                } // check nodeName
            } // if nodeName != null
        } // done for the nodelist loop
        
        if (contentType == NOT_SUPPORTED) {
            SAMLUtils.debug.message("Request: empty content.");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
    }
    
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws SAMLException when the version mismatches.
     */
    private void parseMajorVersion(String majorVer) throws SAMLException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("FSSAMLRequest(Element): invalid "
                        + "MajorVersion", e);
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("FSSAMLRequest(Element): "
                            + "MajorVersion of the Request is too high.");
                }
                throw new SAMLRequestVersionTooHighException(
                        FSUtils.BUNDLE_NAME,"requestVersionTooHigh",null);
            } else {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("FSSAMLRequest(Element): "
                            + "MajorVersion of the Request is too low.");
                }
                throw new SAMLRequestVersionTooLowException(FSUtils.BUNDLE_NAME,
                        "requestVersionTooLow",null);
            }
        }
        
    }
    
    /**
     * Sets the <code>MinorVersion</code> by parsing the version string.
     *
     * @param minorVer a String representing the <code>MinorVersion</code> to
     *        be set.
     * @throws SAMLException when the version mismatches.
     */
    private void parseMinorVersion(String minorVer) throws SAMLException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request(Element): invalid "
                        + "MinorVersion", e);
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        if(minorVersion > IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("Request(Element): MinorVersion"
                    + " of the Request is too high.");
            throw new SAMLRequestVersionTooHighException(FSUtils.BUNDLE_NAME,
                    "requestVersionTooHigh",null);
        } else if (minorVersion <
                IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("Request(Element): MinorVersion"
                    + " of the Request is too low.");
            throw new SAMLRequestVersionTooLowException(FSUtils.BUNDLE_NAME,
                    "requestVersionTooLow",null);
        }
    }
    
    /**
     * Returns the <code>MinorVersion</code> attribute.
     *
     * @return the Minor Version.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param version the minor version in the assertion.
     * @see #setMinorVersion(int)
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
}
