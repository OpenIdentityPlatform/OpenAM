/*
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
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;

/**
 * The interface <code>SPAccountMapper</code> is used to identify the local identities that maps the <code>SAML</code>
 * protocol objects such as <code>Assertion</code>, <code>ManageNameIDRequest</code> etc.
 * This mapper interface is used to map the identities only at the <code>SAML Service Provider</code>.
 * The implementation of this interface will be used by the <code>SAML</code> framework to retrieve the user identity
 * information for the consumption of generating a user session, or manage the user account information while handling
 * the <code>SAML</code> protocols and it is pluggable through local configuration in the <code>SAML2</code> plugin.
 *
 * @see com.sun.identity.saml2.plugins.IDPAccountMapper
 *
 * @supported.all.api
 */ 
public interface SPAccountMapper {

    /**
     * Returns the user's distinguished name or the universal ID for the corresponding <code>SAML Assertion</code>. This
     * method will be invoked by the <code>SAML</code> framework while processing the <code>Assertion</code> and
     * retrieves the identity information.
     *
     * @param assertion <code>SAML Assertion</code> that needs to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm Realm or the organization name that may be used to find the user information.
     * @return User's distinguished name or the universal ID.
     * @throws SAML2Exception If there was any failure.
     */
    public String getIdentity(Assertion assertion, String hostEntityID, String realm) throws SAML2Exception;


    /**
     * Returns the user's distinguished name or the universal ID for the corresponding
     * <code>SAML ManageNameIDRequest</code>. This method will be invoked by the <code>SAML</code> framework for
     * retrieving the user identity while processing the <code>ManageIDRequest</code>.
     *
     * @param manageNameIDRequest <code>SAML ManageNameIDRequest</code> that needs to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm Realm or the organization name that may be used to find the user information.
     * @return User's distinguished name or the universal ID.
     * @throws SAML2Exception If there was any failure.
     */
    public String getIdentity(ManageNameIDRequest manageNameIDRequest, String hostEntityID, String realm)
            throws SAML2Exception;

    /**
     * Tells whether the provided NameID-Format should be persisted in the user data store or not.
     *
     * @param realm The hosted SP's realm.
     * @param hostEntityID The hosted SP's entityID.
     * @param remoteEntityID The remote IdP's entityID.
     * @param nameIDFormat The non-transient NameID-Format in question.
     * @return <code>true</code> if the provided NameID-Format should be persisted in the user data store,
     * <code>false</code> otherwise.
     */
    public boolean shouldPersistNameIDFormat(String realm, String hostEntityID, String remoteEntityID,
            String nameIDFormat);
}
