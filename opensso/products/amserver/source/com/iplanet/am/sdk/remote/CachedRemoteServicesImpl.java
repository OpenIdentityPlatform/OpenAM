/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CachedRemoteServicesImpl.java,v 1.6 2009/11/20 23:52:52 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.iplanet.am.sdk.remote;

import java.security.AccessController;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.security.AdminTokenAction;

import com.iplanet.am.sdk.AMEntryExistsException;
import com.iplanet.am.sdk.AMEvent;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.AMNamingAttrManager;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.common.CacheBlock;
import com.iplanet.am.sdk.common.CacheStats;
import com.iplanet.am.sdk.common.ICachedDirectoryServices;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.sdk.common.MiscUtils;
import com.iplanet.am.util.Cache;
import com.iplanet.am.util.SystemProperties;

public class CachedRemoteServicesImpl extends RemoteServicesImpl implements
        ICachedDirectoryServices {
    private static int maxSize = 10000;

    private static IDirectoryServices instance = null;

    static final String CACHE_MAX_SIZE_KEY = "com.iplanet.am.sdk.cache.maxSize";

    protected static String NSROLEDN_ATTR = "nsroledn";

    protected static String NSROLE_ATTR = "nsrole";

    // Class Private
    private Cache sdkCache;

    private CacheStats cacheStats;

    static {
        initializeParams();
    }

    /**
     * Method to check if caching is enabled or disabled and configure the size
     * of the cache accordingly.
     */
    private static void initializeParams() {
        // Check if the caching property is set in System runtime.
        String cacheSize = SystemProperties.get(CACHE_MAX_SIZE_KEY, "10000");
        try {
            maxSize = Integer.parseInt(cacheSize);
            if (maxSize < 1) {
                maxSize = 10000; // Default
            }
            if (getDebug().messageEnabled()) {
                getDebug().message( "CachedRemoteServicesImpl."
                        + "intializeParams() Caching size set to: " + maxSize);
            }
        } catch (NumberFormatException ne) {
            maxSize = 10000;
            getDebug().warning("CachedRemoteServicesImpl.initializeParams() "
                    + "- invalid value for cache size specified. Setting "
                    + "to default value: " + maxSize);
        }
    }

    private CachedRemoteServicesImpl() {
        super();
        initializeCache();
        cacheStats = CacheStats.createInstance(getClass().getName(), 
                getDebug());
    }

    private void initializeCache() {
        sdkCache = new Cache(maxSize);
    }

    /**
     * Method to get the current cache size
     * 
     * @return the size of the SDK LRU cache
     */
    public int getSize() {
        return sdkCache.size();
    }

    protected static synchronized IDirectoryServices getInstance() {
        if (instance == null) {
            getDebug().message("CachedRemoteServicesImpl.getInstance(): "
                    + "Creating new Instance of CachedRemoteServicesImpl()");
            instance = new CachedRemoteServicesImpl();
        }
        return instance;
    }

    /**
     * Method to get the maximum size of the Cache. To be called by all other
     * LRU Caches that are created in AM SDK
     * 
     * @return the maximum cache size for a LRU cache
     */
    protected static int getMaxSize() {
        return maxSize;
    }

    /**
     * Prints the contents of the cache. For getDebug() purpose only
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n<<<<<<< BEGIN SDK CACHE CONTENTS >>>>>>>>");
        if (!sdkCache.isEmpty()) { // Should never be null
            Enumeration cacheKeys = sdkCache.keys();
            while (cacheKeys.hasMoreElements()) {
                String key = (String) cacheKeys.nextElement();
                CacheBlock cb = (CacheBlock) sdkCache.get(key);
                sb.append("\nSDK Cache Block: ").append(key);
                sb.append(cb.toString());
            }
        } else {
            sb.append("<empty>");
        }
        sb.append("\n<<<<<<< END SDK CACHE CONTENTS >>>>>>>>");
        return sb.toString();
    }

    // *************************************************************************
    // Update/Dirty methods of this class.
    // *************************************************************************
    private void removeCachedAttributes(String affectDNs, Set attrNames) {
        Enumeration cacheKeys = sdkCache.keys();
        while (cacheKeys.hasMoreElements()) {
            String key = (String) cacheKeys.nextElement();
            int l1 = key.length();
            int l2 = affectDNs.length();
            if (key.regionMatches(true, (l1 - l2), affectDNs, 0, l2)) {
                // key ends with 'affectDN' string
                CacheBlock cb = (CacheBlock) sdkCache.get(key);
                if (cb != null && !cb.hasExpiredAndUpdated() && cb.isExists()) {
                    cb.removeAttributes(attrNames);
                }
            }
        }
    }

    private void clearCachedEntries(String affectDNs) {
        Enumeration cacheKeys = sdkCache.keys();
        while (cacheKeys.hasMoreElements()) {
            String key = (String) cacheKeys.nextElement();
            int l1 = key.length();
            int l2 = affectDNs.length();
            if (key.regionMatches(true, (l1 - l2), affectDNs, 0, l2)) {
                // key ends with 'affectDN' string
                CacheBlock cb = (CacheBlock) sdkCache.get(key);
                if (cb != null) {
                    cb.clear();
                }
            }
        }
    }

    /**
     * This method will be called by <code>AMIdRepoListener</code>. This
     * method will update the cache by removing all the entires which are
     * affected as a result of an event notification caused because of
     * changes/deletions/renaming of entries with and without aci's.
     * 
     * <p>
     * NOTE: The event could have been caused either by changes to an aci entry
     * or a costemplate or a cosdefinition or changes to a normal entry
     * 
     * @param dn
     *            name of entity being modified
     * @param eventType
     *            type of modification
     * @param cosType
     *            true if it is cos related. false otherwise
     * @param aciChange
     *            true if it is aci related. false otherwise
     * @param attrNames
     *            Set of attribute Names which should be removed from the
     *            CacheEntry in the case of COS change
     */
    public void dirtyCache(String dn, int eventType, boolean cosType,
            boolean aciChange, Set attrNames) {
        CacheBlock cb;
        String origdn = dn;
        dn = MiscUtils.formatToRFC(dn);
        switch (eventType) {
        case AMEvent.OBJECT_ADDED:
            cb = (CacheBlock) sdkCache.get(dn);
            if (cb != null) { // Mark an invalid entry as valid now
                cb.setExists(true);
            }
            if (cosType) { // A cos type event remove all affected attributes
                removeCachedAttributes(dn, attrNames);
            }
            break;
        case AMEvent.OBJECT_REMOVED:
            cb = (CacheBlock) sdkCache.remove(dn);
            if (cb != null) {
                cb.clear(); // Clear anyway & help the GC process
            }
            if (cosType) {
                removeCachedAttributes(dn, attrNames);
            }
            break;
        case AMEvent.OBJECT_RENAMED:
            // Better to remove the renamed entry, or else it will be just
            // hanging in the cache, until LRU kicks in.
            cb = (CacheBlock) sdkCache.remove(dn);
            if (cb != null) {
                cb.clear(); // Clear anyway & help the GC process
            }
            if (cosType) {
                removeCachedAttributes(dn, attrNames);
            }
            break;
        case AMEvent.OBJECT_CHANGED:
            cb = (CacheBlock) sdkCache.get(dn);
            if (cb != null) {
                cb.clear(); // Just clear the entry. Don't remove.
            }
            if (cosType) {
                removeCachedAttributes(dn, attrNames);
            } else if (aciChange) { // Clear all affected entries
                clearCachedEntries(dn);
            }
            break;
        }
        if (getDebug().messageEnabled()) {
            getDebug().message("CachedRemoteServicesImpl.dirtyCache(): "
                    + "Cache dirtied because of Event Notification. Parameters"
                    + " - eventType: " + eventType
                    + ", cosType: " + cosType + ", aciChange: "
                    + aciChange + ", fullDN: " + origdn
                    + "; rfcDN =" + dn);
        }
    }

    /**
     * This method is used to clear the entire SDK cache in the event that
     * EventService notifies that all entries have been modified (or should be
     * marked dirty).
     * 
     */
    public synchronized void clearCache() {
        sdkCache.clear();
        initializeCache();
    }

    private synchronized void removeFromCache(String dn) {
        String key = MiscUtils.formatToRFC(dn);
        sdkCache.remove(key);
    }

    private void dirtyCache(String dn) {
        String key = MiscUtils.formatToRFC(dn);
        CacheBlock cb = (CacheBlock) sdkCache.get(key);
        if (cb != null) {
            cb.clear();
        }
    }

    private void dirtyCache(Set entries) {
        Iterator itr = entries.iterator();
        while (itr.hasNext()) {
            String entryDN = (String) itr.next();
            String key = MiscUtils.formatToRFC(entryDN);
            CacheBlock cb = (CacheBlock) sdkCache.get(key);
            if (cb != null) {
                cb.clear();
            }
        }
    }

    /**
     * Method that updates the cache entries locally. This method does a write
     * through cache
     */
    private void updateCache(SSOToken token, String dn, Map stringAttributes,
            Map byteAttributes) throws SSOException {
        String key = MiscUtils.formatToRFC(dn);
        CacheBlock cb = (CacheBlock) sdkCache.get(key);
        if (cb != null && !cb.hasExpiredAndUpdated() && cb.isExists()) {
            String pDN = MiscUtils.getPrincipalDN(token);
            cb.replaceAttributes(pDN, stringAttributes, byteAttributes);
        }
    }

    // ***********************************************************************
    // Overriden Methods of RemoteServicesImpl class below
    // ***********************************************************************
    public void createEntry(SSOToken token, String entryName, int objectType,
            String parentDN, Map attributes) throws AMEntryExistsException,
            AMException, SSOException {

        super.createEntry(token, entryName, objectType, parentDN, attributes);
        String cacheDN = AMNamingAttrManager.getNamingAttr(objectType) + "="
                + entryName + "," + parentDN;
        removeFromCache(cacheDN);
    }

    /**
     * Method to be called to validate the entry before any of the get/put/
     * remove methods are called.
     * 
     * @throws AMException
     *             if the entry does not exist in the DS
     */
    private void validateEntry(SSOToken token, CacheBlock cb)
            throws AMException {
        if (!cb.hasExpiredAndUpdated() && !cb.isExists()) {
            // Entry does not exist in DS, invalid entry
            String params[] = { cb.getEntryDN() };
            boolean isPresent = super.doesEntryExists(token, params[0]);
            if (getDebug().messageEnabled()) {
                getDebug().message(
                        "CachedRemoteServicesImpl.validateEntry():" + " DN"
                                + params[0] + " got from DS & exists: "
                                + isPresent);
            }
            if (isPresent) {
                // Intialize the CacheBlock based on isPresent
                // else throw '461' exception/error message.
                // This is for certain containers created dynamically.
                // eg. ou=agents,ou=container,ou=agents.
                String dn = MiscUtils.formatToRFC(params[0]);
                cb = new CacheBlock(params[0], isPresent);
                sdkCache.put(dn, cb);
            } else {
                String locale = MiscUtils.getUserLocale(token);
                throw new AMException(AMSDKBundle.getString("461", params,
                        locale), "461", params);
            }
        }
    }

    public boolean doesEntryExists(SSOToken token, String entryDN) {
        String dn = MiscUtils.formatToRFC(entryDN);
        CacheBlock cb = (CacheBlock) sdkCache.get(dn);
        if (cb != null && !cb.hasExpiredAndUpdated()) {
            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "doesEntryExist(): entryDN: " + entryDN
                        + " found in cache & exists: " + cb.isExists());
            }
            return cb.isExists();
        } else {
            boolean isPresent = super.doesEntryExists(token, dn);
            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "doesEntryExist(): entryDN: " + entryDN
                        + " got from DS & exists: " + isPresent);
            }
            // Intialize the CacheBock based on isPresent
            // Intialize the CacheBock based on isPresent
            if (cb == null) {
                cb = new CacheBlock(entryDN, isPresent);
                sdkCache.put(dn, cb);
            } else { // Cache Block might have just expired, just reset the
                // isExists flag once again
                cb.setExists(isPresent);
            }
            return isPresent;
        }
    }

    private void setOrganizationDNs(String organizationDN, Set childDNSet) {
        Iterator itr = childDNSet.iterator();
        while (itr.hasNext()) {
            String cDN = (String) itr.next();
            CacheBlock cb = (CacheBlock) sdkCache.get(cDN);
            if (cb == null) {
                cb = new CacheBlock(cDN, organizationDN, true);
                sdkCache.put(cDN, cb);
            } else {
                cb.setOrganizationDN(organizationDN);
            }
        }
        if (getDebug().messageEnabled() && !childDNSet.isEmpty()) {
            getDebug().message("CachedRemoteServicesImpl."
                    + "setOrganizationDNs(): Set org DNs as: "
                    + organizationDN + " for children: " + childDNSet);
        }
    }

    public void updateUserAttribute(SSOToken token, Set members,
            String staticGroupDN, boolean toAdd) throws AMException {
        super.updateUserAttribute(token, members, staticGroupDN, toAdd);
        // Note here we are updating the cache only after all user attributes
        // are set. Even if the operation fails for a particular user then, we
        // will not be updating the cache. It should be okay as event
        // notification would clean up.
        dirtyCache(members); // TODO: Just remove the modified attr
    }

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * <p>
     * <b>NOTE:</b> This method will involve serveral directory searches, hence
     * be cautious of Performance hit.
     * 
     * <p>
     * This method does not call its base classes method unlike the rest of the
     * overriden methods to obtain the organization DN, as it requires special
     * processing requirements.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String getOrganizationDN(SSOToken token, String entryDN)
            throws AMException {
        DN dnObject = new DN(entryDN);
        if (entryDN.length() == 0 || !dnObject.isDN()) {
            getDebug().error(
                    "CachedRemoteServicesImpl.getOrganizationDN() "
                            + "Invalid DN: " + entryDN);
            throw new AMException(token, "157");
        }

        String organizationDN = "";
        Set childDNSet = new HashSet();
        boolean errorCondition = false;
        boolean found = false;
        while (!errorCondition && !found) {
            boolean lookupDirectory = true;
            String childDN = dnObject.toRFCString().toLowerCase();
            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "getOrganizationDN() - looping Organization DN for"
                        + " entry: " + childDN);
            }

            CacheBlock cb = (CacheBlock) sdkCache.get(childDN);
            if (cb != null) {
                organizationDN = cb.getOrganizationDN();
                if (organizationDN != null) {
                    if (getDebug().messageEnabled()) {
                        getDebug().message("CachedRemoteServicesImpl."
                                + "getOrganizationDN(): found OrganizationDN: "
                                + organizationDN + " for: "
                                + childDN);
                    }
                    found = true;
                    setOrganizationDNs(organizationDN, childDNSet);
                    continue;
                } else if (cb.getObjectType() == AMObject.ORGANIZATION
                        || cb.getObjectType() == AMObject.ORGANIZATIONAL_UNIT) {
                    // Object type is organization
                    organizationDN = childDN;
                    found = true;
                    childDNSet.add(childDN);
                    setOrganizationDNs(organizationDN, childDNSet);
                    continue;
                } else if (cb.getObjectType() != 
                    AMObject.UNDETERMINED_OBJECT_TYPE) {
                    // Don't lookup directory if the object type is unknown
                    lookupDirectory = false;
                }
            }
            childDNSet.add(childDN);
            if (lookupDirectory) {
                organizationDN = super.verifyAndGetOrgDN(token, entryDN,
                        childDN);
            }
            if (organizationDN != null && organizationDN.length() > 0) {
                found = true;
                setOrganizationDNs(organizationDN, childDNSet);
            } else if (dnObject.countRDNs() == 1) { // Reached topmost level
                errorCondition = true;
                getDebug().error("CachedRemoteServicesImpl."
                        + "getOrganizationDN(): Reached root suffix. Unable to"
                        + " get parent Org");
            } else { // Climb tree on level up
                dnObject = dnObject.getParent();
            }
        }
        return organizationDN;
    }

    /**
     * Gets the type of the object given its DN.
     * 
     * @param token
     *            token a valid SSOToken
     * @param dn
     *            DN of the object whose type is to be known.
     * 
     * @throws AMException
     *             if the data store is unavailable or if the objecttype is
     *             unknown
     * @throws SSOException
     *             if ssoToken is invalid or expired.
     */
    public int getObjectType(SSOToken token, String dn) throws AMException,
            SSOException {
        int objectType = AMObject.UNDETERMINED_OBJECT_TYPE;
        String entryDN = MiscUtils.formatToRFC(dn);
        CacheBlock cb = (CacheBlock) sdkCache.get(entryDN);
        if (cb != null) {
            // Check if the entry exists, if not present throw an exception
            if (!doesEntryExists(token, dn)) {
                String locale = MiscUtils.getUserLocale(token);
                String params[] = { cb.getEntryDN() };
                throw new AMException(AMSDKBundle.getString("461", params,
                    locale), "461", params);
            }
            validateEntry(token, cb);
            objectType = cb.getObjectType();
            if (objectType != AMObject.UNDETERMINED_OBJECT_TYPE) {
                return objectType;
            }
        }

        // Use admintoken to get object type, so that it can be cached
        SSOToken adminToken = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());

        // The method below will throw an AMException if the entry does not
        // exist in the directory. If it exists, then create a cache entry for
        // this DN
        objectType = super.getObjectType(adminToken, entryDN);
        if (cb == null) {
            cb = new CacheBlock(entryDN, true);
            sdkCache.put(entryDN, cb);
        }
        cb.setObjectType(objectType);
        if (objectType == AMObject.ORGANIZATION
                || objectType == AMObject.ORGANIZATIONAL_UNIT) {
            cb.setOrganizationDN(entryDN);
        }
        return objectType;
    }

    /**
     * Returns attributes from an external data store.
     * 
     * @param token
     *            Single sign on token of user
     * @param entryDN
     *            DN of the entry user is trying to read
     * @param attrNames
     *            Set of attributes to be read
     * @param profileType
     *            Integer determining the type of profile being read
     * @return A Map of attribute-value pairs
     * @throws AMException
     *             if an error occurs when trying to read external datastore
     */
    public Map getExternalAttributes(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException {

        String eDN;
        if (profileType == AMObject.USER) {
            eDN = (new DN(entryDN)).getParent().toString();
        } else {
            eDN = entryDN;
        }
        String orgDN = getOrganizationDN(token, eDN);
        if (callBackHelperBase.isExternalGetAttributesEnabled(orgDN)) {
            return super.getExternalAttributes(token, entryDN, attrNames,
                    profileType);
        } else {
            return null;
        }
    }

    public Map getAttributes(SSOToken token, String entryDN, int profileType)
            throws AMException, SSOException {
        boolean ignoreCompliance = true;
        boolean byteValues = false;
        return getAttributes(token, entryDN, ignoreCompliance, byteValues,
                profileType);
    }

    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            int profileType) throws AMException, SSOException {
        boolean ignoreCompliance = true;
        boolean byteValues = false;
        return getAttributes(token, entryDN, attrNames, ignoreCompliance,
                byteValues, profileType);
    }

    public Map getAttributesByteValues(SSOToken token, String entryDN,
            int profileType) throws AMException, SSOException {
        // fetch byte values
        boolean byteValues = true;
        boolean ignoreCompliance = true;
        return getAttributes(token, entryDN, ignoreCompliance, byteValues,
                profileType);
    }

    public Map getAttributesByteValues(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException, SSOException {
        // fetch byte values
        boolean byteValues = true;
        boolean ignoreCompliance = true;
        return getAttributes(token, entryDN, attrNames, ignoreCompliance,
                byteValues, profileType);
    }

    /**
     * Gets all attributes corresponding to the entryDN. This method obtains the
     * DC Tree node attributes and also performs compliance related verification
     * checks in compliance mode. Note: In compliance mode you can skip the
     * compliance checks by setting ignoreCompliance to "false".
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the DN of the entry whose attributes need to retrieved
     * @param ignoreCompliance
     *            a boolean value specificying if compliance related entries
     *            need to ignored or not. Ignored if true.
     * @param byteValues
     *            if false StringValues are fetched, if true byte values are
     *            fetched.
     * @param profileType
     *            the oject type of entryDN
     * @return a Map containing attribute names as keys and Set of values
     *         corresponding to each key.
     * @throws AMException
     *             if an error is encountered in fetching the attributes
     */
    public Map getAttributes(SSOToken token, String entryDN,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException {
        // Attributes are being requested; increment cache stats request counter
        cacheStats.incrementRequestCount(getSize());

        String principalDN = MiscUtils.getPrincipalDN(token);
        String dn = MiscUtils.formatToRFC(entryDN);

        if (getDebug().messageEnabled()) {
            getDebug().message("In CachedRemoteServicesImpl.getAttributes("
                    + "SSOToken entryDN, ignoreCompliance) " + "("
                    + principalDN + ", " + entryDN + ", "
                    + ignoreCompliance + " method.");
        }

        CacheBlock cb = (CacheBlock) sdkCache.get(dn);
        AMHashMap attributes = null;
        if (cb != null) {
            validateEntry(token, cb);
            if (cb.hasCompleteSet(principalDN)) {
                cacheStats.updateHitCount(getSize());
                if (getDebug().messageEnabled()) {
                    getDebug().message("CachedRemoteServicesImpl."
                            + "getAttributes(): found all attributes in " 
                            + "Cache.");
                }
                attributes = (AMHashMap) cb.getAttributes(principalDN,
                        byteValues);
            } else { // Get the whole set from DS and store it;
                // ignore incomplete set
                if (getDebug().messageEnabled()) {
                    getDebug().message("CachedRemoteServicesImpl." 
                            + "getAttributes():  complete attribute set NOT "
                            + "found in cache. Getting from DS.");
                }

                attributes = (AMHashMap) super.getAttributes(token, entryDN,
                        ignoreCompliance, byteValues, profileType);
                cb.putAttributes(principalDN, attributes, null, true,
                        byteValues);
            }
        } else { // Attributes not cached
            // Get all the attributes from DS and store them
            attributes = (AMHashMap) super.getAttributes(token, entryDN,
                    ignoreCompliance, byteValues, profileType);
            cb = new CacheBlock(entryDN, true);
            cb.putAttributes(principalDN, attributes, null, true, byteValues);
            sdkCache.put(dn, cb);

            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "getAttributes(): attributes NOT found in cache. "
                        + "Fetched from DS.");
            }
        }

        // Get all external DS attributes by calling plugin modules.
        // Note these attributes should not be cached.
        Map extAttributes = getExternalAttributes(token, entryDN, null,
                profileType);
        if (extAttributes != null && !extAttributes.isEmpty()) {
            // Note the attributes stored in the cache are already copied to a
            // new map. Hence modifying this attributes is okay.
            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "getAttributes(): External attributes present. Adding"
                        + " them with original list");
            }
            attributes.putAll(extAttributes);
        }
        return attributes;
    }

    private AMHashMap getPluginAttrsAndUpdateCache(SSOToken token,
            String principalDN, String entryDN, CacheBlock cb,
            AMHashMap attributes, Set missAttrNames, boolean byteValues,
            int profileType) throws AMException {
        // Get all external attributes by calling plugin modules.
        // Note these attributes should not be cached.
        Map extAttributes = getExternalAttributes(token, entryDN,
                missAttrNames, profileType);

        if (extAttributes != null && !extAttributes.isEmpty()) {
            // Remove these external attributes from Cache
            Set extAttrNames = extAttributes.keySet();
            cb.removeAttributes(extAttrNames);

            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "getPluginAttrsAndUpdateCache(): External "
                        + "attributes present. Adding them with original"
                        + " list. External Attributes: "
                        + extAttrNames);
            }   

            // Note the attributes stored in the cache are already copied
            // to a new map. Hence modifying this attributes is okay.
            attributes.putAll(extAttributes);
        }
        return attributes;
    }

    /**
     * Gets the specific attributes corresponding to the entryDN. This method
     * obtains the DC Tree node attributes and also performs compliance related
     * verification checks in compliance mode. Note: In compliance mode you can
     * skip the compliance checks by setting ignoreCompliance to "false".
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the DN of the entry whose attributes need to retrieved
     * @param attrNames
     *            a Set of names of the attributes that need to be retrieved.
     *            The attrNames should not be null
     * @param ignoreCompliance
     *            a boolean value specificying if compliance related entries
     *            need to ignored or not. Ignored if true.
     * @return a Map containing attribute names as keys and Set of values
     *         corresponding to each key.
     * @throws AMException
     *             if an error is encountered in fetching the attributes
     */
    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException {
        if (attrNames == null || attrNames.isEmpty()) {
            return getAttributes(token, entryDN, ignoreCompliance, byteValues,
                    profileType);
        }

        // Attributes are being requested; increment cache stats request counter
        cacheStats.incrementRequestCount(getSize());

        // Load the whole attrset in the cache, if in DCTree mode
        // Not good for performance, but fix later TODO (Deepa)
        if (dcTreeServicesImpl.isRequired()) { // TODO: This needs to be fixed!
            getAttributes(token, entryDN, ignoreCompliance, byteValues,
                    profileType);
        }

        String principalDN = MiscUtils.getPrincipalDN(token);
        if (getDebug().messageEnabled()) {
            getDebug().message("In CachedRemoteServicesImpl.getAttributes("
                    + "SSOToken entryDN, attrNames, ignoreCompliance, "
                    + "byteValues) " + "(" + principalDN + ", "
                    + entryDN + ", " + attrNames + ", "
                    + ignoreCompliance + ", " + byteValues
                    + " method.");
        }   

        String dn = MiscUtils.formatToRFC(entryDN);
        CacheBlock cb = (CacheBlock) sdkCache.get(dn);
        if (cb == null) { // Entry not present in cache
            if (getDebug().messageEnabled()) {
                getDebug().message("CachedRemoteServicesImpl."
                        + "getAttributes():  NO entry found in Cache. Getting"
                        + " all these attributes from DS: "
                        + attrNames);
            }   

            // If the attributes returned here have an empty set as value, then
            // such attributes do not have a value or invalid attributes.
            // Internally keep track of these attributes.
            AMHashMap attributes = (AMHashMap) super.getAttributes(token,
                    entryDN, attrNames, ignoreCompliance, byteValues,
                    profileType);

            // These attributes are either not present or not found in DS.
            // Try to check if they need to be fetched by external
            // plugins
            Set missAttrNames = attributes.getMissingAndEmptyKeys(attrNames);
            cb = new CacheBlock(dn, true);
            cb.putAttributes(principalDN, attributes, missAttrNames, false,
                    byteValues);
            sdkCache.put(dn, cb);

            if (!missAttrNames.isEmpty()) {
                attributes = getPluginAttrsAndUpdateCache(token, principalDN,
                        entryDN, cb, attributes, missAttrNames, byteValues,
                        profileType);
            }
            return attributes;
        } else { // Entry present in cache
            validateEntry(token, cb); // Entry may be an invalid entry
            AMHashMap attributes = (AMHashMap) cb.getAttributes(principalDN,
                    attrNames, byteValues);

            // Find the missing attributes that need to be obtained from DS
            // Only find the missing keys as the ones with empty sets are not
            // found in DS
            Set missAttrNames = attributes.getMissingKeys(attrNames);
            if (!missAttrNames.isEmpty()) {
                boolean isComplete = cb.hasCompleteSet(principalDN);
                AMHashMap dsAttributes = null;
                if (!isComplete ||
                // Check for "nsRole" and "nsRoleDN" attributes
                        missAttrNames.contains(NSROLEDN_ATTR)
                        || missAttrNames.contains(NSROLE_ATTR)) {
                    if (getDebug().messageEnabled()) {
                        getDebug().message("CachedRemoteServicesImpl."
                                + "getAttributes(): Trying to get these missing"
                                + " attributes from DS: "
                                + missAttrNames);
                    }

                    dsAttributes = (AMHashMap) super.getAttributes(token,
                            entryDN, missAttrNames, ignoreCompliance,
                            byteValues, profileType);

                    if (dsAttributes != null) {
                        attributes.putAll(dsAttributes);
                        // Add these attributes, may be found in DS or just mark
                        // as invalid (Attribute level Negative caching)
                        Set newMissAttrNames = dsAttributes
                                .getMissingAndEmptyKeys(missAttrNames);

                        // Update dsAttributes with rest of the attributes
                        // in cache
                        dsAttributes.putAll(cb.getAttributes(principalDN,
                                byteValues));

                        // Update the cache
                        cb.putAttributes(principalDN, dsAttributes,
                                newMissAttrNames, isComplete, byteValues);
                        missAttrNames = newMissAttrNames;
                    }
                } else {
                    // Update cache with invalid attributes
                    cb.putAttributes(principalDN, cb.getAttributes(principalDN,
                            byteValues), missAttrNames, isComplete, byteValues);
                }
                if (!missAttrNames.isEmpty()) {
                    attributes = getPluginAttrsAndUpdateCache(token,
                            principalDN, entryDN, cb, attributes,
                            missAttrNames, byteValues, profileType);
                }
            } else { // All attributes found in cache
                if (getDebug().messageEnabled()) {
                    getDebug().message("CachedRemoteServicesImpl."
                            + "getAttributes():  found all attributes in " 
                            + "Cache.");
                }
                cacheStats.updateHitCount(getSize());
            }
            // Remove all the empty values from the return attributes
            return attributes;
        }
    }

    /**
     * Renames an entry. Currently used for only user renaming.
     * 
     * @param token
     *            the sso token
     * @param objectType
     *            the type of entry
     * @param entryDN
     *            the entry DN
     * @param newName
     *            the new name (i.e., if RDN is cn=John, the value passed should
     *            be "John"
     * @param deleteOldName
     *            if true the old name is deleted otherwise it is retained.
     * @return new <code>DN</code> of the renamed entry
     * @throws AMException
     *             if the operation was not successful
     */
    public String renameEntry(SSOToken token, int objectType, String entryDN,
            String newName, boolean deleteOldName) throws AMException {
        String newDN = super.renameEntry(token, objectType, entryDN, newName,
                deleteOldName);
        // Just rename the dn in the cache. Don't remove the entry. So when the
        // event notification happens, it won't find the entry as it is already
        // renamed. Chances are this cache rename operation may happen before
        // the notification thread trys clean up.
        // NOTE: We should have the code to remove the entry for rename
        // operation as the operation could have been performed by some other
        // process such as amadmin.
        String oldDN = MiscUtils.formatToRFC(entryDN);
        CacheBlock cb = (CacheBlock) sdkCache.remove(oldDN);
        newDN = MiscUtils.formatToRFC(newDN);
        sdkCache.put(newDN, cb);
        return newDN;
    }

    /**
     * Method Set the attributes of an entry.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            profile type
     * @param stringAttributes
     *            a AMHashMap of attributes to be set
     * @param byteAttributes
     *            a AMHashMap of attributes to be set
     * @param isAdd
     *            <code>true</code> add to existing value; 
     *            otherwise replace the existing value
     */
    public void setAttributes(SSOToken token, String entryDN, int objectType,
            Map stringAttributes, Map byteAttributes, boolean isAdd)
            throws AMException, SSOException {
        super.setAttributes(token, entryDN, objectType, stringAttributes,
                byteAttributes, isAdd);
        // Cache clean ups
        if (objectType == AMObject.USER) {
            // Update cache locally for modified delted user attributes
            updateCache(token, entryDN, stringAttributes, byteAttributes);
        } else if (objectType != AMObject.USER) {
            // Remove the entry from cache
            dirtyCache(entryDN);
        }
    }

    /**
     * Remove an entry from the directory.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            dn of the profile to be removed
     * @param objectType
     *            profile type
     * @param recursive
     *            if true, remove all sub entries & the object
     * @param softDelete
     *            Used to let pre/post callback plugins know that this delete is
     *            either a soft delete (marked for deletion) or a purge/hard
     *            delete itself, otherwise, remove the object only
     */
    public void removeEntry(SSOToken token, String entryDN, int objectType,
            boolean recursive, boolean softDelete) throws AMException,
            SSOException {
        super.removeEntry(token, entryDN, objectType, recursive, softDelete);
        // write through the cache in case of successful delete
        // (only this entry)
        removeFromCache(entryDN);
    }

    /**
     * Create an AMTemplate (COSTemplate)
     * 
     * @param token
     *            token
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            the object type
     * @param serviceName
     *            Service Name
     * @param attributes
     *            attributes to be set
     * @param priority
     *            template priority
     * @return String DN of the newly created template
     */
    public String createAMTemplate(SSOToken token, String entryDN,
            int objectType, String serviceName, Map attributes, int priority)
            throws AMException {
        String templateDN = super.createAMTemplate(token, entryDN, objectType,
                serviceName, attributes, priority);
        // Mark the entry as exists in cache
        String dn = MiscUtils.formatToRFC(templateDN);
        CacheBlock cb = (CacheBlock) sdkCache.get(dn);
        if (cb != null) {
            cb.setExists(true);
        }
        return templateDN;
    }

    public void setGroupFilter(SSOToken token, String entryDN, String filter)
            throws AMException, SSOException {
        // TODO Auto-generated method stub
        super.setGroupFilter(token, entryDN, filter);
    }
}
