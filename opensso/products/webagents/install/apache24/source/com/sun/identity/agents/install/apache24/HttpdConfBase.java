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
 * $Id: HttpdConfBase.java,v 1.2 2008/06/25 05:54:35 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.install.apache24;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileEditor;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.DeletePattern;

import java.util.Map;

/**
 * Configures Apache server instance's httpd.conf
 */

public class HttpdConfBase implements 
    InstallConstants, IConstants, IConfigKeys {
    
    /**
     * Adds include dsame.conf statement to httpd.conf file.
     * @param stateAccess 
     * @return 
     */
    protected boolean addToHttpdConf(IStateAccess stateAccess) {
        boolean status = false;
        String httpdConfFile = 
            (String)stateAccess.get(STR_KEY_APC24_HTTPD_FILE);
        StringBuffer includeSb = new StringBuffer();
        includeSb.append(LINE_SEP);
        includeSb.append("include ");
        includeSb.append(getDsameConfFilePath(stateAccess));

        try {
            FileUtils.appendDataToFile(httpdConfFile, includeSb.toString());
            status = true;
        } catch (Exception e) {
            Debug.log("ConfigureHttpdConfFile.addToHttpdConf() - Error " +
                    "occurred while adding dsame.conf" +
                    "'. ", e);
        }
        return status;
    }

    /**
     * Removes include dsame.conf statement from httpd.conf file.
     */
    protected boolean removeFromHttpdConf(IStateAccess stateAccess) {
        boolean status = false;
        String httpdConfFileName = 
            (String)stateAccess.get(STR_KEY_APC24_HTTPD_FILE);
        FileEditor fileEditor = new FileEditor(httpdConfFileName);

        try {
            DeletePattern pattern = 
                new DeletePattern(getDsameConfFilePath(stateAccess),
                    DeletePattern.INT_MATCH_OCCURRANCE, 0);
            status = fileEditor.deleteLines(pattern);
        } catch (Exception e) {
            Debug.log("HttpdConfBase.removeFromHttpdConf() - " +
                    "Exception occurred while removing the Agent module from " +
                    "file '" + httpdConfFileName + "'. ", e);
        }

        return status;
    }

    /**
     * Gets dsame.conf filepath.
     * @param stateAccess 
     * @return dsame.conf filename
     */
    private String getDsameConfFilePath(IStateAccess stateAccess) {
        String homeDir = ConfigUtil.getHomePath();
        String agentLibPath = ConfigUtil.getLibPath();
        String instanceName = stateAccess.getInstanceName();
        StringBuffer confSb = new StringBuffer();
        confSb.append(homeDir).append(FILE_SEP);
        confSb.append(instanceName).append(FILE_SEP);
        confSb.append(STR_INSTANCE_CONFIG_DIR_NAME);
        confSb.append(FILE_SEP);
        confSb.append(STR_DSAME_CONF_FILE);
        return confSb.toString();
    }
}
