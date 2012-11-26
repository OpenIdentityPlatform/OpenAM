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
 * $Id: JMQSessionRepository.java,v 1.6 2009/10/30 21:01:44 weisun2 Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */
package com.sun.identity.sm.mq;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.exceptions.NotFoundException;
import com.iplanet.dpro.session.exceptions.StoreException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.coretoken.interfaces.AMTokenRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.coretoken.interfaces.AMTokenSAML2Repository;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;

import java.util.*;
import javax.jms.IllegalStateException;

import org.forgerock.openam.session.model.AMRootEntity;
import org.forgerock.openam.session.model.DBStatistics;
import com.sun.identity.sm.model.FAMRecord;
import com.sun.identity.ha.FAMRecordPersister;
import com.sun.identity.ha.FAMPersisterManager;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.shared.service.OpenAMService;

/**
 * This class implements JMQ-based session repository which
 * is used in session failover mode to store/recover serialized
 * state of InternalSession object
 */
@Deprecated
class JMQSessionRepository extends GeneralTaskRunnable implements
        OpenAMService, AMTokenRepository, AMTokenSAML2Repository {

    /**
     * Singleton Definition.
     */
    private static JMQSessionRepository instance;

    /* Operations */
    static public final String READ = "READ";

    static public final String WRITE = "WRITE";

    static public final String DELETE = "DELETE";

    static public final String DELETEBYDATE = "DELETEBYDATE";

    static public final String SESSION = "session"; 
    
    /* JMQ Properties */
    static public final String ID = "ID";

    /* Config data */
    private static boolean isDatabaseUp = true;

    /**
     * grace period before expired session records are removed from the
     * repository
     */
    private static long gracePeriod = 5 * 60; /* 5 mins in secs */

    private static final String BRIEF_DB_ERROR_MSG = 
        "Session failover service is not functional due to DB unavailability.";

    private static final String DB_ERROR_MSG = 
        "Session database is not available at this moment."
            + "Please check with the system administrator " +
                    "for appropriate actions";

    private static final String LOG_MSG_DB_BACK_ONLINE = 
        "SESSION_DATABASE_BACK_ONLINE";

    private static final String LOG_MSG_DB_UNAVAILABLE = 
        "SESSION_DATABASE_UNAVAILABLE";

    private static boolean lastLoggedDBStatusIsUp = true;

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

    private static boolean caseSensitiveUUID =
        SystemProperties.getAsBoolean(Constants.CASE_SENSITIVE_UUID);

    static Debug debug = SessionService.sessionDebug;

    static {
        try {
            gracePeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_GRACE_PERIOD, String.valueOf(gracePeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_GRACE_PERIOD
                    + ", using default");
        }

        try {
            cleanUpPeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_RUN_PERIOD, String.valueOf(cleanUpPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_RUN_PERIOD
                    + ", using default");
        }

        try {
            healthCheckPeriod = Integer
                    .parseInt(SystemProperties.get(HEALTH_CHECK_RUN_PERIOD,
                            String.valueOf(healthCheckPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + HEALTH_CHECK_RUN_PERIOD
                    + ", using default");
        }

        runPeriod = (cleanUpPeriod <= healthCheckPeriod) ? cleanUpPeriod
                : healthCheckPeriod;
        cleanUpValue = cleanUpPeriod;
    }

    // Message queues
    // One REQUEST queue/topic is suffcient
    // Multiple RESPONSE queues/topics may be necessary
    public static FAMRecordPersister pSession = null;

    /**
     * Provide Service Instance Access to our Singleton
     *
     * @return JMQSessionRepository Singleton Instance.
     *
     */
    public JMQSessionRepository getInstance() {
        initPersistSession();
        SystemTimer.getTimer().schedule(instance, new Date((
                System.currentTimeMillis() / 1000) * 1000));
        return instance;
    }

   /**
    * Constructs new JMQSessionRepository
    */
   protected JMQSessionRepository() {
   }

    public String getInstanceClassName() {
        return JMQSessionRepository.class.getName();
    }

    /**
     *
     * Initialize new persistant session
     */
   
    private synchronized static void initPersistSession() {
        try {
            instance =  new JMQSessionRepository();
            pSession = FAMPersisterManager.getInstance().
                getFAMRecordPersister(); 
            isDatabaseUp = true;
        } catch (Exception e) {
            isDatabaseUp = false;
            FAMPersisterManager.clearInstance();
            debug.error(BRIEF_DB_ERROR_MSG);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        }

    }

   /**
    * Retrives new </code>InternalSession</code> for the session
    * @param sid Session Id
    * @return InternalSession 
    * @throws Exception when cannot create a retrieve internal session
    */
   public InternalSession retrieve(SessionID sid) throws Exception {
        if (!isDatabaseUp) {
            return null;
        }
        try {
            String key = SessionUtils.getEncryptedStorageKey(sid);
           
            FAMRecord famRec = new FAMRecord (
                SESSION, FAMRecord.READ, key, 0, null, 0, null, null);
            //TODO: Add interface  
            FAMRecord retRec = pSession.send(famRec);
            InternalSession is = null;
            if(retRec != null) {
            	byte[] blob = retRec.getSerializedInternalSessionBlob();
            	is = (InternalSession) SessionUtils.decode(blob);
            }

            /*
             * ret.put(SESSIONID, message.getString(SESSIONID)); ret.put(DATA,
             * message.getString(DATA));
             */
            return is;
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            FAMPersisterManager.clearInstance();
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            return null;
        } catch (Exception e) {
            debug.message("JMQSessionRepository.retrieve(): failed retrieving "
                    + "session", e);
            return null;
        }
    }

   /**
    * Deletes the given <code>Session</code>from the repository
    * @param sid SessionId
    * @throws Exception when cannot delete a session
    */
   public void delete(SessionID sid) throws Exception {
        if (!isDatabaseUp) {
            return;
        }
        try {
            String key = SessionUtils.getEncryptedStorageKey(sid);      
            FAMRecord famRec = new FAMRecord (
                SESSION, FAMRecord.DELETE, key, 0, null, 0, null, null);
           
            FAMRecord retRec = pSession.send(famRec);
           
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            FAMPersisterManager.clearInstance();
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.delete(): failed deleting "
                    + "session", e);
        }
    }

    /**
     * Deletes all expired Sessions from the repository
     * @exception Exception thrown when Unable to delete the expired sessions
     */
    public void deleteExpired() throws Exception {
        if (!isDatabaseUp) {
            return;
        }
        try {
            long date = System.currentTimeMillis() / 1000;     
             FAMRecord famRec = new FAMRecord (
                 SESSION, FAMRecord.DELETEBYDATE, null,
                 date, null, 0, null, null);
            //TODO: add interface revoke 
            FAMRecord retRec = pSession.send(famRec);
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            FAMPersisterManager.clearInstance();
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.deleteExpired(): failed "
                    + "deleting Expired Sessions", e);
        }
    }

   /**
    * Saves<code> InternalSession</code> into the <code>SessionRepository</code>
    * @param is InternalSession
    * @exception Exception thrown when cannot save the internal session
    */
   public void save(InternalSession is) throws Exception {
        if (!isDatabaseUp) {
            return;
        }

        try {
            SessionID sid = is.getID();
            String key = SessionUtils.getEncryptedStorageKey(sid);
            byte[] blob = SessionUtils.encode(is);
            long expirationTime = is.getExpirationTime() + gracePeriod;
            String uuid = caseSensitiveUUID ? is.getUUID() : is.getUUID().toLowerCase();
            if (debug.messageEnabled()) {
                debug.message("JMQSessionRepository.save(): " + 
                    "session size=" + blob.length + " bytes");
            }   
            FAMRecord famRec = new FAMRecord (
                SESSION, FAMRecord.WRITE, key, expirationTime, uuid,
                is.getState(), sid.toString(), blob);
           
            FAMRecord retRec = pSession.send(famRec); 
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            FAMPersisterManager.clearInstance();
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.save(): failed "
                    + "to save Session", e);
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid
     *            User's universal unique ID.
     * @return Map of all Session for the user
     * @throws Exception
     *             if there is any problem with accessing the session
     *             repository.
     */
    public Map getSessionsByUUID(String uuid) throws Exception {

        if (!isDatabaseUp) {
            throw new SessionException("Session repository is not "
                    + "available.");
        } 
        HashMap sessions = null; 
       
        try {
            FAMRecord famRec = new FAMRecord (
                SESSION, FAMRecord.GET_RECORD_COUNT,
                null, 0, uuid, 0, null, null);
            FAMRecord retRec = pSession.send(famRec);
            if (retRec != null) {
                sessions = retRec.getExtraStringAttributes();
            } 
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            FAMPersisterManager.clearInstance();
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            throw new SessionException(e);
        } catch (Exception e) {
            throw new SessionException(e);
        }
        return sessions;
    }

    /**
     * This method is invoked to log a message in the following two cases:
     * 
     * (1) the DB is detected down by either the user requests
     * (retrieve/save/delete/getSessionCount) or the background checker thread:
     * Log message: SESSION_DATABASE_UNAVAILABLE (2) the DB is detected
     * available again by the background health checker thread => Log message:
     * SESSION_DATABASE_BACK_ONLINE
     * 
     * The flag "lastLoggedDBStatusIsUp" is used to avoid logging the same DB
     * status again and again if the status actually doesn't change over time.
     * 
     * Please also note that if the DB is already down in the very beginning
     * when starting the AM instance, there will be no message being logged
     * since at this time the session service is not fully initialized yet
     * therefore no sso token can be generated and used for the logging purpose.
     * Nevertheless, the appropriate logging will be done later when the
     * background thread kicks in.
     * 
     */
    private void logDBStatus() {

        SessionService ss = SessionService.getSessionService();

        if (!isDatabaseUp && lastLoggedDBStatusIsUp) {
            ss.logSystemMessage(LOG_MSG_DB_UNAVAILABLE,
                    java.util.logging.Level.WARNING);
            lastLoggedDBStatusIsUp = false;
        }
        if (isDatabaseUp && !lastLoggedDBStatusIsUp) {
            ss.logSystemMessage(LOG_MSG_DB_BACK_ONLINE,
                    java.util.logging.Level.INFO);
            lastLoggedDBStatusIsUp = true;
        }
    }

    public long getRunPeriod() {
        return runPeriod;
    }
    
    public boolean addElement(Object obj) {
        return false;
    }
    
    public boolean removeElement(Object obj) {
        return false;
    }
    
    public boolean isEmpty() {
        return true;
    }
    
    /**
     * Monitoring logic used by background thread This thread is used for both
     * clenup expired sessions in the repository and for the Database health
     * checking. The thread always runs with smallest value of cleanUpPeriod and
     * healthCheckPeriod.
     */
    public void run() {
        
        try {

            if (debug.messageEnabled()) {
                debug.message("Cleaning expired session records");
            }

            /*
             * Clean up is done based on the cleanUpPeriod even though the
             * thread runs based on the runPeriod.
             */
            if (cleanUpValue <= 0) {
                deleteExpired();
                cleanUpValue = cleanUpPeriod;
            }
            cleanUpValue = cleanUpValue - runPeriod;

            /*
             * HealthChecking is done based on the runPeriod but only when
             * the Database is down.
             */
            if (!isDatabaseUp) {
                initPersistSession();
                logDBStatus();
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.run(): Exception in thread",
                    e);
        }

    }

    // *****************************************************************
    //
    // All methods below this comment are stubbed out for resolution
    // of our implementations for consistency.  This whole class will
    // be removed for 10.1.0.
    //
    // *****************************************************************

   @Override
    public DBStatistics getDBStatistics() {
        // TODO
        return null;
    }

   @Override
    public void delete(String id) throws StoreException, NotFoundException {
        // TODO
    }

   @Override
    public void deleteExpired(Calendar expDate) throws StoreException {
        // TODO
    }

   @Override
    public void write(AMRootEntity amRootEntity) throws StoreException {
        // TODO
    }

   @Override
    public AMRootEntity read(String id) throws StoreException, NotFoundException {
        return null;  // TODO
    }

   @Override
    public Set<String> readWithSecKey(String id) throws StoreException, NotFoundException {
        return null;  // TODO
    }

   @Override
    public void shutdown() {
        // TODO
    }

   @Override
    public Map<String, Long> getRecordCount(String id) throws StoreException {
        return null;  // TODO
    }

    /**
     * Retrives existing SAML2 object from persistent datastore
     *
     * @param samlKey primary key
     * @return SAML2 object, if failed, return null.
     */
    @Override
    public Object retrieveSAML2Token(String samlKey) {
        return null;  // TODO
    }

    /**
     * Retrives a list of existing SAML2 object from persistent datastore with
     * secodaryKey
     *
     * @param secKey Secondary Key
     * @return SAML2 object, if failed, return null.
     */
    @Override
    public List retrieveSAML2TokenWithSecondaryKey(String secKey) {
        return null;  // TODO
    }

    /**
     * Deletes the SAML2 object by given primary key from the repository
     *
     * @param samlKey primary key
     */
    @Override
    public void deleteSAML2Token(String samlKey) {
        // TODO
    }

    /**
     * Deletes expired SAML2 object from the repository
     */
    @Override
    public void deleteExpiredSAML2Tokens() {
        // TODO
    }

    /**
     * Saves SAML2 data into the SAML2 Repository
     *
     * @param samlKey        primary key
     * @param samlObj        saml object such as Response, IDPSession
     * @param expirationTime expiration time
     * @param secKey         Secondary Key
     */
    @Override
    public void saveSAML2Token(String samlKey, Object samlObj, long expirationTime, String secKey) {
        // TODO
    }
}
