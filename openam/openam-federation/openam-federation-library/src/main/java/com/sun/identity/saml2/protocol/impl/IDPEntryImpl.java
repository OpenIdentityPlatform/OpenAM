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
 * $Id: IDPEntryImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.IDPEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 
 * This class defines methods to set/retrieve single identity provider
 * information trusted by the request issuer to authenticate the presenter.
 */

public class IDPEntryImpl implements IDPEntry {
    
    static private final String PROVIDERID="ProviderID";
    static private final String NAME="Name";
    static final String LOC="Loc";
    private String providerID ;
    private String name ;
    private String locationURI;
    private boolean isMutable=false;

    /**
     * Constructor to create <code>IDPEntry</code> Object. 
     */
    public IDPEntryImpl() {
	isMutable=true;
    }

    /**
     * Constructor to create <code>IDPEntry</code> Object. 
     *
     * @param element Document Element of <code>IDPEntry<code> object.
     * @throws SAML2Exception if <code>IDPEntry<code> cannot be created.
     */
    public IDPEntryImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }


    /**
     * Constructor to create <code>IDPEntry</code> Object. 
     *
     * @param xmlString XML Representation of the <code>IDPEntry<code> object.
     * @throws SAML2Exception if <code>IDPEntry<code> cannot be created.
     */

    public IDPEntryImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =  
                   XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);  
        if (xmlDocument == null) {     
            throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return value of the <code>ProviderID</code> attribute.
     * @see #setProviderID(String)
     */
    public String getProviderID() {
	return providerID;
    } 

    /** 
     * Sets the value of <code>ProviderID</code> attribute.
     *
     * @param uri new value of <code>ProviderID</code> attribute.
     * @throws SAML2Exception if the object is immutable. 
     * @see #getProviderID
     */
    public void setProviderID(String uri) throws SAML2Exception {
	if (isMutable) {
	    this.providerID = uri;
	} else {
	    throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }

    
    /** 
     * Returns the value of <code>Name</code> attribute.
     *
     * @return value of the <code>Name</code> attribute.
     * @see #setName(String)
     */
    
    public String getName() {
	return name;
    }
    
    /** 
     * Sets the value of <code>Name</code> attribute.
     *
     * @param name new value of <code>Name</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getName
     */
    public void setName(String name) throws SAML2Exception {
	if (isMutable) {
	    this.name= name;
	} else {
	    throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }
    
    /** 
     * Return the value of <code>Loc</code> attribute.
     *
     * @return value of <code>Loc</code> attribute.
     * @see #setLoc(String)
     */
    public String getLoc() {
	return locationURI;
    }
    
    /** 
     * Sets the value of <code>Loc</code> attribute.
     *
     * @param locationURI value of <code>Loc</code> attribute.
     * @throws SAML2Exception if the object is immutable. 
     * @see #getLoc
     */
    
    public void setLoc(String locationURI) throws SAML2Exception {
	 if (isMutable) {
	    this.locationURI = locationURI;
	} else {
	    throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     */
    public String toXMLString() throws SAML2Exception {
	return toXMLString(true,false);
    }
    
    /** 
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *        qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *        within the Element.
     * @return the String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     **/
    
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	   throws SAML2Exception {
	validate(providerID);
	StringBuffer xmlString = new StringBuffer(100);
	xmlString.append(SAML2Constants.START_TAG);
	if (includeNSPrefix) {
	    xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
	}
	xmlString.append(SAML2Constants.IDPENTRY);

	if (declareNS) {
            xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
	}
	xmlString.append(SAML2Constants.SPACE)
	         .append(PROVIDERID)
		 .append(SAML2Constants.EQUAL)
		 .append(SAML2Constants.QUOTE)
		 .append(providerID)
		 .append(SAML2Constants.QUOTE);

	if ((name != null) && (name.length() > 0)) {
	    xmlString.append(SAML2Constants.SPACE)
		     .append(NAME).append(SAML2Constants.EQUAL)
		     .append(SAML2Constants.QUOTE)
		     .append(name)
		     .append(SAML2Constants.QUOTE);
	}

	if ((locationURI != null) && (locationURI.length() > 0)) {
	    xmlString.append(SAML2Constants.SPACE)
		     .append(LOC).append(SAML2Constants.EQUAL)
		     .append(SAML2Constants.QUOTE)
		     .append(locationURI)
		     .append(SAML2Constants.QUOTE);
	}
		 
	xmlString.append(SAML2Constants.END_TAG)
		 .append(SAML2Constants.NEWLINE)
		 .append(SAML2Constants.SAML2_END_TAG)
		 .append(SAML2Constants.IDPENTRY)
		 .append(SAML2Constants.END_TAG);

	return xmlString.toString();
    }

    /** 
     * Makes this object immutable. 
     */
    public void makeImmutable() {
	if (isMutable) {
	    isMutable=false;
 	}
    }
    
    /** 
     * Returns  true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
	return isMutable;
    }

    /* Parses the DOM Element for this object */ 
    private void parseElement(Element element) throws SAML2Exception {
	providerID = element.getAttribute(PROVIDERID);
	validate(providerID);
        name = element.getAttribute(NAME);
	locationURI = element.getAttribute(LOC);
    }

    /* validates the required attribute ProviderID */
    void validate(String providerID) throws SAML2Exception {
	if ((providerID == null) || (providerID.length() == 0)) {
	    SAML2SDKUtils.debug.message("ProviderID is required");
	    throw new SAML2Exception(
			SAML2SDKUtils.bundle.getString("providerIDMissing"));
	}
    }
}
