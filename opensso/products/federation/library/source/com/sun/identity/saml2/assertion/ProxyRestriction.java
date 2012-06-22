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
 * $Id: ProxyRestriction.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import java.util.List;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>ProxyRestriction</code> specifies limitations that the 
 *  asserting party imposes on relying parties that in turn wish to
 *  act as asserting parties and issue subsequent assertions of their 
 *  own on the basis of the information contained in the original
 *  assertion. A relying party acting as an asserting party must not
 *  issue an assertion that itself violates the restrictions specified 
 *  in this condition on the basis of an assertion containing such
 *  a condition.
 *  @supported.all.api
 */
public interface ProxyRestriction extends ConditionAbstract {

    /**
     *  Returns the maximum number of indirections that the asserting
     *  party permits to exist between this assertion and an assertion
     *  which has ultimately been issued on the basis of it.
     *
     *  @return the count number
     */
    public int getCount();

    /**
     *  Sets the maximum number of indirections that the asserting
     *  party permits to exist between this assertion and an assertion
     *  which has ultimately been issued on the basis of it.
     *
     *  @param value the count number
     *  @exception SAML2Exception if the object is immutable
     */
    public void setCount(int value) throws SAML2Exception;

    /**
     *  Returns the list of audiences to whom the asserting party permits
     *  new assertions to be issued on the basis of this assertion.
     *
     *  @return a list of <code>String</code> represented audiences
     */
    public List getAudience();

    /**
     *  Sets the list of audiences to whom the asserting party permits
     *  new assertions to be issued on the basis of this assertion.
     *
     *  @param audiences a list of <code>String</code> represented audiences
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAudience(List audiences) throws SAML2Exception;
}
