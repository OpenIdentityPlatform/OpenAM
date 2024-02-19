/*
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
 * $Id: CacheBlockBase.java,v 1.6 2009/10/29 00:28:46 hengming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.iplanet.am.sdk.common;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.AMObject;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.shared.debug.Debug;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * This class represents the value part stored in the AMCacheManager's cache.
 * Each CacheBlock object would represent a Directory entry. It caches the
 * attributes corresponding to that entry. It also keeps track of the other
 * details such as the Organization DN for the entry.
 *
 * <p>
 * Also, this cache block can be used to serve as dummy block representing a
 * non-existent directory entry (negative caching). This prevents making
 * un-necessary directory calls for non-existent directory entries.
 *
 * <p>
 * Since the attributes that can be retrieved depends on the principal
 * performing the operation (ACI's set), the result set would vary. The
 * attributes that are returned are the ones that are readable by the principal.
 * Each cache block keeps account of these differences in result sets by storing
 * all the attributes readable (and writable) on a per principal basis. This
 * information is stored in a PrincipalAccess object. In order to avoid
 * duplicate copy of the values, the all attribute values are not cached per
 * principal. A single copy of the attributes is stored in the CacheBlock
 * object. Also this copy of attributes stored in the cache block keeps track of
 * non-existent directory attributes (invalid attributes). This would also
 * prevent un-necessary directory calls for non-existent entry attributes.
 *
 * The attribute copy is dirtied by removing the entries which get modified.
 */
public abstract class CacheBlockBase {

    // Maintains a Cache of cacheEntries.
    private AMHashMap cacheEntries;

    // CacheBock representation entry DN
    private final String entryDN;

    private volatile int objectType = AMObject.UNDETERMINED_OBJECT_TYPE; // Not known yet

    private AMHashMap stringAttributes; // Stores all String attributes

    private AMHashMap byteAttributes; // Stores all byte attributes

    private volatile long lastModifiedTime = 0;

    // A true value here makes sures that timestamp is added the very first
    // time
    private volatile boolean isExpired = false; // indicates if the entry has expired

    // The Organization DN corresponding to this record
    private volatile String organizationDN = null; // Always pass a RFC lowercase
                                            // string

    // Indicates if this Entry represents a valid DS Entry.
    private volatile boolean isValidEntry = true;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReadLock readLock = lock.readLock();
    private final WriteLock writeLock = lock.writeLock();

    public CacheBlockBase(String entryDN, boolean validEntry) {
        if (validEntry) {
            cacheEntries = new AMHashMap();
            stringAttributes = new AMHashMap(false);
            byteAttributes = new AMHashMap(true);
        } else {
            // Do not intialize CacheEntries, as it represents a invalid entry
            isValidEntry = false;
        }
        setLastModifiedTime();
        this.entryDN = entryDN;
    }

    public CacheBlockBase(String entryDN, String orgDN, boolean validEntry) {
        this(entryDN, validEntry);
        this.organizationDN = orgDN;
    }

    public abstract boolean isEntryExpirationEnabled();

    public abstract long getUserEntryExpirationTime();

    public abstract long getDefaultEntryExpirationTime();

    public abstract Debug getDebug();

    public void setExists(boolean exists) {
        writeLock.lock();
        try {
            if (exists) {
                cacheEntries = new AMHashMap();
                stringAttributes = new AMHashMap(false);
                byteAttributes = new AMHashMap(true);
            }
            isValidEntry = exists;
            updateLastModifiedTime();
        } finally {
            writeLock.unlock();
        }
    }

    private void setLastModifiedTime() {
        if (isEntryExpirationEnabled()) { // First time setup
            lastModifiedTime = currentTimeMillis();
        }
    }

    private void updateLastModifiedTime() {
        if (isEntryExpirationEnabled() && isExpired) {
            lastModifiedTime = currentTimeMillis();
            isExpired = false;
        }
    }

    public void setObjectType(int type) {
        writeLock.lock();
        try {
            objectType = type;
            updateLastModifiedTime();
        } finally {
            writeLock.unlock();
        }
    }

    public void setOrganizationDN(String orgDN) {
        writeLock.lock();
        try {
            organizationDN = orgDN;
            updateLastModifiedTime();
        } finally {
            writeLock.unlock();
        }
    }

