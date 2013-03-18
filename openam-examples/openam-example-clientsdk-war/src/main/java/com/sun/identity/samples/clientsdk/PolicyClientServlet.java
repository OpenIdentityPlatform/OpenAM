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
 * $Id: PolicyClientServlet.java,v 1.3 2008/06/25 05:41:09 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk;


import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.text.CharacterIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class PolicyClientServlet extends SampleBase {
    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        // Get query parameters
        String orgname = request.getParameter("orgname");
        if ((orgname == null) || (orgname.length() == 0)) {
            orgname = "/";
        }
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String servicename = request.getParameter("servicename");
        String resource = request.getParameter("resource");
        
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println(SampleConstants.HTML_HEADER);
        
        if ((username == null) || (password == null) || 
            (servicename == null) || (resource == null)
        ) {
            out.println(displayXML("Usage: " + request.getRequestURL() +
                "?username=<username>&password=<password>&orgname=<orgname>"+
                "&servicename=<servicename>&resource=<resource>"));
            out.println("</body></html>");
            return;
        }
        
        try {
            PolicyEvaluatorFactory pef = PolicyEvaluatorFactory.getInstance();
            PolicyEvaluator pe = pef.getPolicyEvaluator(servicename);
            AuthContext lc = authenticate(orgname, username, password, out);
            if (lc != null) {
                SSOToken token = lc.getSSOToken();
                Set actions = new HashSet();
                actions.add("GET");
                actions.add("POST");
                Map env = new HashMap();
                Set attrSet = new HashSet();
                attrSet.add("mail");
                env.put("Get_Response_Attributes", attrSet);
                out.println("<h5>USERID: " + username + "<br>");
                out.println("ORG: " + orgname + "<br>");
                out.println("SERVICE NAME: " + servicename + "<br>");
                out.println("RESOURCE: " + resource + "<br>");
                out.println("</h5><br>");
                out.println("----------getPolicyDecision() Test-----------");
                out.println("<br>");
                PolicyDecision pd = pe.getPolicyDecision(
                    token, resource, actions, env);
                out.println(displayXML(pd.toXML()));
                out.println("End of Test.<br>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
        out.println("</body></html>");
    }
    
    
    //This is a utility function used to hack up an HTML display of an XML
    //string.
    private String displayXML(String input) {
        StringCharacterIterator iter = new StringCharacterIterator(input);
        StringBuffer buf = new StringBuffer();
        
        for(char c = iter.first();c != CharacterIterator.DONE;c = iter.next()) {
            if (c=='>') {
                buf.append("&gt;");
            } else if (c=='<') {
                buf.append("&lt;");
            } else if (c=='\n'){
                buf.append("<BR>\n");
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }
}
