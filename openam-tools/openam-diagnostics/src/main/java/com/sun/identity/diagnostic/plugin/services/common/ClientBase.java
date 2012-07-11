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
 * $Id: ClientBase.java,v 1.1 2008/11/22 02:41:19 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;



/**
 * This is the base class for <code>Client</code> related cases.
 * Any class that needs client specific methods can use this base
 * class.
 */
public abstract class ClientBase extends ServiceBase implements ClientConstants {

    protected RESTResponse response = null;
    
    protected String getBaseUrlStr(String propName) {
        String base = null;
        try {
            URL nURL = new URL(propName);
            base = nURL.getProtocol() + "://" + nURL.getHost() +
                ":" + Integer.toString(nURL.getPort()) +
                getURI(nURL);
        } catch (Exception e) {
            //URL is Malformed
            Debug.getInstance(DEBUG_NAME).error(
                "ClientBase.getBaseUrlStr: " +
                "URL Malformed Exception", e);
        }
        return base;
    }
    
    private String getURI(URL url) {
        String uri = url.getPath();
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        int idx = uri.indexOf('/');
        if (idx != -1) {
            uri = uri.substring(0, idx);
        }
        return (uri.length() > 0) ? "/" + uri : uri;
    }
    
    protected Properties loadAgentConfigFromBootfile(String fName)
        throws Exception  {
        Properties bootProp = getPropertiesFromConfigFile(fName);
        SystemProperties.initializeProperties(bootProp);
        return bootProp;
    }
    
    private Properties getPropertiesFromConfigFile(String fName)
        throws Exception {
        Properties result = new Properties();
        BufferedInputStream instream = null;
        try {
            instream = new BufferedInputStream(
                new FileInputStream(fName));
            result.load(instream);
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (Exception ex) {
                    // No handling required
                }
            }
        }
        return result;
    }
    
    public RESTResponse callServiceURL(
        String url,
        String postData
    ) throws MalformedURLException, IOException {
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
            urlConnect.setDoOutput(true);
            urlConnect.setUseCaches(false);
            urlConnect.setConnectTimeout(10000);
            urlConnect.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
            
            // send(post) data
            output = new DataOutputStream(urlConnect.getOutputStream());
            output.writeBytes(postData);
            output.flush();
            output.close();
            output = null;
            
            // read response
            response.setResponseCode(urlConnect.getResponseCode());
            
            reader = new BufferedReader(new InputStreamReader(
                urlConnect.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                returnList.add(line);
            }
        } catch (java.io.FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            InputStream is = urlConnect.getErrorStream();
            BufferedReader br = new BufferedReader(new
                InputStreamReader(is));
            String line = null;
            while ((line=br.readLine()) != null) {
                returnList.add(line);
            }
            Debug.getInstance(DEBUG_NAME).error(
                "ClientBase.callServiceURL: " +
                "IOException from server", ex);
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
    
    protected Properties getHeadersFromURL(String urlStr) throws Exception {
        HttpURLConnection urlConn = null;
        HttpsURLConnection urlSecureConn = null;
        
        Properties headers = new Properties();
        String sHeader = "";
        String sValue = "";
        int nHeaderNumber = 1;
        
        URL url = new URL(urlStr);
        URLConnection svrConn = url.openConnection();
        if (url.getProtocol().equalsIgnoreCase("http")){
            urlConn = (HttpURLConnection)svrConn;
        } else if (url.getProtocol().equalsIgnoreCase("https")) {
            urlSecureConn = (HttpsURLConnection)svrConn;
        }
        if (urlConn != null) {
            urlConn.connect();
            while ((sHeader = urlConn.getHeaderFieldKey(
                nHeaderNumber)) != null) {
                sValue = urlConn.getHeaderField(nHeaderNumber++);
                headers.setProperty(sHeader, sValue);
            }
            urlConn.disconnect();
        } else {
            urlSecureConn.connect();
            while ((sHeader = urlSecureConn.getHeaderFieldKey(
                nHeaderNumber)) != null) {
                sValue = urlSecureConn.getHeaderField(nHeaderNumber++);
                headers.setProperty(sHeader, sValue);
            }
            urlSecureConn.disconnect();
        }
        return headers;
    }
    
    protected Properties processEntries(String resStr) {
        Properties result = new Properties();
        String attrName = null;
        String attrVal = null;
        StringTokenizer st = new StringTokenizer(resStr, "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.equalsIgnoreCase(AGENT_DETAIL_ATTR)) {
                continue;
            }
            StringTokenizer lineSt = new StringTokenizer(line, "=");
            String prop = lineSt.nextToken();
            if (prop.equalsIgnoreCase(AGENT_DETAIL_ATTR_NAME)) {
                attrName = lineSt.nextToken();
            } else if (prop.equalsIgnoreCase(AGENT_DETAIL_ATTR_VALUE)){
                int idx = line.indexOf("=");
                if (idx != -1){
                    attrVal = line.substring(idx + 1);
                } else {
                    attrVal = "";
                }
            }
            setProp(result, attrName, attrVal);
        }
        return result;
    }
    
    private void setProp(Properties prop,
        String attrName,
        String attrValue
    ) {
        if ((attrName == null) || (attrName.trim().length() == 0)) {
            return;
        }
        if (attrValue != null) {
            attrValue = attrValue.trim();
        }
        int idx1 = -1;
        int idx2 = -1;
        if ((attrValue == null) || (attrValue.length() == 0)){
            return;  // has no property to set
        }
        prop.setProperty(attrName, attrValue);
        return;
    }
    
    public class RESTResponse {
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
                    buffer.append(content.get(i)).append("\n");
                }
            }
            return buffer.toString();
        }
    }

    protected String getAppPassword(String ePass, String key) {
        char[] encryptedPasswd = new char[ePass.length() + 1];
        System.arraycopy(ePass.toCharArray(), 0, encryptedPasswd, 0, 
            ePass.length());
        encryptedPasswd[ePass.length()] = '\0';
        char[] decryptedPasswd = new char[1024];
        char[] keystr = new char[key.length() + 1];
        System.arraycopy(key.toCharArray(), 0, keystr, 0, key.length());
        keystr[key.length()] = '\0';
        int retVal = CryptUtils.decryptBase64(encryptedPasswd, decryptedPasswd,
            keystr);
        return (new String(decryptedPasswd)).trim();        
    }
}

