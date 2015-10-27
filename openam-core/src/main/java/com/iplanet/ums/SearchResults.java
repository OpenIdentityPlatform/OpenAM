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
 * $Id: SearchResults.java,v 1.7 2009/01/28 05:34:51 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.iplanet.ums;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.VirtualListViewResponseControl;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

/**
 * Represents search results. Each search result is a PersistentObject
 * 
 * @supported.api
 */
public class SearchResults implements java.io.Serializable {

    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Attribute name used with Object get( String ). Expected return object is
     * Integer getting the content count from the VirtualListResponse control
     * returned by server after a search
     * 
     * @supported.api
     */
    public static final String VLVRESPONSE_CONTENT_COUNT = "vlvContentCount";

    /**
     * Attribute name used with Object get( String ). Expected return object is
     * Integer getting the index of first position from VirtualListResponse
     * control returned by server after a search
     * 
     * @supported.api
     */
    public static final String VLVRESPONSE_FIRST_POSITION = "vlvFirstPosition";

    /**
     * Attribute name used with Object get( String ). Expected return object is
     * Integer getting the result code from from VirtualListResponse control
     * returned by server after a search.
     * 
     * @supported.api
     */
    public static final String VLVRESPONSE_RESULT_CODE = "vlvResultCode";

    /**
     * Attribute name used with Object get( String ). Expected return object is
     * String getting the context cookie from VirtualListResponse control
     * returned by server after a search.
     * 
     * @supported.api
     */
    public static final String VLVRESPONSE_CONTEXT = "vlvContext";

    static final String EXPECT_VLV_RESPONSE = "expectVlvResponse";

    static final String BASE_ID = "baseID";

    static final String SEARCH_FILTER = "searchFilter";

    static final String SORT_KEYS = "sortKeys";

    static final String SEARCH_SCOPE = "searchScope";

    /**
     * Constructs SearchResults from <code>ldapSearchResult</code>.
     * 
     * @param ldapSearchResult <code>LDAPSearchResults</code> to construct from
     * @param conn <code>LDAPConnection</code> assosciated with the search
     *        results.
     * @param dataLayer Data Layer assosciated with the connection.
     */
    protected SearchResults(Connection connection, ConnectionEntryReader ldapSearchResult, Connection conn,
            DataLayer dataLayer) {
        // TODO: SearchResults is tightly coupled with DataLayer and
        // PersistentObject. That could make it harder to separate them
        // in the future.
        //
        this.connection = connection;
        m_ldapSearchResults = ldapSearchResult;
        m_conn = conn;
        m_dataLayer = dataLayer;
        if (debug.messageEnabled()) {
            debug.message("Constructing SearchResults: " + this
                    + " with connection : " + conn);
        }
    }

    /**
     * Constructs Search Results from <code>ldapSearchResult</code>.
     * 
     * @param ldapSearchResult <code>LDAPSearchResults</code> to construct
     *        from.
     * @param conn <code>LDAPConnection</code> associated with the search
     *        results.
     */
    protected SearchResults(Connection connection, ConnectionEntryReader ldapSearchResult, Connection conn) {
        this(connection, ldapSearchResult, conn, null);
    }

    /**
     * Secret constructor for iterating through the values of an entry.
     * 
     * @param attr
     *            An attribute containing 0 or more DN values, to be treated as
     *            individual results
     */
    SearchResults(Attr attr) {
        if (attr == null) {
            m_attrVals = new String[0];
        } else {
            m_attrVals = attr.getStringValues();
        }
    }

    /**
     * Checks whether there are entries available.
     * 
     * @return <code>true</code> if there is more to read
     * @supported.api
     */
    public boolean hasMoreElements() {
        boolean hasGotMoreElements = false;
        try {
            hasGotMoreElements = (m_attrVals != null) ?
                    (m_attrIndex < m_attrVals.length)
                    : m_ldapSearchResults.hasNext();
            if (hasGotMoreElements) {
                readEntry();
            }
            if (debug.messageEnabled()) {
            if (!hasGotMoreElements && m_conn != null) {
                    debug.message("Finishing SearchResults: " + this
                            + "  with connection : " + m_conn);
                    debug.message("SearchResults: " + this
                            + "  releasing connection : " + m_conn);
                }
            }
        } catch (LdapException | SearchResultReferenceIOException ignored) {
        }
        return hasGotMoreElements;
    }

    private void readEntry() throws SearchResultReferenceIOException, LdapException {
        if (m_ldapSearchResults != null) {
            if (m_ldapSearchResults.isReference()) {
                //Ignoring references
                m_ldapSearchResults.readReference();
            }
            currentEntry = m_ldapSearchResults.readEntry();
        }
    }

