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
 * $Id: FSSSOWMLPostProfileHandler.java,v 1.2 2008/06/25 05:46:59 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.fednsso;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

import java.util.List;
import java.util.Iterator;


/**
 * <code>IDP</code> single sign on service handler that handles <code>WML</code>
 * post profile.
 */
public class FSSSOWMLPostProfileHandler extends FSSSOAndFedHandler {
    
    protected FSSSOWMLPostProfileHandler () {
    }
    
    /**
     * Constructor.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param authnRequest authentication request
     * @param spDescriptor <code>SP</code>'s provider descriptor
     * @param spConfig <code>SP</code>'s extended meta config
     * @param spEntityId <code>SP</code>'s entity id
     * @param relayState where to go after single sign on is done
     */
    public FSSSOWMLPostProfileHandler (HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FSAuthnRequest authnRequest, 
                                    SPDescriptorType spDescriptor,
                                    BaseConfigType spConfig,
                                    String spEntityId,
                                    String relayState) 
    {
        super(request, response, authnRequest, spDescriptor, 
            spConfig, spEntityId, relayState);
    }
    
    protected void sendAuthnResponse (FSAuthnResponse authnResponse) {
        FSUtils.debug.message(
            "FSSSOWMLPostProfileHandler.sendAuthnResponse: Called");
        try {
            authnResponse.setProviderId(hostedEntityId);
            Document doc = XMLUtils.toDOMDocument(
                authnResponse.toXMLString(true, true), FSUtils.debug);
            //sign assertions
            if (FSServiceUtils.isSigningOn()){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOWMLPostProfileHandler."
                        + "sendAuthnResponse: start signing assertions");
                }
                List assList = authnResponse.getAssertion();
                if (assList != null){
                    Iterator iter = assList.iterator();
                    while (iter.hasNext ()){
                        FSAssertion assertion = (FSAssertion)iter.next();
                        String id = assertion.getID();
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSSOWMLPostProfileHandler."
                                + "sendAuthnResponse: id attr is" + id);
                        }
                        String certAlias = 
                            IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                        if (certAlias == null) {
                            FSUtils.debug.error(
                                "SOAPReceiver.onMessage: "
                                + "couldn't obtain this site's cert alias.");
                            return;
                        }
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOWMLPostProfileHandler." +
                                "sendAuthnResponse: Site's certAlias is " +
                                certAlias);
                        }
                        XMLSignatureManager manager =
                            XMLSignatureManager.getInstance();
                        int minorVersion = assertion.getMinorVersion();  
                        if (minorVersion ==
                            IFSConstants.FF_11_ASSERTION_MINOR_VERSION) 
                        {
                            manager.signXML(doc, certAlias,
                                SystemConfigurationUtil.getProperty(
                                    SAMLConstants.XMLSIG_ALGORITHM),
                                IFSConstants.ID, id, false);
                        } else if (minorVersion ==
                               IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION
                            ||
                            minorVersion ==
                                IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION)
                        {
                            manager.signXML(doc, certAlias,
                                SystemConfigurationUtil.getProperty(
                                    SAMLConstants.XMLSIG_ALGORITHM),
                                IFSConstants.ASSERTION_ID,
                                assertion.getAssertionID(), false);
                        } else { 
                            FSUtils.debug.message("invalid minor version.");
                        }
                        
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSSOWMLPostProfileHandler."
                                + "sendAuthnResponse: SignatureManager"
                                + " finished signing ");
                        }
                    }
                }
            } 
            
            String respStr = FSServiceUtils.printDocument(doc);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOWMLPostProfileHandler."
                    + "sendAuthnResponse: Signed AuthnResponse: " +  respStr);
            }
            String b64Resp = Base64.encode(respStr.getBytes());
            
            response.setContentType ("text/vnd.wap.wml");
            response.setHeader ("Pragma", "no-cache");
            response.setHeader ("Cache-Control", "no-cache");
            PrintWriter out = response.getWriter ();
            out.println("<!DOCTYPE wml PUBLIC \"-//WAPFORUM//DTD WML 1.1//EN\" "
                + "\"http://www.wapforum.org/DTD/wml_1.1.xml\">");
            out.println("<wml>");
            out.println("<card id=\"response\" title=\"IDP Response\">");
            out.println("<onevent type=\"onenterforward\">");
            out.println("<go method=\"post\" href=\"" 
                + FSServiceUtils.getAssertionConsumerServiceURL(
                    spDescriptor, 
                    authnRequest.getAssertionConsumerServiceID()) + "\">");
            out.println("<postfield name=\"" 
                + IFSConstants.POST_AUTHN_RESPONSE_PARAM + "\" " + "value=\"" 
                + b64Resp + "\"/>");
            out.println("</go>");
            out.println("</onevent>");
            out.println("<onevent type=\"onenterbackward\">");
            out.println("<prev/>");
            out.println("</onevent>");
            out.println("</card>");
            out.println("</wml>");
            out.close();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOWMLPostProfileHandler:sendAuthnResponse: "
                    + "AuthnResponse sent successfully to: "
                    + FSServiceUtils.getAssertionConsumerServiceURL(
                        spDescriptor, 
                        authnRequest.getAssertionConsumerServiceID()));
            }
        } catch(Exception ex){
            FSUtils.debug.message(
                "FSSSOWMLPostProfileHandler:sendAuthnResponse: "
                + "Failed to send AuthnResponse");
        }
    }
    
    protected boolean doSingleSignOn(Object ssoToken, 
                                    String inResponseTo, 
                                    NameIdentifier opaqueHandle, 
                                    NameIdentifier idpOpaqueHandle) 
    {
        FSAuthnResponse authnResponse = createAuthnResponse (ssoToken, 
                                                        inResponseTo, 
                                                        opaqueHandle, 
                                                        idpOpaqueHandle);
        sendAuthnResponse (authnResponse);
        return true;
    }
    
}
