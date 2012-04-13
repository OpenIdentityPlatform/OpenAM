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
 * $Id: ResourceReader.java,v 1.2 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.shared.debug.Debug;

/**
 * A utility class to read a text file from the jar agent.jar or from the 
 * agent installation directories.
 */
public class ResourceReader {
    
    public ResourceReader(Debug debug) {
        setDebug(debug);
    }
    
    /**
     * Reads the text content from the specified fileName file and returns the 
     * contents of the file as a String.
     **/
    public String getTextFromFile(String fileName) throws AgentException {
            
            if (getDebug().messageEnabled()) {
                getDebug().message("ResourceReader.getTextFromFile:"
                        + " file to read from is: " + fileName);
            }
            InputStream inStream = null;
            try {
                inStream = ClassLoader.getSystemResourceAsStream(fileName);
            } catch (Exception ex) {
                if (getDebug().warningEnabled()) {
                    getDebug().warning(
                        "ResourceReader.getTextFromFile: Exception while trying"
                        + " to get system resource stream for file " 
                        + fileName, ex);
                }
            }
            
            if( inStream == null ) {
                try {
                    ClassLoader cl =
                            Thread.currentThread().getContextClassLoader();
                    if( cl != null )
                        inStream = cl.getResourceAsStream(fileName);
                    
                } catch(Exception ex) {
                    if (getDebug().warningEnabled()) {
                        getDebug().warning(
                            "ResourceReader.getTextFromFile: Exception while " 
                            + "trying to get resource for file " + fileName, ex);
                    }
                } 
            }
            
            if (inStream == null) {
                try {
                    File contentFile = new File(fileName);
                    if (!contentFile.exists() || !contentFile.canRead()) {
                      throw new AgentException("ResourceReader.getTextFromFile:"
                                + " File does not exist or can not read file"
                                + " content for file " + fileName);
                    }
                    
                    inStream = new FileInputStream(contentFile);
                } catch (Exception ex) {
                    getDebug().error(
                        "ResourceReader.getTextFromFile: Exception when trying"
                            + " to read content from file " + fileName, ex);
                    throw new AgentException(
                        "ResourceReader.getTextFromFile: Exception when trying"
                         + " to read content from file " + fileName, ex);
                }
            }
            
            if (inStream == null) {
                throw new AgentException("ResourceReader.getTextFromFile: All"
                    + " attempts to read content failed from file " + fileName);
            }
            StringBuffer contentBuffer = new StringBuffer();
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inStream));              
                String       nextLine      = null;
                
                while ( (nextLine = reader.readLine()) != null) {
                    contentBuffer.append(nextLine);
                    contentBuffer.append(IUtilConstants.NEW_LINE);
                }               
                
                if (getDebug().messageEnabled()) {
                    getDebug().message(
                        "ResourceReader.getTextFromFile: File content of file "
                        + fileName + ":" + IUtilConstants.NEW_LINE 
                        + contentBuffer.toString());
                }
            } catch (Exception ex) {
                throw new AgentException("ResourceReader.getTextFromFile:"
                        + "Buffered reader unable to read content of file" 
                        + fileName, ex);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (Exception ex) {
                        getDebug().error("ResourceReader.getTextFromFile:"
                                + " Exception while trying to close input"
                                + " stream for file " + fileName, ex);
                    }
                }
            }
            return contentBuffer.toString();
    }
        
    private void setDebug(Debug debug) {
        _debug = debug;
    }
    
    private Debug getDebug() {
        return _debug;
    }
    
    private Debug _debug;
}
