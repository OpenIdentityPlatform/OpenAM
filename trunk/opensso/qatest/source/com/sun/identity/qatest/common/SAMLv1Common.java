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
 * $Id: SAMLv1Common.java,v 1.3 2009/01/06 01:12:46 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;

/**
 * This class contains helper methods for samlv1 tests
 */
public class SAMLv1Common extends TestCommon {
    
    /** Creates a new instance of SAMLv2Common */
    public SAMLv1Common() {
        super("SAMLv1Common");
    }
       
    /**
     * This method creates sso xml
     * This xml is for sp or idp initiated sso for POST and Artifact profiles
     * The flow is as follows
     * 1. Go to SP or IDP based on whether SP or IDP initiate SSO
     * 2. Specify the Target as the partner 
     *  - If SP initiate TARGET = IDP as partner
     *  - If IDP initiate TARGET = SP as partner
     * 3. The result should be the TARGET Site with one login credentials
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be artifact or post
     * @param testInitiate is either SP or IDP
     * @param attr is the nameidformat attribute 
     */
    public static void getxmlSSO(String xmlFileName, Map m,
            String bindingType, String testInitiate, String attr)
            throws Exception {
        String servURL = null;
        if(bindingType.equalsIgnoreCase("Artifact")){
            servURL =  "/SAMLAwareServlet?TARGET=";
        } else {
            servURL =  "/SAMLPOSTProfileServlet?TARGET=";
        }
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_RESULT);
        if (attr.equals("")) {
            if (testInitiate.equalsIgnoreCase("SP")) {
                out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                        + sp_port + sp_deployment_uri
                        + servURL 
                        + idp_proto + "://" + idp_host + ":"
                        + idp_port + idp_deployment_uri);
            } else {
                out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                        + idp_port + idp_deployment_uri
                        + servURL 
                        + sp_proto + "://" + sp_host + ":"
                        + sp_port + sp_deployment_uri);
            }            
        } else {
            if (testInitiate.equalsIgnoreCase("SP")) {
                out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                        + sp_port + sp_deployment_uri
                        + servURL 
                        + idp_proto + "://" + idp_host + ":"
                        + idp_port + idp_deployment_uri + attr);                                
            } else {
                out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                        + idp_port + idp_deployment_uri
                        + servURL 
                        + sp_proto + "://" + sp_host + ":"
                        + sp_port + sp_deployment_uri + attr);
            }              
        }
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"\">");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_user + "\"/>");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""+ idp_userpw + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
}
