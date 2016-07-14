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
 * $Id: StatusCode.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.StatusCodeImpl;

/**
 * This class represents the <code>StatusCodeType</code> complex type in
 * SAML protocol schema.
 * The <code>StatusCode</code> element specifies a code or a set of nested codes
 * representing the status of the corresponding request.
 *
 * <pre>
 * &lt;complexType name="StatusCodeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}StatusCode" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Value" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = StatusCodeImpl.class)
public interface StatusCode {
    
    /**
     * Returns the value of the statusCode property.
     *
     * @return the value of the statusCode property
     * @see #setStatusCode(StatusCode)
     */
    public com.sun.identity.saml2.protocol.StatusCode getStatusCode();
    
    /**
     * Sets the value of the statusCode property.
     *
     * @param value the value of the statusCode property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatusCode
     */
    public void setStatusCode(com.sun.identity.saml2.protocol.StatusCode value)
    throws SAML2Exception;
    
    /**
     * Returns the value of the value property.
     *
     * @return the value of the value property
     * @see #setValue(String)
     */
    public java.lang.String getValue();
    
    /**
     * Sets the value of the value property.
     *
     * @param value the value of the value property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getValue
     */
    public void setValue(java.lang.String value) throws SAML2Exception;
    
    /**
     * Returns the <code>StatusCode</code> in an XML document String format
     * based on the <code>StatusCode</code> schema described above.
     *
     * @return An XML String representing the <code>StatusCode</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     * <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception;
    
    /**
     * Returns the <code>StatusCode</code> in an XML document String format
     * based on the <code>StatusCode</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusCode</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     * <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception;   
   
    /**
     * Makes the object immutable
     */
    public void makeImmutable();
    
    /**
     * Returns true if the object is mutable, false otherwise
     *
     * @return true if the object is mutable, false otherwise
     */
    public boolean isMutable();
}
