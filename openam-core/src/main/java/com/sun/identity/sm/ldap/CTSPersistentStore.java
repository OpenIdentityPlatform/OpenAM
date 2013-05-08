/*
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

import java.text.SimpleDateFormat;
import java.util.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.exceptions.EntryAlreadyExistsException;
import com.sun.identity.coretoken.interfaces.AMTokenRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.coretoken.interfaces.AMTokenSAML2Repository;
import com.sun.identity.coretoken.interfaces.OAuth2TokenRepository;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.shared.ldap.*;

import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.model.*;

import com.sun.identity.tools.objects.MapFormat;
import org.forgerock.i18n.LocalizableMessage;

import static org.forgerock.openam.session.ha.i18n.AmsessionstoreMessages.*;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.iplanet.dpro.session.exceptions.NotFoundException;
import com.iplanet.dpro.session.exceptions.StoreException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openam.session.model.AMRootEntity;
import org.forgerock.openam.session.model.DBStatistics;

import org.forgerock.openam.utils.TimeDuration;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.*;

/**
 * Provide Implementation of AMTokenRepository using the internal/external
 * Configuration Directory as OpenAM's Session and Token Store.
 * <p>
 * Having a replicated Directory
 * will provide the underlying Session High Availability and Failover
 * for OpenAM Session Data.
 * </p>
 * <p/>
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 * @author jason.lemay@forgerock.com
 */
