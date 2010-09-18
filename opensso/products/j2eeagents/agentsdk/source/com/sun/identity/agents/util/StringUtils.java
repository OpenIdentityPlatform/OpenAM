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
 * $Id: StringUtils.java,v 1.3 2008/08/30 01:40:55 huacui Exp $
 *
 */

package com.sun.identity.agents.util;

import java.net.URLDecoder;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;

/**
 * A util class to manage a query string
 */
public class StringUtils {
    
    public static String removePathInfo(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String protocol = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String requestURI = request.getRequestURI();
        String pathInfo = request.getPathInfo();
        String query = request.getQueryString();      
        if ((pathInfo != null) && (pathInfo.length() != 0)) {
            int index = requestURI.lastIndexOf(pathInfo);
            requestURI = requestURI.substring(0, index);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(protocol);
        sb.append("://");
        sb.append(serverName);
        sb.append(":");
        sb.append(serverPort);
        if ((requestURI != null) && (requestURI.length() != 0)) {
            sb.append(requestURI);
        }
        if (query != null) {
            sb.append("?");
            sb.append(query);
        }
        return sb.toString();
    }
    
   /**
    * Removes the specified parameter from the query string and returns the
    * updated query string.
    * 
    * @param rawQueryString
    * @param parameterName
    * @return
    */
    public static String removeQueryParameter(String queryString, 
            String parameterName) 
    {
        String result = getCanonicalQueryString(queryString);
        while (hasQueryParameter(result, parameterName)) {
            StringTokenizer stok = new StringTokenizer(result, "&");
            String name = parameterName + "=";
            StringBuffer buff = new StringBuffer();
            while (stok.hasMoreTokens()) {
                String nextParameter = stok.nextToken();
                if (!nextParameter.startsWith(name)) {
                    buff.append(nextParameter);
                    if (stok.hasMoreTokens()) {
                        buff.append('&');
                    }
                }
            }
            if (buff.length() > 0) {
                result = buff.toString();
            } else {
                result = null;
            }
        }
        if (result != null) {
            if (queryString.startsWith("?") && !result.startsWith("?")) {
                result = "?" + result;
            }
        }
        return result;
    }
    
   /**
    * Returns <code>true</code> if the given query string has a parameter
    * with a name specified by <code>parameterName</code> argument.
    * @param queryString
    * @param parameterName
    * @return
    */
    public static boolean hasQueryParameter(String queryString, 
            String parameterName) 
    {
        boolean result = false;
        if (queryString != null && queryString.trim().length() > 0) {
            queryString = queryString.trim();
            String name = parameterName + "=";
            int index = queryString.indexOf(name);
            if (index != -1) {
                if (index == 0) {
                    result = true;
                } else {
                    char ch = queryString.charAt(index-1);
                    if (ch == '&' || ch == '?') {
                        result = true;
                    }
                }
            }
        }
        
        return result;
    }
    
   /**
    * Returns the value of the named query parameter from the given
    * query string.
    * @param rawQueryString
    * @param parameterName
    * @return
    */
    public static String getQueryParameter(String rawQueryString, 
            String parameterName) 
    {
        String result = null;
        String value = null;
        String queryString = getCanonicalQueryString(rawQueryString);
        if (queryString != null) {
            String name = parameterName + "=";
            int index = queryString.indexOf(name);
            if (index != -1) {
                if (index == 0 || (index > 0 && 
                        queryString.charAt(index-1) == '&')) 
                    {
                        int start = index + parameterName.length();
                        if (start < queryString.length() - 1) {
                            int end = queryString.indexOf('&', start);
                            if (end == -1) {
                                value = queryString.substring(start+1);
                            } else {
                                value = queryString.substring(start+1, end);
                            }
                        }
                    }
            }
            if (value != null && value.trim().length() > 0) {
                result = URLDecoder.decode(value);
            }
        }
        return result;
    }
    
    private static String getCanonicalQueryString(String queryString) {
        String result = null;
        if (queryString != null) {
            queryString = queryString.trim();
            if (queryString.length() > 0) {
                // In certain containers, the query string may retain the
                // preceeding question mark character. If so, it should be 
                // removed.
                if (queryString.charAt(0) == '?') {
                    queryString = queryString.substring(1);
                }
            }
            
            if (queryString.trim().length() > 0) {
                result = queryString.trim();
            }
        }
        
        return result;
    }

    public static void replaceString(
            StringBuffer buff, String replace, String replaceTo)
    throws AgentException
    {
        if (buff == null || replace == null || replaceTo == null) {
            throw new AgentException("StringReplacement: "
                    + "Invalid content string or replacement value: "
                    + "buffer = " + buff + ", replace = " + replace
                    + ", replaceTo = " + replaceTo);
        }

        int loc = 0;
        int fromLen = replace.length();
        int toLen = replaceTo.length();

        while ((loc = buff.toString().indexOf(replace, loc)) != -1) {
            buff.replace(loc, loc + fromLen, replaceTo);

            loc = loc + toLen;
        }
    }
    
    public static String replaceChars(String replaceStr, char[] oldChars, 
            char newChar)
    {
        String returnStr = replaceStr;
        if (replaceStr != null && oldChars != null) {                
            char[] replaceStrChar = replaceStr.toCharArray();        
            int l1 = replaceStrChar.length;
            int l2 = oldChars.length;
            for (int i=0; i<l1; i++) {            
                for (int j=0; j<l2; j++) {
                    if (replaceStrChar[i] == oldChars[j]) {
                        replaceStrChar[i] = newChar;
                        break;
                    }
                }            
            }
            returnStr = new String(replaceStrChar);
        }
        return returnStr;
    }
    
    public static void main(String args[]) {
        String replaceStr = "/opt/App Server/test=abc/file.xml";
        char[] charsToReplace = { ' ', '=' };
        char replaceWith = '-';
        System.out.println("Original String = " + replaceStr); 
        System.out.println("Final String = " + replaceChars(replaceStr, 
                charsToReplace, replaceWith));
    }
}
