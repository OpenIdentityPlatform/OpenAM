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
 * $Id: JDBCSessionRepository.java,v 1.5 2008/07/23 17:21:56 veiming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.jdbc;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.AMSessionRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * <code>JDBCSessionRepository</code>implements JDBC-based session repository 
 * which is used in session failover mode to store/recover serialized
 * state of <code>InternalSession</code> object
 *
 * @see com.iplanet.dpro.session.service.InternalSession
 * @see com.iplanet.dpro.session.service.AMSessionRepository
 */

public class JDBCSessionRepository extends GeneralTaskRunnable implements
    AMSessionRepository {

     /* Data Source */
     private static DataSource dataSource = null;

     /* Database table name */
     private static final String table = "sunwam_session";

     private static final String table_ext = "sunwam_session_ext";

    /* Database insert SQL statement */
     private static final String INSERT_SQL_STMT = "insert into " + table
            + "(id,blob_chunk,blob_size,expiration_time,"
            + "uuid,sessionstate,version) values(?,?,?,?,?,?,?)";

    /* Update Statement */
     private static final String UPDATE_SQL_STMT = "update " + table
            + " set blob_chunk = ?, blob_size = ? , expiration_time = ? ,"
            + " uuid = ?, sessionstate = ?, version = ? "
            + " where id = ? and version = ?";

    /* Select statement for user session */
     private static final String GET_SESSION_COUNT_SQL_STMT = "select * from "
            + table + " where uuid = ?"
            + " and expiration_time > ? and sessionstate = " + Session.VALID;

    /* Connection Pool name */
     private static final String connectionPoolName = SystemProperties.get(
            "com.sun.identity.session.failover.connectionPoolClass",
            "com.iplanet.dpro.session.jdbc.JDBCConnectionPool");

    /**
     * the blob chunk size is chosen to make sure that the total record size is
     * less than upper limit supported by HADB (8080 bytes)
     */
    private static int BLOB_CHUNK_SIZE = 7800;

    /* Flag check if the database is up */
    private static boolean isDatabaseUp = true;

    /**
     * grace period before expired session records are removed from the
     * repository
     */
    private static long gracePeriod = 5 * 60; /* 5 mins in secs */

    private static final String CLEANUP_GRACE_PERIOD = 
        "com.sun.identity.session.repository.cleanupGracePeriod";

    private static final String BRIEF_DB_ERROR_MSG = 
        "Session failover service is not functional due to DB unavailability.";

    private static final String DB_ERROR_MSG = 
        "Session database is not available at this moment."
            + "Please check with the system administrator for " +
                    "appropriate actions";

    /**
     * Time period between two successive runs of repository cleanup thread
     * which checks and removes expired records
     */

    private static long cleanUpPeriod = 1 * 60 * 1000; // 1 min in milliseconds

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

    /**
     * Utility method to obtain JDBC connection instance
     * 
     */

    private Connection getConnection() throws Exception {

        /**
         * The Factory implementation was done for the dataSource to switch
         * between the container's default implementation or our implementation.
         */
        if (dataSource == null) {
            dataSource = (DataSource) Class.forName(connectionPoolName)
                    .newInstance();
        }

        Connection conn = null;
        try {
            // Use DCL mechanism to optimize the performance. Although
            // DCL is known not to be always safe in some corner
            // cases, it should not be a problem here since the
            // atomicity of the operation is guaranteed in this case.
            if (!isDatabaseUp) {
                synchronized (this) {
                    if (!isDatabaseUp) {
                        ((JDBCConnectionPool) dataSource)
                                .reinitializePoolDataSource();
                        isDatabaseUp = true;
                    }
                }
            }
            conn = dataSource.getConnection();
        } catch (Exception e) {
            isDatabaseUp = false;
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            throw e;
        }
        return conn;
    }

    /**
     * Constructs <code> JDBCSessionRepository </code> checks if OpenSSO
     * Enterprise tables/indices are present in the  database If they are not,
     * it creates them .It also creates and starts cleanup thread
     * @exception If session repository initialiazation fails
     */
    public JDBCSessionRepository() throws Exception {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = getConnection();

            rs = conn.getMetaData().getTables(null, null, table,
                    new String[] { "TABLE" });

            boolean tableExists = rs.next();
            rs.close();
            rs = null;

            /*
             * TODO This check is not valid for Oracle so for time being Instead
             * of throwing Exception changing it to a debug message.
             */
            if (!tableExists) {
                // throw new SessionException("SessionTableNotFound");
                debug.message("DataBase table does not exist");
            }

        } catch (Exception ex) {
            debug.error("JDBCSessionRepository Initialization failed.");
            // throw ex;
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }

        SystemTimer.getTimer().schedule(this, new Date((
            System.currentTimeMillis() / 1000) * 1000));
    }

    /**
     * Retrieves session state from the repository
     * 
     * @param sid
     *            session id
     * @return InternalSession object retrieved from the repository
     * @throws Exception
     *             if anything goes wrong
     */
    public InternalSession retrieve(SessionID sid) throws Exception {

        if (!isDatabaseUp) {
            return null;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            stmt = conn.prepareStatement("select * from " + table
                    + " where id = ?");
            stmt.setString(1, SessionUtils.getEncryptedStorageKey(sid));
            rs = stmt.executeQuery();
            if (!rs.next())
                return null;

            byte[] chunk = rs.getBytes("blob_chunk");
            int blobSize = rs.getInt("blob_size");
            long version = rs.getLong("version");

            byte[] blob = chunk;
            if (blobSize != 0 && blobSize > chunk.length) {
                blob = new byte[blobSize];
                System.arraycopy(chunk, 0, blob, 0, chunk.length);
                retrieveBlobRemainder(conn, sid, blob);
            }

            InternalSession is = (InternalSession) SessionUtils.decode(blob);
            is.setVersion(version);

            conn.commit();
            return is;
        } catch (Exception ex) {
            try {
                conn.rollback();
            } catch (Exception e) {
            }
            // throw ex;
            return null;
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Saves session state to the repository If it is a new session (version ==
     * 0) it inserts new record(s) to the repository. For an existing session it
     * updates repository record instead while checking that versions in the
     * InternalSession and in the record match In case of version mismatch or
     * missing record IllegalArgumentException will be thrown
     * 
     * @param is
     *            reference to InternalSession object being saved
     * @throws Exception
     *             if anything goes wrong
     */
    public void save(InternalSession is) throws Exception {

        if (!isDatabaseUp) {
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            synchronized (is) {
                SessionID sid = is.getID();

                byte[] blob = SessionUtils.encode(is);
                long version = is.getVersion();

                long expirationTime = is.getExpirationTime() + gracePeriod;

                byte[] firstChunk = blob;

                if (blob.length > BLOB_CHUNK_SIZE) {
                    firstChunk = new byte[BLOB_CHUNK_SIZE];
                    System.arraycopy(blob, 0, firstChunk, 0, firstChunk.length);
                }

                String encryptedStorageKey = SessionUtils
                        .getEncryptedStorageKey(sid);

                String uuid = is.getUUID();
                int sessionState = is.getState();

                if (version == 0) {
                    insertNewSessionEntry(conn, encryptedStorageKey,
                            firstChunk, blob.length, expirationTime, uuid,
                            sessionState, version);
                } else {
                    stmt = conn.prepareStatement(UPDATE_SQL_STMT);
                    stmt.setBytes(1, firstChunk);
                    stmt.setInt(2, blob.length);
                    stmt.setLong(3, expirationTime);
                    stmt.setString(4, uuid);
                    stmt.setInt(5, sessionState);
                    stmt.setLong(6, version + 1);
                    stmt.setString(7, encryptedStorageKey);
                    stmt.setLong(8, version);
                    int count = stmt.executeUpdate();

                    if (count == 0) {
                        // The following code is to handle the
                        // situation where the "data intergrity"
                        // is not assured. If the database is
                        // corrupted and the seesion store gets
                        // recreated, for all the active sessions
                        // the "save" operation will result in a new
                        // session record being inserted into DB.
                        insertNewSessionEntry(conn, encryptedStorageKey,
                                firstChunk, blob.length, expirationTime, uuid,
                                sessionState, version);
                    }
                }

                saveBlobRemainder(conn, sid, blob, expirationTime);

                conn.commit();

                is.setVersion(version + 1);
            }
        } catch (Exception ex) {
            debug.error("Exception thrown when saving the session "
                    + "into the JDBC repository.", ex);
            try {
                conn.rollback();
            } catch (Exception e) {
            }
            // throw ex;
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    private void insertNewSessionEntry(Connection conn,
            String encryptedStorageKey, byte[] firstChunk, int blobLen,
            long expirationTime, String uuid, int sessionState, long version)
            throws Exception {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(INSERT_SQL_STMT);
            stmt.setString(1, encryptedStorageKey);
            stmt.setBytes(2, firstChunk);
            stmt.setInt(3, blobLen);
            stmt.setLong(4, expirationTime);
            stmt.setString(5, uuid);
            stmt.setInt(6, sessionState);
            stmt.setLong(7, version + 1);
            stmt.executeUpdate();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Deletes session record from the repository
     * 
     * @param sid
     *            session id
     * @throws Exception
     *             if anything goes wrong
     */
    public void delete(SessionID sid) throws Exception {

        if (!isDatabaseUp) {
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            conn = getConnection();
            stmt = conn.prepareStatement("delete from " + table
                    + " where id = ?");
            stmt.setString(1, SessionUtils.getEncryptedStorageKey(sid));
            stmt.executeUpdate();

            stmt.close();
            stmt = null;

            stmt = conn.prepareStatement("delete from " + table_ext
                    + " where id = ?");
            stmt.setString(1, SessionUtils.getEncryptedStorageKey(sid));
            stmt.executeUpdate();

            conn.commit();
        } catch (Exception ex) {
            debug.error("Exception thrown when deleting the session "
                    + "from the JDBC repository.", ex);
            try {
                conn.rollback();
            } catch (Exception e) {
            }
            // throw ex;
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Deletes expired session records,This is mainly used by the background 
     * clean up thread to clean up the expired session records from the 
     * <code>SessionRepository</code>
     * 
     * @exception when Session cannot be deleted
     * @see com.iplanet.dpro.session.service.SessionRepository
     */
    
    public void deleteExpired() throws Exception {
        if (!isDatabaseUp) {
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();

            // note here we do not require repeatable read isolation
            // intentionally for performance reasons as
            // deleting from primary session table first
            // guarantees access atomicity with respect to
            // blob extension table for other types of operations

            long now = System.currentTimeMillis() / 1000;
            stmt = conn.prepareStatement("delete from " + table
                    + " where expiration_time < ?");
            stmt.setLong(1, now);
            stmt.executeUpdate();

            stmt.close();
            stmt = null;

            stmt = conn.prepareStatement("delete from " + table_ext
                    + " where expiration_time < ?");
            stmt.setLong(1, now);
            stmt.executeUpdate();

            conn.commit();
        } catch (Exception ex) {
            debug.error("Exception thrown when cleanning up the session "
                    + "from the JDBC repository.", ex);
            try {
                conn.rollback();
            } catch (Exception e) {
            }
            // throw ex;
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Utility method used by save() to manage multi-chunk blob values
     * 
     * @param conn
     *            JDBC connection
     * @param sid
     *            session id
     * @param blob
     *            session blob data
     * @param expirationTime
     *            record expiration time
     * @throws Exception
     *             if anything goes wrong
     */
    void saveBlobRemainder(Connection conn, SessionID sid, byte[] blob,
            long expirationTime) throws Exception {

        PreparedStatement stmt = null;

        try {

            int remaining = blob.length - BLOB_CHUNK_SIZE;

            int chunkNumber = 1;

            // remove old chunks (if any)

            stmt = conn.prepareStatement("delete from " + table_ext
                    + " where id = ?");
            stmt.setString(1, SessionUtils.getEncryptedStorageKey(sid));
            stmt.executeUpdate();
            stmt.close();
            stmt = null;

            if (remaining > 0) {
                byte[] chunk = new byte[BLOB_CHUNK_SIZE];

                stmt = conn.prepareStatement("insert into " + table_ext
                        + "(id,blob_chunk,blob_chunk_seq,expiration_time) "
                        + " values(?,?,?,?)");

                while (remaining > 0) {

                    int len = Math.min(remaining, BLOB_CHUNK_SIZE);
                    System.arraycopy(blob, chunkNumber * BLOB_CHUNK_SIZE,
                            chunk, 0, len);

                    stmt.setString(1, SessionUtils.getEncryptedStorageKey(sid));
                    stmt.setBytes(2, chunk);
                    stmt.setInt(3, chunkNumber);
                    stmt.setLong(4, expirationTime);
                    stmt.executeUpdate();
                    ++chunkNumber;
                    remaining -= BLOB_CHUNK_SIZE;
                }
            }

        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Utility method used by retrieve() to manage multi-chunk blob values
     * 
     * @param conn
     *            JDBC connection
     * @param sid
     *            session id
     * @param blob
     *            output parameter used to return retrieved session blob data
     * @throws Exception
     *             if anything goes wrong
     */
    void retrieveBlobRemainder(Connection conn, SessionID sid, byte[] blob)
            throws Exception {

        String storageKey = SessionUtils.getEncryptedStorageKey(sid);
        retrieveBlobRemainder(conn, storageKey, blob);
    }

    /**
     * Utility method used by retrieve() to manage multi-chunk blob values
     * 
     * @param conn
     *            JDBC connection
     * @param sKey
     *            session storage key
     * @param blob
     *            output parameter used to return retrieved session blob data
     * @throws Exception
     *             if anything goes wrong
     */
    void retrieveBlobRemainder(Connection conn, String sKey, byte[] blob)
            throws Exception {

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {

            int remaining = blob.length - BLOB_CHUNK_SIZE;
            if (remaining <= 0)
                return;

            stmt = conn
                    .prepareStatement("select blob_chunk, blob_chunk_seq from "
                            + table_ext + " where id = ?");
            stmt.setString(1, sKey);
            rs = stmt.executeQuery();

            while (rs.next()) {
                byte[] chunk = rs.getBytes("blob_chunk");
                int chunkSeq = rs.getInt("blob_chunk_seq");
                if (chunk.length == 0 || chunk.length > BLOB_CHUNK_SIZE
                        || chunkSeq < 1
                        || chunkSeq * BLOB_CHUNK_SIZE > blob.length) {

                    throw new IllegalArgumentException("Invalid chunk");
                }
                int len = Math.min(chunk.length, blob.length - chunkSeq
                        * BLOB_CHUNK_SIZE);

                System.arraycopy(chunk, 0, blob, chunkSeq * BLOB_CHUNK_SIZE,
                        len);
                remaining -= len;
            }
            if (remaining != 0) {
                throw new IllegalArgumentException("Missing chunks");
            }
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid
     *            User's universal unique ID.
     * @throws Exception
     *             if there is any problem with accessing the session
     *             repository.
     */
    public Map getSessionsByUUID(String uuid) throws Exception {

        // Check if the database is up. No user session is allowed
        // to be created if the session repository is completely
        // down.
        if (!isDatabaseUp) {
            throw new SessionException("Session repository is not "
                    + "available.");
        }
        Map sessions = new HashMap();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(GET_SESSION_COUNT_SQL_STMT);
            stmt.setString(1, uuid);
            long now = System.currentTimeMillis() / 1000;
            stmt.setLong(2, now);

            rs = stmt.executeQuery();
            while (rs.next()) {
                // Implementation Note:
                // Here we need to retrieve the session id (not just
                // the storage key which itself is not sufficient for
                // retrieving the session from the master session table)
                // and the expiration_time for the caller to determine
                // what do do next. Since the database table doesn't
                // have a column to store the master sid, the session
                // blob is retrieved and deserialized to get the
                // needed information (trying to avoid changing the
                // existing table schema). This would possibly
                // introduce some performance overhead but hopefully
                // it's managable because:
                // 1) the number defined for user session quota is
                // expected to be low (usually less than 5)
                // 2) this checking happens only once for session
                //
                // That said, if performacne turns out to be an issue,
                // the other alternative is to change the table schema
                // to include the master sid so that there is no need
                // to retrieve/deserialize the session blob (TBD).
                //
                String sKey = rs.getString("id");
                byte[] chunk = rs.getBytes("blob_chunk");
                int blobSize = rs.getInt("blob_size");
                byte[] blob = chunk;
                if (blobSize != 0 && blobSize > chunk.length) {
                    blob = new byte[blobSize];
                    System.arraycopy(chunk, 0, blob, 0, chunk.length);
                    retrieveBlobRemainder(conn, sKey, blob);
                }
                InternalSession is = (InternalSession) SessionUtils
                        .decode(blob);
                String sid = is.getID().toString();
                Long expirationTime = new Long(is.getExpirationTime());
                sessions.put(sid, expirationTime);
            }
            conn.commit();

        } catch (Exception ex) {
            try {
                conn.rollback();
            } catch (Exception e) {
            }
            throw new SessionException("Error occurs when executing "
                    + "the GET_SESSION_COUNT query");
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
        return sessions;

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
                checkDatabaseAvailability();
            }
        } catch (Exception e) {
        }
    }

    private void checkDatabaseAvailability() {

        Connection conn = null;
        try {
            conn = getConnection();
        } catch (Exception e) {
            // Do nothing...
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

}
