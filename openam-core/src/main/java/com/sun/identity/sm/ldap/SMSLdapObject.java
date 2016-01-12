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
 * $Id: SMSLdapObject.java,v 1.27 2009/11/20 23:52:56 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.sm.ldap;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.DataLayer;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectDB;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSUtils;

import java.security.AccessController;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DereferenceAliasesPolicy;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.EntryNotFoundException;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.SortKey;
import org.forgerock.opendj.ldap.controls.ServerSideSortRequestControl;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

/**
 * This object represents an LDAP entry in the directory server. The UMS have an
 * equivalent class called PersistentObject. The SMS could not integrate with
 * PersistentObject, because of the its dependecy on the Session object. This
 * would mean that, to instantiate an PersistentObject inside SMS, we need to
 * create an UMS instance, which would be having directory parameters of SMS.
 * <p>
 * This class is used both to read and write information into the directory
 * server. The appropriate constructors discusses it is done.
 * <p>
 * There can be only three types of SMS entries in the directory (i) entry with
 * organizationUnit object class (attribute: ou) (ii) entry with sunService
 * object class (attributes: ou, labeledURI, sunServiceSchema, sunPluginSchema,
 * and sunKeyValue (sunXMLKeyValue, in the future) (iii) entry with
 * sunServiceComponent object class (attributes: ou, sunServiceID,
 * sunSMSPriority, sunKeyValue. All the schema, configuration and plugin entries
 * will be stored using the above entries.
 */
public class SMSLdapObject extends SMSObjectDB implements SMSObjectListener {

    public static final String ORG_CANNOT_OBTAIN = "sms-org-cannot-obtain";
    public static final String SUBORG_CANNOT_OBTAIN = "sms-suborg-cannot-obtain";
    // LDAP specific & retry paramters
    static DataLayer dlayer;

    static SMDataLayer smdlayer;

    static int connNumRetry = 3;

    static int connRetryInterval = 1000;

    static Set<ResultCode> retryErrorCodes = new HashSet<>();

    static int entriesPresentCacheSize = 1000;

    static boolean initializedNotification;

    static Set<String> entriesPresent = Collections.synchronizedSet(new LinkedHashSet<String>());

    static Set<String> entriesNotPresent = Collections.synchronizedSet(new LinkedHashSet<String>());

    // Other parameters
    static ResourceBundle bundle;

    boolean initialized;

    static Debug debug;

    static String[] OU_ATTR = new String[1];

    static String[] O_ATTR = new String[1];

    static boolean enableProxy;

    // Admin SSOToken
    static Principal adminPrincipal;

    /**
     * Public constructor for SMSLdapObject
     */
    public SMSLdapObject() throws SMSException {
        // Initialized (should be called only once by SMSEntry)
        initialize();
    }

    /**
     * Synchronized initialized method
     */
    private synchronized void initialize() throws SMSException {
        if (initialized) {
            return;
        }
        // Obtain the I18N resource bundle & Debug
        debug = Debug.getInstance("amSMSLdap");
        AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();
        bundle = amCache.getResBundle(IUMSConstants.UMS_BUNDLE_NAME,
                java.util.Locale.ENGLISH);
        OU_ATTR[0] = getNamingAttribute();
        O_ATTR[0] = getOrgNamingAttribute();

        String enableP = SystemProperties.get(SMSEntry.DB_PROXY_ENABLE);
        enableProxy = (enableP != null) && enableP.equalsIgnoreCase("true");
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: proxy enable value: " + enableProxy);
        }

