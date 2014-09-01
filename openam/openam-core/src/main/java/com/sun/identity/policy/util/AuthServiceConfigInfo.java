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
 * $Id: AuthServiceConfigInfo.java,v 1.3 2008/06/25 05:43:54 qcheng Exp $
 *
 */



/*
 * Created on May 18, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.sun.identity.policy.util;

/**
 * @author bk95756
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
        
public class AuthServiceConfigInfo {
        String orgName = null;
        String portNumber = null;
        String authLevel = null;
        String moduleName = null;

        AuthServiceConfigInfo(String org, String module, String port, 
            String level) 
        {
                orgName = org;
                moduleName = module;
                portNumber = port;
                authLevel = level;
        }
        
        String getOrgName() {
                return orgName;
        }
        String getPortNumber() {
                return portNumber;
        }
        String getAuthLevel() {
                return authLevel;
        }
        public String getModuleName() {
                return moduleName;
        }
}
        
