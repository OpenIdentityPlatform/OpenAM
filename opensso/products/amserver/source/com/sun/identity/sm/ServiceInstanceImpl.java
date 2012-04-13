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
 * $Id: ServiceInstanceImpl.java,v 1.6 2008/07/11 01:46:20 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>ServiceInstanceImpl</code> provides methods to get
 * service's instance variables.
 */
class ServiceInstanceImpl {
    // Cached SMS entry
    private String name;

    private String group;

    private String uri;

    private CachedSMSEntry smsEntry;

    // Instance attributes
    private Map attributes;

    private ServiceInstanceImpl(String name, CachedSMSEntry entry) {
        this.name = name;
        smsEntry = entry;
        smsEntry.addServiceListener(this);
        if (smsEntry.isDirty()) {
            smsEntry.refresh();
        } else {
            update();
        }
    }

    String getName() {
        return (name);
    }

    String getGroup() {
        return (group);
    }

    String getURI() {
        return (uri);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("\nService Instance: ").append(name).append("\n\tGroup: ")
                .append(getGroup()).append("\n\tURI: ").append(getURI())
                .append("\n\tAttributes: ").append(attributes);
        return (sb.toString());
    }

    Map getAttributes() {
        return (SMSUtils.copyAttributes(attributes));
    }

    SMSEntry getSMSEntry() {
        return (smsEntry.getClonedSMSEntry());
    }

    void refresh(SMSEntry newEntry) throws SMSException {
        smsEntry.refresh(newEntry);
    }

    // Gets calls by local changes and also by notifications threads
    // Hence synchronized to avoid data corruption
    synchronized void update() {
        // Read the attributes
        SMSEntry entry = smsEntry.getSMSEntry();
        attributes = SMSUtils.getAttrsFromEntry(entry);

        // Get the group attribute
        group = SMSUtils.DEFAULT;
        String[] groups = entry.getAttributeValues(SMSEntry.ATTR_SERVICE_ID);
        if (groups != null) {
            group = groups[0];
        }

        // Get the URI
        uri = null;
        String[] uris = entry.getAttributeValues(SMSEntry.ATTR_LABELED_URI);
        if (uris != null) {
            uri = uris[0];
        }
    }
    
    boolean isValid() {
        if (smsEntry.isValid() && smsEntry.isDirty()) {
            smsEntry.refresh();
        }
        return (smsEntry.isValid());
    }
    
    void clear() {
        smsEntry.removeServiceListener(this);
        if (smsEntry.isValid()) {
            smsEntry.clear();
        }
    }

    // ----------------------------------------------------------
    // Protected static methods
    // ----------------------------------------------------------
    static ServiceInstanceImpl getInstance(SSOToken token, String serviceName,
            String version, String iName) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("ServiceInstanceImpl::getInstance: called: " +
                 serviceName + "(" + version + ")" + " Instance: " + iName);
        }
        String cName = getCacheName(serviceName, version, iName);
        // Check the cache
        ServiceInstanceImpl answer = getFromCache(cName, serviceName, version,
                iName, token);
        if (answer != null) {
            // Check if the entry has to be updated
            if (!SMSEntry.cacheSMSEntries || answer.smsEntry.isDirty()) {
                // Since the SMSEntries are not to be cached, read the entry
                answer.smsEntry.refresh();
            }
            return (answer);
        }

        // Construct the service instance
        synchronized (serviceInstances) {
            // Check cache again, in case it was added by another thread
            if ((answer = getFromCache(cName, serviceName, version, iName,
                token)) == null) {
                // Still not present in cache, create and add to cache
                CachedSMSEntry entry = checkAndUpdatePermission(cName,
                        serviceName, version, iName, token);
                answer = new ServiceInstanceImpl(iName, entry);
                serviceInstances.put(cName, answer);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ServiceInstanceImpl::getInstance: success: " +
                serviceName + "(" + version + ")" + " Instance: " + iName);
        }
        return (answer);
    }

    // Clears the cache
    static void clearCache() {
        synchronized (serviceInstances) {
            for (Iterator items = serviceInstances.values().iterator();
                items.hasNext();) {
                ServiceInstanceImpl impl = (ServiceInstanceImpl) items.next();
                impl.clear();
            }
            serviceInstances.clear();
        }
    }

    static String getCacheName(String sName, String version, String ins) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(sName).append(version).append(ins);
        return (sb.toString().toLowerCase());
    }

    static ServiceInstanceImpl getFromCache(String cacheName, String sName,
            String version, String iName, SSOToken t) throws SMSException,
            SSOException {
        ServiceInstanceImpl answer = (ServiceInstanceImpl) serviceInstances
                .get(cacheName);
        if (answer != null && !answer.smsEntry.isValid()) {
            // CachedSMSEntry is invalid. Recreate this instance
            serviceInstances.remove(cacheName);
            answer = null;
        }
        if (answer != null) {
            // Check if the user has permissions
            Set principals = (Set) userPrincipals.get(cacheName);
            if (!principals.contains(t.getTokenID().toString())) {
                // Check if Principal has permission to read entry
                checkAndUpdatePermission(cacheName, sName, version, iName, t);
            }
        }
        return (answer);
    }

    static synchronized CachedSMSEntry checkAndUpdatePermission(
            String cacheName, String serviceName, String version, String iName,
            SSOToken t) throws SMSException, SSOException {
        // Construct the DN
        String dn = "ou=" + iName + "," + CreateServiceConfig.INSTANCES_NODE
                + ServiceManager.getServiceNameDN(serviceName, version);
        CachedSMSEntry entry = CachedSMSEntry.getInstance(t, dn);
        if (entry.isDirty()) {
            entry.refresh();
        }
        if (entry.isNewEntry()) {
            String[] args = { iName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-no-such-instance", args));
        }
        Set sudoPrincipals = (Set) userPrincipals.get(cacheName);
        if (sudoPrincipals == null) {
            sudoPrincipals = Collections.synchronizedSet(new HashSet());
        }
        sudoPrincipals.add(t.getTokenID().toString());
        userPrincipals.put(cacheName, sudoPrincipals);
        return (entry);
    }

    private static Map serviceInstances = Collections.synchronizedMap(
        new HashMap());

    private static Map userPrincipals = Collections.synchronizedMap(
        new HashMap());

    private static Debug debug = SMSEntry.debug;
    
    public String toXML() {
        StringBuilder buff = new StringBuilder();
        buff.append("<")
            .append(SMSUtils.INSTANCE)
            .append(" ").append(SMSUtils.NAME).append("=\"").append(name)
            .append("\"")
            .append(" ").append(SMSUtils.GROUP).append("=\"").append(group)
            .append("\"");
        
        if ((uri != null) && (uri.length() > 0)) {
            buff.append(" ").append(SMSUtils.URI).append("=\"").append(uri)
                .append("\"");
        }
        buff.append(">");
        buff.append(SMSUtils.toAttributeValuePairXML(attributes));
        buff.append("</")
            .append(SMSUtils.INSTANCE)
            .append(">\n");
        return buff.toString();
    }
}
