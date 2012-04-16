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
 * $Id: AgentToolsResolver.java,v 1.3 2008/06/25 05:51:51 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.admin;

import java.util.ArrayList;

import com.sun.identity.agents.install.handler.EncryptionHandler;
import com.sun.identity.agents.install.handler.GetEncryptionKeyHandler;
import com.sun.identity.install.tools.admin.BaseToolsResolver;
import com.sun.identity.install.tools.admin.ToolsOptionsInfo;
import com.sun.identity.install.tools.handler.ListProductsHandler;
import com.sun.identity.install.tools.handler.ProductInfoHandler;
import com.sun.identity.install.tools.util.LocalizedMessage;


/**
 * Base class for agent installer
 */
public class AgentToolsResolver extends BaseToolsResolver {

    public ArrayList getSupportedOptions() {
        ArrayList result = new ArrayList();
        
        result.add(new ToolsOptionsInfo(ListProductsHandler.class.getName(), 
                STR_LISTAGENTS_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_LISTPRODUCTS_SHORT_HELP)));
        
        result.add(new ToolsOptionsInfo(ProductInfoHandler.class.getName(), 
                STR_AGENTINFO_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_PRODUCTINFO_SHORT_HELP)));           
        
        result.add(new ToolsOptionsInfo(EncryptionHandler.class.getName(),
                STR_ENCRYPT_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_ENCRYPT_SHORT_HELP)));
        
        result.add(new ToolsOptionsInfo(GetEncryptionKeyHandler.class.getName(),
       	        STR_GEN_ENCRYPT_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_GET_ENCRYPT_KEY_SHORT_HELP)));
        
        return result;
    }

    
    public static final String STR_LISTAGENTS_OPTION = "--listAgents";
    public static final String STR_AGENTINFO_OPTION = "--agentInfo";
    public static final String STR_ENCRYPT_OPTION = "--encrypt";
    public static final String STR_GEN_ENCRYPT_OPTION = "--getEncryptKey";
    
    public static final String LOC_HR_MSG_LISTPRODUCTS_SHORT_HELP = 
        "HR_MSG_LISTPRODUCTS_SHORT_HELP";
    public static final String LOC_HR_MSG_PRODUCTINFO_SHORT_HELP = 
        "HR_MSG_PRODUCTINFO_SHORT_HELP";    
    public static final String LOC_HR_MSG_ENCRYPT_SHORT_HELP =
        "HR_MSG_ENCRYPT_SHORT_HELP";
    public static final String LOC_HR_MSG_GET_ENCRYPT_KEY_SHORT_HELP =
        "HR_MSG_GET_ENCRYPT_KEY_SHORT_HELP";
}
