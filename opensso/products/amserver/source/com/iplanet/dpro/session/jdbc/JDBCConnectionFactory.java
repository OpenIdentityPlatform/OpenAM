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
 * $Id: JDBCConnectionFactory.java,v 1.2 2008/06/25 05:41:30 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.jdbc;

import java.sql.Connection;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.iplanet.am.util.SystemProperties;

/**
 * This class implements a centralized facade interface for obtaining connection
 * instances for JDBC-based Session rpository implementation
 * 
 */

public class JDBCConnectionFactory {
    private static DataSource ds;

    private static String dsName = SystemProperties.get(
            "com.sun.identity.session.repository.dataSourceName",
            "jdbc/SessionRepository");

    /**
     * Returns connection instance from an underlying JDBC data source A
     * reference to the data source is obtained on the first call to
     * getConnection() via JNDI lookup
     * 
     */
    static synchronized Connection getConnection() throws Exception {
        if (ds == null) {
            InitialContext ctx = null;
            String jndiDsName = "java:comp/env/" + dsName;
            ctx = new InitialContext();
            ds = (DataSource) ctx.lookup(jndiDsName);
        }

        return ds.getConnection();
    }
}
