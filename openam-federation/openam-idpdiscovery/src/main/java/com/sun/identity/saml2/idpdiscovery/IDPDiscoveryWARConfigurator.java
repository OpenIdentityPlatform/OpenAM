/*
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
 * $Id: IDPDiscoveryWARConfigurator.java,v 1.4 2008/08/19 19:11:14 veiming Exp $
 *
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */

package com.sun.identity.saml2.idpdiscovery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.ServletException;

/**
 * This class is used to setup the OpenAM IDP Discovery WAR.
 */
public class IDPDiscoveryWARConfigurator {

    /**
     * Creates libIDPDiscoveryConfig.properties file
     * @param configFile Absolute path to the libIDPDiscoveryConfig.properties 
     *    to be created.
     * @param templateFile Template file for libIDPDiscoveryConfig.properties
     * @param properties Properties to be swapped in the template
     */
    public void createIDPDiscoveryConfig(String configFile, 
        String templateFile, Properties properties) throws IOException {
        String content = getFileContent(templateFile);
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String tag = (String)i.next();
            content = content.replaceAll("@" + tag + "@",
                (String)properties.get(tag));
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
        out.write(content);
        out.close();
    }

    private String getFileContent(String fileName) throws IOException {
        InputStream in = getClass().getResourceAsStream(fileName);
        if (in == null) {
            throw new IOException("Unable to open " + fileName);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder buff = new StringBuilder();
        String line = reader.readLine();

        while (line != null) {
            buff.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        return buff.toString();      
    }

    /**
     * Sets properties from libIDPDiscoveryConfig.properties
     * @param configFile path to the libIDPDiscoveryConfig.properties file
     * @throws ServletException when error occurs
     */
    public void setIDPDiscoveryConfig(String configFile) 
        throws ServletException {
        FileInputStream fileStr = null;
        try {
            if (configFile != null) {
                SystemProperties.initializeProperties(configFile);
                ConfiguratorFilter.isConfigured = true;
            } else {
                throw new ServletException("Unable to open: " + configFile);
            }
        } catch (FileNotFoundException fexp) {
            fexp.printStackTrace();
            throw new ServletException(fexp.getMessage());
        } catch (IOException ioexp) {
            ioexp.printStackTrace();
            throw new ServletException(ioexp.getMessage());
        } finally {
            if (fileStr != null) {
                try {
                    fileStr.close();
                } catch (IOException ioe) {
                }
            } 
        }
    }
}
