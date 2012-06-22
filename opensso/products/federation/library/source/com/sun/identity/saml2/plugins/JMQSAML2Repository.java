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
 * $Id: JMQSAML2Repository.java,v 1.3 2008/08/01 22:15:00 hengming Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import java.util.List;

/**
 * This class is used in SAML2 failover mode to store/recover serialized
 * state of IDPSession/Response object.
 */
public interface JMQSAML2Repository {

   /**
    * Retrives existing SAML2 object from persistent datastore
    * @param samlKey primary key 
    * @return SAML2 object, if failed, return null. 
    */
   public Object retrieve(String samlKey);

   /**
    * Retrives a list of existing SAML2 object from persistent datastore with
    * secodaryKey
    *
    * @param secKey Secondary Key 
    * @return SAML2 object, if failed, return null. 
    */
   public List retrieveWithSecondaryKey(String secKey);

   /**
    * Deletes the SAML2 object by given primary key from the repository
    * @param samlKey primary key 
    */
   public void delete(String samlKey);

    /**
     * Deletes expired SAML2 object from the repository
     * @exception When Unable to delete the expired SAML2 object
     */
    public void deleteExpired();

   /**
    * Saves SAML2 data into the SAML2 Repository
    * @param samlKey primary key 
    * @param samlObj saml object such as Response, IDPSession
    * @param expirationTime expiration time 
    * @param secKey Secondary Key 
    */
    public void save(String samlKey, Object samlObj, long expirationTime,
        String secKey);
}
