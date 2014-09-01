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
 * $Id: IdCachedServicesImpl.java,v 1.21 2009/08/25 06:50:53 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.idm.server;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdCachedServices;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdServices;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.common.IdCacheBlock;
import com.sun.identity.idm.common.IdCacheStats;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.SsoServerIdRepoSvcImpl;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.sm.ServiceManager;

import com.iplanet.am.sdk.AMEvent;
import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.util.Cache;
import com.sun.identity.monitoring.MonitoringUtil;

/*
 * Class which provides caching on top of available IdRepoLDAPServices.
 */
public class IdCachedServicesImpl extends IdServicesImpl implements
        IdCachedServices {

    static final String CACHE_MAX_SIZE_KEY = "com.iplanet.am.sdk.cache.maxSize";
    
    static final String CACHE_MAX_SIZE = "10000";
    
    static final int CACHE_MAX_SIZE_INT = 10000;
    
    private static int maxSize;

    private static IdServices instance;

    // Class Private
    private Cache idRepoCache;

    private IdCacheStats cacheStats;

    private static Stats stats;

    private static SsoServerIdRepoSvcImpl monIdRepo;

    static {
        initializeParams();
    }

    /**
     * Method to check if caching is enabled or disabled and configure the size
     * of the cache accordingly.
     */
    private static void initializeParams() {
        // Check if the caching property is set in System runtime.
        String cacheSize = SystemProperties.get(CACHE_MAX_SIZE_KEY,
            CACHE_MAX_SIZE);
        try {
            maxSize = Integer.parseInt(cacheSize);
            if (maxSize < 1) {
                maxSize = CACHE_MAX_SIZE_INT;
            }
            if (getDebug().messageEnabled()) {
                getDebug().message(
                        "IdCachedServicesImpl.intializeParams() "
                                + "Caching size set to: " + maxSize);
            }
        } catch (NumberFormatException ne) {
            maxSize = CACHE_MAX_SIZE_INT;
            getDebug().warning("IdCachedServicesImpl.initializeParams() "
                    + "- invalid value for cache size specified. Setting "
                    + "to default value: " + maxSize);
        }
    }

    private IdCachedServicesImpl() {
        super();
        initializeCache();
        stats = Stats.getInstance(getClass().getName());
        cacheStats = new IdCacheStats(IdConstants.IDREPO_CACHESTAT);
        stats.addStatsListener(cacheStats);
        if (MonitoringUtil.isRunning()) {
            monIdRepo = Agent.getIdrepoSvcMBean();
        }
    }

    private void initializeCache() {
        idRepoCache = new Cache(maxSize);
    }

    /**
     * Method to get the current cache size
     * 
     * @return the size of the SDK LRU cache
     */
    public int getSize() {
        return idRepoCache.size();
    }

    protected static synchronized IdServices getInstance() {
        if (instance == null) {
            getDebug().message("IdCachedServicesImpl.getInstance(): "
                    + "Creating new Instance of IdCachedServicesImpl()");
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            //the instance should be created before acquiring the lock to
            //prevent possible deadlocks by using Stats
            instance = new IdCachedServicesImpl();
            if (shutdownMan.acquireValidLock()) {
                try {
                    shutdownMan.addShutdownListener(
                        new ShutdownListener() {
                            public void shutdown() {
                               synchronized (instance) {
                                   shutdownCalled = true;
                               }
                                instance.clearIdRepoPlugins();
                            }
                        });                                                                             
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n<<<<<<< BEGIN SDK CACHE CONTENTS >>>>>>>>");
        if (!idRepoCache.isEmpty()) { // Should never be null
            Enumeration cacheKeys = idRepoCache.keys();
            while (cacheKeys.hasMoreElements()) {
                String key = (String) cacheKeys.nextElement();
                IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(key);
                sb.append("\nSDK Cache Block: ").append(key);
                sb.append(cb.toString());
            }
        } else {
            sb.append("<empty>");
        }
        sb.append("\n<<<<<<< END SDK CACHE CONTENTS >>>>>>>>");
        return sb.toString();
    }

    /*************************************************************************/
    // Update/Dirty methods of this class.
    // *************************************************************************
    private void removeCachedAttributes(String affectDNs, Set attrNames) {
        Enumeration cacheKeys = idRepoCache.keys();
        while (cacheKeys.hasMoreElements()) {
            String key = DNUtils.normalizeDN(
                cacheKeys.nextElement().toString());
            int l1 = key.length();
            int l2 = affectDNs.length();
            if (key.regionMatches(true, (l1 - l2), affectDNs, 0, l2)) {
                // key ends with 'affectDN' string
                IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(key);
                if (cb != null) {
                    // key ends with 'affectDN' string
                    if ((attrNames != null) &&
                        !cb.hasExpiredAndUpdated() && cb.isExists()) {
                        cb.removeAttributes(attrNames);
                    } else {
                        cb.clear();
                    }
                }
            }
        }
    }

    private void clearCachedEntries(String affectDNs) {
        removeCachedAttributes(affectDNs, null);
    }
    
    /**
     * This method is used to clear the entire SDK cache in the event that
     * EventService notifies that all entries have been modified (or should be
     * marked dirty).
     */
    public synchronized void clearCache() {
        idRepoCache.clear();
        initializeCache();
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
        IdCacheBlock cb;
        String originalDN = dn;
        dn = DNUtils.normalizeDN(dn);
        String cachedID = getCacheId(dn);
        switch (eventType) {
        case AMEvent.OBJECT_ADDED:
            cb = getFromCache(dn);
            if (cb != null) { // Mark an invalid entry as valid now
                cb.setExists(true);
            }
            if (cosType) { // A cos type event remove all affected attributes
                removeCachedAttributes(cachedID, attrNames);
            }
            break;
        case AMEvent.OBJECT_REMOVED:
            cb = (IdCacheBlock) idRepoCache.remove(cachedID);
            if (cb != null) {
                cb.clear(); // Clear anyway & help the GC process
            }
            if (cosType) {
                removeCachedAttributes(cachedID, attrNames);
            }
            break;
        case AMEvent.OBJECT_RENAMED:
            // Better to remove the renamed entry, or else it will be just
            // hanging in the cache, until LRU kicks in.
            cb = (IdCacheBlock) idRepoCache.remove(cachedID);
            if (cb != null) {
                cb.clear(); // Clear anyway & help the GC process
            }
            if (cosType) {
                removeCachedAttributes(cachedID, attrNames);
            }
            break;
        case AMEvent.OBJECT_CHANGED:
            cb = getFromCache(dn);
            if (cb != null) {
                cb.clear(); // Just clear the entry. Don't remove.
            }
            if (cosType) {
                removeCachedAttributes(cachedID, attrNames);
            } else if (aciChange) { // Clear all affected entries
                clearCachedEntries(cachedID);
            }
            break;
        }
        if (getDebug().messageEnabled()) {
            getDebug().message("IdCachedServicesImpl.dirtyCache(): Cache "
                    + "dirtied because of Event Notification. Parameters - "
                    + "eventType: " + eventType + ", cosType: "
                    + cosType + ", aciChange: " + aciChange
                    + ", fullDN: " + originalDN + "; rfcDN ="
                    + dn + "; cachedID=" + cachedID);
        }
    }

    /**
     * Method that updates the cache entries locally. This method does a write
     * through cache
     */
    private void updateCache(SSOToken token, String dn, Map stringAttributes,
        Map byteAttributes) throws IdRepoException, SSOException {
        String key = dn; // This is already normalized
        IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(key);
        if (cb != null && !cb.hasExpiredAndUpdated() && cb.isExists()) {
            AMIdentity tokenId = IdUtils.getIdentity(token);
            String pDN = tokenId.getUniversalId();
            cb.replaceAttributes(pDN, stringAttributes, byteAttributes);
        }
    }

    private void dirtyCache(String dn) {
        String key = DNUtils.normalizeDN(dn);
        IdCacheBlock cb = getFromCache(key);
        if (cb != null) {
            cb.clear();
        }
    }

    public Map getAttributes(SSOToken token, IdType type, String name,
        Set attrNames, String amOrgName, String amsdkDN,
        boolean isStringValues) throws IdRepoException, SSOException {
        
        // If required attributes is null or empty, call the
        // other interface to get all the attributes
        // TODO: Need to provide means to get all the binary attributes too!
        // Currently not needed as AMIdentity does not have getAllBinaryAttr..
        
        if ((attrNames == null) || attrNames.isEmpty()) {
            return getAttributes(token, type, name, amOrgName, amsdkDN);
        }
        cacheStats.incrementGetRequestCount(getSize());
        if (MonitoringUtil.isRunning() &&
            ((monIdRepo = Agent.getIdrepoSvcMBean()) !=
	    null))
	{
            long li = (long)getSize();
            monIdRepo.incGetRqts(li);
        }
        
        // Get the entry DN
        AMIdentity id = new AMIdentity(token, name, type, amOrgName, amsdkDN);
        String dn = id.getUniversalId().toLowerCase();

        // Get the principal DN
        AMIdentity tokenId = IdUtils.getIdentity(token);
        String principalDN = IdUtils.getUniversalId(tokenId);

        // Debug messages
        if (getDebug().messageEnabled()) {
            getDebug().message("In IdCachedServicesImpl." +
                    "getAttributes(SSOToken, type, name, attrNames, " +
                    "amOrgName, amsdkDN) (" + principalDN + ", " + dn +
                    ", " + attrNames + " ," + amOrgName +
                    " , " + amsdkDN + " method.");
        }
        
        // Attributes to be returned
        AMHashMap attributes;
        
        // Check in the cache
        IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(dn);
        if (cb == null) { // Entry not present in cache
            if (getDebug().messageEnabled()) {
                getDebug().message("IdCachedServicesImpl.getAttributes(): "
                        + "NO entry found in Cachefor key = " + dn
                        + ". Getting all these attributes from DS: "
                        + attrNames);
            }   

            // If the attributes returned here have an empty set as value, then
            // such attributes do not have a value or invalid attributes.
            // Internally keep track of these attributes.
            attributes = (AMHashMap) super.getAttributes(token, type, name,
                    attrNames, amOrgName, amsdkDN, isStringValues);

            // Find the missing attributes and add to cache
            Set missAttrNames = attributes.getMissingAndEmptyKeys(attrNames);
            cb = new IdCacheBlock(dn, true);
            cb.putAttributes(principalDN, attributes, missAttrNames, false,
                    !isStringValues);
            idRepoCache.put(dn, cb);
        } else { // Entry present in cache
            attributes = (AMHashMap) cb.getAttributes(principalDN, attrNames,
                    !isStringValues);

            // Find the missing attributes that need to be obtained from DS
            // Only find the missing keys as the ones with empty sets are not
            // found in DS
            Set missAttrNames = attributes.getMissingKeys(attrNames);
            if (!missAttrNames.isEmpty()) {
                if (getDebug().messageEnabled()) {
                    getDebug().message("IdCachedServicesImpl."
                            + "getAttributes(): Trying to gett these missing "
                            + "attributes from DS: "
                            + missAttrNames);
                }
                AMHashMap dsAttributes = (AMHashMap) super.getAttributes(token,
                        type, name, attrNames, amOrgName, amsdkDN,
                        isStringValues);
                attributes.putAll(dsAttributes);
                
                // Add these attributes, may be found in DS or just mark them
                // as invalid (Attribute level Negative caching)
                Set newMissAttrNames = dsAttributes
                        .getMissingAndEmptyKeys(missAttrNames);
                cb.putAttributes(principalDN, dsAttributes, newMissAttrNames,
                        false, !isStringValues);
            } else { // All attributes found in cache
                cacheStats.updateGetHitCount(getSize());
                if (MonitoringUtil.isRunning() &&
                    ((monIdRepo =
		     Agent.getIdrepoSvcMBean()) !=
	             null))
	        {
                    long li = (long)getSize();
                    monIdRepo.incCacheHits(li);
                }
                if (getDebug().messageEnabled()) {
                    getDebug().message("IdCachedServicesImpl" 
                            + ".getAttributes(): " + amsdkDN
                            + " found all attributes in Cache.");
                }
            }
        }
        return attributes;
    }

    public Map getAttributes(SSOToken token, IdType type, String name,
        String amOrgName, String amsdkDN)
        throws IdRepoException, SSOException {
        
        cacheStats.incrementGetRequestCount(getSize());
        if (MonitoringUtil.isRunning() &&
            ((monIdRepo = Agent.getIdrepoSvcMBean()) !=
	    null))
	{
            long li = (long)getSize();
            monIdRepo.incGetRqts(li);
        }
        // Get the identity dn
        AMIdentity id = new AMIdentity(token, name, type, amOrgName, amsdkDN);
        String dn = id.getUniversalId().toLowerCase();

        // Get the principal dn
        AMIdentity tokenId = IdUtils.getIdentity(token);
        String principalDN = IdUtils.getUniversalId(tokenId);

        // Get the cache entry
        IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(dn);
        AMHashMap attributes;
        if ((cb != null) && cb.hasCompleteSet(principalDN)) {
            cacheStats.updateGetHitCount(getSize());
            if (MonitoringUtil.isRunning() &&
                ((monIdRepo =
		 Agent.getIdrepoSvcMBean()) !=
	         null))
	    {
                long li = (long)getSize();
                monIdRepo.incCacheHits(li);
            }
            if (getDebug().messageEnabled()) {
                getDebug().message("IdCachedServicesImpl." 
                    + "getAttributes(): DN: " + dn 
                    + " found all attributes in Cache.");
            }
            attributes = (AMHashMap) cb.getAttributes(principalDN, false);
        } else {
            // Get all the attributes from data store
            if (getDebug().messageEnabled()) {
                getDebug().message("IdCachedServicesImpl."
                    + "getAttributes(): " + dn
                    + " complete attribute"
                    + " set NOT found in cache. Getting from DS.");
            }
            attributes = (AMHashMap) super.getAttributes(token, type, name,
                amOrgName, amsdkDN);
            if (cb == null) {
                cb = new IdCacheBlock(dn, true);
                idRepoCache.put(dn, cb);
            }
            cb.putAttributes(principalDN, attributes, null, true, false);
            if (getDebug().messageEnabled()) {
                getDebug().message("IdCachedServicesImpl.getAttributes(): "
                        + "attributes NOT found in cache. Fetched from DS.");
            }
        }
        return attributes;
    }

    public void setActiveStatus(SSOToken token, IdType type, String name,
        String amOrgName, String amsdkDN, boolean active) throws SSOException,
        IdRepoException {
        super.setActiveStatus(token, type, name, amOrgName, amsdkDN, active);
        AMIdentity id = new AMIdentity(token, name, type, amOrgName, amsdkDN);
        String dn = IdUtils.getUniversalId(id).toLowerCase();
        dirtyCache(dn);
    }

    public void setAttributes(SSOToken token, IdType type, String name,
        Map attributes, boolean isAdd, String amOrgName, String amsdkDN,
        boolean isString) throws IdRepoException, SSOException {

        // Update attributes in data store
        super.setAttributes(token, type, name, attributes, isAdd, amOrgName,
                amsdkDN, isString);
        
        // Get identity DN
        AMIdentity id = new AMIdentity(token, name, type, amOrgName, amsdkDN);
        String dn = IdUtils.getUniversalId(id).toLowerCase();

        // Update the cache
        if (type.equals(IdType.USER)) {
            // Update cache locally for modified delted user attributes
            if (isString) {
                updateCache(token, dn, attributes, null);
            } else {
                updateCache(token, dn, null, attributes);
            }
        } else {
            dirtyCache(dn);
        }
    }

    public void delete(SSOToken token, IdType type, String name,
        String orgName, String amsdkDN) throws IdRepoException,
        SSOException {
        // Call parent to delete the entry
        super.delete(token, type, name, orgName, amsdkDN);

        // Clear the cache, get identity DN
        AMIdentity id = new AMIdentity(token, name, type, orgName, amsdkDN);
        String dn = id.getUniversalId().toLowerCase();
        idRepoCache.remove(dn);
    }

    public void removeAttributes(SSOToken token, IdType type, String name,
        Set attrNames, String orgName, String amsdkDN)
        throws IdRepoException, SSOException {
        // Call parent to remove the attributes
        super.removeAttributes(token, type, name, attrNames, orgName, amsdkDN);

        // Update the cache
        AMIdentity id = new AMIdentity(token, name, type, orgName, amsdkDN);
        String dn = id.getUniversalId().toLowerCase();
        IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(dn);
        if ((cb != null) && !cb.hasExpiredAndUpdated() && cb.isExists()) {
            // Remove the attributes
            cb.removeAttributes(attrNames);
        }
    }

    public IdSearchResults search(SSOToken token, IdType type, String pattern,
        IdSearchControl ctrl, String orgName)
        throws IdRepoException, SSOException {
        IdSearchResults answer = new IdSearchResults(type, orgName);
        cacheStats.incrementSearchRequestCount(getSize());
        if (MonitoringUtil.isRunning() &&
            ((monIdRepo = Agent.getIdrepoSvcMBean()) !=
	    null))
	{
            long li = (long)getSize();
            monIdRepo.incSearchRqts(li);
        }
        // in legacy mode we must do search in order
        // to get the AMSDKDN component added to AMIdentity's uvid.
        // otherwise unix and anonymous login will fail.
        if ((pattern.indexOf('*') == -1) &&
            ServiceManager.isRealmEnabled()) {
            // First check if the specific identity is in cache.
            // If yes, get Attributes from cache.
            // If not search in server.
            AMIdentity uvid = new AMIdentity(token, pattern, type,
                orgName, null);
            String universalID = uvid.getUniversalId().toLowerCase();
            IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(universalID);
            if ((cb != null) && !cb.hasExpiredAndUpdated() &&
                cb.isExists() &&
                (ctrl.getSearchModifierMap() == null)) {

                // Check if search is for a specific identity
                // Search is for a specific user, look in the cache
                Map attributes;
                try {
                    cacheStats.updateSearchHitCount(getSize());
                    if (MonitoringUtil.isRunning() &&
                        ((monIdRepo =
			 Agent.getIdrepoSvcMBean()) !=
	                 null))
	            {
                        long li = (long)getSize();
                        monIdRepo.incSearchCacheHits(li);
                    }
                    if (ctrl.isGetAllReturnAttributesEnabled()) {
                        attributes = getAttributes(token, type, pattern,
                            orgName, null);
                    } else {
                        Set attrNames = ctrl.getReturnAttributes();
                        attributes = getAttributes(token, type, pattern,
                            attrNames, orgName, null, true);
                    }
                    // Construct IdSearchResults
                    AMIdentity id = new AMIdentity(token, pattern,
                        type, orgName, null);
                    answer.addResult(id, attributes);
                } catch (IdRepoException ide) {
                    // Check if the exception is name not found
                    if (!ide.getErrorCode().equals("220")) {
                        // Throw the exception
                        throw (ide);
                    }
                }
            } else {
                // Not in Cache.
                // Do a search is server.
                answer = super.search(token, type, pattern, ctrl, orgName);
            }
        } else {
            // Pattern contains "*", need a search in server.
            answer = super.search(token, type, pattern, ctrl, orgName);
        }
        return (answer);
    }
    
    // Returns fully qualified names for the identity
    public Set getFullyQualifiedNames(SSOToken token,
        IdType type, String name, String orgName)
        throws IdRepoException, SSOException {

        // Get the identity DN
        AMIdentity id = new AMIdentity(token, name, type, orgName, null);
        String dn = id.getUniversalId().toLowerCase();

        // Get the cache entry
        Set answer = null;
        IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(dn);
        if (cb != null) {
            // Get the fully qualified names
            answer = cb.getFullyQualifiedNames();
        }
        if (answer == null) {
            // Obtain from the data stores
            answer = super.getFullyQualifiedNames(
                token, type, name, orgName);
            if (cb != null) {
                cb.setFullyQualifiedNames(answer);
            }
        }
        return (answer);
    }

    // Return cache block for the universal identifier
    private IdCacheBlock getFromCache(String dn) {
        IdCacheBlock cb = (IdCacheBlock) idRepoCache.get(dn);
        if (cb == null) {
            int ind = dn.toLowerCase().indexOf(",amsdkdn=");
            if (ind > -1) {
                String tmp = dn.substring(0, ind);
                // TODO: Should return entries which might have amsdkDN but
                // notifications have not told us about it (like
                // notifications from plugins other than AMSDKRepo
                cb = (IdCacheBlock) idRepoCache.get(tmp);
            }
        }
        return cb;
    }

    // strip away amsdkdn from dn.
    private String getCacheId(String dn) {
        String cachedId = dn;
        int ind = dn.toLowerCase().indexOf(",amsdkdn=");
        if (ind > -1) {
             cachedId = dn.substring(0, ind);
        }
        return cachedId;
    }

}
