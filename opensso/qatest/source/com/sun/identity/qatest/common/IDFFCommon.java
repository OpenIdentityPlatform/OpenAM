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
 * $Id: IDFFCommon.java,v 1.8 2008/03/07 23:18:06 mrudulahg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class contains helper methods for IDFF tests
 */
public class IDFFCommon extends TestCommon {
    
    /** Creates a new instance of IDFFCommon */
    public IDFFCommon() {
        super("IDFFCommon");
    }
    
    /** Creates a new instance of IDFFCommon */
    public IDFFCommon(String componentName) {
        super(componentName);
    }
    
   /**
     * This method creates xml sp init federation
     * It assumes that sp session is present. 
     * The flow is as follows
     * 1. Go to Federate.jsp on sp side. Click on select provider button. 
     * 2. It redirects to idp login. Enter idp user id & password.
     * 3. After successful idp login, success federation msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPIDFFFederate(String xmlFileName, Map m, boolean 
            idpLogin)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String spProto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String spPort = (String)m.get(TestConstants.KEY_SP_PORT);
        String spHost = (String)m.get(TestConstants.KEY_SP_HOST);
        String spDeploymentURI = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String spMetaalias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idpEntityName = (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME);
        String spUser = (String)m.get(TestConstants.KEY_SP_USER);
        String spUserpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idpUser = (String)m.get(TestConstants.KEY_IDP_USER);
        String idpUserpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult;
        if (idpLogin) {
            strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        } else {
            strResult = (String)m.get(TestConstants.KEY_SSO_RESULT);
        }
        
        out.write("<url href=\"" + spProto +"://" + spHost + ":"
                + spPort + spDeploymentURI
                + "/config/federation/default/Federate.jsp?metaAlias="
                + spMetaalias + "&amp;idpEntityID=" + idpEntityName );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        if (idpLogin) {
            out.write("<form name=\"form1\" buttonName=\"button\" />");
            out.write(System.getProperty("line.separator"));
            out.write("<form name=\"Login\" buttonName=\"\" >");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken1\" value=\"" + idpUser + "\" />");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken2\" value=\""
                    + idpUserpw + "\" />");
        } else {
            out.write("<form>");
        }
        out.write(System.getProperty("line.separator"));
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }

    /**
     * This method overrides getxmlSPIDFFFederate method 
     * Here during federation initiation SP & IDP sessions are already present. 
     * The flow is as follows
     * 1. Go to Federate.jsp on sp side. Click on select provider button. 
     * 2. Success federation msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPIDFFFederate(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String spProto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String spPort = (String)m.get(TestConstants.KEY_SP_PORT);
        String spHost = (String)m.get(TestConstants.KEY_SP_HOST);
        String spDeploymentURI = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String spMetaalias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idpEntityName = (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME);
        String spUser = (String)m.get(TestConstants.KEY_SP_USER);
        String spUserpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idpUser = (String)m.get(TestConstants.KEY_IDP_USER);
        String idpUserpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult;
        strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        
        out.write("<url href=\"" + spProto +"://" + spHost + ":"
                + spPort + spDeploymentURI
                + "/config/federation/default/Federate.jsp?metaAlias="
                + spMetaalias + "&amp;idpEntityID=" + idpEntityName );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form name=\"form1\" buttonName=\"button\" />");
        out.write(System.getProperty("line.separator"));
        out.write("<form>");
        out.write(System.getProperty("line.separator"));
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }
    
    /**
     * This method creates xml sp init sso
     * First login to idp & then run this xml file. 
     * Since user is already federated, SSO will be successful. 
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPIDFFSSO(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String spProto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String spPort = (String)m.get(TestConstants.KEY_SP_PORT);
        String spHost = (String)m.get(TestConstants.KEY_SP_HOST);
        String spDeploymentURI = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String spMetaalias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idpEntityName = (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME);
        String spUser = (String)m.get(TestConstants.KEY_SP_USER);
        String spUserpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idpUser = (String)m.get(TestConstants.KEY_IDP_USER);
        String idpUserpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_RESULT);
        
        out.write("<url href=\"" + spProto +"://" + spHost + ":"
                + spPort + spDeploymentURI
                + "/config/federation/default/Federate.jsp?metaAlias="
                + spMetaalias + "&amp;idpEntityID=" + idpEntityName );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form>");
        out.write(System.getProperty("line.separator"));
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }
    
    /**
     * This method creates xml sp init Logout
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPIDFFLogout(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String spProto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String spPort = (String)m.get(TestConstants.KEY_SP_PORT);
        String spHost = (String)m.get(TestConstants.KEY_SP_HOST);
        String spDeploymentURI = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String spMetaalias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idpEntityName = (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME);
        String spUser = (String)m.get(TestConstants.KEY_SP_USER);
        String spUserpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idpUser = (String)m.get(TestConstants.KEY_IDP_USER);
        String idpUserpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SP_SLO_RESULT);
        
        out.write("<url href=\"" + spProto +"://" + spHost + ":"
                + spPort + spDeploymentURI
                + "/liberty-logout?metaAlias=" + spMetaalias);
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form>");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }
    
    /**
     * This method creates xml sp init federation termination
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPIDFFTerminate(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String spProto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String spPort = (String)m.get(TestConstants.KEY_SP_PORT);
        String spHost = (String)m.get(TestConstants.KEY_SP_HOST);
        String spDeploymentURI = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String spMetaalias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idpEntityName = (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME);
        String spUser = (String)m.get(TestConstants.KEY_SP_USER);
        String spUserpw = (String)m.get(TestConstants.KEY_SP_USER_PASSWORD);
        String idpUser = (String)m.get(TestConstants.KEY_IDP_USER);
        String idpUserpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_TERMINATE_RESULT);
        
        out.write("<url href=\"" + spProto +"://" + spHost + ":"
                + spPort + spDeploymentURI
                + "/config/federation/default/Termination.jsp?metaAlias="
                + spMetaalias );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form name=\"selectprovider\" buttonName=\"doIt\">");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }
    
    /**
     * This method creates xml sp init Logout
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPIDFFLogout(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idpProto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idpPort = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idpHost = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idpDeploymentURI = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idpMetaalias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String strResult = (String)m.get(TestConstants.KEY_IDP_SLO_RESULT);
        
        out.write("<url href=\"" + idpProto +"://" + idpHost + ":"
                + idpPort + idpDeploymentURI
                + "/liberty-logout?metaAlias=" + idpMetaalias);
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form>");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }
    
    /**
     * This method creates xml idp init federation termination
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPIDFFTerminate(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idpProto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idpPort = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idpHost = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idpDeploymentURI = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idpMetaalias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String strResult = (String)m.get(TestConstants.KEY_TERMINATE_RESULT);
        
        out.write("<url href=\"" + idpProto +"://" + idpHost + ":"
                + idpPort + idpDeploymentURI
                + "/config/federation/default/Termination.jsp?metaAlias="
                + idpMetaalias );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form name=\"selectprovider\" buttonName=\"doIt\">");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }
    
    /**
     * This method creates xml sp init Name Registration
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlSPIDFFNameReg(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String spProto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String spPort = (String)m.get(TestConstants.KEY_SP_PORT);
        String spHost = (String)m.get(TestConstants.KEY_SP_HOST);
        String spDeploymentURI = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String spMetaalias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String strResult = (String)m.get(TestConstants.KEY_NAME_REG_RESULT);
        
        out.write("<url href=\"" + spProto +"://" + spHost + ":"
                + spPort + spDeploymentURI
                + "/config/federation/default/NameRegistration.jsp?metaAlias="
                + spMetaalias );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form name=\"selectprovider\" buttonName=\"doIt\">");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }

    /**
     * This method creates xml sp init Name Registration
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDPIDFFNameReg(String xmlFileName, Map m)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String idpProto = (String)m.get(TestConstants.KEY_IDP_PROTOCOL);
        String idpPort = (String)m.get(TestConstants.KEY_IDP_PORT);
        String idpHost = (String)m.get(TestConstants.KEY_IDP_HOST);
        String idpDeploymentURI = (String)m.get(
                TestConstants.KEY_IDP_DEPLOYMENT_URI);
        String idpMetaalias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
        String strResult = (String)m.get(TestConstants.KEY_NAME_REG_RESULT);
        
        out.write("<url href=\"" + idpProto +"://" + idpHost + ":"
                + idpPort + idpDeploymentURI
                + "/config/federation/default/NameRegistration.jsp?metaAlias="
                + idpMetaalias );
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        out.write("<form name=\"selectprovider\" buttonName=\"doIt\">");
        out.write("<result text=\"" + strResult + "\" />");
        out.write(System.getProperty("line.separator"));
        out.write("</form>");
        out.write(System.getProperty("line.separator"));
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.close();
    }

    /**
     * This method changes the order of the SSO profile in ext metadata
     * @param spmetadataext SP metadata
     * @param strProfile Either http or soap
     */
    public String setSPSSOProfile(String spmetadataext, String strProfile)
    throws Exception {
        try {
            if (strProfile.equals("post")) {
                spmetadataext = spmetadataext.replaceAll(
                        TestConstants.SSO_BROWSER_ARTIFACT_VALUE,
                        "TEMP_STRING");
                spmetadataext = spmetadataext.replaceAll(
                        TestConstants.SSO_BROWSER_POST_VALUE,
                        TestConstants.SSO_BROWSER_ARTIFACT_VALUE);
                spmetadataext = spmetadataext.replaceAll("TEMP_STRING",
                        TestConstants.SSO_BROWSER_POST_VALUE);
            } else {
                spmetadataext = spmetadataext.replaceAll(
                        TestConstants.SSO_BROWSER_POST_VALUE, "TEMP_STRING");
                spmetadataext = spmetadataext.replaceAll(
                        TestConstants.SSO_BROWSER_ARTIFACT_VALUE,
                        TestConstants.SSO_BROWSER_POST_VALUE);
                spmetadataext = spmetadataext.replaceAll("TEMP_STRING",
                        TestConstants.SSO_BROWSER_ARTIFACT_VALUE);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setSPSSOProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return spmetadataext;
    }

    /**
     * This method changes the order of the SLO profile in SP metadata
     * @param spmetadata SP metadata
     * @param strProfile Either http or soap
     */
    public String setSPSLOProfile(String spmetadata, String strProfile)
    throws Exception {
        try {
            if (strProfile.equals("soap")) {
                spmetadata = spmetadata.replaceAll(
                        TestConstants.SLO_HTTP_PROFILE_VALUE, "TEMP_STRING");
                spmetadata = spmetadata.replaceAll(
                        TestConstants.SLO_SOAP_PROFILE_VALUE,
                        TestConstants.SLO_HTTP_PROFILE_VALUE);
                spmetadata = spmetadata.replaceAll("TEMP_STRING",
                        TestConstants.SLO_SOAP_PROFILE_VALUE);
            } else {
                spmetadata = spmetadata.replaceAll(
                        TestConstants.SLO_SOAP_PROFILE_VALUE, "TEMP_STRING");
                spmetadata = spmetadata.replaceAll(
                        TestConstants.SLO_HTTP_PROFILE_VALUE,
                        TestConstants.SLO_SOAP_PROFILE_VALUE);
                spmetadata = spmetadata.replaceAll("TEMP_STRING",
                        TestConstants.SLO_HTTP_PROFILE_VALUE);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setSPSLOProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return spmetadata;
    } 

    /**
     * This method changes the order of the Termination profile in SP metadata
     * @param spmetadata SP metadata
     * @param strProfile Either http or soap
     */
    public String setSPTermProfile(String spmetadata, String strProfile)
    throws Exception {
        try {
            if (strProfile.equals("soap")) {
                spmetadata = spmetadata.replaceAll(
                        TestConstants.TERMIATION_HTTP_PROFILE_VALUE,
                        "TEMP_STRING");
                spmetadata = spmetadata.replaceAll(
                        TestConstants.TERMIATION_SOAP_PROFILE_VALUE,
                        TestConstants.TERMIATION_HTTP_PROFILE_VALUE);
                spmetadata = spmetadata.replaceAll("TEMP_STRING",
                        TestConstants.TERMIATION_SOAP_PROFILE_VALUE);
            } else {
                spmetadata = spmetadata.replaceAll(
                        TestConstants.TERMIATION_SOAP_PROFILE_VALUE,
                        "TEMP_STRING");
                spmetadata = spmetadata.replaceAll(
                        TestConstants.TERMIATION_HTTP_PROFILE_VALUE,
                        TestConstants.TERMIATION_SOAP_PROFILE_VALUE);
                spmetadata = spmetadata.replaceAll("TEMP_STRING",
                        TestConstants.TERMIATION_HTTP_PROFILE_VALUE);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setSPTermProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return spmetadata;
    } 

    /**
     * This method changes the order of the Registration profile in SP metadata
     * @param spmetadata SP metadata
     * @param strProfile Either http or soap
     */
    public String setSPRegProfile(String spmetadata, String strProfile)
    throws Exception {
        try {
            if (strProfile.equals("soap")) {
                 spmetadata = spmetadata.replaceAll(
                         TestConstants.REG_HTTP_PROFILE_VALUE, "TEMP_STRING");
                spmetadata = spmetadata.replaceAll(
                        TestConstants.REG_SOAP_PROFILE_VALUE,
                        TestConstants.REG_HTTP_PROFILE_VALUE);
                spmetadata = spmetadata.replaceAll("TEMP_STRING",
                        TestConstants.REG_SOAP_PROFILE_VALUE);
            } else {
                spmetadata = spmetadata.replaceAll(
                        TestConstants.REG_SOAP_PROFILE_VALUE, "TEMP_STRING");
                spmetadata = spmetadata.replaceAll(
                        TestConstants.REG_HTTP_PROFILE_VALUE,
                        TestConstants.REG_SOAP_PROFILE_VALUE);
                spmetadata = spmetadata.replaceAll("TEMP_STRING",
                        TestConstants.REG_HTTP_PROFILE_VALUE);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setSPRegProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return spmetadata;
    } 
    
    /*
     * This method loads the SP metadata on sp & idp 
     * @param metadata is the standard metadata of SP
     * @param metadataext is the extended metadata of SP
     * @param FederationManager object initiated with SP details. 
     * @param FederationManager object initiated with IDP details. 
     * @param MAP containing all the SP & IDP details
     * @param WebClient object after admin login is successful.
     * @param boolean extMetadataOnly is set to true if only ext metadata is 
     * supplied
     */
    public boolean loadSPMetadata(String metadata, String metadataext, 
            FederationManager fmSP, FederationManager fmIDP, Map configMap, 
            WebClient webClient, boolean extMetadataOnly) 
    throws Exception {
        boolean status = true;
        try {
            if (extMetadataOnly) {
                if ((metadataext.equals(null)) || (metadataext.equals(""))) {
                    log(Level.SEVERE, "loadSPMetadata", "Ext metadata " +
                            "cannot be empty");
                    status = false;
                }
            } else if ((metadata.equals(null)) || (metadataext.equals(null)) || 
                    (metadata.equals("")) || (metadataext.equals(""))) {
                log(Level.SEVERE, "loadSPMetadata", "metadata cannot be empty");
                log(Level.FINEST, "loadSPMetadata", "metadata is : " + 
                        metadata);
                log(Level.FINEST, "loadSPMetadata", "ext metadata is : " + 
                        metadataext);
                status = false;
            }

            HtmlPage spDeleteEntityPage;
            if (extMetadataOnly) {
                spDeleteEntityPage = fmSP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), true, 
                        "idff");
            } else {
                spDeleteEntityPage = fmSP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        false, "idff");
            }
            if (FederationManager.getExitCode(spDeleteEntityPage) == 0) {
                log(Level.FINE, "loadSPMetadata", "Deleted SP entity on SP " +
                        "side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Delete SP entity on SP " +
                        "side failed");
                log(Level.SEVERE, "loadSPMetadata", "deleteEntity famadm" +
                        " command failed");
                status = false;
            }
            
            HtmlPage idpDeleteEntityPage;
            if (extMetadataOnly) {
                idpDeleteEntityPage = fmIDP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        true, "idff");
            } else {
                idpDeleteEntityPage = fmIDP.deleteEntity(webClient, 
                        (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                        (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, "idff");
            }
            if (FederationManager.getExitCode(idpDeleteEntityPage) == 0) {
                log(Level.FINE, "loadSPMetadata", "Deleted SP entity on " +
                        "IDP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Delete SP entity on " +
                        "IDP side failed");
                log(Level.SEVERE, "loadSPMetadata", "deleteEntity famadm" +
                        " command failed");
                status = false;
            }

            Thread.sleep(9000);
            HtmlPage importSPMeta = fmSP.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    metadata, metadataext, null, "idff");
            if (FederationManager.getExitCode(importSPMeta) != 0) {
                log(Level.SEVERE, "loadSPMetadata", "Couldn't import SP " +
                        "metadata on SP side");
                log(Level.SEVERE, "loadSPMetadata", "importEntity famadm" +
                        " command failed");
                status = false;
            } else {
                 log(Level.FINE, "loadSPMetadata", "Successfully imported " +
                         "modified SP entity on SP side");
            }

            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_SP_COT),
                    (String)configMap.get(TestConstants.KEY_IDP_COT));
            importSPMeta = fmIDP.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    metadata, metadataext, null, "idff");
            if (FederationManager.getExitCode(importSPMeta) != 0) {
                log(Level.SEVERE, "loadSPMetadata", "Couldn't import SP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "loadSPMetadata", "importEntity famadm" +
                        " command failed");
                status = false;
            } else {
                 log(Level.FINEST, "loadSPMetadata", "Successfully imported " +
                         "modified SP entity on IDP side");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadSPMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return status;
    }

    /*
     * This method loads the IDP metadata on sp & idp 
     * @param metadata is the standard metadata of IDP
     * @param metadataext is the extended metadata of IDP
     * @param FederationManager object initiated with SP details. 
     * @param FederationManager object initiated with IDP details. 
     * @param MAP containing all the SP & IDP details
     * @param WebClient object after admin login is successful.
     * @param boolean extMetadataOnly is set to true if only ext metadata is 
     * supplied
     */
    public boolean loadIDPMetadata(String metadata, String metadataext, 
            FederationManager fmSP, FederationManager fmIDP, Map configMap, 
            WebClient webClient, boolean extMetadataOnly) 
    throws Exception {
        boolean status = true;
        try {
            if (extMetadataOnly) {
                if ((metadataext.equals(null)) || (metadataext.equals(""))) {
                    log(Level.SEVERE, "loadIDPMetadata", "Ext metadata " +
                            "cannot be empty");
                    status = false;
                }
            } else if ((metadata.equals(null)) || (metadataext.equals(null)) || 
                    (metadata.equals("")) || (metadataext.equals(""))) {
                log(Level.SEVERE, "loadIDPMetadata", "metadata cannot be" +
                        " empty");
                log(Level.FINEST, "loadIDPMetadata", "metadata is : " + 
                        metadata);
                log(Level.FINEST, "loadIDPMetadata", "ext metadata is : " + 
                        metadataext);
                status = false;
            }
            //TODO: Add if condition for ext metadata deletion/addition
            HtmlPage idpDeleteEntityPage;
            if (extMetadataOnly) {
                idpDeleteEntityPage = fmIDP.deleteEntity(webClient, 
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME), 
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), true, 
                    "idff");
            } else {
                idpDeleteEntityPage = fmIDP.deleteEntity(webClient, 
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME), 
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false, 
                    "idff");
            }
            if (FederationManager.getExitCode(idpDeleteEntityPage) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Deleted IDP entity on " +
                        "IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldnt delete IDP" +
                        " entity on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "deleteEntity famadm" +
                        " command failed");
                status = false;
            }  
            
            HtmlPage spDeleteEntityPage;
            if (extMetadataOnly) {
                 spDeleteEntityPage = fmSP.deleteEntity(webClient, 
                        (String)configMap.get(
                        TestConstants.KEY_IDP_ENTITY_NAME),
                        (String)configMap.get(
                        TestConstants.KEY_SP_EXECUTION_REALM), 
                        true, "idff");
            } else {
                 spDeleteEntityPage = fmSP.deleteEntity(webClient, 
                        (String)configMap.get(
                        TestConstants.KEY_IDP_ENTITY_NAME),
                        (String)configMap.get(
                         TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, "idff");
            }
            if (FederationManager.getExitCode(spDeleteEntityPage) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Deleted SP entity on " +
                        "IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Delete SP entity on " +
                        "IDP side faield");
                log(Level.SEVERE, "loadIDPMetadata", "deleteEntity famadm" +
                        " command failed");
                status = false;
            }

            Thread.sleep(9000);
            HtmlPage importIDPMeta = fmIDP.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    metadata, metadataext, null, "idff");
            if (FederationManager.getExitCode(importIDPMeta) != 0) {
                log(Level.SEVERE, "loadIDPMetadata", "Couldn't import IDP " +
                        "metadata on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "importEntity famadm" +
                        " command failed");
                status = false;
            } else {
                 log(Level.FINE, "loadIDPMetadata", "Successfully imported " +
                         "modified IDP entity on IDP side");
            }

            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_IDP_COT),
                    (String)configMap.get(TestConstants.KEY_SP_COT));
            importIDPMeta = fmSP.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    metadata, metadataext, null, "idff");
            if (FederationManager.getExitCode(importIDPMeta) != 0) {
                log(Level.SEVERE, "loadIDPMetadata", "Couldn't import IDP " +
                        "metadata on SP side");
                log(Level.SEVERE, "loadIDPMetadata", "importEntity famadm" +
                        " command failed");
                status = false;
            } else {
                 log(Level.FINE, "loadIDPMetadata", "Successfully imported " +
                         "modified IDP entity on SP side");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadIDPMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return status;
    }
}
