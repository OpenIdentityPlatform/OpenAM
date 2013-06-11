/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: URLResourceName.java,v 1.1 2009/11/24 21:42:35 madan_ranganath Exp $
 *
 */


package com.sun.identity.shared.whitelist;

import java.util.StringTokenizer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URL;
import java.net.MalformedURLException;
import com.sun.identity.shared.debug.Debug;

/**
 * This plugin extends the functionality provided in
 * <code>PrefixResourceName</code> to provide special handling to
 * URL type prefix resource names in <code>canonicalize</code> method
 * like validating port, assigning default port of 80, if port absent etc.
 */
public class URLResourceName 
       extends PrefixResourceName implements Comparator {

    private Debug debug = Debug.getInstance("patternMatching");
    private static Comparator comparator = new URLResourceName();
    private static final String QUERY_PARAMETER_DELIMITER = "&";
    private static final String QUERY_PARAMETER_VALUE_DELIMITER = "=";
    private static final String DEFAULT_WEB_PROTOCOL = "http";
    private static final String SECURE_WEB_PROTOCOL = "https";
    private static final String DEFAULT_PORT = "80";
    private static final String SECURE_PORT = "443";

    /**
     * This method is used to canonicalize a url string.
     * it removed leading delimiters after the protocol
     * http:////abc becomes http://abc
     * if port number is provided validates it to be either wildcard
     * a valid integer, if not provided, adds default port 80
     * Makes sure URL is not malformed, also if query parameters
     * are present sorts them based on the comparator's sort method.
     * 
     * @param urlStr the url string to be canonicalized
     * @return the url string in its canonicalized form.
     * @throws MalformedURLException if the url string is invalid
     */

    public String canonicalize(String urlStr)
        throws MalformedURLException {

         /* if no http or https protocol resources
         * only call super.canonicalize() 
         * to validate the wildcard usage and
         * remove extra delimiters.
         */
        if ((!urlStr.startsWith(DEFAULT_WEB_PROTOCOL + "://")) 
             && (!urlStr.startsWith(SECURE_WEB_PROTOCOL + "://"))) {
            return super.canonicalize(urlStr);
        }
        int index = urlStr.indexOf("://"); 
        String proto = urlStr.substring(0, index);
        String resource = urlStr.substring(index + 3); // host.sample.com...

        String hostAndPort = resource;
        String urlPath = "";
        if (resource.startsWith(delimiter)) {
            int len = resource.length();
            char[] oldchars = resource.toCharArray();
            char[] newchars = new char[len];
            int j = 0;
            // charAt(0) assuming delimiter is only one character.
            while (j < len && oldchars[j] == delimiter.charAt(0)) {
                j++; // skip leading '/'
            }

            int i = 0;
            while (j < len) {
                newchars[i++] = oldchars[j++];
            }
            resource =  String.valueOf(newchars, 0, i);         
        }
        String hostName = "";
        String port = "";
        if (resource != null && resource.length() != 0) {
            index = resource.indexOf('/');
            if (index == -1) {
                index = resource.indexOf('?');
            }
            if (index != -1) {
                hostAndPort = resource.substring(0, index);
                urlPath = resource.substring(index);
            }
            hostName = hostAndPort;
            index = hostAndPort.indexOf(':');
            if (index != -1) {
                hostName = hostAndPort.substring(0, index);
                port = hostAndPort.substring(index + 1);
                validatePort(port);
            }

            /* there is no port specified in the url, add default port
             * based on the url protocol.
             */
            if ( port.length() == 0) {
                if (proto.equals(DEFAULT_WEB_PROTOCOL)) {
                    port = DEFAULT_PORT;
                } else {
                    port = SECURE_PORT;
                }
            }
        }
        // construct a new url based on the url but without port    
        String newUrl = proto + "://" + hostName + urlPath;

        // validate the new url with java.net.URL

        URL url = null;
        try {
            url = new URL(newUrl);
        } catch (MalformedURLException me) {
            throw (new MalformedURLException("Invalid Resource specified"));
        }

        StringBuffer sb = new StringBuffer(100);
        sb.append(proto);
        sb.append("://");
        sb.append(hostName);
        if (hostName.length() != 0) {
            sb.append(":");
            sb.append(port);
        }

        String query = url.getQuery();
        if (debug.messageEnabled()) {
            debug.message("URLResourceName: url query=" + query);
        }

        if (query != null) {
            int indexQuery = urlPath.lastIndexOf(query);
            String prefix = super.canonicalize(
                    urlPath.substring(0, indexQuery - 1));
            sb.append(prefix);
            sb.append('?');
            // check if there are more than one query parameters
            int indexAmp = query.indexOf(QUERY_PARAMETER_DELIMITER);
            if (indexAmp != -1) {
                // there are more than query parameters in the url
                String suffix= urlPath.substring(
                        indexQuery + query.length());
                ArrayList al = new ArrayList();
                StringTokenizer st = new StringTokenizer(query,
                        QUERY_PARAMETER_DELIMITER);
                while (st.hasMoreTokens()) {
                    al.add(st.nextToken());
                }
                // sort the query parameters based on rules of
                // the comparator
                Collections.sort(al, comparator);
                int size = al.size();
                // reconstruct the url in canonicalized form
                for (int i = 0; i < size; i++) {
                    if (i < (size-1)) {
                        sb.append((String) al.get(i)).append(QUERY_PARAMETER_DELIMITER);
                    } else {
                        sb.append((String)al.get(i));
                    }
                }
                sb.append(suffix);
            } else {
                // there is only one query parameter in the url
                sb.append(query);
            }
        } else {
            // there is no query string in the url
            sb.append(super.canonicalize(urlPath));
        }
            
        return sb.toString();
    }

    /**
     * This method is used to compare two url query parameter
     * strings. A query parameter string is in the form of
     * variablename=value.
     * 
     * @param o1 a url query parameter to be compared  
     * @param o2 a url query parameter to be compared  
     * @return -1 if o1 < o2; 0 if o1 = o2; 1 if o1 > o2
     */

    public int compare(Object o1, Object o2)
    {
       String s1 = (String)o1; 
       String s2 = (String)o2; 

       if (s1 == null) {
           if (s2 != null) {
               return -1;
           } else {
               return 0;
           }
       } else {
           if (s2 == null) {
               return 1;
           }
       }

       // neither s1 nor s2 is null
       String var1 = s1;
       String value1 = null;
       String var2 = s2;
       String value2 = null;
       int index1 = s1.indexOf(QUERY_PARAMETER_VALUE_DELIMITER); 
       int index2 = s2.indexOf(QUERY_PARAMETER_VALUE_DELIMITER); 

       if (index1 != -1) {
           var1 = s1.substring(0, index1);
           value1 = s1.substring(index1);
       }
       if (index2 != -1) {
           var2 = s2.substring(0, index2);
           value2 = s2.substring(index2);
       }

       int result = var1.compareTo(var2);
       if (result == 0) {
           // variable names are the same, we need to further
           // compare the values
           if (value1 == null) {
               if (value2 != null) {
                   result = -1;
               } else {
                   result = 0;
               }
           } else {
               if (value2 == null) {
                   result = 1;
               } else {
                   result = value1.compareTo(value2);
               }
           }
       } 
       return result;
    }

    /* Validate the port number to make sure there is no invalid
     * character in the port part.
     * Valid characters are digits and wildcard.
     */
    private void validatePort(String port)
        throws MalformedURLException
    {
        String portString = port;
        int idx = port.indexOf(wildcard);
        if (idx != -1) {
            int begin = 0; 
            int wildcardLen = wildcard.length();
            StringBuffer sb = new StringBuffer(100);
            while (idx != -1) {
                sb.append(port.substring(begin, idx));
                begin = idx + wildcardLen;
                idx = port.indexOf(wildcard, begin);
            }
            sb.append(port.substring(begin));
            portString = sb.toString();
        }
        /* wildcards have been extracted, the remainder should 
         * be an integer or an empty string.
         */
        if ((portString != null) && (portString.length() != 0)) {
            try {
                Integer.parseInt(portString);
            } catch (Exception e) {
                String objs[] = { port };
                throw (new MalformedURLException(
                       "Invalid Port number specified"));
            }
        }
        if (debug.messageEnabled()) {
            debug.message("URLResourceName: portString = " + portString);
        }
    }
}
