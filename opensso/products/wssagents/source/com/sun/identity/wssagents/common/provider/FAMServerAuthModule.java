/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMServerAuthModule.java,v 1.9 2009/05/05 01:16:12 mallas Exp $
 *
 */

package com.sun.identity.wssagents.common.provider;

import java.util.Map;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.soap.SOAPMessage;
import javax.security.auth.message.module.ServerAuthModule;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.security.trust.WSTrustConstants;
import com.sun.xml.ws.security.trust.WSTrustVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.wss.impl.ProcessingContextImpl;
import com.sun.xml.ws.security.IssuedTokenContext;

/**
 * The <code>FAMServerAuthModule</code> class implements an interface
 * <code>ServerAuthModule</code> defined by the JSR196 and will be invoked
 * by the JSR196 for Validating webservice requests and Securing the
 * webservice responses.
 */

public class FAMServerAuthModule implements ServerAuthModule {
    // Pointer to SOAPRequestHandler class
    private static Class _handler;
    // Methods in SOAPRequestHandler class
    private static Method init;
    private static Method validateRequest;
    private static Method print;
    private static Method secureResponse;
    private static final Logger logger =
                   Logger.getLogger("com.sun.identity.wssagents.security");
    
    // Instance of SOAPRequestHandler
    private Object serverAuthModule;
    private static ClassLoader cls;
    private static final String  WS_TRUST_13_ACTION =
            "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue";
    
    /**
     * Initializes the module using the configuration defined through
     * deployment descriptors.
     * @param requestPolicy
     * @param responsePolicy
     * @param handler a
     *     <code>javax.security.auth.callback.CallbackHandler</code>
     * @param options
     * @exception AuthException for any failures.
     */
    
