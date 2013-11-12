/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: SAML2RepositoryFactory.java,v 1.1 2008/07/22 18:08:20 weisun2 Exp $
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

package com.sun.identity.coretoken.interfaces;

import com.iplanet.dpro.session.exceptions.StoreException;

import java.util.List;

/**
 *
 * This class is used in SAML2 Token Persistence to store/recover serialized
 * state of IDPSession/Response objects.
 *
 */
public interface AMTokenSAML2Repository {

    /**
    * Retrives existing SAML2 object from persistent Repository.
    *
    * @param samlKey primary key 
    * @return Object - SAML2 unMarshaled Object, if failed, return null.
    */
   public Object retrieveSAML2Token(String samlKey) throws StoreException;

   /**
    * Retrieves a list of existing SAML2 object from persistent Repository with the Secondary Key.
    *
    * @param secKey Secondary Key 
    * @return List<Object> - List of SAML2 unMarshaled Objects, if failed, return null.
    */
   public List<Object> retrieveSAML2TokenWithSecondaryKey(String secKey) throws StoreException;

   /**
    * Deletes the SAML2 object by given primary key from the repository
    * @param samlKey primary key 
    */
   public void deleteSAML2Token(String samlKey) throws StoreException;

    /**
     * Deletes expired SAML2 object from the repository
     */
    public void deleteExpiredSAML2Tokens() throws StoreException;

   /**
    * Saves SAML2 data into the SAML2 Repository.
    *
    * @param samlKey Primary key.
    * @param samlObj SAML object such as Response, IDPSession.
    * @param expirationTime Expiration time in seconds from epoch.
    * @param secKey Secondary Key.
    */
    public void saveSAML2Token(String samlKey, Object samlObj, long expirationTime,
        String secKey) throws StoreException;
}
