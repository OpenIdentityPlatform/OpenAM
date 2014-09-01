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
 * $Id: SMSObject.java,v 1.9 2009/10/28 04:24:26 hengming Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.naming.directory.ModificationItem;

/**
 * Abstract class that needs to be implemented to store configuration data in a
 * data store. An implementation of this can be configured via
 * <code>AMConfig.properties</code> by setting the property <code>
 * com.sun.identity.sm.sms_object_class_name</code>
 * to the fully qualified class name (i.e., including the package name) without
 * the <code>.class</code> extension. Only one instance of this class will be
 * instantiated within a single JVM, hence the function must be reentrant. The
 * implementation of this class must provide an empty constructor that will be
 * used to create an instance of this class.
 */
public abstract class SMSObject {

    /**
     * Initialization parameters that are configured via system properties or
     * can be dynamically set during run-time.
     */
    public void initialize(Map initParams) throws SMSException {
        // do nothing
    }

    /**
     * Reads in the object from persistent store. It assumes the object name and
     * the ssoToken are valid. If the entry does not exist the method should
     * return <code>null</code>
     */
    public abstract Map read(SSOToken token, String objName)
            throws SMSException, SSOException;

    /**
     * Creates an entry in the persistent store. Throws an exception if the
     * entry already exists
     */
    public abstract void create(SSOToken token, String objName, Map attributes)
            throws SMSException, SSOException;

    /**
     * Modifies the attributes to the object.
     */
    public abstract void modify(SSOToken token, String objName,
            ModificationItem[] mods) throws SMSException, SSOException;

    /**
     * Delete the entry in the datastore. This should delete sub-entries also
     */
    public abstract void delete(SSOToken token, String objName)
            throws SMSException, SSOException;

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects that
     * are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public abstract Set searchSubOrgNames(SSOToken token, String dn,
            String filter, int numOfEntries, boolean sortResults,
            boolean ascendingOrder, boolean recursive) throws SMSException,
            SSOException;

    /**
     * Returns the organization names. Returns a set of SMSEntry objects that
     * are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public abstract Set searchOrganizationNames(SSOToken token, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException;

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public abstract Set subEntries(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException;

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public abstract Set schemaSubEntries(SSOToken token, String dn,
            String filter, String sidFilter, int numOfEntries,
            boolean sortResults, boolean ascendingOrder) throws SMSException,
            SSOException;

    /**
     * Searchs the data store for objects that match the filter
     */
    public abstract Set search(SSOToken token, String startDN, String filter,
            int numOfEntries, int timeLimit, boolean sortResults,
            boolean ascendingOrder) throws SMSException, SSOException;

    /**
     * Searchs the data store for objects that match the filter
     */
    public abstract Iterator search(SSOToken token, String startDN,
        String filter, int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes) throws SMSException, SSOException;

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public abstract boolean entryExists(SSOToken token, String objName);

    /**
     * Registration of Notification Callbacks
     */
    public void registerCallbackHandler(
            SMSObjectListener changeListener) throws SMSException {
        // default implementation
    }

    /**
     * De-Registration of Notification Callbacks
     */
    public void deregisterCallbackHandler(String listenerID) {
        // default implementation
    }

    /**
     * Returns the root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public abstract String getRootSuffix();

    /**
     * Returns the AMSDK BaseDN for the UM objects.
     */
    public abstract String getAMSdkBaseDN();


    // Non-abstract convenience methods for implementation classes

    /**
     * Returns the naming attribute
     */
    public String getNamingAttribute() {
        return (SMSEntry.PLACEHOLDER_RDN);
    }

    /**
     * Returns the organization naming attribute
     */
    public String getOrgNamingAttribute() {
        return (SMSEntry.ORGANIZATION_RDN);
    }

    /**
     * Returns all the SMS attribute names
     */
    public String[] getAttributeNames() {
        return (SMSEntry.SMS_ATTRIBUTES);
    }

    /**
     * Returns search (LDAP) filter to search for SMS objects
     */
    public String getSearchFilter() {
        return (SMSEntry.FILTER_PATTERN);
    }

    /**
     * Returns search (LDAP) filter to search for SMS objects
     */
    public String getServiceIdSearchFilter() {
        return (SMSEntry.FILTER_PATTERN_ALL);
    }

    public Debug debug() {
        return (SMSEntry.debug);
    }

    public boolean cacheResults() {
        return (SMSEntry.cacheSMSEntries);
    }
    
    // Method to close the resources held by the plugins
    public void shutdown() {
        // Default empty implementation
    }
}
