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
 * $Id: JDBCConnectionPool.java,v 1.4 2008/06/25 05:41:30 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.jdbc;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

/**
* <code>JDBCConnectionPool</code> implements <code>DataSource</code>,
* <code>ConnectionEventListener</code> provides the ConnectionPool for
* data sources which are used in in session failover mode to store/recover 
* serialized state of <code>InternalSession</code> object
* @see com.iplanet.dpro.session.service.SessionService
* @see com.iplanet.dpro.session.service.SessionRepository
* @see com.iplanet.dpro.session.service.InternalSession
*
*/
public class JDBCConnectionPool implements DataSource, ConnectionEventListener {

     
    /** 
     * Database Connection State
     */
    static public class ConnectionState {
        private boolean busy;

        private long timestamp;

    /**
     * Checks if the Connection State in the connection pool
     * @return true if it is free
     */
     public boolean isFree() {
            return !busy;
        }

    /**
     * Checks if the connection state is busy
     * @return true if the connection state is busy
     */
     public boolean isBusy() {
            return busy;
        }

    /**
     * Sets the Connection State to busy
     * @param busy true if busy
     */
     public void setBusy(boolean busy) {
            this.busy = busy;
        }

    /** 
     * Returns the time stamp
     * @return timestamp in milliseconds
     *
     */
     public long getTimestamp() {
            return timestamp;
        }

    /**
     * Sets to the current system time.
     */
     public void touchTimestamp() {
            timestamp = System.currentTimeMillis();
        }

    /** 
     * Initialize ConnectionState
     */
     public ConnectionState() {
            touchTimestamp();
        }

    }

    // config properties

    private int maxPoolSize; // Max size of the pool

    private int steadyPoolSize; // Steady size of the pool

    private int maxWaitTime; // The total time a thread is willing to wait

    // for a connection object.

    private boolean requireConnectionValidation = false;

    private boolean failAllConnections = false;

    private int isolationLevel;

    private static boolean isIsolationSet = false;

    // writer for logging
    private PrintWriter logWriter;

    //Datasource Login time out
    private int loginTimeout;

    // Falg to hold if the connection pool has been initialized
    private boolean poolInitialized = false;

    // PooledConnection -> ConnectionStates
    private Map connectionStates;

    /**
     * maps Connections that are free to their ConnectionStates
     */
    private Map free;

    private ConnectionPoolDataSource poolDS;

    // JDBC repository provider
    private JDBCConnectionImpl jdbcProvider;

    Debug debug = SessionService.sessionDebug;

    /** 
     * Constructs <code>JDBCConnectionPool</code>
     * @exception if something goes wrong
     */
     public JDBCConnectionPool() throws Exception {

        setMaxPoolSize(SessionService.getMaxPoolSize());
        setSteadyPoolSize(SessionService.getMinPoolSize());
        setMaxWaitTime(SessionService.getConnectionMaxWaitTime());

        String jdbcDriverImpl = SessionService.getJdbcDriverClass();

        jdbcProvider = (JDBCConnectionImpl) Class.forName(jdbcDriverImpl)
                .newInstance();

        jdbcProvider.init(SessionService.getJdbcURL(), SessionService
                .getSessionStoreUserName(), SessionService
                .getSessionStorePassword());

        this.poolDS = jdbcProvider.getConnectionPoolDataSource();
    }

   /* This method does not need to be synchronized 
    * since all caller methods are,
    * but it does not hurt. Just to be safe.
    */
    private synchronized void initPool() throws SQLException {

        if (poolInitialized) {
            return;
        }

        connectionStates = new HashMap(getSteadyPoolSize());
        free = new HashMap(getSteadyPoolSize());

        createSteadyConnections();

        poolInitialized = true;
    }

