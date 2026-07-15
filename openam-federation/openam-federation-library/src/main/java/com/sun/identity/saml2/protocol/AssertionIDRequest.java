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
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems, LLC
 */


package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.AssertionIDRequestImpl;
import java.util.List;

/**
 * This class represents the AssertionIDRequestType complex type.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <pre>
 * &lt;complexType name="AssertionIDRequestType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AssertionIDRef" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = AssertionIDRequestImpl.class)
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
