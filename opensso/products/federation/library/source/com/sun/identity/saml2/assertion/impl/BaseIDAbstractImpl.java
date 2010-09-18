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
 * $Id: BaseIDAbstractImpl.java,v 1.2 2008/06/25 05:47:43 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import com.sun.identity.saml2.assertion.BaseIDAbstract;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 *  The <code>BaseIDAbstract</code> is an abstract type usable only as 
 *  the base of a derived type. It includes the following attributes 
 *  for use by extended identifier representations:
 *   NameQualifier [optional]
 *     The security or administrative domain that qualifies the identifier.
 *     This attribute provides a means to federate identifiers from 
 *     disparate user data stores without collision.
 *   SPNameQualifier [optional]
 *     Further qualifies an identifier with the name of a service provider 
 *     or affiliation of providers. This attribute provides an additional
 *     means to federate identifiers on the basis of the relying party
 *     or parties.
 */
public abstract class BaseIDAbstractImpl implements BaseIDAbstract {
    private String nameQualifier;
    private String spNameQualifier;
    private boolean isMutable = true;

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
     *  @param nameQualifier the name qualifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNameQualifier(String nameQualifier) 
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        this.nameQualifier = nameQualifier;
    }

    /**
     *  Returns the <code>SP</code> name qualifier
     *
     *  @return the <code>SP</code> name qualifier
     */
    public String getSPNameQualifier() {
        return spNameQualifier;
    }

    /**
     *  Sets the <code>SP</code> name qualifier
     *
     *  @param spNameQualifier the SP name qualifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSPNameQualifier(String spNameQualifier) 
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        this.spNameQualifier = spNameQualifier;
    }

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
