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
 *
 */

package com.sun.identity.agents.install.configurator;

import java.util.Map;
import java.net.URL;
import java.net.URLConnection;
import java.net.InetAddress;
import java.lang.Exception;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.StringTokenizer;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidateURL;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class ValidateServerURL extends ValidateURL {

    public ValidateServerURL() throws InstallException {
        super();
    }

    /*
     * Checks if server url is valid
     * 
     * @param url @param props @param state
     * 
     * @return ValidationResult
     */
    public ValidationResult isServerURLValid(String url, Map props,
            IStateAccess state) {
           
        // delegate this function to its proxy with timeout.
        //return new URLValidatorProxy(url, props, state, true).isURLValid();
        return isServerURLValidInternal(url, props, state);
    }
    
    /*
     * Checks if server url is valid
     * 
     * @param url @param props @param state
     * 
     * @return ValidationResult
     */
    private ValidationResult isServerURLValidInternal(String url, Map props,
            IStateAccess state) {

        LocalizedMessage returnMessage = null;
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        boolean invalidDeploymentURI = false;
	
        try {
	    URL serverUrl = new URL(url);
            String protocol = serverUrl.getProtocol();
            String hostName = serverUrl.getHost();
            int portNum = serverUrl.getPort();           
            if (portNum == -1) {
                if (protocol.equals("http")) {
                    portNum = 80;
                } else if (protocol.equals("https")) {
                    portNum = 443;
                }
            }
            String sPortNum = new Integer(portNum).toString();
            String deploymentURI = serverUrl.getPath();
            if (deploymentURI.length() > 0) {
                Map tokens = state.getData();
                
                tokens.put("SERVER_PROTOCOL", protocol);
                tokens.put("SERVER_HOST", hostName);
                tokens.put("SERVER_PORT", sPortNum);
                tokens.put("DEPLOY_URI", deploymentURI);

                state.putData(tokens);               
		// Establish the connection
	        URLConnection urlConnect = serverUrl.openConnection();
	        urlConnect.connect();
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_SERVER_URL,
                            new Object[] { url });
                state.put("isServerURLValid", "true");
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            } else {
                invalidDeploymentURI = true;
                validRes = ValidationResultStatus.STATUS_FAILED;
                returnMessage = LocalizedMessage.get(
                                LOC_VA_MSG_IN_VAL_DEPLOYMENT_URI,
                                new Object[] { url });
            }    
        } catch (UnknownHostException uhe) {
                Debug.log("ValidateURL.isServerUrlValid threw exception :",
                                uhe);
                returnMessage = LocalizedMessage.get(
                                LOC_VA_WRN_UN_REACHABLE_SERVER_URL,
                                new Object[] { url });
                state.put("isServerURLValid", "false");
	        validRes = ValidationResultStatus.STATUS_WARNING;
        } catch (ConnectException ce) {
                Debug.log("ValidateURL.isServerUrlValid threw exception :",
                                ce);
                returnMessage = LocalizedMessage.get(
                                LOC_VA_WRN_UN_REACHABLE_SERVER_URL,
                                new Object[] { url });
                state.put("isServerURLValid", "false");
	        validRes = ValidationResultStatus.STATUS_WARNING;               
        } catch (Exception e) {
             if (url.toLowerCase().startsWith("https")) {
                 Debug.log("ValidateURL.isServerUrlValid threw exception :",
                         e);
                 returnMessage = LocalizedMessage.get(
                         LOC_VA_WRN_UN_REACHABLE_SSL_SERVER_URL,
                         new Object[] { url });
                 state.put("isServerURLValid", "false");
                 validRes = ValidationResultStatus.STATUS_WARNING;
             } else {
                 Debug.log("ValidateURL.isServerUrlValid threw exception :",
                         e);
             } 	 
        }
	 if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
         {
             if (!invalidDeploymentURI) {
                returnMessage = LocalizedMessage.get(
                                LOC_VA_WRN_IN_VAL_SERVER_URL,
                                new Object[] { url });
             }
         }
        
         return new ValidationResult(validRes, null, returnMessage);
    }
}