    public void initialize(MessagePolicy requestPolicy,
	       MessagePolicy responsePolicy,
	       CallbackHandler handler,
	       Map options) throws AuthException {
        
        if(logger.isLoggable(Level.FINE)) {
           logger.log(Level.FINE, "FAMServerAuthModule.Init");
        }
        
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        try {
            if (_handler == null) {
                try {
                    oldcc.loadClass(
                       "com.sun.identity.classloader.FAMClassLoader");
                    // Get the OpenSSO Classloader
                    cls = 
                        com.sun.identity.classloader.FAMClassLoader.
                            getFAMClassLoader(null,jars);
                } catch (ClassNotFoundException cnfe) {
                    if(logger.isLoggable(Level.FINE)) {
                       logger.log(Level.FINE, "FAMServerAuthModule : " +
                        "ClassNotFoundException, will load " + 
                            "wssagents.classloader.FAMClassLoader");
                    }
                    cls = 
                        com.sun.identity.wssagents.classloader.FAMClassLoader.
                            getFAMClassLoader(jars);
                } catch (java.lang.NoClassDefFoundError ncdfe) {
                   if(logger.isLoggable(Level.FINE)) {
                       logger.log(Level.FINE, "FAMServerAuthModule : " +
                        "ClassNotFoundException, will load " + 
                            "wssagents.classloader.FAMClassLoader");
                    }
                    cls = 
                        com.sun.identity.wssagents.classloader.FAMClassLoader.
                            getFAMClassLoader(jars);
                }
                
                Thread.currentThread().setContextClassLoader(cls);
                
                // Obtain in the instance of class
                _handler = cls.loadClass(
                    "com.sun.identity.wss.security.handler.SOAPRequestHandler");
                // Get the methods
                Class clsa[] = new Class[1];
                clsa[0] = Class.forName("java.util.Map");
                init = _handler.getDeclaredMethod("init", clsa);
                clsa[0] = Class.forName("org.w3c.dom.Node");
                print = _handler.getDeclaredMethod("print", clsa);
                clsa = new Class[5];
                clsa[0] = Class.forName("javax.xml.soap.SOAPMessage", true, cls);
                clsa[1] = Class.forName("javax.security.auth.Subject");
                clsa[2] = Class.forName("java.util.Map");
                clsa[3] = 
                    Class.forName("javax.servlet.http.HttpServletRequest");
                clsa[4] = 
                    Class.forName("javax.servlet.http.HttpServletResponse");
                validateRequest = _handler.getDeclaredMethod(
                    "validateRequest", clsa);
                clsa = new Class[2];
                clsa[0] = Class.forName("javax.xml.soap.SOAPMessage", true, cls);
                clsa[1] = Class.forName("java.util.Map");
                secureResponse = _handler.getDeclaredMethod(
                    "secureResponse", clsa);
            }
            // Construct an instance of SOAPRequestHandler & initialize
            serverAuthModule = _handler.newInstance();
            Object args[] = new Object[1];
            args[0] = options;
            init.invoke(serverAuthModule, args);
        } catch(Exception ex) {
            if(logger.isLoggable(Level.SEVERE)) {
               logger.log(Level.SEVERE,
                "FAMServerAuthModule.initialization failed : ",  ex);
            }
            throw new RuntimeException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
    }
    
    /**
     * Validates the SOAP Request based on the configuration
     * enforcement on the webservice endpoint and exposes the principal
     * and credentials for the application.
     *
     * @param messageInfo that has a inbound <code>SOAPMessage</code>
     * @param clientSubject Client J2EE <code>Subject</code>
     * @param serviceSubject Service J2EE <code>Subject</code>
     * @return AuthStatus if successful in validating the request.
     * @exception AuthException if there is an error occured in validating
     *            the request.
     */
    public AuthStatus validateRequest(MessageInfo messageInfo,
			       Subject clientSubject,
			       Subject serviceSubject) throws AuthException {        
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cls);
            
            Packet packet = (Packet)messageInfo.getMap().get("REQ_PACKET");
            boolean isTrustMsg = false;
            HeaderList hl = packet.getMessage().getHeaders();
            String action = 
                hl.getAction(AddressingVersion.W3C, SOAPVersion.SOAP_12);            
            if(action == null) {
               action =  hl.getAction(AddressingVersion.W3C, 
                       SOAPVersion.SOAP_11);
            }
            if ((WSTrustConstants.REQUEST_SECURITY_TOKEN_ISSUE_ACTION).equals(
                action) || (WS_TRUST_13_ACTION.equals(action))) {
                isTrustMsg = true;
                //set the IS_TRUST_MSG into MessageInfo
                messageInfo.getMap().put("IS_TRUST_MSG", 
                    Boolean.valueOf(isTrustMsg));
                hl.getTo(AddressingVersion.W3C, SOAPVersion.SOAP_12);
                
                if(!WS_TRUST_13_ACTION.equals(action)) {
                   packet.invocationProperties.put(WSTrustConstants.WST_VERSION, 
                    WSTrustVersion.WS_TRUST_10);
                } else {
                   packet.invocationProperties.put(WSTrustConstants.WST_VERSION, 
                    WSTrustVersion.WS_TRUST_13); 
                }
                ProcessingContextImpl ctx = 
                    new ProcessingContextImpl(packet.invocationProperties);
                ctx.isTrustMessage(true);
                IssuedTokenContext ictx = 
                    ((ProcessingContextImpl)ctx).getTrustContext();
                if(ictx != null && ictx.getAuthnContextClass() != null){                    
                    packet.invocationProperties.put(
                        WSTrustConstants.AUTHN_CONTEXT_CLASS, 
                        ictx.getAuthnContextClass());
                }                
            }
            messageInfo.getMap().put("VALIDATE_REQ_PACKET", packet);
            
            SOAPMessage soapMessage = 
                (SOAPMessage)messageInfo.getRequestMessage();
            
            Object args[] = new Object[1];
            if(logger != null) {
                args[0] = soapMessage.getSOAPPart().getEnvelope();
                logger.log(Level.FINE, "FAMServerAuthModule.validateRequest: "
                    + "SOAPMessage before validation: " + print.invoke(
                    serverAuthModule, args));
            }
            args = new Object[5];
            args[0] = soapMessage;
            args[1] = clientSubject;
            args[2] = messageInfo.getMap();
            clientSubject = 
                (Subject) validateRequest.invoke(serverAuthModule, args);

            packet.invocationProperties.put("javax.security.auth.Subject", 
                clientSubject);
            
            if(logger.isLoggable(Level.FINE)) {
                args = new Object[1];
                args[0] = soapMessage.getSOAPPart().getEnvelope();
                logger.log(Level.FINE, "FAMServerAuthModule.validateRequest: "
                    + "SOAPMessage after validation: " + print.invoke(
                    serverAuthModule, args));
            }
            
        } catch(Exception sbe) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING,"FAMServerAuthModule.validateRequest:"
                            + "Failed in Validating the Request.", sbe);
            }
            AuthException ae = new AuthException(sbe.getCause().getMessage());
            ae.initCause(sbe.getCause());
            throw ae;
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
        
        return AuthStatus.SUCCESS;
    }
    
    /**
     * Secures the response sending by the application by reading
     * the configuration from the application deployment descriptors.
     *
     * @param messageInfo that has a <code>SOAPMessage</code>.
     * @param serviceSubject Service J2EE <code>Subject</code> that will have 
     *        authenticated principal
     * @return AuthStatus if successful
     * @exception AuthException for any error while securing the response.
     */
    public AuthStatus secureResponse(MessageInfo messageInfo, 
        Subject serviceSubject) throws AuthException {
        ClassLoader oldcc = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cls);
            
            // Add addrsssing headers to trust message
            String iTM = (String)messageInfo.getMap().get("IS_TRUST_MESSAGE");
            boolean isTrustMessage = (iTM != null) ? true : false;
            
            Packet packet = 
                (Packet)messageInfo.getMap().get("VALIDATE_REQ_PACKET");
            Packet retPacket = (Packet)messageInfo.getMap().get("RES_PACKET");
            if (isTrustMessage){
                retPacket = 
                    addAddressingHeaders(packet, retPacket.getMessage(), 
                    (WSTrustVersion.WS_TRUST_10).getIssueFinalResoponseAction());
                ProcessingContextImpl ctx = 
                    new ProcessingContextImpl(retPacket.invocationProperties);
                ctx.isTrustMessage(true);
            }
            
            SOAPMessage soapMessage = 
                (SOAPMessage)messageInfo.getResponseMessage();

            Object[] args = new Object[1];
            args[0] = soapMessage.getSOAPPart().getEnvelope();
            if(logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, "FAMServerAuthModule.secureResponse: " 
                    + "SOAPMessage before securing: " + print.invoke(
                    serverAuthModule, args));
            }
            args = new Object[2];
            args[0] = soapMessage;
            args[1] = messageInfo.getMap();
            soapMessage = (SOAPMessage) secureResponse.invoke(
                serverAuthModule, args);
            if(logger.isLoggable(Level.FINE)) {
                args = new Object[1];
                args[0] = soapMessage.getSOAPPart().getEnvelope();
                logger.log(Level.FINE, "FAMServerAuthModule.secureResponse: " 
                    + "SOAPMessage after securing: " + print.invoke(
                    serverAuthModule, args));
            }
            
        } catch(Exception ie) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING, "FAMClientAuthModule.secureResponse:"
                    + " Failed in securing the response.", ie);
            }
            AuthException ae = new AuthException(ie.getCause().getMessage());
            ae.initCause(ie);
            throw ae;
        } finally {
            Thread.currentThread().setContextClassLoader(oldcc);
        }
        
        return AuthStatus.SUCCESS;
    }
    
    /**
     * Diposes the subject and security credentials.
     * @param messageInfo
     * @param subject
     * @exception AuthException if there is an error occured while disposing 
     *     the subject.
     */
    public void cleanSubject(MessageInfo messageInfo, Subject subject)
	throws AuthException {
        
        if(subject == null) {
            throw new AuthException("nullSubject");
        }
    }
           
    
     public Class[] getSupportedMessageTypes() {
         return null;
     }
     
    private Packet addAddressingHeaders(Packet packet, Message retMsg, 
        String action){
        Packet retPacket = 
            packet.createServerResponse(retMsg, AddressingVersion.W3C, 
            SOAPVersion.SOAP_12, action);
        
        retPacket.proxy = packet.proxy;
        retPacket.invocationProperties.putAll(packet.invocationProperties);
        
        return retPacket;
    }
    
    /**
     * The list of jar files to be loaded by FAMClassLoader.
     */
    public static String[] jars = new String[]{
        "webservices-rt.jar",
        "openssoclientsdk.jar",
        "xalan.jar",
        "xercesImpl.jar"
    };
}