    private static boolean isValid(Connection c) {
        try {
            c.setAutoCommit(false);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

     /**
     * Returns connection from the pool.
     * We do not support principal-specific connections
     *
     * @return a free pooled connection object 
     * 
     * @throws SQLException
     *          - if any error occurrs
     *          - or the pool has reached its max size and the 
     *                  max-connection-wait-time-in-millis has expired.
     */
    public Connection getConnection(String user, String password)
            throws SQLException {
        return getConnection();
    }

    /**
     * Returns connection from the pool.
     *
     * @return a free pooled connection object 
     * 
     * @throws SQLException
     *          - if any error occurrs
     *          - or the pool has reached its max size and the 
     *                  max-connection-wait-time-in-millis has expired.
     */
    public Connection getConnection() throws SQLException {
        // Note: this method should not be synchronized or the
        // startTime would be incorrect for threads waiting to enter

        Connection result = null;

        long startTime = 0;
        long elapsedWaitTime = 0;
        long remainingWaitTime = 0;

        if (getMaxWaitTime() > 0) {
            startTime = System.currentTimeMillis();
        }

        while (true) {
            // Try to get a free connection. Note: internalGetConnection()
            // will create a new connection if none is free and the max has
            // not been reached.
            // - If can't get one, get on the wait queue.
            // - Repeat this until maxWaitTime expires.
            // - If maxWaitTime == 0, repeat indefinitely.
            result = internalGetConnection();
            if (result != null) {
                // got one, return it
                break;
            } else {
                // did not get a connection.

                if (getMaxWaitTime() > 0) {
                    elapsedWaitTime = System.currentTimeMillis() - startTime;
                    if (elapsedWaitTime < getMaxWaitTime()) {
                        // time has not expired, determine remaining wait time.
                        remainingWaitTime = getMaxWaitTime() - elapsedWaitTime;
                    } else {
                        // wait time has expired
                        throw new SQLException(
                                "No available connection. Wait-time expired.");
                    }
                }

                synchronized (this) {
                    try {
                        if (free.isEmpty()) {
                            this.wait(remainingWaitTime);
                        }
                    } catch (InterruptedException ex) {
                        // Could be system shutdown.
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * return connection in free list. If none is found, returns null
     * 
     */
    synchronized private Connection internalGetConnection() throws SQLException 
    {

        initPool();
        Connection result = null;

        PooledConnection c = null;
        ConnectionState state = null;

        Iterator iter = free.entrySet().iterator();
        if (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            c = (PooledConnection) entry.getKey();
            state = (ConnectionState) entry.getValue();
            // set correct state
            state.setBusy(true);
            free.remove(c);
        }

        if (c != null) {
            result = c.getConnection();

            // Now we need to validate the connection
            if (isRequireConnectionValidation()) {
                // if error occurs here, will consider the connection invalid
                boolean valid = false;
                try {
                    valid = isValid(result);
                } catch (Exception ex) {
                    // problems in underlying connection
                    // the connection is not valid
                    valid = false;
                }
                if (!valid) {

                    if (failAllConnections) {
                        // destroy all connections in pool
                        result = failAllConnections();
                    } else {
                        // note the order of these two calls to eliminate the
                        // possibility that a callback could put the connection
                        // back
                        // into the free pool
                        connectionStates.remove(c);
                        destroyConnection(c);
                        // Now set result to null so another can be created.
                        result = null;
                    }
                }
            }
        }

        if (result == null) {
            // Either no free connection found or an invalid connection has
            // been destroyed. Let's see if we can add one.
            if (connectionStates.size() < getMaxPoolSize()) {
                c = createConnection(true);
                result = c.getConnection();
            }
        }

        if (result != null) {
            if (!isIsolationSet) {
                DatabaseMetaData dbMetaData = result.getMetaData();
                if (dbMetaData.supportsTransactionIsolationLevel(
                        Connection.TRANSACTION_REPEATABLE_READ)) {
                    setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
                    SessionService.sessionDebug.message("JDBCConnectionPool " +
                           "IsolationLevel set to TRANSACTION_REPEATABLE_READ");
                } else {
                    setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
                    SessionService.sessionDebug.message("JDBCConnectionPool " +
                            "IsolationLevel set to TRANSACTION_READ_COMMITTED");
                }
                isIsolationSet = true;
            }
            result.setAutoCommit(false);
            result.setTransactionIsolation(isolationLevel);
        }
        return result;
    }

    /**
     * this is only called from synchronized methods
     */
    private PooledConnection createConnection(boolean busy) throws SQLException 
    {
        PooledConnection c = poolDS.getPooledConnection();
        ConnectionState state = new ConnectionState();
        state.setBusy(busy);
        c.addConnectionEventListener(this);
        connectionStates.put(c, state);
        if (!busy) {
            free.put(c, state);
            this.notifyAll();
        }
        return c;
    }

    /*
     * This method performs the tasks defined by Fail All Connections - Destroy
     * all the connections in the pool (emptyPool) - Recreate the steadySize
     * number of connections. - returns a handle to one of the newly created
     * connections - returns null if no connections were created (database
     * unavailable)
     */
    private Connection failAllConnections() throws SQLException {
        // to be only called from synchronized methods !

        Connection result = null;
        emptyPool();
        createSteadyConnections();

        // we dont need to matching, just return the first connection in pool
        if (free.size() > 0) {
            Map.Entry entry = (Map.Entry) free.keySet().iterator().next();
            PooledConnection c = (PooledConnection) entry.getKey();
            ConnectionState state = (ConnectionState) entry.getValue();
            state.setBusy(true);
            free.remove(c);
            result = c.getConnection();
        }

        return result;
    }

    /*
     * Reinitialize the jdbc driver's implementation class of the
     * ConnectionPoolDataSource interface. This method is used when trying to
     * reestablish the connection to the DB instance which was previous
     * unavailable.
     */
    void reinitializePoolDataSource() throws SQLException {

        // This method should be only called from synchronized
        // block.
        jdbcProvider.init(SessionService.getJdbcURL(), SessionService
                .getSessionStoreUserName(), SessionService
                .getSessionStorePassword());
        poolDS = jdbcProvider.getConnectionPoolDataSource();

        emptyPool();
        createSteadyConnections();
    }

    /*
     * Create connections upto steadyPoolSize
     */
    synchronized private void createSteadyConnections() throws SQLException {
        for (int i = 0; i < getSteadyPoolSize(); i++) {
            createConnection(false);
        }
    }

   /* destroys the connection */
    private void destroyConnection(PooledConnection c) {
        try {
            c.removeConnectionEventListener(this);
            c.close();
        } catch (SQLException ex) {
            debug.message("JDBCConnectionPool:destroyConnection", ex);
        }
    }

    /**
     * this method is called to indicate that the connection is not used by a
     * caller anymore
     */

    synchronized public void connectionClosed(ConnectionEvent evt) {
        PooledConnection con = (PooledConnection) evt.getSource();
        ConnectionState state = getConnectionState(con);
        if (state == null || !state.isBusy()) {
            throw new IllegalStateException();
        }
        state.setBusy(false); // mark as not busy
        state.touchTimestamp();
        // move to free list
        free.put(con, state);
        this.notifyAll();
    }

    synchronized public void connectionErrorOccurred(ConnectionEvent evt) {
        PooledConnection con = (PooledConnection) evt.getSource();
        ConnectionState state = getConnectionState(con);
        if (state == null || state.isBusy() == false) {
            throw new IllegalStateException();
        }

        connectionStates.remove(con);
        destroyConnection(con);

    }

    private ConnectionState getConnectionState(PooledConnection c) {
        return (ConnectionState) connectionStates.get(c);
    }

    synchronized public void emptyPool() {
        Iterator i = connectionStates.keySet().iterator();
        while (i.hasNext()) {
            destroyConnection((PooledConnection) i.next());

        }
        free.clear();
        connectionStates.clear();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Pool [");
        sb.append("] PoolSize=");
        sb.append(connectionStates.size());
        sb.append("  FreeConnections=");
        sb.append(free.size());
        return sb.toString();
    }

   /** 
    * Gets the LogWriter for this connection
    * @return <code>Printwriter</code>
    */
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

   /**
    * Sets the LogWriter
    * @param out <code>PrintWriter</code> Object
    */
    public void setLogWriter(PrintWriter out) throws SQLException {
        logWriter = out;
    }

   /**
    * Sets the Login Timeout for this connection
    * @param loginTimeout
    */
    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    /**
     * Gets the Login time out for connection object
     * @return time in milli seconds
     */
    public int getLoginTimeout() {
        return loginTimeout;
    }

    /**
     * Sets the maximum connection pool size
     * @param maxPoolSize connection pool size
     *
     */
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Gets the maximum connection pool size
     * @return int, connection pool size
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /** 
     * Sets the steady pool size for this connection object
     * 
     * @param steadyPoolSize steady pool size
     */
    public void setSteadyPoolSize(int steadyPoolSize) {
        this.steadyPoolSize = Math.min(steadyPoolSize, maxPoolSize);
    }

    /** 
     * Gets the steady pool size for this connection object
     * 
     * @return steady pool size
     */
    public int getSteadyPoolSize() {
        return steadyPoolSize;
    }

    /** 
     * Sets the maximum connection pool wait time  thread is willing 
     * to wait for a connection object
     * @param maxWaitTime maximum wait time
     *
     */
    public void setMaxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    /** 
     * Gets the maximum connection pool wait time  thread is willing 
     * to wait for a connection object
     * @return  int, maximum wait time
     *
     */
    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * Sets the Connection validation
     * @param requireConnectionValidation true if connection 
     *          validation is required.
     */ 
    public void setRequireConnectionValidation(
            boolean requireConnectionValidation) {
        this.requireConnectionValidation = requireConnectionValidation;
    }

    /**
     * Checks if  the Connection validation is required
     * @return true if connection validation is required.
     */ 
    public boolean isRequireConnectionValidation() {
        return requireConnectionValidation;
    }

    /**
     * Sets the Isolaction level
     * @param isolationLevel Isolation level
     */
    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    /**
     * Returns the level of isolation set for connection pool
     * @return isolation level
     */
    public int getIsolationLevel() {
        return isolationLevel;
    }

    public boolean isWrapperFor(Class c) {
        return false;
    }

    public Object unwrap(Class iface)
        throws SQLException {
        throw new SQLException();
    }

}
