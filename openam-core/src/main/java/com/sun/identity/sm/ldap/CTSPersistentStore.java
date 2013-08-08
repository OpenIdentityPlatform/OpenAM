/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package com.sun.identity.sm.ldap;

import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.fields.CoreTokenField;
import com.sun.identity.sm.ldap.api.fields.CoreTokenFieldTypes;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import com.sun.identity.sm.ldap.impl.CoreTokenLDAPAdapter;
import com.sun.identity.sm.ldap.impl.QueryBuilder;
import com.sun.identity.sm.ldap.impl.QueryFilter;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.TokenEncryption;
import com.sun.identity.tools.objects.MapFormat;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.forgerock.openam.session.ha.i18n.AmsessionstoreMessages.*;

/**
 * Core Token Service Persistent Store is responsible for the storage and retrieval of
 * Tokens from the persistent store.
 *
 * The Core Token Service is exposed through a series of CRUDL operations which use TokenAdapters
 * to convert from objects to be stored in the Core Token Service, to the Token format required
 * for generic storage.
 *
 * The Core Token Service is responsible for the storage mechanism behind the Session fail-over
 * feature.
 *
 * Persistence is currently provided by LDAP.
 *
 * @see com.sun.identity.sm.ldap.adapters.TokenAdapter
 * @see Token
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 * @author jason.lemay@forgerock.com
 * @author robert.wapshott@forgerock.com
 */
public class CTSPersistentStore extends GeneralTaskRunnable {

    private static Debug DEBUG;

    // Singleton Instance
    private static volatile CTSPersistentStore instance = null;

    private static Thread storeThread;
    private CoreTokenLDAPAdapter adapter = null;
    private CoreTokenConfig coreTokenConfig;
    private LDAPDataConversion dataConversion;
    private TokenEncryption tokenEncryption;
    private final DataLayerConnectionFactory connectionFactory;

    /**
     * Globals private Constants, so not to pollute entire product.
     */
    private static final String OBJECTCLASS = "objectClass";

    private static final String ANY_OBJECTCLASS_FILTER = "(" + OBJECTCLASS + Constants.EQUALS + Constants.ASTERISK + ")";

    /**
     * Shared SM Data Layer Accessor.
     */
    private static volatile CTSDataLayer CTSDataLayer;


    /**
     * Service Globals
     */
    private static volatile boolean shutdown = false;


    private final static String ID = "CTSPersistentStore";

    private final Object LOCK = new Object();

    /**
     * Configuration Definitions
     */
    static public final String OAUTH2 = "oauth2";

    private static final String FAMRECORDS_NAMING = "ou=famrecords";
    private static final String OAUTH2TOKENS_NAMING = "ou=" + OAUTH2 + "tokens";

    /**
     * Define Global DN and Container Constants
     */
    // Our default Top Level Root Suffix.
    // This is resolved during Initialization process.
    private static final String BASE_ROOT_DN_NAME = "BASE_ROOT";
    private static String BASE_ROOT_DN;

    // Our default for TOKEN_ROOT_SUFFIX = ou=tokens
    private static final String TOKEN_ROOT_SUFFIX =
            SystemPropertiesManager.get(CoreTokenConstants.SYS_PROPERTY_TOKEN_ROOT_SUFFIX,
                    Constants.DEFAULT_TOKEN_ROOT_SUFFIX);

    //  Our default for TOKEN_ROOT = ou=tokens,dc=openam,dc=forgerock,dc=org
    private static final String TOKEN_ROOT = TOKEN_ROOT_SUFFIX +
            Constants.COMMA + "{" + BASE_ROOT_DN_NAME + "}";

    // Our default for TOKEN_SESSION_HA_ROOT_SUFFIX = ou=openam-session
    private static final String TOKEN_SESSION_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(CoreTokenConstants.SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_SESSION_HA_ROOT_SUFFIX);

