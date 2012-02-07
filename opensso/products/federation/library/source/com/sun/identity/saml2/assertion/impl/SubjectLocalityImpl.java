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
 * $Id: SubjectLocalityImpl.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.SubjectLocality;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 * This class implements interface <code>SubjectLocality</code>.
 *
 * The <code>SubjectLocality</code> element specifies the DNS domain name
 * and IP address for the system entity that performed the authentication.
 * It exists as part of <code>AuthenticationStatement</code> element.
 * <p>
 * <pre>
 * &lt;complexType name="SubjectLocalityType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="Address" 
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DNSName" 
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class SubjectLocalityImpl implements SubjectLocality {

    private String address = null;
    private String dnsName = null;
    private boolean mutable = true;

    // used by the constructors.
    private void parseElement(org.w3c.dom.Element element)
                throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("SubjectLocalityImpl.parseElement: "
                    + "Input element is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is a SubjectLocality.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("SubjectLocality"))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("SubjectLocalityImpl.parseElement: "
                    + "input is not SubjectLocality");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attribute of <SubjectLocality> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            Node att = atts.getNamedItem("Address");
            if (att != null) {
                address = ((Attr)att).getValue().trim();
            }
            att = atts.getNamedItem("DNSName");
            if (att != null) {
                dnsName = ((Attr)att).getValue().trim();
            }

        }
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public SubjectLocalityImpl() {
    }

    /**
     * Class constructor with <code>SubjectLocality</code> in
     * <code>Element</code> format.
     */
    public SubjectLocalityImpl(org.w3c.dom.Element element)
        throws SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>SubjectLocality</code> in xml string format.
     */
    public SubjectLocalityImpl(String xmlString)
        throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }


    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable; 
     *                <code>false</code> otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns the value of the <code>DNSName</code> attribute.
     *
     * @return the value of the <code>DNSName</code> attribute.
     * @see #setDNSName(String)
     */
    public String getDNSName() {
        return dnsName;
    }

    /**
     * Sets the value of the <code>DNSName</code> attribute.
     *
     * @param value new value of the <code>DNSName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDNSName()
     */
    public void setDNSName(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        dnsName = value;
    }

    /**
     * Returns the value of the <code>Address</code> attribute.
     *
     * @return the value of the <code>Address</code> attribute.
     * @see #setAddress(String)
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the value of the <code>Address</code> attribute.
     *
     * @param value new value of <code>Address</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAddress()
     */
    public void setAddress(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        address = value;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *        By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
        throws SAML2Exception
    {
        return this.toXMLString(true, false);
    }

    /**
     * Returns a String representation of the
     * <code>SubjectLocality</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *                within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAML2Constants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAML2Constants.ASSERTION_DECLARE_STR;
        }

        result.append("<").append(prefix).append("SubjectLocality").append(uri);
        if (address != null && address.trim().length() != 0) {
            result.append(" Address=\"").append(address).append("\"");
        }
        if (dnsName != null && dnsName.trim().length() != 0) {
            result.append(" DNSName=\"").append(dnsName).append("\"");
        }
        result.append("></").append(prefix).append("SubjectLocality>");
        return result.toString();
    }

}
