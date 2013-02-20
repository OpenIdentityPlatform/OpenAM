/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WebsphereHomeDirFinder.java,v 1.1 2008/11/21 22:21:55 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere.v61;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.agents.tools.websphere.IConstants;

/**
 * This class locates home directory of Websphere.
 */

public class WebsphereHomeDirFinder implements IDefaultValueFinder, IConstants{
    
    public String getDefaultValue(String key, IStateAccess state, 
            String value) {
        String result = null;
        if (value != null) {
            result = value;
        } else {
            result = getDefaultWASHomeDirPath();
        }
        
        return result;
    }
    
    
    private String getDefaultWASHomeDirPath() {
        String result = null;
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);
        if (osName.toLowerCase().startsWith(STR_WINDOWS)) {
            result = "C:\\Program Files\\IBM\\WebSphere\\AppServer";
        } else {
            result = "/opt/IBM/WebSphere/AppServer";
        }
        
        return result;
    }
    
}
