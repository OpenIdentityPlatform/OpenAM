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
 * $Id: RESTUtils.java,v 1.5 2008/08/19 19:13:03 veiming Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Utility class to call REST APIs on OpenSSO.
 */
public class RESTUtils {
    
    /**
     * call the REST service URL and return its reponse.
     * @param url the URL to be called
     * @postData the data to be posted over the url
     * @return the response from called URL
     */
    public static RESTResponse callServiceURL(String url,
            String postData) throws MalformedURLException, IOException {
        
        URL serviceURL = null;
        HttpURLConnection urlConnect = null;
        DataOutputStream output = null;
        BufferedReader reader = null;
        RESTResponse response = new RESTResponse();
        ArrayList returnList = new ArrayList();
        try {
            serviceURL = new URL(url);
            urlConnect = (HttpURLConnection)serviceURL.openConnection();
            urlConnect.setRequestMethod("POST");
            urlConnect.setUseCaches(false);
            urlConnect.setDoOutput(true);
            //urlConnect.setConnectTimeout(10000);
            urlConnect.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            
            // post data
            output = new DataOutputStream(urlConnect.getOutputStream());
            output.writeBytes(postData);
            output.flush();
            
            // read response
            response.setResponseCode(urlConnect.getResponseCode());
            
            reader = new BufferedReader(new InputStreamReader(
                    urlConnect.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                returnList.add(line);
            }
            
        } catch (FileNotFoundException ex) {
            throw ex;
            
        } catch (UnknownHostException ex) {
            throw ex;

        } catch (ConnectException ex) {
            throw ex;

        } catch (IOException ex) {
            BufferedReader br = null;
            try {
                InputStream is = urlConnect.getErrorStream();
                br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line=br.readLine()) != null) {
                    returnList.add(line);
                }
            } finally {
                if (br != null) {
                    try { br.close(); } catch(Exception e) {}
                }
            }
            
        } finally {
            if (output != null) {
                try { output.close(); } catch (Exception ex) {}
            }
            if (reader != null) {
                try { reader.close(); } catch (Exception ex) {}
            }
        }
        response.setContent(returnList);
        
        return response;
    }
    
    /**
     * Inner public class to encapsulate the reponse from REST API.
     */
    public static class RESTResponse {
        private int responseCode = -1;
        private ArrayList content = null;
        
        public ArrayList getContent() {
            return content;
        }
        
        public void setContent(ArrayList content) {
            this.content = content;
        }
        
        public int getResponseCode() {
            return responseCode;
        }
        
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }
        
        public String toString() {
            StringBuffer buffer = null;
            if (content != null) {
                buffer = new StringBuffer();
                for (int i=0; i<content.size(); i++) {
                    buffer.append(content.get(i) + "\n");
                }
            } // end of if (...
            return buffer.toString();
        } // end of toString()
        
    }
}