    /**
     * Returns the next entry in the search results.
     * 
     * @throws UMSException
     *             No more entries in the search results.
     * @supported.api
     */
    public PersistentObject next() throws UMSException {
        // TODO: define detailed exception list (eg. referral, ...)
        //

        SearchResultEntry ldapEntry;

        if (m_attrVals != null) {
            if (m_attrIndex < m_attrVals.length) {
                String dn = m_attrVals[m_attrIndex++];
                PersistentObject pO = new PersistentObject();
                pO.setGuid(new Guid(dn));
                pO.setPrincipal(m_principal);
                return pO;
            } else {
                throw new NoSuchElementException();
            }
        }

        if ((ldapEntry = currentEntry) != null) {
            String id = ldapEntry.getName().toString();
            Collection<Attribute> attributes = new ArrayList<>();
            for (Attribute attribute : ldapEntry.getAllAttributes()) {
                attributes.add(attribute);
            }
            AttrSet attrSet = new AttrSet(attributes);
            Class javaClass = TemplateManager.getTemplateManager()
                    .getJavaClassForEntry(id, attrSet);
            PersistentObject pO = null;
            try {
                pO = (PersistentObject) javaClass.newInstance();
            } catch (Exception e) {
                String args[] = new String[1];

                args[0] = e.toString();
                String msg = i18n.getString(
                        IUMSConstants.NEW_INSTANCE_FAILED, args);
                throw new UMSException(msg);
            }
            // Make it a live object
            pO.setAttrSet(attrSet);
            pO.setGuid(new Guid(ldapEntry.getName().toString()));
            pO.setPrincipal(m_principal);
            return pO;
        }
        return null;
    }

    /**
     * Assert if the search result contains one and only one entry.
     * 
     * @return Entry if and only if there is one single entry
     * @throws EntryNotFoundException
     *             if there is no entry at all
     * 
     * @supported.api
     */
    public PersistentObject assertOneEntry() throws EntryNotFoundException,
            UMSException {
        PersistentObject entry = null;
        while (hasMoreElements()) {
            entry = next();
            break;
        }

        if (entry == null) {
            throw new EntryNotFoundException();
        }

        if (hasMoreElements()) {
            abandon();
            // TODO: to be replaced by new exception
            //
            throw new UMSException(
                        i18n.getString(IUMSConstants.MULTIPLE_ENTRY));
        }

        return entry;
    }

    /**
     * Get search result attributes related to the search operation performed.
     * 
     * @param name
     *            Name of attribute to return, null if attribute is unknown or
     *            not found
     * @throws UMSException
     *             from accessor methods on LDAPVirtualListResponse control
     * 
     * @supported.api
     */
    public Object get(String name) throws UMSException {

        // For non vlv related attributes, get it
        // from the generic hash table
        //
        if (!isVLVAttrs(name)) {
            return m_attrHash == null ? null : m_attrHash.get(name);
        }

        // The rest is related to vlv response control
        //
        if (currentEntry == null)
            return null;

        List<Control> ctrls = currentEntry.getControls();

        if (ctrls == null && expectVlvResponse() == true) {

            //
            // Code to deal with response controls not being returned yet. It
            // instructs a small search with vlv ranage that expect one result
            // to
            // return so that the response is returned. This probe is only
            // launched if EXPECT_VLV_RESPONSE is set for true in SearchResults
            //

            PersistentObject parent = getParentContainer();

            synchronized (this) {
                // The following code fragment uses a test control that only
                // asks
                // one result to return. This is done so that the response
                // control
                // can be queried for the vlvContentCount. This is a search
                // probe to
                // get the vlvCount
                //
                String[] sortAttrNames = { "objectclass" };
                SortKey[] sortKeys = (SortKey[]) get(SearchResults.SORT_KEYS);
                String filter = (String) get(SearchResults.SEARCH_FILTER);
                Integer scopeVal = (Integer) get(SearchResults.SEARCH_SCOPE);
                int scope = scopeVal == null ? SearchControl.SCOPE_SUB
                        : scopeVal.intValue();

                SearchControl testControl = new SearchControl();
                testControl.setVLVRange(1, 0, 0);

                if (sortKeys == null) {
                    testControl.setSortKeys(sortAttrNames);
                } else {
                    testControl.setSortKeys(sortKeys);
                }

                testControl.setSearchScope(scope);

                SearchResults testResults = parent.search(filter,
                        sortAttrNames, testControl);
                while (testResults.hasMoreElements()) {
                    // This while loop is required to
                    // enumerate the result set to get the response control
                    testResults.next();
                }

                // After all the hazzle, now the response should be in after the
                // search probe, use the probe's search results to get the vlv
                // related attribute(s). Set the internal flag not to launch
                // the probe again in subsequent get.
                //
                testResults.set(SearchResults.EXPECT_VLV_RESPONSE, Boolean.FALSE);
                return testResults.get(name);
            }
        }

        // the control can be null
        if (ctrls == null)
            return null;

        VirtualListViewResponseControl vlvResponse = null;

        // Find the VLV response control recorded in SearchResults
        //
        for (Control control : ctrls) {
            if (VirtualListViewResponseControl.OID.equals(control.getOID())) {
                vlvResponse = (VirtualListViewResponseControl) control;
            }
        }

        // Check on the attribute to return and
        // return the value from the response control
        // Currently only expose the VirtualListResponse control
        // returned after a search operation
        //
        if (vlvResponse != null) {
            if (name.equalsIgnoreCase(VLVRESPONSE_CONTENT_COUNT)) {
                return vlvResponse.getContentCount();
            } else if (name.equalsIgnoreCase(VLVRESPONSE_FIRST_POSITION)) {
                return vlvResponse.getTargetPosition();
            } else if (name.equalsIgnoreCase(VLVRESPONSE_RESULT_CODE)) {
                return vlvResponse.getResult().intValue();
            } else if (name.equalsIgnoreCase(VLVRESPONSE_CONTEXT)) {
                return vlvResponse.getValue().toString();
            }
        }

        // For all other unknown attribute names,
        // just return a null object
        //
        return null;

    }

