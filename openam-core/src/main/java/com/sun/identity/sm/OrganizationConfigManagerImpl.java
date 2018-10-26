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
 * $Id: OrganizationConfigManagerImpl.java,v 1.12 2009/07/25 05:11:55 qcheng Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.sun.identity.sm;

import javax.naming.event.NamingEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;

/**
 * The class <code>OrganizationConfigManagerImpl</code> provides interfaces to
 * read the service's configuration data. It provides access to
 * <code>OrganizationConfigImpl</code> which represents a single
 * "configuration" in the service. It manages configuration data only for GLOBAL
 * and ORGANIZATION types.
 */
class OrganizationConfigManagerImpl implements SMSObjectListener {

    // Instance variables
    private String orgDN;

    private CachedSubEntries subEntries;
    
    private CachedSMSEntry smsEntry;

    // Pointer to schema changes listeners
    private Map listenerObjects = Collections.synchronizedMap(
        new HashMap());
    private String listenerId;

    // Notification search string
    private String orgNotificationSearchString;

    /**
     * Constructs an instance of <code>OrganizationConfigManagerImpl</code>
     * for the given organization. It requires an user identity that will be
     * used to perform read operations. It is assumed that the application
     * calling this constructor should authenticate the user.
     */
    private OrganizationConfigManagerImpl(CachedSMSEntry entry,
        String orgDN, SSOToken token) throws SMSException {
        this.smsEntry = entry;
        this.orgDN = orgDN;
        
        // Register for notifications
        listenerId = SMSNotificationManager.getInstance()
            .registerCallbackHandler(this);

        if (!orgDN.startsWith(SMSEntry.SERVICES_RDN)) {
            DN notifyDN = DN.valueOf(SMSEntry.SERVICES_RDN + "," + orgDN);
            orgNotificationSearchString = notifyDN.toString().toLowerCase();
        } else {
            orgNotificationSearchString = orgDN;
        }
    }

    /**
     * Returns organization name as DN
     */
    String getOrgDN() {
        return (orgDN);
    }