    // Our default for TOKEN_SAML2_HA_ROOT_SUFFIX = ou=openam-saml2
    private static final String TOKEN_SAML2_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(CoreTokenConstants.SYS_PROPERTY_TOKEN_SAML2_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_SAML2_HA_ROOT_SUFFIX);

    // Our default for TOKEN_OAUTH2_HA_ROOT_SUFFIX = ou-openam-oauth2
    private static final String TOKEN_OAUTH2_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(CoreTokenConstants.SYS_PROPERTY_TOKEN_OAUTH2_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_OAUTH2_HA_ROOT_SUFFIX);

    /**
     * Define Session DN Constants
     */
    private static final String SESSION_FAILOVER_HA_BASE_DN =
            FAMRECORDS_NAMING + Constants.COMMA +
                    TOKEN_SESSION_HA_ROOT_SUFFIX + Constants.COMMA + TOKEN_ROOT;

    /**
     * Define SAML2 DN Constants
     */
    private static final String SAML2_HA_BASE_DN =
            FAMRECORDS_NAMING + Constants.COMMA +
                    TOKEN_SAML2_HA_ROOT_SUFFIX + Constants.COMMA + TOKEN_ROOT;

    /**
     * Define OAUTH2 DN Constants
     */
    private static final String OAUTH2_HA_BASE_DN =
            OAUTH2TOKENS_NAMING + Constants.COMMA +
                    TOKEN_OAUTH2_HA_ROOT_SUFFIX + Constants.COMMA + TOKEN_ROOT;

    /**
     * Private restricted to preserve Singleton Instantiation.
     */
    private CTSPersistentStore(CoreTokenConfig coreTokenConfig, LDAPDataConversion dataConversion,
                               TokenEncryption tokenEncryption, DataLayerConnectionFactory connectionFactory) {
        this.coreTokenConfig = coreTokenConfig;
        this.dataConversion = dataConversion;
        this.tokenEncryption = tokenEncryption;
        this.connectionFactory = connectionFactory;
        this.DEBUG = SessionService.sessionDebug;
    }

    /**
     * Provide Service Instance Access to our Singleton
     *
     * @return CTSPersistentStore Singleton Instance.
     */
    public static final CTSPersistentStore getInstance() {
        synchronized (CTSPersistentStore.class) {

            if (instance == null) {

                DataLayerConnectionFactory connectionFactory = InjectorHolder.getInstance(DataLayerConnectionFactory.class);
                instance = new CTSPersistentStore(
                        new CoreTokenConfig(),
                        new LDAPDataConversion(),
                        new TokenEncryption(),
                        connectionFactory);

                // Proceed With Initialization of Service.
                try {
                    // Initialize the Initial Singleton Service Instance.
                    initialize();
                } catch (StoreException se) {
                    DEBUG.error("CTS Persistent Store Initialization Failed: " + se.getMessage());
                    DEBUG.error("CTS Persistent Store requests will be Ignored, until this condition is resolved!");
                }
            }
        } // End of synchronized block.
        // Return Instance.
        return instance;
    }

