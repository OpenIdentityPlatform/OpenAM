/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HostDefaultValueFinder.java,v 1.2 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sun.identity.install.tools.util.Debug;

/**
 * @author krishc
 * 
 * Class to find the local host name
 * 
 */
public class HostDefaultValueFinder implements IDefaultValueFinder {

    public String getDefaultValue(String key, IStateAccess state, String value)
    {

        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ukhe) {
            Debug.log("HostDefaultValueFinder: exception thrown while finding "
                    + "local host name :", ukhe);
        } catch (SecurityException se) {
            Debug.log("HostDefaultValueFinder: exception thrown while finding "
                    + "local host name :", se);
        } catch (Exception ex) {
            Debug.log("HostDefaultValueFinder: exception thrown while finding "
                    + "local host name :", ex);
        }

        Debug.log("HostDefaultValueFinder: Local host name =" + hostname);
        return hostname;
    }

}
