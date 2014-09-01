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
 * $Id: Bootstrap.java,v 1.2 2008/06/25 05:44:23 qcheng Exp $
 *
 */

package com.sun.identity.test.common;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.security.AdminTokenAction;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.util.Map;
import java.util.Properties;

/**
 * This is to create <code>AMConfig.properties</code> in file system to
 * enable unittest.
 */
public class Bootstrap {
    private Bootstrap() {
    }
    
    public static void main(String[] args) {
        String basedir = args[0];
        try {
            com.sun.identity.setup.Bootstrap.load();
            SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            
            
            String serverName = SystemProperties.getServerInstanceName();
            Properties prop = ServerConfiguration.getServerInstance(
                token, serverName);
            Map defProp = ServerConfiguration.getServerInstance(
                token, ServerConfiguration.DEFAULT_SERVER_CONFIG);
            defProp.putAll(prop);
            defProp.put(SystemProperties.CONFIG_PATH, basedir);
            
            writeToFile(basedir + "/AMConfig.properties",
                FormatUtils.formatProperties(defProp));
            
            String xml = ServerConfiguration.getServerConfigXML(
                token, serverName);
            writeToFile(basedir + "/serverconfig.xml", xml);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private static void writeToFile(String outfile, String content) 
        throws FileNotFoundException, SecurityException {
        FileOutputStream fout = null;
        PrintWriter pwout = null;

        try {
            fout = new FileOutputStream(outfile, true);
            pwout = new PrintWriter(fout, true);
            pwout.write(content);
            pwout.flush();
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
                if (pwout != null) {
                    pwout.close();
                }
            } catch (IOException ex) {
                //do nothing
            }
        }
    }
}
