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
 * $Id: HADBConnectionImpl.java,v 1.2 2008/06/25 05:41:30 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.jdbc;

import javax.sql.ConnectionPoolDataSource;

import com.iplanet.dpro.session.service.SessionService;

/**
 * 
 * HADB is the persistence store and provides 
 * high availability for session data.
 * <code>HADBConnection</code> implements <code>JDBCConnectionImpl</code>
 * provides the Connection for HADB data source which is used in session 
 * failover mode to store/recover serialized state of 
 * <code>InternalSession</code> object
 * 
 * @see com.iplanet.dpro.session.jdbc.JDBCConnectionImpl
 */

public class HADBConnectionImpl extends JDBCConnectionImpl {

    ConnectionPoolDataSource ds = null;

    static final String HADB_POOL_CLASS_NAME = 
        "com.sun.hadb.jdbc.pool.HadbConnectionPoolDataSource";

    static final String ERROR_MSG_CLASS_NOT_FOUND = 
        "Please check hadbjdbc4.jar file is in the classpath.";

    static final String ERROR_MSG_INIT_ERROR = 
        "Error while initializing HadbConnectionPoolDataSource";

    /**
     * Constructs <code>HADBConnectionImpl</code> for HADB Connection
     *
     */
    public HADBConnectionImpl() {
    }

    /**
     * initialize the connection pool data source
     * @param serverString , the server url to connect
     * @param username , user to connect db with
     * @param password , user password
     */
    public void init(String serverString, String username, String password) {
        try {
            ds = (ConnectionPoolDataSource) Class.forName(HADB_POOL_CLASS_NAME)
                    .newInstance();
            invokeSetMethodCaseInsensitive(ds, "serverList", serverString);
            invokeSetMethodCaseInsensitive(ds, "username", username);
            invokeSetMethodCaseInsensitive(ds, "password", password);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            SessionService.sessionDebug.error(ERROR_MSG_CLASS_NOT_FOUND, e);
        } catch (Exception e) {
            SessionService.sessionDebug.error(ERROR_MSG_INIT_ERROR, e);
        }
    }

    /**
     * Returns ConnectionPoolDataSource object
     * @return ConnectionPoolDataSource
     */
    public ConnectionPoolDataSource getConnectionPoolDataSource() {
        return ds;
    }
}
