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
 * $Id: FedletDataStoreProvider.java,v 1.3 2008/08/06 17:28:14 exu Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.sun.identity.plugin.datastore.impl;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.shared.debug.Debug;

import java.util.Map;
import java.util.Set;

/**
 * The <code>FedletDataStoreProvider</code> is an implementation of 
 * <code>DataStoreProvider</code> for Fedlet deployment. The implemetation
 * performs no operation on those methods.
 *
 * @see com.sun.identity.plugin.datastore.DataStoreProvider
 */
public class FedletDataStoreProvider implements DataStoreProvider {
    
    private static Debug debug = Debug.getInstance("libPlugins");

    /**
     * Default Constructor.
     */
    public FedletDataStoreProvider() {
        debug.message("FedletDataStoreProvider.constructor()");
    }

    /**
     * Initializes the provider.
     * @param componentName name of the component.
     * @throws DataStoreProviderException if an error occurred during
     *  initialization.
     */
    public void init(String componentName)
        throws DataStoreProviderException
    {
    }

    /**
     * Returns values for a given attribute. 
     * @param userID Universal identifier of the user.
     * @param attrName Name of the attribute whose value to be retrieved.
     * @return Set of the values for the attribute.
     * @throws DataStoreProviderException if unable to retrieve the attribute. 
     */
    public Set<String> getAttribute(String userID, String attrName)
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.getAttribute(String, String)");
        return null;
    }

    /**
     * Returns attribute values for a user. 
     * @param userID Universal identifier of the user. 
     * @param attrNames Set of attributes whose values are to be retrieved.
     * @return Map containing attribute key/value pair, key is the
     *  attribute name, value is a Set of values. 
     * @throws DataStoreProviderException if unable to retrieve the values. 
     */
    public Map<String, Set<String>> getAttributes(String userID, Set<String> attrNames)
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.getAttribute(String, Set)");
        return null;
    }

    /**
     * Returns values for a given attribute.
     * @param userID Universal identifier of the user.
     * @param attrName Name of the attribute whose value to be retrieved.
     * @return Set of the values for the attribute.
     * @throws DataStoreProviderException if unable to retrieve the attribute.
     */
    public byte[][] getBinaryAttribute(String userID, String attrName)
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.getBinaryAttribute(String, String)");
        return null;
    }

    /**
     * Returns attribute values for a user.
     * @param userID Universal identifier of the user.
     * @param attrNames Set of attributes whose values are to be retrieved.
     * @return Map containing attribute key/value pair, key is the
     *  attribute name, value is a Set of values.
     * @throws DataStoreProviderException if unable to retrieve the values.
     */
    public Map<String, byte[][]> getBinaryAttributes(String userID, Set<String> attrNames)
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.getBinaryAttributes(String, Set)");
        return null;
    }

    /**
     * Sets attributes for a user. 
     * @param userID Universal identifier of the user. 
     * @param attrMap Map of attributes to be set, key is the
     *  attribute name and value is a Set containing the attribute values.
     * @throws DataStoreProviderException if unable to set values. 
     */
    public void setAttributes(String userID, Map<String, Set<String>> attrMap)
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.setAttribute(String, Map)");
    }
    
    /**
     * Returns user matching the search criteria.
     * @param orgDN The realm to search the user. If null,
     *  searches the root realm.
     * @param avPairs Attribute key/value pairs that will be used for 
     *  searching the user. Key is the attribute name, value 
     *  is a Set containing attribute value(s).
     * @return Universal identifier of the matching user, null if
     *  the matching user could not be found. 
     * @throws DataStoreProviderException if error occurs during search or
     *  multiple matching users found.
     */
    public String getUserID(String orgDN, Map<String, Set<String>> avPairs)
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.getUserID(String, Map)");
        return null;
    }

    /**
     * Checks if a given user exists.
     * @param userID Universal identifier of the user to be checked.
     * @return <code>true</code> if the user exists.
     * @throws DataStoreProviderException if an error occurred.
     */
    public boolean isUserExists(String userID) 
        throws DataStoreProviderException
    {
        debug.message("FedletDataStoreProvider.isUserExists(String)");
        return false;
    }
}
