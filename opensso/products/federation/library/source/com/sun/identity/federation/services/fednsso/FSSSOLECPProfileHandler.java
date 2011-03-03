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
 * $Id: FSSSOLECPProfileHandler.java,v 1.3 2008/06/25 05:46:59 qcheng Exp $
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
import com.sun.identity.federation.message.FSAuthnResponseEnvelope;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.*;
import com.sun.identity.federation.services.util.*;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.encode.URLEncDec;
import javax.xml.soap.*;


import javax.servlet.*;
import java.util.*;

import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <code>IDP</code> single sign on service handler handles <code>LECP</code>
 * profile.
 */
public class FSSSOLECPProfileHandler extends FSSSOAndFedHandler {
    
    protected FSSSOLECPProfileHandler () {
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
    public FSSSOLECPProfileHandler (HttpServletRequest request, 
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
    
    /**
     * Processes <code>LECP</code> authentication request.
     * @param authnRequest authentication request
     */
    public void processLECPAuthnRequest (FSAuthnRequest authnRequest){
        processAuthnRequest(authnRequest, false);
        return;
    }
    
    /**
     * Generates local login url.
     * @param loginUrl authentication base url
     * @param authnContext requested <code>AuthnContextRef</code>
     * @return local login url with appropriate parameters
     */
    public String formatLoginURL(
        String loginUrl, 
        String authnContext
    ) 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSSOLECPProfileHandler.formatLoginURL: Called" +
                "\nloginUrl=" + loginUrl +
                "\nauthnContext=" + authnContext);
        }
        
