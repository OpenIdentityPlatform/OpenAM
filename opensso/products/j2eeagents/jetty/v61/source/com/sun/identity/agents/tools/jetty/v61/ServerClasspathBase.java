/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: ServerClasspathBase.java,v 1.1 2009/01/21 18:43:56 kanduls Exp $
 */

package com.sun.identity.agents.tools.jetty.v61;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;


public class ServerClasspathBase implements IConstants, IConfigKeys {
    
    protected boolean updateJettyStartupJarFile(IStateAccess stateAccess) {
        Debug.log("ServerClasspathBase.updateJettyStartupJarFile(): " +
                "Update jar file");
        boolean status = false;
        ArrayList<String> agentClasspathsDirs = getAgentClasspathDirectories(
                stateAccess);
        String homeDir = (String) stateAccess.get(STR_KEY_JETTY_HOME_DIR);
        Debug.log("ServerClasspathBase.updateJettyStartupJarFile(): " + 
                "Home directory :" + homeDir);
        String startJarPath =  homeDir + STR_FORWARD_SLASH + JETTY_START_JAR;
        String newJarPath = homeDir + STR_FORWARD_SLASH + "start-temp.jar";
        JarInputStream oldJarInputStream = null;
        JarOutputStream fos =null;
        try {
            JarFile oldJarFile = new JarFile(startJarPath);
            Manifest m = oldJarFile.getManifest();
            oldJarInputStream = new JarInputStream(
                    new FileInputStream(new File(startJarPath)));
            JarEntry entry = oldJarInputStream.getNextJarEntry();
            fos = new JarOutputStream(new FileOutputStream(newJarPath), m);
            while (entry != null) {
                if (!entry.isDirectory()) {
                    fos.putNextEntry(new JarEntry(entry.getName()));
                    //byte[] bytes = new byte[BUFF_SIZE];
                    //int read = oldJarInputStream.read(bytes, 0, BUFF_SIZE);
                    //while (read > 0) {
                    //    fos.write(bytes, 0, read);
                    //    read = oldJarInputStream.read(bytes, 0, BUFF_SIZE);
                    //}
                    writeToStream(oldJarInputStream, fos);
                    if (entry.getName().indexOf(JETTY_CLASSPATH_CONF_FILE) > 0 )
                    {
                        Iterator<String> itr = agentClasspathsDirs.iterator();
                        while (itr.hasNext()) {
                            String pathToAdd = itr.next() + "\n";
                            Debug.log("ServerClasspathBase." +
                                    "updateJettyStartupJarFile(): Adding path" 
                                    + " " + pathToAdd);
                            fos.write(pathToAdd.getBytes(), 0, 
                                    pathToAdd.getBytes().length);
                        }
                        fos.flush();
                    }
                }
                entry = oldJarInputStream.getNextJarEntry();
            }
            status = true;
        } catch (Exception  ex){
            Debug.log(
                    "ServerClasspathBase.updateJettyStartupJarFile():" +
                    " Updating jar failed with exception: " + ex.getMessage());
            status = false;
        } finally {
           try {
               if (fos != null) {
                  fos.close();
               }
               if (oldJarInputStream != null) {
                   oldJarInputStream.close();
               }
           } catch (Exception ex) {
               //ignore
           }
        }
        try {
           if (status) {
               FileUtils.copyFile(newJarPath, startJarPath);
           }
        } catch (Exception ex) {
             Debug.log("ServerClasspathBase.updateJettyStartupJarFile(): " +
                     "Error copying jar file: " + ex.getMessage());
             status = false;
        } finally {
           if (newJarPath != null) {
               File delFile = new File(newJarPath);
               delFile.delete();
           }
        }
        return status;
    }

    private ArrayList<String> getAgentClasspathDirectories(
            IStateAccess stateAccess) {
        Debug.log("UpdateJettyServerClasspath.getAgentClasspathDirectories():" +
                " Get Agent Classpath Directories");
        ArrayList<String> pathDirs = new ArrayList<String>();
        String homeDir = ConfigUtil.getHomePath();
        String instanceName = stateAccess.getInstanceName();
        StringBuffer sb = new StringBuffer();
        sb.append(homeDir);
        sb.append(STR_FORWARD_SLASH);
        sb.append(instanceName);
        sb.append(STR_FORWARD_SLASH);
        sb.append(STR_INSTANCE_CONFIG_DIR_NAME);
        String configDir = sb.toString();
        String agentLibPath = ConfigUtil.getLibPath();
        String agentJarPath = agentLibPath + STR_FORWARD_SLASH + AGENT_JAR;
        String sdkJarPath = agentLibPath + STR_FORWARD_SLASH + 
                OPENSSO_CLIENT_SDK_JAR;
        String agentLocaleDir = ConfigUtil.getLocaleDirPath();
        pathDirs.add(configDir);
        pathDirs.add(agentJarPath);
        pathDirs.add(sdkJarPath);
        pathDirs.add(agentLocaleDir);
        return pathDirs;
    }
    
