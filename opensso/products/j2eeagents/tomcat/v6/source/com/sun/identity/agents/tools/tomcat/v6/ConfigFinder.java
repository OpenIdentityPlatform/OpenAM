/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: ConfigFinder.java,v 1.2 2008/11/28 12:36:21 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.install.tools.configurator.IStateAccess;


/**
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ConfigFinder implements IDefaultValueFinder {
    public static final String STR_OS_NAME_PROPERTY = "os.name";
    public static final String STR_WINDOWS = "windows";

    public String getDefaultValue(
        String key,
        IStateAccess state,
        String value) {
        String result = null;

        if (value != null) {
            result = value;
        } else {
            result = getDefaultConfigDirectoryPath();
        }

        return result;
    }

    private String getDefaultConfigDirectoryPath() {
        String result = null;
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);

        if (osName.toLowerCase()
                      .startsWith(STR_WINDOWS)) {
            result = "C:/Program Files/" +
            		"Apache Software Foundation/Tomcat 6.0/conf";
        } else {
            result = "/opt/apache-tomcat-6.0.14/conf";
        }

        return result;
    }
}