    public String getOrganizationDN() {
        // Call this to expire the value if needed. If expired, a null value
        // will be returned. It is okay as the orgDN will be fetched again.
        hasExpiredAndUpdated();
        return organizationDN;
    }

    public int getObjectType() {
        return objectType;
    }

    public String getEntryDN() {
        // No need to call expiredAndUpdated here. Because this does not change
        // This block is corresponding to key entryDN in the map. So, it is okay
        // to leave this unchanged.
        return entryDN;
    }

    /**
     * Method which specifies if the entry exists in the directory or is an
     * invalid entry. <br>
     * <b>NOTE:</b> Call to this method should preceeded with
     * expiredAndUpdated() check. For example, isExists() return value holds
     * good only it the entry has not expired. So, check anywhere it is called
     * the check should be if (!cb.expiredAndUpdated() && cb.isExists()) or
     * similar.
     *
     * @return true if it represents a valid entry, false otherwise
     */
    public boolean isExists() {
        // We cannot call expiredAndUpdated() here as it will change the
        // behaviour of the method. Hence it needs to called externally.
        return isValidEntry;
    }

    /**
     * <p>If cache expiry has been enabled then this will return true if the cache has expired.</p>
     * <b>Note:</b> This call must be made outside of a readLock because if the cache has expired then a writeLock
     * is required as part of calling {@link CacheBlockBase#clear()} which would result in a deadlock, a readLock
     * cannot be upgraded to a writeLock.
     * @return true if cache expiry is enabled and the cache has expired
     */
    public boolean hasExpiredAndUpdated() {
        readLock.lock();
        try {
            // We need to have the isExpired variable to make sure
            // the notifications are sent only once.
            if (isEntryExpirationEnabled() && !isExpired) { // Happens only if enabled
                long expirationTime = 0;
                switch (objectType) {
                    case AMObject.USER:
                        expirationTime = getUserEntryExpirationTime();
                        break;
                    default:
                        expirationTime = getDefaultEntryExpirationTime();
                        break;
                }
                long elapsedTime = currentTimeMillis() - lastModifiedTime;
                if (elapsedTime >= expirationTime) { // Expired
                    readLock.unlock();
                    writeLock.lock();
                    try {
                        elapsedTime = currentTimeMillis() - lastModifiedTime;
                        if (!isExpired && elapsedTime >= expirationTime) { // Expired
                            // Send notifications first to the SDK listeners
                            isExpired = true;
                            clear();
                            if (getDebug().messageEnabled()) {
                                getDebug().message(
                                        "CacheBlock.hasExpiredAndUpdated(): "
                                                + "Entry with DN " + entryDN + " expired.");
                            }
                            // FIXME: AMObjectImpl.sendExpiryEvent(entryDN, sourceType);
                            // TODO: Add object notification mechanism
                        }
                        readLock.lock();
                    } finally {
                        writeLock.unlock();
                    }
                }
            }
            return isExpired;
        } finally {
            readLock.unlock();
        }
    }

    public boolean hasCache(String principalDN) {
        boolean hasExpired = hasExpiredAndUpdated();
        readLock.lock();
        try {
            CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
            return (ce != null && !hasExpired);
        } finally {
            readLock.unlock();
        }
    }

