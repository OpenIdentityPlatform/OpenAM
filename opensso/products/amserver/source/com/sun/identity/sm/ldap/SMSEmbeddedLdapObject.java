/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: SMSEmbeddedLdapObject.java,v 1.3 2009/10/28 04:24:27 hengming Exp $
*/
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.ldap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import com.sun.identity.shared.ldap.util.DN;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectDB;
import com.sun.identity.sm.SMSObjectListener;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.ModificationType;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchScope;
import org.opends.server.types.SearchResultEntry;

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
public class SMSEmbeddedLdapObject extends SMSObjectDB
    implements SMSObjectListener {

    static int entriesPresentCacheSize = 1000;
    static boolean initializedNotification;

    static Set entriesPresent = Collections.synchronizedSet(
        new LinkedHashSet());

    static Set entriesNotPresent = Collections.synchronizedSet(
        new LinkedHashSet());

    // Other parameters
    static ResourceBundle bundle;

    static boolean initialized = false;
    static Debug debug;
    static LinkedHashSet OU_ATTR;
    static LinkedHashSet O_ATTR;
    static InternalClientConnection icConn;
    static LinkedHashSet smsAttributes;
    static final String SMS_EMBEDDED_LDAP_OBJECT_SEARCH_LIMIT = 
        "com.sun.identity.sm.sms_embedded_ldap_object_search_limit";

    /**
     * Public constructor for SMSEmbeddedLdapObject
     */
    public SMSEmbeddedLdapObject() throws SMSException {
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
        debug = Debug.getInstance("amSMSEmbeddedLdap");
        AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();
        bundle = amCache.getResBundle(IUMSConstants.UMS_BUNDLE_NAME,
            java.util.Locale.ENGLISH);
        OU_ATTR = new LinkedHashSet(1);
        OU_ATTR.add(getNamingAttribute());
        O_ATTR = new LinkedHashSet(1);
        O_ATTR.add(getOrgNamingAttribute());

        icConn = InternalClientConnection.getRootConnection();
        try {
            String serviceDN = SMSEntry.SERVICES_RDN + SMSEntry.COMMA +
                getRootSuffix();
            if (!entryExists(serviceDN)) {
                Map attrs = new HashMap();
                Set attrValues = new HashSet();
                attrValues.add(SMSEntry.OC_TOP);
                attrValues.add(SMSEntry.OC_ORG_UNIT);
                attrs.put(SMSEntry.ATTR_OBJECTCLASS, attrValues);
                create(serviceDN, attrs);
            }
        } catch (Exception e) {
            // Unable to initialize (trouble!!)
            debug.error("SMSEmbeddedLdapObject.initialize: " +
                "Unable to initalize(exception):", e);
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.CONFIG_MGR_ERROR, null));
        }

        String[] smsAttrs = getAttributeNames();
        smsAttributes = new LinkedHashSet(smsAttrs.length);
        for(int i=0; i<smsAttrs.length; i++) {
            smsAttributes.add(smsAttrs[i]);
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
            debug.error("SMSEmbeddedLdapObject.read: Null or Empty DN=" + dn);
            throw new SMSException("", "sms-NO_SUCH_OBJECT");
        }
        
       
        if (!DN.isDN(dn)) {
            debug.warning("SMSEmbeddedLdapObject: Invalid DN=" + dn);
            String[] args = {dn};
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-INVALID_DN", args);
        }

        // Check if entry does not exist
        if (SMSNotificationManager.isCacheEnabled() &&
            entriesNotPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject:read Entry not present: "+
                    dn + " (checked in cached)");
            }
            return (null);
        }

        try {
            InternalSearchOperation iso = icConn.processSearch(dn,
                SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, "(|(objectclass=*)(objectclass=ldapsubentry))",
                smsAttributes);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList searchResult = iso.getSearchEntries();
                if (!searchResult.isEmpty()) {
                    SearchResultEntry entry =
                        (SearchResultEntry)searchResult.get(0);
                    List attributes = entry.getAttributes();

                    return EmbeddedSearchResultIterator.
                        convertLDAPAttributeSetToMap(attributes);
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                // Add to not present Set
                objectChanged(dn, DELETE);
                if (debug.messageEnabled()) {
                    debug.message("SMSEmbeddedLdapObject.read: " +
                        "entry not present:"+ dn);
                }
                return null;
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("SMSEmbeddedLdapObject.read: " +
                       "Error in accessing entry DN: " + dn + 
                       ", error code = " + resultCode);
                }
                throw new SMSException("", "sms-entry-cannot-access");
            }
        } catch (DirectoryException dex) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject.read: " +
                   "Error in accessing entry DN: " + dn, dex);
            }
            throw new SMSException(dex, "sms-entry-cannot-access");
        }
    }

    /**
     * Create an entry in the directory
     */
    public void create(SSOToken token, String dn, Map attrs)
        throws SMSException, SSOException {

        create(dn, attrs);
        // Update entryPresent cache
        objectChanged(dn, ADD);
    }

    /**
     * Create an entry in the directory using the principal name
     */
    private static void create(String dn, Map attrs)
            throws SMSException, SSOException {

        List attrList = copyMapToAttrList(attrs);
        AddOperation ao = icConn.processAdd(dn, attrList);
        ResultCode resultCode = ao.getResultCode();
        if (resultCode == ResultCode.SUCCESS) {
            if (debug.messageEnabled()) {
                debug.message(
                    "SMSEmbeddedLdapObject.create: Successfully created" +
                    " entry: " + dn);
            }
        } else if (resultCode == ResultCode.ENTRY_ALREADY_EXISTS) {
                    // During install time and other times,
                    // this error gets throws due to unknown issue. Issue: 
                    // Hence mask it.
                    debug.warning("SMSEmbeddedLdapObject.create: Entry " +
                        "Already Exists Error for DN" + dn);
        } else {
            debug.error("SMSEmbeddedLdapObject.create: Error creating entry: "+
                dn + ", error code = " + resultCode);
            throw new SMSException("", "sms-entry-cannot-create");
        }
    }

    /**
     * Save the entry using the token provided. The principal provided will be
     * used to get the proxy connection.
     */
    public void modify(SSOToken token, String dn, ModificationItem mods[])
        throws SMSException, SSOException {

        List modList = copyModItemsToLDAPModList(mods);
        ModifyOperation mo = icConn.processModify(dn, modList);
        ResultCode resultCode = mo.getResultCode();
        if (resultCode == ResultCode.SUCCESS) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject.modify: Successfully " +
                    "modified entry: " + dn);
            }
        } else {
            debug.error("SMSEmbeddedLdapObject.modify: Error modifying entry "+
                dn + " by Principal: " + token.getPrincipal().getName() +
                ", error code = " + resultCode );
                throw new SMSException("", "sms-entry-cannot-modify");
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
                debug.message("SMSEmbeddedLdapObject: deleting sub-entry: " +
                    entry);
            }
            delete(token, getNamingAttribute() + "=" + entry + "," + dn);
        }
        // Check if there are suborganizations, delete if present
        // The recursive 'false' here has the scope SCOPE_ONE
        // while searching for the suborgs.
        // Loop through the suborg at the first level and if there
        // is no next suborg, delete that.
        Set subOrgNames = searchSubOrgNames(token, dn, "*", 0, false, false,
            false);
        
        for (Iterator so = subOrgNames.iterator(); so.hasNext(); ) {
            String subOrg = (String) so.next();
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject: deleting " +
                    "suborganization: " + subOrg);
            }
            delete(token, subOrg);
        }

        DeleteOperation dop = icConn.processDelete(dn);
        ResultCode resultCode = dop.getResultCode();
        if (resultCode != ResultCode.SUCCESS) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject.delete: " +
                    "Unable to delete entry:" + dn);
            }
            throw (new SMSException("", "sms-entry-cannot-delete"));
        }
        objectChanged(dn, DELETE);
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
            debug.message("SMSEmbeddedLdapObject: SubEntries search: " + dn);
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

        // sorting is not implemented
        // Get the sub entries
        try {
            InternalSearchOperation iso = icConn.processSearch(dn,
                SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                numOfEntries, 0, false, filter, OU_ATTR, null, null);

            ResultCode resultCode = iso.getResultCode();
            if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                if (debug.messageEnabled()) {
                    debug.message("SMSEmbeddedLdapObject.getSubEntries(): " +
                        "entry not present:" + dn);
                }
            } else if (resultCode == ResultCode.SIZE_LIMIT_EXCEEDED) {
                if (debug.messageEnabled()) {
                    debug.message("SMSEmbeddedLdapObject.getSubEntries: " +
                        "size limit " + numOfEntries + " exceeded for " +
                        "sub-entries: " + dn);
                }
            } else if (resultCode != ResultCode.SUCCESS) {
                if (debug.warningEnabled()) {
                    debug.warning("SMSEmbeddedLdapObject.getSubEntries: " +
                        "Unable to search for " + "sub-entries: " + dn);
                }
                throw new SMSException("", "sms-entry-cannot-search");
            }

            // Construct the results and return
            Set answer = new OrderedSet();
            LinkedList searchResult = iso.getSearchEntries();
            for(Iterator iter = searchResult.iterator(); iter.hasNext(); ) {
                SearchResultEntry entry = (SearchResultEntry)iter.next();
                String edn = entry.getDN().toString();
                if (!edn.toLowerCase().startsWith("ou=")) {
                    continue;
                }
                String rdn = entry.getDN().getRDN().getAttributeValue(0)
                    .toString();
                answer.add(rdn);
            }
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject.getSubEntries: " +
                    "Successfully obtained sub-entries for : " + dn);
            }
            return (answer);
        } catch (DirectoryException dex) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject.getSubEntries: " +
                    "Unable to search for " + "sub-entries: " + dn, dex);
            }
            throw new SMSException(dex, "sms-entry-cannot-search");
        }
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
            debug.message("SMSEmbeddedLdapObject: schemaSubEntries search: " +
                dn);
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
        return ("SMSEmbeddedLdapObject");
    }

    public Iterator search(SSOToken token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults, 
        boolean ascendingOrder, Set excludes)
        throws SSOException, SMSException {

        InternalSearchOperation iso = searchObjects(startDN, filter,
            SearchScope.WHOLE_SUBTREE, numOfEntries, sortResults,
            ascendingOrder);

        ResultCode resultCode = iso.getResultCode();
        if (resultCode == ResultCode.SIZE_LIMIT_EXCEEDED) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject.search:" +
                    " size limit exceeded. numOfEntries = " + numOfEntries);
            }
        } else if (resultCode != ResultCode.SUCCESS) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject.searchEx: Unable to " +
                    "search. startDN = " + startDN + ", filter = " +
                    filter + ", resultCode = " + resultCode);
                throw new SMSException("", "sms-error-in-searching");
            }
        }

        Map answer = new HashMap();
        LinkedList searchResult = iso.getSearchEntries();

        return new EmbeddedSearchResultIterator(searchResult, excludes);
    }

    /**
     * Returns LDAP entries that match the filter, using the start DN provided
     * in method
     */
    public Set search(SSOToken token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder) throws SSOException, SMSException {
        if (debug.messageEnabled()) {
            debug.message("SMSEmbeddedLdapObject.search: startDN = " + 
                startDN + ", filter: " + filter);
        }

        InternalSearchOperation iso = searchObjects(startDN, filter,
            SearchScope.WHOLE_SUBTREE, numOfEntries, sortResults,
            ascendingOrder);

        ResultCode resultCode = iso.getResultCode();
        if (resultCode == ResultCode.SIZE_LIMIT_EXCEEDED) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject.search:" +
                    " size limit exceeded. numOfEntries = " + numOfEntries);
            }
        } else if (resultCode != ResultCode.SUCCESS) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject.search: Unable to " +
                    "search. startDN = " + startDN + ", filter = " +
                    filter + ", resultCode = " + resultCode);
            }
            throw new SMSException("", "sms-error-in-searching");
        }

        Set answer = new OrderedSet();
        LinkedList searchResult = iso.getSearchEntries();
        for(Iterator iter = searchResult.iterator(); iter.hasNext(); ) {
            SearchResultEntry entry = (SearchResultEntry)iter.next();
            String dn = entry.getDN().toString();
            answer.add(dn);
        }

        if (debug.messageEnabled()) {
            debug.message("SMSEmbeddedLdapObject.search: returned " +
                "successfully: " + filter + "\n\tObjects: " + answer);
        }
        return answer;
    }


    private InternalSearchOperation searchObjects(String startDN,
        String filter, SearchScope scope, int numOfEntries,
        boolean sortResults, boolean ascendingOrder)
        throws SSOException, SMSException {

        // sorting is not implemented
        try {
            // Get the sub entries
            InternalSearchOperation iso = icConn.processSearch(startDN, scope, 
                DereferencePolicy.NEVER_DEREF_ALIASES, numOfEntries, 0, false,
                filter, null, null, null);

            return iso;
        } catch (DirectoryException dex) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject.searchObjects: " +
                    "Unable to " + "search. startDN = " + startDN +
                    ", filter = " + filter, dex);
            }
            throw new SMSException(dex, "sms-error-in-searching");
        }
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(SSOToken token, String dn) {
        if (debug.messageEnabled()) {
            debug.message("SMSEmbeddedLdapObject.entryExists: checking if " +
                "entry exists: " + dn);
        }
        dn = (new DN(dn)).toRFCString().toLowerCase();
        // Check the caches
        if (SMSNotificationManager.isCacheEnabled() &&
            entriesPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject.entryExists: " +
                    "entry present in cache: " + dn);
            }
            return (true);
        } else if (SMSNotificationManager.isCacheEnabled() &&
            entriesNotPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject.entryExists: " + 
                    "entry present in not-present-cache: " + dn);
            }
            return (false);
        }

        // Check if entry exisits
        boolean entryExists = entryExists(dn);

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
        try {
            InternalSearchOperation iso = icConn.processSearch(dn,
                SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, "(|(objectclass=*)(objectclass=ldapsubentry))",
                null);

            ResultCode resultCode = iso.getResultCode();
            if (resultCode == ResultCode.SUCCESS) {
                return true;
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("SMSEmbeddedLdapObject:entryExists: " + dn +
                        "does not exist. resultCode = " + resultCode);
                }
                return false;
            }
        } catch (DirectoryException dex) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject:entryExists: " + dn +
                    "does not exist.", dex);
            }
            return false;
        }
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
    private static List copyMapToAttrList(Map attrs) {
        if ((attrs == null) || (attrs.isEmpty())) {
            return null;
        }
        List attrList = new ArrayList(attrs.size());
        for(Iterator iter = attrs.keySet().iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            Set values = (Set)attrs.get(attrName);
            if ((values != null) && (!values.isEmpty())) {
                List valueList = new ArrayList();
                valueList.addAll(values);
                attrList.add(new LDAPAttribute(attrName, valueList));
            }
        }
        return attrList;
    }

    // Method to covert JNDI ModificationItems to LDAPModificationSet
    private static List copyModItemsToLDAPModList(
        ModificationItem mods[]) throws SMSException {

        if ((mods == null) || (mods.length == 0)) {
            return null;
        }
        List modList = new ArrayList(mods.length);
        try {
            for (int i = 0; i < mods.length; i++) {
                javax.naming.directory.Attribute dAttr =
                    mods[i].getAttribute();
                String attrName = dAttr.getID();
                List values = new ArrayList();
                for(NamingEnumeration ne = dAttr.getAll(); ne.hasMore();) {
                    values.add((String) ne.next());
                }
                ModificationType modType = null;
                switch (mods[i].getModificationOp()) {
                    case DirContext.ADD_ATTRIBUTE:
                        modType = ModificationType.ADD;
                        break;
                    case DirContext.REPLACE_ATTRIBUTE:
                        modType = ModificationType.REPLACE;
                        break;
                    case DirContext.REMOVE_ATTRIBUTE:
                        modType = ModificationType.DELETE;
                        break;
                }
                if (modType != null) {
                    modList.add(new LDAPModification(modType,
                        new LDAPAttribute(attrName, values)));
                }
            }
        } catch (NamingException nne) {
            throw (new SMSException(nne, 
                "sms-cannot-copy-fromModItemToModSet"));
        }
        return (modList);
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
            SMSEntry.debug.warning("SMSEmbeddedLDAPObject.allObjectsChanged:" +
                " got notifications, all objects changed");
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
            debug.message("SMSEmbeddedLdapObject.searchSubOrgNames: search: " +
            dn);
        }

        /*
         * Instead of constructing the filter in the framework(SMSEntry.java),
         * Construct the filter here in SMSEmbeddedLdapObject or the plugin
         * implementation to support JDBC or other data store.
         */
        String[] objs = { filter };

        String FILTER_PATTERN_ORG = "(&(objectclass=" +
            SMSEntry.OC_REALM_SERVICE + ")(" + SMSEntry.ORGANIZATION_RDN +
            "={0}))";

        String sfilter = MessageFormat.format(FILTER_PATTERN_ORG,
            (Object[])objs);
        Set answer = searchSubOrganizationNames(dn, sfilter, numOfEntries,
            sortResults, ascendingOrder, recursive);
        return (answer);
    }

    private Set searchSubOrganizationNames(String dn, String filter,
        int numOfEntries, boolean sortResults, boolean ascendingOrder,
        boolean recursive) throws SMSException, SSOException {

        SearchScope scope = (recursive) ? SearchScope.WHOLE_SUBTREE :
            SearchScope.SINGLE_LEVEL;
        InternalSearchOperation iso = searchObjects(dn, filter,
            scope, numOfEntries, sortResults, ascendingOrder);

        ResultCode resultCode = iso.getResultCode();
        if (resultCode == ResultCode.NO_SUCH_OBJECT) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject." +
                    "searchSubOrganizationNames: suborg not present:" + dn);
            }
        } else if (resultCode == ResultCode.SIZE_LIMIT_EXCEEDED) {
            if (debug.messageEnabled()) {
                debug.message("SMSEmbeddedLdapObject." +
                    "searchSubOrganizationNames: size limit exceeded. " +
                    "numOfEntries = " + numOfEntries + ", dn = " + dn);
            }
        } else if (resultCode != ResultCode.SUCCESS) {
            if (debug.warningEnabled()) {
                debug.warning("SMSEmbeddedLdapObject." +
                    "searchSubOrganizationNames: Unable to search. dn = "+
                    dn + ", filter = " + filter + ", resultCode = " +
                    resultCode);
            }
            throw new SMSException("", "sms-suborg-cannot-search");
        }

        Set answer = new OrderedSet();
        LinkedList searchResult = iso.getSearchEntries();
        for(Iterator iter = searchResult.iterator(); iter.hasNext(); ) {
            SearchResultEntry entry = (SearchResultEntry)iter.next();
            String edn = entry.getDN().toString();
            answer.add(edn);
        }

        if (debug.messageEnabled()) {
            debug.message("SMSEmbeddedLdapObject.searchSubOrganizationName: " +
                "Successfully obtained suborganization names for : " + dn);
            debug.message("SMSEmbeddedLdapObject.searchSubOrganizationName: " +
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
            debug.message("SMSEmbeddedLdapObject.searchOrganizationNames" +
                " search dn: " + dn);
        }

        /*
         * Instead of constructing the filter in the framework(SMSEntry.java),
         * Construct the filter here in SMSEmbeddedLdapObject or the plugin
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
            debug.message("SMSEmbeddedLdapObject.searchOrganizationNames: " +
                "orgNames search filter: " + sfilter);
        }
        Set answer = searchSubOrganizationNames(dn, sfilter, numOfEntries,
            sortResults, ascendingOrder, true);

        return (answer);
    }
    
    public void shutdown() {
    }
}