    /**
     * Perform Initialization
     */
    private synchronized static void initialize()
            throws StoreException {
        // Initialize this Service
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Initializing Configuration for the OpenAM Session Repository using Implementation Class: " +
                    CTSPersistentStore.class.getSimpleName());
        }
        // *******************************
        // Set up Shutdown Thread Hook.
        ShutdownManager shutdownManager = ShutdownManager.getInstance();
        if (shutdownManager.acquireValidLock()) {
            try {
                shutdownManager.addShutdownListener(new ShutdownListener() {
                    @Override
                    public void shutdown() {
                        internalShutdown();
                    }
                });
            } finally {
                shutdownManager.releaseLockAndNotify();
            }
        }

        // *************************************************************
        // Obtain our Directory Connection and ensure we can access our
        // Internal Directory Connection or use an External Source as
        // per configuration.
        //
        prepareCTSPersistenceStore();
        // ******************************************
        // Start our AM Repository Store Thread.
        storeThread = new Thread(instance);
        storeThread.setName(ID);
        storeThread.start();
        DEBUG.warning(DB_DJ_STR_OK.get().toString());
        // Finish Initialization
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Successful Configuration Initialization for the OpenAM Session Repository using Implementation Class: " +
                    CTSPersistentStore.class.getSimpleName());
        }
    }

    /**
     * Prepare our BackEnd Persistence Store.
     *
     * @throws StoreException - Exception thrown if Error condition exists.
     */
    private synchronized static void prepareCTSPersistenceStore() throws StoreException {
        String messageTag = "CTSPersistenceStore.prepareCTSPersistenceStore: ";
        DEBUG.message(messageTag + "Attempting to Prepare BackEnd Persistence Store for CTS Services.");
        try {
            CTSDataLayer = CTSDataLayer.getSharedSMDataLayerAccessor();
            if (CTSDataLayer == null) {
                throw new StoreException("Unable to obtain BackEnd Persistence Store for CTS Services.");
            }
        } catch (Exception e) {
            DEBUG.error("Exception Occurred during attempt to access shared SM Data Layer!", e);
            throw new StoreException(e);
        }
        // Obtain our Root DN to use as out Base DN.
        BASE_ROOT_DN = SMSEntry.getRootSuffix();
        if ((BASE_ROOT_DN == null) || (BASE_ROOT_DN.isEmpty())) {
            throw new StoreException("Unable to obtain Base Root DN from SMSEntry.getRootSuffix() method call!");
        }
        // Show Informational Message.
        if (DEBUG.messageEnabled()) {
            DEBUG.message(messageTag + "Successfully Prepared BackEnd Persistent Store for CTS Services.");
        }
    }

    /**
     * Provides an instance of the CoreTokenAdapter the first time this function is called.
     * Otherwise returns the same instance for all subsequent calls.
     *
     * @return Non null instance of the CoreTokenAdapter.
     * @throws IllegalStateException If the connection to the LDAP database could not be established.
     */
    private CoreTokenLDAPAdapter getAdapter() {
        if (adapter == null) {
            synchronized (this) {
                if (adapter == null) {
                    LDAPDataConversion conversion = new LDAPDataConversion();
                    CoreTokenConstants constants = new CoreTokenConstants(SMSEntry.getRootSuffix());
                    adapter = new CoreTokenLDAPAdapter(connectionFactory, conversion, constants);
                }
            }
        }
        return adapter;
    }

    /**
     * Create a Token in the persistent store. If the Token already exists in the store then this
     * function will throw a CoreTokenException. Instead it is recommended to use the update function.
     *
     * @see CTSPersistentStore#update(com.sun.identity.sm.ldap.api.tokens.Token)
     *
     * @param token Non null Token to create.
     * @throws CoreTokenException If there was a non-recoverable error during the operation or if
     * the Token already exists in the store.
     */
    public void create(Token token) throws CoreTokenException {
        if (coreTokenConfig.isTokenEncrypted()) {
            token = tokenEncryption.encrypt(token);
        }
        getAdapter().create(token);
    }

    /**
     * Read a Token from the persistent store.
     *
     * @param tokenId The non null Token Id that the Token was created with.
     * @return Null if there was no matching Token. Otherwise a fully populated Token will be returned.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public Token read(String tokenId) throws CoreTokenException {
        return getAdapter().read(tokenId);
    }

    /**
     * Update an existing Token in the store. If the Token does not exist in the store then a
     * Token is created. If the Token did exist in the store then it is updated.
     *
     * Not all fields on the Token can be updated, see the Token class for more details.
     *
     * @see Token
     *
     * @param token Non null Token to update.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public void update(Token token) throws CoreTokenException {
        if (coreTokenConfig.isTokenEncrypted()) {
            token = tokenEncryption.encrypt(token);
        }

        getAdapter().update(token);
        if (DEBUG.messageEnabled()) {
            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Update: {0} updated",
                    token.getTokenId()));
        }
    }

    /**
     * Delete the Token from the store.
     *
     * @param token Non null Token to be deleted from the store.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public void delete(Token token) throws CoreTokenException {
        delete(token.getTokenId());
    }

    /**
     * Delete the Token from the store based on its id.
     *
     * Note: It is often more efficient to delete the token based on the Id if you already
     * have this information, rather than reading the Token first before removing it.
     *
     * @param tokenId The non null Token Id of the token to remove.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public void delete(String tokenId) throws DeleteFailedException {
        getAdapter().delete(tokenId);
        if (DEBUG.messageEnabled()) {
            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Delete: {0} deleted",
                    tokenId));
        }
    }

    /**
     * Delete a collection of Tokens from the Token Store using a filter to narrow down the
     * Tokens to be deleted.
     *
     * Note: This operation is linear in its execution time so the more Tokens being deleted, the
     * longer it will take.
     *
     * @param query Non null filters which will be combined logically using AND.
     *
     * @return total number of tokens deleted by query.
     *
     * @throws DeleteFailedException If the delete failed for any reason.
     */
    public int delete(Map<CoreTokenField, Object> query) throws DeleteFailedException {
        QueryFilter.QueryFilterBuilder queryFilter = getAdapter().buildFilter().and();
        for (Map.Entry<CoreTokenField, Object> entry : query.entrySet()) {
            CoreTokenField key = entry.getKey();
            Object value = entry.getValue();
            queryFilter = queryFilter.attribute(key, value);
        }

        QueryBuilder builder = getAdapter()
                .query()
                .withFilter(queryFilter.build())
                .returnTheseAttributes(CoreTokenField.TOKEN_ID);

        Collection<Entry> entries;
        try {
            entries = builder.executeRawResults();
            for (Entry entry : entries) {
                Attribute attribute = entry.getAttribute(CoreTokenField.TOKEN_ID.toString());
                String tokenId = attribute.firstValueAsString();
                getAdapter().delete(tokenId);
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Delete: {0} deleted",
                        entries.size()));
            }
        } catch (CoreTokenException e) {
            throw new DeleteFailedException(builder, e);
        }
        return entries.size();
    }

    /**
     * Perform a query based on a collection of queryable parameters.
     *
     * The query will be an AND query where each matching Token must match on all query parameters
     * provided.
     *
     * @param query A mapping of CoreTokenField keys to values.
     * @return A non null, but possibly empty collection of Tokens.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public Collection<Token> list(Map<CoreTokenField, Object> query) throws CoreTokenException {
        // Verify all types are safe to cast.
        CoreTokenFieldTypes.validateTypes(query);

        QueryBuilder builder = getAdapter().query();
        QueryFilter.QueryFilterBuilder filterBuilder = getAdapter().buildFilter().and();

        for (Map.Entry<CoreTokenField, Object> entry : query.entrySet()) {
            CoreTokenField key = entry.getKey();
            Object value = entry.getValue();
            filterBuilder = filterBuilder.attribute(key, value);
        }

        Collection<Token> tokens = builder.withFilter(filterBuilder.build()).execute();
        tokens = decryptTokens(tokens);

        if (DEBUG.messageEnabled()) {
            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "List: {0} Tokens listed",
                    tokens.size()));
        }

        return tokens;
    }

    /**
     * Performs a list operation against the Core Token Service with a predefined filter. This
     * allows more complex filters to be constructed and is intended to be used with the
     * QueryFilter fluent class.
     *
     * @see QueryFilter
     *
     * @param filter A non null OpenDJ LDAP Filter to use to control the results returned.
     * @return A non null, but possible empty collection of Tokens.
     * @throws CoreTokenException If there was an unrecoverable error.
     */
    public Collection<Token> list(Filter filter) throws CoreTokenException {
        return decryptTokens(getAdapter().query().withFilter(filter).execute());
    }

    /**
     * Handles the decrypting of tokens when needed.
     *
     * @param tokens A non null collection of Tokens.
     * @return A new collection of Tokens with their byte contents decrypted if necessary.
     */
    private Collection<Token> decryptTokens(Collection<Token> tokens) {
        if (coreTokenConfig.isTokenEncrypted()) {
            Collection<Token> encryptedTokens = new LinkedList<Token>();
            for (Token token : tokens) {
                encryptedTokens.add(tokenEncryption.decrypt(token));
            }
            return encryptedTokens;
        }
        return tokens;
    }

    /**
     * Perform Service Shutdown.
     */
    public void shutdown() {
        internalShutdown();
        DEBUG.warning(DB_AM_SHUT.get().toString());
    }

    /**
     * Internal Service Shutdown Process.
     */
    protected static void internalShutdown() {
        shutdown = true;
        DEBUG.warning(DB_AM_INT_SHUT.get().toString());
    }

    /**
     * Service Thread Run Process Loop.
     */
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        synchronized (LOCK) {
            while (!shutdown) {
                try {
                    boolean more = deleteExpired();
                    // Trigger a rerun of the loop if it is clear there are more to delete.
                    if (more) {
                        continue;
                    }

                    // Wait for next tick or interrupt.
                    LOCK.wait(coreTokenConfig.getSleepInterval());
                } catch (InterruptedException ie) {
                    // very important, when Exception thrown, Interrupt is actually cleared.
                    // Need to ensure all child loop iterations get killed.
                    Thread.currentThread().interrupt();
                    break;
                } catch (CoreTokenException e) {
                    DEBUG.warning(DB_STR_EX.get().toString(), e);
                } catch (Exception e) {
                    DEBUG.warning(DB_STR_EX.get().toString(), e);
                }
            }
        }
    }

    /**
     * Default Service Method, used to satisfy GeneralTaskRunnable extending.
     *
     * @param obj
     * @return
     */
    public boolean addElement(Object obj) {
        return false;
    }

    /**
     * Default Service Method, used to satisfy GeneralTaskRunnable extending.
     *
     * @param obj
     * @return
     */
    public boolean removeElement(Object obj) {
        return false;
    }

    /**
     * Default Service Method, used to satisfy GeneralTaskRunnable extending.
     *
     * @return
     */
    public boolean isEmpty() {
        return true;
    }


    /**
     * Return Service Run Period.
     *
     * @return long current run period.
     */
    public long getRunPeriod() {
        return coreTokenConfig.getRunPeriod();
    }


    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid User's universal unique ID.
     * @return Map of all Session for the user
     * @throws Exception if there is any problem with accessing the session
     *                   repository.
     */
    public Map<String, Long> getTokensByUUID(String uuid) throws CoreTokenException {
        Collection<Entry> entries;
        Filter filter = getAdapter().buildFilter().and().userId(uuid).build();
        entries = getAdapter().query()
                .withFilter(filter)
                .returnTheseAttributes(CoreTokenField.TOKEN_ID, CoreTokenField.EXPIRY_DATE)
                .executeRawResults();

        if (DEBUG.messageEnabled()) {
            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Querying Sessions by User Id. Found {0} Sessions.\n" +
                    "UUID: {1}",
                    entries.size(),
                    uuid));
        }

        Map<String, Long> sessions = new HashMap<String, Long>();
        for (Entry entry : entries) {
            String sessionId = entry.getAttribute(CoreTokenField.TOKEN_ID.toString()).firstValueAsString();
            String dateString = entry.getAttribute(CoreTokenField.EXPIRY_DATE.toString()).firstValueAsString();

            Calendar timestamp = dataConversion.fromLDAPDate(dateString);
            long epochedSeconds = dataConversion.toEpochedSeconds(timestamp);

            sessions.put(sessionId, epochedSeconds);
        }

        return sessions;
    }

    /**
     * Protected Common Helper Method to Obtain an LDAP Connection
     * from the SMDataLayer Pool.
     *
     * @return LDAPConnection - Obtained Directory Connection from Pool.
     * @throws StoreException
     */
    protected LDAPConnection getDirectoryConnection() throws StoreException {
        LDAPConnection ldapConnection = CTSDataLayer.getConnection();
        if (ldapConnection == null) {
            throw new StoreException("CTSPersistenceStore.prepareCTSPersistenceStore: Unable to Obtain Directory Connection!");
        }
        // Return Obtain Connection from Pool.
        return ldapConnection;
    }

    /**
     * Protected Common Helper Method for external parallel layer components @see CTSDataUtils
     *
     * @param ldapConnection
     * @throws StoreException
     */
    protected void releaseDirectoryConnection(LDAPConnection ldapConnection, LDAPException lastLDAPException) throws StoreException {
        if (ldapConnection != null) {
            // Release the Connection.
            CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
        }
    }

    protected static String getAnyObjectclassFilter() {
        return ANY_OBJECTCLASS_FILTER;
    }

    protected static String getBASE_ROOT_DN() {
        return getFormattedDNString(BASE_ROOT_DN, null, null);
    }

    protected static String getTokenRoot() {
        return getFormattedDNString(TOKEN_ROOT, null, null);
    }

    protected static String getTokenSessionHaRootDn() {
        return getFormattedDNString(TOKEN_SESSION_HA_ROOT_SUFFIX + "," + TOKEN_ROOT, null, null);
    }

    protected static String getTokenSaml2HaRootDn() {
        return getFormattedDNString(TOKEN_SAML2_HA_ROOT_SUFFIX + "," + TOKEN_ROOT, null, null);
    }

    protected static String getTokenOauth2HaRootDn() {
        return getFormattedDNString(TOKEN_OAUTH2_HA_ROOT_SUFFIX + "," + TOKEN_ROOT, null, null);
    }

    protected static String getSessionFailoverHaBaseDn() {
        return getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null);
    }

    protected static String getSaml2HaBaseDn() {
        return getFormattedDNString(SAML2_HA_BASE_DN, null, null);
    }

    protected static String getOauth2HaBaseDn() {
        return getFormattedDNString(OAUTH2_HA_BASE_DN, null, null);
    }

    /**
     * Helper method to correctly format a String with a name,value pair.
     * This uses the include Open Source @see MapFormat Source.
     *
     * @param template
     * @param name     - Can be Null.
     * @param value
     * @return String of Formatted Template with DN Names resolved.
     */
    private static String getFormattedDNString(String template, String name, String value) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(BASE_ROOT_DN_NAME, BASE_ROOT_DN); // Always Resolve our Base Root DN with any Template.
        if ((name != null) && (!name.isEmpty())) {
            map.put(name, value);
        }
        return MapFormat.format(template, map);
    }

    /**
     * Delete all Expired Sessions, within Default Limits.
     *
     * @return True if there are more tokens to delete.
     *
     * @throws CoreTokenException If there was a problem performing the delete.
     */
    private boolean deleteExpired() throws CoreTokenException {
        Calendar nowTimestamp = Calendar.getInstance();

        Filter filter = getAdapter().buildFilter().and().beforeDate(nowTimestamp).build();
        Collection<Entry> entries = getAdapter().query()
                .withFilter(filter)
                .limitResultsTo(coreTokenConfig.getExpiredSessionsSearchLimit())
                .returnTheseAttributes(CoreTokenField.TOKEN_ID)
                .executeRawResults();

        for (Entry entry : entries) {
            Attribute attribute = entry.getAttribute(CoreTokenField.TOKEN_ID.toString());
            String tokenId = attribute.firstValueAsString();
            delete(tokenId);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Delete Expired: {0} expired tokens deleted",
                    entries.size()));
        }

        return entries.size() == coreTokenConfig.getExpiredSessionsSearchLimit();
    }
}