    public boolean hasCompleteSet(String principalDN) {
        boolean hasExpired = hasExpiredAndUpdated();
        readLock.lock();
        try {
            CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
            if (ce != null && !hasExpired) {
                return ce.isCompleteSet();
            } else {
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    public Map getAttributes(String principalDN, boolean byteValues) {
        return getAttributes(principalDN, null, byteValues);
    }

    public Map getAttributes(String principalDN, Set attrNames, boolean byteValues) {
        boolean hasExpired = hasExpiredAndUpdated();
        Map attributes = new AMHashMap(byteValues);
        readLock.lock();
        try {
            // Get the cache entry for the principal
            CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
            if (ce != null && !hasExpired) {
                // Get the names of attributes that this principal can access
                Set accessibleAttrs = null;
                if (attrNames == null) {
                    accessibleAttrs = ce.getReadableAttrNames();
                } else {
                    accessibleAttrs = ce.getReadableAttrNames(attrNames);
                }
                if (!accessibleAttrs.isEmpty()) {
                    // Get the attribute values from the appropriate string or binary cache if they exist.
                    if (!byteValues) {
                        attributes = stringAttributes.getCopy(accessibleAttrs);
                        if (attributes.isEmpty()) {
                            if (getDebug().messageEnabled()) {
                                getDebug().message("CacheBlockBase.getAttributes(): accessibleAttrs:" + accessibleAttrs
                                        + " not found in stringAttributes, attributes in the byteAttributes cache:"
                                        + byteAttributes.keySet());
                            }
                        }
                    } else {
                        attributes = byteAttributes.getCopy(accessibleAttrs);
                        if (attributes.isEmpty()) {
                            if (getDebug().messageEnabled()) {
                                getDebug().message("CacheBlockBase.getAttributes(): accessibleAttrs:" + accessibleAttrs
                                        + " not found in byteAttributes, attributes in the stringAttributes cache:"
                                        + stringAttributes.keySet());
                            }
                        }
                    }
                }
                // Get the names of attributes that are invalid/not accessible
                Set inAccessibleAttrs = ce.getInaccessibleAttrNames(attrNames);
                ((AMHashMap) attributes).addEmptyValues(inAccessibleAttrs);
            }
        } finally {
            readLock.unlock();
        }
        return attributes;
    }

    public void putAttributes(String principalDN, Map attributes,
            Set inAccessibleAttrNames, boolean isCompleteSet, boolean byteValues) {
        writeLock.lock();
        try {
            CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
            if (ce == null) {
                ce = new CacheEntry();
                cacheEntries.put(principalDN, ce);
            }

            // Copy only the attributes in the common place. Store the invalid/
            // unreadable attrs per principal.
            if (!byteValues) {
                Set attrsWithValues = stringAttributes.copyValuesOnly(attributes);
                ce.putAttributes(attrsWithValues, inAccessibleAttrNames,
                        isCompleteSet);
            } else {
                Set attrsWithValues = byteAttributes.copyValuesOnly(attributes);
                ce.putAttributes(attrsWithValues, inAccessibleAttrNames,
                        isCompleteSet);
            }

            updateLastModifiedTime();
        } finally {
            writeLock.unlock();
        }
    }

    public void removeAttributes(String principalDN) {
        writeLock.lock();
        try {
            CacheEntry ce = (CacheEntry) cacheEntries.remove(principalDN);
            if (ce != null) {
                ce.clear(); // To remove all used references
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void removeAttributes(Set attrNames) {
        if ((attrNames != null) && (!attrNames.isEmpty())) {
            writeLock.lock();
            try {
                stringAttributes.removeKeys(attrNames);
                byteAttributes.removeKeys(attrNames);
                // Remove them from the list of readable attributes of each principal
                Iterator itr = cacheEntries.keySet().iterator();
                while (itr.hasNext()) {
                    String principalDN = (String) itr.next();
                    removeAttributes(principalDN, attrNames);
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    private void removeAttributes(String principalDN, Set attrNames) {
        CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
        if (ce != null) {
            ce.removeAttributeNames(attrNames);
        }
    }

    public void replaceAttributes(String principalDN, Map sAttributes, Map bAttributes) {
        if (sAttributes != null && !sAttributes.isEmpty()) {
            putAttributes(principalDN, sAttributes, null, false, false);
        } else if (bAttributes != null && !bAttributes.isEmpty()) {
            putAttributes(principalDN, bAttributes, null, false, true);
        }
    }

    /**
     * Should be cleared, only if the entry is still valid only the data has
     * changed. If entry has been deleted then should be removed.
     */
    public void clear() {
        writeLock.lock();
        try {
            if (isValidEntry) { // Clear only if it is a valid entry
                // If entry is not valid then all these maps will be null
                stringAttributes.clear();
                byteAttributes.clear();
                cacheEntries.clear();
            }
            // Don't have to change isValidEntry as it would have been updated if
            // the entry was deleted. Also do not change the object type here
            // it is need. Ideally the object type won't change for a DN
            lastModifiedTime = 0; // => expired
            organizationDN = null; // Could have been renamed
        } finally {
            writeLock.unlock();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n----------- START CACHE BLOCK -----------");
        sb.append("\nEntry DN: ").append(entryDN);
        sb.append(" Valid Entry: ").append(isValidEntry);
        sb.append("\nOrganization: ").append(organizationDN);
        sb.append("\nString Attributes: ");
        if ((stringAttributes != null) && (!stringAttributes.isEmpty())) {
            sb.append(MiscUtils.mapSetToString(stringAttributes));
        }        
        sb.append("\nByte Attributes: ");
        sb.append(MiscUtils.mapSetToString(byteAttributes));
        sb.append("\nByte Negative Attributes: ");
        if ((byteAttributes != null) && (!byteAttributes.isEmpty())) {
            sb.append(byteAttributes.getNegativeByteAttrClone().toString());
        }        
        sb.append("\nCache Entries: ");

        if (cacheEntries != null && !cacheEntries.isEmpty()) {
            Iterator itr = cacheEntries.keySet().iterator();
            while (itr.hasNext()) {
                String principal = (String) itr.next();
                CacheEntry ce = (CacheEntry) cacheEntries.get(principal);
                sb.append("\nPrincipal: ").append(principal);
                sb.append(ce.toString());
            }
        } else {
            sb.append("<empty>");
        }
        sb.append("\n----------- END CACHE BLOCK -----------");
        return sb.toString();
    }

    class CacheEntry {
        private boolean completeSet = false;

        private final Set readableAttrNames;

        // Names of attributes which are either
        // 1 - not readable by the principal corresponding to this entry or
        // 2 - not present in the directory entry.
        // Either way there is not need to explictly distinguish them.
        private final Set inAccessibleAttrNames;

        CacheEntry() {
            readableAttrNames = new CaseInsensitiveHashSet();
            inAccessibleAttrNames = new CaseInsensitiveHashSet();
        }

        /**
         * Method to get all the stored attributes
         * 
         * @return a new copy of the map of all attributes
         */
        protected Set getReadableAttrNames() {
            return (readableAttrNames);
        }

        protected Set getReadableAttrNames(Set attrNames) {
            // Return the intersection of attribute names present in stored set
            Set attributesPresent = new HashSet();
            Iterator itr = attrNames.iterator();
            while (itr.hasNext()) {
                String name = (String) itr.next();
                if (readableAttrNames.contains(name)) {
                    attributesPresent.add(name);
                }
            }
            return attributesPresent;
        }

        protected Set getInaccessibleAttrNames(Set attrNames) {
            // Return the intersection of attribute names present in stored set
            if (attrNames == null || attrNames.isEmpty()) {
                // Return empty set
                return Collections.EMPTY_SET;
            }
            Set attributesPresent = new HashSet();
            // Iterator itr = inAccessibleAttrNames.iterator();
            Iterator itr = attrNames.iterator();
            while (itr.hasNext()) {
                String name = (String) itr.next();
                if (inAccessibleAttrNames.contains(name)) {
                    attributesPresent.add(name);
                }
            }
            return attributesPresent;
        }

        protected void putAttributes(Set attrNames, Set invalidAttrs,
                boolean isCompleteSet) {
            completeSet = isCompleteSet;

            if (attrNames != null && !attrNames.isEmpty()) {
                // readableAttrNames.addAll(attrNames);
                Iterator it = attrNames.iterator();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    readableAttrNames.add(name);
                    inAccessibleAttrNames.remove(name);
                }
            }
            if (invalidAttrs != null && !invalidAttrs.isEmpty()) {
                // inAccessibleAttrNames.addAll(invalidAttrs);
                Iterator it = invalidAttrs.iterator();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    inAccessibleAttrNames.add(name);
                    readableAttrNames.remove(name);
                }
            }
        }

        protected void removeAttributeNames(Set attrNames) {
            completeSet = false;
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                boolean removed = readableAttrNames.remove(name);
                if (!removed) { // May be in accessible attr. Try removing it.
                    inAccessibleAttrNames.remove(name);
                }
            }
        }

        protected boolean isCompleteSet() {
            return completeSet;
        }

        protected void clear() {
            completeSet = false;
            readableAttrNames.clear();
            inAccessibleAttrNames.clear();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(" Complete Set: ").append(completeSet);
            sb.append(" Attributes: ").append(readableAttrNames);
            sb.append(" In Accessable attributes: ");
            sb.append(inAccessibleAttrNames);

            return sb.toString();
        }
    }
}
