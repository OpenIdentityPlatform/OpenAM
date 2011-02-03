/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WeblogicHomeDirFinder.java,v 1.2 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.IDefaultValueFinder;

/**
 * This task locates Weblogic home.
 */
public class WeblogicHomeDirFinder implements IDefaultValueFinder, IConfigKeys {
    
    public String getDefaultValue(
            String key,
            IStateAccess state,
            String value) {
        return (value != null) ? value : getDefaultWeblogicHomeDirPath();
    }
    
    private String getDefaultWeblogicHomeDirPath() {
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);
        return (osName.toLowerCase().startsWith(STR_WINDOWS)) ?
            "C:\\bea\\wlserver_10.0" :
            "/usr/local/bea/wlserver_10.0";
    }
}
