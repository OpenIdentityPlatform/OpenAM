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
 * $Id: FedletCommon.java,v 1.3 2009/08/20 17:09:04 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * This class contains helper methods for samlv2 fedlet tests
 */
public class FedletCommon extends TestCommon {

    public static WebClient webClient;
    
    /** Creates a new instance of FedletCommon */
    public FedletCommon() {
        super("FedletCommon");
    } 
    
    /**
     * This method creates the xml file    
     * 1. It goes to the idp side whether it is fedlet initated or 
     * idp initiated. 
     * 2. Enter the idpuser and password
     * 3. After successful idp login, "Single Sign-On successful" msg is
     * displayed.
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param urlStr is the URL link of the fedlet initated or idp initated 
     * HTTP-POST or HTTP-Artifact     
     */
    public static void getxmlFedletSSO(String xmlFileName, Map m, String urlStr)
            throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);           
        String fedletidp_user = (String)m.get(TestConstants.KEY_FEDLETIDP_USER);
        String fedletidp_userpw = (String)m.get(
                TestConstants.KEY_FEDLETIDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        out.write("<url href=\"" + urlStr);
   
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" +
                fedletidp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + fedletidp_userpw + "\" />");
        out.write(newline);        
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * This method creates the xml file
     * 1. It logsout the user session from IDP
     * @param xmlFileName is the file to be created.
     * @param Map m contains all the data for xml generation
     * @param urlStr is the URL link of the fedlet(SP) initated or idp initated
     * Single Logout using SOAP, HTTP-Redirect or HTTP-POST
     */
    public static void getxmlFedletSLO(String xmlFileName, Map m,
            String logouturlStr, String strResult) throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("<url href=\"" + logouturlStr);
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
     * Creates the webClient which will be used for rest of the tests.
     */
    public static void getWebClient()
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * @param str is the string passed whether it is Fedlet(SP) or IDP
     * initated HTTP-POST or HTTP-Artifact profile
     */
    public static String getAnchors(HtmlPage page, String string)
            throws Exception {
            try {
            getWebClient();
            String urlStr = "";

            // Get Anchors
            log(Level.FINEST, "getAnchors", "Page: " +
                    page.getWebResponse().getContentAsString());

            HtmlAnchor anchor = page.getFirstAnchorByText(string);

            int index = anchor.toString().indexOf("\"");
            if (index != -1) {
                String str = anchor.toString().substring(
                        index + 1, anchor.toString().length()).trim();
                int inx = str.indexOf("\"");
                if (inx != -1) {
                    urlStr = str.substring(0, inx);
                }
            }

            log(Level.FINEST, "getAnchors", "String: " + urlStr);
            return urlStr;
        } catch (Exception e) {
            log(Level.FINEST, "getAnchors", e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
            
            if (role.equalsIgnoreCase("IDP")) {
                deployurl = m.get(TestConstants.KEY_AMC_PROTOCOL) + "://"
                        + m.get(TestConstants.KEY_AMC_HOST) + ":"
                        + m.get(TestConstants.KEY_AMC_PORT)
                        + m.get(TestConstants.KEY_AMC_URI);
                entityName = (String)m.get(
                        TestConstants.KEY_FEDLETIDP_ENTITY_NAME);
                idpmetaAlias = (String)m.get(
                        TestConstants.KEY_IDP_METAALIAS);
                idpcertAlias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                executionRealm = (String)m.get(
                        TestConstants.KEY_ATT_EXECUTION_REALM);
                idpattrauthority = (String)m.get(
                        TestConstants.KEY_IDP_ATTRAUTHOIRTY);
                idpauthnauthority = (String)m.get(
                        TestConstants.KEY_IDP_AUTHNAUTHORITY);
                idpscertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpttrascertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpauthnascertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpecertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpattraecertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                idpauthnaecertalias = (String)m.get(
                        TestConstants.KEY_ATT_CERTALIAS);
                cot = (String)m.get(TestConstants.KEY_FEDLET_COT);
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
            
            arrMetadata[0] = MultiProtocolCommon.getMetadataFromPage(metaPage);
            arrMetadata[1] = MultiProtocolCommon.getExtMetadataFromPage(metaPage);

            if ((arrMetadata[0].equals(null)) || 
                    (arrMetadata[1].equals(null))) {
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
}
