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
 * $Id: MultiProtocolCommon.java,v 1.17 2009/08/19 22:56:54 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.owasp.esapi.codecs.HTMLEntityCodec;


/**
 * This class contains common helper methods for samlv2, IDFF tests
 */
public class MultiProtocolCommon extends TestCommon {
    
    private static HTMLEntityCodec decoder = new HTMLEntityCodec();;

    /** Creates a new instance of MultiProtocolCommon */
    public MultiProtocolCommon() {
        super("MultiProtocolCommon");
    }
    
    /**
     * This method creates the hosted SP metadata template & loads it.
     * It returns the uploaded standard & extended metadata.
     * Null is returned in case of failure.
     * @param WebClient object after admin login is successful.
     * @param Map consisting of SP data
     * @param boolean signed metadata should contain signature true or false
     */
    public static String[] configureSP(WebClient webClient, Map m, String spec,
            boolean signed) {
        String[] arrMetadata= {"", ""};
        try {
            String spurl = m.get(TestConstants.KEY_SP_PROTOCOL) + "://" +
                    m.get(TestConstants.KEY_SP_HOST) + ":" +
                    m.get(TestConstants.KEY_SP_PORT)
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
                        null, null, null, null, null, null, spec);
            } else {
                spmetaPage = spfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_SP_ENTITY_NAME), true,
                        true, (String)m.get(TestConstants.KEY_SP_METAALIAS),
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, spec);
            }
            if (FederationManager.getExitCode(spmetaPage) != 0) {
                assert false;
            }
            
            arrMetadata[0] = getMetadataFromPage(spmetaPage, spec);
            arrMetadata[1] = getExtMetadataFromPage(spmetaPage, spec);
            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null))) {
                assert(false);
            } else {
                if (FederationManager.getExitCode(spfm.importEntity(webClient,
                        (String)m.get(TestConstants.KEY_SP_REALM),
                        arrMetadata[0], arrMetadata[1],
                        (String)m.get(TestConstants.KEY_SP_COT), spec)) != 0) {
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
     * @param String spec describing "samlv2", "idff"
     * @param boolean signed metadata should contain signature true or false
     */
    public static String[] configureIDP(WebClient webClient, Map m, String spec,
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
                        null, null, null, null, null, spec);
            } else {
                idpmetaPage = idpfm.createMetadataTempl(webClient,
                        (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME), true,
                        true, null,
                        (String)m.get(TestConstants.KEY_IDP_METAALIAS), null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, spec);
            }
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
                assert false;
            }
            
            arrMetadata[0] = getMetadataFromPage(idpmetaPage, spec);
            arrMetadata[1] = getExtMetadataFromPage(idpmetaPage, spec);
            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null))) {
                assert(false);
            } else {
                if (FederationManager.getExitCode(idpfm.importEntity(webClient,
                        (String)m.get(TestConstants.KEY_IDP_REALM),
                        arrMetadata[0], arrMetadata[1],
                        (String)m.get(TestConstants.KEY_IDP_COT), spec)) != 0) {
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
     * This method fills map with SP configuration data which is needed by
     * TestCommon.configureProduct method.
     */
    public static Map getSPConfigurationMap(Map confMap)
    throws Exception {
        Map spMap = new HashMap<String, String>();
        try {
            spMap.put("serverurl", confMap.get(TestConstants.KEY_SP_PROTOCOL)
            + ":" + "//" + confMap.get(TestConstants.KEY_SP_HOST) + ":"
                    + confMap.get(TestConstants.KEY_SP_PORT));
            spMap.put("serveruri",
                    confMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI));
            spMap.put(TestConstants.KEY_ATT_COOKIE_DOMAIN,
                    confMap.get(TestConstants.KEY_SP_COOKIE_DOMAIN));
            spMap.put(TestConstants.KEY_ATT_CONFIG_DIR,
                    confMap.get(TestConstants.KEY_SP_CONFIG_DIR));
            spMap.put(TestConstants.KEY_ATT_AMADMIN_PASSWORD,
                    confMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            spMap.put(TestConstants.KEY_ATT_CONFIG_DATASTORE,
                    confMap.get(TestConstants.KEY_SP_DATASTORE));
            spMap.put(TestConstants.KEY_ATT_AM_ENC_PWD,
                    confMap.get(TestConstants.KEY_SP_ENC_KEY));
            spMap.put(TestConstants.KEY_ATT_DIRECTORY_SERVER,
                    confMap.get(TestConstants.KEY_SP_DIRECTORY_SERVER));
            spMap.put(TestConstants.KEY_ATT_DIRECTORY_PORT,
                    confMap.get(TestConstants.KEY_SP_DIRECTORY_PORT));
            spMap.put(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX,
                    confMap.get(TestConstants.KEY_SP_CONFIG_ROOT_SUFFIX));
            spMap.put(TestConstants.KEY_ATT_DS_DIRMGRDN,
                    confMap.get(TestConstants.KEY_SP_DS_DIRMGRDN));
            spMap.put(TestConstants.KEY_ATT_DS_DIRMGRPASSWD,
                    confMap.get(TestConstants.KEY_SP_DS_DIRMGRPASSWORD));
            spMap.put(TestConstants.KEY_ATT_LOAD_UMS,
                    confMap.get(TestConstants.KEY_SP_LOAD_UMS));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            return spMap;
        }
    }
    
    /**
     * This method fills map with IDP configuration data which is needed by
     * TestCommon.configureProduct method.
     */
    public static Map getIDPConfigurationMap(Map confMap)
    throws Exception {
        Map idpMap = new HashMap<String, String>();
        try {
            idpMap.put("serverurl", confMap.get(TestConstants.KEY_IDP_PROTOCOL)
            + ":" + "//" + confMap.get(TestConstants.KEY_IDP_HOST) + ":"
                    + confMap.get(TestConstants.KEY_IDP_PORT));
            idpMap.put("serveruri",
                    confMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI));
            idpMap.put(TestConstants.KEY_ATT_COOKIE_DOMAIN,
                    confMap.get(TestConstants.KEY_IDP_COOKIE_DOMAIN));
            idpMap.put(TestConstants.KEY_ATT_CONFIG_DIR,
                    confMap.get(TestConstants.KEY_IDP_CONFIG_DIR));
            idpMap.put(TestConstants.KEY_ATT_AMADMIN_PASSWORD,
                    confMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            idpMap.put(TestConstants.KEY_ATT_CONFIG_DATASTORE,
                    confMap.get(TestConstants.KEY_IDP_DATASTORE));
            idpMap.put(TestConstants.KEY_ATT_AM_ENC_PWD,
                    confMap.get(TestConstants.KEY_IDP_ENC_KEY));
            idpMap.put(TestConstants.KEY_ATT_DIRECTORY_SERVER,
                    confMap.get(TestConstants.KEY_IDP_DIRECTORY_SERVER));
            idpMap.put(TestConstants.KEY_ATT_DIRECTORY_PORT,
                    confMap.get(TestConstants.KEY_IDP_DIRECTORY_PORT));
            idpMap.put(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX,
                    confMap.get(TestConstants.KEY_IDP_CONFIG_ROOT_SUFFIX));
            idpMap.put(TestConstants.KEY_ATT_DS_DIRMGRDN,
                    confMap.get(TestConstants.KEY_IDP_DS_DIRMGRDN));
            idpMap.put(TestConstants.KEY_ATT_DS_DIRMGRPASSWD,
                    confMap.get(TestConstants.KEY_IDP_DS_DIRMGRPASSWORD));
            idpMap.put(TestConstants.KEY_ATT_LOAD_UMS,
                    confMap.get(TestConstants.KEY_SP_LOAD_UMS));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            return idpMap;
        }
    }
    
    /**
     * This method grep Metadata from the htmlpage & returns as the string.
     * @param HtmlPage page which contains metadata
     */
    public static String getMetadataFromPage(HtmlPage page)
    throws Exception {
        String metadata = "";
        String metaPage = page.getWebResponse().getContentAsString();
        metaPage = decoder.decode(metaPage);
        if (!(metaPage.indexOf("EntityDescriptor") == -1)) {
            metadata = metaPage.substring(metaPage.
                    indexOf("EntityDescriptor") - 1,
                    metaPage.lastIndexOf("EntityDescriptor") + 17);
        }
        log(Level.FINEST, "getMetadataFromPage", "Encoded metadata = " +
                metadata);
        log(Level.FINEST, "getMetadataFromPage", "Decoded metadata = " +
                metadata);
        return metadata;
    }
    
    /**
     * This method grep ExtendedMetadata from the htmlpage & returns the string
     * @param HtmlPage page which contains extended metadata
     */
    public static String getExtMetadataFromPage(HtmlPage page)
    throws Exception {
        String metadata = "";
        String metaPage = page.getWebResponse().getContentAsString();
        metaPage = decoder.decode(metaPage);
        log(Level.FINEST, "getMetadataFromPage", "metaPage  = " +
                "metaPage");
        if (!(metaPage.indexOf("EntityConfig") == -1)) {
            metadata = metaPage.substring(metaPage.
                    indexOf("EntityConfig") - 1,
                    metaPage.lastIndexOf("EntityConfig") + 13);
        }
        log(Level.FINEST, "getMetadataFromPage", "Encoded metadata = " +
                metadata);
        log(Level.FINEST, "getMetadataFromPage", "Decoded metadata = " +
                metadata);
        return metadata;
    }
    
    /**
     * This method grep Metadata from the htmlpage & returns as the string.
     * @param HtmlPage page which contains metadata
     */
    public static String getMetadataFromPage(HtmlPage page, String spec)
    throws Exception {
        String metadata = "";
        if (spec.equals("wsfed")) {
            String metaPage = page.getWebResponse().getContentAsString();
            log(Level.FINEST, "getMetadataFromPage", "Encoded metaPage  for " +
                    "wsfed = " + metaPage);
            metaPage = decoder.decode(metaPage);
            log(Level.FINEST, "getMetadataFromPage", "Decoded metaPage  for " +
                    "wsfed = " +  metaPage);
            if (!(metaPage.indexOf("Federation ") == -1)) {
                metadata = metaPage.substring(metaPage.
                        indexOf("Federation ") - 1,
                        metaPage.indexOf("/Federation", metaPage.
                        indexOf("Federation ")) + 12);
            }
        } else if ((spec.equals("saml2")) || (spec.equals("idff"))) {
            metadata = getMetadataFromPage(page);
            log(Level.FINEST, "getMetadataFromPage", "Encoded metadata = " +
                    metadata);
        }
        log(Level.FINEST, "getMetadataFromPage", "Decoded metadata = " +
                metadata);

        return metadata;
    }
    
    /**
     * This method grep ExtendedMetadata from the htmlpage & returns the string
     * @param HtmlPage page which contains extended metadata
     */
    public static String getExtMetadataFromPage(HtmlPage page, String spec)
    throws Exception {
        String metadata = "";
        if (spec.equals("wsfed")) {
            String metaPage = page.getWebResponse().getContentAsString();
            log(Level.FINEST, "getExtMetadataFromPage", "Encoded metaPage for " +
                    "wsfed = " + metaPage);
            metaPage = decoder.decode(metaPage);
            log(Level.FINEST, "getExtMetadataFromPage", "Decoded metaPage for " +
                    "wsfed = " + metaPage);
            if (!(metaPage.indexOf("FederationConfig") == -1)) {
                metadata = metaPage.substring(metaPage.
                        indexOf("FederationConfig") - 1,
                        metaPage.lastIndexOf("FederationConfig") + 17);
            }
        } else if ((spec.equals("saml2")) || (spec.equals("idff"))) {
            metadata = getExtMetadataFromPage(page);
            log(Level.FINEST, "getExtMetadataFromPage", "Encoded metadata = " +
                    metadata);
        }
        log(Level.FINEST, "getExtMetadataFromPage", "Decoded metadata = " +
                metadata);

        return metadata;
    }
    
    /**
     * This method creates xml for Wsfed SP init SSO
     * The flow is as follows
     * If the SSO has been already established for the user with different
     * provider then SSO with WSFed will succeed without prompting to enter
     * user details
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlWSFedSPInitSSO(String xmlFileName, Map m)
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
        String strResult = (String)m.get(TestConstants.KEY_SSO_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/WSFederationServlet/metaAlias" + sp_alias
                + "?goto=" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri);
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
     * This method creates xml for IDFF sp init sso
     * The flow is as follows
     * If the SSO has been already established for the user with different
     * provider then SSO with IDFF will succeed without prompting to enter
     * user details
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     */
    public static void getxmlIDFFSPInitSSO(String xmlFileName, Map m)
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
     * This method creates xml for SAMLv2 sp init sso
     * The flow is as follows
     * If the SSO has been already established for the user with different
     * provider then SSO with SAMLv2 will succeed without prompting to enter
     * user details
     * 4. After successful sp login, "Single sign-on succeeded" msg is displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param bindingType can be artifact or post
     */
    public static void getxmlSAMLv2SPInitSSO(String xmlFileName, Map m,
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
        out.write("<form >");
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
     * This method creates the hosted SP/IDP metadata template & loads it.
     * It returns the uploaded standard & extended metadata.
     * Null is returned in case of failure.
     * @param WebClient object after admin login is successful.
     * @param Map consisting of SP/IDP data
     * @param boolean signed metadata should contain signature true or false
     */
    public static String[] importMetadata(WebClient webClient, Map m,
            boolean signed, String role) {
        String[] arrMetadata= {"", ""};
        try {
            String deployurl = "";
            String entityName = "";
            String idpmetaAlias = "";
            String spmetaAlias = "";
            String spcertAlias = "";
            String spattrqprovider = "";
            String spscertalias = "";
            String spattqsceralias = "";
            String specertalias = "";
            String spattrqecertalias = "";
            String idpattrauthority = "";
            String idpauthnauthority = "";
            String idpscertalias = "";
            String idpttrascertalias = "";
            String idpauthnascertalias = "";
            String idpecertalias = "";
            String idpattraecertalias = "";
            String idpauthnaecertalias = "";
            String idpcertAlias = "";
            String executionRealm = "";
            String cot = "";
            
            if (role.equalsIgnoreCase("SP")) {
                deployurl = m.get(TestConstants.KEY_SP_PROTOCOL) + "://"
                        + m.get(TestConstants.KEY_SP_HOST) + ":"
                        + m.get(TestConstants.KEY_SP_PORT)
                        + m.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
                entityName = (String)m.get(TestConstants.KEY_SP_ENTITY_NAME);
                spmetaAlias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
                spcertAlias = (String)m.get(TestConstants.KEY_SP_CERTALIAS);
                executionRealm = (String)m.get(TestConstants.KEY_SP_EXECUTION_REALM);
                spattrqprovider = (String)m.get(TestConstants.KEY_SP_ATTRQPROVIDER);
                spscertalias = (String)m.get(TestConstants.KEY_SP_SCERTALIAS);
                spattqsceralias = (String)m.get(TestConstants.KEY_SP_ATTRQ_SCERTALIAS);
                specertalias = (String)m.get(TestConstants.KEY_SP_ECERTALIAS);
                spattrqecertalias = (String)m.get(TestConstants.KEY_SP_ATTRQECERTALIAS);
                cot = (String)m.get(TestConstants.KEY_SP_COT);
                if (spattrqprovider.equals(null) ||
                        spattrqprovider.equals("")) {
                    spattrqprovider = "/attrq";
                }
                if (spscertalias.equals(null) ||
                        spscertalias.equals("")) {
                    spscertalias = spcertAlias;
                }
                if (spattqsceralias.equals(null) ||
                        spattqsceralias.equals("")) {
                    spattqsceralias = spcertAlias;
                }
                if (specertalias.equals(null) ||
                        specertalias.equals("")) {
                    specertalias = spcertAlias;
                }
                if (spattrqecertalias.equals(null) ||
                        spattrqecertalias.equals("")) {
                    spattrqecertalias = spcertAlias;
                }
            } else if (role.equalsIgnoreCase("IDP")) {
                deployurl = m.get(TestConstants.KEY_IDP_PROTOCOL) + "://"
                        + m.get(TestConstants.KEY_IDP_HOST) + ":"
                        + m.get(TestConstants.KEY_IDP_PORT)
                        + m.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
                entityName = (String)m.get(TestConstants.KEY_IDP_ENTITY_NAME);
                idpmetaAlias = (String)m.get(TestConstants.KEY_IDP_METAALIAS);
                idpcertAlias = (String)m.get(TestConstants.KEY_IDP_CERTALIAS);
                executionRealm = (String)m.get(TestConstants.KEY_IDP_EXECUTION_REALM);
                idpattrauthority = (String)m.get(TestConstants.KEY_IDP_ATTRAUTHOIRTY);
                idpauthnauthority = (String)m.get(TestConstants.KEY_IDP_AUTHNAUTHORITY);
                idpscertalias = (String)m.get(TestConstants.KEY_IDP_IDPSCERTALIAS);
                idpttrascertalias = (String)m.get(TestConstants.KEY_IDP_ATTRASCERTALIAS);
                idpauthnascertalias = (String)m.get(TestConstants.KEY_IDP_AUTHNASCERTALIAS);
                idpecertalias = (String)m.get(TestConstants.KEY_IDP_IDPECERTALIAS);
                idpattraecertalias = (String)m.get(TestConstants.KEY_IDP_ATTRAECERTALIAS);
                idpauthnaecertalias = (String)m.get(TestConstants.KEY_IDP_AUTHNAECERTALIAS);
                cot = (String)m.get(TestConstants.KEY_IDP_COT);
                if (idpattrauthority == null ||
                        idpattrauthority.equals("")) {
                    idpattrauthority = "/attra";
                }
                if (idpauthnauthority == null ||
                        idpauthnauthority.equals(""))  {
                    idpauthnauthority = "/authna";
                }
                if (idpscertalias == null ||
                        idpscertalias.equals("")) {
                    idpscertalias = idpcertAlias;
                }
                if (idpttrascertalias == null ||
                        idpttrascertalias.equals("")) {
                    idpttrascertalias = idpcertAlias;
                }
                if (idpauthnascertalias == null ||
                        idpauthnascertalias.equals("")) {
                    idpauthnascertalias = idpcertAlias;
                }
                if (idpecertalias == null ||
                        idpecertalias.equals("")) {
                    idpecertalias = idpcertAlias;
                }
                if (idpattraecertalias == null ||
                        idpattraecertalias.equals("")) {
                    idpattraecertalias = idpcertAlias;
                }
                if (idpauthnaecertalias == null ||
                        idpauthnaecertalias.equals("")) {
                    idpauthnaecertalias = idpcertAlias;
                }
            } else if (role.equalsIgnoreCase("IDPPROXY")) {
                deployurl = m.get(TestConstants.KEY_IDP_PROXY_PROTOCOL) + "://"
                        + m.get(TestConstants.KEY_IDP_PROXY_HOST) + ":"
                        + m.get(TestConstants.KEY_IDP_PROXY_PORT)
                        + m.get(TestConstants.KEY_IDP_PROXY_DEPLOYMENT_URI);
                entityName = (String)m.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME);
                spmetaAlias = (String)m.get(TestConstants.KEY_IDP_PROXY_SP_METAALIAS);
                idpmetaAlias = (String)m.get(TestConstants.KEY_IDP_PROXY_IDP_METAALIAS);
                spcertAlias = (String)m.get(TestConstants.KEY_IDP_PROXY_CERTALIAS);
                idpcertAlias = (String)m.get(TestConstants.KEY_IDP_PROXY_CERTALIAS);
                executionRealm = (String)m.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM);
                cot = (String)m.get(TestConstants.KEY_IDP_PROXY_COT);
            }
            //get sp & idp extended metadata
            FederationManager fm = new FederationManager(deployurl);
            HtmlPage metaPage;
            if (signed) {
                metaPage = fm.createMetadataTempl(webClient, entityName, true,
                        true, spmetaAlias, idpmetaAlias,
                        spattrqprovider, idpattrauthority, idpauthnauthority,
                        null, null, null, null, spcertAlias, idpcertAlias,
                        spattqsceralias, idpscertalias, idpttrascertalias,
                        null, null, null, spcertAlias, idpcertAlias,
                        spattrqecertalias, idpattraecertalias,
                        idpauthnaecertalias, null, null, null, "saml2");
            } else {
                metaPage = fm.createMetadataTempl(webClient, entityName, true,
                        true, spmetaAlias, idpmetaAlias,
                        spattrqprovider, idpattrauthority, idpauthnauthority, 
                        null,  null, null, null, null,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "saml2");
            }
            if (FederationManager.getExitCode(metaPage) != 0) {
                assert false;
            }
            
            String page = metaPage.getWebResponse().getContentAsString();
            arrMetadata[0] = getMetadataFromPage(metaPage);
            arrMetadata[1] = getExtMetadataFromPage(metaPage);
            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null))) {
                assert(false);
            } else {
                if (FederationManager.getExitCode(fm.importEntity(webClient,
                        executionRealm, arrMetadata[0], arrMetadata[1],
                        cot, "saml2")) != 0) {
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
     * This method checks if COT contains any entities in it. 
     * @param FederationManager 
     * @param WebClient with authenticated session
     * @param COTName COT name string
     * @param realm execution realm
     */
    public static boolean COTcontainsEntities(FederationManager fm, WebClient 
            webClient, String COTName, String realm)
    throws Exception {
        boolean result = true;
        HtmlPage samlv2page = fm.listCotMembers(webClient, COTName, realm, 
                "saml2");
        HtmlPage idffpage = fm.listCotMembers(webClient, COTName, realm, 
                "idff");
        HtmlPage wsfedpage = fm.listCotMembers(webClient, COTName, realm, 
                "wsfed");
        if ((FederationManager.getExitCode(samlv2page) != 0) || 
                (FederationManager.getExitCode(idffpage) != 0) ||
                (FederationManager.getExitCode(wsfedpage) != 0)) {
            log(Level.SEVERE, "listCOTmembers", "Couldn't get COT " +
                    "members");
        } else {
            if ((samlv2page.getWebResponse().getContentAsString().contains(
                    TestConstants.KEY_LIST_COT_NO_ENTITIES)) & 
            (idffpage.getWebResponse().getContentAsString().contains(
                    TestConstants.KEY_LIST_COT_NO_ENTITIES)) & 
            (wsfedpage.getWebResponse().getContentAsString().contains(
                    TestConstants.KEY_LIST_COT_NO_ENTITIES))) {
                log(Level.FINEST, "listCOTmembers", "COT doesn't contain any " +
                        "entities");
                result = false;
            }
        }
        return result;
    }
}
