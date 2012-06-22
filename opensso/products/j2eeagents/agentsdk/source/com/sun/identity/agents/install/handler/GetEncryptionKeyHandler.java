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
 * $Id: GetEncryptionKeyHandler.java,v 1.2 2008/06/25 05:51:53 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.agents.install.handler;

import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.handler.ConfigHandlerBase;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.EncryptionKeyGenerator;
import java.util.List;

/**
 * The class generates a random encryption key 
 */
public class GetEncryptionKeyHandler extends ConfigHandlerBase 
    implements IToolsOptionHandler { 
    
    public GetEncryptionKeyHandler() {
        super();
    }
    
    public boolean checkArguments(List arguments) {        
        // There should be no arguments to this option. If there is any it 
        // would an error.       
        boolean validArgs = true;
        if (arguments != null && arguments.size() > 0) {
            String specifiedArgs = formatArgs(arguments);
            Debug.log("GetEncryptionKeyHandler: invalid argument(s) specified - " + 
                specifiedArgs);
            printConsoleMessage(LOC_HR_MSG_INVALID_OPTION, 
                new Object[] { specifiedArgs });
            validArgs = false;
        }               
        return validArgs;
    }
       
    public void handleRequest(List arguments) {
        String encryptKey = EncryptionKeyGenerator.generateRandomString();
        Console.println();
        Console.println(
            LocalizedMessage.get(LOC_HR_MSG_AGENT_ENCRYPT_KEY_RESULT),
            new Object[] { encryptKey });
        Console.println();
    }
    
    public void displayHelp() {
        Console.println();        
        Console.println(LocalizedMessage.get(
            LOC_HR_MSG_GET_ENCRYPT_KEY_USAGE_DESC));        
        Console.println();
    }
   
    public static final String LOC_HR_MSG_GET_ENCRYPT_KEY_USAGE_DESC = 
        "HR_MSG_GET_ENCRYPT_KEY_USAGE_DESC";
    public static final String LOC_HR_MSG_INVALID_OPTION = 
        "HR_MSG_INVALID_OPTION";
    public static final String LOC_HR_MSG_AGENT_ENCRYPT_KEY_RESULT =
        "HR_MSG_AGENT_ENCRYPT_KEY_RESULT";

}
