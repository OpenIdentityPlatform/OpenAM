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
 * $Id: BaseToolsResolver.java,v 1.4 2008/06/25 05:51:16 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.admin;

import java.util.ArrayList;

import com.sun.identity.install.tools.handler.InstallHandler;
import com.sun.identity.install.tools.handler.CustomInstallHandler;
import com.sun.identity.install.tools.handler.UninstallAllHandler;
import com.sun.identity.install.tools.handler.UninstallHandler;
import com.sun.identity.install.tools.handler.MigrateHandler;
import com.sun.identity.install.tools.handler.VersionHandler;
import com.sun.identity.install.tools.util.LocalizedMessage;

public abstract class BaseToolsResolver {

    public ArrayList getDefaultOptions() {
        ArrayList result = new ArrayList();
	String customInstallHelp = null;
        result.add(new ToolsOptionsInfo(InstallHandler.class.getName(),
                STR_INSTALL_OPTION, LocalizedMessage
                        .get(LOC_HR_MSG_INSTALL_SHORT_HELP)));

        if (LocalizedMessage.get(LOC_HR_MSG_CUSTOM_INSTALL_SHORT_HELP)!= null) {	
            result.add(new ToolsOptionsInfo(CustomInstallHandler.class.getName(),
                STR_CUSTOM_INSTALL_OPTION, LocalizedMessage
                        .get(LOC_HR_MSG_CUSTOM_INSTALL_SHORT_HELP)));
        }

        result.add(new ToolsOptionsInfo(UninstallHandler.class.getName(),
                STR_UNINSTALL_OPTION, LocalizedMessage
                        .get(LOC_HR_MSG_UNINSTALL_SHORT_HELP), false));

        result.add(new ToolsOptionsInfo(VersionHandler.class.getName(),
                STR_VERSION_OPTION, LocalizedMessage
                        .get(LOC_HR_MSG_VERSION_SHORT_HELP)));

        result.add(new ToolsOptionsInfo(UninstallAllHandler.class.getName(),
                STR_UNINSTALL_ALL_OPTION, LocalizedMessage
                        .get(LOC_HR_MSG_UNINSTALL_ALL_SHORT_HELP), false));
        
        result.add(new ToolsOptionsInfo(MigrateHandler.class.getName(),
                STR_MIGRATE_OPTION,
                LocalizedMessage.get(LOC_HR_MSG_MIGRATE_SHORT_HELP),
                true));        

        return result;
    }

    /**
     * Gets all the supported options. This method needs be implemented by all
     * the installer implementations, to specify the list of custom supproted 
     * options. 
     * @return an ArrayList of <code>ToolsOptionsInfo<code> objects, in the
     * desired order.
     */
    public abstract ArrayList getSupportedOptions();

    public ArrayList getAllSupportedOptions() {
        ArrayList allOptions = new ArrayList();
        allOptions.addAll(getDefaultOptions());
        allOptions.addAll(getSupportedOptions());
        return allOptions;
    }

    public static final String STR_INSTALL_OPTION = "--install";

    public static final String STR_CUSTOM_INSTALL_OPTION = "--custom-install";

    public static final String STR_UNINSTALL_OPTION = "--uninstall";

    public static final String STR_VERSION_OPTION = "--version";

    public static final String STR_UNINSTALL_ALL_OPTION = "--uninstallAll";
    
    public static final String STR_MIGRATE_OPTION = "--migrate";

    public static final String LOC_HR_MSG_INSTALL_SHORT_HELP = 
        "HR_MSG_INSTALL_SHORT_HELP";

    public static final String LOC_HR_MSG_CUSTOM_INSTALL_SHORT_HELP = 
        "HR_MSG_CUSTOM_INSTALL_SHORT_HELP";

    public static final String LOC_HR_MSG_UNINSTALL_SHORT_HELP = 
        "HR_MSG_UNINSTALL_SHORT_HELP";

    public static final String LOC_HR_MSG_VERSION_SHORT_HELP = 
        "HR_MSG_VERSION_SHORT_HELP";

    public static final String LOC_HR_MSG_UNINSTALL_ALL_SHORT_HELP = 
        "HR_MSG_UNINSTALL_ALL_SHORT_HELP";
    
     public static final String LOC_HR_MSG_MIGRATE_SHORT_HELP =
        "HR_MSG_MIGRATE_SHORT_HELP";
}