        try {
            if (loginUrl == null){
                FSUtils.debug.error("FSSSOLECPProfileHandler.formatLoginURL: ");
                return null;
            }
            
            //create return url
            String ssoUrl = hostedDesc.getSingleSignOnServiceURL();
            StringBuffer returnUrl = new StringBuffer(ssoUrl);
            if (ssoUrl.indexOf('?') == -1) {
                returnUrl.append("?");
            } else {
                returnUrl.append("&");
            }
            returnUrl.append(IFSConstants.LECP_INDICATOR_PARAM)
                .append("=").append(IFSConstants.LECP_INDICATOR_VALUE)
                .append("&").append(IFSConstants.AUTHN_INDICATOR_PARAM)
                .append("=").append(IFSConstants.AUTHN_INDICATOR_VALUE)
                .append("&");
            if (!authnContext.equals(
                IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD)) 
            {
                returnUrl.append(IFSConstants.AUTHN_CONTEXT)
                    .append("=")
                    .append(URLEncDec.encode(authnContext))
                    .append("&");
            }
            returnUrl.append(IFSConstants.PROVIDER_ID_KEY)
                .append("=")
                .append(URLEncDec.encode(hostedEntityId))
                .append("&").append(IFSConstants.REALM)
                .append("=")
                .append(URLEncDec.encode(realm))
                .append("&").append(IFSConstants.META_ALIAS)
                .append("=")
                .append(URLEncDec.encode(metaAlias))
                .append("&").append(IFSConstants.AUTH_REQUEST_ID)
                .append("=")
                .append(URLEncDec.encode(authnRequest.getRequestID()));
            
            //create goto url
            StringBuffer gotoUrl = 
                new StringBuffer(IFSConstants.POST_LOGIN_PAGE);
            gotoUrl.append("/").append(IFSConstants.META_ALIAS)
                .append(metaAlias).append("/");

            FSSessionManager sessMgr =
                FSSessionManager.getInstance(metaAlias);
            String id = authnRequest.getRequestID();
            sessMgr.setRelayState(id, returnUrl.toString());

            gotoUrl.append(IFSConstants.LRURL).append("/")
                .append(URLEncDec.encode(id))
                .append("/").append(IFSConstants.SSOKEY).append("/")
                .append(IFSConstants.SSOVALUE);
            
            //create redirect url
            StringBuffer redirectUrl = new StringBuffer(100);
            redirectUrl.append(loginUrl);
            if (loginUrl.indexOf('?') == -1) {
                redirectUrl.append("?");
            } else {
                redirectUrl.append("&");
            }
            redirectUrl.append(IFSConstants.GOTO_URL_PARAM).append("=");
            redirectUrl.append(URLEncDec.encode(
                gotoUrl.toString())).append("&");

            String authUrl = FSUtils.getAuthDomainURL(realm);
            if (authUrl != null && authUrl.length() != 0){
                redirectUrl.append(IFSConstants.ORGKEY).append("=").
                    append(URLEncDec.encode(authUrl)).append("&");
            }
            int len = redirectUrl.length() - 1;
            if (redirectUrl.charAt(len) == '&') {
                redirectUrl = redirectUrl.deleteCharAt(len);
            }
            return redirectUrl.toString();
        } catch(Exception e){
            FSUtils.debug.error(
                "FSSSOLECPProfileHandler.formatLoginURL: Exception: ", e);
            return null;
        }
    }
    
    protected void sendAuthnResponse (FSAuthnResponse authnResponse) {
        authnResponse.setProviderId(hostedEntityId);
        FSAuthnResponseEnvelope respEnvelope = 
            new FSAuthnResponseEnvelope (authnResponse);        
        respEnvelope.setMinorVersion(authnResponse.getMinorVersion());
        respEnvelope.setAssertionConsumerServiceURL(
            FSServiceUtils.getAssertionConsumerServiceURL(spDescriptor, null));
        FSSOAPService soapService = FSSOAPService.getInstance();
        
        SOAPMessage retMessage = null;
        try {
            retMessage = soapService.bind(respEnvelope.toXMLString(true, true));
        } catch (FSMsgException ex) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOLECPProfileHandler.sendAuthnResponse: ", ex);
            }
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            returnSOAPMessage(
                soapService.formSOAPError(
                    "Server", "cannotProcessRequest", null),
                response);
            return;
        }
        response.setHeader(IFSConstants.LECP_HEADER_NAME , 
                        request.getHeader(IFSConstants.LECP_HEADER_NAME));
        response.setContentType(IFSConstants.LECP_RESP_CONTENT_TYPE_HEADER);
        if (FSServiceUtils.isSigningOn()){
            try {
                List assList = authnResponse.getAssertion();
                Iterator iter = assList.iterator();
                while (iter.hasNext ()){
                    FSAssertion assertion = (FSAssertion)iter.next();
                    String id = assertion.getID();
                    Document doc =
                        (Document)FSServiceUtils.createSOAPDOM(retMessage);
                    String certAlias =
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                    if (certAlias == null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOLECPProfileHandler.sendAuthnResponse: "
                                + "couldn't obtain this site's cert alias.");
                        }
                        response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                        returnSOAPMessage(
                            soapService.formSOAPError(
                                "Server", "cannotProcessRequest", null),
                            response);
                        return;
                    }
                    XMLSignatureManager manager =
                        XMLSignatureManager.getInstance();
                    int minorVersion = assertion.getMinorVersion();  
                    if (minorVersion ==
                        IFSConstants.FF_11_ASSERTION_MINOR_VERSION) 
                    {
                        manager.signXML(doc,
                            certAlias,
                            SystemConfigurationUtil.getProperty(
                                SAMLConstants.XMLSIG_ALGORITHM),
                            IFSConstants.ID,
                            id,
                            false);
                    } else if (minorVersion == 
                            IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION ||
                        minorVersion ==
                            IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION)
                    {
                        manager.signXML(doc,
                            certAlias,
                            SystemConfigurationUtil.getProperty(
                                SAMLConstants.XMLSIG_ALGORITHM),
                            IFSConstants.ASSERTION_ID,
                            assertion.getAssertionID(),
                            false);
                    } else { 
                        FSUtils.debug.message("invalid minor version.");
                    }
                    
                    retMessage = FSServiceUtils.convertDOMToSOAP(doc);
                }
            } catch (Exception e) {
                response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                returnSOAPMessage(
                    soapService.formSOAPError(
                        "Server", "cannotProcessRequest", null),
                    response);
                return;
            }
        }
        returnSOAPMessage(retMessage, response);
        
    }
    
    protected boolean doSingleSignOn (Object ssoToken, 
                                      String inResponseTo,
                                      NameIdentifier opaqueHandle,
                                      NameIdentifier idpOpaqueHandle) 
    {
        FSAuthnResponse authnResponse = createAuthnResponse(
            ssoToken, inResponseTo, opaqueHandle, idpOpaqueHandle);
        sendAuthnResponse(authnResponse);
        return true;
    }
    
    private void returnSOAPMessage(
        SOAPMessage msg,
        HttpServletResponse response) 
    {
        try {
            if (msg != null) {
                response.setHeader(IFSConstants.LECP_HEADER_NAME,
                        request.getHeader(IFSConstants.LECP_HEADER_NAME));
                response.setContentType(
                    IFSConstants.LECP_RESP_CONTENT_TYPE_HEADER);
                ServletOutputStream servletoutputstream = 
                    response.getOutputStream();
                
                msg.writeTo(servletoutputstream);
                servletoutputstream.flush();
                return;
            } else {
                response.flushBuffer ();
                return;
            }
        } catch(Exception e) {
            FSUtils.debug.error(
                "FSSOAPReceiver.returnSOAPMessage: Exception::", e);
            return;
        }
    }
}
