/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AssertionIDRequest.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import java.util.List;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.RequestAbstract;

/**
 * This class represents the AssertionIDRequestType complex type.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;complexType name="AssertionIDRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AssertionIDRef" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public interface AssertionIDRequest extends RequestAbstract {

    /** 
     * Returns a list of <code>AssertionIDRef</code> objects.
     *
     * @return list of <code>AssertionIDRef</code> objects.
     * @see #setAssertionIDRefs(List)
     */
    public List getAssertionIDRefs();

    /** 
     * Sets a list of <code>AssertionIDRef</code> Objects.
     *
     * @param assertionIDRefs the list of <code>AssertionIDRef</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionIDRefs
     */
    public void setAssertionIDRefs(List assertionIDRefs) throws SAML2Exception;
}
