/* The contents of this file are subject to the terms
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
 * $Id: IdSvcsCommon.java,v 1.6 2008/09/25 22:42:53 vimal_67 Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** 
 * This class contains the common methods for the 
 * Identity service Operations
 */
public class IdSvcsCommon extends TestCommon {

    private String serverURI;
    private TextPage page;
    private WebClient webClient;
                
    /**
     * Class constructor Definition
     */
    public IdSvcsCommon() throws Exception {
        super("IdSvcsCommon");
        serverURI = protocol + ":" + "//" + host + ":" + port + uri;
    }
    
    /** 
     * This function authenticates the identity and returns
     * the token as String. It is a common authenticate 
     * function used in all methods
     */
    public String authenticateREST(String user, String password)
            throws Exception {
        entering("authenticateREST", null);
        String token = null;
        try {
            webClient = new WebClient();
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + user +
                    "&password=" + password);
            String s0 = page.getContent();
            log(Level.FINEST, "authenticateREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            token = s0.substring(i1 + 1, s0.length()).trim();
        } catch (Exception e) {
            log(Level.SEVERE, "authenticateREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("authenticateREST");
        return token;
    }

    /** 
     * This function deletes the Identity using Identity Type.
     * It is a common delete method for all kinds of Identities 
     */
    public void commonDeleteREST(String identity, String identity_type, 
            String token) throws Exception {
        try {
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/delete?&identity_name=" + identity + "&admin=" +
                    URLEncoder.encode(token, "UTF-8") + "&identity_type=" +
                    identity_type);
            log(Level.FINEST, "commonDeleteREST", "Page: " +
                    page.getContent());
        } catch (Exception e) {
            log(Level.SEVERE, "commonDeleteREST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Common Function for the URL of REST Operations. 
     * It takes operations like create, read, search,..etc, map of parameters
     * like identity name, identity type,..etc, and map of attributes like cn,
     * sn, ..etc
     */
    public TextPage commonURLREST(String operation, Map par_map, Map<String, 
            Set<String>> att_map, String token) throws Exception {
        entering("commonURLREST", null);
        TextPage URL = null;
        String parameters = "";
        String readParameters = "";
        String att_names = "";
        Object identity_value = null;
        try {
            
            // parameters Map    
            Set set_par = par_map.keySet();
            Iterator iter_par = set_par.iterator();
            while (iter_par.hasNext()) {
                Object key = iter_par.next();       
                Object value = par_map.get(key);    
                if (key.equals("identity_name")) {
                    identity_value = par_map.get(key);
                }
                if (key.toString().contains("attributes")) {
                    readParameters = readParameters + "&" + key.toString() +
                            "=" + value.toString();
                } else {
                    parameters = parameters + "&" + key + "=" + value;
                }
            }
            log(Level.FINEST, "commonURLREST", "Parameters URL: " + parameters);
            
            // attributes names Map            
            Set set_att = att_map.keySet();
            Iterator iter_att = set_att.iterator();
            while (iter_att.hasNext()) {
                String value = "";
                String keyString = "";
                String valueString = "";
                Object key = iter_att.next(); 
                Set values = (Set) att_map.get(key);
                
                if (operation.equals("search") || 
                        operation.equals("attributes")) {
                    keyString = "&attributes_names=" + key;
                } else {
                    keyString = "&identity_attribute_names=" + key;
                } 
                String rdattrsKey = "identitydetails.attribute.name=" +
                        key;
                String[] strValuesArr = (String[]) values.toArray(
                        new String[0]);
                for (int j = 0; j < strValuesArr.length; j++) {
                    value = strValuesArr[j];
                    if (value.toString().contains("^")) {
                        value.toString().replace("^", ",");
                    }
                
                    // Calling REST Operation read
                    // Checking attributes one by one after calling read
                    if (operation.equals("read")) {
                        String rdattrsValue = "identitydetails.attribute." +
                                "value=" + value;
                                                                                            
                        // Reading the attributes 
                        URL  = (TextPage)webClient.getPage(serverURI +
                                "/identity/read?&attributes_names=" + key + 
                                parameters + readParameters + "&admin=" + 
                                URLEncoder.encode(token, "UTF-8"));
                        log(Level.FINEST, "commonURLREST", "Page for " +
                                operation + " : " + URL.getContent());
                        if (URL.getContent().contains(rdattrsKey) && 
                                URL.getContent().contains(rdattrsValue)) {
                            log(Level.FINEST, "commonURLREST", operation + " " + 
                                    "attribute with Key " + key + " and " +
                                    "Value " + value + " together found");
                        } else if (URL.getContent().contains(rdattrsKey)) {
                            log(Level.FINEST, "commonURLREST", operation + " " +  
                                    "attribute with Key " + key +
                                    " only found");
                        } else {
                            log(Level.FINEST, "commonURLREST", operation + " " + 
                                    "attribute with neither Key " + key +
                                    " nor " + "Value " + value + " found");
                            assert false;
                        }
                    } else if (operation.equals("update")) {
                    
                        // Reading the attributes with old values
                        URL  = (TextPage)webClient.getPage(serverURI +
                                "/identity/read?&attributes_names=" + key + 
                                "&name=" + identity_value + readParameters + 
                                "&admin=" + URLEncoder.encode(token, "UTF-8"));
                        log(Level.FINEST, "commonURLREST", "Page for " +
                                operation + " : " + URL.getContent());
                        valueString = "&identity_attribute_values_" + key +
                                "=" + value + valueString;
                    } else if (operation.equals("search")) {
                        valueString = "&attributes_values_" + key + "=" +
                                value + valueString;
                    } else {
                        valueString = "&identity_attribute_values_" + key +
                                "=" + value + valueString;
                    }
                }
                att_names = keyString + valueString + att_names;
            }
            log(Level.FINEST, "commonURLREST", 
                    "Attributes Names URL: " + att_names);
            log(Level.FINEST, "commonURLREST ", "Operation: " + operation);
            
            // Calling REST Operations
            if (operation.equals("search")) {
                URL = (TextPage)webClient.getPage(serverURI +
                      "/identity/" + operation + "?" + parameters + att_names +
                      "&admin=" + URLEncoder.encode(token, "UTF-8"));
            } else if (operation.equals("create")) {
                URL = (TextPage)webClient.getPage(serverURI +
                      "/identity/" + operation + "?" + parameters + att_names +
                      "&admin=" + URLEncoder.encode(token, "UTF-8"));
            } else if (operation.equals("delete")) {
                URL = (TextPage)webClient.getPage(serverURI +
                      "/identity/" + operation + "?" + parameters + 
                      "&admin=" + URLEncoder.encode(token, "UTF-8"));
            } else if (operation.equals("update")) {
                URL = (TextPage)webClient.getPage(serverURI +
                      "/identity/" + operation + "?" + parameters + att_names +
                      "&admin=" + URLEncoder.encode(token, "UTF-8"));
            } else if (operation.equals("isTokenValid")) {
                URL = (TextPage)webClient.getPage(serverURI +
                      "/identity/" + operation + "?" + parameters);
            } else if (operation.equals("attributes")) {
                URL = (TextPage)webClient.getPage(serverURI +
                      "/identity/" + operation + "?" + att_names +
                      "&subjectid=" + URLEncoder.encode(token, "UTF-8"));
            } else if (operation.equals("read")) {
                log(Level.FINEST, "commonURLREST", "Read");
            } else {
                log(Level.SEVERE, "commonURLREST", "Operation Not Found " + 
                        operation);
                assert false;
            }
            log(Level.FINEST, "commonURLREST", "Page for " +
                    operation + " : " + URL.getContent());
                                   
        } catch(Exception e) {
            log(Level.SEVERE, "commonURLREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("commonURLREST");
        return URL;
    }
    
    /** 
     * This function searches the Identity.
     * It is a common search method for all kinds of Identities 
     */
    public void commonSearchREST(String objecttype, String admintoken, 
            TextPage pageURL, String filter, String[] identities,
            Boolean contains) throws Exception {
        try {
            // If search filter contains "*"
            if (filter.contains("*")) {
                page = pageURL;
                log(Level.FINEST, "commonSearchREST", "Page: " +
                        page.getContent());
                String str = page.getContent();
                for (int i = 0; i < identities.length; i++) {
                                        
                    // For Identities exists
                    if (contains) {
                        if (!str.contains(identities[i])) {
                            log(Level.SEVERE, "commonSearchREST", "Identity " +
                                    "does not exist: " + identities[i]);
                            assert false;
                        } else {
                            log(Level.FINEST, "commonSearchREST",
                                    "Identity exists: " + identities[i]);
                        }
                    } 
                    // For Identities not exists
                    else {
                        if (str.contains(identities[i])) {
                            log(Level.SEVERE, "commonSearchREST", "Identity " +
                                    "does not exist: " + identities[i]);
                            assert false;
                        } else {
                            log(Level.FINEST, "commonSearchREST", 
                                    "Identity exists: " + identities[i]);
                        }
                    }
                }
            }
            // If search filter does not contains "*"
            else {
                for (int i = 0; i < identities.length; i++) {
                    page = pageURL;
                    log(Level.FINEST, "commonSearchREST", "Page: " +
                            page.getContent());
                    String str = page.getContent();

                    // For Identities exists
                    if (contains.TRUE) {
                        if (!str.contains(identities[i])) {
                            log(Level.SEVERE, "commonSearchREST", "Identity " +
                                    "does not exists: " + identities[i]);
                            assert false;
                        }
                    } 
                    // For Identities not exists 
                    else {
                        if (str.contains(identities[i])) {
                            log(Level.SEVERE, "commonSearchREST",
                                    "Identity exists: " + identities[i]);
                            assert false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "commonSearchREST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
  
    /** 
     * This function logs out the User.
     * It is a common logout method for all kinds of Users 
     */
    public void commonLogOutREST(String token)
            throws Exception {
        entering("commonLogOutREST", null);
        try {
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/logout?subjectid=" +
                    URLEncoder.encode(token, "UTF-8"));
                
        } catch (Exception e) {
            log(Level.SEVERE, "commonLogOutREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("commonLogOutREST");
    }
}
