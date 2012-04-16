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
 * $Id: GroupUpdateHandlerBase.java,v 1.2 2008/11/21 22:21:43 leiming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.agents.tools.websphere;

import java.util.ArrayList;
import java.io.File;
import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;
import com.sun.identity.install.tools.handler.ConfigHandlerBase;
import java.util.List;

/**
 * GroupUpdateHandlerBase is the base class for SetGroup and RemoveGroup
 * handlers
 */
public abstract class GroupUpdateHandlerBase extends ConfigHandlerBase
        implements IToolsOptionHandler, InstallConstants,
        IConstants {
    
    public GroupUpdateHandlerBase() {
        super();
    }
    
    public boolean checkArguments(List arguments) {
        
        boolean result = true;
        String specifiedArgs = formatArgs(arguments);
        
        if (arguments != null && arguments.size() == 3) {
            
            Debug.log("GroupUpdateHandlerBase: Valid argument(s) specified - " +
                    specifiedArgs);
            String wasRoleName = (String)arguments.get(0);
            String amGroupName = (String)arguments.get(1);
            String adminAuthFile = (String)arguments.get(2);
            
            if ((wasRoleName != null) && (amGroupName != null) &&
                    (adminAuthFile != null)) {
                File file = new File(adminAuthFile + STR_FILE_SEP +
                        STR_ADMIN_AUTHZ_XML_FILE);
                if (file.exists() && file.isFile() && file.canWrite()) {
                    Debug.log("GroupUpdateHandlerBase: Valid directory to " +
                            "admin-authz.xml specified ");
                } else {
                    result = false;
                    Console.println();
                    Console.println(LocalizedMessage.get(
                            LOC_HR_ERR_UPDATE_GROUP_INVALID_ADMIN_DIR,
                            STR_WAS_GROUP));
                    Console.println();
                }
            } else {
                result = false;
                Console.println();
                Console.println(LocalizedMessage.get(
                        LOC_HR_ERR_UPDATE_GROUP_INVALID_ARGS,
                        STR_WAS_GROUP));
                Console.println();
            }
        } else {
            Console.println();
            Console.println(LocalizedMessage.get(
                    LOC_HR_ERR_INVALID_UPDATE_GROUP_OPTION,STR_WAS_GROUP),
                    new Object[] { specifiedArgs });
            Console.println();
            result = false;
        }
        return result;
    }
    
    public XMLElement findAuthElement(XMLDocument adminauthFile,
            List arguments) {
        
        XMLElement returnElem = null;
        String wasRoleName = (String)arguments.get(0);
        String amGroupName = (String)arguments.get(1);
        File file = new File((String)arguments.get(2) +
                STR_FILE_SEP + STR_ADMIN_AUTHZ_XML_FILE);
        
        try {
            ArrayList list = adminauthFile.getRootElement().
                    getNamedChildElements(
                    "rolebasedauthz:AuthorizationTableExt");
            if ((list != null) && (list.size()>0)) {
                XMLElement searchElem = (XMLElement)list.get(0);
                if (adminauthFile != null) {
                    XMLElement roleElem = getElement(searchElem,
                            "roles","roleName",wasRoleName);
                    if (roleElem != null) {
                        Debug.log(
                            "GroupUpdateHandlerBase: found <roles> element " +
                            "with " + wasRoleName + ":\n" +
                            roleElem.toXMLString());
                        String roleId = roleElem.getAttributeValue("xmi:id");
                        if (roleId != null) {
                            Debug.log(
                                "GroupUpdateHandlerBase: trying to find " +
                                "<authorizations>" + "element with attribute " +
                                "role having value =" + roleId);
                            XMLElement authElem = getElement(searchElem,
                                    "authorizations","role",roleId);
                            if (authElem != null) {
                                Debug.log(
                                    "GroupUpdateHandlerBase: found " +
                                    "<authorizations> " + "element for role " +
                                    "attr value = " + roleId);
                                returnElem = authElem;
                            } else {
                                Debug.log(
                                    "GroupUpdateHandlerBase: missing " +
                                    "<authorizations> element for role attr " +
                                    "value = " + roleId);
                                Console.println();
                                Console.println(LocalizedMessage.get(
                                        LOC_HR_ERR_INVALID_XML_FILE_FORMAT,
                                        STR_WAS_GROUP));
                                Console.println();
                            }
                        } else {
                            Debug.log(
                                    "GroupUpdateHandlerBase: missing xmi:id " +
                                    "attribute " + "for WAS role = " +
                                    wasRoleName + " in admin-authz.xml");
                            Console.println();
                            Console.println(LocalizedMessage.get(
                                    LOC_HR_ERR_INVALID_XML_FILE_FORMAT,
                                    STR_WAS_GROUP));
                            Console.println();
                        }
                    } else {
                        Debug.log(
                                "GroupUpdateHandlerBase: failed to find WAS " +
                                "role = " + wasRoleName +
                                " in admin-authz.xml");
                        Console.println();
                        Console.println(LocalizedMessage.get(
                                LOC_HR_ERR_INVALID_WAS_ROLE_NAME,STR_WAS_GROUP),
                                new Object[] { file.getAbsolutePath()});
                        Console.println();
                    }
                }
            }
        } catch (Exception ex) {
            Debug.log("GroupUpdateHandlerBase: failed to parse/find auth " +
                    "element in " + "admin-authz.xml",ex);
            Console.println();
            Console.println(LocalizedMessage.get(
                    LOC_HR_ERR_INVALID_ADMIN_AUTH_FILE,STR_WAS_GROUP),
                    new Object[] { arguments.get(2) });
            Console.println();
        }
        
        return returnElem;
        
    }
    
    /**
     * Generic function to get an element with attribute name and value
     */
    public XMLElement getElement(XMLElement parent, String elementName,
            String attrName, String attrValue)  {
        
        XMLElement result = null;
        ArrayList list = parent.getNamedChildElements(elementName);
        if (list != null &&  list.size() > 0) {
            int count = list.size();
            for (int i = 0; i < count; i++) {
                XMLElement element = (XMLElement) list.get(i);
                String value = element.getAttributeValue(attrName);
                if (value.equals(attrValue)) {
                    result = element;
                    break;
                }
            }
        }
        return result;
    }
    
    
    public static final String LOC_HR_ERR_INVALID_UPDATE_GROUP_OPTION =
            "HR_ERR_INVALID_UPDATE_GROUP_OPTION";
    public static final String LOC_HR_ERR_UPDATE_GROUP_INVALID_ARGS =
            "HR_ERR_UPDATE_GROUP_INVALID_ARGS";
    public static final String LOC_HR_ERR_UPDATE_GROUP_INVALID_ADMIN_DIR =
            "HR_ERR_UPDATE_GROUP_INVALID_ADMIN_DIR";
    public static final String  LOC_HR_ERR_INVALID_ADMIN_AUTH_FILE=
            "HR_ERR_INVALID_ADMIN_AUTH_FILE";
    public static final String LOC_HR_ERR_INVALID_WAS_ROLE_NAME =
            "HR_ERR_INVALID_WAS_ROLE_NAME";
    public static final String LOC_HR_ERR_INVALID_XML_FILE_FORMAT =
            "HR_ERR_INVALID_XML_FILE_FORMAT";
    public static final String LOC_HR_MSG_UPDATED_ADMIN_AUTH_FILE =
            "HR_MSG_UPDATED_ADMIN_AUTH_FILE";
    public static final String LOC_HR_ERROR_FAILED_UPDATE_ADMIN_AUTH_FILE =
            "HR_ERROR_FAILED_UPDATE_ADMIN_AUTH_FILE";
}
