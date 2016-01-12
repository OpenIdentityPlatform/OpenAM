/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SwapClientConfig.java,v 1.2 2009/12/03 18:57:50 mrudul_uchil Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.setup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import com.iplanet.services.util.Crypt;

/**
 * Tag swap client properties for server protocol, host, port and deploy uri.
 */
public class SwapClientConfig {
    
    private SwapClientConfig() {
    }
    
    public static void main(String[] args) {
        try {
            String serverURL = args[0];
            String baseDir = args[1];
            String appUser = args[2];
            String appPassword = args[3];
            String wscProviderName = args[4];
            StringBuffer templateFile = getInputStringBuffer(args[5], false);
            createAMConfigProperties(args[6],templateFile,serverURL,baseDir,
                appUser,appPassword,wscProviderName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates AMConfig.properties file
     * @param configFile Absolute path to the AMConfig.properties to be created.
     * @param templateFile Template file for AMConfig.properties
     * @param serverURL OpenAM server URL to be swapped in the template file
     * @param baseDir Base directory for Debug and Keystore files, to be swapped in the template file
     * @param appUser Application user name to be swapped in the template file
     * @param appPassword Application user password to be swapped in the template file
     * @param wscProviderName Unique agent profile name to be used for WSC
     */
    private static void createAMConfigProperties(String configFile,
        StringBuffer templateFile, String serverURL, String baseDir,
        String appUser, String appPassword, String wscProviderName)
        throws IOException {
        String content = templateFile.toString();

        String server_protocol = null;
        String server_host = null;
        String server_port = null;
        String deploy_uri = null;

        int indexProtocol = serverURL.indexOf("://");
        if (indexProtocol != -1) {
            server_protocol = serverURL.substring(0,indexProtocol);
            String tempServerURL = serverURL.substring(indexProtocol+3);

            int indexHost = tempServerURL.indexOf(":");
            if (indexHost != -1) {
                server_host = tempServerURL.substring(0,indexHost);
                tempServerURL = tempServerURL.substring(indexHost+1);

                int indexPort = tempServerURL.indexOf("/");
                if (indexPort != -1) {
                    server_port = tempServerURL.substring(0,indexPort);
                    deploy_uri = tempServerURL.substring(indexPort);
                } else {
                    System.out.println("Error : Incorrect Server URL Syntax");
                }
            } else {
                System.out.println("Error : Incorrect Server URL Syntax");
            }
        } else {
            System.out.println("Error : Incorrect Server URL Syntax");
        }

        // Due with extra / before deployment URI
        if ((deploy_uri != null) && (deploy_uri.length() > 0)) {
            if (deploy_uri.charAt(0) != '/') {
                deploy_uri = "/" + deploy_uri;
            }
            content = content.replaceAll("/@DEPLOY_URI@", deploy_uri);
        }

        content = content.replaceAll("@SERVER_PROTOCOL@", server_protocol);
        content = content.replaceAll("@SERVER_HOST@", server_host);
        content = content.replaceAll("@SERVER_PORT@", server_port);

        String newBaseDir = baseDir.trim().replace("\\", "/");
        content = content.replaceAll("@DEBUG_DIR@", newBaseDir + "/debug");
        content = content.replaceAll("@KEYSTORE_LOCATION@", newBaseDir + "/resources");

        content = content.replaceAll("@APPLICATION_USER@", appUser);
        content = content.replaceAll("@ENCODED_APPLICATION_PASSWORD@",
            Crypt.encrypt(appPassword));

        if((wscProviderName != null) && (wscProviderName.trim().length() != 0)
                && (!wscProviderName.equals("<@wsc.providername@>"))) {
            content = content.replaceAll("com.sun.identity.wss.wsc.providername=",
                "com.sun.identity.wss.wsc.providername=" + wscProviderName);
        }

        writeToFile(content, configFile);
    }
    
    /**
     * Returns input file as StringBuffer.
     * @param filename Name of the file.
     * @param skipCopyright if false, keep copyright notice in the beginning
     *        of the input file. if true, remove the copyright notice.
     * @return StringBuffer
     */
    private static StringBuffer getInputStringBuffer(String filename, 
        boolean skipCopyright)
        throws Exception {
        StringBuffer buff = new StringBuffer(20480);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename)));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (skipCopyright && line.startsWith("#")) {
                    continue;
                } 
                // done skipping the copyright in the beginning of the file
                skipCopyright = false;
                buff.append(line).append("\n");
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return buff;
    }

    private static void writeToFile(String file1, String filename) 
        throws FileNotFoundException, IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filename);
            fout.write(file1.getBytes());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