        try {
            if (enableProxy) {
                // Initialize the principal, used only with AMSDK
                // for proxy connections
                adminPrincipal = new AuthPrincipal((String)
                        AccessController.doPrivileged(new AdminDNAction()));

                // Get UMS datalayer
                dlayer = DataLayer.getInstance();

                if (debug.messageEnabled()) {
                    debug.message("SMSLdapObject: DataLayer instance "
                            + "obtained.");
                }
            } else {
                // Get SM datalayer
                smdlayer = SMDataLayer.getInstance();

                if (debug.messageEnabled()) {
                    debug.message("SMSLdapObject: SMDataLayer instance "
                            + "obtained.");
                }
            }
            if ((dlayer == null) && (smdlayer == null)) {
                debug.error("SMSLdapObject: Unable to initialize LDAP");
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.CONFIG_MGR_ERROR, null));
            }
            debug.message("SMSLdapObject: LDAP Initialized successfully");

            // Get connection retry parameters
            DataLayer.initConnectionParams();
            connNumRetry = DataLayer.getConnNumRetry();
            connRetryInterval = DataLayer.getConnRetryInterval();
            retryErrorCodes = DataLayer.getRetryErrorCodes();

            // Need to check if the root nodes exists. If not, create them
            String serviceDN =
                    SMSEntry.SERVICES_RDN + SMSEntry.COMMA + getRootSuffix();
            if (!entryExists(serviceDN)) {
                Map attrs = new HashMap();
                Set attrValues = new HashSet();
                attrValues.add(SMSEntry.OC_TOP);
                attrValues.add(SMSEntry.OC_ORG_UNIT);
                attrs.put(SMSEntry.ATTR_OBJECTCLASS, attrValues);
                create(adminPrincipal, serviceDN, attrs);
            }
        } catch (Exception e) {
            // Unable to initialize (trouble!!)
            debug.error("SMSEntry: Unable to initalize(exception):", e);
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.CONFIG_MGR_ERROR, null));
        }
        initialized = true;
    }

    private void initializeNotification() {
        if (!initializedNotification) {
            // If cache is enabled, register for notification to maintian
            // internal cache of entriesPresent
            if (SMSNotificationManager.isCacheEnabled()) {
                SMSNotificationManager.getInstance()
                        .registerCallbackHandler(this);
            }
            initializedNotification = true;
        }
    }

    /**
     * Reads in the object from persistent store, assuming that the guid and the
     * SSOToken are valid
     */
    public Map<String, Set<String>> read(SSOToken token, String dn) throws SMSException,
            SSOException {
        if (dn == null || dn.length() == 0) {
            // This must not be possible return an exception.
            debug.error("SMSLdapObject: read():Null or Empty DN=" + dn);
            throw new SMSException(LdapException.newLdapException(ResultCode.NO_SUCH_OBJECT,
                    getBundleString(IUMSConstants.SMS_INVALID_DN, dn)), "sms-NO_SUCH_OBJECT");
        }


        if (!LDAPUtils.isDN(dn)) {
            debug.warning("SMSLdapObject: Invalid DN=" + dn);
            String[] args = {dn};
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME, "sms-INVALID_DN", args);
        }

        // Check if entry does not exist
        if (SMSNotificationManager.isCacheEnabled() && entriesNotPresent.contains(dn)) {
            debug.message("SMSLdapObject:read Entry not present: {} (checked in cache)", dn);
            return null;
        }

        Entry ldapEntry = null;
        int retry = 0;
        while (retry <= connNumRetry) {
            debug.message("SMSLdapObject.read() retry: {}", retry);

            ResultCode errorCode = null;
            try (Connection conn = getConnection(token.getPrincipal())) {
                ldapEntry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(DN.valueOf(dn),
                        getAttributeNames()));
                break;
            } catch (LdapException e) {
                errorCode = e.getResult().getResultCode();
                if (!retryErrorCodes.contains(errorCode) || retry == connNumRetry) {
                    if (errorCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                        // Add to not present Set
                        objectChanged(dn, DELETE);
                        debug.message("SMSLdapObject.read: entry not present: {}", dn);
                        break;
                    } else {
                        debug.warning("SMSLdapObject.read: Error in accessing entry DN: {}", dn, e);
                        throw new SMSException(e, "sms-entry-cannot-access");
                    }
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            }
        }

        if (ldapEntry != null) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.read(): reading entry: " + dn);
            }
            return SMSUtils.convertEntryToAttributesMap(ldapEntry);
        } else {
            return null;
        }
    }

    /**
     * Create an entry in the directory
     */
    public void create(SSOToken token, String dn, Map attrs)
            throws SMSException, SSOException {
        // Call the private method that takes the principal name
        create(token.getPrincipal(), dn, attrs);
        // Update entryPresent cache
        objectChanged(dn, ADD);
    }

    /**
     * Create an entry in the directory using the principal name
     */
    private static void create(Principal p, String dn, Map attrs)
            throws SMSException, SSOException {
        int retry = 0;
        Entry entry = copyMapToEntry(attrs).setName(dn);
        while (retry <= connNumRetry) {
            debug.message("SMSLdapObject.create() retry: {}", retry);

            try (Connection conn = getConnection(p)) {
                conn.add(LDAPRequests.newAddRequest(entry));
                debug.message("SMSLdapObject.create Successfully created entry: {}", dn);
                break;
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (errorCode.equals(ResultCode.ENTRY_ALREADY_EXISTS) && retry > 0) {
                    // During install time and other times,
                    // this error gets throws due to unknown issue. Issue:
                    // Hence mask it.
                    debug.warning("SMSLdapObject.create() Entry Already Exists Error for DN {}", dn);
                    break;
                }

                if (!retryErrorCodes.contains(errorCode) || retry >= connNumRetry) {
                    debug.error("SMSLdapObject.create() Error in creating: {} By Principal: {}", dn, p.getName(), e);
                    throw new SMSException(e, "sms-entry-cannot-create");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    //ignored
                }
            }
        }
    }

    /**
     * Save the entry using the token provided. The principal provided will be
     * used to get the proxy connection.
     */
    public void modify(SSOToken token, String dn, ModificationItem mods[])
            throws SMSException, SSOException {
        int retry = 0;
        ModifyRequest request = copyModItemsToModifyRequest(DN.valueOf(dn), mods);
        while (retry <= connNumRetry) {
            debug.message("SMSLdapObject.modify() retry: {}", retry);

            try (Connection conn = getConnection(token.getPrincipal())) {
                conn.modify(request);
                debug.message("SMSLdapObject.modify(): Successfully modified entry: {}", dn);
                break;
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (!retryErrorCodes.contains(errorCode) || retry == connNumRetry) {
                    debug.error("SMSLdapObject.modify(): Error modifying: {} By Principal {}", dn,
                            token.getPrincipal().getName(), e);
                    throw new SMSException(e, "sms-entry-cannot-modify");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            }
        }
    }

    /**
     * Delete the entry in the directory. This will delete sub-entries also!
     */
    public void delete(SSOToken token, String dn) throws SMSException,
            SSOException {
        // Check if there are sub-entries, delete if present
        for (String entry : subEntries(token, dn, "*", 0, false, false)) {
            debug.message("SMSLdapObject: deleting sub-entry: {}", entry);
            delete(token, getNamingAttribute() + "=" + entry + "," + dn);
        }
        // Check if there are suborganizations, delete if present
        // The recursive 'false' here has the scope SCOPE_ONE
        // while searching for the suborgs.
        // Loop through the suborg at the first level and if there
        // is no next suborg, delete that.
        for (String subOrg : searchSubOrgNames(token, dn, "*", 0, false, false, false)) {
            debug.message("SMSLdapObject: deleting suborganization: {}", subOrg);
            delete(token, subOrg);
        }

        // Get LDAP connection
        delete(token.getPrincipal(), dn);
        // Update entriesPresent cache
        objectChanged(dn, DELETE);
    }

    private static void delete(Principal p, String dn)
            throws SMSException {
        // Delete the entry
        try {
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("SMSLdapObject.delete() retry: " + retry);
                }
                try (Connection conn = getConnection(p)) {
                    conn.delete(LDAPRequests.newDeleteRequest(dn));
                    break;
                } catch (LdapException e) {
                    ResultCode errorCode = e.getResult().getResultCode();
                    if (!retryErrorCodes.contains(errorCode) || retry == connNumRetry) {
                        throw e;
                    }
                    retry++;
                    try {
                        Thread.sleep(connRetryInterval);
                    } catch (InterruptedException ex) {
                        // ignored
                    }
                }
            }
        } catch (LdapException e) {
            debug.warning("SMSLdapObject:delete() Unable to delete entry: {}", dn, e);
            throw new SMSException(e, "sms-entry-cannot-delete");
        }
    }

    /**
     * Returns the sub-entry names. Returns a set of RDNs that are sub-entries.
     * The paramter <code>numOfEntries</code> identifies the number of entries
     * to return, if <code>0</code> returns all the entries.
     */
    public Set<String> subEntries(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        if (filter == null) {
            filter = "*";
        }

        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: SubEntries search: " + dn);
        }

        // Construct the filter
        String sfilter = "(objectClass=*)";
        if (!filter.equals("*")) {
            // This is a workaround for Issue 3823, where DS returns an
            // empty set if restarted during OpenSSO operation
            String[] objs = {filter};
            sfilter = MessageFormat.format(getSearchFilter(), (Object[]) objs);
        }
        Set answer = getSubEntries(token, dn, sfilter, numOfEntries,
                sortResults, ascendingOrder);
        return (answer);
    }

    private Set<String> getSubEntries(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        SearchRequest request = getSearchRequest(dn, filter, SearchScope.SINGLE_LEVEL, numOfEntries, 0, sortResults,
                ascendingOrder, getNamingAttribute(), O_ATTR);
        int retry = 0;

        Set<String> answer = new LinkedHashSet<>();
        ConnectionEntryReader results;
        while (retry <= connNumRetry) {
            debug.message("SMSLdapObject.subEntries() retry: {}", retry);

            try (Connection conn = getConnection(token.getPrincipal())) {
                // Get the sub entries
                ConnectionEntryReader iterResults = conn.search(request);
                iterResults.hasNext();
                results = iterResults;
                // Construct the results and return
                try {
                    while (results != null && results.hasNext()) {
                        try {
                            if (results.isReference()) {
                                debug.warning("Skipping reference result: {}", results.readReference());
                                continue;
                            }
                            SearchResultEntry entry = results.readEntry();
                            // Check if the attribute starts with "ou="
                            // Workaround for 3823, where (objectClass=*) is used
                            if (entry.getName().toString().toLowerCase().startsWith("ou=")) {
                                answer.add(entry.getName().rdn().getFirstAVA().getAttributeValue().toString());
                            }
                        } catch (SearchResultReferenceIOException e) {
                            debug.error("SMSLdapObject.subEntries: Reference should be handled already for dn {}", dn, e);
                        }
                    }
                } catch (LdapException e) {
                    debug.warning("SMSLdapObject.subEntries: Error in obtaining sub-entries: {}", dn, e);
                    throw new SMSException(e, "sms-entry-cannot-obtain");
                }
                break;
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (errorCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                    debug.message("SMSLdapObject.subEntries(): entry not present: {}", dn);
                    break;
                }
                if (!retryErrorCodes.contains(errorCode) || retry >= connNumRetry) {
                    debug.warning("SMSLdapObject.subEntries: Unable to search for sub-entries: {}", dn, e);
                    throw new SMSException(e, "sms-entry-cannot-search");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            }
        }
        debug.message("SMSLdapObject.subEntries: Successfully obtained sub-entries for {}", dn);
        return answer;
    }

    /**
     * Returns the sub-entry names. Returns a set of RDNs that are sub-entries.
     * The paramter <code>numOfEntries</code> identifies the number of entries
     * to return, if <code>0</code> returns all the entries.
     */
    public Set<String> schemaSubEntries(SSOToken token, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults,
            boolean ascendingOrder) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: schemaSubEntries search: " + dn);
        }

        // Construct the filter
        String[] objs = {filter, sidFilter};
        String sfilter = MessageFormat.format(getServiceIdSearchFilter(), objs);
        return getSubEntries(token, dn, sfilter, numOfEntries,
                sortResults, ascendingOrder);
    }

    public String toString() {
        return ("SMSLdapObject");
    }

    /**
     * Returns a LDAPConnection for the given principal
     */
    private static Connection getConnection(Principal p) throws SMSException {
        Connection conn = null;
        if (enableProxy) {
            try {
                conn = dlayer.getConnection(p);
            } catch (LdapException e) {
                debug.error("SMSLdapObject:getConnection() - Failed to get Connection", e);
            }
        } else {
            conn = smdlayer.getConnection();
        }
        if (conn == null) {
            debug.error("SMSLdapObject: Unable to get connection to LDAP server for the principal: {}", p);
            throw new SMSException(bundle.getString(IUMSConstants.SMS_SERVER_DOWN), "sms-SERVER_DOWN");
        }
        return conn;
    }

    /**
     * Returns LDAP entries that match the filter, using the start DN provided
     * in method
     */
    public Iterator<SMSDataEntry> search(SSOToken token, String startDN, String filter,
            int numOfEntries, int timeLimit, boolean sortResults,
            boolean ascendingOrder, Set<String> excludes)
            throws SSOException, SMSException {
        Connection conn = getConnection(adminPrincipal);
        ConnectionEntryReader results = searchObjectsEx(token, startDN, filter,
                numOfEntries, timeLimit, sortResults, ascendingOrder, conn);
        return new SearchResultIterator(results, excludes, conn);
    }

    private ConnectionEntryReader searchObjectsEx(SSOToken token,
            String startDN, String filter, int numOfEntries, int timeLimit,
            boolean sortResults, boolean ascendingOrder, Connection conn
    ) throws SSOException, SMSException {
        ConnectionEntryReader results = null;
        int retry = 0;
        SearchRequest request = getSearchRequest(startDN, filter, SearchScope.WHOLE_SUBTREE, numOfEntries, timeLimit,
                SMSEntry.ATTR_KEYVAL, SMSEntry.ATTR_XML_KEYVAL);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.search() retry: " + retry);
            }

            try {
                ConnectionEntryReader iterResults = conn.search(request);
                iterResults.hasNext();
                results = iterResults;
                break;
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (errorCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                    debug.warning("SMSLdapObject.search: size limit {} exceeded", numOfEntries);
                    break;
                }

                if (!retryErrorCodes.contains(errorCode) || retry >= connNumRetry) {
                    debug.warning("SMSLdapObject.search(): LDAP exception in search for filter match: {}", filter, e);
                    throw new SMSException(e, "sms-error-in-searching");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    //ignored
                }
            }
        }
        return results;
    }

    /**
     * Returns LDAP entries that match the filter, using the start DN provided
     * in method
     */
    public Set<String> search(SSOToken token, String startDN, String filter,
            int numOfEntries, int timeLimit, boolean sortResults,
            boolean ascendingOrder) throws SSOException, SMSException {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: search filter: " + filter);
        }

        Set<String> answer = new LinkedHashSet<>();
        // Convert LDAP results to DNs
        try (Connection conn = getConnection(token.getPrincipal())) {
            ConnectionEntryReader results = searchObjects(token, startDN, filter,
                    numOfEntries, timeLimit, sortResults, ascendingOrder, conn);

            while (results != null && results.hasNext()) {
                try {
                    if (results.isEntry()) {
                        answer.add(results.readEntry().getName().toString());
                    } else {
                        debug.warning("SMSLdapObject.search(): ignoring reference", results.readReference());
                    }
                } catch (SearchResultReferenceIOException e) {
                    debug.error("SMSLdapObject.search: reference should already be handled", e);
                }
            }
        } catch (LdapException e) {
            ResultCode errorCode = e.getResult().getResultCode();
            if (errorCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                debug.warning("SMSLdapObject.search: size limit {} exceeded", numOfEntries);
            } else {
                debug.warning("SMSLdapObject.search(): Error in searching for filter match: {}", filter, e);
                throw new SMSException(e, "sms-error-in-searching");
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject.search() returned successfully: "
                    + filter + "\n\tObjects: " + answer);
        }
        return answer;
    }


    private ConnectionEntryReader searchObjects(
            SSOToken token,
            String startDN,
            String filter,
            int numOfEntries,
            int timeLimit,
            boolean sortResults,
            boolean ascendingOrder,
            Connection conn) throws SSOException, SMSException {
        ConnectionEntryReader results = null;
        int retry = 0;
        SearchRequest request = getSearchRequest(startDN, filter, SearchScope.WHOLE_SUBTREE, numOfEntries, timeLimit);

        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.search() retry: " + retry);
            }

            try {
                results = conn.search(request);
                results.hasNext();
                return results;
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (!retryErrorCodes.contains(errorCode) || retry >= connNumRetry) {
                    debug.warning("SMSLdapObject.search(): LDAP exception in search for filter match: {}", filter, e);
                    throw new SMSException(e, "sms-error-in-searching");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    //ignored
                }
            }
        }
        return null;
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     *
     * @param token Admin token.
     * @param dn    The DN to check.
     * @return <code>true</code> if the entry exists, <code>false</code> otherwise.
     */
    @Override
    public boolean entryExists(SSOToken token, String dn) {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: checking if entry exists: " + dn);
        }
        dn = DN.valueOf(dn).toString().toLowerCase();
        // Check the caches
        if (SMSNotificationManager.isCacheEnabled()) {
            if (entriesPresent.contains(dn)) {
                if (debug.messageEnabled()) {
                    debug.message("SMSLdapObject: entry present in cache: " + dn);
                }
                return true;
            } else if (entriesNotPresent.contains(dn)) {
                if (debug.messageEnabled()) {
                    debug.message("SMSLdapObject: entry present in not-present-cache: " + dn);
                }
                return false;
            }
        }

        try {
            // Check if entry exists
            boolean entryExists = entryExists(dn);

            // Update the cache
            if (SMSNotificationManager.isCacheEnabled()) {
                initializeNotification();
                Set<String> cacheToUpdate = entryExists ? entriesPresent : entriesNotPresent;
                cacheToUpdate.add(dn);
                if (cacheToUpdate.size() > entriesPresentCacheSize) {
                    synchronized (cacheToUpdate) {
                        if (!cacheToUpdate.isEmpty()) {
                            cacheToUpdate.remove(cacheToUpdate.iterator().next());
                        }
                    }
                }
            }

            return entryExists;
        } catch (SMSException smse) {
            return false;
        }
    }

    /**
     * Checks if the provided DN exists.
     */
    private static boolean entryExists(String dn) throws SMSException {
        boolean entryExists = false;
        try (Connection conn = getConnection(adminPrincipal)) {
            // Use the Admin Principal to check if entry exists
            conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn, OU_ATTR));
            entryExists = true;
        } catch (EntryNotFoundException e) {
            debug.warning("SMSLdapObject:entryExists: {} does not exist", dn);
        } catch (LdapException e) {
            throw new SMSException("Unable to find entry with DN: " + dn, e, IUMSConstants.SMS_LDAP_OPERATION_FAILED);
        }
        return entryExists;
    }

    /**
     * Registration of Notification Callbacks
     */
    public void registerCallbackHandler(SMSObjectListener changeListener)
            throws SMSException {
        LDAPEventManager.addObjectChangeListener(changeListener);
    }

    public void deregisterCallbackHandler(String id) {
        LDAPEventManager.removeObjectChangeListener();
    }

    // Method to convert Map to LDAPAttributeSet
    private static Entry copyMapToEntry(Map<String, Set<String>> attrs) {
        Entry entry = new LinkedHashMapEntry();
        for (Map.Entry<String, Set<String>> attr : attrs.entrySet()) {
            entry.addAttribute(new LinkedAttribute(attr.getKey(), attr.getValue()));
        }
        return entry;
    }

    // Method to covert JNDI ModificationItems to LDAPModificationSet
    private static ModifyRequest copyModItemsToModifyRequest(DN dn, ModificationItem mods[]) throws SMSException {
        ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(dn);
        try {
            for (ModificationItem mod : mods) {
                Attribute attribute = mod.getAttribute();
                LinkedAttribute attr = new LinkedAttribute(attribute.getID());
                for (NamingEnumeration ne = attribute.getAll(); ne.hasMore(); ) {
                    attr.add(ne.next());
                }
                switch (mod.getModificationOp()) {
                    case DirContext.ADD_ATTRIBUTE:
                        modifyRequest.addModification(new Modification(ModificationType.ADD, attr));
                        break;
                    case DirContext.REPLACE_ATTRIBUTE:
                        modifyRequest.addModification(new Modification(ModificationType.REPLACE, attr));
                        break;
                    case DirContext.REMOVE_ATTRIBUTE:
                        modifyRequest.addModification(new Modification(ModificationType.DELETE, attr));
                        break;
                }
            }
        } catch (NamingException nne) {
            throw new SMSException(nne, "sms-cannot-copy-fromModItemToModSet");
        }
        return modifyRequest;
    }

    public void objectChanged(String dn, int type) {
        dn = DN.valueOf(dn).toString().toLowerCase();
        if (type == DELETE) {
            // Remove from entriesPresent Set
            entriesPresent.remove(dn);
        } else if (type == ADD) {
            // Remove from entriesNotPresent set
            entriesNotPresent.remove(dn);

        }
    }

    public void allObjectsChanged() {
        // Not clear why this class is implemeting the SMSObjectListener
        // interface.
        if (SMSEntry.debug.warningEnabled()) {
            SMSEntry.debug.warning(
                    "SMSLDAPObject: got notifications, all objects changed");
        }
        entriesPresent.clear();
        entriesNotPresent.clear();
    }

    /**
     * Returns the suborganization names. Returns a set of RDNs that are
     * suborganization name. The paramter <code>numOfEntries</code> identifies
     * the number of entries to return, if <code>0</code> returns all the
     * entries.
     */
    public Set<String> searchSubOrgNames(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject.searchSubOrgNames search: " + dn);
        }

        /*
         * Instead of constructing the filter in the framework(SMSEntry.java),
         * Construct the filter here in SMSLdapObject or the plugin
         * implementation to support JDBC or other data store.
         */
        String[] objs = {filter};

        String FILTER_PATTERN_ORG = "(&(objectclass="
                + SMSEntry.OC_REALM_SERVICE + ")(" + SMSEntry.ORGANIZATION_RDN
                + "={0}))";

        String sfilter = MessageFormat.format(FILTER_PATTERN_ORG, (Object[]) objs);
        return searchSubOrganizationNames(token, dn, sfilter, numOfEntries, sortResults, ascendingOrder, recursive);
    }

    private Set<String> searchSubOrganizationNames(
            SSOToken token,
            String dn,
            String filter,
            int numOfEntries,
            boolean sortResults,
            boolean ascendingOrder,
            boolean recursive
    ) throws SMSException, SSOException {
        SearchRequest request = getSearchRequest(dn, filter,
                recursive ? SearchScope.WHOLE_SUBTREE : SearchScope.SINGLE_LEVEL, numOfEntries, 0, sortResults,
                ascendingOrder, getOrgNamingAttribute(), O_ATTR);
        int retry = 0;
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message(
                        "SMSLdapObject.searchSubOrganizationNames() retry: " +
                                retry);
            }

            try (Connection conn = getConnection(token.getPrincipal())) {
                // Get the suborganization names
                ConnectionEntryReader iterResults = conn.search(request);
                iterResults.hasNext();
                return toDNStrings(iterResults, dn, SUBORG_CANNOT_OBTAIN);
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (!retryErrorCodes.contains(errorCode) || retry >= connNumRetry) {
                    if (errorCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                        debug.message("SMSLdapObject.searchSubOrganizationNames(): suborg not present: {}", dn);
                        break;
                    } else {
                        debug.warning("SMSLdapObject.searchSubOrganizationName(): Unable to search: {}", dn, e);
                        throw new SMSException(e, "sms-suborg-cannot-search");
                    }
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            }
        }
        return Collections.emptySet();
    }

    private SearchRequest getSearchRequest(String dn, String filter, SearchScope scope, int numOfEntries, int timeLimit,
            String... attributes) {
        return getSearchRequest(dn, filter, scope, numOfEntries, timeLimit, false, true, null, attributes);
    }

    private SearchRequest getSearchRequest(String dn, String filter, SearchScope scope, int numOfEntries, int timeLimit,
            boolean sortResults, boolean ascendingOrder, String sortAttribute, String... attributes) {
        SearchRequest request = LDAPRequests.newSearchRequest(dn, scope, filter, attributes)
                .setDereferenceAliasesPolicy(DereferenceAliasesPolicy.NEVER)
                .setTimeLimit(timeLimit);
        if (numOfEntries > 0) {
            request.setSizeLimit(numOfEntries);
        }
        if (sortResults) {
            SortKey sortKey = new SortKey(sortAttribute, !ascendingOrder);
            request.addControl(ServerSideSortRequestControl.newControl(true, sortKey));
        }
        return request;
    }

    /**
     * Returns the organization names. Returns a set of RDNs that are
     * organization name. The paramter <code>numOfEntries</code> identifies
     * the number of entries to return, if <code>0</code> returns all the
     * entries.
     */
    public Set<String> searchOrganizationNames(SSOToken token, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject:searchOrganizationNames search dn: "
                    + dn);
        }

        /*
         * Instead of constructing the filter in the framework(SMSEntry.java),
         * Construct the filter here in SMSLdapObject or the plugin
         * implementation to support JDBC or other data store. To return
         * organization names that match the given attribute name and values,
         * only exact matching is supported, and if more than one value is
         * provided the organization must have all these values for the
         * attribute. Basically an AND is performed for attribute values for
         * searching. The attributes can be under the service config as well
         * under the Realm/Organization directly. For eg.,
         * (|(&(objectclass=sunRealmService)(&
         * (|(sunxmlkeyvalue=SERVICE_NAME-ATTR_NAME=VALUE1)
         * (sunxmlkeyvalue=ATTR_NAME=VALUE1))
         * (|(sunxmlkeyvalue=SERVICE_NAME-ATTR_NAME=VALUE2)
         * (sunxmlkeyvalue=ATTR_NAME=VALUE2))(...))
         * (&(objectclass=sunServiceComponent)(&
         * (|(sunxmlkeyvalue=SERVICE_NAME-ATTR_NAME=VALUE1)
         * (sunxmlkeyvalue=ATTR_NAME=VALUE1))
         * (|(sunxmlkeyvalue=SERVICE_NAME-ATTR_NAME=VALUE2)
         * (sunxmlkeyvalue=ATTR_NAME=VALUE2))(...))
         * 
         */

        StringBuilder sb = new StringBuilder();
        sb.append("(&");
        for (Iterator itr = values.iterator(); itr.hasNext();) {
            String val = (String) itr.next();
            sb.append("(|(").append(SMSEntry.ATTR_XML_KEYVAL).append("=")
                    .append(serviceName).append("-").append(attrName).append(
                            "=").append(val).append(")");
            sb.append("(").append(SMSEntry.ATTR_XML_KEYVAL).append("=").append(
                    attrName).append("=").append(val).append("))");
        }
        sb.append(")");
        String filter = sb.toString();

        String FILTER_PATTERN_SEARCH_ORG = "{0}";
        String dataStore = SMSEntry.getDataStore(token);
        if ((dataStore != null) && !dataStore.equals(
            SMSEntry.DATASTORE_ACTIVE_DIR)
        ) {
           // Include the OCs only for sunDS, not Active Directory.
           //String FILTER_PATTERN_SEARCH_ORG = "(|(&(objectclass="
           FILTER_PATTERN_SEARCH_ORG = "(|(&(objectclass="
                + SMSEntry.OC_REALM_SERVICE + "){0})" + "(&(objectclass="
                + SMSEntry.OC_SERVICE_COMP + "){0}))";
        }

        String[] objs = { filter };
        String sfilter = MessageFormat.format(
                FILTER_PATTERN_SEARCH_ORG, (Object[]) objs);
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject:orgNames search filter: " + sfilter);
        }
        return getOrgNames(token, dn, sfilter, numOfEntries, sortResults, ascendingOrder);
    }
    
    public void shutdown() {
        if (!enableProxy && (smdlayer != null)) {
            smdlayer.shutdown();
        }
        // dlayer (from AMSDK) has dependecy on AMSDK
        // and cannot be shutdown by SMS.
        // Should be initialized by AMSDK
    }

    private Set<String> getOrgNames(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        ConnectionEntryReader results = null;
        int retry = 0;
        SearchRequest request = getSearchRequest(dn, filter, SearchScope.WHOLE_SUBTREE, numOfEntries, 0, sortResults,
                ascendingOrder, getOrgNamingAttribute(), O_ATTR);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.getOrgNames() retry: "+ retry);
            }

            try (Connection conn = getConnection(token.getPrincipal())) {
                // Get the organization names
                results = conn.search(request);
                results.hasNext();
                return toDNStrings(results, dn, ORG_CANNOT_OBTAIN);
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (!retryErrorCodes.contains(errorCode) || retry == connNumRetry) {
                    if (errorCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                        debug.message("SMSLdapObject.getOrgNames(): org not present: {}", dn);
                        break;
                    } else {
                        debug.warning("SMSLdapObject.getOrgNames: Unable to search for organization names: {}", dn, e);
                        throw new SMSException(e, "sms-org-cannot-search");
                    }
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            }
        }
        return Collections.emptySet();
    }

    private CharSequence getBundleString(String key, Object... appendables) {
        StringBuilder message = new StringBuilder(bundle.getString(key));
        for (Object toAppend : appendables) {
            message.append(toAppend);
        }
        return message.toString();
    }

    private Set<String> toDNStrings(ConnectionEntryReader results, String dn, String errorCode)
            throws SMSException {
        // Construct the results and return
        Set<String> answer = new LinkedHashSet<>();
        try {
            while (results != null && results.hasNext()) {
                try {
                    if (results.isReference()) {
                        debug.warning("SMSLdapObject.toDNStrings: Skipping reference result: {}", results.readReference());
                        continue;
                    }
                    answer.add(results.readEntry().getName().toString());
                } catch (SearchResultReferenceIOException e) {
                    debug.error("SMSLdapObject.toDNStrings: Reference should be handled already for {}", dn, e);
                }
            }
        } catch (LdapException e) {
            debug.warning("SMSLdapObject.toDNStrings: Error in obtaining suborg names: {}", dn, e);
            throw new SMSException(e, errorCode);
        }

        debug.message("SMSLdapObject.searchSubOrganizationName: Successfully obtained suborganization names for {}: {}",
                dn, answer);
        return answer;
    }
}
