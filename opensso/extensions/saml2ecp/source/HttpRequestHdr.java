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
 * $Id: HttpRequestHdr.java,v 1.1 2007/10/04 16:55:28 hengming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


/**
 * Parses and stores a http server request.
 *
 */
public class HttpRequestHdr
{
    private Hashtable headers = new Hashtable();
 
   /**
    * Http Request method. Such as get or post.
    */
    private  String method = new String();

   /**
    * The requested url. The universal resource locator that
    * hopefully uniquely describes the object or service the
    * client is requesting.
    */
    private String url   = new String();

   /**
    * Version of http being used. Such as HTTP/1.0
    */
    public String version           = new String();

    private static String CR ="\r\n";

    /**
     * Return the URL string.
     */

    public String getURL() {
        return url;
    }

    /**
     * Return the method.
     */

    public String getMethod() {
        return method;
    }

    /**
     * Return the HTTP headers from the request.
     */
    public Hashtable getHeaders() {
        return headers;
    }

    /**
     * Add header to the HTTP request.
     */
    public void addHeader(String headerName, String value) {
        if (headerName.equalsIgnoreCase("ACCEPT")) {
            Set vals = (Set)headers.remove("Accept");
            if (vals != null) {
                value = value + "," + ((String)vals.iterator().next());
            }
            vals = new HashSet();
            vals.add(value);
            headers.put("Accept", vals);
        } else {
            Set vals = (Set)headers.get(headerName);
            if (vals == null) {
                vals = new HashSet();
                headers.put(headerName, vals);
            }
            vals.add(value);
        }
    }

    public int getContentLength() {

        Set vals = (Set) headers.get("Content-Length");
        if ((vals == null) || vals.isEmpty()) {
             return -1;
        }

        return Integer.parseInt((String)vals.iterator().next());
    }

    /**
     * Parses a http header from a stream.
     *
     * @param in  The stream to parse.
     * @return    true if parsing sucsessfull.
     */
    public void parse(InputStream in) throws Exception {
        String CR ="\r\n";

        boolean keepGoing = true;
        StringBuffer sb = new StringBuffer();
        int totalRead = 0;
        int b;
        boolean isFirstLine = true;
        while (keepGoing) {
            b = in.read();
            if (b == -1) {
                throw new Exception("Unexpected EOF");
            }

            totalRead++;
            sb.append((char) b);

            // \r\n
            if (sb.length() > 1 && sb.charAt(sb.length() - 2) == 13 &&
                sb.charAt(sb.length() - 1) == 10) {
                String headerLine = sb.toString();
                if (headerLine.equals(CR)) {
                    keepGoing = false;
                } else {
                    addHeaderLine(sb.toString(), isFirstLine);
                    if (isFirstLine) {
                        isFirstLine = false;
                    }
                }
                sb.setLength(0);
            }
        }
    }

    private void addHeaderLine(String headerLine, boolean isFirstLine) {

        if (isFirstLine) {
            StringTokenizer stz = new StringTokenizer(headerLine);
            if (stz.hasMoreTokens()) {
                method = stz.nextToken().toUpperCase();
            }
            if (stz.hasMoreTokens()) {
                url = stz.nextToken();
            }
            if (stz.hasMoreTokens()) {
                version = stz.nextToken();
            }
        } else {
            int index = headerLine.indexOf(':');
            if (index == - 1) {
                System.out.println("Invalid Header " + headerLine);
                return;
            }

            String headerName = headerLine.substring(0, index).trim();
            String value = headerLine.substring(index + 1).trim();
            addHeader(headerName, value);
        }
    }
 
    /*
     * Rebuilds the header in a string
     * @returns      The header in a string.
     */
    public String toString() {
        StringBuffer request = new StringBuffer(100);

        if (0 == method.length()) {
             method = "GET";
        }

        request.append(method).append(" ").append(url).append(" HTTP/1.0")
               .append(CR);
        for (Enumeration keys = headers.keys(); keys.hasMoreElements() ;){
            String key = (String) keys.nextElement();
            Set vals = (Set) headers.get(key);
            if ((vals != null) && (!vals.isEmpty())) {
                for(Iterator iter = vals.iterator(); iter.hasNext();) {
                    String val = (String)iter.next();
                    request.append(key).append(": ").append(val).append(CR);
                }
            }
        }

        request.append(CR);
 
        return request.toString();
    }
} 

