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
 * $Id: IdRepoDataStoreProvider.java,v 1.6 2008/08/06 17:29:26 exu Exp $
 *
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */

package com.sun.identity.plugin.datastore.impl;

import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.openam.utils.CollectionUtils;

/**
 * The <code>IdRepoDataStoreProvider</code> is an implementation of 
 * <code>DataStoreProvider</code> using <code>IdRepo</code> APIs. It can be
 * used for getting/setting user attributes, as well as searching user.
 *
 * @see com.sun.identity.plugin.datastore.DataStoreProvider
 */
public class IdRepoDataStoreProvider implements DataStoreProvider {
    
    private static ResourceBundle bundle = 
        Locale.getInstallResourceBundle("fmDataStoreProvider");
    private static Debug debug = Debug.getInstance("libPlugins");
    // Identity repository instance map
    private static Map idRepoMap = new HashMap();


    /**
     * Default Constructor.
     */
    public IdRepoDataStoreProvider() {
        debug.message("IdRepoDataStoreProvider.constructor()");
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
    public Set<String> getAttribute(String userID, String attrName) throws DataStoreProviderException {

        if (userID == null) {
            throw new DataStoreProviderException(bundle.getString(
                "nullUserId"));
        }

        if (attrName == null) {
            throw new DataStoreProviderException(bundle.getString(
                "nullAttrName"));
        }

        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentity amId = IdUtils.getIdentity(adminToken, userID);
            return amId.getAttribute(attrName);
        } catch (SSOException ssoe) {
            debug.error("IdRepoDataStoreProvider.getAttribute(1): "
                + "invalid admin SSOtoken", ssoe);
            throw new DataStoreProviderException(ssoe);
        } catch (IdRepoException ide) {
            debug.error("IdRepoDataStoreProvider.getAttribute(1): "
                + "IdRepo exception", ide);
            throw new DataStoreProviderException(ide);
        }
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
            throws DataStoreProviderException {

        if (userID == null) {
            throw new DataStoreProviderException(bundle.getString(
                "nullUserId"));
        }

        if (attrNames == null) {
            throw new DataStoreProviderException(bundle.getString(
                "nullAttrSet"));
        }

        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentity amId = IdUtils.getIdentity(adminToken, userID);
            return amId.getAttributes(attrNames);
        } catch (SSOException ssoe) {
            debug.error("IdRepoDataStoreProvider.getAttribute(2): "
                + "invalid admin SSOtoken", ssoe);
            throw new DataStoreProviderException(ssoe);
        } catch (IdRepoException ide) {
            debug.error("IdRepoDataStoreProvider.getAttribute(2): "
                + "IdRepo exception", ide);
            throw new DataStoreProviderException(ide);
        }
    }

    /**
     * Returns binary values for a given attribute.
     * @param userID Universal identifier of the user.
     * @param attrName Name of the attribute whose value to be retrieved.
     * @return A byte array of the byte values for the attribute.
     * @throws DataStoreProviderException if unable to retrieve the attribute.
     */
    public byte[][] getBinaryAttribute(String userID, String attrName) throws DataStoreProviderException {

        if (userID == null) {
            throw new DataStoreProviderException(bundle.getString("nullUserId"));
        }

        if (attrName == null) {
            throw new DataStoreProviderException(bundle.getString("nullAttrName"));
        }

        Set<String> attributes = CollectionUtils.asSet(attrName);

        // There is currently no getBinaryAttribute in AMIdentity, leverage the getBinaryAttributes call
        Map<String, byte[][]> results = getBinaryAttributes(userID, attributes);
        return results.get(attrName);
    }