    /**
     * Returns a set of service names that are assigned to this realm
     */
    Set getAssignedServices(SSOToken token) throws SMSException {
        try {
            HashSet answer = new HashSet();
            // Get service names and iterate through them
            CachedSubEntries se = null;
            if (orgDN.equals(DNMapper.serviceDN)) {
                se = CachedSubEntries.getInstance(token, orgDN);
            } else {
                se = CachedSubEntries.getInstance(token, "ou=services,"+orgDN);
            }
            for (Iterator names = se.getSubEntries(token).iterator(); names
                    .hasNext();) {
                String serviceName = (String) names.next();
                ServiceConfigManagerImpl scmi;
                if (ServiceManager.isCoexistenceMode()) {
                    // For backward compatibility, get the version from the
                    // service. no hardcoding to '1.0', even if it improves
                    // performance in OpenAM. Otherwise, it breaks for
                    // services like iplanetAMProviderConfigService with
                    // '1.1' as version.
                    scmi = ServiceConfigManagerImpl
                       .getInstance(token, serviceName, ServiceManager
                       .serviceDefaultVersion(token, serviceName));
                } else {
                    // handle special case for co-existence of OpenSSO and
                    // AM 7.1. The version for iplanetAMProviderConfigService
                    // is "1.1" instead of "1.0" for other services.
                    scmi = ServiceConfigManagerImpl.getInstance(token, 
                        serviceName, ServiceManager.getVersion(serviceName));
                }
                try {
                    ServiceConfigImpl sci = scmi.getOrganizationConfig(token,
                            orgDN, null);
                    if (sci != null && !sci.isNewEntry()) {
                        answer.add(serviceName);
                    }
                } catch (SMSException smse) {
                    if (smse.getExceptionCode() != 
                        SMSException.STATUS_NO_PERMISSION) 
                    {
                        throw (smse);
                    }
                }
            }
            return (answer);
        } catch (SSOException ssoe) {
            debug.error("OrganizationConfigManagerImpl.getAssignedServices "
                    + "Unable to get assigned services", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Returns the names of all suborganizations.
     */
    Set getSubOrganizationNames(SSOToken token) throws SMSException {
        return (getSubOrganizationNames(token, "*", false));
    }

    /**
     * Returns the names of suborganizations that match the given pattern.
     */
    Set getSubOrganizationNames(SSOToken token, String pattern,
            boolean recursive) throws SMSException {

        try {
            if (subEntries == null) {
                subEntries = CachedSubEntries.getInstance(token, orgDN);
            }
            return (subEntries.searchSubOrgNames(token, pattern, recursive));
        } catch (SSOException ssoe) {
            debug.error("OrganizationConfigManagerImpl: Unable to "
                    + "get sub organization names", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Registers for changes to organization's configuration. The object will be
     * called when configuration for this organization is changed.
     * 
     * @param listener
     *            callback object that will be invoked when organization
     *            configuration has changed
     * @return an ID of the registered listener.
     */

    synchronized String addListener(ServiceListener listener) {
        String id = SMSUtils.getUniqueID();
        listenerObjects.put(id, listener);
        return (id);
    }

    /**
     * Removes the listener from the organization for the given listener ID. The
     * ID was issued when the listener was registered.
     * 
     * @param listenerID
     *            the listener ID issued when the listener was registered
     */
    public void removeListener(String listenerID) {
        listenerObjects.remove(listenerID);
        if ((listenerID != null) && listenerObjects.isEmpty()) {
            SMSNotificationManager.getInstance().removeCallbackHandler(
                listenerID);
        }
    }

    // Implementations for SMSObjectListener
    public void allObjectsChanged() {
        // Ignore, do nothing
    }
    
    public void objectChanged(String dn, int type) {
        // Check for listeners
        if (listenerObjects.isEmpty()) {
            if (SMSEntry.eventDebug.messageEnabled()) {
                SMSEntry.eventDebug.message("OrgConfigMgrImpl::entryChanged"
                        + " No listeners registered: " + dn
                        + "\norgNotificationSearchString: "
                        + orgNotificationSearchString);
            }
            return;
        }

        // check for service name, version and type
        int index = 0;
        int orgIndex = 0;

        // From realm tree, orgNotificationSearchString will be
        // ou=services,o=hpq,ou=services,dc=iplanet,dc=com
        if (SMSEntry.eventDebug.messageEnabled()) {
            SMSEntry.eventDebug.message("OrgConfigMgrImpl::entryChanged "
                    + " DN: " + dn + "\norgNotificationSearchString: "
                    + orgNotificationSearchString);
        }

        // Check if the DN matches with organization name
        if ((index = dn.indexOf(orgNotificationSearchString)) != -1) {
            orgIndex = SMSEntry.SERVICES_RDN.length();

            // Initialize parameters
            String serviceName = "";
            String version = "";
            String groupName = "";
            String compName = "";

            // Get the DN ignoring the organization name
            if (index != 0) {
                DN ndn = DN.valueOf(dn.substring(0, index - 1));
                int size = ndn.size();
                // Needs to check if the DN has more realm names
                if (size != 0 && "o".equals(LDAPUtils.rdnValue(ndn.rdn()))) {
                    // More realm names are present, changes not meant for
                    // this organization
                    if (SMSEntry.eventDebug.messageEnabled()) {
                        SMSEntry.eventDebug.message(
                            "OrgConfigMgrImpl::entryChanged  Notification " +
                            "not sent since realms names donot match. \nDN: " +
                            dn + " And orgNotificationSearchString: " + 
                            orgNotificationSearchString);
                    }
                    return;
                }

                Iterator<RDN> rdnIterator = ndn.iterator();
                // Get the version, service, group and component name
                if (size > 0) {
                    serviceName = LDAPUtils.rdnValue(rdnIterator.next());
                }
                if (size > 1) {
                    version = LDAPUtils.rdnValue(rdnIterator.next());
                }
                if (size >= 4) {
                    //Skip 1 RDNs
                    rdnIterator.next();
                    groupName = LDAPUtils.rdnValue(rdnIterator.next());
                }

                // The subconfig names should be "/" separated and left to right
                if (ndn.size() >= 5) {
                    StringBuilder sbr = new StringBuilder();
                    while (rdnIterator.hasNext()) {
                        sbr.append('/').append(LDAPUtils.rdnValue(rdnIterator.next()));
                    }
                    compName = sbr.toString();
                } else {
                    compName = "/";
                }
            }

            // Convert changeType from JNDI to com.sun.identity.shared.ldap
            switch (type) {
            case NamingEvent.OBJECT_ADDED:
                type = ServiceListener.ADDED;
                break;
            case NamingEvent.OBJECT_REMOVED:
                type = ServiceListener.REMOVED;
                break;
            default:
                type = ServiceListener.MODIFIED;
            }

            // Get organization name
            String orgName = dn.substring(index + orgIndex + 1);

            if (SMSEntry.eventDebug.messageEnabled()) {
                SMSEntry.eventDebug.message("OrganizationConfigManagerImpl:"
                        + "entryChanged() serviceName " + serviceName);
                SMSEntry.eventDebug.message("OrganizationConfigManagerImpl:"
                        + "entryChanged() version " + version);
                SMSEntry.eventDebug.message("OrganizationConfigManagerImpl:"
                        + "entryChanged() orgName " + orgName);
                SMSEntry.eventDebug.message("OrganizationConfigManagerImpl:"
                        + "entryChanged() groupName " + groupName);
                SMSEntry.eventDebug.message("OrganizationConfigManagerImpl:"
                        + "entryChanged() compName " + compName);
                SMSEntry.eventDebug.message("OrganizationConfigManagerImpl:"
                        + "entryChanged() type " + type);
            }

            // Send notifications to listeners
            notifyOrgConfigChange(serviceName, version, orgName, groupName,
                    compName, type);
        }
    }

    void notifyOrgConfigChange(String serviceName, String version,
        String orgName, String groupName, String comp, int type) {
        synchronized (listenerObjects) {
            Iterator items = listenerObjects.values().iterator();
            while (items.hasNext()) {
                ServiceListener sl = (ServiceListener) items.next();
                try {
                    sl.organizationConfigChanged(serviceName, version, orgName,
                        groupName, comp, type);
                } catch (Throwable t) {
                    SMSEntry.eventDebug.error("OrganizationConfigManager" +
                        "Impl:notifyOrgConfigChange Error sending notify to" +
                        sl.getClass().getName(), t);
                }
            }
        }
    }
    
    void clear() {
        // Clears the listeners
        if ((listenerId != null) && ((listenerObjects == null) ||
            listenerObjects.isEmpty())) {
            SMSNotificationManager.getInstance().removeCallbackHandler(
                listenerId);
        }
        if (smsEntry.isValid()) {
            smsEntry.clear();
        }
    }
    
    boolean isValid() throws SMSException {
        if ((smsEntry.isValid() && smsEntry.isDirty()) ||
            ServiceManager.isCoexistenceMode()) {
            // If in co-exist mode, SMS will not get updates for org
            // hence have to update the SMSEntry
            smsEntry.refresh();
        }
        // Check if the organization still exists
        if (smsEntry.isNewEntry()) {
            if (debug.messageEnabled()) {
                debug.message("OrganizationConfigManagerImpl::isValid" +
                    " Organization deleted: " + orgDN);
            }
            String args[] = {orgDN};
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-REALM_NAME_NOT_FOUND", args));
        }
        // If entry is invalid, remove from cache
        if (!smsEntry.isValid()) {
            configMgrImpls.remove(orgDN);
        }
        return (smsEntry.isValid());
    }

    // ---------------------------------------------------------
    // Static Protected Methods
    // ---------------------------------------------------------
    protected static OrganizationConfigManagerImpl getInstance(SSOToken token,
        String orgName) throws SMSException, SSOException {

        // Convert orgName to DN
        String orgDN = DNMapper.orgNameToDN(orgName);
        // If orgDN is the baseDN, append "ou=services" to it
        if (orgDN.equalsIgnoreCase(SMSEntry.baseDN)) {
            orgDN = DNMapper.serviceDN;
        }
        if (debug.messageEnabled()) {
            debug.message("OrganizationConfigMgrImpl::getInstance: called: " +
                "(" + orgName + ")=" + orgDN);
        }

        // check in cache for organization name
        OrganizationConfigManagerImpl answer = getFromCache(orgDN, token);
        if ((answer != null) && ServiceManager.isRealmEnabled()) {
            // If in co-exist mode, SMS will not get updates for org
            // hence have to update the cEntry
            if (ServiceManager.isCoexistenceMode()) {
                answer.smsEntry.refresh();
            }
        } else {
            // Not in cache, construct the object and validate
            synchronized (configMgrImpls) {
                // Check the cache again, maybe added by another thread
                if ((answer = getFromCache(orgDN, null)) == null) {
                    CachedSMSEntry cEntry =
                        checkAndUpdatePermission(orgDN, token);
                    // If in co-exist mode, SMS will not get updates for org
                    // hence have to update the cEntry
                    if (ServiceManager.isCoexistenceMode()) {
                        cEntry.update();
                    }
                    answer = new OrganizationConfigManagerImpl(
                        cEntry, orgDN, token);
                    configMgrImpls.put(orgDN, answer);
                }
            }
        }
        // Validate the entry
        answer.isValid();
        
        if (debug.messageEnabled()) {
            debug.message("OrganizationConfigMgrImpl::getInstance: success: " +
                orgDN);
        }
        return (answer);
    }

    private static OrganizationConfigManagerImpl getFromCache(String cacheName, 
        SSOToken t) throws SMSException, SSOException {
         OrganizationConfigManagerImpl answer = (OrganizationConfigManagerImpl)
            configMgrImpls.get(cacheName);
        if ((answer != null) && (t != null)) {
            // Check of OCM is valid
            if (!answer.isValid()) {
                configMgrImpls.remove(cacheName);
                answer = null;
            } else {
                // Check if the user has permissions
                Set principals = (Set) userPrincipals.get(cacheName);
                if ((principals == null) ||
                    !principals.contains(t.getTokenID().toString())) {
                    // Principal name not in cache, need to check perm
                    checkAndUpdatePermission(cacheName, t);
                }
            }
        }
        return (answer);
    }

    private static CachedSMSEntry checkAndUpdatePermission(String cacheName,
        SSOToken t) throws SSOException, SMSException {
        CachedSMSEntry answer = CachedSMSEntry.getInstance(t, cacheName);
        Set sudoPrincipals = (Set) userPrincipals.get(cacheName);
        if (sudoPrincipals == null) {
            sudoPrincipals = Collections.synchronizedSet(new HashSet(2));
            userPrincipals.put(cacheName, sudoPrincipals);
        }
        sudoPrincipals.add(t.getTokenID().toString());
        return (answer);
    }
    
    static void clearCache() {
        for (OrganizationConfigManagerImpl ocm : configMgrImpls.values()) {
        	ocm.clear();
		}
        configMgrImpls.clear();
    }

    private static Map<String,OrganizationConfigManagerImpl> configMgrImpls = new ConcurrentHashMap<String, OrganizationConfigManagerImpl>();

    private static Map userPrincipals = Collections.synchronizedMap(
        new HashMap());

    private static Debug debug = SMSEntry.debug;
}
