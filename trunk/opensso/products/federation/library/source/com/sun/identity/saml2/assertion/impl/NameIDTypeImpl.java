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
 * $Id: NameIDTypeImpl.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import java.io.Serializable;
import org.w3c.dom.Element;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.NameIDType;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 *  The <code>NameIDType</code> is used when an element serves to represent 
 *  an entity by a string-valued name. In addition to the string content
 *  containing the actual identifier, it provides the following optional
 *  attributes:
 *    NameQualifier
 *    SPNameQualifier
 *    Format
 *    SPProvidedID
 */
public abstract class NameIDTypeImpl implements NameIDType, Serializable {

    private boolean isMutable = true;
    private String value;
    private String nameQualifier;
    private String spNameQualifier;
    private String format; 
    private String spProvidedID;

    public static final String NAME_ID_TYPE_ELEMENT = "NameIDType";
    public static final String NAME_QUALIFIER_ATTR = "NameQualifier";
    public static final String SP_NAME_QUALIFIER_ATTR = "SPNameQualifier";
    public static final String FORMAT_ATTR = "Format";
    public static final String SP_PROVIDED_ID_ATTR = "SPProvidedID";

    /**
     *  Returns the string-valued identifier
     *
     *  @return the string-valued identifier
     */
    public String getValue() {
        return value;
    }

    /**
     *  Sets the string-valued identifier
     *
     *  @param value the string-valued identifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setValue(String value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        this.value = value;
    }

    /**
     *  Returns the name qualifier
     *
     *  @return the name qualifier 
     */
    public String getNameQualifier() {
        return nameQualifier;
    }

    /**
     *  Sets the name qualifier
     *
     *  @param value the name qualifier 
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNameQualifier(String value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        nameQualifier = value;
    }
    

    /**
     *  Returns the SP provided ID
     *
     *  @return the SP provided ID
     */
    public String getSPProvidedID() {
        return spProvidedID;
    }

    /**
     *  Sets the SP provided ID
     *
     *  @param value the SP provided ID
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSPProvidedID(String value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        spProvidedID = value;
    }

    /**
     *  Returns the SP name qualifier
     *
     *  @return the SP name qualifier 
     */
    public String getSPNameQualifier() {
        return spNameQualifier;
    }

    /**
     *  Sets the SP name qualifier
     *
     *  @param value the SP name qualifier 
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSPNameQualifier(String value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        spNameQualifier = value;
    }

    /**
     *  Returns the format 
     *
     *  @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     *  Sets the format 
     *
     *  @param value the format
     *  @exception SAML2Exception if the object is immutable
     */
    public void setFormat(String value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        format = value;
    }

    protected void getValueAndAttributes(Element element) 
        throws SAML2Exception {
        // starts processing attributes
        String attrValue = element.getAttribute(NAME_QUALIFIER_ATTR);
        if (attrValue != null) {
            setNameQualifier(attrValue);
        }

        attrValue = element.getAttribute(SP_NAME_QUALIFIER_ATTR);
        if (attrValue != null) {
            setSPNameQualifier(attrValue);
        }

        attrValue = element.getAttribute(FORMAT_ATTR);
        if (attrValue != null) {
            setFormat(attrValue);
        }

        attrValue = element.getAttribute(SP_PROVIDED_ID_ATTR);
        if (attrValue != null) {
            setSPProvidedID(attrValue);
        }
       
        // gets the name identifier value 
        String nameValue = XMLUtils.getElementString(element);
        if ((nameValue != null) && (nameValue.trim().length() != 0)) {
            setValue(nameValue);
        } else {
            SAML2SDKUtils.debug.error(
                "NameIDTypeImpl.processElement(): name identifier is missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_name_identifier"));
        }
    }

    /**
     * Returns a String representation
     * @param includeNSPrefix Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A String representation
     * @exception SAML2Exception if something is wrong during conversion
     */
    public abstract String toXMLString(boolean includeNSPrefix,
        boolean declareNS) throws SAML2Exception; 

    /**
     * Returns a String representation
     *
     * @return A String representation
     * @exception SAML2Exception if something is wrong during conversion
     */
    public abstract String toXMLString() throws SAML2Exception;

    /**
     * Makes the object immutable
     */
    public void makeImmutable() {
        isMutable = false;
    }

    /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable() {
        return isMutable;
    }
}
