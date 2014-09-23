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
 * $Id: Task.java,v 1.10 2008/10/29 00:02:39 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2014 ForgeRock AS
 */

package com.sun.identity.workflow;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/**
 * Base class for all Tasks.
 */
public abstract class Task
    implements ITask 
{
    private static Map resMap = new HashMap();
    static String REQ_OBJ = "_request_";
    private static Debug debug = Debug.getInstance("workflow");

    /**
     * Return a trimmed String that represents the value from the
     * passed in params Map for the given key
     * @param params The Map that holds a String value or a Set containing
     * a String value for the given key
     * @param key The key to use to look up the String value in params Map
     * @return A trimmed String for the given key from the params Map.
     */
    protected String getString(Map params, String key) {
        
        String result = null;
        Object values = params.get(key);
        
        if (values != null) {
            if (values instanceof Set) {
                // Returns a trimmed String from the Set
                result = CollectionHelper.getMapAttr(params, key);
            } else {
                if (values != null) {
                    result = ((String)values).trim();
                }
            }
        }
        
        return result;
    }

    protected static ResourceBundle getResourceBundle(Locale locale) {
        ResourceBundle rb = (ResourceBundle)resMap.get(locale);
        if (rb == null){
            rb = ResourceBundle.getBundle("workflowMessages" ,locale);
            resMap.put(locale, rb);
        }
        return rb;
    }

    protected static String getMessage(String key, Locale locale) {
        ResourceBundle resBundle = getResourceBundle(locale);
        return resBundle.getString(key);
    }

    public static String getContent(String resName, Locale locale)
        throws WorkflowException {

        if (resName == null) {
            return resName;
        }

        if (resName.startsWith("http://") ||
            resName.startsWith("https://")
        ) {
            return getWebContent(resName, locale);
        } else {
            // XML content that is posted directly is escaped, un-escape before returning
            return XMLUtils.unescapeSpecialCharacters(resName);
        }
    }
    
    protected String getFileContent(String filename)
        throws WorkflowException {
        StringBuffer buff = new StringBuffer();
        try {
            FileReader input = new FileReader(filename);
            BufferedReader bufRead = new BufferedReader(input);
            String line = bufRead.readLine();
            while (line != null) {
                buff.append(line).append("\n");
                line = bufRead.readLine();
            }
            return buff.toString();
        } catch (IOException e){
            throw new WorkflowException(e.getMessage());
        }
    }

    /**
     * Reads the content in from a URL and returns as a String.
     * @param url The URL to fetch content from.
     * @param locale The locale used to construct error messages.
     * @return A string of the contents fetched from the URL.
     * @throws WorkflowException Thrown if the URL is malformed, or there is an error fetching content.
     */
    protected static String getWebContent(String url, Locale locale)
        throws WorkflowException {
        try {
            StringBuffer content = new StringBuffer();
            URL urlObj = new URL(url);
            URLConnection conn = HttpURLConnectionManager.getConnection(urlObj);
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection)conn;
                httpConnection.setRequestMethod("GET");
                httpConnection.setDoOutput(true);

                httpConnection.connect();
                int response = httpConnection.getResponseCode();
                InputStream is = httpConnection.getInputStream();
                BufferedReader dataInput = new BufferedReader(
                    new InputStreamReader(is));
                String line = dataInput.readLine();

                while (line != null) {
                    content.append(line).append('\n');
                    line = dataInput.readLine();
                }
            }
            return content.toString();
        } catch (ProtocolException e) {
            debug.error("unable to reach url: ", e);
            Object[] param = {url};
            throw new WorkflowException(MessageFormat.format(
                getMessage("unable.to.reach.url", locale), param));
        } catch (MalformedURLException e) {
            debug.error("malformed url: ", e);
            Object[] param = {url};
            throw new WorkflowException(MessageFormat.format(
                getMessage("malformedurl", locale), param));
        } catch (IOException e) {
            debug.error("unable to reach url: ", e);
            Object[] param = {url};
            throw new WorkflowException(MessageFormat.format(
                getMessage("unable.to.reach.url", locale), param));
        }
    }
    
    protected List getAttributeMapping(Map params) {
        List list = new ArrayList();
        String strAttrMapping = getString(params, ParameterKeys.P_ATTR_MAPPING);
        if ((strAttrMapping != null) && (strAttrMapping.length() > 0)) {
            StringTokenizer st = new StringTokenizer(strAttrMapping, "|");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.length() > 0) {
                    list.add(s);
                }
            }
        }
        return list;
    }
    
    static String generateMetaAliasForIDP(String realm)
        throws WorkflowException {
        try {
            Set metaAliases = new HashSet();
            SAML2MetaManager mgr = new SAML2MetaManager();
            metaAliases.addAll(
                mgr.getAllHostedIdentityProviderMetaAliases(realm));
            metaAliases.addAll(
                mgr.getAllHostedServiceProviderMetaAliases(realm));
            String metaAliasBase = (realm.equals("/")) ? "/idp" : realm + "/idp";
            String metaAlias = metaAliasBase;
            int counter = 1;

            while (metaAliases.contains(metaAlias)) {
                metaAlias = metaAliasBase + Integer.toString(counter);
                counter++;
            }
            return metaAlias;
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        }
    }
    
    static String generateMetaAliasForSP(String realm)
        throws WorkflowException {
        try {
            Set metaAliases = new HashSet();
            SAML2MetaManager mgr = new SAML2MetaManager();
            metaAliases.addAll(
                mgr.getAllHostedIdentityProviderMetaAliases(realm));
            metaAliases.addAll(
                mgr.getAllHostedServiceProviderMetaAliases(realm));
            String metaAliasBase = (realm.equals("/")) ? "/sp" : realm + "/sp";
            String metaAlias = metaAliasBase;
            int counter = 1;

            while (metaAliases.contains(metaAlias)) {
                metaAlias = metaAliasBase + Integer.toString(counter);
                counter++;
            }
            return metaAlias;
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        }
    }

    protected String getRequestURL(Map map) {
        boolean isConsoleRemote = Boolean.valueOf(
            SystemProperties.get(Constants.AM_CONSOLE_REMOTE)).booleanValue();

        if (isConsoleRemote) {
            return SystemProperties.getServerInstanceName();
        } else {
            HttpServletRequest req = (HttpServletRequest) map.get(REQ_OBJ);
            String uri = req.getRequestURI().toString();
            int idx = uri.indexOf('/', 1);
            uri = uri.substring(0, idx);
            return req.getScheme() + "://" + req.getServerName() +
                ":" + req.getServerPort() + uri;
        }

    }
}
