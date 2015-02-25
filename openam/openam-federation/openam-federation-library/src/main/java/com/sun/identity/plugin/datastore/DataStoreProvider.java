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
 * $Id: DataStoreProvider.java,v 1.2 2008/06/25 05:47:27 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.sun.identity.plugin.datastore;

import java.util.Map;
import java.util.Set;


/**
 * Interface used for storing & retrieving information. Also used to search
 * user.
 * @supported.all.api
 */
public interface DataStoreProvider {
    
    /**
     * Initializes the provider.
     * @param componentName Component name, such as saml, saml2, id-ff, disco,
     *  authnsvc, and idpp.
     * @throws DataStoreProviderException if an error occurred during
     *  initialization.
     */
    public void init(String componentName)
        throws DataStoreProviderException;

    /**
     * Gets user attribute.
     * @param userID ID value for the user.
     * @param attrName Name of the attribute whose value to be retrieved.
     * @return Set of the values for the attribute.
     * @throws DataStoreProviderException if an error occurred.
     */
    public Set<String> getAttribute(String userID, String attrName)
        throws DataStoreProviderException;

    /**
     * Gets user attributes.
     * @param userID ID value for the user.
     * @param attrNames The Set of attribute names.
     * @return Map of specified attributes. Map key is the attribute
     *  name and value is the attribute value Set.
     * @throws DataStoreProviderException if an error occurred.
     */

    public Map<String, Set<String>> getAttributes(String userID, Set<String> attrNames)
        throws DataStoreProviderException;

    /**
     * Gets user binary attribute.
     * @param userID ID value for the user.
     * @param attrName Name of the attribute whose value to be retrieved.
     * @return Set of the values for the attribute.
     * @throws DataStoreProviderException if an error occurred.
     */
    public byte[][] getBinaryAttribute(String userID, String attrName)
        throws DataStoreProviderException;

    /**
     * Gets user binary attributes.
     * @param userID ID value for the user.
     * @param attrNames The Set of attribute names.
     * @return Map of specified attributes. Map key is the attribute
     *  name and value is the attribute value Set.
     * @throws DataStoreProviderException if an error occurred.
     */
    public Map<String, byte[][]> getBinaryAttributes(String userID, Set<String> attrNames)
        throws DataStoreProviderException;

    /**
     * Sets user attributes.
     * @param userID ID value for the user.
     * @param attrMap Map of specified attributes to be set. Map key is
     *  the attribute name and value is the attribute value Set.
     * @throws DataStoreProviderException if an error occurred.
     */
    public void setAttributes(String userID, Map<String, Set<String>> attrMap)
        throws DataStoreProviderException;

    /**
     * Searches user.
     * @param orgDN The organization to search the user.
     * @param avPairs Attribute value pairs that will be used for searching
     *  the user.
     * @throws DataStoreProviderException if an error occurred.
     */
    public String getUserID(String orgDN, Map<String, Set<String>> avPairs)
        throws DataStoreProviderException;

    /**
     * Checks if the user exists with a given userid.
     * @param userID ID of an user
     * @return <code>true</code> if the user exists; <code>false</code>
     *  otherwise.
     * @throws DataStoreProviderException if an error occurred.
     */
    public boolean isUserExists(String userID) 
       throws DataStoreProviderException;

}
