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
 * $Id: RemoveGroupHandler.java,v 1.2 2008/11/21 22:21:44 leiming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.agents.tools.websphere;

import java.io.File;
import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import java.util.List;

/**
 * RemoveGroupHandler will remove the OpenSSO Enterprise group name in admin-authz.xml file
 * for WAS role.
 *
 */
public class RemoveGroupHandler extends GroupUpdateHandlerBase
        implements IToolsOptionHandler, InstallConstants,
        IConstants {
    
    public RemoveGroupHandler() {
        super();
    }
    
    public boolean checkArguments(List arguments) {
        return super.checkArguments(arguments);
    }
    
    public void handleRequest(List arguments) {
        
        Debug.log("RemoveGroupHandler: handle request");
        String amGroupName = (String)arguments.get(1);
        File file = new File((String)arguments.get(2) +
                STR_FILE_SEP + STR_ADMIN_AUTHZ_XML_FILE);
        
        try {
            XMLDocument adminauthFile = new XMLDocument(file);
            adminauthFile.setIndentDepth(2);
            XMLElement authElem = findAuthElement(adminauthFile,arguments);
            
            if (authElem != null) {
                XMLElement groupElem = getElement(authElem, "groups",
                        "name", amGroupName);
                if (groupElem != null) {
                    Debug.log("RemoveGroupHandler: found <groups> element : " +
                            groupElem.toXMLString());
                    groupElem.delete();
                    Debug.log("RemoveGroupHandler: successfully remove " +
                            "<groups> element in" + " admin-authz.xml");
                    Console.println();
                    Console.println(LocalizedMessage.get(
                            LOC_HR_MSG_UPDATED_ADMIN_AUTH_FILE,
                            STR_WAS_GROUP));
                    Console.println();
                } else {
                    Debug.log(
                        "RemoveGroupHandler: " +
                        "Failed to remove <groups> element in" +
                        " admin-authz.xml");
                    Console.println();
                    Console.println(LocalizedMessage.get(
                            LOC_HR_ERR_FAILED_TO_REMOVE_GROUP,STR_WAS_GROUP));
                    Console.println();
                }
            }
            
            adminauthFile.store();
        } catch (Exception ex) {
            Debug.log(
                    "RemoveGroupHandler: failed to update admin-authz.xml",
                    ex);
            Console.println();
            Console.println(LocalizedMessage.get(
                    LOC_HR_ERR_INVALID_ADMIN_AUTH_FILE,STR_WAS_GROUP),
                    new Object[] { arguments.get(2) });
            Console.println();
        }
    }
    
    
    public void displayHelp() {
        Console.println();
        Console.println(LocalizedMessage.get(LOC_HR_MSG_REMOVEGROUP_USAGE_DESC,
                STR_WAS_GROUP));
        Console.println();
        Console.println();
        Console.println(LocalizedMessage.get(LOC_HR_MSG_REMOVEGROUP_USAGE_HELP,
                STR_WAS_GROUP));
        Console.println();
    }
    
    
    public static final String LOC_HR_MSG_REMOVEGROUP_USAGE_DESC =
            "HR_MSG_REMOVEGROUP_USAGE_DESC";
    public static final String LOC_HR_MSG_REMOVEGROUP_USAGE_HELP =
            "HR_MSG_REMOVEGROUP_USAGE_HELP";
    public static final String LOC_HR_ERR_FAILED_TO_REMOVE_GROUP =
            "HR_ERR_FAILED_TO_REMOVE_GROUP";
    
}
