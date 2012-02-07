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
 * $Id: AgentRemoteConfigUtils.java,v 1.7 2008/08/26 00:35:35 huacui Exp $
 *
 */

package com.sun.identity.agents.util;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.agents.arch.AgentException;

public class AgentRemoteConfigUtils {

    static final String USER_DETAILS = "identitydetails";
    static final String ATTRIBUTE = "attribute";
    static final String ATTRIBUTE_NAME = "name";
    static final String VALUE = "value";
    static final String FREE_FORM_PROPERTY =
                        "com.sun.identity.agents.config.freeformproperties";
    static final String ATTRIBUTE_SERVICE = "/xml/read";
    static final String VERSION_SERVICE = "/SMSServlet?method=version";
    
    /**
     * Returns <code>Properties</code> object constructed from XML.
     *
     * @param xml the XML document for the <code>Properties</code> object.
     * @return constructed <code>Properties</code> object.
     */
    public static Properties getAgentProperties(Vector urls, String tokenId, 
        String profileName, String realm) throws AgentException {
        Properties result = new Properties();
        if (urls == null) {
            return result;
        }
        
        String xml = null;
        for (int i = 0; i < urls.size(); i++) {
            URL url = (URL)urls.get(i);
            xml = getAttributesInXMLFromRest(url, tokenId, profileName, realm);
            if (xml != null) {
                break;
            }
        }
        if (xml != null) {
            Document doc = null;
            try {
                doc = XMLUtils.getXMLDocument(
                     new ByteArrayInputStream(xml.getBytes("UTF-8")));
            } catch (Exception xe) {
                throw new AgentException("xml parsing error", xe);
            }

            // get the root node of agent configuration response
            Element rootElement = doc.getDocumentElement();;
            if (rootElement == null) {
                throw new AgentException("Invalid root element");
            }
        
            String elemName = rootElement.getTagName();
            if (elemName == null) {
                throw new AgentException("Missing local name");
            } else if (!elemName.equals(USER_DETAILS)) {
                throw new AgentException("Invalid root element name");
            }

            // Get all the agent configuration attribute nodes
            NodeList nodes = rootElement.getChildNodes();
            int numOfNodes = nodes.getLength();
            if (numOfNodes < 1) {
                throw new AgentException("Missing agent configuration elements");
            }
            int nextElem = 0;
            while (nextElem < numOfNodes) {
                Node elem = (Node)nodes.item(nextElem);
                if (elem.getNodeType() == Node.ELEMENT_NODE) {
                    if (elem.getNodeName().equals(ATTRIBUTE)) {
                        String attrName = ((Element)elem).getAttribute(
                                ATTRIBUTE_NAME);
                        if ((attrName == null) || (attrName.trim().length() == 0)) {
                            throw new AgentException("Missing attribute name");
                        }
                        NodeList valueNodes = elem.getChildNodes();
                        int numOfValueNodes = valueNodes.getLength();
                        int nextValueNode = 0;
                        while (nextValueNode < numOfValueNodes) {
                            Node valueNode = (Node)valueNodes.item(nextValueNode);
                            if (valueNode.getNodeType() == Node.ELEMENT_NODE) {
                                if (valueNode.getNodeName().equals(VALUE)) {
                                    String value = XMLUtils.getElementValue(
                                                     (Element)valueNode);
                                    setProp(result, attrName, value);
                                }
                            }
                            nextValueNode++;
                        }    
                    }
                }
                nextElem++;
            }
        }
        return result;
    }
    
    /**
     * get OpenSSO server version by calling servlet SMSServlet?method=version.
     *
     * @param urls Vector of OpenSSO server URLs.
     * @return the version of OpenSSO Server
     */
    public static String getServerVersion(Vector urls, Debug debug) {
        String version = null;
        
        if (urls == null || urls.size() == 0) {
            return null;
        }
        int size = urls.size();
        URL url = null;
        for (int i=0; i<size; i++) {
            url = (URL)urls.get(i);
            String path = url.getPath();
            String appContext = null;
            
            int index = path.indexOf("/", 1);
            if (index > 0) {
                appContext = path.substring(1,index);
            } else {
                appContext = path.substring(1);
            }
            String urlString = url.getProtocol() + "://" + url.getHost() + ":" +
                    url.getPort() + "/" + appContext + VERSION_SERVICE;
            debug.message("AgentRemoteConfigUtils.getServerVersion() - " +
                    "Service URL: " +
                    urlString);
            version = callServerService(urlString, debug);
            if (version != null || version.trim().length() > 0) {
                break;
            }
        }
        
        return version;
    }
    
