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
 * $Id: StatusDetail.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class represents the <code>StatusDetailType</code> complex type in
 * SAML protocol schema.
 * The <code>StatusDetail</code> element MAY be used to specify additional
 * information concerning the status of the request.
 *
 * <pre>
 * &lt;complexType name="StatusDetailType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */

public interface StatusDetail {
    
    /**
     * Returns the value of the Any property.
     *
     * @return A list containing objects of type <code>String</code>
     * @see #setAny(List)
     */
    public java.util.List getAny();
    
    /**
     * Sets the value of the Any property.
     *
     * @param anyList
     *        A list containing objects of type <code>String</code>
     * @throws SAML2Exception if the object is immutable
     * @see #getAny
     */
    public void setAny(java.util.List anyList) throws SAML2Exception;
    
    /**
     * Returns the <code>StatusDetail</code> in an XML document String format
     * based on the <code>StatusDetail</code> schema described above.
     *
     * @return An XML String representing the <code>StatusDetail</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception;
    
    /**
     * Returns the <code>StatusDetail</code> in an XML document String format
     * based on the <code>StatusDetail</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusDetail</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception;
    
    /**
     * Makes the obejct immutable
     */
    public void makeImmutable();
    
    /**
     * Returns true if the object is mutable false otherwise
     *
     * @return true if the object is mutable false otherwise
     */
    public boolean isMutable();
}
