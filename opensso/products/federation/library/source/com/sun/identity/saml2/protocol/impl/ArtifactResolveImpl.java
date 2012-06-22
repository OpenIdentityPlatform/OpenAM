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
 * $Id: ArtifactResolveImpl.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 */



package com.sun.identity.saml2.protocol.impl;

import java.security.PublicKey;
import java.security.Signature;
import java.text.ParseException;
import java.util.Date;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.ArtifactResolve;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.impl.RequestAbstractImpl;


/**
 * This is an implementation of interface <code>ArtifactResolve</code>.
 *
 * The <code>ArtifactResolve</code> message is used to request that a SAML
 * protocol message be returned in an <code>ArtifactResponse</code> message
 * by specifying an artifact that represents the SAML protocol message.
 * It has the complex type <code>ArtifactResolveType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ArtifactResolveType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Artifact"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ArtifactResolveImpl extends RequestAbstractImpl
	implements ArtifactResolve {

    private Artifact artifact = null;
    private boolean validationDone = false;
    
    protected void validateData() throws SAML2Exception {
	super.validateData();
	if (artifact == null) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
		SAML2SDKUtils.debug.message("ArtifactResolveImpl.validateData: "
		    + "missing Artifact.");
	    }
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("missingElement"));
	}
    }

    private void parseElement(Element element)
        throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
        	SAML2SDKUtils.debug.message("ArtifactResolveImpl.parseElement: "
		    + "element input is null.");
	    }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an ArtifactResolve.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("ArtifactResolve"))) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
        	SAML2SDKUtils.debug.message("ArtifactResolveImpl.parseElement: "
		    + "not ArtifactResolve.");
	    }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <ArtifactResolve> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
	    int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) atts.item(i);
                String attrName = attr.getName();
                String attrValue = attr.getValue().trim();
                if (attrName.equals("ID")) {
                    requestId = attrValue;
                } else if (attrName.equals("Version")) {
                    version = attrValue;
                } else if (attrName.equals("IssueInstant")) {
		    try {
			issueInstant = DateUtils.stringToDate(attrValue);
		    } catch (ParseException pe) {
			throw new SAML2Exception(pe.getMessage());
		    }
                } else if (attrName.equals("Destination")) {
		    destinationURI = attrValue;
                } else if (attrName.equals("Consent")) {
		    consent = attrValue;
		}
            }
        }

	// handle child elements
	NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Issuer")) {
		    if (nameID != null) {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"
                                + "Element: included more than one Issuer.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    if (signatureString != null ||
			extensions != null ||
			artifact != null)
		    {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"				+ "Element:wrong sequence.");
			}
			throw new SAML2Exception(
			    SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
		    nameID = AssertionFactory.getInstance().createIssuer(
			(Element) child);
		} else if (childName.equals("Signature")) {
		    if (signatureString != null) {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"
                                + "Element:included more than one Signature.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    if (extensions != null || artifact != null) {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"				+ "Element:wrong sequence.");
			}
			throw new SAML2Exception(
			    SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
		    signatureString = XMLUtils.print((Element) child);
		    isSigned = true;
		} else if (childName.equals("Extensions")) {
		    if (extensions != null) {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"
                                + "Element:included more than one Extensions.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    if (artifact != null) {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"				+ "Element:wrong sequence.");
			}
			throw new SAML2Exception(
			    SAML2SDKUtils.bundle.getString("schemaViolation"));
		    }
		    extensions = ProtocolFactory.getInstance().createExtensions(
			(Element) child);
		} else if (childName.equals("Artifact")) {
		    if (artifact != null) {
			if (SAML2SDKUtils.debug.messageEnabled()) {
                            SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"
                                + "Element: included more than one Artifact.");
                        }
                        throw new SAML2Exception(
                            SAML2SDKUtils.bundle.getString("moreElement"));
		    }
		    artifact = ProtocolFactory.getInstance().createArtifact(
			(Element) child);
		} else {
		    if (SAML2SDKUtils.debug.messageEnabled()) {
                        SAML2SDKUtils.debug.message("ArtifactResolveImpl.parse"
                            + "Element: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
		}
	    }
	}

	validateData();
    }

    /**
     * Constructor. Caller may need to call setters to populate the
     * object.
     */
    public ArtifactResolveImpl() {
	isMutable = true;
    }

    /**
     * Class constructor with <code>ArtifactResolve</code> in
     * <code>Element</code> format.
     *
     * @param element the Document Element
     * @throws SAML2Exception if there is an creating the object.
     */
    public ArtifactResolveImpl(org.w3c.dom.Element element)
        throws SAML2Exception {
        parseElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Class constructor with <code>ArtifactResolve</code> in xml string format.
     */
    public ArtifactResolveImpl(String xmlString)
        throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Gets the <code>Artifact</code> of the request.
     *
     * @return <code>Artifact</code> of the request.
     * @see #setArtifact(Artifact)
     */
    public Artifact getArtifact() {
	return artifact;
    }

    /**
     * Sets the <code>Artifact</code> of the request.
     * 
     * @param value new <code>Artifact</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getArtifact()
     */
    public void setArtifact(Artifact value)
	throws SAML2Exception {
	if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
	artifact = value;
    }

    /**
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception if it could not create String object
     */
    public String toXMLString() throws SAML2Exception {
	return this.toXMLString(true, false);
    }

    /**
     * Returns a String representation
     *
     * @param includeNSPrefix determines whether or not the namespace
     *         qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *         within the Element.
     * @throws SAML2Exception if it could not create String object.
     * @return a String representation of this Object.
     **/

    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
        throws SAML2Exception {
	if (isSigned && signedXMLString != null) {
	    return signedXMLString;
	}

	this.validateData();
        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNSPrefix) {
            prefix = SAML2Constants.PROTOCOL_PREFIX;
        }
        if (declareNS) {
            uri = SAML2Constants.PROTOCOL_DECLARE_STR;
        }

        result.append("<").append(prefix).append("ArtifactResolve").
                append(uri).append(" ID=\"").append(requestId).append("\"").
		append(" Version=\"").append(version).append("\"").
		append(" IssueInstant=\"").
		append(DateUtils.toUTCDateFormat(issueInstant)).append("\"");
	if (destinationURI != null && destinationURI.trim().length() != 0) {
	    result.append(" Destination=\"").append(destinationURI).
		append("\"");
	}
	if (consent != null && consent.trim().length() != 0) {
	    result.append(" Consent=\"").append(consent).append("\"");
	}
	result.append(">");
	if (nameID != null) {
	    result.append(nameID.toXMLString(includeNSPrefix, declareNS));
	}
	if (signatureString != null) {
	    result.append(signatureString);
	}
	if (extensions != null) {
	    result.append(extensions.toXMLString(includeNSPrefix, declareNS));
	}
	result.append(artifact.toXMLString(includeNSPrefix, declareNS));
	result.append("</").append(prefix).append("ArtifactResolve>");
	return result.toString();
    }
}