    /*
     * call OpenSSO service URL and return its content.
     *
     * @param urlSring OpenSSO URL to be called
     * @return String of content returned by OpenSSO URL.
     */
    private static String callServerService(String urlString, Debug debug) {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            URL url = new URL(urlString);
            
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line = null;
            while((line = br.readLine()) != null) {
                stringBuffer.append(line).append("\n");
            }
            
        } catch (Exception ex) {
            debug.message("AgentRemoteConfigUtils.callServerService() - ", ex);
        }
        
        return stringBuffer.toString();
    }

    private static void setProp(Properties prop, 
                                String attrName, 
                                String attrValue) {
        if ((attrName == null) || (attrName.trim().length() == 0)) {
            return;
        }

        if (attrName.trim().equals(FREE_FORM_PROPERTY)) {
        /* 
         This is a freeform property. Its value is in <property>=<value> format:
         com.sun.identity.agents.config.freeformproperties=my.property=hello
         com.sun.identity.agents.config.freeformproperties=my.list.property[0]=hello0
         com.sun.identity.agents.config.freeformproperties=my.list.property[1]=hello1
         The agent properties then are:
         my.property=hello
         my.list.property[0]=hello0
         my.list.property[1]=hello1
        
         Thus needs further parsing.  
        */
            if (attrValue != null) {
                attrValue = attrValue.trim();
            }
            int idx1 = -1;
            int idx2 = -1;
            if ((attrValue == null) || (attrValue.length() == 0)
                || ((idx1 = attrValue.indexOf("=")) <= 0)) {
                return;  // has no property to set
            }
            idx2 = attrValue.indexOf("["); 
            if ((idx2 > 0) && (idx2 < idx1)) {
                // "[" comes before the "="
                // so it is something like: name[key]=value
                attrName = attrValue.substring(0, idx2);
                attrValue = attrValue.substring(idx2);
            } else {
                // "=" comes before the "[" or there is no "["
                // so it is something like: name=value
                attrName = attrValue.substring(0, idx1);
                attrValue = attrValue.substring(idx1+1);
            }
                
        }    
        if (attrValue == null) {
            attrValue = "";
        } else {
            attrValue = attrValue.trim();
            if (attrValue.indexOf("[") == 0) {
                int idx = attrValue.indexOf("]");
                if (idx > 0) {
                    int delimitIndex = attrValue.indexOf("=", idx);
                    if (delimitIndex > idx) {
                        String indexKey = attrValue.substring(0, idx+1);
                        attrName += indexKey; 
                        attrValue = attrValue.substring(delimitIndex+1).trim();
                    }
                }
            }
        }
        prop.setProperty(attrName, attrValue);
        return;
    }
    
    private static String getAttributesInXMLFromRest(URL url, String tokenId,
        String profileName, String realm) throws AgentException {
	HttpURLConnection conn = null;
        char[] buf = new char[1024];
        StringBuffer in_buf = new StringBuffer();
        int len;
	try {
            String attributeServiceURL = url + ATTRIBUTE_SERVICE 
                + "?name=" + URLEncoder.encode(profileName, "UTF-8") 
                + "&attributes_names=realm"
                + "&attributes_values_realm=" 
                + URLEncoder.encode(realm, "UTF-8")
                + "&attributes_names=objecttype"
                + "&attributes_values_objecttype=Agent"
                + "&admin=" + URLEncoder.encode(tokenId, "UTF-8"); 
            URL serviceURL = new URL(attributeServiceURL);
	    conn = (HttpURLConnection)serviceURL.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
	    while((len = in.read(buf,0,buf.length)) != -1) {
	        in_buf.append(buf,0,len);
	    }
	} catch (Exception e) {
           throw new AgentException(
                   "Fetching Agent configuration properties failed", e);
	} 
        return in_buf.toString();
    }
} 
