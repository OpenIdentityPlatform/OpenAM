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
 * $Id: NameIDPolicyImpl.java,v 1.5 2008/08/31 05:49:48 bina Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 
 * This interface defines methods to retrieve name identifier related 
 * properties.
 */

public class NameIDPolicyImpl implements NameIDPolicy {
    
    static private final String FORMAT="Format";
    static private final String SPNAMEQUALIFIER="SPNameQualifier";
    static private final String ALLOWCREATE="AllowCreate";
    private boolean isMutable=false;
    private String format;
    private String spNameQualifier;
    private Boolean allowCreate;

    /**
     * Constructor creates <code>NameIDPolicy</code> object.
     */

    public NameIDPolicyImpl() {
	isMutable=true;
    }

    /**
     * Constructor creates <code>NameIDPolicy</code> object.
     *
     * @param element Document Element of <code>NameIDPolicy</code> Object.
     * @throws SAML2Exception if <code>NameIDPolicy<code> cannot be created.
     */
    public NameIDPolicyImpl(Element element) throws SAML2Exception {
	parseElement(element);
    }

    /**
     * Returns the <code>NameIDPolicy</code> Object.
     *
     * @param xmlString XML String Representation of <code>NameIDPolicy</code>
     *	      object.
     * @throws SAML2Exception if <code>NameIDPolicy<code> cannot be created.
     */

    public NameIDPolicyImpl(String xmlString) throws SAML2Exception {
	Document xmlDocument =
                   XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
	if (xmlDocument == null) {
            throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("errorObtainingElement"));
 	}
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of <code>Format</code> attribute.
     *
     * @return the value of <code>Format</code> attribute.
     * @see #setFormat(String)
     */
    public String getFormat() {
	return format;
    }
    
    /** 
     * Sets the value of the <code>Format</code> attribute.
     *
     * @param uri the new value of <code>Format</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #setFormat(String)
     */
    public void setFormat(String uri) throws SAML2Exception {
	if (isMutable) {
            format = uri;
	} else {
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }
    
    /** 
     * Returns the value of the <code>SPNameQualifier<code> attribute.
     *
     * @return the value of <code>SPNameQualifier</code> attribute.
     */
    public String getSPNameQualifier() {
	return spNameQualifier;
    }

    /** 
     * Sets the value of <code>SPNameQualifier</code> attribute.
     *
     * @param spNameQualifier new value of <code>SPNameQualifier</code> 
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setSPNameQualifier(String spNameQualifier) 
			    throws SAML2Exception {
	if (isMutable) {
            this.spNameQualifier= spNameQualifier;
	} else {
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }
    
    /** 
     * Sets the value of <code>AllowCreate</code> attribute.
     *
     * @param value e the new value of <code>AllowCreate</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setAllowCreate(boolean value) throws SAML2Exception {
	if (isMutable) {
	    allowCreate = value ? Boolean.TRUE : Boolean.FALSE;
	} else {
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
    }
    
    /** 
     * Returns true if the identity provider is allowed to create a
     * new identifier to represent the principal.
     *
     * @return value of <code>AllowCreate</code> attribute.
     */
    public boolean isAllowCreate() {
	if (allowCreate == null) {
	    return false;
	}
	return allowCreate.booleanValue();
    }
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     */
    public String toXMLString() throws SAML2Exception {
	return toXMLString(true,false);
    }
    
    /** 
     * Returns a String representation
     *
     * @param includeNSPrefix determines whether or not the namespace
     *	      qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *	      within the Element.
     * @return String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     */
    
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
    throws SAML2Exception {
	StringBuffer xmlString = new StringBuffer(150);
	xmlString.append(SAML2Constants.START_TAG);
	if (includeNSPrefix) {
            xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
	}
        xmlString.append(SAML2Constants.NAMEIDPOLICY)
		 .append(SAML2Constants.SPACE);

	if (declareNS) {
            xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
	}

	if (StringUtils.isNotEmpty(format)) {
	    xmlString.append(SAML2Constants.SPACE)
		     .append(FORMAT).append(SAML2Constants.EQUAL)
		     .append(SAML2Constants.QUOTE)
		     .append(format).append(SAML2Constants.QUOTE);
	}

	if (StringUtils.isNotEmpty(spNameQualifier)) {
	    xmlString.append(SAML2Constants.SPACE)
	             .append(SPNAMEQUALIFIER).append(SAML2Constants.EQUAL)
		     .append(SAML2Constants.QUOTE)
		     .append(spNameQualifier)
	   	     .append(SAML2Constants.QUOTE);
	}

	if (allowCreate != null) {
	    xmlString.append(SAML2Constants.SPACE)
		     .append(ALLOWCREATE).append(SAML2Constants.EQUAL)
		     .append(SAML2Constants.QUOTE)
		     .append(allowCreate.toString())
	   	     .append(SAML2Constants.QUOTE);
	}

	xmlString.append(SAML2Constants.END_TAG)
		 .append(SAML2Constants.SAML2_END_TAG)
		 .append(SAML2Constants.NAMEIDPOLICY)
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
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable(){
	return isMutable;
    }

    /* Parses the NameIDPolicy Element */
    private void parseElement(Element element) {
	format = element.getAttribute(FORMAT);
	spNameQualifier=element.getAttribute(SPNAMEQUALIFIER);
	String allowCreateStr = element.getAttribute(ALLOWCREATE);
        if ((allowCreateStr != null) && (allowCreateStr.length() > 0 )) {
            allowCreate = SAML2SDKUtils.booleanValueOf(allowCreateStr);
	}
    }

}
