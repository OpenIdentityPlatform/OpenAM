/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock US Inc. All Rights Reserved
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
import com.sun.identity.coretoken.interfaces.AMSessionRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.shared.ldap.*;

import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.sm.model.AMRecord;
import com.sun.identity.sm.model.AMRecordDataEntry;
import com.sun.identity.sm.model.AMSessionRepositoryDeferredOperation;

import com.sun.identity.sm.model.FAMRecord;
import org.forgerock.i18n.LocalizableMessage;

import static org.forgerock.openam.session.ha.i18n.AmsessionstoreMessages.*;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.iplanet.dpro.session.exceptions.NotFoundException;
import com.iplanet.dpro.session.exceptions.StoreException;
import org.forgerock.openam.session.model.AMRootEntity;
import org.forgerock.openam.session.model.DBStatistics;

import org.forgerock.openam.utils.TimeDuration;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.*;

/**
 * Provide Implementation of AMSessionRepository using the internal/external
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
 */
public class CTSPersistentStore extends GeneralTaskRunnable implements AMSessionRepository {

    /**
     * Globals Constants, so not to pollute entire product.
     */
    public static final String OU_FAMRECORDS = "ou=famrecords";

    public static final String FR_FAMRECORD = "frFamRecord";

    private static final String AMRECORD_NAMING_ATTR = "pKey";

    private static final String OBJECTCLASS = "objectClass";

    private static final String FAMRECORD_FILTER = "(" + OBJECTCLASS + Constants.EQUALS + Constants.ASTERISK + ")";

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

    private static boolean caseSensitiveUUID =
            SystemProperties.getAsBoolean(com.sun.identity.shared.Constants.CASE_SENSITIVE_UUID);

