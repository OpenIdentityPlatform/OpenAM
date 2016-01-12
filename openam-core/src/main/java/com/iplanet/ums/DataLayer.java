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
 * $Id: DataLayer.java,v 1.19 2009/11/20 23:52:52 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.iplanet.ums;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.AUTHN_BIND_REQUEST;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.ldap.event.EventService;
import com.iplanet.services.util.I18n;
import com.sun.identity.common.configuration.ConfigurationListener;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.security.ServerInstanceAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Attributes;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ConnectionPool;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.ProxiedAuthV1RequestControl;
import org.forgerock.opendj.ldap.controls.ServerSideSortRequestControl;
import org.forgerock.opendj.ldap.controls.VirtualListViewRequestControl;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ModifyDNRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.requests.SimpleBindRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 * DataLayer (A PACKAGE SCOPE CLASS) to access LDAP or other database
 * 
 * TODO: 1. Needs to subclass and isolate the current implementation of
 * DataLayer as DSLayer for ldap specific operations 2. Improvements needed for
 * _ldapPool: destroy(), initial bind user, tunning for MIN and MAX initial
 * settings etc 3. May choose to extend implementation of _ldapPool from
 * LDAPConnectionPool so that there is load balance between connections. Also
 * _ldapPool may be implemented with a HashTable of (host,port) for mulitple
 * pools of connections for mulitple (host,port) to DS servers instead of single
 * host and port.
 * 
 * @supported.api
 */
public class DataLayer implements java.io.Serializable {

    private static final String RETRIES_KEY = "com.iplanet.am.replica.num.retries";
    private static final String RETRIES_DELAY_KEY = "com.iplanet.am.replica.delay.between.retries";

    /**
     * Static section to retrieve the debug object.
     */
    private static Debug debug;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    private static DataLayerConfigListener configListener;

    /**
     * Default minimal connections if none is defined in configuration
     */

    /**
     * Default maximum connections if none is defined in configuration
     */
    static final int MAX_CONN = 20;

    /**
     * Default maximum backlog queue size
     */
    static final int MAX_BACKLOG = 100;

    static final String LDAP_MAXBACKLOG = "maxbacklog";

    static final String LDAP_RELEASECONNBEFORESEARCH =
        "releaseconnectionbeforesearchcompletes";

    static final String LDAP_REFERRAL = "referral";

    private static int replicaRetryNum = 1;

    private static long replicaRetryInterval = 1000;

    private static final String LDAP_CONNECTION_NUM_RETRIES = 
        "com.iplanet.am.ldap.connection.num.retries";

    private static final String LDAP_CONNECTION_RETRY_INTERVAL = 
        "com.iplanet.am.ldap.connection.delay.between.retries";

    private static final String LDAP_CONNECTION_ERROR_CODES = 
        "com.iplanet.am.ldap.connection.ldap.error.codes.retries";

    private static int connNumRetry = 3;

    private static int connRetryInterval = 1000;

