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
 * $Id: SPAccountMapper.java,v 1.5 2008/08/19 19:11:15 veiming Exp $
 *
 */


package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The interface <code>SPAccountMapper</code> is used to identify the
 * local identities that maps the <code>SAML</code> protocol objects such as
 * <code>Assertion</code>, <code>ManageNameIDRequest</code> etc.
 * This mapper interface is used to map the identities only at the
 * <code>SAMLAssertionConsumer</code>, in otherwords, 
 * <code>ServiceProvider</code> version of the <code>SAML</code> provider. 
 * The implementation of this interface will be used by the <code>SAML</code>
 * framework to retrieve the user identity information for the consumption
 * of generating a user session, or manage the user account information while
 * handling the <code>SAML</code> protocols and it is <code>pluggable</code>
 * through local configuration in the <code>SAML2</code> plugin.
 *
 * The implementation of this interface may need to consider the
 * deployment of the SAML v2 plugin for example on the OpenSSO
 * platform or on Federation Manager.
 * @see com.sun.identity.saml2.plugins.IDPAccountMapper
 *
 * @supported.all.api
 */ 
public interface SPAccountMapper {

    /**
     * Returns the user's disntinguished name or the universal ID for the 
     * corresponding  <code>SAML</code> <code>Assertion</code>. This method
     * will be invoked by the <code>SAML</code> framework while processing
     * the <code>Assertion</code> and retrieves the identity information. 
     *
     * @param assertion <code>SAML</code> <code>Assertion</code> that needs
     *        to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm realm or the organization name that may be used to find
     *        the user information.
     * @return user's disntinguished name or the universal ID.
     * @exception SAML2Exception if any failure.
     */
    public java.lang.String getIdentity(
        com.sun.identity.saml2.assertion.Assertion assertion,
        java.lang.String hostEntityID,
        java.lang.String realm
    ) throws SAML2Exception;


    /**
     * Returns the user's disntinguished name or the universal ID for the 
     * corresponding  <code>SAML</code> <code>ManageNameIDRequest</code>.
     * This method will be invoked by the <code>SAML</code> framework for
     * retrieving the user identity while processing the
     * <code>ManageIDRequest</code>. 
     * @param manageNameIDRequest <code>SAML</code> 
     *     <code>ManageNameIDRequest</code> that needs to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm realm or the organization name that may be used to find
     *        the user information.
     * @return user's disntinguished name or the universal ID.
     * @exception SAML2Exception if any failure.
     */
    public java.lang.String getIdentity(
        com.sun.identity.saml2.protocol.ManageNameIDRequest manageNameIDRequest,
        java.lang.String hostEntityID,
        java.lang.String realm
    ) throws SAML2Exception;

}
