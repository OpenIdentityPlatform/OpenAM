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
 * $Id: SMSLdapObject.java,v 1.27 2009/11/20 23:52:56 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011-2013] [ForgeRock AS]
 */
package com.sun.identity.sm.ldap;

import java.security.Principal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPCompareAttrNames;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPAddRequest;
import com.sun.identity.shared.ldap.LDAPModifyRequest;
import com.sun.identity.shared.ldap.LDAPSearchRequest;
import com.sun.identity.shared.ldap.LDAPDeleteRequest;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.DataLayer;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectDB;
import com.sun.identity.sm.SMSObjectListener;
import java.security.AccessController;
import java.util.Collections;
import java.util.LinkedHashSet;

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

    // LDAP specific & retry paramters
    static DataLayer dlayer;

    static SMDataLayer smdlayer;

    static int connNumRetry = 3;

    static int connRetryInterval = 1000;

    static HashSet retryErrorCodes = new HashSet();
    
    static int entriesPresentCacheSize = 1000;
    
    static boolean initializedNotification;

    static Set entriesPresent = Collections.synchronizedSet(
        new LinkedHashSet());

    static Set entriesNotPresent = Collections.synchronizedSet(
        new LinkedHashSet());

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
    public Map read(SSOToken token, String dn) throws SMSException,
            SSOException {
        if (dn == null || dn.length() == 0 ) {
            // This must not be possible return an exception.
            debug.error("SMSLdapObject: read():Null or Empty DN=" + dn);
            throw (new SMSException(new LDAPException(bundle
                .getString(IUMSConstants.SMS_INVALID_DN)
                    + dn, LDAPException.NO_SUCH_OBJECT), "sms-NO_SUCH_OBJECT"));
        }
        
       
        if (!DN.isDN(dn)) {
            debug.warning("SMSLdapObject: Invalid DN=" + dn);
            String[] args = {dn};
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-INVALID_DN", args);
        }

        // Check if entry does not exist
        if (SMSNotificationManager.isCacheEnabled() &&
            entriesNotPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject:read Entry not present: " + dn
                        + " (checked in cached)");
            }
            return (null);
        }

        LDAPEntry ldapEntry = null;
        int retry = 0;
        LDAPConnection conn = null;
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(
            getNormalizedName(token, dn), getAttributeNames());
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.read() retry: " + retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(token.getPrincipal());
                ldapEntry = conn.read(request);
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry == connNumRetry)
                ) {
                    if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                        // Add to not present Set
                        objectChanged(dn, DELETE);
                        if (debug.messageEnabled()) {
                            debug.message(
                                "SMSLdapObject.read: entry not present:" + dn);
                        }
                        break;
                    } else {
                        if (debug.warningEnabled()) {
                            debug.warning("SMSLdapObject.read: " +
                                "Error in accessing entry DN: " + dn, e);
                        }
                        throw new SMSException(e, "sms-entry-cannot-access");
                    }
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
                }
            }
        }

        if (ldapEntry != null) {
            LDAPAttributeSet attrSet = ldapEntry.getAttributeSet();
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.read(): reading entry: " + dn);
            }
            return SearchResultIterator.convertLDAPAttributeSetToMap(attrSet);
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
        create(token.getPrincipal(), getNormalizedName(token, dn), 
            attrs);
        // Update entryPresent cache
        objectChanged(dn, ADD);
    }

    /**
     * Create an entry in the directory using the principal name
     */
    private static void create(Principal p, String dn, Map attrs)
            throws SMSException, SSOException {
        int retry = 0;
        LDAPConnection conn = null;
        LDAPAttributeSet attrSet = copyMapToAttrSet(attrs);
        LDAPAddRequest request = LDAPRequestParser.parseAddRequest(
            new LDAPEntry(dn, attrSet));
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.create() retry: " + retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(p);
                conn.add(request);
                if (debug.messageEnabled()) {
                    debug.message(
                        "SMSLdapObject.create Successfully created entry: " +
                        dn);
                }
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if ((errorCode == LDAPException.ENTRY_ALREADY_EXISTS) &&
                    (retry > 0)) {
                    // During install time and other times,
                    // this error gets throws due to unknown issue. Issue: 
                    // Hence mask it.
                    debug.warning("SMSLdapObject.create() Entry " +
                        "Already Exists Error for DN" + dn);
                    break;
                }

                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry >= connNumRetry) 
                ) {
                    debug.error(
                        "SMSLdapObject.create() Error in creating entry: " +
                        dn + "\nBy Principal: " + p.getName(), e);
                    throw new SMSException(e, "sms-entry-cannot-create");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    //ignored
                }                        
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
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
        LDAPConnection conn = null;
        LDAPModificationSet modSet = copyModItemsToLDAPModSet(mods);
        LDAPModifyRequest request = LDAPRequestParser.parseModifyRequest(
            getNormalizedName(token, dn), modSet);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.modify() retry: " + retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(token.getPrincipal());
                conn.modify(request);
                if (debug.messageEnabled()) {
                    debug.message(
                        "SMSLdapObject.modify(): Successfully modified entry: "
                        + dn);
                }
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry == connNumRetry)
                ) {
                    debug.error(
                        "SMSLdapObject.modify(): Error in modifying entry: " +
                            dn + "\nBy Principal: " +
                            token.getPrincipal().getName(), e);
                    throw new SMSException(e, "sms-entry-cannot-modify");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
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
        Iterator se = subEntries(token, dn, "*", 0, false, false).iterator();
        while (se.hasNext()) {
            String entry = (String) se.next();
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: deleting sub-entry: " + entry);
            }
            delete(token, getNamingAttribute() + "=" + entry + "," + dn);
        }
        // Check if there are suborganizations, delete if present
        // The recursive 'false' here has the scope SCOPE_ONE
        // while searching for the suborgs.
        // Loop through the suborg at the first level and if there
        // is no next suborg, delete that.
        Set subOrgNames = searchSubOrgNames(
            token, dn, "*", 0, false, false, false);
        
        for (Iterator so = subOrgNames.iterator(); so.hasNext(); ) {
            String subOrg = (String) so.next();
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: deleting suborganization: "
                        + subOrg);
            }
            delete(token, getNormalizedName(token, subOrg));
        }

        // Get LDAP connection
        LDAPDeleteRequest request = LDAPRequestParser.parseDeleteRequest(
            getNormalizedName(token, dn));
        delete(token.getPrincipal(), request);
        // Update entriesPresent cache
        objectChanged(dn, DELETE);
    }

    private static void delete(Principal p, LDAPDeleteRequest request)
            throws SMSException {
        LDAPConnection conn = null;
        // Delete the entry
        try {
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("SMSLdapObject.delete() retry: " + retry);
                }
                try {
                    conn = getConnection(p);
                    conn.delete(request);
                    break;
                } catch (LDAPException e) {
                    int errorCode = e.getLDAPResultCode();
                    releaseConnection(conn, errorCode);
                    conn = null;
                    if (!retryErrorCodes.contains("" + e.getLDAPResultCode())
                            || retry == connNumRetry) {
                        throw e;
                    }
                    retry++;
                    try {
                        Thread.sleep(connRetryInterval);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (LDAPException le) {
            if (debug.warningEnabled()) {
                debug.warning("SMSLdapObject:delete() Unable to delete entry:"
                        + request.getDN(), le);
            }
            throw (new SMSException(le, "sms-entry-cannot-delete"));
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    /**
     * Returns the sub-entry names. Returns a set of RDNs that are sub-entries.
     * The paramter <code>numOfEntries</code> identifies the number of entries
     * to return, if <code>0</code> returns all the entries.
     */
    public Set subEntries(SSOToken token, String dn, String filter,
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
            String[] objs = { filter };
            sfilter = MessageFormat.format(getSearchFilter(),(Object[])objs);
        }
        Set answer = getSubEntries(token, dn, sfilter, numOfEntries,
                sortResults, ascendingOrder);
        return (answer);
    }

    private Set getSubEntries(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        LDAPSearchResults results = null;
        int retry = 0;
        LDAPConnection conn = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            getNormalizedName(token, dn), LDAPConnection.SCOPE_ONE, filter,
            OU_ATTR, false, 0, LDAPRequestParser.DEFAULT_DEREFERENCE,
            numOfEntries);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.subEntries() retry: " + retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(token.getPrincipal());
                // Get the sub entries                
                results = conn.search(request);
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                    if (debug.messageEnabled()) {
                        debug.message(
                            "SMSLdapObject.subEntries(): entry not present:" +
                            dn);
                    }
                    break;
                }
                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry >= connNumRetry)
                ) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "SMSLdapObject.subEntries: Unable to search for " +
                            "sub-entries: " + dn, e);
                    }
                    throw new SMSException(e, "sms-entry-cannot-search");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
                }
            }
        }
        // Construct the results and return
        Set answer = new OrderedSet();
        if (results != null) {
            // Check if the results have to sorted
            if (sortResults) {
                LDAPCompareAttrNames comparator = new LDAPCompareAttrNames(
                    getNamingAttribute(), ascendingOrder);
                results.sort(comparator);
            }
            while (results.hasMoreElements()) {
                try {
                    LDAPEntry entry = results.next();
                    // Check if the attribute starts with "ou="
                    // Workaround for 3823, where (objectClass=*) is used
                    String edn = entry.getDN();
                    if (!edn.toLowerCase().startsWith("ou=")) {
                        continue;
                    }
                    String temp = LDAPDN.explodeDN(entry.getDN(), true)[0]; 
                    answer.add(getDenormalizedName(token, temp));
                } catch (LDAPException e) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "SMSLdapObject.subEntries: Error in obtaining " +
                            "sub-entries: " + dn, e);
                    }
                    throw new SMSException(e, "sms-entry-cannot-obtain");
                }
            }
            if (debug.messageEnabled()) {
                debug.message(
                    "SMSLdapObject.subEntries: Successfully obtained " +
                    "sub-entries for : " + dn);
            }
        }
        return (answer);
    }

    /**
     * Returns the sub-entry names. Returns a set of RDNs that are sub-entries.
     * The paramter <code>numOfEntries</code> identifies the number of entries
     * to return, if <code>0</code> returns all the entries.
     */
    public Set schemaSubEntries(SSOToken token, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults,
            boolean ascendingOrder) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: schemaSubEntries search: " + dn);
        }
        
        // Construct the filter
        String[] objs = { filter, sidFilter };
        String sfilter = MessageFormat.format(
            getServiceIdSearchFilter(), (Object[])objs);
        Set answer = getSubEntries(token, dn, sfilter, numOfEntries,
                sortResults, ascendingOrder);
        return (answer);
    }

    public String toString() {
        return ("SMSLdapObject");
    }

    /**
     * Releases a LDAPConnection.
     */
    private static void releaseConnection(LDAPConnection conn) {
        if (conn != null) {
            if (enableProxy) {
                dlayer.releaseConnection(conn);
            } else {
                smdlayer.releaseConnection(conn);
            }
        }
    }

    /**
     * Releases a LDAPConnection.
     */
    private static void releaseConnection(LDAPConnection conn, int errorCode){

        if (conn != null) {
            if (enableProxy) {
              dlayer.releaseConnection(conn, errorCode);
            } else {
              smdlayer.releaseConnection(conn, errorCode);
            }
        }
    }

    /**
     * Returns a LDAPConnection for the given principal
     */
    private static LDAPConnection getConnection(Principal p)
            throws SMSException {
        LDAPConnection conn = null;
        if (enableProxy) {
            conn = dlayer.getConnection(p);
        } else {
            conn = smdlayer.getConnection();
        }
        if (conn == null) {
            debug.error("SMSLdapObject: Unable to get connection to LDAP "
                    + "server for the principal: " + p);
            throw (new SMSException(new LDAPException(bundle
                    .getString(IUMSConstants.SMS_SERVER_DOWN)),
                    "sms-SERVER_DOWN"));
        }
        return (conn);
    }

    /**
     * Returns LDAP entries that match the filter, using the start DN provided
     * in method
     */
    public Iterator search(SSOToken token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes)
        throws SSOException, SMSException {
        LDAPSearchResults results = searchObjectsEx(token, startDN, filter,
            numOfEntries, timeLimit, sortResults, ascendingOrder);
        return new SearchResultIterator(results, excludes);
    }

    private LDAPSearchResults searchObjectsEx(SSOToken token,
        String startDN, String filter, int numOfEntries, int timeLimit,
        boolean sortResults, boolean ascendingOrder
    ) throws SSOException, SMSException {
        LDAPSearchResults results = null;
        int retry = 0;
        String[] smsAttrs = { SMSEntry.ATTR_KEYVAL,
            SMSEntry.ATTR_XML_KEYVAL };
        LDAPConnection conn = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            getNormalizedName(token, startDN), LDAPConnection.SCOPE_SUB, filter,
            smsAttrs, false, timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            numOfEntries);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.search() retry: " + retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(adminPrincipal);                    
                results = conn.search(request);
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (errorCode == LDAPException.SIZE_LIMIT_EXCEEDED) {
                    if (debug.warningEnabled()) {
                        debug.warning("SMSLdapObject.search: size limit " +
                            numOfEntries + " exceeded");
                    }
                    break;
                }

                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry >= connNumRetry)
                ) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "SMSLdapObject.search(): LDAP exception in search "
                            + "for filter match: " + filter, e);
                    }
                    throw new SMSException(e, "sms-error-in-searching");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    //ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
                }
            }
        }
        return results;
    }

    /**
     * Returns LDAP entries that match the filter, using the start DN provided
     * in method
     */
    public Set search(SSOToken token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder) throws SSOException, SMSException {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: search filter: " + filter);
        }

        LDAPSearchResults results = searchObjects(token, startDN, filter,
            numOfEntries, timeLimit, sortResults, ascendingOrder);

        // Convert LDAP results to DNs
        Set answer = new OrderedSet();
        while ((results != null) && results.hasMoreElements()) {
            try {
                LDAPEntry entry = results.next();
                answer.add(entry.getDN());
            } catch (LDAPException ldape) {
                int errorCode = ldape.getLDAPResultCode();
                if (errorCode == LDAPException.SIZE_LIMIT_EXCEEDED) {
                    if (debug.warningEnabled()) {
                        debug.warning("SMSLdapObject.search: size limit " +
                            numOfEntries + " exceeded");
                    }
                    break;
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "SMSLdapObject.search(): Error in searching for " +
                            "filter match: " + filter, ldape);
                    }
                    throw new SMSException(ldape, "sms-error-in-searching");
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject.search() returned successfully: "
                    + filter + "\n\tObjects: " + answer);
        }
        return answer;
    }


    private LDAPSearchResults searchObjects(
        SSOToken token,
        String startDN,
        String filter,
        int numOfEntries,
        int timeLimit,
        boolean sortResults,
        boolean ascendingOrder
    ) throws SSOException, SMSException {
        LDAPSearchResults results = null;
        int retry = 0;
        LDAPConnection conn = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            getNormalizedName(token, startDN), LDAPConnection.SCOPE_SUB, filter,
            null, false, timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            numOfEntries);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.search() retry: " + retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(adminPrincipal);
                results = conn.search(request);
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry >= connNumRetry)
                ) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "SMSLdapObject.search(): LDAP exception in search "
                            + "for filter match: " + filter, e);
                    }
                    throw new SMSException(e, "sms-error-in-searching");
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    //ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
                }
            }
        }
        return results;
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(SSOToken token, String dn) {
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject: checking if entry exists: " + dn);
        }
        dn = (new DN(dn)).toRFCString().toLowerCase();
        // Check the caches
        if (SMSNotificationManager.isCacheEnabled() &&
            entriesPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: entry present in cache: " + dn);
            }
            return (true);
        } else if (SMSNotificationManager.isCacheEnabled() &&
            entriesNotPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: entry present in "
                        + "not-present-cache: " + dn);
            }
            return (false);
        }

        // Check if entry exisits
        boolean entryExists = entryExists(getNormalizedName(token, dn));

        // Update the cache
        if (entryExists && SMSNotificationManager.isCacheEnabled()) {
            initializeNotification();
            entriesPresent.add(dn);
            if (entriesPresent.size() > entriesPresentCacheSize) {
                synchronized (entriesPresent) {
                    Iterator items = entriesPresent.iterator();
                    if (items.hasNext()) {
                        items.next();
                        items.remove();
                    }
                }
            }
        } else if (SMSNotificationManager.isCacheEnabled()) {
            initializeNotification();
            entriesNotPresent.add(dn);
            if (entriesNotPresent.size() > entriesPresentCacheSize) {
                synchronized (entriesNotPresent) {
                    Iterator items = entriesNotPresent.iterator();
                    if (items.hasNext()) {
                        items.next();
                        items.remove();
                    }
                }
            }
        }
        return (entryExists);
    }

    /**
     * Checks if the provided DN exists.
     */
    private static boolean entryExists(String dn) {
        boolean entryExists = false;
        LDAPConnection conn = null;
        try {
            // Use the Admin Principal to check if entry exists
            LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
                OU_ATTR);
            conn = getConnection(adminPrincipal);       
            conn.read(request);
            entryExists = true;
        } catch (LDAPException e) {
            releaseConnection(conn, e.getLDAPResultCode());
            conn = null;
            if (debug.warningEnabled()) {
                debug.warning("SMSLdapObject:entryExists: " + dn
                        + "does not exist");
            }
        } catch (SMSException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("SMSLdapObject: SMSException while "
                        + " checking for entry: " + dn, ssoe);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
        return (entryExists);
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
    private static LDAPAttributeSet copyMapToAttrSet(Map attrs) {
        LDAPAttribute[] ldapAttrs = new LDAPAttribute[attrs.size()];
        Iterator items = attrs.keySet().iterator();
        for (int i = 0; items.hasNext(); i++) {
            String attrName = (String) items.next();
            Set attrValues = (Set) attrs.get(attrName);
            ldapAttrs[i] = new LDAPAttribute(attrName, (String[]) attrValues
                    .toArray(new String[attrValues.size()]));
        }
        return (new LDAPAttributeSet(ldapAttrs));
    }

    // Method to covert JNDI ModificationItems to LDAPModificationSet
    private static LDAPModificationSet copyModItemsToLDAPModSet(
            ModificationItem mods[]) throws SMSException {
        LDAPModificationSet modSet = new LDAPModificationSet();
        try {
            for (int i = 0; i < mods.length; i++) {
                Attribute attribute = mods[i].getAttribute();
                LDAPAttribute attr = new LDAPAttribute(attribute.getID());
                for (NamingEnumeration ne = attribute.getAll(); ne.hasMore();) {
                    attr.addValue((String) ne.next());
                }
                switch (mods[i].getModificationOp()) {
                case DirContext.ADD_ATTRIBUTE:
                    modSet.add(LDAPModification.ADD, attr);
                    break;
                case DirContext.REPLACE_ATTRIBUTE:
                    modSet.add(LDAPModification.REPLACE, attr);
                    break;
                case DirContext.REMOVE_ATTRIBUTE:
                    modSet.add(LDAPModification.DELETE, attr);
                    break;
                }
            }
        } catch (NamingException nne) {
            throw (new SMSException(nne, 
                    "sms-cannot-copy-fromModItemToModSet"));
        }
        return (modSet);
    }

    public void objectChanged(String dn, int type) {
        dn = (new DN(dn)).toRFCString().toLowerCase();
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
    public Set searchSubOrgNames(SSOToken token, String dn, String filter,
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
        String[] objs = { filter };

        String FILTER_PATTERN_ORG = "(&(objectclass="
                + SMSEntry.OC_REALM_SERVICE + ")(" + SMSEntry.ORGANIZATION_RDN
                + "={0}))";

        String sfilter = MessageFormat.format(
            FILTER_PATTERN_ORG, (Object[])objs);
        Set answer = searchSubOrganizationNames(token, dn, sfilter,
                numOfEntries, sortResults, ascendingOrder, recursive);
        return (answer);
    }

    private Set searchSubOrganizationNames(
        SSOToken token, 
        String dn,
        String filter,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder, 
        boolean recursive
    ) throws SMSException, SSOException {
        LDAPSearchResults results = null;
        int scope = (recursive) ? LDAPConnection.SCOPE_SUB :
            LDAPConnection.SCOPE_ONE;
        int retry = 0;
        LDAPConnection conn = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            getNormalizedName(token, dn), scope, filter, O_ATTR, false, 0,
            LDAPRequestParser.DEFAULT_DEREFERENCE, numOfEntries);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message(
                    "SMSLdapObject.searchSubOrganizationNames() retry: " +
                    retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(token.getPrincipal());
                // Get the suborganization names
                results = conn.search(request);                
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry >= connNumRetry)
                ) {
                    if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                        if (debug.messageEnabled()) {
                            debug.message(
                                "SMSLdapObject.searchSubOrganizationNames(): " +
                                "suborg not present:" + dn);
                        }
                        break;
                    } else {
                        if (debug.warningEnabled()) {
                            debug.warning(
                                "SMSLdapObject.searchSubOrganizationName(): " + 
                                "Unable to search for suborganization names: "
                                + dn, e);
                        }
                        throw new SMSException(e, "sms-suborg-cannot-search");
                    }
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
                }
            }
        }
        // Check if the results have to be sorted
        if ((results != null) && (sortResults)) {
            LDAPCompareAttrNames comparator = new LDAPCompareAttrNames(
                getOrgNamingAttribute(), ascendingOrder);
            results.sort(comparator);
        }
        // Construct the results and return
        Set answer = new OrderedSet();
        while ((results != null) && results.hasMoreElements()) {
            try {
                LDAPEntry entry = results.next();
                String rdn = (entry.getDN()).toString();
                answer.add(rdn);
            } catch (LDAPException e) {
                if (debug.warningEnabled()) {
                    debug.warning(
                        "SMSLdapObject.searchSubOrganizationName: " +
                        "Error in obtaining suborganization names: " + dn, e);
                }
                throw new SMSException(e, "sms-suborg-cannot-obtain");
            }

        }
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject.searchSubOrganizationName: " + 
                "Successfully obtained suborganization names for : " + dn);
            debug.message("SMSLdapObject.searchSubOrganizationName: " +
                "Successfully obtained suborganization names  : " +
                answer.toString());
        }
        return (answer);
    }

    /**
     * Returns the organization names. Returns a set of RDNs that are
     * organization name. The paramter <code>numOfEntries</code> identifies
     * the number of entries to return, if <code>0</code> returns all the
     * entries.
     */
    public Set searchOrganizationNames(SSOToken token, String dn,
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
            FILTER_PATTERN_SEARCH_ORG, (Object[])objs);
        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject:orgNames search filter: " + sfilter);
        }
        Set answer = getOrgNames(token, dn, sfilter, numOfEntries, sortResults,
                ascendingOrder);
        return (answer);
    }
    
    public void shutdown() {
        if (!enableProxy && (smdlayer != null)) {
            smdlayer.shutdown();
        }
        // dlayer (from AMSDK) has dependecy on AMSDK
        // and cannot be shutdown by SMS.
        // Should be initialized by AMSDK
    }

    private Set getOrgNames(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        LDAPSearchResults results = null;
        int retry = 0;

        LDAPConnection conn = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            getNormalizedName(token, dn), LDAPConnection.SCOPE_SUB, filter,
            O_ATTR, false, 0, LDAPRequestParser.DEFAULT_DEREFERENCE,
            numOfEntries);
        while (retry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject.getOrgNames() retry: "+ retry);
            }

            int errorCode = 0;
            try {
                conn = getConnection(token.getPrincipal());                     
                // Get the organization names
                results = conn.search(request);
                break;
            } catch (LDAPException e) {
                errorCode = e.getLDAPResultCode();
                releaseConnection(conn, errorCode);
                conn = null;
                if (!retryErrorCodes.contains(Integer.toString(errorCode)) ||
                    (retry == connNumRetry)
                ) {
                    if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                        if (debug.messageEnabled()) {
                            debug.message(
                                "SMSLdapObject.getOrgNames(): org not present:" 
                                + dn);
                        }
                        break;
                    } else {
                        if (debug.warningEnabled()) {
                            debug.warning(
                                "SMSLdapObject.getOrgNames: " +
                                "Unable to search for organization names: " +
                                dn, e);
                        }
                        throw new SMSException(e, "sms-org-cannot-search");
                    }
                }
                retry++;
                try {
                    Thread.sleep(connRetryInterval);
                } catch (InterruptedException ex) {
                    // ignored
                }
            } finally {
                if (conn != null) {
                    releaseConnection(conn);
                }
            }
        }
        // Check if the results have to be sorted
        if ((results != null) && (sortResults)) {
            LDAPCompareAttrNames comparator = new LDAPCompareAttrNames(
                getOrgNamingAttribute(), ascendingOrder);
            results.sort(comparator);
        }
        // Construct the results and return
        Set answer = new OrderedSet();

        while ((results != null) && results.hasMoreElements()) {
            try {
                LDAPEntry entry = results.next();
                String rdn = (entry.getDN()).toString();
                answer.add(rdn);
            } catch (LDAPException e) {
                if (debug.warningEnabled()) {
                    debug.warning("SMSLdapObject.getOrgNames: " +
                        "Error in obtaining organization names: " + dn, e);
                }
                throw new SMSException(e, "sms-org-cannot-obtain");
            }
        }

        if (debug.messageEnabled()) {
            debug.message("SMSLdapObject.getOrgNames(): " +
                "Successfully obtained organization names for : " + dn);
            debug.message("SMSLdapObject.getOrgNames(): " + 
                "Successfully obtained organization names  : " +
                    answer.toString());
        }
        return (answer);
    }
    
    private String getDenormalizedName(SSOToken token, String name) {
        if (name.indexOf("^") >= 0) {
            String dataStore = SMSEntry.getDataStore(token);
            if ((dataStore != null) && 
                dataStore.equals(SMSEntry.DATASTORE_ACTIVE_DIR)
            ) {
                name = name.replaceAll("_", "=");
            }
        }
        return name;
    }
    
    private String getNormalizedName(SSOToken token, String dn) {
        if (dn.indexOf("^") >= 0) {
            String dataStore = SMSEntry.getDataStore(token);
            /*
             * If the datastore is Active Directory, convert
             * ou=dc=samples^dc=com^^AgentLogging to
             * ou=dc_samples^dc_com^^AgentLogging.
             * Otherwise BAD_NAME error LDAPException code 34 will occur.
             **/
            if ((dataStore != null) && 
                dataStore.equals(SMSEntry.DATASTORE_ACTIVE_DIR)
            ) {
                String[] dns = LDAPDN.explodeDN(dn, false);
                StringBuilder buff = new StringBuilder();

                String s = dns[0];
                int idx = s.indexOf('=');
                String naming = s.substring(0, idx+1);
                String value = s.substring(idx+1).replaceAll("=", "_");
                buff.append(naming).append(value);

                for (int i = 1; i < dns.length; i++) {
                    s = dns[i];
                    idx = s.indexOf('=');
                    naming = s.substring(0, idx+1);
                    value = s.substring(idx+1).replaceAll("=", "_");
                    buff.append(",").append(naming).append(value);
                }
                dn = buff.toString();
            }
        }
        return dn;
    }
}
