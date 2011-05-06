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
 * $Id: OracleConnectionImpl.java,v 1.2 2008/06/25 05:41:30 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.jdbc;

import javax.sql.ConnectionPoolDataSource;

import com.iplanet.dpro.session.service.SessionService;

/**
 * This is the Oracle factory class for database connection .
 * 
 */
public class OracleConnectionImpl extends JDBCConnectionImpl {

    ConnectionPoolDataSource ds = null;

    static final String ORCL_POOL_CLASS_NAME = 
        "oracle.jdbc.pool.OracleConnectionPoolDataSource";

    static final String ERROR_MSG_CLASS_NOT_FOUND = 
        "Please check oracle driver file is in the classpath";

    static final String ERROR_MSG_INIT_ERROR = 
        "Error while initializing OracleConnectionPoolDataSource";

   /**
    * Constructs <code>OracleConnectionImpl</code> for the Oracle Database 
    * connection
    */
    public OracleConnectionImpl() {
    }

    /**
     * initialize the connection pool data source
     * 
     * @param connString ,
     *            the jdbc url to connect
     * @param username ,
     *            user to connect db with
     * @param password ,
     *            user password
     */

    public void init(String connString, String username, String password) {
        try {
            ds = (ConnectionPoolDataSource) Class.forName(ORCL_POOL_CLASS_NAME)
                    .newInstance();

            if (SessionService.sessionDebug.messageEnabled()) {
                SessionService.sessionDebug.message("username is :" + username);
                SessionService.sessionDebug.message("URL is :" + connString);
            }
            invokeSetMethodCaseInsensitive(ds, "url", connString);
            invokeSetMethodCaseInsensitive(ds, "user", username);
            invokeSetMethodCaseInsensitive(ds, "password", password);

        } catch (ClassNotFoundException e) {
            SessionService.sessionDebug.error(ERROR_MSG_CLASS_NOT_FOUND, e);
        } catch (Exception e) {
            SessionService.sessionDebug.error(ERROR_MSG_INIT_ERROR, e);
        }
    }

    /**
     * Returns OracleConnectionPoolDataSource object
     * 
     * @return ConnectionPoolDataSource
     */

    public ConnectionPoolDataSource getConnectionPoolDataSource() {
        return ds;
    }
}
