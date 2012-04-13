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
 * $Id: MigrateConfigurePropertiesTask.java,v 1.3 2008/06/25 05:51:21 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;

/**
 * This class extends class ConfigurePropertiesTask and adds extra steps
 * to merge previous product's config file with the one newly generated.
 */
public class MigrateConfigurePropertiesTask extends ConfigurePropertiesTask {
    
	// parameters not to be migrated from previous product
	private static HashSet nonMigratedParameters = new HashSet();
	static {
		nonMigratedParameters.add("com.iplanet.services.debug.directory");
		nonMigratedParameters.add("com.sun.identity.agents.config.local.logfile");
	}
	
    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties)
    throws InstallException {
        
        boolean status = false;
        status = super.execute(name, stateAccess, properties);
        
        if (status) {
            status = false;
            String instanceConfigFileMigrate = (String) stateAccess
                    .get(STR_CONFIG_DIR_PREFIX_MIGRATE_TAG);
            
            Debug.log("MigrateConfigurePropertiesTask.execute() - " +
                    "instance config file name to migrate from: " + 
                    instanceConfigFileMigrate);
            
            String instanceConfigFile = (String) stateAccess
                    .get(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG);
            
            Debug.log("MigrateConfigurePropertiesTask.execute() - " +
                    "instance config file name: " + instanceConfigFile);
            
            String configFile = (String) stateAccess
            .get(STR_CONFIG_FILE_PATH_TAG);
    
            Debug.log("MigrateConfigurePropertiesTask.execute() - " +
            "config file name: " + instanceConfigFile);
    
            
            try {
                mergeConfigFiles(instanceConfigFileMigrate, instanceConfigFile);
                status = true;
                
                mergeConfigFiles(instanceConfigFileMigrate, configFile);
                status = true;
                
            } catch (Exception e) {
                Debug.log(
                        "MigrateConfigurePropertiesTask.execute() - Exception "
                        + "occurred while merging config files. ", e);
            }
        }
        return status;
    }
    
    /**
     * merge previous product's config file with the one newly generated.
     *
     * @param instanceConfigFileMigrate
     * @param instanceConfigFile
     * @throws Exception
     */
    private void mergeConfigFiles(String instanceConfigFileMigrate,
            String instanceConfigFile ) throws Exception {
        
        BufferedReader br = null;
        PrintWriter pw = null;
        
        Debug.log(
                "MigrateConfigurePropertiesTask.mergeConfigFiles() - " +
                "config file to migrate from: " + instanceConfigFileMigrate +
                " config file to migrate to: " + instanceConfigFile);
        
        try {
            FileReader fr = new FileReader(instanceConfigFile);
            br = new BufferedReader(fr);
            
            String tmpFileName = instanceConfigFile + ".tmp";
            pw = new PrintWriter(new FileWriter(tmpFileName));
            
            String lineData = null;
            KeyValue keyValue = null;
            ArrayList migrateLines = null;
            
            while ((lineData = br.readLine()) != null) {
                lineData = lineData.trim();
                
                if (lineData.startsWith(FileUtils.HASH) ||
                        lineData.length() == 0) {
                    // write back comment statement and empty line.
                    pw.println(lineData);
                } else {
                    
                    keyValue = new KeyValue(lineData);
                    
                    if (nonMigratedParameters.contains(keyValue.getKey())) {
                    	pw.println(lineData);
                    	continue;
                    }
                    
                    migrateLines = getMigrateLines(keyValue.getParameter(),
                            instanceConfigFileMigrate);
                    
                    Debug.log(
                        "MigrateConfigurePropertiesTask.mergeConfigFiles() - " +
                        "parameter: " + keyValue.getParameter() +
                        " matched migration parameters: " + migrateLines);
                    
                    if (migrateLines.size() > 0) {
                        for (int i=0; i<migrateLines.size(); i++) {
                            pw.println(migrateLines.get(i));
                        }
                    } else {
                        // new parameter, write back.
                        pw.println(lineData);
                    }
                } // end of if (lineData..
            } // end of while
            
            br.close();
            pw.flush();
            pw.close();
            
            FileUtils.copyFile(tmpFileName, instanceConfigFile);
            File tmpFile = new File(tmpFileName);
            tmpFile.delete();
            
        } catch (Exception ex) {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ex1) {}
            }
            if (pw != null) {
                try {
                    pw.close();
                } catch (Exception ex1) {}
            }
        }
    }
    
    /**
     * read the lines of parameters within previous product's config file
     * for each parameter.
     *
     * @param parameter
     * @param instanceConfigFileMigrate
     * @return ArrayList of matching lines to be migrated from.
     * @throws Exception
     */
    private ArrayList getMigrateLines(String parameter,
            String instanceConfigFileMigrate) throws IOException {
        
        ArrayList migrateLines = new ArrayList();
        
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(instanceConfigFileMigrate);
            br = new BufferedReader(fr);
            
            String lineData = null;
            
            while ((lineData = br.readLine()) != null) {
                if (!lineData.startsWith(FileUtils.HASH) &&
                        lineData.indexOf(parameter) >= 0) {
                    KeyValue keyValue = new KeyValue(lineData);
                    if (keyValue.getParameter().equals(parameter)) {
                        migrateLines.add(lineData);
                    }
                }
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                }
            }
        }
        return migrateLines;
        
    }
    
        /*
         * inner class to parse&wrap config parameters.
         */
    class KeyValue {
        String key = null;
        String parameter = null;
        String value = null;
        
        public KeyValue(String lineData) {
            int index = 0;
            if (lineData != null && lineData.length() != 0) {
                index = lineData.indexOf(FileUtils.EQUAL_TO);
                if (index > 0) {
                    key = lineData.substring(0, index).trim();
                    value = lineData.substring(index+1).trim();
                    getParameterName();
                }
            }
        }
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getParameter() {
            return parameter;
        }
        
        public void setParameter(String parameter) {
            this.parameter = parameter;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        private void getParameterName() {
            parameter = key;
            
            if (key != null && key.length() > 0) {
                if (key.endsWith(FileUtils.SQRBRACKET_CLOSE)) {
                    int index = key.lastIndexOf(FileUtils.SQRBRACKET_OPEN);
                    if (index > 0) {
                        parameter = key.substring(0, index).trim();
                    }
                }
            }
        } // end of getParameterName
        
    }
    
}
