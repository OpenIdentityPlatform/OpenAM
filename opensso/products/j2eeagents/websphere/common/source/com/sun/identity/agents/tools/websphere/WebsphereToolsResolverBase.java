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
 * $Id: WebsphereToolsResolverBase.java,v 1.2 2008/11/21 22:21:44 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

import java.util.ArrayList;
import com.sun.identity.agents.install.admin.AgentToolsResolver;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.admin.ToolsOptionsInfo;

/**
 * Resolver class for agentadmin tool.
 */
public class WebsphereToolsResolverBase extends  AgentToolsResolver implements
        IConstants {
    
    public ArrayList getSupportedOptions() {
        
        ArrayList result = super.getSupportedOptions();
        result.add(new ToolsOptionsInfo(SetGroupHandler.class.getName(),
                STR_SET_GROUP_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_SETGROUP_SHORT_HELP,
                STR_WAS_GROUP)));
        
        result.add(new ToolsOptionsInfo(RemoveGroupHandler.class.getName(),
                STR_REMOVE_GROUP_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_REMOVEGROUP_SHORT_HELP,
                STR_WAS_GROUP)));
        
        return result;
    }
    
    public static final String STR_SET_GROUP_OPTION = "--setGroup";
    public static final String STR_REMOVE_GROUP_OPTION = "--removeGroup";
    
    
    public static final String LOC_HR_MSG_SETGROUP_SHORT_HELP =
            "HR_MSG_SETGROUP_SHORT_HELP";
    
    public static final String LOC_HR_MSG_REMOVEGROUP_SHORT_HELP =
            "HR_MSG_REMOVEGROUP_SHORT_HELP";
    
}
