/*
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
 * $Id: SAMLClient.java,v 1.6 2008/08/19 19:11:11 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.saml; 

import java.io.*;
import java.util.*;
import java.net.URL;
import javax.servlet.http.*;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.*;
import com.sun.identity.saml.assertion.*;
import com.sun.identity.saml.protocol.*;
import com.sun.identity.saml.servlet.SAMLSOAPReceiver;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * The class <code>SAMLClient</code> provides interfaces
 * to do Web and POST profile as specified by SAML specification. It
 * also provides methods to get Assertions based on Artifacts.
 * @supported.api
 */

public class SAMLClient {   
    /**
     * This private method is designed to do the SAML Single-Sign-On. 
     * It is called internally by doWebArtifact and doWebPOST methods. 
     * @param request HTTP Servlet Request
     * @param response HTTP Servlet Response
     * @param target the target URL
     * @param service the service name 
     * @exception IOException if an input or output exception occurs when 
     *     redirecting to service <code>URL</code>
     * @exception SAMLException if SAML error occurs during Single-Sign-On.
     */
    private static void doSSO(HttpServletRequest request,
                              HttpServletResponse response, 
                              String target, String service) 
                              throws IOException, SAMLException {
        if (request == null || response == null || target == null) {
            SAMLUtils.debug.error("SAMLClient:Input parameter is null.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput")); 
        }
        if ((!service.equals(SAMLConstants.SAML_AWARE_NAMING)) &&
            (!service.equals(SAMLConstants.SAML_POST_NAMING)) &&
            (!service.equals(SAMLConstants.SAML_SOAP_NAMING))) {
            SAMLUtils.debug.error("SAMLClient:illegal naming service name.");
            throw new SAMLException(
                SAMLUtils.bundle.getString("illegalNamingService")); 
        }
        Object ssoToken = null;  
        SessionProvider sessionProvider;
        try {
            sessionProvider = SessionManager.getProvider();
            ssoToken =sessionProvider.getSession(request);
            if (ssoToken == null) {
                SAMLUtils.debug.error("SAMLClient:SSOToken is null.");
                throw new SAMLException( 
                    SAMLUtils.bundle.getString("nullSSOToken")); 
            }
            if (!sessionProvider.isValid(ssoToken)) {
                SAMLUtils.debug.error("SAMLClient:Session is invalid."); 
                throw new SAMLException(
                    SAMLUtils.bundle.getString("invalidSSOToken")); 
            }
        } catch (SessionException se) {
            SAMLUtils.debug.error("SAMLClient", se);
            throw new SAMLException("SAMLClient:doSSO:" + se.getMessage()); 
        }
        URL weburl = null;
        try { 
            URL serverurl = new URL(SAMLServiceManager.getServerURL()); 
            weburl = SystemConfigurationUtil.getServiceURL(service,  
                serverurl.getProtocol(), serverurl.getHost(),
                serverurl.getPort(), serverurl.getPath());  
            
        } catch(SystemConfigurationException ue) {
            SAMLUtils.debug.error("SAMLClient", ue); 
            throw new SAMLException(
                SAMLUtils.bundle.getString("URLNotFoundException")); 
        }
        StringBuffer redirectedurl = new StringBuffer(200); 
        String tname = (String) SAMLServiceManager.
             getAttribute(SAMLConstants.TARGET_SPECIFIER);   
        redirectedurl.append(weburl).append("?").append(tname).append("=").
             append(target);
        response.sendRedirect(redirectedurl.toString()); 
    }             

    /**
     * This method is designed to do the SAML web-browser profile with 
     * Artifact. Once the browser (user) authenticated to OpenAM,
     * it can call this method to complete the single sign on to the
     * target host and be redirected to the specified target site.
     * @param request HTTP Servlet Request
     * @param response HTTP Servlet Response
     * @param target A String representing the target URL
     * @exception IOException if an input or output exception occurs when
     *     redirecting to service <code>URL</code>
     * @exception SAMLException if SAML error occurs during the process 
     * @supported.api
     */
    public static void doWebArtifact(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     String target) 
                                     throws IOException, SAMLException {
        doSSO(request, response, target, SAMLConstants.SAML_AWARE_NAMING);
    }

    /**
     * This method is designed to do the SAML web-browser POST profile. 
     * Once the browser (user) authenticated to OpenAM,
     * it can call this method
     * to complete the single sign on to the target host and be
     * redirected to the target site.
     * @param request HTTP Servlet Request
     * @param response HTTP Servlet Response
     * @param target A String representing the target URL
     * @exception IOException if an input or output exception occurs when
     *     redirecting to service <code>URL</code>
     * @exception SAMLException if SAML error occurs during the process 
     * @supported.api
     */
    public static void doWebPOST(HttpServletRequest request,
                                 HttpServletResponse response, 
                                 String target)
                                 throws IOException, SAMLException {
        doSSO(request, response, target, SAMLConstants.SAML_POST_NAMING);
    }
    
    /**
     * This method returns the Assertion for the corresponding artifact.
     * It sends an <code>ArtifactQuery</code> SAML message to the
     * destination identified by the source ID in the artifact and
     * returns the Assertion contained in the SAML response message.
     *
     * @param artifact An <code>AssertionArtifact</code> representing the
     *                 artifact 
     * @return An Assertion corresponding to the artifact
     * @exception IOException if an input or output exception occurs when
     *     connecting to SAML service <code>URL</code>
     * @exception SAMLException if SAML error occurs during the process 
     * @supported.api
     */
    public static Assertion getAssertionByArtifact(AssertionArtifact artifact)
        throws IOException, SAMLException {          
        return getAssertionByArtifact(artifact.getAssertionArtifact());
    }

    /**
     * This method returns the Assertion for the corresponding artifact.
     * It sends an <code>ArtifactQuery</code> SAML message to the destination
     * identified by the source ID in the artifact and returns the Assertion
     * contained in the SAML response message.
     *
     * @param artifact A String representing the artifact
     * @return An Assertion corresponding to the artifact
     * @exception IOException if an input or output exception occurs when
     *     connecting to SAML service <code>URL</code>
     * @exception SAMLException if SAML error occurs during the process
     * @supported.api
     */
    public static Assertion getAssertionByArtifact(String artifact)
        throws IOException, SAMLException {
        if (artifact == null || artifact.length() == 0) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLClient: input is null.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("nullInput"));
        }
        // first, check if the sourceid contained in the artifact has an entry 
        // in SAML config
        AssertionArtifact aa = new AssertionArtifact(artifact); 
        String sid = aa.getSourceID(); 
        String ssurl = getSamlSoapUrl(sid); 
        // if not, query naming service to get the soap url in case of local 
        URL samlsoap = null; 
        try {
            if (ssurl == null) {
                Map instances= (Map)
                  SAMLServiceManager.getAttribute(SAMLConstants.INSTANCE_LIST); 
                if (instances == null || instances.size() == 0) {
                    throw new SAMLException( 
                        SAMLUtils.bundle.getString("instancemapNull")); 
                }
                String server= (String) instances.get(sid); 
                if (server == null || server.length() == 0) {
                    throw new SAMLException(
                        SAMLUtils.bundle.getString("instanceNotFound")); 
                }
                URL serverurl = new URL(server); 
                samlsoap = SystemConfigurationUtil.getServiceURL(
                    SAMLConstants.SAML_SOAP_NAMING,
                    serverurl.getProtocol(), serverurl.getHost(),
                    serverurl.getPort(), serverurl.getPath());   
            } else {
                samlsoap = new URL(ssurl);
            }
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLClient:SOAPUrl=" +
                    samlsoap.toString());
            }
        } catch (SystemConfigurationException ue) {
            SAMLUtils.debug.error("SAMLClient", ue);
            throw new SAMLException(
                SAMLUtils.bundle.getString("URLNotFoundException"));
        }
        if (!setLocalFlag(samlsoap)) {
            throw new SAMLException( 
                SAMLUtils.bundle.getString("failSetLocalFlag"));
        }
     
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("SAMLClient:getAssertionByArtifact: " +
                                    "check localFlag : " +
                                     SAMLServiceManager.localFlag);
        }
        String encodedSourceid = (String) SAMLServiceManager.getAttribute(
                                                SAMLConstants.SITE_ID);
        boolean isMySite = sid.equals(encodedSourceid.trim()); 
        if (SAMLServiceManager.localFlag && isMySite)  {
            // if the localFlag is true and the Artifact's source id is 
            // the same as my site_id, (means SAMLClient and AssertionManager 
            // in the same JVM, call AssertionManager directly.
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLClient:getAssertionByArtifact" +
                                ":call AssertionManager.getAssertion(" +
                                "AssertionArtifact)");      
            }
            AssertionManager assertManager = AssertionManager.getInstance(); 
            Assertion assertion = assertManager.getAssertion(aa);  
            return assertion;
        }
        String[] strarray = new String[1];
        strarray[0]= artifact;
        List asserts = null; 
        if (isMySite && ssurl == null) {
            asserts = artifactQueryHandler(strarray, samlsoap.toString()); 
        } else {
            asserts = artifactQueryHandler(strarray, null); 
        }
        if (asserts == null || asserts.isEmpty()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLClient:getAssertionByArtifact" +
                                    ":returned assertion list is null.");      
               }
            return null; 
        }
        return ((Assertion) asserts.get(0)); 
    }

    private static String getSamlSoapUrl(String sourceid) {
        String soapurl = null;
        try {
            Map partner = (Map) SAMLServiceManager.getAttribute(
                                                   SAMLConstants.PARTNER_URLS); 
            if (partner == null) {
                SAMLUtils.debug.error("SAMLClient:Partner URL is null.");
                return null;
            }
            SAMLServiceManager.SOAPEntry partnerdest = 
                           (SAMLServiceManager.SOAPEntry) partner.get(sourceid);
            if (partnerdest != null) {
                soapurl = partnerdest.getSOAPUrl(); 
            } else {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("SAMLClient: " + sourceid + 
                                            " is not on trusted site list.");
                }
            }
            return soapurl;
        } catch (Exception se) {
            SAMLUtils.debug.error("SAMLClient: ", se);
            return null;
        } 
    }
    
    public static boolean setLocalFlag(URL url) {
        if (url == null) {
            SAMLUtils.debug.error("SAMLClient:setLocalFlag has null input.");
            return false; 
        }
        try {
             // Preload class SAMLSOAPReceiver since it wouldn't be included 
             // in the remote sdk. If the class SAMLSOAPReceiver isn't 
             // presented, we consider it is client application. 
             Class.forName("com.sun.identity.saml.servlet.SAMLSOAPReceiver");
             if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("in setLocalFlag(), url : " + 
                                        url.toString());
                SAMLUtils.debug.message("SAMLSOAPReceiver.localSAMLServiceID : "
                                        + SAMLSOAPReceiver.localSAMLServiceID);
            }
            if (SAMLSOAPReceiver.localSAMLServiceID != null) { 
                URL samlservice =
                    new URL(SAMLSOAPReceiver.localSAMLServiceID);   
                if ((url.getHost().equalsIgnoreCase(samlservice.getHost())) && 
                    (url.getPort() == samlservice.getPort())) {
                    SAMLServiceManager.localFlag = true;
                    return true; 
                }
            }
        } catch (ClassNotFoundException cnfe) {
          if (SAMLUtils.debug.messageEnabled()) {
              SAMLUtils.debug.message("SAMLClient::setLocalFlag: ",  
                                       cnfe); 
           }
           SAMLServiceManager.localFlag = false; 
           return true; 
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLClient::setLocalFlag:: ", e);
            return false; 
        }
        SAMLServiceManager.localFlag = false; 
        return true;
    }

    /**
     * This private method takes a SAML request object and returns a SOAPMessage
     * wrapped around the request object. 
     * @param req A SAML request object 
     * @return a SOAPMessage 
     * @exception SAMLException
     */
    private static String createSOAPMessage(Request req)
                               throws SAMLException {
        if (req == null){
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }

        try {
            StringBuffer envBegin = new StringBuffer(100);
            envBegin.append("<").append(SAMLConstants.SOAP_ENV_PREFIX).
                append(":Envelope").append(SAMLConstants.SPACE).
                append("xmlns:").append(SAMLConstants.SOAP_ENV_PREFIX).
                append("=\"").append(SAMLConstants.SOAP_URI).append("\">").
                append(SAMLConstants.NL).append("<").
                append(SAMLConstants.SOAP_ENV_PREFIX).append(":Body>").
                append(SAMLConstants.NL);

            StringBuffer envEnd = new StringBuffer(100);
            envEnd.append(SAMLConstants.START_END_ELEMENT).
                append(SAMLConstants.SOAP_ENV_PREFIX).append(":Body>").
                append(SAMLConstants.NL).
                append(SAMLConstants.START_END_ELEMENT).
                append(SAMLConstants.SOAP_ENV_PREFIX).
                append(":Envelope>").append(SAMLConstants.NL);

            StringBuffer sb = new StringBuffer(300);
            sb.append(envBegin).append(req.toString(true, true)).append(envEnd);
            return(sb.toString());
        } catch (Exception e) {
            throw new SAMLException(e.getMessage()); 
        }   
    }
    
    /**
     * This private method is designed to get the URLEndpoint which points to 
     * the partner's SOAP Receiver service, such as the URLEndpoint of 
     * SAMLSOAPReceiver servlet in OpenAM context.
     * @param destSite  A object of 
     *                com.sun.identity.saml.common.SAMLServiceManager.SOAPEntry
     * @param to An URLEndpoint object 
     * @exception IOException if <code>URL</code> is invalid
     * @exception SAMLException if SAML error occurs during the process
     */
    private static String createSOAPReceiverUrl(
            com.sun.identity.saml.common.SAMLServiceManager.SOAPEntry destSite, 
            String to) throws IOException, SAMLException {
        if (destSite == null || to == null || to.length() == 0) {
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        //get authentication type 
        String authtype = destSite.getAuthType();    
        String urlEndpoint  = null;
        int idnx = -1; 
        if ((idnx = to.indexOf("//")) == -1) {
            SAMLUtils.debug.error("SAMLClient:createSOAPReceiverUrl:" +
                                  "Illegal format of input parameter.");  
            throw new SAMLException(
                      SAMLUtils.bundle.getString("illegalFormatSOAPUrl")); 
        }
        String protocol = to.substring(0, idnx-1);  
        // check if the authentication type matches the protocol specified in 
        // input parameter "to". 
        if (authtype.equalsIgnoreCase(SAMLConstants.BASICAUTH) || 
            authtype.equalsIgnoreCase(SAMLConstants.NOAUTH)) {
            if (!protocol.equals(SAMLConstants.HTTP)) {    
                if (SystemConfigurationUtil.isServerMode()) {
                    String[] data = {SAMLUtils.bundle.getString(
                      "mismatchAuthTypeandProtocol")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.AUTH_PROTOCOL_MISMATCH, data);
                }
                throw new SAMLException(    
                    SAMLUtils.bundle.getString("mismatchAuthTypeandProtocol"));
            }
        } else if (authtype.equalsIgnoreCase(SAMLConstants.SSLWITHBASICAUTH)  
                || authtype.equalsIgnoreCase(SAMLConstants.SSL)) {
            if (!protocol.equals(SAMLConstants.HTTPS)) {    
                if (SystemConfigurationUtil.isServerMode()) {
                    String[] data = {SAMLUtils.bundle.getString(
                      "mismatchAuthTypeandProtocol")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.AUTH_PROTOCOL_MISMATCH, data);
                }
                throw new SAMLException(
                  SAMLUtils.bundle.getString("mismatchAuthTypeandProtocol"));
            }
        } else {
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString(
                   "wrongAuthType")};
                LogUtils.error(java.util.logging.Level.INFO,
                    LogUtils.INVALID_AUTH_TYPE, data);
            }
            throw new SAMLException(
                      SAMLUtils.bundle.getString("wrongAuthType"));
        }

        // If the authentication type is BASICAUTH or SSLWITHBASICAUTH, 
        // call ServiceManager to retrieve the partner's user name and password 
        // which protects the partner's SOAPReceiverURL. 
        if (authtype.equalsIgnoreCase(SAMLConstants.BASICAUTH) || 
            authtype.equalsIgnoreCase(SAMLConstants.SSLWITHBASICAUTH)) {
            String username = destSite.getBasicAuthUserID();
            String password = destSite.getBasicAuthPassword();
            if (username == null || password == null) {
                SAMLUtils.debug.error("SAMLClient:createSOAPReceiverUrl:" +
                    "PartnerSite required basic authentication. But the " +
                    "user name or password used for authentication is null.");
                throw new SAMLException(
                    SAMLUtils.bundle.getString("wrongConfigBasicAuth"));
            }
            String toSOAP = to.substring(0, idnx+2) + username + ":" +
                            password + "@" + to.substring(idnx+2); 
            urlEndpoint = toSOAP;
        } else {
            urlEndpoint = to; 
        }
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("Sending message to URL: " +
                                    urlEndpoint);
        }
        if (SystemConfigurationUtil.isServerMode()) {
            String[] data = {SAMLUtils.bundle.getString("SOAPReceiverURL"),
                urlEndpoint};
            LogUtils.access(java.util.logging.Level.FINE, 
                LogUtils.SOAP_RECEIVER_URL, data);
        }
        return urlEndpoint;
    }
    
    /**
     * This private method is designed to get the SAML response object from  
     * a SOAPMessage string. 
     * @param xmlString A String representing a string of SOAPMessage 
     * @return a SAML Response object
     * @exception IOException if an input or output exception occurs when
     *     connecting to SAML service <code>URL</code>
     * @exception SAMLException if SAML error occurs during the process
     */
    private static Response getSAMLResponse(String xmlString) 
                            throws IOException, SAMLException {
        if (xmlString == null || xmlString.length() == 0) {
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }   
        Response samlResp = null;                                
        Document doc = XMLUtils.toDOMDocument(xmlString, SAMLUtils.debug);
        Element root= doc.getDocumentElement();
        String rootName  = root.getLocalName();
        if ((rootName == null) || (rootName.length() == 0)) {
            SAMLUtils.debug.error("Missing Envelope tag.");
            throw new SAMLException (
                      SAMLUtils.bundle.getString("missingSOAPEnvTag"));
        }
        if (!(rootName.equals("Envelope")) ||
            (!(root.getNamespaceURI().equals(SAMLConstants.SOAP_URI)))) {
            SAMLUtils.debug.error("Wrong Envelope tag or namespace.");   
            throw new SAMLException(
                SAMLUtils.bundle.getString("serverError"));
        }
        //exam the child element of <SOAP-ENV:Envelope>
        NodeList  nodes = root.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount <= 0) {
            SAMLUtils.debug.error("Envelope does not contain a SOAP body."); 
            throw new SAMLException(
                      SAMLUtils.bundle.getString("missingSOAPBody"));
        }
        String tagName = null; 
        String ctagName = null; 
        Node currentNode = null; 
        Node cnode = null; 
        for (int i = 0; i < nodeCount; i++) {
            currentNode = nodes.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                tagName = currentNode.getLocalName();
                if ((tagName == null) || tagName.length() == 0) {
                    SAMLUtils.debug.error("Missing tag name of child element");
                    throw new SAMLException(
                             SAMLUtils.bundle.getString("missingChildTagName"));
                }
                if (tagName.equals("Body")) {
                    NodeList cNodes = currentNode.getChildNodes(); 
                    int cnodeCount = cNodes.getLength(); 
                    for (int j = 0; j < cnodeCount; j++) {
                        cnode = cNodes.item(j); 
                        if (cnode.getNodeType() == Node.ELEMENT_NODE){ 
                            ctagName = cnode.getLocalName();
                            if ((ctagName == null) || ctagName.length() == 0) {
                                SAMLUtils.debug.error("Missing tag name of " +
                                            "child element of <SOAP-ENV:Body>");
                                throw new SAMLException(      
                                          SAMLUtils.bundle.getString(
                                                    "missingChildTagName"));
                            }
                            if (ctagName.equals("Fault")) {
                                SAMLUtils.debug.error("SOAPFault error."); 
                                throw new SAMLException(
                                          XMLUtils.print(cnode)); 
                            } else if (ctagName.equals("Response")) {
                                samlResp = new Response((Element) cnode); 
                                if (SAMLUtils.debug.messageEnabled()) {
                                    SAMLUtils.debug.message("SAML Response:" +
                                                        samlResp.toString());
                                }
                                break;
                            } else {
                                SAMLUtils.debug.error("Wrong child element " +
                                                      "in SOAPBody");
                                throw new SAMLException(
                                          SAMLUtils.bundle.getString(
                                          "wrongSOAPBody")); 
                            }
                        }
                    } // end of for(int j=0; j <cnodeCount; j++) 
                } else if (tagName.equals("Header")) {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("Inside SOAP Response:" +
                                                 " SOAP Header");
                    }
                } else {
                    SAMLUtils.debug.error("Wrong child element in Envelope"); 
                    throw new SAMLException(
                              SAMLUtils.bundle.getString("wrongSOAPElement"));  
                }
            } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE) 
        } // end of for (int i = 0; i < nodeCount; i++)
        return samlResp; 
    }

    /**
     * This method is designed to get a list of assertion from the 
     * SAML Response.
     * @param samlresponse A SAML Response object
     * @param alist a List  
     * @return a List object representing a list of Assertion
     * @exception SAMLException
     */
    private static List getAssertionList(Response samlresponse, List alist) 
                                         throws SAMLException {
        if (samlresponse == null || alist == null) {
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        // get a list of SAML assertion 
        List assertions = new ArrayList(); 
        assertions = samlresponse.getAssertion();
        if (assertions == null || assertions.isEmpty()) {      
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString(
                "noAssertioninResponse"), samlresponse.toString(true, true)};
                LogUtils.error(java.util.logging.Level.INFO, 
                    LogUtils.NO_ASSERTION_IN_RESPONSE, data);
            }
            throw new SAMLException(
                 SAMLUtils.displayXML(samlresponse.getStatus().toString()));
        }  
        if (assertions.size() != alist.size()) {
            SAMLUtils.debug.error("The SAML response containing assertions !=" 
                             + "the number of artifacts in SAML request");
            if (SystemConfigurationUtil.isServerMode()) {                       
                String[] data = {SAMLUtils.bundle.getString(
                    "wrongNumberAssertions"),
                    samlresponse.toString(true, true)};
                LogUtils.error(java.util.logging.Level.INFO, 
                    LogUtils.MISMATCHED_ASSERTION_AND_ARTIFACT, data);
            }
            throw new SAMLException(
                      SAMLUtils.bundle.getString("wrongNumberAssertions")); 
        }   
        return assertions; 
    }
    
    /**
     * This method is designed to get a list of assertion based on the input 
     * <code>AssertionArtifact</code>(s). 
     *
     * @param arti An array of String 
     * @return a List object representing a list of Assertions
     * @exception IOException if an input or output exception occurs when
     *     connecting to SAML service <code>URL</code>
     * @exception SAMLException if SAML error occurs during the process
     */
    public static List artifactQueryHandler(String[] arti, String connecto) 
                       throws IOException, SAMLException {
        if ((arti == null) || (arti.length == 0)) {
            SAMLUtils.debug.message("artifactQueryHandler: null input.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }

        String firstSourceID = null;
        com.sun.identity.saml.common.SAMLServiceManager.SOAPEntry dest= null;
        Response samlresponse = null; 
        List al = new ArrayList(); 
        List artl = new ArrayList();
        
        AssertionArtifact firstArtifact = new AssertionArtifact(arti[0]); 
        firstSourceID = firstArtifact.getSourceID();
        if (SystemConfigurationUtil.isServerMode()) {
            String[] data = {SAMLUtils.bundle.getString("Artifact") + " " + 0, 
                arti[0]};
            LogUtils.access(java.util.logging.Level.INFO,
                LogUtils.ARTIFACT_TO_SEND, data);
        }
        artl.add(firstArtifact);
        al.add(arti[0]);
        AssertionArtifact assertArtifact = null; 
        String destination = null; 
        for (int k = 1; k < arti.length; k++) {
            // check if all Artifact come from the same source id             
            assertArtifact = new AssertionArtifact(arti[k]);
            destination = assertArtifact.getSourceID(); 
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SourceID within the Artifact is " +
                                        destination);
            }
            if (!destination.equals(firstSourceID)) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("Received multiple Artifacts " +
                                                "have different source id.");
                }
                throw new SAMLException(
                          SAMLUtils.bundle.getString("sourceidDifferent")); 
            }
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString("Artifact") + " " 
                    + k, arti[k]};
                LogUtils.access(java.util.logging.Level.FINE,
                    LogUtils.ARTIFACT_TO_SEND, data);
            }
            artl.add(assertArtifact);
            al.add(arti[k]);
        }
        
        try {
            //Retrieve the soap-receiver-url using the sourceid inside of 
            //the AssertionArtifact 
            String to = null; 
            Map soaps = (Map) SAMLServiceManager.getAttribute(
                                                   SAMLConstants.PARTNER_URLS); 
            if (soaps == null) {
                SAMLUtils.debug.error(
                                  SAMLUtils.bundle.getString("nullPartnerUrl"));
                throw new SAMLException(
                                  SAMLUtils.bundle.getString("nullPartnerUrl"));
            }
            String urlEndpoint = null; 
            if (soaps.containsKey(firstSourceID)) {
                dest = (SAMLServiceManager.SOAPEntry) soaps.get(firstSourceID);
                to = dest.getSOAPUrl(); 
                if (to==null) {
                    if (connecto == null || connecto.length() == 0) {
                        if (SystemConfigurationUtil.isServerMode()) {
                            String[] data = {SAMLUtils.bundle.getString(
                                "wrongPartnerSOAPUrl")};
                            LogUtils.error(java.util.logging.Level.INFO,
                                LogUtils.WRONG_SOAP_URL, data);
                        }
                        throw new SAMLException(
                          SAMLUtils.bundle.getString("wrongPartnerSOAPUrl")); 
                    } else {
                        urlEndpoint = connecto;
                    }
                } else {
                    urlEndpoint = createSOAPReceiverUrl(dest, to); 
                }
            } else {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("SAMLClient:artifactQueryHandler: " 
                                 + "Failed to locate SOAP-Receiver-URL " +
                                 "using the source id from AssertionArtifact.");
                }
                if (connecto == null || connecto.length() == 0) {
                    throw new SAMLException(
                             SAMLUtils.bundle.getString("failedLocateSOAPUrl"));
                } else { 
                    urlEndpoint = connecto; 
                }
            }   
         
            if (urlEndpoint == null) {
                SAMLUtils.debug.error("SAMLClient:artifactQueryHandler:" + 
                                      "createSOAPReceiverURL Error!");
                if (SystemConfigurationUtil.isServerMode()) {
                    String[] data = {SAMLUtils.bundle.getString(
                        "wrongPartnerSOAPUrl")};
                    LogUtils.error(java.util.logging.Level.INFO,
                        LogUtils.WRONG_SOAP_URL, data);
                }
                throw new SAMLException(SAMLUtils.bundle.getString(
                                        "wrongPartnerSOAPUrl")); 
            }
            //generate SAML Request    
            Request req = new Request(null, artl);
            String ver = dest.getVersion(); 
            if (ver != null) {
                StringTokenizer st = new StringTokenizer(ver,".");
                if (st.countTokens() == 2) {
                    req.setMajorVersion(
                        Integer.parseInt(st.nextToken().trim()));
                    req.setMinorVersion(
                        Integer.parseInt(st.nextToken().trim()));
                }
            }

            if (((Boolean) SAMLServiceManager.getAttribute(
                SAMLConstants.SIGN_REQUEST)).booleanValue())
            {
                req.signXML();
            }
            // SOAPMessage msg = createSOAPMessage(req);  
            String xmlString = createSOAPMessage(req);

            // Send the message to the provider using the connection.
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SENDING message: \n " + xmlString);  
            }
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString(
                    "sendingSAMLRequest"), xmlString};
                LogUtils.access(java.util.logging.Level.FINE, 
                    LogUtils.SAML_ARTIFACT_QUERY, data); 
            }
            
            // SOAPMessage  reply = con.call(msg, urlEndpoint); 
            String[] urls = { urlEndpoint };
            SOAPClient client = new SOAPClient(urls);
            InputStream inbuf = client.call(xmlString, null, null);
            StringBuffer reply = new StringBuffer();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                inbuf, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                reply.append(line).append("\n");
            }

            //reply should contain SAML response 
            if (reply == null) {
                if (SystemConfigurationUtil.isServerMode()) {
                    String[] data = {SAMLUtils.bundle.getString(
                        "noReplyfromSOAPReceiver")};
                    LogUtils.error(java.util.logging.Level.INFO, 
                        LogUtils.NO_REPLY_FROM_SOAP_RECEIVER, data);
                }
                throw new SAMLException( 
                         SAMLUtils.bundle.getString("noReplyfromSOAPReceiver"));
            }
            
            // check the SOAP message for any SOAP related errors
            // before passing control to SAML processor
            xmlString = reply.toString();
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("REPLIED message: \n " + xmlString);  
            }
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString(
                    "repliedSOAPMessage"), xmlString};
                LogUtils.access(java.util.logging.Level.FINE, 
                    LogUtils.REPLIED_SOAP_MESSAGE, data);
            }
            samlresponse = getSAMLResponse(xmlString); 
            if (samlresponse == null) {
                SAMLUtils.debug.error("SAMLClient:artifactQueryHandler:"+
                            "No SAML Response contained in SOAPMessage.");
                if (SystemConfigurationUtil.isServerMode()) {
                    String[] data = {SAMLUtils.bundle.getString(
                        "noSAMLResponse")};
                    LogUtils.error(java.util.logging.Level.INFO, 
                        LogUtils.NULL_SAML_RESPONSE, data);
                }
                throw new SAMLException(SAMLUtils.bundle.getString(
                                        "noSAMLResponse"));
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLClient:artifactQueryHandler", e); 
            throw new SAMLException(e.getMessage()); 
        }
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("Start to process SAML Response..."); 
        }
        // Process saml Response 
        if (!samlresponse.isSignatureValid()) {
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString(
                   "cannotVerifyResponse")};
                LogUtils.error(java.util.logging.Level.INFO, 
                   LogUtils.INVALID_RESPONSE_SIGNATURE, data);
            }
            throw new SAMLException(
                      SAMLUtils.bundle.getString("cannotVerifyResponse"));
        }       
        try {        
            String statuscode= samlresponse.getStatus().getStatusCode().
                               getValue(); 
            int idex=0; 
            if ((idex=statuscode.indexOf(":")) == -1) {
                throw new SAMLException(
                      SAMLUtils.bundle.getString("wrongformatStatusCode")); 
            }
            if (!(statuscode.substring(idex).equals(":Success"))) {
               SAMLUtils.debug.error("Error:SAML StatusCode is not Success");
               throw new SAMLException(
                   SAMLUtils.displayXML(samlresponse.getStatus().toString())); 
            }      
        } catch (Exception e) {
            if (SystemConfigurationUtil.isServerMode()) {
                String[] data = {SAMLUtils.bundle.getString(
                    "errorSAMLStatusCode")};
                LogUtils.error(java.util.logging.Level.INFO, 
                    LogUtils.ERROR_RESPONSE_STATUS, data);
            }
            throw new SAMLException(e.getMessage());
        } 
        // retrieve SAML Assertion
        List asserts = new ArrayList(); 
        asserts = getAssertionList(samlresponse, al); 
        return asserts; 
    }
}
