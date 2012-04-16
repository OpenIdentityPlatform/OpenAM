/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.upgrade;

import com.sun.identity.setup.IHttpServletRequest;
import com.sun.identity.setup.SetupConstants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author steve
 */
public class UpgradeHttpServletRequest implements IHttpServletRequest {
    protected Locale locale;
    protected Map<String, String> parameters;
    protected String contextPath;
    
    public UpgradeHttpServletRequest(String baseDir) 
    throws UpgradeException {
        parameters = new HashMap<String, String>();
        
        try {
            initialize(baseDir);
        } catch (IOException ioe) {
            UpgradeUtils.debug.error("Unable to initialize UpgradeHttpServletRequest", ioe);
            throw new UpgradeException("Unable to initialize UpgradeHttpServletRequest" + ioe.getMessage());
        }
    }
    
    private void initialize(String baseDir)
    throws IOException {
        Set<String> file = new HashSet<String>();
        BufferedReader fileIn = 
                new BufferedReader(new FileReader(baseDir + SetupConstants.CONFIG_PARAM_FILE));
        String input;
        
        try {
            while ((input = fileIn.readLine()) != null) {
                file.add(input);
            }
        } finally {
            fileIn.close();
        }
        
        for (String line : file) {
            if (line.indexOf('=') == -1) {
                continue;
            }
            
            String attributeName = line.substring(0, line.indexOf('='));
            String value = line.substring(line.indexOf('=') + 1);
            
            if (attributeName.equals("locale")) {
                locale = new Locale(value);
            }
            
            if (attributeName.equals(SetupConstants.CONFIG_VAR_SERVER_URI)) {
                contextPath = getContextPath(value);
            }
            
            parameters.put(attributeName, value);
        }
        //If a given instance was added to an existing deployment, then .configParam
        //will not contain URLAccessAgent's password, so this hack makes sure
        //that the password and confirmation is always present, this way
        //ServicesDefaultValues#validatePassword will always succeed
        parameters.put(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD, "********!");
        parameters.put(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM, "********!");
    }
    
    public Locale getLocale() {
        return locale;
    }

    public void addParameter(String parameterName, Object parameterValue) {
        parameters.put(parameterName, (String) parameterValue);
    }

    public Map getParameterMap() {
        return parameters;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getHeader(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * In OpenAM prior to 10, the .configParam file set the SERVER_URI property
     * like this:
     * 
     * SERVER_URI=/openam/config/wizard/wizard.htm
     * 
     * This method ensures the correct result of /openam is always returned
     * regardless of the file.
     * 
     * @param uri The entry from the .configParam file
     * @return The SERVER_URI; typically /openam
     */
    protected String getContextPath(String uri) {
        if (uri.indexOf('/') == uri.lastIndexOf('/')) {
            return uri;
        } else {
            return uri.substring(0, uri.indexOf('/', 1));
        }
    }
}
