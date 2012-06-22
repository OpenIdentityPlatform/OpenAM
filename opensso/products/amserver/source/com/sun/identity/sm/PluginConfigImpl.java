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
 * $Id: PluginConfigImpl.java,v 1.5 2008/07/11 01:46:21 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>PluginConfigImpl</code> provides interfaces to read the
 * plugin configuration information of a service.
 */
class PluginConfigImpl {
    private PluginSchemaImpl ps;

    private boolean newEntry;

    private String orgName;

    private int priority;

    private Map attributes;

    private CachedSMSEntry smsEntry;

    /**
     * Private constructor
     */
    private PluginConfigImpl(PluginSchemaImpl ps, CachedSMSEntry entry,
            String orgName) throws SMSException {
        this.ps = ps;
        this.orgName = (orgName == null) ? SMSEntry.baseDN : orgName;
        smsEntry = entry;
        smsEntry.addServiceListener(this);
        // Read the attributes
        if (smsEntry.isDirty()) {
            smsEntry.refresh();
        } else {
            update();
        }
    }

    /**
     * Returns the organization name
     */
    String getOrganizationName() {
        return (orgName);
    }

    /**
     * Returns the priority assigned to the service configuration.
     */
    int getPriority() {
        return (priority);
    }

    /**
     * Returns the service configuration parameters. The keys in the
     * <code>Map</code> contains the attribute names and their corresponding
     * values in the <code>Map</code> is a <code>Set</code> that contains
     * the values for the attribute.
     */
    Map getAttributes() {
        if (!SMSEntry.cacheSMSEntries || smsEntry.isDirty()) {
            // Read the entry, since it should not be cached
            smsEntry.refresh();
        }
        return (SMSUtils.copyAttributes(attributes));
    }

    /**
     * Returns the DN associated with this entry
     */
    String getDN() {
        return (smsEntry.getDN());
    }

    /**
     * Returns the SMSEntry associated with this object
     */
    SMSEntry getSMSEntry() {
        return (smsEntry.getClonedSMSEntry());
    }

    /**
     * Updates the SMSEntry with the new changes
     */
    void refresh(SMSEntry e) throws SMSException {
        smsEntry.refresh(e);
    }

    /**
     * Returns the PluginSchemaImpl assicated with this object
     */
    PluginSchemaImpl getPluginSchemaImpl() {
        return (ps);
    }

    /**
     * Checks if the entry exists in the directory
     */
    boolean isNewEntry() {
        return (newEntry);
    }

    // Gets calls by local changes and also by notifications threads
    // Hence synchronized to avoid data corruption
    synchronized void update() {
        // Get the SMSEntry
        SMSEntry entry = smsEntry.getSMSEntry();
        newEntry = entry.isNewEntry();

        // Read the attributes
        attributes = SMSUtils.getAttrsFromEntry(entry);

        // Add default values, if attribute not present
        // and decrypt password attributes
        Iterator ass = ps.getAttributeSchemaNames().iterator();
        while (ass.hasNext()) {
            AttributeValidator av = ps.getAttributeValidator((String) ass
                    .next());
            attributes = av.inheritDefaults(attributes);
        }

        // Read the priority
        priority = 0;
        String priorities[] = entry.getAttributeValues(SMSEntry.ATTR_PRIORITY);
        if (priorities != null) {
            try {
                priority = Integer.parseInt(priorities[0]);
            } catch (NumberFormatException nfe) {
                SMSEntry.debug.error("ServiceConfig::getPriority() " + nfe);
            }
        }
    }
    
    boolean isValid() {
        if (smsEntry.isValid() && smsEntry.isDirty()) {
            smsEntry.refresh();
        }
        return (smsEntry.isValid());
    }
    
    void clear() {
        // Deregister from CachedSMSEntry
        smsEntry.removeServiceListener(this);
        if (smsEntry.isValid()) {
            smsEntry.clear();
        }
    }

    // ------------------------------------------------------------------
    // Static Protected method to get an instance of ServiceConfigImpl
    // ------------------------------------------------------------------
    static PluginConfigImpl getInstance(SSOToken token, PluginSchemaImpl ps,
            String dn, String oName) throws SSOException, SMSException {
        if (debug.messageEnabled()) {
            debug.message("PluginConfigImpl::getInstance: called: " + dn);
        }
        String orgName = DNMapper.orgNameToDN(oName);
        String cacheName = getCacheName(ps, orgName);
        // Check in cache
        PluginConfigImpl answer = getFromCache(cacheName, dn, token);
        if (answer != null) {
            if (!SMSEntry.cacheSMSEntries || answer.smsEntry.isDirty()) {
                // Read the entry, since it should not be cached
                answer.smsEntry.refresh();
            }
            return (answer);
        }

        // Check if the orgName exists
        if (!SMSEntry.checkIfEntryExists(
            DNMapper.orgNameToDN(orgName), token)) {
            // Object [] args = { orgName };
            // throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
            // "sms-invalid-org-name", args));
            return (null);
        }

        // Construct the PluginConfigImpl object
        synchronized (configImpls) {
            // Check the cache again, in case it was added by another thread
            if ((answer = getFromCache(cacheName, dn, token)) == null) {
                CachedSMSEntry entry = checkAndUpdatePermission(cacheName, dn,
                        token);
                answer = new PluginConfigImpl(ps, entry, orgName);
                configImpls.put(cacheName, answer);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("PluginConfigImpl::getInstance: return: " + dn);
        }
        return (answer);
    }

    // Clears the cache
    static void clearCache() {
        synchronized (configImpls) {
            for (Iterator items = configImpls.values().iterator();
                items.hasNext();) {
                PluginConfigImpl pci = (PluginConfigImpl) items.next();
                pci.clear();
            }
            configImpls.clear();
        }
    }

    static String getCacheName(PluginSchemaImpl ps, String orgName) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(ps.getName()).append(ps.getVersion()).append(orgName);
        return (sb.toString().toLowerCase());
    }

    static PluginConfigImpl getFromCache(String cacheName, 
            String dn, SSOToken t) throws SMSException, SSOException {
        PluginConfigImpl answer = (PluginConfigImpl) configImpls.get(cacheName);
        if (answer != null && !answer.smsEntry.isValid()) {
            // CachedSMSEntry is invalid, so create a new PluginConfigImpl
            // by clearing cache and setting this one to null
            configImpls.remove(cacheName);
            answer = null;
        }
        if (answer != null) {
            // Check if the user has permissions
            Set principals = (Set) userPrincipals.get(cacheName);
            if (!principals.contains(t.getTokenID().toString())) {
                // Check if Principal has permission to read the entry
                checkAndUpdatePermission(cacheName, dn, t);
            }
        }
        return (answer);
    }

    static synchronized CachedSMSEntry checkAndUpdatePermission(
            String cacheName, String dn, SSOToken token) throws SMSException,
            SSOException {
        CachedSMSEntry answer = CachedSMSEntry.getInstance(token, dn);
        Set sudoPrincipals = (Set) userPrincipals.get(cacheName);
        if (sudoPrincipals == null) {
            sudoPrincipals = Collections.synchronizedSet(new HashSet());
            userPrincipals.put(cacheName, sudoPrincipals);
        }
        sudoPrincipals.add(token.getTokenID().toString());
        return (answer);
    }

    private static Map configImpls = Collections.synchronizedMap(
        new HashMap());

    private static Map userPrincipals = Collections.synchronizedMap(
        new HashMap());

    private static Debug debug = SMSEntry.debug;
}
