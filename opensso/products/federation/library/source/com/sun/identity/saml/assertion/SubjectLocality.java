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
 * $Id: SubjectLocality.java,v 1.2 2008/06/25 05:47:33 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

import org.w3c.dom.*;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;

/**
 *The <code>SubjectLocality</code> element specifies the DNS domain name 
 *and IP address for the system entity that performed the authentication. 
 *It exists as part of <code>AuthenticationStatement</code> element.
 *@supported.all.api
 */
public class SubjectLocality {
    static SAMLConstants sc;

    private java.lang.String _ipAddress=null;
    private java.lang.String _dnsAddress=null;
   
   
    /**
     *Default Constructor
     */
    public SubjectLocality() {}

    /**
     * Constructs an instance of <code>SubjectLocality</code> from an existing
     * XML block.
     *
     * @param localityElement A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>SubjectLocality</code> object.
     * @exception SAMLException if it could not process the Element properly,
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public SubjectLocality(org.w3c.dom.Element localityElement)  
        throws SAMLException
    {
        Element elt = (Element) localityElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectLocality: local name "
                        + "missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("nullInput")) ;   
        }
        if (!(eltName.equals("SubjectLocality"))) 
            throw new SAMLException("invalidElement") ;   
        String read = elt.getAttribute("IPAddress");
        if ((read != null) && (read.length() != 0)) {
            _ipAddress= read;
        }
        read = elt.getAttribute("DNSAddress");
        if ((read != null) && (read.length() != 0)) {
            _dnsAddress=read;
        }
    }
   
    /**
     * Constructs an instance of <code>SubjectLocality</code>.
     *
     * @param ipAddress String representing the IP Address of the entity
     *        that was authenticated.
     * @param dnsAddress String representing the DNS Address of the entity that
     *        was authenticated. As per SAML specification  they are both
     *        optional, so values can be null.
     */
    public SubjectLocality(String ipAddress,String dnsAddress) {
        if (ipAddress == null) {
            _ipAddress = "";
        } else  {
        _ipAddress = ipAddress;
        }
        if (dnsAddress == null) {
            _dnsAddress = "";
        } else {
            _dnsAddress = dnsAddress;
        }
    }    
   
    /**
     * Returns the IP address from <code>SubjectLocality</code> locality
     *
     * @return A String representation of IP address.
     */
    public java.lang.String getIPAddress() {
        return _ipAddress;
    }
   
    /**
     * Sets the DNS address for <code>SubjectLocality></code> locality.
     *
     * @param dnsAddress A String representation of DNS address.
     * @return true indicating the success of the operation.
     */
    public boolean setDNSAddress(java.lang.String dnsAddress) {
        if ((dnsAddress == null) || (dnsAddress.length() == 0)) {
            SAMLUtilsCommon.debug.message("DNS Address is null");
            return false;
        }
        _dnsAddress = dnsAddress;
        return true;
    }
   
    /**
     * Sets the IP address for <code>SubjectLocality</code> locality.
     *
     * @param ipAddress A String representation of IP address.
     * @return true indicating the success of the operation.
     */
    public boolean setIPAddress(java.lang.String ipAddress) {
        if ((ipAddress == null) || (ipAddress.length() == 0)) {
            SAMLUtilsCommon.debug.message("IP Address is null");
            return false;
        }
        _ipAddress = ipAddress;
        return true;
    }

    /**
     * Returns the DNS address from <code>SubjectLocality</code> locality
     *
     * @return A String representation of DNS address.
     */
    public java.lang.String getDNSAddress() {
        return _dnsAddress; 
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:SubjectLocality&gt;</code>.
    */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**
     * Returns a String representation of the
     * <code>&lt;SubjectLocality&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
     */                      
    public java.lang.String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String NS="";
        String appendNS="";
        if (declareNS) NS=sc.assertionDeclareStr;
        if (includeNS) appendNS="saml:";
        xml.append("<").append(appendNS).append("SubjectLocality").
            append(" ").append(NS).append(" ");
        if ((_ipAddress != null) && !(_ipAddress.length() == 0))
            xml.append("IPAddress").append("=\"").append(_ipAddress).
                append("\"").append(" ");
        if ((_dnsAddress != null) && !(_dnsAddress.length() == 0))
            xml.append("DNSAddress").append("=\"").append(_dnsAddress).
                append("\"").append(" ");
            xml.append(sc.END_ELEMENT);
        return xml.toString();
    }
}

