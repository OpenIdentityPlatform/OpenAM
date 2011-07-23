
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
 * $Id: WSFedCommon.java,v 1.2 2008/03/07 23:18:06 mrudulahg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;

/**
 * This class contains common helper methods for WSFed tests
 */
public class WSFedCommon extends TestCommon {
    
    /** Creates a new instance of WSFedCommon */
    public WSFedCommon() {
        super("WSFedCommon");
    }
    
    /** Creates a new instance of WSFedCommon */
    public WSFedCommon(String componentName) {
        super(componentName);
    }

    /**
     * This method creates spssoinit xml
     * The flow is as follows
     * 1. Go to WSFederationServlet/metaAlias/<metaalias>
     * 2. It redirects to idp login. Enter idp user id & password.
     * 3. After successful idp login, it is validates access rightes and 
     * then end user profile page is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPInitSSO(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name = (String)m.get(
                TestConstants.KEY_IDP_ENTITY_NAME);
        String sp_user = (String)m.get(TestConstants.KEY_SP_USER);
        String sp_userpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/WSFederationServlet/metaAlias" + sp_alias
                + "?goto=" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + idp_userpw + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\" />");
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * This method creates spsloinit xml
     * The flow is as follows
     * 1. Go to WSFederationServlet/metaAlias/<metaalias>?wa=wsignout1.0
     * 2. It logs out user session from SP & IDP
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPInitSLO(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name = (String)m.get(
                TestConstants.KEY_IDP_ENTITY_NAME);
        String strResult = (String)m.get(TestConstants.KEY_SP_SLO_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/WSFederationServlet/metaAlias" + sp_alias
                + "?wa=wsignout1.0");
        out.write("\">");
        out.write(newline);
        out.write("<form>");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * This method creates idpsloinit xml
     * The flow is as follows
     * 1. Go to WSFederationServlet/metaAlias/<metaalias>?wa=wsignout1.0
     * 2. It logs out user session from SP & IDP
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPInitSLO(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_alias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String strResult = (String)m.get(TestConstants.KEY_IDP_SLO_RESULT);
        
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri
                + "/WSFederationServlet/metaAlias" + idp_alias
                + "?wa=wsignout1.0");
        out.write("\">");
        out.write(newline);
        out.write("<form>");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
}