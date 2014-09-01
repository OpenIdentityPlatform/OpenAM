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
 * $Id: DataLayer.java,v 1.19 2009/11/20 23:52:52 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.ldap.event.EventService;
import com.iplanet.services.util.I18n;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.security.ServerInstanceAction;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPBind;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPControl;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPSchema;
import com.sun.identity.shared.ldap.LDAPSchemaElement;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.LDAPAddRequest;
import com.sun.identity.shared.ldap.LDAPDeleteRequest;
import com.sun.identity.shared.ldap.LDAPModifyRequest;
import com.sun.identity.shared.ldap.LDAPModifyRDNRequest;
import com.sun.identity.shared.ldap.LDAPSearchRequest;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPSortKey;
import com.sun.identity.shared.ldap.controls.LDAPProxiedAuthControl;
import com.sun.identity.shared.ldap.controls.LDAPSortControl;
import com.sun.identity.shared.ldap.controls.LDAPVirtualListControl;

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

    /**
     * Static section to retrieve the debug object.
     */
    private static Debug debug;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

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

    private static HashSet retryErrorCodes = new HashSet();
    
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
                retryErrorCodes.add(stz.nextToken().trim());
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
     * @param guid
     *            Identification of an authenticated principal
     * @param pwd
     *            Password for the user
     */
    private DataLayer(String id, String pwd, String host, int port)
        throws UMSException {
        m_proxyUser = id;
        m_proxyPassword = pwd;
        m_host = host;
        m_port = port;

        initReplicaProperties();
        initLdapPool();
    }

    /**
     * create the singelton DataLayer object if it doesn't exist already.
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

            // Start the EventService thread if it has not already started.
            initializeEventService();
        }
        return m_instance;
    }

    /**
     * create the singelton DataLayer object if it doesn't exist already.
     * Assumes the server instance for "LDAPUser.Type.AUTH_PROXY".
     *
     * @supported.api
     */
    public static DataLayer getInstance() throws UMSException {
        // Make sure only one instance of this class is created.
        if (m_instance == null) {
            try {
                DSConfigMgr cfgMgr = DSConfigMgr.getDSConfigMgr();
                ServerInstance serverCfg = cfgMgr
                        .getServerInstance(LDAPUser.Type.AUTH_PROXY);
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
    public LDAPConnection getConnection(java.security.Principal principal) {
        if (_ldapPool == null)
            return null;

        if (debug.messageEnabled()) {
            debug.message("Invoking _ldapPool.getConnection()");
        }

        // proxy as given principal
        LDAPProxiedAuthControl proxyCtrl = new LDAPProxiedAuthControl(principal
                .getName(), true);
        LDAPConnection conn = _ldapPool.getConnection();
        if (debug.messageEnabled()) {
            debug.message("Got Connection : " + conn);
        }
        LDAPSearchConstraints cons = conn.getSearchConstraints();
        cons.setServerControls(proxyCtrl);
        conn.setSearchConstraints(cons);

        return conn;
    }

    /**
     * Just call the pool method to release the connection so that the given
     * connection is free for others to use
     * 
     * @param conn
     *            connection in the pool to be released for others to use
     *
     * @supported.api
     */
    public void releaseConnection(LDAPConnection conn) {
        if (_ldapPool == null || conn == null)
            return;

        // reset the original constraints
        // TODO: check with ldapjdk and see if this is appropriate
        // to restore the default constraints.
        //
        conn.setSearchConstraints(_defaultSearchConstraints);

        // A soft close on the connection. Returns the connection to the pool
        // and
        // make it available.
        if (debug.messageEnabled()) {
            debug.message("Invoking _ldapPool.close(conn) : " + conn);
        }
        _ldapPool.close(conn);
        if (debug.messageEnabled()) {
            debug.message("Released Connection : " + conn);
        }
    }

    /**
     * Just call the pool method to release the connection so that the given
     * connection is free for others to use
     * 
     * @param conn
     *            connection in the pool to be released for others to use
     * @param ldapErrCode ldap exception error code used to determine failover.
     *
     * @supported.api
     */
    public void releaseConnection( LDAPConnection conn , int ldapErrCode)
    {
        if (_ldapPool == null || conn == null) return;

        // reset the original constraints
        // TODO: check with ldapjdk and see if this is appropriate
        //       to restore the default constraints.
        //
        conn.setSearchConstraints(_defaultSearchConstraints);

        // A soft close on the connection.
        // Returns the connection to the pool and
        // make it available.
        if (debug.messageEnabled()) {
            debug.message("Invoking _ldapPool.close(conn,ldapErrCode) : " +
                conn + ":" + ldapErrCode);
        }
        _ldapPool.close( conn, ldapErrCode );
        if (debug.messageEnabled()) {
            debug.message("Released Connection:close(conn,ldapErrCode) : " +
                conn);
        }
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
    public String[] getAttributeString(
        java.security.Principal principal,
        Guid guid,
        String attrName
    ) {
        String id = guid.getDn();
        LDAPEntry ldapEntry = null;
        LDAPSearchRequest request =
            LDAPRequestParser.parseReadRequest(id);
        try {
            ldapEntry = readLDAPEntry(principal, request);
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning(
                        "Exception in DataLayer.getAttributeString for DN: "
                                + id, e);
            }
            return null;
        }
        LDAPAttribute attr = ldapEntry.getAttribute(attrName);           
        return attr.getStringValueArray();
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
    public Attr getAttribute(
        java.security.Principal principal,
        Guid guid,
        String attrName
    ) {
        String id = guid.getDn();
        LDAPEntry ldapEntry = null;
        try {
            String[] attrNames = new String[1];
            attrNames[0] = attrName;
            LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(id,
                attrNames);
            ldapEntry = readLDAPEntry(principal, request);
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.getAttribute for DN: "
                        + id, e);
            }
            return null;
        }
        LDAPAttribute ldapAttr = ldapEntry.getAttribute(attrName);
        if (ldapAttr == null) {
            return null;
        } else {
            return new Attr(ldapAttr);
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
    public Collection getAttributes(
        java.security.Principal principal,
        Guid guid,
        Collection attrNames
    ) {
        Collection attributes = new ArrayList();
        String id = guid.getDn();
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(id,
            (String[]) attrNames.toArray(EMPTY_STRING_ARRAY));
        LDAPEntry ldapEntry = null;
        try {
            ldapEntry = readLDAPEntry(principal, request);
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.getAttributes for DN: "
                        + id, e);
            }
            return null;
        }
        if (ldapEntry == null) {
            debug.warning("No attributes returned may not have " +
                "permission to read");
            return Collections.EMPTY_SET;
        }
        Iterator iter = attrNames.iterator();
        while (iter.hasNext()) {
            String attrName = (String) iter.next();
            LDAPAttribute ldapAttribute = ldapEntry.getAttribute(attrName);
            if (ldapAttribute != null) {
                attributes.add(new Attr(ldapAttribute));
            }
        }
        return attributes;
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
    ) throws AccessRightsException, EntryAlreadyExistsException, UMSException {
        LDAPConnection conn = null;
        String id = guid.getDn();
        int errorCode = 0;

        try {
            LDAPEntry entry = new LDAPEntry(id, attrSet.toLDAPAttributeSet());
            LDAPAddRequest request = LDAPRequestParser.parseAddRequest(entry);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.addEntry retry: " + retry);
                }

                try {
                    conn = getConnection(principal);
                    conn.add(request);
                    return;
                } catch (LDAPException e) {
                    errorCode = e.getLDAPResultCode();
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
        } catch (LDAPException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.addEntry for DN: " + id,
                        e);
            }
            errorCode = e.getLDAPResultCode();
            String[] args = { id };
            switch (errorCode) {
            case LDAPException.ENTRY_ALREADY_EXISTS:
                throw new EntryAlreadyExistsException(i18n.getString(
                        IUMSConstants.ENTRY_ALREADY_EXISTS, args), e);
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(i18n.getString(
                        IUMSConstants.INSUFFICIENT_ACCESS_ADD, args), e);
            default:
                throw new UMSException(i18n.getString(
                        IUMSConstants.UNABLE_TO_ADD_ENTRY, args), e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
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
            throws AccessRightsException, EntryNotFoundException, UMSException {
        if (guid == null) {
            String msg = i18n.getString(IUMSConstants.BAD_ID);
            throw new IllegalArgumentException(msg);
        }
        LDAPConnection conn = null;
        String id = guid.getDn();
        int errorCode = 0;

        try {
            LDAPDeleteRequest request =
                LDAPRequestParser.parseDeleteRequest(id);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.deleteEntry retry: " + retry);
                }

                try {
                    conn = getConnection(principal);
                    conn.delete(request);
                    return;
                } catch (LDAPException e) {
                    errorCode = e.getLDAPResultCode();
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
        } catch (LDAPException e) {
            debug.error("Exception in DataLayer.deleteEntry for DN: " + id, e);
            errorCode = e.getLDAPResultCode();
            String[] args = { id };
            switch (errorCode) {
            case LDAPException.NO_SUCH_OBJECT:
                throw new EntryNotFoundException(i18n.getString(
                        IUMSConstants.ENTRY_NOT_FOUND, args), e);
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(i18n.getString(
                        IUMSConstants.INSUFFICIENT_ACCESS_DELETE, args), e);
            default:
                throw new UMSException(i18n.getString(
                        IUMSConstants.UNABLE_TO_DELETE_ENTRY, args), e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
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
            throws EntryNotFoundException, UMSException {
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
    ) throws EntryNotFoundException, UMSException {
        String id = guid.getDn();
        LDAPEntry entry = null;
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(id,
            attrNames);

        try {
            entry = readLDAPEntry(principal, request);
        } catch (LDAPException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.read for DN: " + id);
                debug.warning("LDAPException: " + e);
            }
            int errorCode = e.getLDAPResultCode();
            String[] args = { id };
            if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                throw new EntryNotFoundException(i18n.getString(
                        IUMSConstants.ENTRY_NOT_FOUND, args), e);
            } else {
                throw new UMSException(i18n.getString(
                        IUMSConstants.UNABLE_TO_READ_ENTRY, args), e);
            }
        }

        if (entry == null) {
            throw new AccessRightsException(id);
        }

        LDAPAttributeSet ldapAttrSet = entry.getAttributeSet();
        if (ldapAttrSet == null) {
            String[] args = { id };
            throw new EntryNotFoundException(i18n.getString(
                    IUMSConstants.ENTRY_NOT_FOUND, args));
        }

        return new AttrSet(ldapAttrSet);
    }

    public void rename(java.security.Principal principal, Guid guid,
            String newName, boolean deleteOldName)
            throws AccessRightsException, EntryNotFoundException, UMSException {
        LDAPConnection conn = null;
        String id = guid.getDn();
        int errorCode = 0;

        try {
            LDAPModifyRDNRequest request =
                LDAPRequestParser.parseModifyRDNRequest(id, newName,
                deleteOldName);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.rename retry: " + retry);
                }

                try {
                    conn = getConnection(principal);
                    conn.rename(request);
                    return;
                } catch (LDAPException e) {
                    errorCode = e.getLDAPResultCode();
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
        } catch (LDAPException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.rename for DN: " + id, e);
            }
            errorCode = e.getLDAPResultCode();
            switch (errorCode) {
            case LDAPException.NO_SUCH_OBJECT:
                throw new EntryNotFoundException(id, e);
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(id, e);
            default:
                throw new UMSException(id, e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    /**
     * Modifies an ldap entry.
     * 
     * @param principal Authentication Principal.
     * @param guid globally unique identifier for the entry.
     * @param modSet Set of modifications for the entry.
     * @exception AccessRightsException if insufficient access
     * @exception EntryNotFoundException if the entry is not found.
     * @exception UMSException if failure
     *
     * @supported.api
     */
    public void modify(
        java.security.Principal principal,
        Guid guid,
        ModSet modSet
    ) throws AccessRightsException, EntryNotFoundException, UMSException {
        LDAPConnection conn = null;
        String id = guid.getDn();
        int errorCode = 0;

        try {
            LDAPModifyRequest request = LDAPRequestParser.parseModifyRequest(
                id, modSet);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.modify retry: " + retry);
                }

                try {
                    conn = getConnection(principal);
                    conn.modify(request);
                    return;
                } catch (LDAPException e) {
                    errorCode = e.getLDAPResultCode();
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
        } catch (LDAPException e) {
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.modify for DN: " + id, e);
            }
            errorCode = e.getLDAPResultCode();
            switch (errorCode) {
            case LDAPException.NO_SUCH_OBJECT:
                throw new EntryNotFoundException(id, e);
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(id, e);
            default:
                throw new UMSException(id, e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
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
    public void changePassword(Guid guid, String attrName, String oldPassword,
        String newPassword)
        throws AccessRightsException, EntryNotFoundException, UMSException {

        ModSet modSet = new ModSet();
        modSet.add(LDAPModification.REPLACE,
            new LDAPAttribute(attrName, newPassword));

        String id = guid.getDn();

        LDAPConnection ldc = null;
        int resultCode = 0;
        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            String hostAndPort = dsCfg.getHostName("default");

            ldc = new LDAPConnection();
            ldc.connect(hostAndPort, 389, id, oldPassword);

            ldc.modify(id, modSet);
        } catch (LDAPException ldex) {
            if (debug.warningEnabled()) {
                debug.warning("DataLayer.changePassword:", ldex);
            }
            int errorCode = ldex.getLDAPResultCode();
            switch (errorCode) {
            case LDAPException.NO_SUCH_OBJECT:
                throw new EntryNotFoundException(id, ldex);
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(id, ldex);
            default:
                throw new UMSException(id, ldex);
            }
        } catch (LDAPServiceException ex) {
            debug.error("DataLayer.changePassword:", ex);
            throw new UMSException(id, ex);
        } finally {
            if (ldc != null) {
                try {
                    ldc.disconnect();
                } catch (LDAPException lde) {
                }
            }
        }
    }

    /**
     * Adds value for an attribute and saves the change in the database.
     * 
     * @param principal Authenticated Principal.
     * @param guid ID of the entry to which to add the attribute value.
     * @param name name of the attribute to which value is being added.
     * @param value Value to be added to the attribute.
     * @throws UMSException if there is any error while adding the value
     *
     * @supported.api
     */
    public void addAttributeValue(
        java.security.Principal principal,
        Guid guid,
        String name,
        String value
    ) throws UMSException {
        ModSet modSet = new ModSet();
        modSet.add(LDAPModification.ADD, new LDAPAttribute(name, value));

        // Delegate to the other modify() method.
        modify(principal, guid, modSet);
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
    public void removeAttributeValue(java.security.Principal principal,
            Guid guid, String name, String value) throws UMSException {
        ModSet modSet = new ModSet();
        modSet.add(LDAPModification.DELETE, new LDAPAttribute(name, value));

        // Delegate to the other modify() method.
        modify(principal, guid, modSet);
    }

    /**
     * retrive LDAPConnection for search.
     */
    private LDAPConnection getSearchConnection(
            java.security.Principal principal, SearchControl searchControl) {
        LDAPConnection conn = getConnection(principal);

        if (searchControl != null) {
            LDAPSearchConstraints constraints;
            int[] vlvRange = searchControl.getVLVRange();
            SortKey[] sortKeys = searchControl.getSortKeys();
            LDAPSortKey[] ldapSortKeys;
            ArrayList ctrls = new ArrayList(); // will hold all server controls

            if (sortKeys != null) {
                ldapSortKeys = new LDAPSortKey[sortKeys.length];
                for (int i = 0; i < ldapSortKeys.length; i++) {
                    ldapSortKeys[i] = new LDAPSortKey(
                            sortKeys[i].attributeName, sortKeys[i].reverse);
                }

                ctrls.add(new LDAPSortControl(ldapSortKeys, false));

                if (vlvRange != null) {
                    if (searchControl.getVLVJumpTo() == null) {
                        ctrls.add(new LDAPVirtualListControl(vlvRange[0],
                                vlvRange[1], vlvRange[2], 0));
                    } else {
                        ctrls.add(new LDAPVirtualListControl(searchControl
                                .getVLVJumpTo(), vlvRange[1], vlvRange[2]));
                    }
                }
            }
            
            constraints = conn.getSearchConstraints();
            LDAPControl[] existingCtrls = constraints.getServerControls();
            ctrls.addAll(Arrays.asList(existingCtrls));

            // This should be 0 if intermediate results are not needed,
            // and 1 if results are to be processed as they come in.
            // (By default, this is 1.)
            constraints.setBatchSize(1);
            constraints.setMaxResults(searchControl.getMaxResults());
            constraints.setServerTimeLimit(searchControl.getTimeOut());
            if (sortKeys != null) {
                constraints.setServerControls((LDAPControl[]) ctrls
                        .toArray(new LDAPControl[0]));
            }

            searchControl.set("constraints", constraints);
        }

        return conn;
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
    ) throws InvalidSearchFilterException, UMSException {
        LDAPConnection conn = null;
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

        LDAPSearchResults ldapResults = null;

        // if searchFilter is null, search for everything under the base
        if (searchFilter == null) {
            searchFilter = "(objectclass=*)";
        }
        int errorCode = 0;

        try {
            conn = getSearchConnection(principal, searchControl);
            // call readLDAPEntry() only in replica case, save one LDAP search
            // assume replica case when replicaRetryNum is not 0
            if (replicaRetryNum != 0) {
                readLDAPEntry(conn, id, null);
            }

            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.search retry: " + retry);
                }

                try {
                    if (searchControl == null) {
                        ldapResults = conn.search(id, scope, searchFilter,
                                attrNames1, attrOnly);
                    } else {
                        if (searchControl.isGetAllReturnAttributesEnabled()) {
                            /*
                             * The array {"*"} is used, because LDAPv3 defines
                             * "*" as a special string indicating all
                             * attributes. This gets all the attributes.
                             */

                            attrNames1 = new String[] { "*" };
                        }

                        ldapResults = conn.search(id, scope, searchFilter,
                                attrNames1, attrOnly,
                                (LDAPSearchConstraints) searchControl
                                        .get("constraints"));
                    }
                    break;
                } catch (LDAPException e) {
                    errorCode = e.getLDAPResultCode();
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

            // TODO: need review and see if conn is recorded properly for
            // subsequent use
            //
            SearchResults result = new SearchResults(ldapResults, conn, this);
            result.set(SearchResults.BASE_ID, id);
            result.set(SearchResults.SEARCH_FILTER, searchFilter);
            result.set(SearchResults.SEARCH_SCOPE, new Integer(scope));

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

        } catch (LDAPException e) {
            errorCode = e.getLDAPResultCode();
            releaseConnection(conn, errorCode);
            if (debug.warningEnabled()) {
                debug.warning("Exception in DataLayer.search: ", e);
            }
            String msg = i18n.getString(IUMSConstants.SEARCH_FAILED);
            switch (errorCode) {
            case LDAPException.TIME_LIMIT_EXCEEDED: {
                int timeLimit = searchControl != null ? searchControl
                        .getTimeOut() : 0;
                throw new TimeLimitExceededException(String.valueOf(timeLimit),
                        e);
            }
            case LDAPException.SIZE_LIMIT_EXCEEDED: {
                int sizeLimit = searchControl != null ? searchControl
                        .getMaxResults() : 0;
                throw new SizeLimitExceededException(String.valueOf(sizeLimit),
                        e);
            }
            case LDAPException.PARAM_ERROR:
            case LDAPException.PROTOCOL_ERROR:
                throw new InvalidSearchFilterException(searchFilter, e);
            default:
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
     *
     * @supported.api
     */
    public LDAPSchema getSchema(java.security.Principal principal)
            throws AccessRightsException, UMSException {
        LDAPConnection conn = null;
        LDAPSchema dirSchema = new LDAPSchema();
        int errorCode = 0;

        try {
            LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(
                "fake=fake");
            conn = getConnection(principal);            
            // disable the checking of attribute syntax quoting and the
            // read on ""
            conn.setProperty(DSConfigMgr.SCHEMA_BUG_PROPERTY,
                    DSConfigMgr.VAL_STANDARD);
            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("DataLayer.getSchema retry: " + retry);
                }

                try {
                    // after connection is down, fetchSchema will not try to
                    // reconnect. So use read to force it to reconnect
                    if (retry > 0) {
                        try {
                            conn.read(request);
                        } catch (Exception ex) {
                        }
                    }

                    dirSchema.fetchSchema(conn, "cn=schema");
                    return dirSchema;
                } catch (LDAPException e) {
                    errorCode = e.getLDAPResultCode();
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
        } catch (LDAPException e) {
            debug.error("Exception in DataLayer.getSchema: ", e);
            errorCode = e.getLDAPResultCode();
            switch (errorCode) {
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(m_host, e);
            default:
                throw new UMSException(m_host, e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }

        return dirSchema;
    }

    /**
     * Adds schema element to the schema at the root DSE
     * 
     * @param schemaElement
     *            schema element to be added
     * @exception AccessRightsException
     *                insufficient access
     * @exception SchemaElementAlreadyExistsException
     *                if the element already exists
     * @exception UMSException
     *                Fail to add schema element.
     *
     * @supported.api
     */
    public void addSchema(java.security.Principal principal,
            LDAPSchemaElement schemaElement) throws AccessRightsException,
            SchemaElementAlreadyExistsException, UMSException {
        LDAPConnection conn = null;
        try {
            conn = getConnection(principal);
            // disable the checking of attribute syntax quoting and the
            // read on ""
            conn.setProperty("com.sun.identity.shared.ldap.schema.quoting",
                "standard");
            schemaElement.add(conn, "cn=schema");
        } catch (LDAPException e) {
            int errorCode = e.getLDAPResultCode();
            releaseConnection(conn, errorCode);
            conn = null;
            debug.error("Exception in DataLayer.addSchema: ", e);
            switch (errorCode) {
            case LDAPException.ATTRIBUTE_OR_VALUE_EXISTS:
                throw new SchemaElementAlreadyExistsException(schemaElement
                        .getName(), e);
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(schemaElement.getName(), e);
            default:
                throw new UMSException(schemaElement.getName(), e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    /**
     * Removes schema element from the schema
     * 
     * @param schemaElement
     *            schema element to be removed
     * @exception AccessRightsException
     *                insufficient access
     * @exception UMSException
     *                Fail to remove schema element.
     *
     * @supported.api
     */
    public void removeSchema(java.security.Principal principal,
            LDAPSchemaElement schemaElement) throws AccessRightsException,
            UMSException {
        LDAPConnection conn = null;

        try {
            conn = getConnection(principal);
            // disable the checking of attribute syntax quoting and the
            // read on ""
            conn.setProperty("com.sun.identity.shared.ldap.schema.quoting",
                "standard");
            schemaElement.remove(conn, "cn=schema");

        } catch (LDAPException e) {
            int errorCode = e.getLDAPResultCode();
            releaseConnection(conn, errorCode);
            conn = null;
            debug.error("Exception in DataLayer.removeSchema:", e);
            switch (errorCode) {
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                throw new AccessRightsException(schemaElement.getName(), e);
            default:
                throw new UMSException(schemaElement.getName(), e);
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    private void initReplicaProperties() {
        String retries = SystemProperties
                .get("com.iplanet.am.replica.num.retries");
        if (retries != null) {
            try {
                replicaRetryNum = Integer.parseInt(retries);
                if (replicaRetryNum < 0) {
                    replicaRetryNum = 0;
                    debug.warning("Invalid value for replica retry num, " +
                            "set to 0");
                }

            } catch (NumberFormatException e) {
                debug.warning("Invalid value for replica retry num");
            }
        }

        String interval = SystemProperties
                .get("com.iplanet.am.replica.delay.between.retries");
        if (interval != null) {
            try {
                replicaRetryInterval = Long.parseLong(interval);
                if (replicaRetryInterval < 0) {
                    replicaRetryInterval = 0;
                    debug.warning("Invalid value for replica interval, " +
                            "set to 0");
                }

            } catch (NumberFormatException e) {
                debug.warning("Invalid value for replica interval");
            }
        }
    }

    public LDAPEntry readLDAPEntry(LDAPConnection ld, String dn,
            String[] attrnames) throws LDAPException {

        LDAPException ldapEx = null;
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
                    return ld.read(dn);
                } else {
                    return ld.read(dn, attrnames);
                }
            } catch (LDAPException e) {
                int errorCode = e.getLDAPResultCode();
                if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                    if (debug.messageEnabled()) {
                        debug.message("Replica: entry not found: " + dn
                                + " retry: " + retry);
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
                    throw e;
                }
            }
        }

        throw ldapEx;
    }

    public LDAPEntry readLDAPEntry(java.security.Principal principal,
        LDAPSearchRequest request) throws LDAPException {

        LDAPException ldapEx = null;
        int retry = 0;
        int connRetry = 0;
        LDAPConnection ld = null;
        while (retry <= replicaRetryNum && connRetry <= connNumRetry) {
            if (debug.messageEnabled()) {
                debug.message("DataLayer.readLDAPEntry: connRetry: "
                        + connRetry);
                debug.message("DataLayer.readLDAPEntry: retry: " + retry);
            }
            try {
                ld = getConnection(principal);
                return ld.read(request);
            } catch (LDAPException e) {
                int errorCode = e.getLDAPResultCode();
                releaseConnection(ld, errorCode);
                ld = null;
                if (errorCode == LDAPException.NO_SUCH_OBJECT) {
                    if (debug.messageEnabled()) {
                        debug.message("Replica: entry not found: " +
                            request.getBaseDN() + " retry: " + retry);
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
                    throw e;
                }
            } finally {
                if (ld != null) {
                    releaseConnection(ld);
                }
            }
        }

        throw ldapEx;
    }


    /**
     * Initialize the pool shared by all DataLayer object(s).
     * 
     * @param host
     *            ldaphost to init the pool from
     * @param port
     *            ldapport to init the pool from
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
        HashMap connOptions = new HashMap();

        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            hostName = dsCfg.getHostName("default");

            _trialConn = dsCfg.getNewProxyConnection();

            svrCfg = dsCfg.getServerInstance(LDAPUser.Type.AUTH_PROXY);
        } catch (LDAPServiceException ex) {
            debug.error("Error initializing connection pool "
                        + ex.getMessage());
        }
        
        // Check if svrCfg was successfully obtained
        if ((svrCfg == null) || (_trialConn == null)) {
            debug.error("Error getting server config.");
            // throw exception
            String args[] = new String[1];
            args[0] = (hostName == null) ? "default" : hostName;
            throw new UMSException(i18n.getString(
                IUMSConstants.NEW_INSTANCE_FAILED, args));
        }

        int poolMin = svrCfg.getMinConnections();
        int poolMax = svrCfg.getMaxConnections();
        int maxBackLog = svrCfg.getIntValue(LDAP_MAXBACKLOG, MAX_BACKLOG);
        m_releaseConnectionBeforeSearchCompletes = svrCfg.getBooleanValue(
                LDAP_RELEASECONNBEFORESEARCH, false);
        boolean referrals = svrCfg.getBooleanValue(LDAP_REFERRAL, true);
        String connDN = svrCfg.getAuthID();
        String connPWD = svrCfg.getPasswd();

        if (debug.messageEnabled()) {
            debug.message("Creating ldap connection pool with :");
            debug.message("poolMin : " + poolMin);
            debug.message("poolMax : " + poolMax);
            debug.message("maxBackLog : " + maxBackLog);
        }

        try {
            // establish one good connection before the pool
            // _trialConn = new LDAPConnection();

            _trialConn.setOption(LDAPConnection.MAXBACKLOG, new Integer(
                    maxBackLog));
            _trialConn.setOption(LDAPConnection.REFERRALS, Boolean.valueOf(
                    referrals));

            /*
             * Default rebind method is to provide the same authentication
             * in the rebind to the server being referred.
             */
            LDAPBind defaultBinder = new LDAPBind() {
                public void bind(LDAPConnection ld) throws LDAPException {
                    /*
                     * There is possibly a bug in the ldapjdk that the passed in
                     * ld is not carrying the original authentication dn and pwd
                     * Hence, we have to kludge here using the one connection
                     * that we know
                     * about: the connection that we use to initialize the
                     * connection
                     * pool.
                     * TODO: need to investigate
                     */
                    String dn = _trialConn.getAuthenticationDN();
                    String pwd = _trialConn.getAuthenticationPassword();
                    String newhost = ld.getHost();
                    int newport = ld.getPort();
                    ld.connect(3, newhost, newport, dn, pwd);
                }
            };
            _trialConn.setOption(LDAPConnection.BIND, defaultBinder);

            // _trialConn.connect(3, m_host, m_port, m_proxyUser,
            // m_proxyPassword);

            // remember the original search constraints
            _defaultSearchConstraints = _trialConn.getSearchConstraints();

            // Construct the pool by cloning the successful connection
            // Set the default options too for failover and fallback features.

            connOptions.put("maxbacklog", new Integer(maxBackLog));
            connOptions.put("referrals", Boolean.valueOf(referrals));
            connOptions.put("searchconstraints", _defaultSearchConstraints);

            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    _ldapPool = new LDAPConnectionPool("DataLayer", poolMin,
                        poolMax, hostName, 389, connDN, connPWD, _trialConn,
                        connOptions);
                    shutdownMan.addShutdownListener(
                        new ShutdownListener() {
                            public void shutdown() {
                                if (_ldapPool != null) {
                                    _ldapPool.destroy();
                                }
                            }
                        }
                    );
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }

        } catch (LDAPException e) {
            debug.error("Exception in DataLayer.initLdapPool:", e);
        }
    }

    public static int getConnNumRetry() {
        return connNumRetry;
    }

    public static int getConnRetryInterval() {
        return connRetryInterval;
    }

    public static HashSet getRetryErrorCodes() {
        return retryErrorCodes;
    }
    
    private static void initializeEventService() {
        // Initialize event service. This is to make sure that EventService
        // thread is started. The other place where it is also tried to start
        // is: com.iplanet.am.sdk.ldap.AMEventManager which is
        // initialized in com.iplanet.am.sdk.ldap.DirectoryManager
        if (!EventService.isThreadStarted()) {
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
                if (!EventService.isThreadStarted()) {
                    es.resetAllSearches(false);
                }
            } catch (Exception e) {
                // An Error occurred while intializing EventService
                debug.error("InitEventServiceThread:run() Unable to "
                    + "start EventService!!", e);
            }
        }
    }    

    static private LDAPConnectionPool _ldapPool = null;

    static private LDAPConnection _trialConn = null;

    static private LDAPSearchConstraints _defaultSearchConstraints = null;

    static private DataLayer m_instance = null;

    private String m_host = null;

    private int m_port;

    private String m_proxyUser = "";

    private String m_proxyPassword = "";

    private boolean m_releaseConnectionBeforeSearchCompletes = false;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

}
