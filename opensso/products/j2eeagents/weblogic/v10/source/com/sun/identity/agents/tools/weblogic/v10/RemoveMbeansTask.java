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
 * $Id: RemoveMbeansTask.java,v 1.2 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

/**
 * This task remove agent's mbean jar from Weblogic's mbeantypes directory
 * during uninstall.
 */
public class RemoveMbeansTask implements ITask, IConfigKeys {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = true;
        
        // Delete the file amauthprovider.jar
        String mbeansDir = getMbeanTypesDir(stateAccess);
        File file = new File(mbeansDir, STR_MBEANS_JAR_FILE);
        Debug.log("RemoveMbeansTask.execute() - " + mbeansDir);
        
        if (!file.delete()) {
            Debug.log("RemoveMbeansTask.rollBack() Unable to delete file: " +
                    file.getAbsolutePath());
            status = false;
        }
        
        return status;
    }
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        // Nothing to roll back during un-install
        return true;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String mbeansDir = getMbeanTypesDir(stateAccess);
        Object[] args = { mbeansDir };
        LocalizedMessage message =
                LocalizedMessage.get(
                LOC_TSK_MSG_REMOVE_MBEANS_EXECUTE, STR_WL_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        // No roll back during un-install
        return null;
    }
    
    private String getMbeanTypesDir(IStateAccess stateAccess) {
        String result = "";
        
        String wlhomeDir = (String) stateAccess.get(STR_KEY_WEBLOGIC_HOME_DIR);
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);
        if (osName.toLowerCase().startsWith(STR_WINDOWS)) {
            result = wlhomeDir + "\\server\\lib\\mbeantypes\\";
        } else {
            result = wlhomeDir + "/server/lib/mbeantypes";
        }
        
        return result;
    }
    
    public static final String LOC_TSK_MSG_REMOVE_MBEANS_EXECUTE =
            "TSK_MSG_REMOVE_MBEANS_EXECUTE";
    public static final String LOC_TSK_MSG_REMOVE_MBEANS_ROLLBACK =
            "TSK_MSG_REMOVE_MBEANS_ROLLBACK";
}
