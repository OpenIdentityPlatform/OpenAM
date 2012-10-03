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
 * $Id: CacheBlockBase.java,v 1.6 2009/10/29 00:28:46 hengming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.common;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.AMObject;
import com.sun.identity.shared.debug.Debug;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the value part stored in the AMCacheManager's cache.
 * Each CacheBlock object would represent a Directory entry. It caches the
 * attributes corresponding to that entry. It also keeps track of red other
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
    private String entryDN;

    private int objectType = AMObject.UNDETERMINED_OBJECT_TYPE; // Not known yet

    private AMHashMap stringAttributes; // Stores all String attributes

    private AMHashMap byteAttributes; // Stores all byte attributes

    private long lastModifiedTime = 0;

    // A true value here makes sures that timestamp is added the very first
    // time
    private boolean isExpired = false; // indicates if the entry has expired

    // The Organization DN corresponding to this record
    private String organizationDN = null; // Always pass a RFC lowercase
                                            // string

    // Indicates if this Entry represents a valid DS Entry.
    private boolean isValidEntry = true;

    public abstract Debug getDebug();

    public abstract boolean isEntryExpirationEnabled();

    public abstract long getUserEntryExpirationTime();

    public abstract long getDefaultEntryExpirationTime();

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

    public synchronized void setExists(boolean exists) {
        if (exists) {
            cacheEntries = new AMHashMap();
            stringAttributes = new AMHashMap(false);
            byteAttributes = new AMHashMap(true);
        }
        isValidEntry = exists;
        updateLastModifiedTime();
    }

    private synchronized void setLastModifiedTime() {
        if (isEntryExpirationEnabled()) { // First time setup
            lastModifiedTime = System.currentTimeMillis();
        }
    }

    private synchronized void updateLastModifiedTime() {
        if (isEntryExpirationEnabled() && isExpired) {
            lastModifiedTime = System.currentTimeMillis();
            isExpired = false;
        }
    }

    public synchronized void setObjectType(int type) {
        objectType = type;
        updateLastModifiedTime();
    }

    public synchronized void setOrganizationDN(String orgDN) {
        organizationDN = orgDN;
        updateLastModifiedTime();
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
    public synchronized boolean isExists() {
        // We cannot call expiredAndUpdated() here as it will change the
        // behaviour of the method. Hence it needs to called externally.
        return isValidEntry;
    }

    public synchronized boolean hasExpiredAndUpdated() {
        // We need to have the isExpired variable to make sure
        // the notifications are sent only once.
        if (isEntryExpirationEnabled() && !isExpired) { // Happens only if
                                                        // enabled
            long expirationTime = 0;
            switch (objectType) {
            case AMObject.USER:
                expirationTime = getUserEntryExpirationTime();
                break;
            default:
                expirationTime = getDefaultEntryExpirationTime();
                break;
            }
            long elapsedTime = System.currentTimeMillis() - lastModifiedTime;
            if (elapsedTime >= expirationTime) { // Expired
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
        }
        return isExpired;
    }

    public synchronized boolean hasCache(String principalDN) {
        CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
        return (ce != null && !hasExpiredAndUpdated());
    }

    public synchronized boolean hasCompleteSet(String principalDN) {
        CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
        boolean completeSet = false;
        if (ce != null && !hasExpiredAndUpdated()) {
            completeSet = ce.isCompleteSet();
        }
        return completeSet;
    }

    public synchronized Map getAttributes(String principalDN, 
            boolean byteValues) {
        return (getAttributes(principalDN, null, byteValues));
    }

    public synchronized Map getAttributes(String principalDN, Set attrNames,
            boolean byteValues) {
        Map attributes = new AMHashMap(byteValues);

        // Get the cache entry for the principal
        CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
        if (ce != null && !hasExpiredAndUpdated()) {
            // Get the names of attributes that this principal can access
            Set accessibleAttrs = null;
            if (attrNames == null) {
                accessibleAttrs = ce.getReadableAttrNames();
            } else {
                accessibleAttrs = ce.getReadableAttrNames(attrNames);
            }
            // Get the attribute values from cache
            if (!byteValues) {
                attributes = stringAttributes.getCopy(accessibleAttrs);
                if (ce.isCompleteSet()
                        && !attributes.keySet().containsAll(accessibleAttrs)
                        && !byteAttributes.isEmpty()) {
                    // Since the flag for complete set does not distingusih
                    // between string and binary attributes, check for
                    // missing attributes in byteAttributes
                    for (Iterator items = accessibleAttrs.iterator(); items
                            .hasNext();) {
                        Object key = items.next();
                        if (!attributes.containsKey(key)
                                && byteAttributes.containsKey(key)) {
                            byte[][] values = (byte[][]) byteAttributes
                                    .get(key);
                            Set valueSet = new HashSet(values.length * 2);
                            for (int i = 0; i < values.length; i++) {
                                try {
                                    valueSet.add(new String(values[i], "UTF8"));
                                } catch (UnsupportedEncodingException uee) {
                                    // Use default encoding
                                    valueSet.add(new String(values[i]));
                                }
                            }
                            attributes.put(key, valueSet);
                        }
                    }
                }
            } else {
                attributes = byteAttributes.getCopy(accessibleAttrs);
                if (ce.isCompleteSet()
                        && !attributes.keySet().containsAll(accessibleAttrs)
                        && !stringAttributes.isEmpty()) {
                    // Since the flag for complete set does not distingusih
                    // between string and binary attributes, check for
                    // missing attributes in stringAttributes
                    for (Iterator items = accessibleAttrs.iterator(); items
                            .hasNext();) {
                        Object key = items.next();
                        if (!attributes.containsKey(key)
                                && stringAttributes.containsKey(key)) {
                            Set valueSet = (Set) stringAttributes.get(key);
                            byte[][] values = new byte[valueSet.size()][];
                            int item = 0;
                            for (Iterator vals = valueSet.iterator(); vals
                                    .hasNext();) {
                                String val = (String) vals.next();
                                values[item] = new byte[val.length()];
                                byte[] src = null;
                                try {
                                    src = val.getBytes("UTF8");
                                } catch (UnsupportedEncodingException uee) {
                                    // Use default encoding
                                    src = val.getBytes();
                                }
                                System.arraycopy(src, 0, values[item], 0, val
                                        .length());
                                item++;
                            }
                            attributes.put(key, values);
                        }
                    }
                }
            }

            // Get the names of attributes that are invalid/not accessible
            Set inAccessibleAttrs = ce.getInaccessibleAttrNames(attrNames);
            ((AMHashMap) attributes).addEmptyValues(inAccessibleAttrs);
        }

        return attributes;
    }

    public synchronized void putAttributes(String principalDN, Map attributes,
            Set inAccessibleAttrNames, boolean isCompleteSet, 
            boolean byteValues) {
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
    }

    public synchronized void removeAttributes(String principalDN) {
        CacheEntry ce = (CacheEntry) cacheEntries.remove(principalDN);
        if (ce != null) {
            ce.clear(); // To remove all used references
        }
    }

    public synchronized void removeAttributes(Set attrNames) {
        if ((attrNames != null) && (!attrNames.isEmpty())) {
            stringAttributes.removeKeys(attrNames);
            byteAttributes.removeKeys(attrNames);
            // Remove them from the list of readble attributes of each principal
            Iterator itr = cacheEntries.keySet().iterator();
            while (itr.hasNext()) {
                String principalDN = (String) itr.next();
                removeAttributes(principalDN, attrNames);
            }
        }
    }

    private synchronized void removeAttributes(String principalDN, 
            Set attrNames) {
        CacheEntry ce = (CacheEntry) cacheEntries.get(principalDN);
        if (ce != null) {
            ce.removeAttributeNames(attrNames);
        }
    }

    public synchronized void replaceAttributes(String principalDN,
            Map sAttributes, Map bAttributes) {

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
    public synchronized void clear() {
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

        private Set readableAttrNames;

        // Names of attributes which are either
        // 1 - not readable by the principal corresponding to this entry or
        // 2 - not present in the directory entry.
        // Either way there is not need to explictly distinguish them.
        private Set inAccessibleAttrNames;

        CacheEntry() {
            readableAttrNames = new HashSet();
            inAccessibleAttrNames = new HashSet();
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
                String name = ((String) itr.next()).toLowerCase();
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
                String name = ((String) itr.next()).toLowerCase();
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
                    String name = ((String) it.next()).toLowerCase();
                    readableAttrNames.add(name);
                    inAccessibleAttrNames.remove(name);
                }
            }
            if (invalidAttrs != null && !invalidAttrs.isEmpty()) {
                // inAccessibleAttrNames.addAll(invalidAttrs);
                Iterator it = invalidAttrs.iterator();
                while (it.hasNext()) {
                    String name = ((String) it.next()).toLowerCase();
                    inAccessibleAttrNames.add(name);
                    readableAttrNames.remove(name);
                }
            }
        }

        protected void removeAttributeNames(Set attrNames) {
            completeSet = false;
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String name = ((String) iter.next()).toLowerCase();
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