    private static Set<ResultCode> retryErrorCodes = new HashSet<>();
    
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
        initConnectionParams();
    }
    
    public static void initConnectionParams() {
        String numRetryStr = SystemProperties.get(LDAP_CONNECTION_NUM_RETRIES);
        if (numRetryStr != null) {
            try {
                connNumRetry = Integer.parseInt(numRetryStr);
            } catch (NumberFormatException e) {
                if (debug.warningEnabled()) {
                    debug.warning("Invalid value for "
                            + LDAP_CONNECTION_NUM_RETRIES);
                }
            }
        }

        String retryIntervalStr = SystemProperties
                .get(LDAP_CONNECTION_RETRY_INTERVAL);
        if (retryIntervalStr != null) {
            try {
                connRetryInterval = Integer.parseInt(retryIntervalStr);
            } catch (NumberFormatException e) {
                if (debug.warningEnabled()) {
                    debug.warning("Invalid value for "
                            + LDAP_CONNECTION_RETRY_INTERVAL);
                }
            }
        }

        String retryErrs = SystemProperties.get(LDAP_CONNECTION_ERROR_CODES);
        if (retryErrs != null) {
            StringTokenizer stz = new StringTokenizer(retryErrs, ",");
            while (stz.hasMoreTokens()) {
                retryErrorCodes.add(ResultCode.valueOf(Integer.parseInt(stz.nextToken().trim())));
            }
        }

        if (debug.messageEnabled()) {
            debug.message("DataLayer: number of retry = " + connNumRetry);
            debug.message("DataLayer: retry interval = " + connRetryInterval);
            debug.message("DataLayer: retry error codes = " + retryErrorCodes);
        }
    }

    /**
     * DataLayer constructor
     */
    private DataLayer() {
    }

    /**
     * Constructor given the extra parameter of guid and pwd identifying an
     * authenticated principal
     * 
     * @param host
     *            LDAP host
     * @param port
     *            LDAP port
     * @param pwd
     *            Password for the user
     */
    private DataLayer(String id, String pwd, String host, int port)
        throws UMSException {
        m_proxyUser = id;
        m_proxyPassword = pwd;
        m_host = host;
        m_port = port;
        configListener = new DataLayerConfigListener();

        initReplicaProperties();
        initLdapPool();
    }

    /**
     * Create the singleton DataLayer object if it doesn't exist already.
     *
     * @supported.api
     */
    public synchronized static DataLayer getInstance(ServerInstance serverCfg)
        throws UMSException {
        // Make sure only one instance of this class is created.
        if (m_instance == null) {
            String host = "localhost";
            int port = 389;
            String pUser = "";
            String pPwd = "";

            if (serverCfg != null) {
                host = serverCfg.getServerName();
                port = serverCfg.getPort();
                pUser = serverCfg.getAuthID();
                pPwd = (String) AccessController
                        .doPrivileged(new ServerInstanceAction(serverCfg));
            }
            m_instance = new DataLayer(pUser, pPwd, host, port);

            ConfigurationObserver.getInstance().addListener(configListener);

            // Start the EventService thread if it has not already started.
            initializeEventService();
        }
        return m_instance;
    }

    /**
     * Create the singleton DataLayer object if it doesn't exist already.
     * Assumes the server instance for "LDAPUser.Type.AUTH_PROXY".
     *
     * @supported.api
     */
    public static DataLayer getInstance() throws UMSException {
        // Make sure only one instance of this class is created.
        if (m_instance == null) {
            try {
                DSConfigMgr cfgMgr = DSConfigMgr.getDSConfigMgr();
                ServerInstance serverCfg = cfgMgr.getServerInstance(LDAPUser.Type.AUTH_PROXY);
                m_instance = getInstance(serverCfg);
            } catch (LDAPServiceException ex) {
                debug.error("Error:  Unable to get server config instance "
                        + ex.getMessage());
            }
        }
        return m_instance;
    }

    /**
     * Get connection from pool. Reauthenticate if necessary
     * 
     * @return connection that is available to use.
     *
     * @supported.api
     */
    public Connection getConnection(java.security.Principal principal) throws LdapException {
        if (_ldapPool == null)
            return null;

        if (debug.messageEnabled()) {
            debug.message("Invoking _ldapPool.getConnection()");
        }

        // proxy as given principal
        ProxiedAuthV1RequestControl.newControl(principal.getName());
        Connection conn = _ldapPool.getConnection();
        if (debug.messageEnabled()) {
            debug.message("Got Connection : " + conn);
        }

        return conn;
    }

    /**
     * Returns String values of the attribute.
     * 
     * @param principal Authentication Principal.
     * @param guid distinguished name.
     * @param attrName attribute name.
     *
     * @supported.api
     */
    public String[] getAttributeString(Principal principal, Guid guid, String attrName) {
        String id = guid.getDn();
        SearchRequest request = LDAPRequests.newSearchRequest(id, SearchScope.BASE_OBJECT, "(objectclass=*)");
        try {
            try (ConnectionEntryReader reader = readLDAPEntry(principal, request)) {
                Attribute attribute = reader.readEntry().getAttribute(attrName);
                Collection<String> values = new ArrayList<>();
                for (ByteString byteString : attribute) {
                    values.add(byteString.toString());
                }
                return values.toArray(new String[0]);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning(
                        "Exception in DataLayer.getAttributeString for DN: "
                                + id, e);
            }
            return null;
        }
    }

    /**
     * Returns <code>Attr</code> from the given attribute name.
     * 
     * @param principal Authentication Principal.
     * @param guid Distinguished name.
     * @param attrName Attribute name.
     *
     * @supported.api
     */
    public Attr getAttribute(Principal principal, Guid guid, String attrName) {
        String id = guid.getDn();
        try {
            SearchRequest request = LDAPRequests.newSearchRequest(id, SearchScope.BASE_OBJECT, "(objectclass=*)",
                    attrName);
            try (ConnectionEntryReader reader = readLDAPEntry(principal, request)) {
                Attribute attribute = reader.readEntry().getAttribute(attrName);
                if (attribute == null) {
                    return null;
                } else {
                    return new Attr(attribute);
                }
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.getAttribute for DN: "
                        + id, e);
            }
            return null;
        }
    }

    /**
     * Returns attributes for the given attribute names.
     * 
     * @param principal Authentication Principal.
     * @param guid Distinguished name.
     * @param attrNames Attribute names.
     * @return collection of Attr.
     *
     * @supported.api
     */
    public Collection<Attr> getAttributes(Principal principal, Guid guid, Collection<String> attrNames) {
        String id = guid.getDn();
        SearchRequest request = LDAPRequests.newSearchRequest(id, SearchScope.BASE_OBJECT, "(objectclass=*)",
                attrNames.toArray(EMPTY_STRING_ARRAY));
        ConnectionEntryReader ldapEntry;
        try {
            ldapEntry = readLDAPEntry(principal, request);

            if (ldapEntry == null) {
                debug.warning("No attributes returned may not have permission to read");
                return Collections.emptySet();
            }

            Collection<Attr> attributes = new ArrayList<>();
            while (ldapEntry.hasNext()) {
                if (ldapEntry.isEntry()) {
                    SearchResultEntry entry = ldapEntry.readEntry();
                    for (Attribute attr : entry.getAllAttributes()) {
                        attributes.add(new Attr(attr));
                    }
                }
            }
            return attributes;
        } catch(Exception e) {
            debug.warning("Exception in DataLayer.getAttributes for DN: {}", id, e);
            return null;
        }
    }

    /**
     * Adds entry to the server.
     * 
     * @param principal Authenticated Principal.
     * @param guid Distinguished name.
     * @param attrSet attribute set containing name/value pairs.
     * @exception AccessRightsException if insufficient access>
     * @exception EntryAlreadyExistsException if the entry already exists.
     * @exception UMSException if fail to add entry.
     *
     * @supported.api
     */
    public void addEntry(
        java.security.Principal principal,
        Guid guid,
        AttrSet attrSet
    ) throws UMSException {
        String id = guid.getDn();
        ResultCode errorCode;

        try {
            AddRequest request = LDAPRequests.newAddRequest(id);
            for (Attribute attribute : attrSet.toLDAPAttributeSet()) {
                request.addAttribute(attribute);
            }

            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.addEntry retry: " + retry);
                }

                try (Connection conn = getConnection(principal)) {
                    conn.add(request);
                    return;
                } catch (LdapException e) {
                    errorCode = e.getResult().getResultCode();
                    if (!retryErrorCodes.contains(errorCode) || retry == connNumRetry) {
                        throw e;
                    }
                    retry++;
                    try {
                        Thread.sleep(connRetryInterval);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (LdapException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.addEntry for DN: " + id,
                        e);
            }
            errorCode = e.getResult().getResultCode();
            String[] args = {id};
            if (ResultCode.ENTRY_ALREADY_EXISTS.equals(errorCode)) {
                throw new EntryAlreadyExistsException(i18n.getString(IUMSConstants.ENTRY_ALREADY_EXISTS, args), e);
            } else if (ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(errorCode)) {
                throw new AccessRightsException(i18n.getString(IUMSConstants.INSUFFICIENT_ACCESS_ADD, args), e);
            } else {
                throw new UMSException(i18n.getString(IUMSConstants.UNABLE_TO_ADD_ENTRY, args), e);
            }
        }
    }

    /**
     * Delete entry from the server
     * 
     * @param guid
     *            globally unique identifier for the entry
     * @exception AccessRightsException
     *                insufficient access
     * @exception EntryNotFoundException
     *                if the entry is not found
     * @exception UMSException
     *                Fail to delete the entry
     *
     * @supported.api
     */
    public void deleteEntry(java.security.Principal principal, Guid guid)
            throws UMSException {
        if (guid == null) {
            String msg = i18n.getString(IUMSConstants.BAD_ID);
            throw new IllegalArgumentException(msg);
        }
        String id = guid.getDn();
        ResultCode errorCode;

        try {
            DeleteRequest request = LDAPRequests.newDeleteRequest(id);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.deleteEntry retry: " + retry);
                }

                try (Connection conn = getConnection(principal)) {
                    conn.delete(request);
                    return;
                } catch (LdapException e) {
                    if (!retryErrorCodes.contains(e.getResult().getResultCode())
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
        } catch (LdapException e) {
            debug.error("Exception in DataLayer.deleteEntry for DN: " + id, e);
            errorCode = e.getResult().getResultCode();
            String[] args = { id };
            if (ResultCode.NO_SUCH_OBJECT.equals(errorCode)) {
                throw new EntryNotFoundException(i18n.getString(IUMSConstants.ENTRY_NOT_FOUND, args), e);
            } else if (ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(errorCode)) {
                throw new AccessRightsException(i18n.getString(IUMSConstants.INSUFFICIENT_ACCESS_DELETE, args), e);
            } else {
                throw new UMSException(i18n.getString(IUMSConstants.UNABLE_TO_DELETE_ENTRY, args), e);
            }
        }
    }

    /**
     * Read an ldap entry
     * 
     * @param guid
     *            globally unique identifier for the entry
     * @return an attribute set representing the entry in ldap, all non
     *         operational attributes are read
     * @exception EntryNotFoundException
     *                if the entry is not found
     * @exception UMSException
     *                Fail to read the entry
     *
     * @supported.api
     */
    public AttrSet read(java.security.Principal principal, Guid guid)
            throws UMSException {
        return read(principal, guid, null);
    }

    /**
     * Reads an ldap entry.
     * 
     * @param principal Authentication Principal.
     * @param guid Globally unique identifier for the entry.
     * @param attrNames Attributes to read.
     * @return an attribute set representing the entry in LDAP.
     * @exception EntryNotFoundException if the entry is not found.
     * @exception UMSException if fail to read the entry.
     *
     * @supported.api
     */
    public AttrSet read(
        java.security.Principal principal,
        Guid guid,
        String attrNames[]
    ) throws UMSException {
        String id = guid.getDn();
        ConnectionEntryReader entryReader;
        SearchRequest request = LDAPRequests.newSearchRequest(id, SearchScope.BASE_OBJECT, "(objectclass=*)",
                attrNames);

        entryReader = readLDAPEntry(principal, request);

        if (entryReader == null) {
            throw new AccessRightsException(id);
        }

        Collection<Attribute> attrs = new ArrayList<>();
        try (ConnectionEntryReader reader = entryReader) {
            while (reader.hasNext()) {
                if (reader.isReference()) {
                    reader.readReference();
                    //TODO AME-7017
                }
                SearchResultEntry entry = entryReader.readEntry();
                for (Attribute attr : entry.getAllAttributes()) {
                    attrs.add(attr);
                }
            }
            if (attrs.isEmpty()) {
                throw new EntryNotFoundException(i18n.getString(IUMSConstants.ENTRY_NOT_FOUND, new String[]{id}));
            }
            return new AttrSet(attrs);
        } catch (IOException e) {
            throw new UMSException(i18n.getString(IUMSConstants.UNABLE_TO_READ_ENTRY, new String[]{id}), e);
        }
    }

    public void rename(java.security.Principal principal, Guid guid,
            String newName, boolean deleteOldName)
            throws UMSException {
        String id = guid.getDn();
        ResultCode errorCode;

        try {
            ModifyDNRequest request = LDAPRequests.newModifyDNRequest(id, newName);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.rename retry: " + retry);
                }

                try (Connection conn = getConnection(principal)) {
                    conn.applyChange(request);
                    return;
                } catch (LdapException e) {
                    errorCode = e.getResult().getResultCode();
                    if (!retryErrorCodes.contains(errorCode) || retry == connNumRetry) {
                        throw e;
                    }
                    retry++;
                    try {
                        Thread.sleep(connRetryInterval);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (LdapException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.rename for DN: " + id, e);
            }
            errorCode = e.getResult().getResultCode();
            if (ResultCode.NO_SUCH_OBJECT.equals(errorCode)) {
                throw new EntryNotFoundException(id, e);
            } else if (ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(errorCode)) {
                throw new AccessRightsException(id, e);
            } else {
                throw new UMSException(id, e);
            }
        }
    }

    /**
     * Modifies an ldap entry.
     * 
     * @param principal Authentication Principal.
     * @param guid globally unique identifier for the entry.
     * @param modifications Set of modifications for the entry.
     * @exception AccessRightsException if insufficient access
     * @exception EntryNotFoundException if the entry is not found.
     * @exception UMSException if failure
     *
     * @supported.api
     */
    public void modify(Principal principal, Guid guid, Collection<Modification> modifications)
            throws UMSException {
        String id = guid.getDn();
        ResultCode errorCode;

        try {
            ModifyRequest request = LDAPRequests.newModifyRequest(id);
            for (Modification modification : modifications) {
                request.addModification(modification);
            }
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.modify retry: " + retry);
                }

                try (Connection conn = getConnection(principal)) {
                    conn.modify(request);
                    return;
                } catch (LdapException e) {
                    if (!retryErrorCodes.contains("" + e.getResult().getResultCode().toString())
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
        } catch (LdapException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.modify for DN: " + id, e);
            }
            errorCode = e.getResult().getResultCode();
            if (ResultCode.NO_SUCH_OBJECT.equals(errorCode)) {
                throw new EntryNotFoundException(id, e);
            } else if (ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(errorCode)) {
                throw new AccessRightsException(id, e);
            } else {
                throw new UMSException(id, e);
            }
        }
    }

    /**
     * Changes user password.
     * 
     * @param guid globally unique identifier for the entry.
     * @param attrName password attribute name
     * @param oldPassword old password
     * @param newPassword new password
     * @exception AccessRightsException if insufficient access
     * @exception EntryNotFoundException if the entry is not found.
     * @exception UMSException if failure
     *
     * @supported.api
     */
    public void changePassword(Guid guid, String attrName, String oldPassword, String newPassword)
            throws UMSException {

        Modification modification = new Modification(ModificationType.REPLACE,
                Attributes.singletonAttribute(attrName, newPassword));

        String id = guid.getDn();

        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            String hostAndPort = dsCfg.getHostName("default");

            // All connections will use authentication
            SimpleBindRequest bindRequest = LDAPRequests.newSimpleBindRequest(id, oldPassword.toCharArray());
            Options options = Options.defaultOptions()
                    .set(AUTHN_BIND_REQUEST, bindRequest);

            try (ConnectionFactory factory = new LDAPConnectionFactory(hostAndPort, 389, options)) {
                Connection ldc = factory.getConnection();
                ldc.modify(LDAPRequests.newModifyRequest(id).addModification(modification));
            } catch (LdapException ldex) {
                if (debug.warningEnabled()) {
                    debug.warning("DataLayer.changePassword:", ldex);
                }
                ResultCode errorCode = ldex.getResult().getResultCode();
                if (ResultCode.NO_SUCH_OBJECT.equals(errorCode)) {
                    throw new EntryNotFoundException(id, ldex);
                } else if (ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(errorCode)) {
                    throw new AccessRightsException(id, ldex);
                } else {
                    throw new UMSException(id, ldex);
                }
            }
        } catch (LDAPServiceException ex) {
            debug.error("DataLayer.changePassword:", ex);
            throw new UMSException(id, ex);
        }
    }

    /**
     * Adds value for an attribute and saves the change in the database.
     * 
     * @param principal Authenticated Principal.
     * @param guid ID of the entry to which to add the attribute value.
     * @param name name of the attribute to which value is being added.
     * @param value Value to be added to the attribute.
     * @throws UMSException if there is any error while adding the value.
     *
     * @supported.api
     */
    public void addAttributeValue(Principal principal, Guid guid, String name, String value) throws UMSException {
        // Delegate to the other modify() method.
        modifyAttributeValue(ModificationType.ADD, principal, guid, name, value);
    }

    /**
     * Removes value for an attribute and saves the change in the database.
     * 
     * @param principal Authenticated Principal.
     * @param guid the id of the entry from which to remove the attribute value.
     * @param name Name of the attribute from which value is being removed
     * @param value Value to be removed from the attribute.
     * @throws UMSException if there is any error while removing the value.
     *
     * @supported.api
     */
    public void removeAttributeValue(Principal principal, Guid guid, String name, String value) throws UMSException {
        // Delegate to the other modify() method.
        modifyAttributeValue(ModificationType.DELETE, principal, guid, name, value);
    }

    private void modifyAttributeValue(ModificationType modType, Principal principal, Guid guid, String name,
            String value) throws UMSException {
        // Delegate to the other modify() method.
        modify(principal, guid, Collections.singleton(
                new Modification(modType, Attributes.singletonAttribute(name, value))));
    }

    private List<Control> getSearchControls(SearchControl searchControl) throws LdapException {
        if (searchControl != null) {
            int[] vlvRange = searchControl.getVLVRange();
            SortKey[] sortKeys = searchControl.getSortKeys();
            Collection<org.forgerock.opendj.ldap.SortKey> ldapSortKeys;
            List<Control> ctrls = new ArrayList<>(); // will hold all server controls

            if (sortKeys != null) {
                ldapSortKeys = new ArrayList<>(sortKeys.length);
                for (SortKey sortKey : sortKeys) {
                    ldapSortKeys.add(new org.forgerock.opendj.ldap.SortKey(sortKey.attributeName, sortKey.reverse));
                }

                ctrls.add(ServerSideSortRequestControl.newControl(false, ldapSortKeys));

                if (vlvRange != null) {
                    if (searchControl.getVLVJumpTo() == null) {
                        ctrls.add(VirtualListViewRequestControl.newOffsetControl(false, vlvRange[0], 0, vlvRange[1],
                                vlvRange[2], null));
                    } else {
                        ctrls.add(VirtualListViewRequestControl.newAssertionControl(false,
                                ByteString.valueOfUtf8(searchControl.getVLVJumpTo()), vlvRange[1], vlvRange[2], null));
                    }
                }
            }
            return ctrls;
        }
        return null;
    }

    /**
     * Performs synchronous search based on specified ldap filter. This is low
     * level API which assumes caller knows how to construct a data store filer.
     * 
     * @param principal Authenticated Principal.
     * @param guid Unique identifier for the entry.
     * @param scope Scope can be either <code>SCOPE_ONE</code>,
     *        <code>SCOPE_SUB</code> or <code>SCOPE_BASE</code>.
     * @param searchFilter Search filter for this search.
     * @param attrNames Attribute name for retrieving.
     * @param attrOnly if true, returns the names but not the values of the
     *        attributes found.
     * @param searchControl Search Control.
     * @exception UMSException if failure.
     * @exception InvalidSearchFilterException if failure
     *
     * @supported.api
     */
    public SearchResults search(
        java.security.Principal principal,
        Guid guid,
        int scope,
        String searchFilter,
        String attrNames[],
        boolean attrOnly,
        SearchControl searchControl
    ) throws UMSException {
        String id = guid.getDn();

        // always add "objectclass" to attributes to get, to find the right java
        // class
        String[] attrNames1 = null;
        if (attrNames != null) {
            attrNames1 = new String[attrNames.length + 1];
            System.arraycopy(attrNames, 0, attrNames1, 0, attrNames.length);
            attrNames1[attrNames1.length - 1] = "objectclass";
        } else {
            attrNames1 = new String[] { "objectclass" };
        }

        ConnectionEntryReader ldapResults = null;

        // if searchFilter is null, search for everything under the base
        if (searchFilter == null) {
            searchFilter = "(objectclass=*)";
        }
        ResultCode errorCode;

        try {
            Connection conn = getConnection(principal);
            List<Control> controls = getSearchControls(searchControl);
            // call readLDAPEntry() only in replica case, save one LDAP search
            // assume replica case when replicaRetryNum is not 0
            if (replicaRetryNum != 0) {
                readLDAPEntry(conn, id, null);
            }

            SearchRequest request = null;
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.search retry: " + retry);
                }

                if (searchControl != null && searchControl.isGetAllReturnAttributesEnabled()) {
                    /*
                     * The array {"*"} is used, because LDAPv3 defines
                     * "*" as a special string indicating all
                     * attributes. This gets all the attributes.
                     */
                    attrNames1 = new String[] { "*" };
                }
                request = LDAPRequests.newSearchRequest(id, SearchScope.valueOf(scope), searchFilter, attrNames1);
                break;
            }
            for (Control control : controls) {
                request.addControl(control);
            }

            ldapResults = conn.search(request);

            // TODO: need review and see if conn is recorded properly for
            // subsequent use
            //
            SearchResults result = new SearchResults(conn, ldapResults, conn, this);
            result.set(SearchResults.BASE_ID, id);
            result.set(SearchResults.SEARCH_FILTER, searchFilter);
            result.set(SearchResults.SEARCH_SCOPE, scope);

            if ((searchControl != null)
                    && (searchControl.contains(SearchControl.KeyVlvRange) 
                       || searchControl.contains(SearchControl.KeyVlvJumpTo))) {
                result.set(SearchResults.EXPECT_VLV_RESPONSE, Boolean.TRUE);

            }

            if (searchControl != null
                    && searchControl.contains(SearchControl.KeySortKeys)) {
                SortKey[] sortKeys = searchControl.getSortKeys();
                if (sortKeys != null && sortKeys.length > 0) {
                    result.set(SearchResults.SORT_KEYS, sortKeys);
                }
            }

            return result;

        } catch (LdapException e) {
            errorCode = e.getResult().getResultCode();
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.search: ", e);
            }
            String msg = i18n.getString(IUMSConstants.SEARCH_FAILED);
            if (ResultCode.TIME_LIMIT_EXCEEDED.equals(errorCode)) {
                int timeLimit = searchControl != null ? searchControl.getTimeOut() : 0;
                throw new TimeLimitExceededException(String.valueOf(timeLimit), e);
            } else if (ResultCode.SIZE_LIMIT_EXCEEDED.equals(errorCode)) {
                int sizeLimit = searchControl != null ? searchControl.getMaxResults() : 0;
                throw new SizeLimitExceededException(String.valueOf(sizeLimit), e);
            } else if (ResultCode.CLIENT_SIDE_PARAM_ERROR.equals(errorCode)
                    || ResultCode.PROTOCOL_ERROR.equals(errorCode)) {
                throw new InvalidSearchFilterException(searchFilter, e);
            } else {
                throw new UMSException(msg, e);
            }
        }
    }

    /**
     * Perform synchronous search based on specified ldap filter. This is low
     * level API which assumes caller knows how to construct a data store filer.
     * 
     * @param principal Authenticated Principal.
     * @param guid Unique identifier for the entry
     * @param scope Scope can be either <code>SCOPE_ONE</code>,
     *        <code>SCOPE_SUB</code>, <code>SCOBE_BASE</code>
     * @param searchFilter Search filter for this search.
     * @param searchControl Search Control.
     * @exception UMSException if failure.
     * @exception InvalidSearchFilterException if failure.
     *
     * @supported.api
     */
    public SearchResults searchIDs(
        java.security.Principal principal,
        Guid guid,
        int scope,
        String searchFilter,
        SearchControl searchControl
    ) throws InvalidSearchFilterException, UMSException {
        // TODO: support LDAP referral
        String attrNames[] = { "objectclass" };
        return search(principal, guid, scope, searchFilter, attrNames, false,
                searchControl);
    }

    /**
     * Fetches the schema from the LDAP directory server. Retrieve the entire
     * schema from the root of a Directory Server.
     * 
     * @return the schema in the LDAP directory server
     * @exception AccessRightsException
     *                insufficient access
     * @exception UMSException
     *                Fail to fetch the schema.
     * @exception LdapException
     *                Error with LDAP connection.
     *
     * @supported.api
     */
    public Schema getSchema(java.security.Principal principal) throws UMSException {
        ResultCode errorCode;

        try (Connection conn = getConnection(principal)) {
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.getSchema retry: " + retry);
                }

                try {
                    return Schema.readSchemaForEntry(conn, DN.valueOf("cn=schema"));
                } catch (LdapException e) {
                    if (!retryErrorCodes.contains(e.getResult().getResultCode()) || retry == connNumRetry) {
                        throw e;
                    }
                    retry++;
                    try {
                        Thread.sleep(connRetryInterval);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (LdapException e) {
            debug.error("Exception in DataLayer.getSchema: ", e);
            errorCode = e.getResult().getResultCode();
            if (ResultCode.INSUFFICIENT_ACCESS_RIGHTS.equals(errorCode)) {
                throw new AccessRightsException(m_host, e);
            } else {
                throw new UMSException(m_host, e);
            }
        }

        return null;
    }

    private synchronized void initReplicaProperties() {
        int retries = SystemProperties.getAsInt(RETRIES_KEY, 0);
        if (retries < 0) {
            retries = 0;
            debug.warning("Invalid value for replica retry num, set to 0");
        }

        replicaRetryNum = retries;

        long interval = SystemProperties.getAsLong(RETRIES_DELAY_KEY, 0);
        if (interval < 0) {
            interval = 0;
            debug.warning("Invalid value for replica interval, set to 0");
        }

        replicaRetryInterval = interval;
    }

    public Entry readLDAPEntry(Connection ld, String dn,
            String[] attrnames) throws LdapException {

        LdapException ldapEx = null;
        int retry = 0;
        int connRetry = 0;
        while (retry <= replicaRetryNum && connRetry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("DataLayer.readLDAPEntry: connRetry: "
                        + connRetry);
                debug.message("DataLayer.readLDAPEntry: retry: " + retry);
            }
            try {
                if (attrnames == null) {
                    return ld.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn));
                } else {
                    return ld.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(dn, attrnames));
                }
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (ResultCode.NO_SUCH_OBJECT.equals(errorCode)) {
                    if (debug.messageEnabled()) {
                        debug.message("Replica: entry not found: " + dn
                                + " retry: " + retry);
                    }
                    if (retry == replicaRetryNum) {
                        ldapEx = e;
                    } else {
                        try {
                            Thread.sleep(replicaRetryInterval);
                        } catch (Exception ignored) {
                        }
                    }
                    retry++;
                } else if (retryErrorCodes.contains("" + errorCode)) {
                    if (connRetry == connNumRetry) {
                        ldapEx = e;
                    } else {
                        try {
                            Thread.sleep(connRetryInterval);
                        } catch (Exception ignored) {
                        }
                    }
                    connRetry++;
                } else {
                    throw e;
                }
            }
        }

        throw ldapEx;
    }

    public ConnectionEntryReader readLDAPEntry(Principal principal, SearchRequest request) throws UMSException {

        LdapException ldapEx = null;
        int retry = 0;
        int connRetry = 0;
        while (retry <= replicaRetryNum && connRetry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("DataLayer.readLDAPEntry: connRetry: "
                        + connRetry);
                debug.message("DataLayer.readLDAPEntry: retry: " + retry);
            }
            try (Connection conn = getConnection(principal)) {
                return conn.search(request);
            } catch (LdapException e) {
                ResultCode errorCode = e.getResult().getResultCode();
                if (ResultCode.NO_SUCH_OBJECT.equals(errorCode)) {
                    if (debug.messageEnabled()) {
                        debug.message("Replica: entry not found: " +
                            request.getName().toString() + " retry: " + retry);
                    }
                    if (retry == replicaRetryNum) {
                        ldapEx = e;
                    } else {
                        try {
                            Thread.sleep(replicaRetryInterval);
                        } catch (Exception ex) {
                        }
                    }
                    retry++;
                } else if (retryErrorCodes.contains("" + errorCode)) {
                    if (connRetry == connNumRetry) {
                        ldapEx = e;
                    } else {
                        try {
                            Thread.sleep(connRetryInterval);
                        } catch (Exception ex) {
                        }
                    }
                    connRetry++;
                } else {
                    throw new UMSException(e.getMessage(), e);
                }
            }
        }

        throw new UMSException(ldapEx.getMessage(), ldapEx);
    }


    /**
     * Initialize the pool shared by all DataLayer object(s).
     */
    private synchronized void initLdapPool() throws UMSException {
        // Don't do anything if pool is already initialized
        if (_ldapPool != null)
            return;

        /*
         * Initialize the pool with minimum and maximum connections settings
         * retrieved from configuration
         */
        ServerInstance svrCfg = null;
        String hostName = null;

        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            hostName = dsCfg.getHostName("default");
            baseFactory = dsCfg.getNewProxyConnectionFactory();

            svrCfg = dsCfg.getServerInstance(LDAPUser.Type.AUTH_PROXY);
        } catch (LDAPServiceException ex) {
            debug.error("Error initializing connection pool " + ex.getMessage());
        }
        
        // Check if svrCfg was successfully obtained
        if (svrCfg == null) {
            debug.error("Error getting server config.");
            String args[] = new String[1];
            args[0] = hostName == null ? "default" : hostName;
            throw new UMSException(i18n.getString(IUMSConstants.NEW_INSTANCE_FAILED, args));
        }

        int poolMin = svrCfg.getMinConnections();
        int poolMax = svrCfg.getMaxConnections();
        m_releaseConnectionBeforeSearchCompletes = svrCfg.getBooleanValue(LDAP_RELEASECONNBEFORESEARCH, false);

        if (debug.messageEnabled()) {
            debug.message("Creating ldap connection pool with: poolMin {}, poolMax {}", poolMin, poolMax);
        }

        int idleTimeout = SystemProperties.getAsInt(Constants.LDAP_CONN_IDLE_TIME_IN_SECS, 0);
        if (idleTimeout == 0) {
            debug.warning("Idle timeout not set. Defaulting to 0.");
        }

        _ldapPool = Connections.newCachedConnectionPool(
                Connections.newNamedConnectionFactory(baseFactory, "DataLayer"), poolMin, poolMax, idleTimeout,
                TimeUnit.SECONDS);

        ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();
        shutdownMan.addShutdownListener(
            new ShutdownListener() {
                public void shutdown() {
                    if (_ldapPool != null) {
                        _ldapPool.close();
                    }
                }
            }
        );
    }

    public static int getConnNumRetry() {
        return connNumRetry;
    }

    public static int getConnRetryInterval() {
        return connRetryInterval;
    }

    public static Set<ResultCode> getRetryErrorCodes() {
        return retryErrorCodes;
    }
    
    private static void initializeEventService() {
        // Initialize event service. This is to make sure that EventService
        // thread is started. The other place where it is also tried to start
        // is: com.iplanet.am.sdk.ldap.AMEventManager which is
        // initialized in com.iplanet.am.sdk.ldap.DirectoryManager
        if (!EventService.isStarted()) {
            // Use a separate thread to start the EventService thread.
            // This will prevent deadlocks associated in the system because
            // of EventService related dependencies.
            InitEventServiceThread th = new InitEventServiceThread();
            Thread initEventServiceThread = new Thread(th,
                "InitEventServiceThread");
            initEventServiceThread.setDaemon(true);
            initEventServiceThread.start();
        }
    }

    private static class InitEventServiceThread implements Runnable {
        public void run() {
            debug.message("InitEventServiceThread:initializeEventService() - "
                + "EventService thread getting  initialized ");
            try {
                EventService es = EventService.getEventService();
                synchronized (es) {
                    if (!EventService.isStarted()) {
                        es.restartPSearches();
                    }
                }
            } catch (Exception e) {
                // An Error occurred while initializing EventService
                debug.error("InitEventServiceThread:run() Unable to start EventService!!", e);
            }
        }
    }    

    static private ConnectionPool _ldapPool = null;

    static private ConnectionFactory baseFactory = null;

    static private DataLayer m_instance = null;

    private String m_host = null;

    private int m_port;

    private String m_proxyUser = "";

    private String m_proxyPassword = "";

    private boolean m_releaseConnectionBeforeSearchCompletes = false;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private class DataLayerConfigListener implements ConfigurationListener {

        @Override
        public synchronized void notifyChanges() {
            final int retries = SystemProperties.getAsInt(RETRIES_KEY, 0);
            final long delay = SystemProperties.getAsLong(RETRIES_DELAY_KEY, 0);

            if (retries != replicaRetryNum || delay != replicaRetryInterval) {
                initReplicaProperties();
            }
        }
    }
}
