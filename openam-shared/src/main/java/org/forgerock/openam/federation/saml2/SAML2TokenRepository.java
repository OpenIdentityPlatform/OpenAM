/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.federation.saml2;

import java.util.List;

/**
 *
 * Enables the storing/recovering serialized state of SAML2 Tokens when in SAML2 failover-mode.
 * It is in the OpenAM Shared package so that the Federation Library does not need to have
 * a dependency on OpenAM Core and visa-versa.
 *
 */
public interface SAML2TokenRepository {

    /**
    * Retrieves an existing SAML2 object from the CTS as an un-cast Object using the passed primary key.
    *
    * @param primaryKey primary key
    * @return The object found in the repository or null if not found.
    * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
    */
    public Object retrieveSAML2Token(String primaryKey) throws SAML2TokenRepositoryException;

   /**
    * Retrieves a list of existing SAML2 objects from the CTS as un-cast Objects using the passed secondary key.
    *
    * @param secondaryKey Secondary Key
    * @return A non null, but possibly empty collection of SAML2 objects.
    * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
    */
   public List<Object> retrieveSAML2TokensWithSecondaryKey(String secondaryKey) throws SAML2TokenRepositoryException;

   /**
    * Deletes the SAML2 object by using the passed primary key from the repository
    *
    * @param primaryKey primary key
    * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
    */
   public void deleteSAML2Token(String primaryKey) throws SAML2TokenRepositoryException;

   /**
    * Saves SAML2 data into the SAML2 Repository.
    *
    * @param primaryKey Primary key.
    * @param secondaryKey Secondary Key, can be null
    * @param samlObj the SAML2 object to store such as Response, IDPSession.
    * @param expirationTime Expiration time in seconds from epoch.
    * @throws SAML2TokenRepositoryException if there was a problem accessing the SAML2 Token Repository
    */
    public void saveSAML2Token(String primaryKey, String secondaryKey, Object samlObj, long expirationTime)
            throws SAML2TokenRepositoryException;
}
