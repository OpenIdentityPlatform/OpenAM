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
 * $Id: FSSSOBrowserPostProfileHandler.java,v 1.3 2008/12/19 06:50:46 exu Exp $
 *
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAssertion;
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
import java.util.logging.Level;
import java.util.Iterator;

/**
 * <code>IDP</code> single sign on service handler handles browser post
 * profile.
 */
public class FSSSOBrowserPostProfileHandler extends FSSSOAndFedHandler {
    
    protected FSSSOBrowserPostProfileHandler () {
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
    public FSSSOBrowserPostProfileHandler(HttpServletRequest request, 
                                            HttpServletResponse response, 
                                            FSAuthnRequest authnRequest, 
                                            SPDescriptorType spDescriptor,
                                            BaseConfigType spConfig,
                                            String spEntityId,
                                            String relayState) 
    {
        super(request, response, authnRequest, spDescriptor, 
            spConfig, spEntityId, relayState);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message ("FSSSOBrowserPostProfileHandler: "
                + "Handler created to handle AuthnRequest");
        }
        
    }
    
    protected void sendAuthnResponse (FSAuthnResponse authnResponse) {
        FSUtils.debug.message(
            "FSSSOBrowserPostProfileHandler.sendAuthnResponse: Called");
        try {
            authnResponse.setProviderId(hostedEntityId);
            Document doc = XMLUtils.toDOMDocument(
                authnResponse.toXMLString(true, true), FSUtils.debug);
            //sign assertions
            if (FSServiceUtils.isSigningOn() ||
                FSServiceUtils.isSigningOptional())
            {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOBrowserPostProfileHandler."
                        + "sendAuthnResponse: start signing assertions");
                }
                List assList = authnResponse.getAssertion();
                if (assList != null){
                    Iterator iter = assList.iterator();
                    while (iter.hasNext ()){
                        FSAssertion assertion = (FSAssertion)iter.next();
                        String id = assertion.getID();
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOBrowserPostProfileHandler."
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
                                "FSSSOBrowserPostProfileHandler."
                                + "sendAuthnResponse: Site's certAlias is "
                                +  certAlias);
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
                                 
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOBrowserPostProfileHandler."
                                + "sendAuthnResponse: SignatureManager "
                                + "finished signing ");
                        }
                    }
                }
            } 
            
            String respStr = FSServiceUtils.printDocument(doc);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOBrowserPostProfileHandler."
                    + "sendAuthnResponse: Signed AuthnResponse: " +  respStr);
            }
             if (LogUtil.isAccessLoggable(Level.FINER)) {
                String[] data = { respStr };
                LogUtil.access(
                    Level.FINER,LogUtil.CREATE_AUTHN_RESPONSE,data,ssoToken);
            } else {
                String[] data = {
                        FSUtils.bundle.getString("responseID") + "=" +
                        authnResponse.getResponseID() + "," +
                        FSUtils.bundle.getString("inResponseTo") + "=" +
                        authnResponse.getInResponseTo()};
                LogUtil.access(
                    Level.INFO,LogUtil.CREATE_AUTHN_RESPONSE,data,ssoToken);
            }

            String b64Resp = Base64.encode(respStr.getBytes());
            String targetURL = FSServiceUtils.getAssertionConsumerServiceURL(
                spDescriptor, authnRequest.getAssertionConsumerServiceID());
            response.setContentType ("text/html");
            PrintWriter out = response.getWriter ();
            out.println ("<HTML>");
            out.println ("<BODY Onload=\"document.Response.submit()\">");
            out.println ("<FORM NAME=\"Response\" METHOD=\"POST\" ACTION=\"" +
                targetURL + "\">");
            out.println ("<INPUT TYPE=\"HIDDEN\" NAME=\"" +
                IFSConstants.POST_AUTHN_RESPONSE_PARAM + "\" " + "VALUE=\"" +
                b64Resp + "\"/>");
            out.println ("</FORM>");
            out.println ("</BODY></HTML>");
            out.close ();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message ("FSSSOBrowserPostProfileHandler:send" +
                    "AuthnResponse: AuthnResponse sent successfully to: " +
                    targetURL);
            }
            String[] data = {
                    targetURL,
                    FSUtils.bundle.getString("responseID") + "=" +
                    authnResponse.getResponseID() + "," +
                    FSUtils.bundle.getString("inResponseTo") + "=" +
                    authnResponse.getInResponseTo()};
            LogUtil.access(
                Level.INFO,LogUtil.SENT_AUTHN_RESPONSE,data,ssoToken);

            return;
        } catch(Exception ex){
            FSUtils.debug.error ("FSSSOBrowserPostProfileHandler:sendAuthn" +
                "Response:",  ex);
            return;
        }
    }
    
    protected boolean doSingleSignOn(
        Object ssoToken,
        String inResponseTo,
        NameIdentifier spHandle,
        NameIdentifier idpHandle)
    {
        FSUtils.debug.message(
            "FSSSOBrowserPostProfileHandler.doSingleSignOn: Called");
        this.ssoToken = ssoToken;
        FSAuthnResponse authnResponse = createAuthnResponse(
            ssoToken, inResponseTo, spHandle, idpHandle);
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSSOBrowserPostProfileHandler.doSingleSignOn: "
                    + "AuthnResponse created: " + authnResponse.toXMLString ());
            }
        } catch(FSException ex){
            FSUtils.debug.error(
                "FSSSOBrowserPostProfileHandler.doSingleSignOn: "
                + "Created AuthnResponse is not valid: ", ex);
            return false;
        }
        if (authnResponse == null){
            FSUtils.debug.error(
                "FSSSOBrowserPostProfileHandler.doSingleSignOn: "
                + "No valid AuthnResponse could be created. "
                + "Sending error AuthnResponse");
            return false;
        }        
        sendAuthnResponse (authnResponse);
        return true;
    }
}
