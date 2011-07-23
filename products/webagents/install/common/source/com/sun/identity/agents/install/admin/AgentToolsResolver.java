/* The contents of this file are subject to the terms
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
 * $Id: AgentToolsResolver.java,v 1.1 2006/10/06 18:27:34 subbae Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.agents.install.admin;

import java.util.ArrayList;

import com.sun.identity.install.tools.admin.BaseToolsResolver;
import com.sun.identity.install.tools.admin.ToolsOptionsInfo;
import com.sun.identity.install.tools.handler.ListProductsHandler;
import com.sun.identity.install.tools.handler.ProductInfoHandler;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * Base class for agent installer. Provides agent installer supported options.
 */
public class AgentToolsResolver extends BaseToolsResolver {

    public static final String STR_LISTAGENTS_OPTION = "--listAgents";
    public static final String STR_AGENTINFO_OPTION = "--agentInfo";
    
    public static final String LOC_HR_MSG_LISTPRODUCTS_SHORT_HELP = 
        "HR_MSG_LISTPRODUCTS_SHORT_HELP";
    public static final String LOC_HR_MSG_PRODUCTINFO_SHORT_HELP = 
        "HR_MSG_PRODUCTINFO_SHORT_HELP";    

     /**
     * Returns list of agent installer supported options.
     * @return list of agent installer supported options
     */
   public ArrayList getSupportedOptions() {
        ArrayList result = new ArrayList();
        
        result.add(new ToolsOptionsInfo(ListProductsHandler.class.getName(), 
                STR_LISTAGENTS_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_LISTPRODUCTS_SHORT_HELP)));
        
        result.add(new ToolsOptionsInfo(ProductInfoHandler.class.getName(), 
                STR_AGENTINFO_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_PRODUCTINFO_SHORT_HELP)));           
        
        return result;
    }    
}
