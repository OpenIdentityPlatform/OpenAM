/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Utils.java,v 1.4 2008/11/10 22:57:00 veiming Exp $
 *
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */

package com.sun.identity.sae.api;

import org.owasp.esapi.ESAPI;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;

/**
  * Util class to implement simple Http request/response transactions.
  */
public class Utils 
{
    /**
      * Http POST
      */
    public static final String POST = "POST";

    /**
      * Http GET
      */
    public static final String GET = "GET";

    /**
     * Redirects to <code>redirectUrl</code> as a GET or a POST
     *  based on <code>action</code> parameter provided.
     *  in case of POST all params need to be specified in <code>pmap</code>
     *  parameter.
     *
     *  @param hres HttpSevletResponse to be used for the redirect
     *  @param out the print writer for writing out presentation
     *  @param redirectUrl URL to redirect to.
     *  @param pmap  http parameters to be sent as part of the redirect
     *  @param action http action to be executed : GET or POST
     */
    public static void redirect(HttpServletResponse hres, PrintWriter out, String redirectUrl,
           Map pmap, String action)
       throws Exception
    {
        if (action.equals("GET")) {
             StringBuilder buf = null;
             if (pmap != null) {
                 // Put it all together in query part of Url
                 String query = queryStringFromMap(pmap);
                 buf = new StringBuilder();
                 buf.append(redirectUrl);
                 if (!redirectUrl.contains("?")) {
                     buf.append("?");
                 } 
                 buf.append(query);
             }
             String finalRedirectUrl = redirectUrl;
             if (buf != null)
                 finalRedirectUrl = buf.toString();
             hres.sendRedirect(finalRedirectUrl);
        } else {
             String html = formFromMap(redirectUrl, pmap, true);
             out.write(html);
        }
    }
    /**
      * Generates a query string from the parameters in the request.
      * @param request http request to pick params from.
      * @return query string
      */  
    public static String queryStringFromRequest(HttpServletRequest request)
    {
        Enumeration en = request.getParameterNames();
        StringBuilder buf = new StringBuilder();
        boolean priorparam = false;
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            String val = request.getParameter(name);
            if (priorparam)
                buf.append("&");
            buf.append(name).append("=").append(val);
            priorparam = true;
        }
        return buf.toString();
    }
    
    /**
      * Generates a query string from the a <code>Map</code>.
      * @param pmap params to be added to the query string.
      * @return query string
      */  
    public static String queryStringFromMap(Map pmap)
    {
        Iterator iter = pmap.keySet().iterator();
        StringBuilder buf = new StringBuilder();
        boolean priorparam = false;
        while (iter.hasNext()) {
            String name = (String) iter.next();
            String val = (String) pmap.get(name);
            if (priorparam)
                buf.append("&");
            buf.append(name).append("=").append(URLEncoder.encode(val));
            priorparam = true;
        }
        return buf.toString();
    }

    /**
      * Generates a html hidden form to acccomplish a auto POST from the browser
      * Form is assigned an id=saeform
      * @param redirectUrl URL to post teh form to.
      * @param pmap parameters to be sent in the POST
      * @param addAutoSubmit adds html and javascript to autosubmit form
      * @return html code
      */  
    public static String formFromMap(String redirectUrl,  Map pmap, 
            boolean addAutoSubmit)
    {
        StringBuilder buf = new StringBuilder();
        if (addAutoSubmit) {
            buf.append("<HTML><HEAD><TITLE>SAE POST</TITLE></HEAD>");
            buf.append("<BODY Onload=\"document.forms[0].submit()\">");
        }
        buf.append("<FORM id=\"saeform\" METHOD=\"POST\" ACTION=\"").append(redirectUrl).append("\">");
        for (Object name : pmap.keySet()) {
            String val = (String) pmap.get(name);
            buf.append("<INPUT TYPE=\"HIDDEN\" NAME=\"").append(name).append("\" VALUE=\"");
            buf.append(ESAPI.encoder().encodeForHTML(val)).append("\">");
        }
        buf.append("</FORM>");
        if (addAutoSubmit) {
            buf.append("</BODY></HTML>");
        }
        return buf.toString();
    } 

}