    /**
     * Define Session DN Constants
     */
    private static final String SM_CONFIG_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_SM_CONFIG_ROOT_SUFFIX, Constants.DEFAULT_ROOT_SUFFIX);

    private static final String SESSION_FAILOVER_HA_ROOT_SUFFIX =
            SystemPropertiesManager.get(SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX,
                    Constants.DEFAULT_SESSION_HA_ROOT_SUFFIX);

    private static final String SESSION_FAILOVER_HA_BASE_DN =
            SESSION_FAILOVER_HA_ROOT_SUFFIX +
                    Constants.COMMA + SM_CONFIG_ROOT_SUFFIX;

    private static final String FAM_RECORDS_BASE_DN =
            OU_FAMRECORDS + Constants.COMMA + SESSION_FAILOVER_HA_BASE_DN;

    private static final String SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE =
            AMRECORD_NAMING_ATTR + Constants.EQUALS + "%" + Constants.COMMA +
                    FAM_RECORDS_BASE_DN;

    /**
     * Session Expiration Filter.
     */
    private final static String SESSION_EXPIRATION_FILTER_TEMPLATE =
            "(&(" + OBJECTCLASS + Constants.EQUALS + FR_FAMRECORD +
                    ")" + EXPDATE_FILTER_PRE + "?" + EXPDATE_FILTER_POST + ")";

    /**
     * Return Attribute Constructs
     */
    private static LinkedHashSet<String> returnAttrs;
    private static String[] returnAttrs_ARRAY;

    private static LinkedHashSet<String> returnAttrs_PKEY_ONLY;
    private static String[] returnAttrs_PKEY_ONLY_ARRAY;

    private static LinkedHashSet<String> returnAttrs_DN_ONLY;
    private static String[] returnAttrs_DN_ONLY_ARRAY;

    /**
     * Expired Session Search Limit.
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
     * @throws StoreException
     */
    public static AMSessionRepository getInstance() throws StoreException {
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
            @Override
            public void run() {
                internalShutdown();
            }
        });
        // **********************************************************************************************
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
    //@Override - Compiling as 1.5
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
    //@Override - Compiling as 1.5
    public void run() {
        synchronized (LOCK) {
            while (!shutdown) {
                try {
                    // Delete any expired Sessions up to now.
                    deleteExpired(Calendar.getInstance());
                    // Process any Deferred Operations
                    processDeferredAMSessionRepositoryOperations();
                    // Wait for next tick or interrupt.
                    LOCK.wait(SLEEP_INTERVAL);
                } catch (InterruptedException ie) {
                    DEBUG.warning(DB_THD_INT.get().toString(), ie);
                } catch (StoreException se) {
                    DEBUG.warning(DB_STR_EX.get().toString(), se);
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
    //@Override - Compiling as 1.5
    public void save(InternalSession is) throws Exception {
        if (is == null) {
            return;
        }
        saveImmediate(is);
    }

    /**
     * Private Helper Method to perform the InternalSession Save Immediately,
     * without any queueing of request.
     *
     * @param is - InternalSession to Marshal/Serialize.
     * @throws Exception
     */
    private void saveImmediate(InternalSession is) throws Exception {
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
            String uuid = caseSensitiveUUID ? is.getUUID() : is.getUUID().toLowerCase();
            // Create a FAMRecord Object to wrap our Serialized Internal Session
            FAMRecord famRec = new FAMRecord(
                    SESSION, FAMRecord.WRITE, key, expirationTime, uuid,
                    is.getState(), sid.toString(), serializedInternalSession);
            // Persist Session Record
            writeImmediate(famRec);
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
    //@Override - Compiling as 1.5
    public void write(AMRootEntity amRootEntity) throws StoreException {
        if (amRootEntity == null) {
            return;
        }
        writeImmediate(amRootEntity);
    }

    /**
     * Takes an AMRecord and writes this to the store
     *
     * @param record The record object to store
     * @throws com.iplanet.dpro.session.exceptions.StoreException
     *
     */
    private void writeImmediate(AMRootEntity record)
            throws StoreException {
        // Setup the BaseDN.
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", (record).getPrimaryKey());
        // Perform a create/store by default and if we fail due to the entry already exists, then perform
        // an Update/modify of the existing Directory Entry.
        try {
            // Assume we are Adding the Entry.
            storeImmediate(record);
            // Log Action
            logAMRootEntity(record, "CTSPersistenceStore.storeImmediate: \nBaseDN:[" + baseDN.toString() + "] ");
        } catch(EntryAlreadyExistsException entryAlreadyExistsException) {
            // Update / Modify existing Entry.
            updateImmediate(record);
            // Log Action
            logAMRootEntity(record, "CTSPersistenceStore.updateImmediate: \nBaseDN:[" + baseDN.toString() + "] ");
        }
    }

    /**
     * Perform a Store Immediate to the Directory, since our record does not
     * exist per our up-stream caller.
     *
     * @param record
     * @throws StoreException
     */
    private void storeImmediate(AMRootEntity record)
            throws StoreException, EntryAlreadyExistsException {
        if ((record == null) || (record.getPrimaryKey() == null)) {
            return;
        }
        String messageTag = "CTSPersistenceStore.storeImmediate: ";
        // Prepare to Marshal our AMRootEntity Object.
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();
        // Construct our Entity's DN.
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", (record).getPrimaryKey());
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
            LDAPEntry ldapEntry = new LDAPEntry(baseDN, ldapAttributeSet);
            ldapConnection.add(ldapEntry);
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_SVR_CREATE.get(baseDN);
                DEBUG.message(messageTag + message.toString());
            }
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            if (ldapException.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                final LocalizableMessage message = DB_SVR_CRE_FAIL.get(baseDN);
                DEBUG.warning(messageTag + message.toString());
                throw new EntryAlreadyExistsException(message.toString());
            } else if (ldapException.getLDAPResultCode() == LDAPException.OBJECT_CLASS_VIOLATION) {
                // This usually indicates schema has not been applied to the Directory Information Base (DIB) Instance.
                final LocalizableMessage message = OBJECTCLASS_VIOLATION.get(baseDN);
                DEBUG.warning(messageTag + message.toString());
            } else {
                final LocalizableMessage message = DB_SVR_CRE_FAIL2.get(baseDN, Integer.toString(ldapException.getLDAPResultCode()));
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
    private void updateImmediate(AMRootEntity record)
            throws StoreException {
        List<RawModification> modList = createModificationList(record);
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", (record).getPrimaryKey());
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
            ldapConnection.modify(baseDN, ldapModificationSet);
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_SVR_MOD.get(baseDN);
                DEBUG.message(message.toString());
            }
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            // Not Found  No Such Object
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                DEBUG.warning(message.toString());
            } else if (ldapException.getLDAPResultCode() == LDAPException.OBJECT_CLASS_VIOLATION) {
                // This usually indicates schema has not been applied to the Directory Information Base (DIB) Instance.
                final LocalizableMessage message = OBJECTCLASS_VIOLATION.get(baseDN);
                DEBUG.warning(message.toString());
            } else {
                // Other Issue Detected.
                final LocalizableMessage message = DB_SVR_MOD_FAIL.get(baseDN, ldapException.errorCodeToString());
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
    //@Override - Compiling as 1.5
    public void delete(String id) throws StoreException, NotFoundException {
        deleteImmediate(id);
    }

    /**
     * Deletes session record from the repository.
     *
     * @param sid session ID.
     * @throws Exception if anything goes wrong.
     */
    //@Override - Compiling as 1.5
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
        long startTime = System.currentTimeMillis();
        if ((id == null) || (id.isEmpty())) {
            return;
        }
        // Initialize.
        String messageTag = "CTSPersistenceStore.deleteImmediate: ";
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", id);
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
    //@Override - Compiling as 1.5
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
            String filter = SESSION_EXPIRATION_FILTER_TEMPLATE.replace("?", formattedExpirationDate);
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
            LDAPSearchResults searchResults = ldapConnection.search(FAM_RECORDS_BASE_DN,
                    LDAPv2.SCOPE_SUB, filter.toString(), returnAttrs_PKEY_ONLY_ARRAY, false, ldapSearchConstraints);
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
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(FAM_RECORDS_BASE_DN, ldapException.errorCodeToString());
                DEBUG.error(messageTag + message.toString(), ldapException);
                throw new StoreException(messageTag + message.toString(), ldapException);
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
            timeDuration.stop();
            if (objectsDeleted > 0) {
                //if (DEBUG.messageEnabled()) {  // TODO -- Uncomment to limit verbosity.
                DEBUG.error(messageTag + "Number of Expired Sessions Deleted:[" + objectsDeleted + "], Duration:[" + timeDuration.getDurationToString() + "]");
                //}
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
    //@Override - Compiling as 1.5
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
    //@Override - Compiling as 1.5
    public Map<String, String> getSessionsByUUID(String uuid) throws SessionException {
        try {
            AMRecord amRecord = (AMRecord) this.read(uuid);
            if ((amRecord != null) && (amRecord.getExtraStringAttributes() != null)) {
                return amRecord.getExtraStringAttributes();
            }
            return null;
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
    //@Override - Compiling as 1.5
    public AMRootEntity read(String id)
            throws NotFoundException, StoreException {
        if ((id == null) || (id.isEmpty())) {
            return null;
        }
        // Establish our DN
        String baseDN = SESSION_FAILOVER_HA_ELEMENT_DN_TEMPLATE.replace("%", id);
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;
        String messageTag = "CTSPersistenceStore.read: ";
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            searchResults = ldapConnection.search(baseDN,
                    LDAPv2.SCOPE_BASE, FAMRECORD_FILTER, returnAttrs_ARRAY, false, new LDAPSearchConstraints());
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
    //@Override - Compiling as 1.5
    public Set<String> readWithSecKey(String id)
            throws StoreException, NotFoundException {
        if ((id == null) || (id.isEmpty())) {
            return null;
        }
        StringBuilder filter = new StringBuilder();
        filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);
        String messageTag = "CTSPersistenceStore.readWithSecKey: ";
        if (DEBUG.messageEnabled()) {
            DEBUG.message(messageTag + "Attempting Read of BaseDN:[" + FAM_RECORDS_BASE_DN + "]");
        }
        // Initialize LDAP Objects
        LDAPConnection ldapConnection = null;
        LDAPSearchResults searchResults = null;
        LDAPException lastLDAPException = null;

        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Perform the Search.
            searchResults = ldapConnection.search(FAM_RECORDS_BASE_DN,
                    LDAPv2.SCOPE_ONE, filter.toString(), returnAttrs_ARRAY, false, new LDAPSearchConstraints());
            // Anything Found?
            if ((searchResults == null) || (searchResults.getCount() <= 0) || (!searchResults.hasMoreElements())) {
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
            // Return UnMarshaled Object
            Set<String> result = new HashSet<String>();
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
    //@Override - Compiling as 1.5
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
            // Perform the Search.
            searchResults = ldapConnection.search(FAM_RECORDS_BASE_DN,
                    LDAPv2.SCOPE_ONE, filter.toString(), returnAttrs_ARRAY, false, new LDAPSearchConstraints());
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
                final LocalizableMessage message = DB_ENT_NOT_P.get(FAM_RECORDS_BASE_DN);
                DEBUG.message(messageTag + message.toString());
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(FAM_RECORDS_BASE_DN,
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
    //@Override - Compiling as 1.5
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
    //@Override - Compiling as 1.5
    public long getRunPeriod() {
        return runPeriod;
    }

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
        DEBUG.message(messageTag + "Attempting to Prepare BackEnd Persistence Store for Session Services.");
        try {
            CTSDataLayer = CTSDataLayer.getSharedSMDataLayerAccessor();
            if (CTSDataLayer == null) {
                throw new StoreException("Unable to obtain BackEnd Persistence Store for Session Services.");
            }
        } catch (Exception e) {
            DEBUG.error("Exception Occurred during attempt to access shared SM Data Layer!", e);
            throw new StoreException(e);
        }
        // Now Exercise and Validate the
        // LDAP Connection from the Pool and perform a Read of our DIT Container to verify setup.
        if (!validateCTSPersistenceStore()) {
            String message = "Validation of BackEnd Persistent Store was unsuccessful!\n" +
                    "Please Verify DIT Structure in OpenAM Configuration Directory.";
            DEBUG.error(messageTag + message);
            throw new StoreException(messageTag + message);
        }
        // Show Informational Message.
        if (DEBUG.messageEnabled()) {
            DEBUG.message(messageTag + "Successfully Prepared BackEnd Persistence Store for Session Services.");
        }
    }

    /**
     * Private Common Helper Method to Obtain an LDAP Connection
     * from the SMDataLayer Pool.
     *
     * @return LDAPConnection - Obtained Directory Connection from Pool.
     * @throws StoreException
     */
    private LDAPConnection getDirectoryConnection() throws StoreException {
        LDAPConnection ldapConnection = CTSDataLayer.getConnection();
        if (ldapConnection == null) {
            throw new StoreException("CTSPersistenceStore.prepareCTSPersistenceStore: Unable to Obtain Directory Connection!");
        }
        // Return Obtain Connection from Pool.
        return ldapConnection;
    }

    /**
     * Private Helper Method to perform Validation of our LDAP Connection.
     *
     * @return boolean - indicates if validation was successful or not.
     * @throws StoreException - Exception thrown if Error condition exists.
     */
    private static boolean validateCTSPersistenceStore() throws StoreException {
        if (instance.doesDNExist(SESSION_FAILOVER_HA_BASE_DN)) {
            if (DEBUG.messageEnabled()) {
                final LocalizableMessage message = DB_ENT_P.get(SESSION_FAILOVER_HA_BASE_DN);
                DEBUG.message("CTSPersistenceStore.validateCTSPersistenceStore: " + message.toString());
            }
            return true;
        }
        return false;
    }

    /**
     * Private Helper method, Does DN Exist?
     *
     * @param dn - To be Check for Existence.
     * @return boolean - indicator, "True" if DN has been Found, otherwise "False".
     */
    private boolean doesDNExist(final String dn) throws StoreException {
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        try {
            // Obtain a Connection.
            ldapConnection = getDirectoryConnection();
            // Perform the Search.
            LDAPSearchResults searchResults = ldapConnection.search(dn,
                    LDAPv2.SCOPE_BASE, FAMRECORD_FILTER, returnAttrs_DN_ONLY_ARRAY, false, new LDAPSearchConstraints());
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return false;
            }
            return true;
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                return false;
            }
            DEBUG.error("CTSPersistenceStore.doesDNExist:Exception Occurred, Unable to Perform method," +
                    " Directory Error Code: " + ldapException.errorCodeToString() +
                    " Directory Exception: " + ldapException.errorCodeToString(), ldapException);
            return false;
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                CTSDataLayer.releaseConnection(ldapConnection, lastLDAPException);
            }
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
     * Simple Helper Method to convert a Time represented as a Long in
     * Milliseconds to a Calendar object.
     *
     * @param expirationDate
     * @return Calendar Object set to Expiration Date.
     */
    private static Calendar getFormattedExpirationDate(long expirationDate) {
        Calendar expirationDateOnCalendar = Calendar.getInstance();
        expirationDateOnCalendar.setTimeInMillis(expirationDate);
        return expirationDateOnCalendar;
    }

}