    protected boolean removeClasspath(IStateAccess stateAccess) {
        Debug.log("ServerClasspathBase.removeClasspath(): " +
                "Remove agent classpath from start.jar.");
        boolean status = false;
        ArrayList<String> agentClasspathsDirs = getAgentClasspathDirectories(
                stateAccess);
        String homeDir = (String) stateAccess.get(STR_KEY_JETTY_HOME_DIR);
        Debug.log("ServerClasspathBase.removeClasspath(): " + 
                "Home directory :" + homeDir);
        String startJarPath =  homeDir + STR_FORWARD_SLASH + JETTY_START_JAR;
        String newJarPath = homeDir + STR_FORWARD_SLASH + "start-temp.jar";
        JarInputStream oldJarInputStream = null;
        JarOutputStream fos =null;
        FileOutputStream tempConfFileOut = null;
        FileInputStream tempConfFileIn = null;
        String tempFile = TEMP_DIR + STR_FORWARD_SLASH + "start-conf.temp";
        try {
            JarFile oldJarFile = new JarFile(startJarPath);
            Manifest m = oldJarFile.getManifest();
            oldJarInputStream = new JarInputStream(
                    new FileInputStream(new File(startJarPath)));
            JarEntry entry = oldJarInputStream.getNextJarEntry();
            fos = new JarOutputStream(new FileOutputStream(newJarPath), m);
            while (entry != null) {
                if (!entry.isDirectory()) {
                    fos.putNextEntry(new JarEntry(entry.getName()));
                    if (entry.getName().indexOf(JETTY_CLASSPATH_CONF_FILE) > 0) 
                    {
                        tempConfFileOut = new FileOutputStream(
                                new File(tempFile));
                        writeToStream(oldJarInputStream, tempConfFileOut);
                        tempConfFileOut.flush();
                        tempConfFileOut.close();
                        Iterator<String> itr = agentClasspathsDirs.iterator();
                        int lineNo = 0;
                        while (itr.hasNext()) {
                            String pathToDel = itr.next();
                            lineNo = FileUtils.getFirstOccurence(tempFile,
                                    pathToDel, true, false, false);
                            FileUtils.removeLinesByNum(tempFile, lineNo, 1);
                        }
                        //Now write the file back to fos
                        tempConfFileIn = new FileInputStream(
                                new File(tempFile));
                        writeToStream(tempConfFileIn, fos);
                    } else {
                        writeToStream(oldJarInputStream, fos);
                    }
                    entry = oldJarInputStream.getNextJarEntry();
                }
            }
            status = true;
        } catch (Exception  ex){
            Debug.log(
                    "ServerClasspathBase.updateJettyStartupJarFile():" +
                    " Updating jar failed with exception: " + ex.getMessage());
            status = false;
        } finally {
           try {
               if (fos != null) {
                  fos.close();
               }
               if (oldJarInputStream != null) {
                   oldJarInputStream.close();
               }
               if (tempConfFileOut != null) {
                   tempConfFileOut.close();
               }
               if (tempConfFileIn != null) {
                   tempConfFileIn.close();
               }
           } catch (Exception ex) {
               //ignore
           }
        }
        try {
           if (status) {
               FileUtils.copyFile(newJarPath, startJarPath);
           }
        } catch (Exception ex) {
             Debug.log("ServerClasspathBase.updateJettyStartupJarFile(): " +
                     "Error copying jar file: " + ex.getMessage());
             status = false;
        } finally {
           if (newJarPath != null) {
               File delFile = new File(newJarPath);
               delFile.delete();
           }
           if (tempFile != null) {
               File delFile = new File(tempFile);
               delFile.delete();
           }
        }
        return status;
    }
   
    protected void writeToStream(InputStream in, OutputStream out) 
        throws IOException {
        byte[] bytes = new byte[BUFF_SIZE];
        int read = in.read(bytes, 0, BUFF_SIZE);
        while (read > 0) {
            out.write(bytes, 0, read);
            read = in.read(bytes, 0, BUFF_SIZE);
        }
    }
}
