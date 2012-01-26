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
 * $Id: SAMLv2Common.java,v 1.18 2009/08/18 19:09:52 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * This class contains helper methods for samlv2 tests
 */
public class SAMLv2Common extends TestCommon {
    
    public static String fileseparator =
            System.getProperty("file.separator");

    /** Creates a new instance of SAMLv2Common */
    public SAMLv2Common() {
        super("SAMLv2Common");
    }
    
    /**
     * This method creates spssoinit xml
     * It handles two redirects. The flow is as follows
     * 1. Go to spSSOInit.jsp on sp side.
     * 2. It redirects to idp login. Enter idp user id & password.
     * 3. After successful idp login, it is redirected to sp login page.
     * Enter sp user id & password.
     * 4. After successful sp login, "Single sign-on succeeded" msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be artifact or post
     * @param idpLoginOnly can be used in autofedertion case, where only
     * idplogin is req
     * @param idpProxy should be set to true in IDP proxy scenario
     */
    public static void getxmlSPInitSSO(String xmlFileName, Map m,
            String bindingType, boolean idpLoginOnly, boolean idpProxy)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name = "";
        String idp_proxy_user = "";
        String idp_proxy_userpw = "";
        if (idpProxy) {
            idp_entity_name = (String)m.get(
                    TestConstants.KEY_IDP_PROXY_ENTITY_NAME);
            idp_proxy_user = (String)m.get(TestConstants.
                    KEY_IDP_PROXY_USER);
            idp_proxy_userpw = (String)m.get(TestConstants.
                    KEY_IDP_PROXY_USER_PASSWORD);
        } else {
            idp_entity_name = (String)m.get(
                    TestConstants.KEY_IDP_ENTITY_NAME);
        }
        String sp_user = (String)m.get(TestConstants.KEY_SP_USER);
        String sp_userpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/saml2/jsp/spSSOInit.jsp?metaAlias=" + sp_alias
                + "&amp;idpEntityID=" + idp_entity_name );
        if (bindingType == "post") {
            out.write("&amp;binding=HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + idp_userpw + "\" />");
        out.write(newline);
        if ((idpProxy) && (!idpLoginOnly)) {
            out.write("</form>");
            out.write(newline);
            out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
            out.write(newline);
            out.write("<input name=\"IDToken1\" value=\"" + idp_proxy_user + "\" />");
            out.write(newline);
            out.write("<input name=\"IDToken2\" value=\""
                    + idp_proxy_userpw + "\" />");
            out.write(newline);
        }
        if (!idpLoginOnly) {
            out.write("</form>");
            out.write(newline);
            out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
            out.write(newline);
            out.write("<input name=\"IDToken1\" value=\"" + sp_user + "\" />");
            out.write(newline);
            out.write("<input name=\"IDToken2\" value=\""
                    + sp_userpw + "\" />");
            out.write(newline);
        }
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates spssoinit xml
     * It assumes that user has logged in to the idp.
     * 1. Go to idpSSOInit.jsp on idp side.
     * 2. It redirects to sp login.
     * Enter sp user id & password.
     * 4. After successful sp login, "Single sign-on succeeded" msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be artifact or post
     * @param idpLoginOnly can be used in autofedertion case, where only
     * idplogin is req
     */
    public static void getxmlIDPInitSSO(String xmlFileName, Map m,
            String bindingType, boolean idpLoginOnly)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_alias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String sp_entity_name = (String)m.get(TestConstants.KEY_SP_ENTITY_NAME);
        String sp_user = (String)m.get(TestConstants.KEY_SP_USER);
        String sp_userpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri
                + "/saml2/jsp/idpSSOInit.jsp?metaAlias=" + idp_alias
                + "&amp;spEntityID=" + sp_entity_name);
        if (bindingType == "post") {
            out.write("&amp;binding=HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        if (!idpLoginOnly) {
            out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
            out.write(newline);
            out.write("<input name=\"IDToken1\" value=\"" + sp_user + "\" />");
            out.write(newline);
            out.write("<input name=\"IDToken2\" value=\""+ sp_userpw + "\" />");
            out.write(newline);
        } else {
            out.write("<form>");
            out.write(newline);
        }
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    
    /**
     * This method creates spsso xml
     * This xml is for sp initiated sso for already existing federation.
     * The flow is as follows
     * 1. Go to spSSOInit.jsp on sp side.
     * 2. It redirects to idp login. Enter idp user id & password.
     * 3. After successful idp login, "Single sign-on succeeded" msg is
     * displayed
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be artifact or post
     */
    public static void getxmlSPSSO(String xmlFileName, Map m,
            String bindingType)
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
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/saml2/jsp/spSSOInit.jsp?metaAlias=" + sp_alias
                + "&amp;idpEntityID=" + idp_entity_name );
        if (bindingType == "artifact") {
            out.write("\">");
        } else {
            out.write("&amp;binding=HTTP-POST\">");
        }
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\">");
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
    
    /**
     * This method creates spslo xml
     * This xml is for sp initiated slo. The flow is as follows
     * 1. Go to spSingleLogoutInit.jsp on sp side.
     * 2. After successful logout on sp & idp "SP initiated single logout
     * succeeded." msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be http or soap
     * @param idpProxy should be set to true in IDP proxy scenario
     */
    public static void getxmlSPSLO(String xmlFileName, Map m,
            String bindingType, boolean idpProxy)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name;
        if (idpProxy) {
            idp_entity_name = (String)m.get(
                    TestConstants.KEY_IDP_PROXY_ENTITY_NAME);
        } else {
            idp_entity_name = (String)m.get(
                    TestConstants.KEY_IDP_ENTITY_NAME);
        }
        String strResult = (String)m.get(TestConstants.KEY_SP_SLO_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/saml2/jsp/spSingleLogoutInit.jsp?metaAlias="
                + sp_alias + "&amp;idpEntityID=" + idp_entity_name);
        if (bindingType.equals("soap")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
        } else if (bindingType.equals("post")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates spslo xml
     * This xml is for idp initiated slo. The flow is as follows
     * 1. Go to idpSingleLogoutInit.jsp on sp side.
     * 2. After successful logout on sp & idp "IDP initiated single logout
     * succeeded." msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be http (http-redirect) or soap
     */
    public static void getxmlIDPSLO(String xmlFileName, Map m,
            String bindingType)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_alias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String sp_entity_name = (String)m.get(TestConstants.KEY_SP_ENTITY_NAME);
        String strResult = (String)m.get(TestConstants.KEY_IDP_SLO_RESULT);
        
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri
                + "/saml2/jsp/idpSingleLogoutInit.jsp?metaAlias="
                + idp_alias + "&amp;spEntityID=" + sp_entity_name);
        if (bindingType.equals("soap")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
        } else if (bindingType.equals("post")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates spMNIRequestInit xml
     * This xml is for sp initiated termination. This assumes there is no sso
     * done in the current browser session. The flow is as follows
     * 1. Go to spMNIRequestInit.jsp on sp side.
     * 2. It redirects to sp login. Enter sp user id & password.
     * 3. After successful sp login, "ManageNameID Request succeeded." msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be http (http-redirect) or soap
     */
    public static void getxmlSPTerminate(String xmlFileName, Map m,
            String bindingType)
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
        String strResult = (String)m.get(TestConstants.KEY_TERMINATE_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/saml2/jsp/spMNIRequestInit.jsp?metaAlias=" + sp_alias
                + "&amp;idpEntityID=" + idp_entity_name
                + "&amp;requestType=Terminate");
        if (bindingType.equals("soap")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
        } else if (bindingType.equals("post")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\">");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + sp_user + "\"/>");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\"" + sp_userpw + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates spMNIRequestInit xml
     * This xml is for sp initiated termination. This assumes there is no sso
     * done in the current browser session. The flow is as follows
     * 1. Go to spMNIRequestInit.jsp on sp side.
     * 2. It redirects to sp login. Enter sp user id & password.
     * 3. After successful sp login, "ManageNameID Request succeeded." msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be http (http-redirect) or soap
     */
    public static void getxmlIDPTerminate(String xmlFileName, Map m,
            String bindingType)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_alias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String sp_entity_name = (String)m.get(TestConstants.KEY_SP_ENTITY_NAME);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_TERMINATE_RESULT);
        
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri
                + "/saml2/jsp/idpMNIRequestInit.jsp?metaAlias=" + idp_alias
                + "&amp;spEntityID=" + sp_entity_name
                + "&amp;requestType=Terminate");
        if (bindingType.equals("soap")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
        } else if (bindingType.equals("post")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\">");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_user + "\"/>");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\"" + idp_userpw
                + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    
    /**
     * This method creates spMNIRequestInit xml
     * This xml is for sp initiated termination. This assumes there is no sso
     * done in the current browser session. The flow is as follows
     * 1. Go to spMNIRequestInit.jsp on sp side.
     * 2. It redirects to sp login. Enter sp user id & password.
     * 3. After successful sp login, "ManageNameID Request succeeded." msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be http (http-redirect) or soap
     */
    public static void getxmlSPTerminate2(String xmlFileName, Map m,
            String bindingType)
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
        String strResult = (String)m.get(TestConstants.KEY_TERMINATE_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/saml2/jsp/spMNIRequestInit.jsp?metaAlias=" + sp_alias
                + "&amp;idpEntityID=" + idp_entity_name
                + "&amp;requestType=Terminate" );
        if (bindingType == "soap") {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP");
        } else if (bindingType.equals("post")) {
            out.write("&amp;" +
                    "binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.close();
    }
    
    
    /**
     * This method creates spconfigurator xml
     * This xml is for configuring samlv2 sp. The flow is as follows
     *  1. go to samples/saml2/sp/configure.jsp & enter idp details.
     *  2. Configuration is successful.
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPConfigurator(String xmlFileName, Map m)
    throws Exception {
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
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name = (String)m.get(
                TestConstants.KEY_IDP_ENTITY_NAME);
        String strResult = (String)m.get("spconfiguratorresult");
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/samples/saml2/sp/configure.jsp\" >");
        out.write(newline);
        out.write("<form name=\"_none_\" buttonName=\"IDButton\">");
        out.write(newline);
        out.write("<input name=\"proto\" value=\"" + idp_proto + "\"/>");
        out.write(newline);
        out.write("<input name=\"host\" value=\"" + idp_host + "\"/>");
        out.write(newline);
        out.write("<input name=\"port\" value=\"" + idp_port + "\"/>");
        out.write(newline);
        out.write("<input name=\"deploymenturi\" value=\""
                + idp_deployment_uri + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.close();
    }
    
    /**
     * This method creates spconfigurator xml
     * This xml is for configuring samlv2 sp. The flow is as follows
     *  1. go to samples/saml2/sp/configure.jsp & enter idp details.
     *  2. Configuration is successful.
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPConfigurator(String xmlFileName, Map m)
    throws Exception {
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
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name = (String)m.get(
                TestConstants.KEY_IDP_ENTITY_NAME);
        String strResult = (String)m.get("idconfiguratorresult");
        
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri
                + "/samples/saml2/idp/configure.jsp\" >");
        out.write(newline);
        out.write("<form name=\"_none_\" buttonName=\"IDButton\">");
        out.write(newline);
        out.write("<input name=\"proto\" value=\"" + sp_proto + "\"/>");
        out.write(newline);
        out.write("<input name=\"host\" value=\"" + sp_host + "\"/>");
        out.write(newline);
        out.write("<input name=\"port\" value=\"" + sp_port + "\"/>");
        out.write(newline);
        out.write("<input name=\"deploymenturi\" value=\""
                + sp_deployment_uri + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.close();
    }
    
    /**
     * This method creates splogin xml
     * Enter sp user id & password.
     * After successful sp login, "Authentication successful" msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPLogin(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_user = (String)m.get(TestConstants.KEY_SP_USER);
        String sp_userpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_LOGIN_RESULT);
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri + "/UI/Login\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + sp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + sp_userpw + "\" />");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates idplogin xml
     * Enter idp user id & password.
     * After successful sp login, "Authentication successful" msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPLogin(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_LOGIN_RESULT);
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri + "/UI/Login\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + idp_userpw + "\" />");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates spconsolelogin xml
     * Enter sp admin user id & password.
     * After successful sp login, "Authentication successful" msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPConsoleLogin(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_admin = (String)m.get(TestConstants.KEY_SP_AMADMIN_USER);
        String sp_adminpw = (String)m.get(
                TestConstants.KEY_SP_AMADMIN_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_LOGIN_RESULT);
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri +
                "/UI/Login\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + sp_admin + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + sp_adminpw + "\" />");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates idpconsolelogin xml
     * Enter idp admin user id & password.
     * After successful sp login, "Authentication successful" msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPConsoleLogin(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idp_admin = (String)m.get(TestConstants.KEY_IDP_AMADMIN_USER);
        String idp_adminpw = (String)m.get(
                TestConstants.KEY_IDP_AMADMIN_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_LOGIN_RESULT);
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri +
                "/UI/Login\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"IDButton\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_admin + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + idp_adminpw + "\" />");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates spconsolelogout xml
     * Logs the user out
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPLogout(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String strResult = (String)m.get("loginresult");
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri + "/UI/Logout\">");
        out.write(newline);
        out.write("<form>");
        out.write(newline);
        out.write("<result text=\"Access Manager\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method creates idplogout xml
     * Logs the user out.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPLogout(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idp_proto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idp_port = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idp_host = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idp_deployment_uri = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String strResult = (String)m.get("loginresult");
        out.write("<url href=\"" + idp_proto +"://" + idp_host + ":"
                + idp_port + idp_deployment_uri + "\">");
        out.write(newline);
        out.write("<form>");
        out.write(newline);
        out.write("<result text=\"Access Manager\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * This method loads the resource bundle & puts all the values in map
     * @param String rbName Resource bundle name
     * @param Map m will be populated with resource bundle data
     */
    public static void getEntriesFromResourceBundle(String rbName, Map map) {
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            map.put(key, rb.getString(key));
        }
    }
    
    /**
     * This method grep Metadata from the htmlpage & returns as the string.
     * @param HtmlPage page which contains metadata
     */
    public static String getMetadataFromPage(HtmlPage page)
    throws Exception {
        String metadata = "";
        metadata = MultiProtocolCommon.getMetadataFromPage(page);
        return metadata;
    }
    
    /**
     * This method grep ExtendedMetadata from the htmlpage & returns the string
     * @param HtmlPage page which contains extended metadata
     */
    public static String getExtMetadataFromPage(HtmlPage page)
    throws Exception {
        String metadata = "";
        metadata = MultiProtocolCommon.getExtMetadataFromPage(page);
        return metadata;
    }
    
    /**
     * This method loads the IDP metadata on sp & idp
     * @param metadata is the standard metadata of IDP
     * @param metadataext is the extended metadata of IDP
     * @param FederationManager object initiated with SP details.
     * @param FederationManager object initiated with IDP details.
     * @param MAP containing all the SP & IDP details
     * @param WebClient object after admin login is successful.
     */
    public void loadIDPMetadata(String metadata, String metadataext,
            FederationManager fmsp, FederationManager fmidp, Map configMap,
            WebClient webClient)
            throws Exception {
        try{
            
            if ((metadata.equals(null)) || (metadataext.equals(null)) ||
                    (metadata.equals("")) || (metadataext.equals(""))) {
                log(Level.SEVERE, "loadIDPMetadata", "metadata cannot be " +
                        "empty");
                log(Level.FINEST, "loadIDPMetadata", "metadata is : " +
                        metadata);
                log(Level.FINEST, "loadIDPMetadata", "ext metadata is : " +
                        metadataext);
                assert false;
            }
            if (FederationManager.getExitCode(fmidp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false,
                    "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Deleted idp entity on " +
                        "IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldnt delete idp " +
                        "entity on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "deleteEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmidp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    metadata, metadataext, null, "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Successfully " +
                        "imported IDP metadata on IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldn't import IDP" +
                        " metadata on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "importEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            
            //delete & load idp metadata on SP
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_IDP_COT), "");
            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            log(Level.FINER, "loadIDPMetadata", "IDP Ext. Metadata to load " +
                    "on SP" + metadataext);
            if (FederationManager.getExitCode(fmsp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                    "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Deleted idp entity on " +
                        "SP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldnt delete idp " +
                        "entity on SP side");
                log(Level.SEVERE, "loadIDPMetadata", "deleteEntity (SP)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmsp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), metadata,
                    metadataext,
                    (String)configMap.get(TestConstants.KEY_SP_COT),
                    "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Successfully " +
                        "imported SP metadata on IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldn't import SP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "importEntity (SP)" +
                        " famadm command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadIDPMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /*
     * This method loads the SP metadata on sp & idp
     * @param metadata is the standard metadata of SP
     * @param metadataext is the extended metadata of SP
     * @param FederationManager object initiated with SP details.
     * @param FederationManager object initiated with IDP details.
     * @param MAP containing all the SP & IDP details
     * @param WebClient object after admin login is successful.
     */
    public void loadSPMetadata(String metadata, String metadataext,
            FederationManager fmsp, FederationManager fmidp, Map configMap,
            WebClient webClient)
            throws Exception {
        try {
            if ((metadata.equals(null)) || (metadataext.equals(null)) ||
                    (metadata.equals("")) & (metadataext.equals(""))) {
                log(Level.SEVERE, "loadSPMetadata", "metadata cannot be empty");
                log(Level.FINEST, "loadSPMetadata", "metadata is : " +
                        metadata);
                log(Level.FINEST, "loadSPMetadata", "ext metadata is : " +
                        metadataext);
                assert false;
            }
            if (FederationManager.getExitCode(fmsp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                    "saml2")) == 0) {
                log(Level.FINEST, "loadSPMetadata", "Deleted sp entity on " +
                        "SP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldnt delete sp " +
                        "entity on SP side");
                log(Level.SEVERE, "loadSPMetadata", "deleteEntity (SP)" +
                        " famadm command failed");
                assert false;
            }
            
            if (FederationManager.getExitCode(fmsp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    metadata, metadataext, null, "saml2")) == 0) {
                log(Level.FINE, "loadSPMetadata", "Successfully " +
                        "imported SP metadata on SP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldn't import SP " +
                        "metadata on SP side");
                log(Level.SEVERE, "loadSPMetadata", "importEntity (SP)" +
                        " famadm command failed");
                assert false;
            }
            
            //delete & load sp metadata on IDP
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_SP_COT), "");
            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            log(Level.FINER, "loadSPMetadata", "SP Ext. Metadata to load " +
                    "on IDP" + metadataext);
            if (FederationManager.getExitCode(fmidp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    false, "saml2")) == 0) {
                log(Level.FINE, "loadSPMetadata", "Deleted sp entity on " +
                        "IDP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldnt delete sp " +
                        "entity on IDP side");
                log(Level.SEVERE, "loadSPMetadata", "deleteEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmidp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    metadata, metadataext,
                    (String)configMap.get(TestConstants.KEY_IDP_COT),
                    "saml2")) == 0) {
                log(Level.FINE, "loadSPMetadata", "Successfully " +
                        "imported SP metadata on IDP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldn't import SP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "loadSPMetadata", "importEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadSPMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This method creates the hosted SP metadata template & loads it.
     * It returns the uploaded standard & extended metadata.
     * Null is returned in case of failure.
     * @param WebClient object after admin login is successful.
     * @param Map consisting of SP data
     * @param boolean signed metadata should contain signature true or false
     */
    public static String[] configureSP(WebClient webClient, Map m,
            boolean signed) {
        String[] arrMetadata= {"", ""};
        try {
            String spurl = m.get(TestConstants.KEY_SP_PROTOCOL) + "://" +
                    m.get(TestConstants.KEY_SP_HOST) + ":"
                    + m.get(TestConstants.KEY_SP_PORT)
                    + m.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            
            //get sp & idp extended metadata
            FederationManager spfm = new FederationManager(spurl);
            HtmlPage spmetaPage;
            if (signed) {
                spmetaPage = spfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_SP_ENTITY_NAME), true,
                        true, (String)m.get(TestConstants.KEY_SP_METAALIAS),
                        null, null, null, null, null, null, null, null,
                        (String)m.get(TestConstants.KEY_SP_CERTALIAS), null,
                        null, null, null, null, null, null,
                        (String)m.get(TestConstants.KEY_SP_CERTALIAS), null,
                        null, null, null, null, null, null, "saml2");
            } else {
                spmetaPage = spfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_SP_ENTITY_NAME), true,
                        true, (String)m.get(TestConstants.KEY_SP_METAALIAS),
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "saml2");
            }
            if (FederationManager.getExitCode(spmetaPage) != 0) {
                assert false;
            }
            
            arrMetadata[0] = MultiProtocolCommon.getMetadataFromPage(spmetaPage);
            arrMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);

            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null))) {
                assert(false);
            } else {
                if (FederationManager.getExitCode(spfm.importEntity(webClient,
                        (String)m.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        arrMetadata[0], arrMetadata[1],
                        (String)m.get(TestConstants.KEY_SP_COT), "saml2")) != 0) {
                    arrMetadata[0] = null;
                    arrMetadata[1] = null;
                    assert(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrMetadata;
        
    }
    
    /**
     * This method creates the hosted IDP metadata template & loads it.
     * @param WebClient object after admin login is successful.
     * @param Map consisting of IDP data
     * @param boolean signed metadata should contain signature true or false
     */
    public static String[] configureIDP(WebClient webClient, Map m,
            boolean signed) {
        String[] arrMetadata={"",""};
        try {
            String idpurl = m.get(TestConstants.KEY_IDP_PROTOCOL) + "://" +
                    m.get(TestConstants.KEY_IDP_HOST) + ":"
                    + m.get(TestConstants.KEY_IDP_PORT)
                    + m.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            //get sp & idp extended metadata
            FederationManager idpfm = new FederationManager(idpurl);
            HtmlPage idpmetaPage;
            if (signed) {
                idpmetaPage = idpfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME), true,
                        true, null,
                        (String)m.get(TestConstants.KEY_IDP_METAALIAS), null,
                        null, null, null, null, null, null, null,
                        (String)m.get(TestConstants.KEY_IDP_CERTALIAS), null,
                        null, null, null, null, null, null,
                        (String)m.get(TestConstants.KEY_IDP_CERTALIAS), null,
                        null, null, null, null, null, "saml2");
            } else {
                idpmetaPage = idpfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME), true,
                        true, null,
                        (String)m.get(TestConstants.KEY_IDP_METAALIAS), null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, "saml2");
            }
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
                assert false;
            }
            arrMetadata[0] = MultiProtocolCommon.getMetadataFromPage(
                    idpmetaPage);
            arrMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(
                    idpmetaPage);
         
            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null))) {
                assert(false);
            } else {
                if (FederationManager.getExitCode(idpfm.importEntity(webClient,
                        (String)m.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        arrMetadata[0], arrMetadata[1],
                        (String)m.get(TestConstants.KEY_IDP_COT), "saml2"))
                        != 0) {
                    arrMetadata[0] = null;
                    arrMetadata[1] = null;
                    assert(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrMetadata;
    }
    
    
    /**
     * This method returns the termination URL based on the termination initiated
     * either by SP or IDP and binding for termination can be SOAP or HTTP
     * @param initiator can SP or IDP
     * @param binding can be SOAP or HTTP
     * @param Map consisting the configuration data
     * @return termination URL
     */
    public static String getTerminateURL(String initiate, String binding,
            Map m){
        String terminateURL;
        String spurl = m.get(TestConstants.KEY_SP_PROTOCOL) + "://" +
                m.get(TestConstants.KEY_SP_HOST) + ":"
                + m.get(TestConstants.KEY_SP_PORT)
                + m.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
        String idpurl = m.get(TestConstants.KEY_IDP_PROTOCOL) +
                "://" + m.get(TestConstants.KEY_IDP_HOST) + ":" +
                m.get(TestConstants.KEY_IDP_PORT) +
                m.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        
        if (initiate.equalsIgnoreCase("SP")) {
            if (binding.equalsIgnoreCase("HTTP")) {
                terminateURL = spurl + "/saml2/jsp/spMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_SP_METAALIAS) +
                        "&idpEntityID=" + m.get(TestConstants.KEY_IDP_ENTITY_NAME) +
                        "&requestType=Terminate&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:HTTP-Redirect";
            } else {
                terminateURL = spurl + "/saml2/jsp/spMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_SP_METAALIAS) +
                        "&idpEntityID=" + m.get(TestConstants.KEY_IDP_ENTITY_NAME) +
                        "&requestType=Terminate&binding=urn:oasis:names:tc:" +
                        "SAML:2.0:bindings:SOAP";
            }
        } else {
            if (binding.equalsIgnoreCase("HTTP")) {
                terminateURL = idpurl + "/saml2/jsp/idpMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_IDP_METAALIAS) +
                        "&spEntityID=" + m.get(TestConstants.KEY_SP_ENTITY_NAME) +
                        "&requestType=Terminate&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:HTTP-Redirect";
            } else {
                terminateURL = idpurl + "/saml2/jsp/idpMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_IDP_METAALIAS) +
                        "&spEntityID=" + m.get(TestConstants.KEY_SP_ENTITY_NAME) +
                        "&requestType=Terminate&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:SOAP";
            }
        }
        return terminateURL;
    }
    
    /**
     * This method returns newID Request URL based on the request initiated
     * either by SP or IDP and binding for termination can be SOAP or HTTP
     * @param initiator can SP or IDP
     * @param binding can be SOAP or HTTP
     * @param Map consisting the configuration data
     * @return newID request URL
     */
    public static String getNewIDRequestURL(String idInitiator,String binding,
            Map m) {
        String newIDReqURL;
        String spurl = m.get(TestConstants.KEY_SP_PROTOCOL) + "://" +
                m.get(TestConstants.KEY_SP_HOST) + ":"
                + m.get(TestConstants.KEY_SP_PORT)
                + m.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
        String idpurl = m.get(TestConstants.KEY_IDP_PROTOCOL) +
                "://" + m.get(TestConstants.KEY_IDP_HOST) + ":" +
                m.get(TestConstants.KEY_IDP_PORT) +
                m.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        
        if (idInitiator.equalsIgnoreCase("SP")) {
            if (binding.equalsIgnoreCase("HTTP")) {
                newIDReqURL = spurl + "/saml2/jsp/spMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_SP_METAALIAS) +
                        "&idpEntityID=" + m.get(TestConstants.KEY_IDP_ENTITY_NAME) +
                        "&requestType=NewID&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:HTTP-Redirect";
            } else {
                newIDReqURL = spurl + "/saml2/jsp/spMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_SP_METAALIAS) +
                        "&idpEntityID=" + m.get(TestConstants.KEY_IDP_ENTITY_NAME) +
                        "&requestType=NewID&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:SOAP";
            }
        } else {
            if (binding.equalsIgnoreCase("HTTP")) {
                newIDReqURL = idpurl + "/saml2/jsp/idpMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_IDP_METAALIAS) +
                        "&spEntityID=" + m.get(TestConstants.KEY_SP_ENTITY_NAME) +
                        "&requestType=NewID&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:HTTP-Redirect";
            } else {
                newIDReqURL = idpurl + "/saml2/jsp/idpMNIRequestInit.jsp?metaAlias=" +
                        m.get(TestConstants.KEY_IDP_METAALIAS) +
                        "&spEntityID=" + m.get(TestConstants.KEY_SP_ENTITY_NAME) +
                        "&requestType=NewID&binding=urn:oasis:names:tc:SAML:2.0:" +
                        "bindings:SOAP";
            }
        }
        return newIDReqURL;
    }
}
