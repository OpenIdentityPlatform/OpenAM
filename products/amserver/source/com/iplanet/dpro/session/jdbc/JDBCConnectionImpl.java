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
 * $Id: JDBCConnectionImpl.java,v 1.2 2008/06/25 05:41:30 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.sql.ConnectionPoolDataSource;

/**
* <code>JDBCConnectionImpl</code> is an abstract class implements the
* ConnectionPool for the session data source which is used in the failover
* to retrieve/store session data
*
*
*/
public abstract class JDBCConnectionImpl {

    protected JDBCConnectionImpl() {
    }

    /**
     * abstract method to initialize the connection pool data source
     * @param serverString the server url to connect
     * @param username to connect the db
     * @param password to connect 
     */
    public abstract void init(String serverString, String username,
            String password);

    /**
     * abstract method for the connection pool data source
     * @return ConnectionPoolDataSource
     */
    public abstract ConnectionPoolDataSource getConnectionPoolDataSource();

    
    /**
     * Invokes the appropriate connection pool class methods based on the 
     * the type of session repository.
     * @param obj ConnectionPoolDataSource object
     * @param prop Property key can be Serverlist, username, password
     * @param value value for the property
     */
    public static void invokeSetMethodCaseInsensitive(Object obj, String prop,
            String value) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        String alternateMethodName = null;
        Class cl = obj.getClass();
        String setMeth = "set" + prop;
        Method[] methodsList = cl.getMethods();
        boolean methodFound = false;
        int i = 0;
        for (i = 0; i < methodsList.length; ++i) {
            if (methodsList[i].getName().equalsIgnoreCase(setMeth) == true) {
                Class[] parameterTypes = methodsList[i].getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0].getName().equals("java.lang.String"))
                    {
                        methodFound = true;
                        break;
                    } else
                        alternateMethodName = methodsList[i].getName();
                }
            }
        }
        if (methodFound == true) {
            Object[] params = { value };
            methodsList[i].invoke(obj, params);
            return;
        }
        if (alternateMethodName != null) {
            try {
                // try int method
                Class[] cldef = { Integer.TYPE };
                Method meth = cl.getMethod(alternateMethodName, cldef);
                Object[] params = { Integer.valueOf(value) };
                meth.invoke(obj, params);
                return;
            } catch (NoSuchMethodException nsmex) {
                // try boolean method
                Class[] cldef = { Boolean.TYPE };
                Method meth = cl.getMethod(alternateMethodName, cldef);
                Object[] params = { Boolean.valueOf(value) };
                meth.invoke(obj, params);
                return;
            }
        } else
            throw new NoSuchMethodException(setMeth);
    }
}