    /**
     * Returns binary attribute values for a user.
     * @param userID Universal identifier of the user.
     * @param attrNames Set of attributes whose values are to be retrieved.
     * @return Map containing attribute key/value pair, key is the
     *  attribute name, value is a Set of byte[][] values.
     * @throws DataStoreProviderException if unable to retrieve the values.
     */
    public Map<String, byte[][]> getBinaryAttributes(String userID, Set<String> attrNames)
            throws DataStoreProviderException {

        if (userID == null) {
            throw new DataStoreProviderException(bundle.getString("nullUserId"));
        }

        if (attrNames == null) {
            throw new DataStoreProviderException(bundle.getString("nullAttrSet"));
        }

        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentity amId = IdUtils.getIdentity(adminToken, userID);
            return amId.getBinaryAttributes(attrNames);
        } catch (SSOException ssoe) {
            debug.error("IdRepoDataStoreProvider.getBinaryAttributes(): invalid admin SSOToken", ssoe);
            throw new DataStoreProviderException(ssoe);
        } catch (IdRepoException ide) {
            debug.error("IdRepoDataStoreProvider.getBinaryAttributes(): IdRepo exception", ide);
            throw new DataStoreProviderException(ide);
        }
    }

    /**
     * Sets attributes for a user. 
     * @param userID Universal identifier of the user. 
     * @param attrMap Map of attributes to be set, key is the
     *  attribute name and value is a Set containing the attribute values.
     * @throws DataStoreProviderException if unable to set values. 
     */
    public void setAttributes(String userID, Map<String, Set<String>> attrMap) throws DataStoreProviderException
    {
        if (userID == null) {
            throw new DataStoreProviderException(bundle.getString(
               "nullUserId"));
        }
        if (attrMap == null) {
            throw new DataStoreProviderException(bundle.getString(
                "nullAttrMap"));
        }

        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentity amId = IdUtils.getIdentity(adminToken, userID);
            amId.setAttributes(attrMap);
            amId.store();
        } catch (SSOException ssoe) {
            debug.error("IdRepoDataStoreProvider.setAttribute(): "
                + "invalid admin SSOtoken", ssoe);
            throw new DataStoreProviderException(ssoe);
        } catch (IdRepoException ide) {
            debug.error("IdRepoDataStoreProvider.setAttribute(): "
                + "IdRepo exception", ide);
            throw new DataStoreProviderException(ide);
        }
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
        if (orgDN == null) {
            orgDN = SMSEntry.getRootSuffix();
        }
        
        if (avPairs == null || avPairs.isEmpty()) {
            throw new DataStoreProviderException(bundle.getString(
                "nullAvPair"));
        }
        Set amIdSet = null;
        try {
            IdSearchControl searchControl = getIdSearchControl(avPairs, IdSearchOpModifier.AND);
            AMIdentityRepository idRepo = getAMIdentityRepository(orgDN);
            IdSearchResults searchResults = idRepo.searchIdentities(IdType.USER, "*", searchControl);
            amIdSet = searchResults.getSearchResults();
        } catch (IdRepoException ame) {
            debug.error("IdRepoDataStoreProvider.getUserID(): IdRepoException",
                ame);
            throw new DataStoreProviderException(ame);
        } catch (SSOException ssoe) {
            debug.error("IdRepoDataStoreProvider.getUserID() : SSOException", 
                ssoe);
            throw new DataStoreProviderException(ssoe);
        }
        if (amIdSet == null || amIdSet.isEmpty()) {
            debug.message("IdRepoDataStoreProvider.getUserID : user not found");
            return null;
        } else if (amIdSet.size() > 1) {
            debug.message("IdRepoDataStoreProvider.getUserID : multiple match");
            throw new DataStoreProviderException(bundle.getString(
                "multipleMatches"));
        }
        // single user found.
        final AMIdentity amId = (AMIdentity)amIdSet.iterator().next();
        final String universalId = IdUtils.getUniversalId(amId);

        if (debug.messageEnabled()) {
            debug.message("IdRepoDataStoreProvider.getUserID()"
                + " Name=: " + amId.getName()
                + " DN=: " + amId.getDN()
                + " univId=: " + universalId);
        }

        return universalId;
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
        if (userID == null) {
            throw new DataStoreProviderException(bundle.getString(
                "nullUserId"));
        }

        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentity amId = IdUtils.getIdentity(adminToken, userID);
            // treat inactive as user does not exist
            return amId.isActive();
        } catch (IdRepoException ide) {
            debug.message("IdRepoDataStoreProvider.isUserExists()", ide);
            return false;
        } catch (SSOException ssoe) {
            debug.error("IdRepoDataStoreProvider.isUserExists() : SSOException",
                ssoe);
            throw new DataStoreProviderException(ssoe);
        }
    }

    /**
     * Returns identity repository for a realm.
     * @param realm Name of the realm.
     * @return identity repository for a realm.
     * @throws DataStoreProviderException if unable to get the identity 
     *  repository for the realm. 
     */
    private synchronized AMIdentityRepository getAMIdentityRepository(
        String realm)
        throws DataStoreProviderException
    {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        AMIdentityRepository amIdentityRepository = (AMIdentityRepository) idRepoMap.get(realm);
        if (amIdentityRepository == null) {
            amIdentityRepository = new AMIdentityRepository(realm, adminToken);
            idRepoMap.put(realm, amIdentityRepository);
            if (debug.messageEnabled()) {
                debug.message("IdRepoDataStoreProvider.getAMIdRepo : "
                     + " create IdRepo for realm " + realm);
            }
        }
        return amIdentityRepository;
    } 

    /**
     * Returns <code>IdSearchControl</code> object.
     * @param avPairs   Attribute key/value pairs that is used to construct
     *                  search control. Key is the attribute name, value
     *                  is a Set containing attribute value(s).
     * @param modifier  Search modification, could be one of:
     *                  <code>IdSearchOpModifier.OR</code>
     *                  <code>IdSearchOpModifier.AND</code>.
     * @return          <code>IdSearchControl</code> object, null if the
     *                  passing map is null.
     */
    private static IdSearchControl getIdSearchControl(
        Map avPairs, IdSearchOpModifier modifier) 
    {
        if ((avPairs == null) || avPairs.isEmpty()) {
            return null;
        }
        IdSearchControl searchControl = new IdSearchControl();
        searchControl.setTimeOut(0);
        searchControl.setMaxResults(0);
        searchControl.setAllReturnAttributes(false);
        searchControl.setSearchModifiers(IdSearchOpModifier.AND, avPairs);
        return searchControl;
    }

}
