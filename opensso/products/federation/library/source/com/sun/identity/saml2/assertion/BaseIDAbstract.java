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
 * $Id: BaseIDAbstract.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>BaseIDAbstract</code> is an abstract type usable only as 
 *  the base of a derived type. It includes the following attributes 
 *  for use by extended identifier representations:
 *   <code>NameQualifier</code> [optional]
 *     The security or administrative domain that qualifies the identifier.
 *     This attribute provides a means to federate identifiers from 
 *     disparate user data stores without collision.
 *   <code>SPNameQualifier</code> [optional]
 *     Further qualifies an identifier with the name of a service provider 
 *     or affiliation of providers. This attribute provides an additional
 *     means to federate identifiers on the basis of the relying party
 *     or parties.
 *  @supported.all.api
 */
public interface BaseIDAbstract {

    /**
     *  Returns the name qualifier
     *
     *  @return the name qualifier
     */
    public String getNameQualifier();

    /**
     *  Sets the name qualifier
     *
     *  @param nameQualifier the name qualifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNameQualifier(String nameQualifier) 
        throws SAML2Exception;

    /**
     *  Returns the <code>SP</code> name qualifier
     *
     *  @return the <code>SP</code> name qualifier
     */
    public String getSPNameQualifier();

    /**
     *  Sets the <code>SP</code> name qualifier
     *
     *  @param spNameQualifier the <code>SP</code> name qualifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSPNameQualifier(String spNameQualifier) 
        throws SAML2Exception;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable();

}