    /**
     * Abandons a current search operation, notifying the server not to send
     * additional search results.
     * 
     * @throws UMSException
     *             from abandoning a search operation from LDAP
     * @supported.api
     */
    public void abandon() throws UMSException {
        //Nothing to do
        IOUtils.closeIfNotNull(connection, m_ldapSearchResults);
    }

    /**
     * Sets the principal with which to associate search results.
     * 
     * @param principal Authenticated principal.
     */
    protected void setPrincipal(Principal principal) {
        m_principal = principal;
    }

    /**
     * Set attribute internal to search result. This set function is explicitly
     * coded for package scope.
     * 
     * @param name
     *            Name of attribute to set.
     * @param value
     *            Value of attribute to set
     */
    void set(String name, Object value) {

        if (m_attrHash == null) {
            synchronized (this) {
                if (m_attrHash == null) {
                    m_attrHash = new Hashtable();
                }
            }
        }

        m_attrHash.put(name, value);
    }

    /**
     * Check if this search result expects a VLV response control
     * 
     * @return <code>true</code> if search result expects a VLV response
     *         control and <code>false</code> otherwise
     */
    private boolean expectVlvResponse() {
        Boolean expected = Boolean.FALSE;

        try {
            expected = (Boolean) get(EXPECT_VLV_RESPONSE);
        } catch (Exception e) {
        }

        return expected == null ? false : expected.booleanValue();
    }

    /**
     * Gets the original container that the search result is originated from
     * 
     * @return PersistentObject The parent container object.
     * @throws UMSException
     *             If an exception occurs.
     */
    private PersistentObject getParentContainer() throws UMSException {
        String parentID = null;
        PersistentObject parent = null;

        try {
            parentID = (String) get(BASE_ID);
            Guid parentGuid = new Guid(parentID);
            parent = new PersistentObject(m_principal, parentGuid);
        } catch (UMSException e) {
            throw new UMSException(e.getMessage());
        }

        return parent;
    }

    /**
     * Check if attribute name is related to vlv response attributes
     * 
     */
    private boolean isVLVAttrs(String name) {

        for (int i = 0; i < vlvAttrNames.length; i++) {
            if (name.equalsIgnoreCase(vlvAttrNames[i])) {
                return true;
            }
        }
        return false;
    }

    /*
     * Iterator iterator() { return new Iterator() { public boolean hasNext() {
     * return SearchResults.this.hasMoreElements(); }
     * 
     * public Object next() { PersistentObject po = null; try { po =
     * SearchResults.this.next(); } catch ( Exception ignored) { } return po; }
     * 
     * public void remove() { throw new UnsupportedOperationException(); } }; }
     */

    private SearchResultEntry currentEntry = null;

    private Connection connection;

    private ConnectionEntryReader m_ldapSearchResults = null;

    private Connection m_conn = null;

    private Principal m_principal = null;

    private Hashtable m_attrHash = null;

    private static String[] vlvAttrNames = { VLVRESPONSE_CONTENT_COUNT,
            VLVRESPONSE_FIRST_POSITION, VLVRESPONSE_RESULT_CODE,
            VLVRESPONSE_CONTEXT };

    //
    // These are only used for the tricky constructor with SearchResults(Attr)
    //
    private String[] m_attrVals = null;

    private int m_attrIndex = 0;

    private DataLayer m_dataLayer = null;

}
