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
 * $Id: CachedSubEntries.java,v 1.10 2008/07/11 01:46:21 arviranga Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.am.util.Cache;
import com.sun.identity.common.DNUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CachedSubEntries {
    // Cache of CachedSubEntries based on lowercased DN to obtain sub entries
    protected static Map smsEntries = Collections.synchronizedMap(
        new HashMap(100));

    // Instance variables
    // Cache of SubEntries for the given SSOToken
    // Limited cache so that it does not grow in size
    protected Map ssoTokenToSubEntries = new Cache(100);
    private long lastUpdated;

    protected CachedSMSEntry cachedEntry;

    protected String notificationID;

    // Debug & I18n variables
    private static Debug debug = SMSEntry.debug;

    // Private constructor, can be instantiated only via getInstance
    private CachedSubEntries(SSOToken t, String dn) throws SMSException {
        try {
            cachedEntry = CachedSMSEntry.getInstance(t, dn);
            // Register for notifications to clear instance cache
            notificationID = SMSEventListenerManager
                .notifyChangesToSubNodes(t, dn, this);
        } catch (SSOException ssoe) {
            // invalid ssoToken
            debug.warning("CachedSubEntries::init Invalid SSOToken", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries::init: " + dn);
        }
    }

    /**
     * Returns one-level sub-entries for the given DN.
     * Results are cached.
     * 
     * @param t SSOToken to used for searching
     * @return sub entries for the given DN
     * @throws com.sun.identity.sm.SMSException
     * @throws com.iplanet.sso.SSOException
     */
    protected Set getSubEntries(SSOToken t) throws SMSException, SSOException {
        String tokenID = t.getTokenID().toString();
        Set subEntries = (Set) ssoTokenToSubEntries.get(tokenID);
        if ((subEntries != null) && SMSEntry.cacheSMSEntries) {
            if (debug.messageEnabled()) {
                debug.message("CachedSubEntries:getSubEntries Entries from " +
                    "cache: " + subEntries);
            }
            // Check if cached entries can be used
            if (CachedSMSEntry.ttlEnabled && ((System.currentTimeMillis() -
                lastUpdated) > CachedSMSEntry.ttl)) {
                // Clear the cache
                ssoTokenToSubEntries.clear();
            } else {
                return (new LinkedHashSet(subEntries));
            }
        }
        // Obtain sub-entries and add to cache
        subEntries = getSubEntries(t, "*");
        if (SMSEntry.cacheSMSEntries) {   
            // Add to cache
            Set answer = new LinkedHashSet(subEntries);
            ssoTokenToSubEntries.put(tokenID, answer);
            subEntries = new LinkedHashSet(answer);
            lastUpdated = System.currentTimeMillis();
        }
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries:getSubEntries Entries from " +
                "DataStore: " + subEntries);
        }
        return (subEntries);
    }

    /**
     * Return sub-entries that match the pattern.
     * Performs data store operation, the results are not cached
     * 
     * @param token
     * @param pattern
     * @return subentries that match the pattern
     * @throws com.sun.identity.sm.SMSException
     * @throws com.iplanet.sso.SSOException
     */
    public Set getSubEntries(SSOToken token, String pattern)
            throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries: reading sub-entries DN: " + 
               cachedEntry.getDN() + " pattern: " + pattern);
        }
        return (cachedEntry.getSMSEntry().subEntries(
            token, pattern, 0, false, true));
    }

    /**
     * Returns sub-entries that belong to given SubSchema name and
     * statisfies the pattern. The results are not cached.
     * 
     * @param token
     * @param pattern
     * @param serviceidPattern sub-schema name
     * @return subentries that belong to given SubSchema name and satisfies
     * the pattern
     * @throws com.sun.identity.sm.SMSException
     * @throws com.iplanet.sso.SSOException
     */
    public Set getSchemaSubEntries(SSOToken token, String pattern,
            String serviceidPattern) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries: reading sub-entries DN: " + 
                cachedEntry.getDN() + " pattern: " + serviceidPattern);
        }
        return (cachedEntry.getSMSEntry().schemaSubEntries(
            token, pattern, serviceidPattern, 0, true, true));
    }

    protected void add(String entry) {
        // Clear the cache, will be updated in the next lookup
        ssoTokenToSubEntries.clear();
    }

    protected void remove(String entry) {
        // Clear the cache, will be updated in the next lookup
        ssoTokenToSubEntries.clear();
    }

    protected boolean isEmpty(SSOToken t) throws SMSException, SSOException {
        return (getSubEntries(t).isEmpty());
    }

    protected boolean contains(SSOToken t, String entry) throws SMSException,
            SSOException {
        return (getSubEntries(t).contains(entry));
    }

    protected SMSEntry getSMSEntry() {
        if (cachedEntry.isDirty()) {
            cachedEntry.refresh();
        }
        return (cachedEntry.getSMSEntry());
    }

    protected void update() {
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries::update called for dn: " 
                + cachedEntry.getDN());
        }
        // Clear the cache, will be updated in the next lookup
        ssoTokenToSubEntries.clear();
    }

    protected void finalize() throws Throwable {
        SMSEventListenerManager.removeNotification(notificationID);
    }

    /**
     * Returns realm names that matches the given pattern. If <code>
     * recursive<code> is set to <code>true</code>, a sub-tree search
     * is performed. The results are not cached.
     * 
     * @param token
     * @param pattern
     * @param recursive
     * @return realm names that matches the given pattern
     * @throws com.sun.identity.sm.SMSException
     * @throws com.iplanet.sso.SSOException
     */
    public Set searchSubOrgNames(SSOToken token, String pattern,
        boolean recursive) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries: reading subOrgNames DN: " + 
                cachedEntry.getDN() + " pattern: " + pattern);
        }
        return (cachedEntry.getSMSEntry().searchSubOrgNames(
            token, pattern, 0, !recursive, !recursive, recursive));
    }

    /**
     * Returns realm names that match the attribute-values pair for the
     * given service name. The attribute-values pairs is based on organization
     * attribute schema. A sub-tree search is performed.
     * The results are not cached.
     * 
     * @param token
     * @param serviceName
     * @param attrName
     * @param values
     * @return realm names that match the attributevalues pair for the given
     * service name
     * @throws com.sun.identity.sm.SMSException
     * @throws com.iplanet.sso.SSOException
     */
    public Set searchOrgNames(SSOToken token, String serviceName,
            String attrName, Set values) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries: reading orgNames DN: " + 
                cachedEntry.getDN() + " attrName: " + attrName);
        }
        return (cachedEntry.getSMSEntry().searchOrganizationNames(
            token, 0, true, true, serviceName, attrName, values));
    }
    
    // Static methods to get object instance and to clear cache
    public static CachedSubEntries getInstanceIfCached(
        SSOToken token, String dn, boolean cached) throws SMSException {
        if (debug.messageEnabled()) {
            debug.message("CachedSubEntries::getInstance DN: " + dn);
        }
        String entry = DNUtils.normalizeDN(dn);
        CachedSubEntries answer = (CachedSubEntries) smsEntries.get(entry);
        if ((answer != null) || cached) {
            return (answer);
        }
        // Not in cache, synchronize and add to cache
        synchronized (smsEntries) {
            answer = (CachedSubEntries) smsEntries.get(entry);
            if (answer == null) {
                // Create and add to cache
                answer = new CachedSubEntries(token, dn);
                smsEntries.put(entry, answer);
            }
        }
        return (answer);
    }
    public static CachedSubEntries getInstance(SSOToken token, String dn)
            throws SMSException {
        return (getInstanceIfCached(token, dn, false));
    }

    static void clearCache() {
        synchronized (smsEntries) {
            // Clear the individual cached entries
            for (Iterator items = smsEntries.values().iterator();
                items.hasNext();) {
                CachedSubEntries entry = (CachedSubEntries) items.next();
                entry.update();
            }
        }
    }
}
