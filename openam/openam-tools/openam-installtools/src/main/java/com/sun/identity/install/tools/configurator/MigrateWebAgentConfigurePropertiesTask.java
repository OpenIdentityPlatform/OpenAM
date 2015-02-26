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
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.StringBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * This class extends class ConfigurePropertiesTask and performs
 * data migration. 
 */
public class MigrateWebAgentConfigurePropertiesTask extends 
	ConfigurePropertiesTask {
    
    private static HashSet agentListParameters = new HashSet();
    private static HashSet agentMapParameters = new HashSet();
    private static HashSet agentResetCookieParameters = new HashSet();
    private static HashSet nonMigratedParameters = new HashSet();

    // Properties that are of list type
    static {
        agentListParameters.add("com.sun.identity.agents.config.login.url");
        agentListParameters.add("com.sun.identity.agents.config.notenforced.url");
        agentListParameters.add("com.sun.identity.agents.config.notenforced.ip");
        agentListParameters.add(
                        "com.sun.identity.agents.config.cdsso.cdcservlet.url");
        agentListParameters.add(
                        "com.sun.identity.agents.config.cdsso.cookie.domain");
        agentListParameters.add("com.sun.identity.agents.config.logout.url");
    }

    // Properties that are of map type
    static {
        agentMapParameters.add(
                    "com.sun.identity.agents.config.profile.attribute.mapping");
        agentMapParameters.add(
                    "com.sun.identity.agents.config.session.attribute.mapping");
        agentMapParameters.add(
                   "com.sun.identity.agents.config.response.attribute.mapping");
        agentMapParameters.add(
                   "com.sun.identity.agents.config.fqdn.mapping");
    }

    // Properties that are of list type
    static {
        agentResetCookieParameters.add(
                    "com.sun.identity.agents.config.cookie.reset");
        agentResetCookieParameters.add(
                    "com.sun.identity.agents.config.logout.cookie.reset");
    }
    
    // parameters not to be migrated from previous product
    static {
        nonMigratedParameters.add("com.sun.identity.agents.config.local.logfile");
    }
    
    // New property introduced in 3.0
    public static String AGENT_ENCRYPT_KEY_PROPERTY =
                                   "com.sun.identity.agents.config.key";
    // 2.2 agent encrypt key value, used during migration
    public static String AGENT_22_ENCRYPT_KEY_VALUE = "3137517";
	
    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties)
    throws InstallException {
        
        boolean status = false;
        FileInputStream fStream = null;
        Properties mappedProperties;
        status = super.execute(name, stateAccess, properties);
        
        if (status) {
            status = false;
            String instanceConfigFileMigrate = (String) stateAccess
                    .get(STR_CONFIG_DIR_PREFIX_MIGRATE_TAG);
            
            Debug.log("MigrateWebAgentConfigurePropertiesTask.execute() - " +
                    "instance config file name to migrate from: " + 
                    instanceConfigFileMigrate);
            
            String instanceConfigFile = (String) stateAccess
                    .get(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG);
            
            Debug.log("MigrateWebAgentConfigurePropertiesTask.execute() - " +
                    "instance config file name: " + instanceConfigFile);
            
            String configFile = (String) stateAccess
            .get(STR_CONFIG_FILE_PATH_TAG);
    
            Debug.log("MigrateWebAgentConfigurePropertiesTask.execute() - " +
            "config file name: " + instanceConfigFile);

            String migratePropertiesFile = 
                    getAgentMigratePropertiesFile(stateAccess, properties);

            try {
                fStream = new FileInputStream(migratePropertiesFile);
                mappedProperties = new Properties();
                mappedProperties.load(fStream);
             } catch (Exception e) {
                 Debug.log(
                        "MigrateWebAgentConfigurePropertiesTask.execute() - " +
                        "Error loading Migrate Properties file", e);
                    throw new InstallException(LocalizedMessage
                            .get(LOC_IS_ERR_LOAD_INSTALL_STATE), e);
             } 
            try {
                mergeConfigFiles(instanceConfigFileMigrate, instanceConfigFile, 
                                 mappedProperties);
                mergeConfigFiles(instanceConfigFileMigrate, configFile, 
                                 mappedProperties);
                status = true;
                
            } catch (Exception e) {
                Debug.log(
                  "MigrateWebAgentConfigurePropertiesTask.execute() - Exception "
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
            String instanceConfigFile,
            Properties mappedProperties ) throws Exception {
        
        BufferedReader br = null;
        PrintWriter pw = null;
	FileInputStream fStream = null;
	String oldPropertyName = null;
        
        Debug.log(
                "MigrateWebAgentConfigurePropertiesTask.mergeConfigFiles() - " +
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
                    
		    // For the new web agent property, get its corresponding
		    // old property name
		    oldPropertyName = mappedProperties.getProperty(
                                                       keyValue.getParameter());
                    migrateLines = getMigrateLines(keyValue.getParameter(),
                                                   oldPropertyName,
                                                   instanceConfigFileMigrate);
                    Debug.log(
                    "MigrateWebAgentConfigurePropertiesTask.mergeConfigFiles()- " +
                    "parameter: " + keyValue.getParameter() +
                    " matched migration parameters: " + migrateLines);
                    
                    if (migrateLines.size() > 0) {
                        for (int i=0; i<migrateLines.size(); i++) {
                            pw.println(migrateLines.get(i));
                        }
                    } else {
                        if (lineData.indexOf(AGENT_ENCRYPT_KEY_PROPERTY) >= 0) {
                            // encrypt key property
                            StringBuffer newLineData = new StringBuffer();
                            int count = 0;
                            StringTokenizer st = 
                                    new StringTokenizer(lineData, "=");
                            while (st.hasMoreElements()) {
                                String tok = st.nextToken();
                                if (count == 0) {
                                    newLineData.append(tok);
                                    newLineData.append("= ");
                                } else {
                                    newLineData.append(
                                                AGENT_22_ENCRYPT_KEY_VALUE);                                            
                                }
                                count++;
                            }
                            pw.println(newLineData);
                        } else {                            
                            // new parameter, write back.
                            pw.println(lineData);
                        }
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
    private ArrayList getMigrateLines(String parameter, String oldPropertyName,
            String instanceConfigFileMigrate) throws IOException {
        
        ArrayList migrateLines = new ArrayList();
        
        FileReader fr = null;
        BufferedReader br = null;

        if (oldPropertyName == null) {
	    return migrateLines;
        }
        try {
            fr = new FileReader(instanceConfigFileMigrate);
            br = new BufferedReader(fr);
            
            String lineData = null;
            int index = 0;
            
            while ((lineData = br.readLine()) != null) {
                if (!lineData.startsWith(FileUtils.HASH) &&
                        lineData.indexOf(oldPropertyName) >= 0) {
                    KeyValue keyValue = new KeyValue(lineData);
                    if (keyValue.getParameter().equals(oldPropertyName)) {
                        if (agentListParameters.contains(parameter)) {
                            // Property is a list 
                            StringTokenizer strTok = 
                                    new StringTokenizer(keyValue.getValue());
                            while (strTok.hasMoreElements()) {
                               migrateLines.add(
				   parameter + "[" + index + "] = " + 
                                             strTok.nextToken());
                               index++;
                            }
                        } else if (agentMapParameters.contains(parameter)) {
                            // Property is a map
                            StringTokenizer strTok_first = 
                                 new StringTokenizer(keyValue.getValue(), ",");
                            while (strTok_first.hasMoreElements()) {
                                StringTokenizer strTok_sec = 
                                    new StringTokenizer(
					strTok_first.nextToken(),"|");
                                while (strTok_sec.hasMoreElements()) {
                                    migrateLines.add(
				    parameter + "["
                                             + strTok_sec.nextToken().trim()
					     + "] = " + strTok_sec.nextToken());
                                }
                            }                            
                        } else 
			    if (agentResetCookieParameters.contains(parameter)){
                                // Property is reset cookie
                                StringTokenizer strTok = 
                                 new StringTokenizer(keyValue.getValue(), ",");
                                while (strTok.hasMoreElements()) {
                                    migrateLines.add(
                                        parameter + "[" + index + "] = " + 
                                        strTok.nextToken().trim());
                                    index++;
                                }
                        } else {
                            migrateLines.add(parameter + " = " + 
                                             keyValue.getValue());
                        }
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