public class CTSPersistentStore extends GeneralTaskRunnable
        implements AMTokenRepository, AMTokenSAML2Repository, OAuth2TokenRepository {

    /**
     * Globals public Constants, so not to pollute entire product.
     */
    public static final String FR_FAMRECORD = "frFamRecord";

    /**
     * Globals private Constants, so not to pollute entire product.
     */
    private static final String PKEY_NAMING_ATTR = "pKey";

    private static final String FORMATTED_EXPIRATION_DATE_NAME = "formattedExpirationDate";

    private static final String SAML2_KEY_NAME = "samlKey";

    private static final String OAUTH2_KEY_NAME = "oauth2Key";

    private static final String OBJECTCLASS = "objectClass";

    private static final String ANY_OBJECTCLASS_FILTER = "(" + OBJECTCLASS + Constants.EQUALS + Constants.ASTERISK + ")";

    /**
     * Search Constructs
     */
    private final static String SKEY_FILTER_PRE = "(sKey=";
    private final static String SKEY_FILTER_POST = ")";
    private final static String EXPDATE_FILTER_PRE = "(expirationDate<=";
    private final static String EXPDATE_FILTER_POST = ")";

    /**
     * Singleton Instance
     */
    private static volatile CTSPersistentStore instance = new CTSPersistentStore();

    /**
     * Shared SM Data Layer Accessor.
     */
    private static volatile CTSDataLayer CTSDataLayer;

    /**
     * Debug Logging
     */
    private static Debug DEBUG = SessionService.sessionDebug;

    /**
     * Service Globals
     */
    private static volatile boolean shutdown = false;
    private static volatile boolean ditVerified = false;
    private static volatile int attemptsToVerifyDIT = 0;
    private static Thread storeThread;

    private static final int SLEEP_INTERVAL = 60 * 1000;
    private final static String ID = "CTSPersistentStore";

    private final Object LOCK = new Object();

    /**
     * Grace Period
     */
    private static long gracePeriod = 5 * 60; /* 5 mins in secs */

    /**
     * Configuration Definitions
     */
    static public final String SESSION = "session";
    static public final String SAML2 = "saml2";
    static public final String OAUTH2 = "oauth2";

    private static final String FAMRECORDS_NAMING = "ou=famrecords";
    private static final String OAUTH2TOKENS_NAMING = "ou=" + OAUTH2 + "tokens";

    private static boolean caseSensitiveUUID =
            SystemProperties.getAsBoolean(com.sun.identity.shared.Constants.CASE_SENSITIVE_UUID);

    /**
     * Define Global DN and Container Constants
     */
    // Our default Top Level Root Suffix.
    // This is resolved during Initialization process.
    private static final String BASE_ROOT_DN_NAME = "BASE_ROOT";
    private static String BASE_ROOT_DN;

    // Our default for TOKEN_ROOT_SUFFIX = ou=tokens
    private static final String TOKEN_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_TOKEN_ROOT_SUFFIX,
                    Constants.DEFAULT_TOKEN_ROOT_SUFFIX);

    //  Our default for TOKEN_ROOT = ou=tokens,dc=openam,dc=forgerock,dc=org
    private static final String TOKEN_ROOT = TOKEN_ROOT_SUFFIX +
            Constants.COMMA + "{" + BASE_ROOT_DN_NAME + "}";

    // Our default for TOKEN_SESSION_HA_ROOT_SUFFIX = ou=openam-session
    private static final String TOKEN_SESSION_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_SESSION_HA_ROOT_SUFFIX);

    // Our default for TOKEN_SAML2_HA_ROOT_SUFFIX = ou=openam-saml2
    private static final String TOKEN_SAML2_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_TOKEN_SAML2_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_SAML2_HA_ROOT_SUFFIX);

    // Our default for TOKEN_OAUTH2_HA_ROOT_SUFFIX = ou-openam-oauth2
    private static final String TOKEN_OAUTH2_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_TOKEN_OAUTH2_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_OAUTH2_HA_ROOT_SUFFIX);

    /**
     * Define Session DN Constants
     */
    private static final String SESSION_FAILOVER_HA_BASE_DN =
            FAMRECORDS_NAMING + Constants.COMMA +
                    TOKEN_SESSION_HA_ROOT_SUFFIX + Constants.COMMA + TOKEN_ROOT;

    private static final String SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE =
            PKEY_NAMING_ATTR + Constants.EQUALS + "{" + PKEY_NAMING_ATTR + "}" + Constants.COMMA +
                    SESSION_FAILOVER_HA_BASE_DN;

    /**
     * Session Expiration Filter.
     */
    private final static String TOKEN_EXPIRATION_FILTER_TEMPLATE =
            "(&(" + OBJECTCLASS + Constants.EQUALS + CTSPersistentStore.FR_FAMRECORD +
                    ")" + EXPDATE_FILTER_PRE + "{" + FORMATTED_EXPIRATION_DATE_NAME + "}" + EXPDATE_FILTER_POST + ")";


    /**
     * Define SAML2 DN Constants
     */
    private static final String SAML2_HA_BASE_DN =
            FAMRECORDS_NAMING + Constants.COMMA +
                    TOKEN_SAML2_HA_ROOT_SUFFIX + Constants.COMMA + TOKEN_ROOT;

    private static final String TOKEN_SAML2_HA_ELEMENT_DN_TEMPLATE =
            PKEY_NAMING_ATTR + Constants.EQUALS + "{" + SAML2_KEY_NAME + "}" + Constants.COMMA +
                    SAML2_HA_BASE_DN;

    /**
     * Define OAUTH2 DN Constants
     */
    private static final String OAUTH2_HA_BASE_DN =
            OAUTH2TOKENS_NAMING + Constants.COMMA +
                    TOKEN_OAUTH2_HA_ROOT_SUFFIX + Constants.COMMA + TOKEN_ROOT;

    private static final String TOKEN_OAUTH2_HA_ELEMENT_DN_TEMPLATE =
            OAuth2Constants.Params.ID + Constants.EQUALS + "{" + OAUTH2_KEY_NAME + "}" + Constants.COMMA + OAUTH2_HA_BASE_DN;

    /**
     * Return Attribute Constructs
     */
    private static LinkedHashSet<String> returnAttrs;
    private static String[] returnAttrs_ARRAY;

    private static LinkedHashSet<String> returnAttrs_OAuth2;
    private static String[] returnAttrs_OAuth2_ARRAY;

    private static LinkedHashSet<String> returnAttrs_PKEY_ONLY;
    private static String[] returnAttrs_PKEY_ONLY_ARRAY;

    private static LinkedHashSet<String> returnAttrs_DN_ONLY;
    private static String[] returnAttrs_DN_ONLY_ARRAY;

    private static LinkedHashSet<String> returnAttrs_ID_ONLY;
    private static String[] returnAttrs_ID_ONLY_ARRAY;

    /**
     * Expired Session Search Limit and Date & Time Formatter.
     */
    private static final int DEFAULT_EXPIRED_SESSION_SEARCH_LIMIT = 250;
    private static int EXPIRED_SESSION_SEARCH_LIMIT;
    private static SimpleDateFormat simpleDateFormat = null;

    /**
     * Deferred Operation Queue.
     */
    private static ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>
            amSessionRepositoryDeferredOperationConcurrentLinkedQueue
            = new ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>();

    /**
     * Time period between two successive runs of repository cleanup thread
     * which checks and removes expired records
     */

    private static long cleanUpPeriod = 5 * 60 * 1000; // 5 min in milliseconds

    private static long cleanUpValue = 0;

    public static final String CLEANUP_RUN_PERIOD =
            "com.sun.identity.session.repository.cleanupRunPeriod";

    /**
     * Time period between two successive runs of DBHealthChecker thread which
     * checks for Database availability.
     */
    private static long healthCheckPeriod = 1 * 60 * 1000;

    public static final String HEALTH_CHECK_RUN_PERIOD =
            "com.sun.identity.session.repository.healthCheckRunPeriod";

    /**
     * This period is actual one that is used by the thread. The value is set to
     * the smallest value of cleanUPPeriod and healthCheckPeriod.
     */
    private static long runPeriod = 1 * 60 * 1000; // 1 min in milliseconds

    /**
     * Static Initialization Stanza
     * - Set all Timing Periods.
     */
    static {
        try {
            gracePeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_GRACE_PERIOD, String.valueOf(gracePeriod)));
        } catch (Exception e) {
            DEBUG.error("Invalid value for " + CLEANUP_GRACE_PERIOD
                    + ", using default");
        }

        try {
            cleanUpPeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_RUN_PERIOD, String.valueOf(cleanUpPeriod)));
        } catch (Exception e) {
            DEBUG.error("Invalid value for " + CLEANUP_RUN_PERIOD
                    + ", using default");
        }

        try {
            healthCheckPeriod = Integer
                    .parseInt(SystemProperties.get(HEALTH_CHECK_RUN_PERIOD,
                            String.valueOf(healthCheckPeriod)));
        } catch (Exception e) {
            DEBUG.error("Invalid value for " + HEALTH_CHECK_RUN_PERIOD
                    + ", using default");
        }

        runPeriod = (cleanUpPeriod <= healthCheckPeriod) ? cleanUpPeriod
                : healthCheckPeriod;
        cleanUpValue = cleanUpPeriod;

        // Create and Initialize all Necessary Attribute Linked Sets.
        returnAttrs = new LinkedHashSet<String>();
        returnAttrs.add("dn");
        returnAttrs.add(AMRecordDataEntry.PRI_KEY);
        returnAttrs.add(AMRecordDataEntry.SEC_KEY);
        returnAttrs.add(AMRecordDataEntry.AUX_DATA);
        returnAttrs.add(AMRecordDataEntry.DATA);
        returnAttrs.add(AMRecordDataEntry.SERIALIZED_INTERNAL_SESSION_BLOB);
        returnAttrs.add(AMRecordDataEntry.EXP_DATE);
        returnAttrs.add(AMRecordDataEntry.EXTRA_BYTE_ATTR);
        returnAttrs.add(AMRecordDataEntry.EXTRA_STRING_ATTR);
        returnAttrs.add(AMRecordDataEntry.OPERATION);
        returnAttrs.add(AMRecordDataEntry.SERVICE);
        returnAttrs.add(AMRecordDataEntry.STATE);
        returnAttrs_ARRAY = returnAttrs.toArray(new String[returnAttrs.size()]);

        returnAttrs_PKEY_ONLY = new LinkedHashSet<String>();
        returnAttrs_PKEY_ONLY.add(AMRecordDataEntry.PRI_KEY);
        returnAttrs_PKEY_ONLY_ARRAY = returnAttrs_PKEY_ONLY.toArray(new String[returnAttrs_PKEY_ONLY.size()]);

        returnAttrs_DN_ONLY = new LinkedHashSet<String>();
        returnAttrs_DN_ONLY.add("dn");
        returnAttrs_DN_ONLY_ARRAY = returnAttrs_DN_ONLY.toArray(new String[returnAttrs_DN_ONLY.size()]);

        //create OAuth2 attribute Lined Sets
        returnAttrs_OAuth2 = new LinkedHashSet<String>();
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.EXPIRE_TIME);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.SCOPE);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.PARENT);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.USERNAME);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.REDIRECT_URI);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.ISSUED);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.TOKEN_TYPE);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.REALM);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.ID);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.CLIENT_ID);
        returnAttrs_OAuth2.add(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
        returnAttrs_OAuth2_ARRAY = returnAttrs_OAuth2.toArray(new String[returnAttrs_OAuth2.size()]);

        returnAttrs_ID_ONLY = new LinkedHashSet<String>();
        returnAttrs_ID_ONLY.add(OAuth2Constants.CoreTokenParams.ID);
        returnAttrs_ID_ONLY_ARRAY = returnAttrs_ID_ONLY.toArray(new String[returnAttrs_ID_ONLY.size()]);

        // Set Up Our Expired Search Limit
        try {
            EXPIRED_SESSION_SEARCH_LIMIT =
                    Integer.getInteger(SystemPropertiesManager.get(SYS_PROPERTY_EXPIRED_SEARCH_LIMIT, Integer.toString(DEFAULT_EXPIRED_SESSION_SEARCH_LIMIT)));
        } catch (Exception e) {
            EXPIRED_SESSION_SEARCH_LIMIT = DEFAULT_EXPIRED_SESSION_SEARCH_LIMIT;
        }
        // Set Our Simple Date Formatter, per our data Store.
        simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Proceed With Initialization of Service.
        try {
            // Initialize the Initial Singleton Service Instance.
            initialize();
        } catch (StoreException se) {
            DEBUG.error("CTS Persistent Store Initialization Failed: " + se.getMessage());
            DEBUG.error("CTS Persistent Store requests will be Ignored, until this condition is resolved!");
        }
    } // End of Static Initialization Stanza.

    /**
     * Private restricted to preserve Singleton Instantiation.
     */
    private CTSPersistentStore() {
    }

    /**
     * Provide Service Instance Access to our Singleton
     *
     * @return CTSPersistentStore Singleton Instance.
     */
    public static final CTSPersistentStore getInstance() {
        synchronized (instance) {
            if (!ditVerified) {
                try {
                    if (attemptsToVerifyDIT > 3) {
                        // limit the number of Attempts that can possible be performed.
                        return null;
                    }
                    attemptsToVerifyDIT++;
                    CTSDataUtils ctsDataUtils = new CTSDataUtils(instance);
                    if (!ctsDataUtils.isDITValid()) {
                        DEBUG.warning("Existing CTS DIT Structure is not valid, attempting automatic upgrade of Directory.");
                        if (ctsDataUtils.upgradeDIT()) {
                            DEBUG.warning("CTS DIT Upgrade was Successful.");
                        } else {
                            DEBUG.error("CTS DIT Upgrade was not Successful, CTS persistence will be ignored until DIT fixed.");
                            return null;
                        }
                    } // End of If Upgrade Check.
                    // Set our Flag that our DIT Structure and schema has been verified and allow normal operations.
                    ditVerified = true;
                } catch (StoreException se) {
                    DEBUG.error("Exception occurred verifying the CTS DIT Structure and Schemata, Please Verify Directory Configuration.", se);
                    return null;
                }
            } // End of ditVerified if Check.
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
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                internalShutdown();
            }
        });
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
                    // Delete any expired Sessions up to now.
                    deleteExpired(Calendar.getInstance());
                    if (shutdown) {
                        break;
                    }
                    // Delete any expired SAML2 Artifacts
                    deleteExpiredSAML2Tokens();
                    if (shutdown) {
                        break;
                    }
                    // Delete expired OAuth2 tokens
                    StringBuilder filter = new StringBuilder();
                    filter.append(EXPDATE_FILTER_PRE_OAUTH).append(OAuth2Constants.CoreTokenParams.EXPIRE_TIME)
                          .append(EXPDATE_FILTER_COMPARE).append(System.currentTimeMillis())
                          .append(EXPDATE_FILTER_POST_OAUTH);
                    oauth2DeleteWithFilter(filter.toString());
                    if (shutdown) {
                        break;
                    }
                    // Process any Deferred Operations
                    processDeferredAMSessionRepositoryOperations();
                    if (shutdown) {
                        break;
                    }
                    // Wait for next tick or interrupt.
                    LOCK.wait(SLEEP_INTERVAL);
                } catch (InterruptedException ie) {
                    // very important, when Exception thrown, Interrupt is actually cleared.
                    // Need to ensure all child loop iterations get killed.
                    Thread.currentThread().interrupt();
                    break;
                } catch (StoreException se) {
                    DEBUG.warning(DB_STR_EX.get().toString(), se);
                } catch (JsonResourceException e) {
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
     * Saves session state to the repository If it is a new session (version ==
     * 0) it inserts new record(s) to the repository. For an existing session it
     * updates repository record instead while checking that versions in the
     * InternalSession and in the record match In case of version mismatch or
     * missing record IllegalArgumentException will be thrown
     *
     * @param is reference to <code>InternalSession</code> object being saved.
     * @throws Exception if anything goes wrong.
     */
    public void save(InternalSession is) throws Exception {
        if (is == null) {
            return;
        }
        saveImmediate(is, SESSION);
    }

    /**
     * Private Helper Method to perform the InternalSession Save Immediately,
     * without any queueing of request.
     *
     * @param is - InternalSession to Marshal/Serialize.
     * @throws Exception
     */
    private void saveImmediate(InternalSession is, final String type) throws Exception {
        String messageTag = "CTSPersistenceStore.saveImmediate: ";
        try {
            SessionID sid = is.getID();
            String key = SessionUtils.getEncryptedStorageKey(sid);
            if (key == null) {
                DEBUG.error(messageTag + "Primary Encrypted Key "
                        + " is null, Unable to persist Session.");
                return;
            }
            byte[] serializedInternalSession = SessionUtils.encode(is);
            long expirationTime = is.getExpirationTime() + gracePeriod;
            if ((is.getUUID() == null) || (is.getUUID().isEmpty())) {
                // In the case of an inactivated Session, we may not have a
                // uuid, so perform a set to establish and initialize the
                // InternalSession UUID.
                is.setUUID();
            }
            String uuid = caseSensitiveUUID ? is.getUUID() : is.getUUID().toLowerCase();
            // Create a FAMRecord Object to wrap our Serialized Internal Session
            FAMRecord famRec = new FAMRecord(
                    type, FAMRecord.WRITE, key, expirationTime, uuid,
                    is.getState(), sid.toString(), serializedInternalSession);
            // Construct the Entry's DN and Persist Record
            writeImmediate(famRec, getFormattedDNString(SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE, PKEY_NAMING_ATTR, famRec.getPrimaryKey()));
        } catch (Exception e) {
            DEBUG.error(messageTag + "Failed to Save Session", e);
        }
    }

    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param amRootEntity The record object to store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    public void write(AMRootEntity amRootEntity) throws StoreException {
        if (amRootEntity == null) {
            return;
        }
        // Construct our Entity's DN and Write this AM Root Entity Object now.
        writeImmediate(amRootEntity,
                getFormattedDNString(SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE, PKEY_NAMING_ATTR,
                        amRootEntity.getPrimaryKey()));
    }

    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param amRootEntity The record object to store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    private void writeImmediate(AMRootEntity amRootEntity, String baseDN)
            throws StoreException {
        // Perform a create/store by default and if we fail due to the entry already exists, then perform
        // an Update/modify of the existing Directory Entry.
        try {
            // Assume we are Adding the Entry.
            storeImmediate(amRootEntity, baseDN);
            // Log Action
            logAMRootEntity(amRootEntity, "CTSPersistenceStore.storeImmediate: \nBaseDN:[" + getFormattedDNString(baseDN, null, null).toString() + "] ");
        } catch (EntryAlreadyExistsException entryAlreadyExistsException) {
            // Update / Modify existing Entry.
            updateImmediate(amRootEntity, baseDN);
            // Log Action
            logAMRootEntity(amRootEntity, "CTSPersistenceStore.updateImmediate: \nBaseDN:[" + getFormattedDNString(baseDN, null, null).toString() + "] ");
        }
    }

    /**
     * Perform a Store Immediate to the Directory, since our record does not
     * exist per our up-stream caller.
     *
     * @param record
     * @throws StoreException
     */
    private void storeImmediate(AMRootEntity record, final String baseDN)
            throws StoreException, EntryAlreadyExistsException {
        if ((record == null) || (record.getPrimaryKey() == null)) {
            return;
        }
        String messageTag = "CTSPersistenceStore.storeImmediate: ";
        // Prepare to Marshal our AMRootEntity Object.
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();
        // Ensure our ObjectClass Attributes have been set per our Entity instance Type.
        attrList.addAll(AMRecordDataEntry.getObjectClasses());
        // Initialize the Attribute Set
        LDAPAttributeSet ldapAttributeSet = new LDAPAttributeSet();
        // Obtain a Connection.
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        try {
            ldapConnection = getDirectoryConnection();
            // Iterate over and convert RawAttribute List to actual LDAPAttributes.
            for (RawAttribute rawAttribute : attrList) {
                LDAPAttribute ldapAttribute = new LDAPAttribute(rawAttribute.getAttributeType());
                for (ByteString value : rawAttribute.getValues()) {
                    ldapAttribute.addValue(value.toByteArray());
                }
                ldapAttributeSet.add(ldapAttribute);
            }
            // Add the new Directory Entry.
            LDAPEntry ldapEntry = new LDAPEntry(getFormattedDNString(baseDN, null, null), ldapAttributeSet);
            ldapConnection.add(ldapEntry);
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_SVR_CREATE.get(getFormattedDNString(baseDN, null, null));
                DEBUG.message(messageTag + message.toString());
            }
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            if (ldapException.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                final LocalizableMessage message = DB_SVR_CRE_FAIL.get(getFormattedDNString(baseDN, null, null));
                DEBUG.warning(messageTag + message.toString());
                throw new EntryAlreadyExistsException(message.toString());
            } else if (ldapException.getLDAPResultCode() == LDAPException.OBJECT_CLASS_VIOLATION) {
                // This usually indicates schema has not been applied to the Directory Information Base (DIB) Instance.
                final LocalizableMessage message = OBJECTCLASS_VIOLATION.get(getFormattedDNString(baseDN, null, null));
                DEBUG.warning(messageTag + message.toString());
            } else {
                final LocalizableMessage message = DB_SVR_CRE_FAIL2.get(getFormattedDNString(baseDN, null, null), Integer.toString(ldapException.getLDAPResultCode()));
                DEBUG.warning(message.toString());
                throw new StoreException(messageTag + message.toString(), ldapException);
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Perform an Update Immediate to the Directory, since our record does already
     * exist per our up-stream caller.
     *
     * @param record
     * @throws StoreException
     */
    private void updateImmediate(AMRootEntity record, final String baseDN)
            throws StoreException {
        List<RawModification> modList = createModificationList(record);
        // Initialize.
        String messageTag = "CTSPersistenceStore.updateImmediate: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Convert our RawModification List to a LDAPModificationRequest.
            LDAPModificationSet ldapModificationSet = new LDAPModificationSet();
            for (RawModification rawModification : modList) {
                RawAttribute rawAttribute = rawModification.getAttribute();
                LDAPAttributeSet ldapAttributeSet = new LDAPAttributeSet();
                for (ByteString byteString : rawAttribute.getValues()) {
                    LDAPAttribute ldapAttribute = new LDAPAttribute(rawAttribute.getAttributeType());
                    for (ByteString value : rawAttribute.getValues()) {
                        ldapAttribute.addValue(value.toByteArray());
                    }  // End of Inner For Each Attribute ValuesIteration.
                    ldapModificationSet.add(rawModification.getModificationType().intValue(), ldapAttribute);
                } // End of Inner For Each Attribute Iteration.
            } // End of Inner For Each Modification Iteration.
            // Now Perform the Modification of the Object.
            ldapConnection.modify(getFormattedDNString(baseDN, null, null), ldapModificationSet);
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_SVR_MOD.get(getFormattedDNString(baseDN, null, null));
                DEBUG.message(message.toString());
            }
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(getFormattedDNString(baseDN, null, null));
                DEBUG.warning(message.toString());
            } else if (ldapException.getLDAPResultCode() == LDAPException.OBJECT_CLASS_VIOLATION) {
                // This usually indicates schema has not been applied to the Directory Information Base (DIB) Instance.
                final LocalizableMessage message = OBJECTCLASS_VIOLATION.get(getFormattedDNString(baseDN, null, null));
                DEBUG.warning(message.toString());
            } else {
                // Other Issue Detected.
                final LocalizableMessage message = DB_SVR_MOD_FAIL.get(getFormattedDNString(baseDN, null, null), ldapException.errorCodeToString());
                DEBUG.warning(message.toString());
                throw new StoreException(message.toString());
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Prepare the RAW LDAP Modifications List for Directory
     * consumption.
     *
     * @param record
     * @return List<RawModification>
     * @throws StoreException
     */
    private List<RawModification> createModificationList(AMRootEntity record)
            throws StoreException {
        List<RawModification> mods = new ArrayList<RawModification>();
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();

        for (RawAttribute attr : attrList) {
            RawModification mod = new LDAPModification(ModificationType.REPLACE, attr);
            mods.add(mod);
        }
        return mods;
    }

    /**
     * Deletes a record from the store.
     *
     * @param id The id of the record to delete from the store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     * @throws com.iplanet.dpro.session.exceptions.NotFoundException
     *
     */
    public void delete(String id) throws StoreException, NotFoundException {
        deleteImmediate(id);
    }

    /**
     * Deletes session record from the repository.
     *
     * @param sid session ID.
     * @throws Exception if anything goes wrong.
     */
    public void delete(SessionID sid) throws Exception {
        deleteImmediate(sid);
    }

    /**
     * Private Helper method for Delete Immediately by Session ID.
     *
     * @param sid
     * @throws Exception
     */
    private void deleteImmediate(SessionID sid) throws Exception {
        deleteImmediate(SessionUtils.getEncryptedStorageKey(sid));
    }

    /**
     * Private Helper method for Delete Immediately by Session ID.
     *
     * @param id
     * @throws StoreException
     */
    private void deleteImmediate(String id)
            throws StoreException {
        if ((id == null) || (id.isEmpty())) {
            return;
        }
        // Initialize.
        String messageTag = "CTSPersistenceStore.deleteImmediate: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        String baseDN = getFormattedDNString(SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE, PKEY_NAMING_ATTR, id);
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Delete the Entry, our Entries are flat,
            // so we have no children to contend with, if this
            // changes however, this deletion will need to
            // specify a control to delete child entries.
            ldapConnection.delete(baseDN);
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object, simple Ignore a Not Found,
            // as another OpenAM instance could have purged already and replicated
            // the change across the OpenDJ Bus.
            if (ldapException.getLDAPResultCode() != LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_DEL_FAIL.get(baseDN, ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString());
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Delete all Expired Sessions, within Default Limits.
     *
     * @throws Exception
     */
    public void deleteExpired() throws Exception {
        deleteExpired(Calendar.getInstance());
    }

    /**
     * Delete all records in the store
     * that have an expiry date older than the one specified.
     *
     * @param expirationDate The Calendar Entry depicting the time in which all existing Session
     *                       objects should be deleted if less than this time.
     * @throws StoreException
     */
    public void deleteExpired(Calendar expirationDate) throws StoreException {
        /**
         * Set up our Duration Object, should be performed
         * using AspectJ and a Pointcut.
         */
        TimeDuration timeDuration = new TimeDuration();
        // Check and formulate the Date String for Query.
        if (expirationDate == null) {
            expirationDate = Calendar.getInstance();
        }
        // Formulate the Date String.
        String formattedExpirationDate = getFormattedExpirationDate(expirationDate);
        // Initialize.
        String messageTag = "CTSPersistenceStore.deleteExpired: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        int objectsDeleted = 0;
        timeDuration.start();
        try {
            // Initialize Filter.
            String filter = getFormattedDNString(TOKEN_EXPIRATION_FILTER_TEMPLATE,
                    FORMATTED_EXPIRATION_DATE_NAME, formattedExpirationDate);
            if (DEBUG.messageEnabled()) {
                DEBUG.error(messageTag + "Searching Expired Sessions Older than:["
                        + formattedExpirationDate + "]");
            }
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints to limit number of expired sessions returned during this tick,
            // otherwise we could stall this Service Background thread.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(EXPIRED_SESSION_SEARCH_LIMIT);
            // Perform Search
            LDAPSearchResults searchResults = ldapConnection.search(getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null),
                    LDAPv2.SCOPE_SUB, filter.toString(), returnAttrs_PKEY_ONLY_ARRAY, false, ldapSearchConstraints);
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return;
            }
            // Iterate over results and delete each entry.
            while (searchResults.hasMoreElements()) {
                // Check if in Shutdown Mode.
                if (shutdown) {
                    break;
                }
                LDAPEntry ldapEntry = searchResults.next();
                if (ldapEntry == null) {
                    continue;
                }
                // Process the Entry to perform a delete Against it.
                LDAPAttribute primaryKeyAttribute = ldapEntry.getAttribute(AMRecordDataEntry.PRI_KEY);
                if ((primaryKeyAttribute == null) || (primaryKeyAttribute.size() <= 0) ||
                        (primaryKeyAttribute.getStringValueArray() == null)) {
                    continue;
                }
                // Obtain the primary Key and perform the Deletion.
                String[] values = primaryKeyAttribute.getStringValueArray();
                deleteImmediate(values[0]);
                objectsDeleted++;
            } // End of while loop.
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Determine specific actions per LDAP Return Code.
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // Not Found  No Such Object, Nothing to Delete?
                // Possibly the Expired Session has been already deleted
                // by a peer OpenAM Instance.
                return;
            } else if (ldapException.getLDAPResultCode() == LDAPException.SIZE_LIMIT_EXCEEDED) {
                // Our Size Limit was Exceed, so there are more results, but we have consumed
                // our established limit. @see LDAPSearchConstraints setting above.
                // Let our Next Pass obtain another chuck to delete.
                return;
            } else {
                // Some other type of Error has occurred...
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null), ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString(), ldapException);
                throw new StoreException(messageTag + message.toString(), ldapException);
            }
        } catch (Exception ex) {
            // Are we in Shutdown Mode?
            if (!shutdown) {
                DEBUG.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
            timeDuration.stop();
            if (objectsDeleted > 0) {
                DEBUG.error(messageTag + "Number of Expired Sessions Deleted:[" + objectsDeleted + "], Duration:[" + timeDuration.getDurationToString() + "]");
            }
        }
    }

    /**
     * Retrieve a persisted Internal Session.
     *
     * @param sid session ID
     * @return InternalSession
     * @throws Exception
     */
    public InternalSession retrieve(SessionID sid) throws Exception {
        String messageTag = "CTSPersistenceStore.retrieve: ";
        try {
            String key = SessionUtils.getEncryptedStorageKey(sid);
            // Read the Session Information from the Store.
            // if we have a not found, simply return null,
            // this can occur the first time a new session
            // is created and we try to obtain the ID from the
            // store.
            AMRootEntity amRootEntity = this.read(key);
            InternalSession is = null;
            if ((amRootEntity != null) &&
                    (amRootEntity.getSerializedInternalSessionBlob() != null)) {
                is = (InternalSession) SessionUtils.decode(amRootEntity.getSerializedInternalSessionBlob());
                // Log Action
                logAMRootEntity(amRootEntity, "CTSPersistenceStore.retrieve:\nFound Session ID:[" + key + "] ");
            }
            // Return unMarshaled Internal Session or null.
            return is;
        } catch (NotFoundException nfe) {
            // Not performing any message generation here will limit the verbosity
            // as during session recovery, multiple attempts
            // are made to check for session existence and during high session creation,
            // logs can easily spill and cause a performance degradation.
            // Simply return null as upstream will handle that condition.
            return null;
        } catch (Exception e) {
            // Issue other than a Not Found Condition.
            DEBUG.error(messageTag + "Failed Retrieving Session ID:[" + sid + "]", e);
            return null;
        }
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
    public Map<String, Long> getSessionsByUUID(String uuid) throws SessionException {
        try {
            return getRecordCount(uuid);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Read the Persisted Session Record from the Store.
     *
     * @param id The primary key of the record to find
     * @return
     * @throws NotFoundException
     * @throws StoreException
     */
    public AMRootEntity read(String id)
            throws NotFoundException, StoreException {
        if ((id == null) || (id.isEmpty())) {
            return null;
        }
        // Establish our DN
        String baseDN = getFormattedDNString(SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE, PKEY_NAMING_ATTR, id);
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;
        String messageTag = "CTSPersistenceStore.read: ";
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            searchResults = ldapConnection.search(baseDN,
                    LDAPv2.SCOPE_BASE, ANY_OBJECTCLASS_FILTER, returnAttrs_ARRAY, false, new LDAPSearchConstraints());
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return null;
            }
            // UnMarshal LDAP Entry to a Map.
            LDAPEntry ldapEntry = searchResults.next();
            LDAPAttributeSet attributes = ldapEntry.getAttributeSet();
            Map<String, Set<String>> results =
                    CTSEmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
            // UnMarshal
            AMRecordDataEntry dataEntry = new AMRecordDataEntry(baseDN.toString(), AMRecord.READ, results);
            // Log Action
            logAMRootEntity(dataEntry.getAMRecord(), messageTag + "\nBaseDN:[" + baseDN.toString() + "] ");
            // Return UnMarshaled Object
            return dataEntry.getAMRecord();
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found, No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // This can be due to the session has expired and removed from the store.
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                DEBUG.message(messageTag + message.toString());
                throw new NotFoundException(messageTag + message.toString());
            }
            final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, ldapException.errorCodeToString());
            DEBUG.error(messageTag + message.toString());
            throw new StoreException(messageTag + message.toString(), ldapException);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Read with Security Key.
     *
     * @param id The secondary key on which to search the store
     * @return
     * @throws StoreException
     * @throws NotFoundException
     */
    public Set<String> readWithSecKey(String id)
            throws StoreException, NotFoundException {
        if ((id == null) || (id.isEmpty())) {
            return null;
        }
        StringBuilder filter = new StringBuilder();
        filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);
        String messageTag = "CTSPersistenceStore.readWithSecKey: ";
        if (DEBUG.messageEnabled()) {
            DEBUG.message(messageTag + "Attempting Read of BaseDN:[" + getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null) + "]");
        }
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;
        Set<String> result = new HashSet<String>();
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints with no return limits.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(0);
            // Perform the Search.
            searchResults = ldapConnection.search(getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null),
                    LDAPv2.SCOPE_ONE, filter.toString(), returnAttrs_ARRAY, false, ldapSearchConstraints);
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return null;
            }
            // UnMarshal LDAP Entry to a Map, only pull in a Single Entry.
            LDAPEntry ldapEntry = searchResults.next();
            LDAPAttributeSet attributes = ldapEntry.getAttributeSet();
            Map<String, Set<String>> results =
                    CTSEmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
            // UnMarshal
            AMRecordDataEntry dataEntry = new AMRecordDataEntry(filter.toString(), AMRecord.READ, results);
            // Log Action
            logAMRootEntity(dataEntry.getAMRecord(), messageTag + "\nBaseDN:[" + filter.toString() + "] ");
            // Return UnMarshaled Object(s)
            Set<String> value = results.get(AMRecordDataEntry.DATA);
            if (value != null && !value.isEmpty()) {
                for (String v : value) {
                    result.add(v);
                }
            }
            // Show Success
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_R_SEC_KEY_OK.get(id, Integer.toString(result.size()));
                DEBUG.message(messageTag + message.toString());
            }
            // return result
            return result;
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // This can be due to the session has expired and removed from the store.
                final LocalizableMessage message = DB_ENT_NOT_P.get(filter);
                DEBUG.message(messageTag + message.toString());
                throw new NotFoundException(messageTag + message.toString());
            }
            final LocalizableMessage message = DB_ENT_ACC_FAIL.get(filter, ldapException.errorCodeToString());
            DEBUG.error(messageTag + message.toString());
            throw new StoreException(messageTag + message.toString(), ldapException);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Get the number of Session Objects per our secondary key which is the
     * DN Owner of the Session Objects.
     *
     * @param id
     * @return Map<String, Long>
     * @throws StoreException
     */
    public Map<String, Long> getRecordCount(String id)
            throws StoreException {
        if ((id == null) || (id.isEmpty())) {
            return null;
        }
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;
        String messageTag = "CTSPersistenceStore.getRecordCount: ";
        try {
            StringBuilder filter = new StringBuilder();
            // Create Search Filter for our Secondary Key.
            filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints with no return limits.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(0);
            // Perform the Search.
            searchResults = ldapConnection.search(getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null),
                    LDAPv2.SCOPE_ONE, filter.toString(), returnAttrs_ARRAY, false, ldapSearchConstraints);
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return null;
            }
            // Process our Results.
            Map<String, Long> result = new HashMap<String, Long>();
            while (searchResults.hasMoreElements()) {
                LDAPEntry ldapEntry = searchResults.next();
                if (ldapEntry == null) {
                    continue;
                }
                // Process the Entries Attribute Set.
                LDAPAttributeSet attributes = ldapEntry.getAttributeSet();
                // Convert LDAP attributes to a simple Map.
                Map<String, Set<String>> results =
                        CTSEmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);

                // Get the AuxData.
                String key = "";
                Long expDate = new Long(0);
                Set<String> value = results.get(AMRecordDataEntry.AUX_DATA);
                if (value != null && !value.isEmpty()) {
                    for (String v : value) {
                        key = v;
                    }
                }
                // Get our Expiration Date.
                value = results.get(AMRecordDataEntry.EXP_DATE);
                if (value != null && !value.isEmpty()) {
                    for (String v : value) {
                        expDate = AMRecordDataEntry.toAMDateFormat(v);
                    }
                }
                result.put(key, expDate);
            }
            // Return our Results.
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_GET_REC_CNT_OK.get(id, Integer.toString(result.size()));
                DEBUG.message(messageTag + message.toString());
            }
            return result;
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null));
                DEBUG.message(messageTag + message.toString());
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(getFormattedDNString(SESSION_FAILOVER_HA_BASE_DN, null, null),
                        ldapException.errorCodeToString());
                DEBUG.warning(messageTag + message.toString());
                throw new StoreException(messageTag + message.toString());
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Get DB Statistics
     *
     * @return
     */
    public DBStatistics getDBStatistics() {
        DBStatistics stats = DBStatistics.getInstance();
        // TODO Build out proper Statistics.
        return stats;
    }

    /**
     * Return Service Run Period.
     *
     * @return long current run period.
     */
    public long getRunPeriod() {
        return runPeriod;
    }

    // ***************************************************************************************************
    //
    // AMTokenSAML2Repository Implementation Methods.
    //
    // These methods are called directly from the CTSPersistentSAML2Store, if that is the configured and
    // desired implementation.
    //
    // ***************************************************************************************************

    /**
     * Retrieves existing SAML2 object from persistent Repository.
     *
     * @param samlKey primary key
     * @return SAML2 object, if failed, return null.
     */
    public Object retrieveSAML2Token(String samlKey) throws StoreException {
        // Initialize.
        final String messageTag = "CTSPersistenceStore.retrieveSAML2Token: ";
        // Arguments Valid?
        if ((samlKey == null) || (samlKey.isEmpty())) {
            DEBUG.error(messageTag + "Unable to Retrieve SAML2 Token Object, as Primary SAML2 Key was not provided!");
            return null;
        }
        // Establish our DN
        String baseDN = getFormattedDNString(TOKEN_SAML2_HA_ELEMENT_DN_TEMPLATE, SAML2_KEY_NAME, AMRecordDataEntry.encodeKey(samlKey));
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            searchResults = ldapConnection.search(baseDN,
                    LDAPv2.SCOPE_BASE, ANY_OBJECTCLASS_FILTER, returnAttrs_ARRAY, false, new LDAPSearchConstraints());
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return null;
            }
            // UnMarshal LDAP Entry to a Map.
            LDAPEntry ldapEntry = searchResults.next();
            LDAPAttributeSet attributes = ldapEntry.getAttributeSet();
            Map<String, Set<String>> results =
                    CTSEmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
            // UnMarshal
            AMRecordDataEntry dataEntry = new AMRecordDataEntry(baseDN.toString(), AMRecord.READ, results);
            // Log Action
            logAMRootEntity(dataEntry.getAMRecord(), messageTag + "\nBaseDN:[" + baseDN.toString() + "] ");
            // Return UnMarshaled Object
            return dataEntry.getAMRecord();
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found, No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // This can be due to the session has expired and removed from the store.
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                DEBUG.message(messageTag + message.toString());
                return null;
            }
            final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, ldapException.errorCodeToString());
            DEBUG.error(messageTag + message.toString());
            return null;
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Retrives a list of existing SAML2 object from persistent Repository with Secondary Key.
     *
     * @param secKey Secondary Key
     * @return SAML2 object, if failed, return null.
     */
    public List<Object> retrieveSAML2TokenWithSecondaryKey(String secKey) throws StoreException {
        // Initialize.
        final String messageTag = "CTSPersistenceStore.retrieveSAML2TokenWithSecondaryKey: ";
        // Arguments Valid?
        if ((secKey == null) || (secKey.isEmpty())) {
            DEBUG.error(messageTag + "Unable to Retrieve SAML2 Token Object, as Secondary SAML2 Key was not provided!");
            return null;
        }
        StringBuilder filter = new StringBuilder();
        filter.append(SKEY_FILTER_PRE).append(AMRecordDataEntry.encodeKey(secKey)).append(SKEY_FILTER_POST);
        if (DEBUG.messageEnabled()) {
            DEBUG.message(messageTag + "Attempting Read of BaseDN:[" + getFormattedDNString(SAML2_HA_BASE_DN, null, null) + "]");
        }
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;

        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints with no return limits.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(0);
            // Perform the Search.
            searchResults = ldapConnection.search(getFormattedDNString(SAML2_HA_BASE_DN, null, null),
                    LDAPv2.SCOPE_ONE, filter.toString(), returnAttrs_ARRAY, false, ldapSearchConstraints);
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return null;
            }
            // UnMarshal LDAP Entry to a Map, only pull in a Single Entry.
            LDAPEntry ldapEntry = searchResults.next();
            LDAPAttributeSet attributes = ldapEntry.getAttributeSet();
            Map<String, Set<String>> attributeMapResults =
                    CTSEmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
            // UnMarshal
            AMRecordDataEntry dataEntry = new AMRecordDataEntry(filter.toString(), AMRecord.READ, attributeMapResults);
            // Log Action
            logAMRootEntity(dataEntry.getAMRecord(), messageTag + "\nBaseDN:[" + filter.toString() + "] ");
            // return result
            List<Object> results = new ArrayList<Object>(1);
            results.add(dataEntry.getAMRecord());
            return results;
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // This can be due to the session has expired and removed from the store.
                final LocalizableMessage message = DB_ENT_NOT_P.get(filter);
                DEBUG.message(messageTag + message.toString());
                return null;
            }
            final LocalizableMessage message = DB_ENT_ACC_FAIL.get(filter, ldapException.errorCodeToString());
            DEBUG.error(messageTag + message.toString());
            throw new StoreException(messageTag + message.toString(), ldapException);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Saves SAML2 Token and Marshals SAML2 Object into the SAML2 Repository, for subsequent use by the owner.
     *
     * @param samlKey        primary key
     * @param samlObj        saml object such as Response, IDPSession
     * @param expirationTime expiration time
     * @param secKey         Secondary Key
     */
    public void saveSAML2Token(String samlKey, Object samlObj, long expirationTime, String secKey) {
        // Initialize.
        final String messageTag = "CTSPersistenceStore.saveSAML2Token: ";
        // Arguments Valid?
        if ((samlKey == null) || (samlKey.isEmpty())) {
            DEBUG.error(messageTag + "Unable to Persist SAML2 Token Object, as Primary SAML2 Key was not provided!");
            return;
        } else if (samlObj == null) {
            DEBUG.error(messageTag + "Unable to Persist SAML2 Token Object, as Object was not provided or null!");
            return;
        }
        // Now Marshal our Object and establish a FAMRecord for this Token Instance.
        try {
            byte[] serializedInternalSession = SessionUtils.encode(samlObj);
            // Create a FAMRecord Object to wrap our SAML2 Object.
            FAMRecord famRec = new FAMRecord(
                    SAML2, FAMRecord.WRITE, AMRecordDataEntry.encodeKey(samlKey), expirationTime,
                    ((secKey == null) ? secKey : AMRecordDataEntry.encodeKey(secKey)),
                    1, null, serializedInternalSession);
            // Construct the Entry's DN and Persist Record
            writeImmediate(famRec, getFormattedDNString(TOKEN_SAML2_HA_ELEMENT_DN_TEMPLATE, SAML2_KEY_NAME, famRec.getPrimaryKey()));
        } catch (Exception e) {
            DEBUG.error("Failed to Save SAML2 Token", e);
        }
    }

    /**
     * Deletes the SAML2 object by given primary key from the repository
     *
     * @param samlKey primary key
     */
    public void deleteSAML2Token(String samlKey) throws StoreException {
        deleteSAML2Token(samlKey, true);
    }

    /**
     * Deletes the SAML2 object by given primary key from the repository
     *
     * @param samlKey          primary key
     * @param encodePrimaryKey
     */
    private void deleteSAML2Token(String samlKey, boolean encodePrimaryKey) throws StoreException {
        final String messageTag = "CTSPersistenceStore.deleteSAML2Token: ";
        // Arguments Valid?
        if ((samlKey == null) || (samlKey.isEmpty())) {
            DEBUG.error(messageTag + "Unable to Delete SAML2 Token Object, as Primary SAML2 Key was not provided!");
            return;
        }
        // Initialize.
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        String baseDN = getFormattedDNString(TOKEN_SAML2_HA_ELEMENT_DN_TEMPLATE, SAML2_KEY_NAME,
                ((encodePrimaryKey) ? AMRecordDataEntry.encodeKey(samlKey) : samlKey));
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Delete the Entry, our Entries are flat,
            // so we have no children to contend with, if this
            // changes however, this deletion will need to
            // specify a control to delete child entries.
            ldapConnection.delete(baseDN);
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object, simple Ignore a Not Found,
            // as another OpenAM instance could have purged already and replicated
            // the change across the OpenDJ Bus.
            if (ldapException.getLDAPResultCode() != LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_DEL_FAIL.get(baseDN, ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString());
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * Deletes expired SAML2 object from the repository
     */
    public void deleteExpiredSAML2Tokens() throws StoreException {
        // Initialize.
        final String messageTag = "CTSPersistenceStore.deleteExpiredSAML2Tokens: ";
        /**
         * Set up our Duration Object, should be performed
         * using AspectJ and a Pointcut.
         */
        TimeDuration timeDuration = new TimeDuration();
        // Formulate the Date String.
        String formattedExpirationDate = getFormattedExpirationDate(Calendar.getInstance());
        // Initialize.
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        int objectsDeleted = 0;
        timeDuration.start();
        try {
            // Initialize Filter.
            String filter = getFormattedDNString(TOKEN_EXPIRATION_FILTER_TEMPLATE, FORMATTED_EXPIRATION_DATE_NAME, formattedExpirationDate);
            if (DEBUG.messageEnabled()) {
                DEBUG.error(messageTag + "Searching Expired Sessions Older than:["
                        + formattedExpirationDate + "]");
            }
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints to limit number of expired sessions returned during this tick,
            // otherwise we could stall this Service Background thread.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(EXPIRED_SESSION_SEARCH_LIMIT);
            // Perform Search
            LDAPSearchResults searchResults = ldapConnection.search(getFormattedDNString(SAML2_HA_BASE_DN, null, null),
                    LDAPv2.SCOPE_SUB, filter.toString(), returnAttrs_PKEY_ONLY_ARRAY, false, ldapSearchConstraints);
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return;
            }
            // Iterate over results and delete each entry.
            while (searchResults.hasMoreElements()) {
                // Check if in Shutdown Mode.
                if (shutdown) {
                    break;
                }
                LDAPEntry ldapEntry = searchResults.next();
                if (ldapEntry == null) {
                    continue;
                }
                // Process the Entry to perform a delete Against it.
                LDAPAttribute primaryKeyAttribute = ldapEntry.getAttribute(AMRecordDataEntry.PRI_KEY);
                if ((primaryKeyAttribute == null) || (primaryKeyAttribute.size() <= 0) ||
                        (primaryKeyAttribute.getStringValueArray() == null)) {
                    continue;
                }
                // Obtain the primary Key and perform the Deletion.
                String[] values = primaryKeyAttribute.getStringValueArray();
                deleteSAML2Token(values[0], false); // Do not to encode, as our Primary Key is already encoded from Store.
                objectsDeleted++;
            } // End of while loop.
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Determine specific actions per LDAP Return Code.
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // Not Found  No Such Object, Nothing to Delete?
                // Possibly the Expired Session has been already deleted
                // by a peer OpenAM Instance.
                return;
            } else if (ldapException.getLDAPResultCode() == LDAPException.SIZE_LIMIT_EXCEEDED) {
                // Our Size Limit was Exceed, so there are more results, but we have consumed
                // our established limit. @see LDAPSearchConstraints setting above.
                // Let our Next Pass obtain another chuck to delete.
                return;
            } else {
                // Some other type of Error has occurred...
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(getFormattedDNString(SAML2_HA_BASE_DN, null, null), ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString(), ldapException);
                throw new StoreException(messageTag + message.toString(), ldapException);
            }
        } catch (Exception ex) {
            // Are we in Shutdown Mode?
            if (!shutdown) {
                DEBUG.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
            timeDuration.stop();
            if (objectsDeleted > 0) {
                DEBUG.error(messageTag + "Number of Expired SAML2 Artifacts Deleted:[" + objectsDeleted + "], Duration:[" + timeDuration.getDurationToString() + "]");
            }
        }

    }

    // ***************************************************************************************************
    //
    // Private Helper Methods.
    //
    // ***************************************************************************************************

    /**
     * Process any and all deferred AM Session Repository Operations.
     */
    private synchronized void processDeferredAMSessionRepositoryOperations() {
        int count = 0;
        ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>
                tobeRemoved
                = new ConcurrentLinkedQueue<AMSessionRepositoryDeferredOperation>();
        for (AMSessionRepositoryDeferredOperation amSessionRepositoryDeferredOperation :
                amSessionRepositoryDeferredOperationConcurrentLinkedQueue) {
            count++;
            // Check if in Shutdown Mode.
            if (shutdown) {
                break;
            }

            // TODO -- Build out Implementation to invoke Session Repository Events.
            // TODO -- Add Statistics Gathering for Post to Admin Statistics View.

            // Place Entry in Collection to be removed from Queue.
            tobeRemoved.add(amSessionRepositoryDeferredOperation);
        }
        // Remove all entries processed in the Queue.
        amSessionRepositoryDeferredOperationConcurrentLinkedQueue.removeAll(tobeRemoved);
        if (count > 0) {
            DEBUG.warning("Performed " + count + " Deferred Operations.");
        }
    }

    /**
     * Logging Helper Method.
     *
     * @param amRootEntity
     * @param message
     */
    private void logAMRootEntity(AMRootEntity amRootEntity, String message) {
        // Set to Message to Error to see messages.
        if (DEBUG.messageEnabled()) {
            DEBUG.message(
                    ((message != null) && (!message.isEmpty()) ? message : "") +
                            "\nService:[" + amRootEntity.getService() + "]," +
                            "\n     Op:[" + amRootEntity.getOperation() + "]," +
                            "\n     PK:[" + amRootEntity.getPrimaryKey() + "]," +
                            "\n     SK:[" + amRootEntity.getSecondaryKey() + "]," +
                            "\n  State:[" + amRootEntity.getState() + "]," +
                            "\nExpTime:[" + amRootEntity.getExpDate() + " = "
                            + getDate(amRootEntity.getExpDate() * 1000) + "]," +
                            "\n     IS:[" + (
                            (amRootEntity.getSerializedInternalSessionBlob() != null) ?
                                    amRootEntity.getSerializedInternalSessionBlob().length + " bytes]" : "null]") +
                            "\n   Data:[" + (
                            (amRootEntity.getData() != null) ?
                                    amRootEntity.getData().length() + " bytes]" : "null]") +
                            "\nAuxData:[" + (
                            (amRootEntity.getAuxData() != null) ?
                                    amRootEntity.getAuxData().length() + " bytes]" : "null].")
            );
        }
    }

    /**
     * Return current Time in Seconds.
     *
     * @return long Time in Seconds.
     */
    private static long nowInSeconds() {
        return Calendar.getInstance().getTimeInMillis() / 1000;
    }

    /**
     * Return specified Milliseconds in a Date Object.
     *
     * @param milliseconds
     * @return Date
     */
    private static Date getDate(long milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(milliseconds);
        return cal.getTime();
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

    /**
     * Private helper method to properly format the Expiration Date
     * for a proper LDAP Date Attribute Query.
     *
     * @param expirationDate
     * @return String - representing the Formatted Date String for Query.
     */
    private static String getFormattedExpirationDate(Calendar expirationDate) {
        return simpleDateFormat.format(expirationDate.getTime());
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

    // ***************************************************************************************************
    //
    // OAuth2TokenRepository Implementation Methods.
    //
    // ***************************************************************************************************


    /**
     * {@inheritDoc}
     */
    public JsonValue oauth2Create(JsonValue request) throws JsonResourceException {

        if ((request == null) || (request.size() == 0)) {
            return null;
        }
        String messageTag = "CTSPersistenceStore.oauth2Create: ";

        TokenDataEntry token = new TokenDataEntry(request);
        List<RawAttribute> attrList = token.getAttrList();
        attrList.addAll(token.getObjectClasses());

        String dn = getFormattedDNString(TOKEN_OAUTH2_HA_ELEMENT_DN_TEMPLATE, OAUTH2_KEY_NAME, token.getDN());

        LDAPAttributeSet ldapAttributeSet = new LDAPAttributeSet();
        // Obtain a Connection.
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        try {
            ldapConnection = getDirectoryConnection();
            // Iterate over and convert RawAttribute List to actual LDAPAttributes.
            for (RawAttribute rawAttribute : attrList) {
                LDAPAttribute ldapAttribute = new LDAPAttribute(rawAttribute.getAttributeType());
                for (ByteString value : rawAttribute.getValues()) {
                    ldapAttribute.addValue(value.toByteArray());
                }
                ldapAttributeSet.add(ldapAttribute);
            }
            // Add the new Directory Entry.
            LDAPEntry ldapEntry = new LDAPEntry(dn, ldapAttributeSet);
            ldapConnection.add(ldapEntry);
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_SVR_CREATE.get(dn);
                DEBUG.message(messageTag + message.toString());
            }
            request.get("value").put(OAuth2Constants.Params.ID, token.getDN());
            return new JsonValue(request.get("values"));
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            if (ldapException.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                final LocalizableMessage message = DB_SVR_CRE_FAIL.get(dn);
                DEBUG.warning(messageTag + message.toString());
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, message.toString());
            } else if (ldapException.getLDAPResultCode() == LDAPException.OBJECT_CLASS_VIOLATION) {
                // This usually indicates schema has not been applied to the Directory Information Base (DIB) Instance.
                final LocalizableMessage message = OBJECTCLASS_VIOLATION.get(dn);
                DEBUG.warning(messageTag + message.toString());
            } else {
                final LocalizableMessage message = DB_SVR_CRE_FAIL2.get(dn, Integer.toString(ldapException.getLDAPResultCode()));
                DEBUG.warning(message.toString());
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, messageTag + message.toString(), ldapException);
            }
        } catch (StoreException e) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getMessage(), e);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue oauth2Read(JsonValue request) throws JsonResourceException {

        if ((request == null) || (request.size() == 0)) {
            return null;
        }
        // Establish our DN
        String dn = request.get(OAuth2Constants.CoreTokenParams.ID).required().asString();
        String baseDN = getFormattedDNString(TOKEN_OAUTH2_HA_ELEMENT_DN_TEMPLATE, OAUTH2_KEY_NAME, dn);
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;
        String messageTag = "CTSPersistenceStore.oauth2Read: ";
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            searchResults = ldapConnection.search(baseDN,
                    LDAPv2.SCOPE_BASE, ANY_OBJECTCLASS_FILTER, returnAttrs_OAuth2_ARRAY, false, new LDAPSearchConstraints());
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return null;
            }
            // UnMarshal LDAP Entry to a Map.
            LDAPEntry ldapEntry = searchResults.next();
            LDAPAttributeSet attributes = ldapEntry.getAttributeSet();
            Map<String, Set<String>> results =
                    CTSEmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
            // UnMarshal
            AMRecordDataEntry dataEntry = new AMRecordDataEntry(baseDN, AMRecord.READ, results);
            // Log Action
            logAMRootEntity(dataEntry.getAMRecord(), messageTag + "\nBaseDN:[" + baseDN + "] ");
            // Return UnMarshaled Object
            return new JsonValue(results);
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found, No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // This can be due to the session has expired and removed from the store.
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                DEBUG.message(messageTag + message.toString());
                throw new JsonResourceException(JsonResourceException.NOT_FOUND, messageTag + message.toString());
            }
            final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, ldapException.errorCodeToString());
            DEBUG.error(messageTag + message.toString());
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, messageTag + message.toString(), ldapException);
        } catch (StoreException e) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getMessage(), e);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue oauth2Update(JsonValue request) throws JsonResourceException {
        return oauth2Create(request);
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue oauth2Delete(JsonValue request) throws JsonResourceException {
        if ((request == null) || (request.size() == 0)) {
            return null;
        }

        String id = request.get(OAuth2Constants.CoreTokenParams.ID).required().asString();
        String dn = getFormattedDNString(TOKEN_OAUTH2_HA_ELEMENT_DN_TEMPLATE, OAUTH2_KEY_NAME, id);

        String messageTag = "CTSPersistenceStore.oauth2Delete: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Delete the Entry, our Entries are flat,
            // so we have no children to contend with, if this
            // changes however, this deletion will need to
            // specify a control to delete child entries.
            ldapConnection.delete(dn);
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object, simple Ignore a Not Found,
            // as another OpenAM instance could have purged already and replicated
            // the change across the OpenDJ Bus.
            if (ldapException.getLDAPResultCode() != LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_DEL_FAIL.get(dn, ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString());
            }
        } catch (StoreException e) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getMessage(), e);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }

        return new JsonValue(null);
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue oauth2Query(JsonValue request) throws JsonResourceException {
        if ((request == null) || (request.size() == 0)) {
            return null;
        }

        Map<String, Object> filters = (Map<String, Object>) request.get("params").required().asMap().get("filter");
        Set<Map<String, Set<String>>> tokens = new HashSet<Map<String, Set<String>>>();
        StringBuilder filter;
        if (filters == null || filters.isEmpty()) {
            filter = null;
        } else {
            filter = new StringBuilder();
            filter.append("(&");
            for (String key : filters.keySet()) {
                filter.append("(").append(key.replace("_", "")).append(Constants.EQUALS)
                        .append(filters.get(key).toString()).append(")");
            }
            filter.append(")");
        }
        StringBuilder baseDN = new StringBuilder();
        baseDN.append(getFormattedDNString(OAUTH2_HA_BASE_DN, null, null));

        String messageTag = "CTSPersistenceStore.oauth2Query: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        int objectsDeleted = 0;
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints to limit number of expired sessions returned during this tick,
            // otherwise we could stall this Service Background thread.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(EXPIRED_SESSION_SEARCH_LIMIT);
            // Perform Search
            LDAPSearchResults searchResults = ldapConnection.search(baseDN.toString(),
                    LDAPv2.SCOPE_SUB, filter.toString(), returnAttrs_OAuth2_ARRAY, false, ldapSearchConstraints);
            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return new JsonValue(null);
            }
            // Iterate over results and delete each entry.
            while (searchResults.hasMoreElements()) {
                // Check if in Shutdown Mode.
                if (shutdown) {
                    break;
                }
                LDAPEntry ldapEntry = searchResults.next();
                if (ldapEntry == null) {
                    continue;
                }
                Map<String, Set<String>> results =
                        EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(ldapEntry.getAttributeSet());
                tokens.add(results);
            } // End of while loop.
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Determine specific actions per LDAP Return Code.
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // Not Found  No Such Object, Nothing to Delete?
                // Possibly the Expired Session has been already deleted
                // by a peer OpenAM Instance.
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR);
            } else if (ldapException.getLDAPResultCode() == LDAPException.SIZE_LIMIT_EXCEEDED) {
                // Our Size Limit was Exceed, so there are more results, but we have consumed
                // our established limit. @see LDAPSearchConstraints setting above.
                // Let our Next Pass obtain another chuck to delete.
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR);
            } else {
                // Some other type of Error has occurred...
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(getFormattedDNString(OAUTH2_HA_BASE_DN, null, null), ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString(), ldapException);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, messageTag + message.toString(), ldapException);
            }
        } catch (Exception ex) {
            // Are we in Shutdown Mode?
            if (!shutdown) {
                DEBUG.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }

        return new JsonValue(tokens);
    }

    /**
     * {@inheritDoc}
     */
    public void oauth2DeleteWithFilter(String filter) throws JsonResourceException {

        StringBuilder baseDN = new StringBuilder();
        baseDN.append(getFormattedDNString(OAUTH2_HA_BASE_DN, null, null));

        String messageTag = "CTSPersistenceStore.oauth2DeleteExpired: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        int objectsDeleted = 0;
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Create our Search Constraints to limit number of expired sessions returned during this tick,
            // otherwise we could stall this Service Background thread.
            LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
            ldapSearchConstraints.setMaxResults(EXPIRED_SESSION_SEARCH_LIMIT);
            // Perform Search
            LDAPSearchResults searchResults = ldapConnection.search(baseDN.toString(),
                    LDAPv2.SCOPE_SUB, filter.toString(), returnAttrs_ID_ONLY_ARRAY, false, ldapSearchConstraints);

            // Anything Found?
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return;
            }
            // Iterate over results and delete each entry.
            while (searchResults.hasMoreElements()) {
                LDAPEntry ldapEntry = searchResults.next();
                if (ldapEntry == null) {
                    continue;
                }
                // Process the Entry to perform a delete Against it.
                LDAPAttribute primaryKeyAttribute = ldapEntry.getAttribute(OAuth2Constants.Params.ID);
                if ((primaryKeyAttribute == null) || (primaryKeyAttribute.size() <= 0) ||
                        (primaryKeyAttribute.getStringValueArray() == null)) {
                    continue;
                }
                // Obtain the primary Key and perform the Deletion.
                String[] values = primaryKeyAttribute.getStringValueArray();
                oauth2Delete(values[0]);
                objectsDeleted++;
            } // End of while loop.
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Determine specific actions per LDAP Return Code.
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                // Not Found  No Such Object, Nothing to Delete?
                // Possibly the Expired Session has been already deleted
                // by a peer OpenAM Instance.
                return;
            } else if (ldapException.getLDAPResultCode() == LDAPException.SIZE_LIMIT_EXCEEDED) {
                // Our Size Limit was Exceed, so there are more results, but we have consumed
                // our established limit. @see LDAPSearchConstraints setting above.
                // Let our Next Pass obtain another chuck to delete.
                return;
            } else {
                // Some other type of Error has occurred...
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(getFormattedDNString(OAUTH2_HA_BASE_DN, null, null), ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString(), ldapException);
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, messageTag + message.toString(), ldapException);
            }
        } catch (Exception ex) {
            // Are we in Shutdown Mode?
            if (!shutdown) {
                DEBUG.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            } else {
                DEBUG.error(DB_ENT_EXP_FAIL.get().toString(), ex);
            }
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
            if (objectsDeleted > 0) {
                //if (DEBUG.messageEnabled()) {  // TODO -- Uncomment to limit verbosity.
                DEBUG.error(messageTag + "Number of Expired Sessions Deleted:[" + objectsDeleted + "]");
                //}
            }
        }

        return;
    }


    /**
     * {@inheritDoc}
     */
    public void oauth2Delete(String id) throws JsonResourceException {

        if ((id == null) || (id.isEmpty())) {
            return;
        }
        // Initialize.
        String messageTag = "CTSPersistenceStore.oauth2Delete: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        String baseDN = getFormattedDNString(TOKEN_OAUTH2_HA_ELEMENT_DN_TEMPLATE, OAUTH2_KEY_NAME, id);
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Delete the Entry, our Entries are flat,
            // so we have no children to contend with, if this
            // changes however, this deletion will need to
            // specify a control to delete child entries.
            ldapConnection.delete(baseDN);
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object, simple Ignore a Not Found,
            // as another OpenAM instance could have purged already and replicated
            // the change across the OpenDJ Bus.
            if (ldapException.getLDAPResultCode() != LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_DEL_FAIL.get(baseDN, ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString());
            }
        } catch (StoreException e) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE, e.getMessage(), e);
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
        }
        return;
    }

    // ***********************************************
    // Protected Helper Methods for protected access
    // to our private and runtime resolvable properties.
    // ***********************************************


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
        return getFormattedDNString(TOKEN_SESSION_HA_ROOT_SUFFIX+","+TOKEN_ROOT, null, null);
    }

    protected static String getTokenSaml2HaRootDn() {
        return getFormattedDNString(TOKEN_SAML2_HA_ROOT_SUFFIX+","+TOKEN_ROOT, null, null);
    }

    protected static String getTokenOauth2HaRootDn() {
        return getFormattedDNString(TOKEN_OAUTH2_HA_ROOT_SUFFIX+","+TOKEN_ROOT, null, null);
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

    protected static String[] getReturnAttrs_DN_ONLY_ARRAY() {
        return returnAttrs_DN_ONLY_ARRAY;
    }
}
