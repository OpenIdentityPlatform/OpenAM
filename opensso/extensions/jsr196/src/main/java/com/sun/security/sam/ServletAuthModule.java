/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: ServletAuthModule.java,v 1.2 2008/12/08 17:52:31 rsoika Exp $
 */
package com.sun.security.sam;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.security.auth.Subject;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author monzillo
 */
public abstract class ServletAuthModule implements ServerAuthModule {

    protected static final String ASSIGN_GROUPS_OPTIONS_KEY = "assign.groups";
    protected static final String POLICY_CONTEXT_OPTIONS_KEY =
            "javax.security.jacc.PolicyContext";
    protected static final String DEBUG_OPTIONS_KEY = "debug";
    public static final String AUTH_TYPE_INFO_KEY = "javax.servlet.http.authType";
    public static final String IS_MANDATORY_INFO_KEY =
            "javax.security.auth.message.MessagePolicy.isMandatory";
    protected static final String AUTHORIZATION_HEADER = "authorization";
    protected static final String AUTHENTICATION_HEADER = "WWW-Authenticate";
    protected static final String SAVED_REQUEST_ATTRIBUTE =
            "javax.security.auth.message.SavedHttpRequest";
    protected static final String SAVED_PRINCIPAL_ATTRIBUTE =
            "javax.security.auth.message.SavedPrincipals";
    protected static final Class[] supportedMessageTypes = new Class[]{
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class
    };
    protected final Logger logger =
            Logger.getLogger(ServletAuthModule.class.getName());
    protected MessagePolicy requestPolicy;
    protected MessagePolicy responsePolicy;
    protected CallbackHandler handler;
    protected Map options;
    protected boolean debug;
    protected Level debugLevel;
    protected String policyContextID;
    protected String[] assignedGroups;
    protected boolean isMandatory;

    /**
     * Initialize this module with request and response message policies
     * to enforce, a CallbackHandler, and any module-specific configuration
     * properties.
     *
     * <p> The request policy and the response policy must not both be null.
     *
     * @param requestPolicy The request policy this module must enforce,
     *		or null.
     *
     * @param responsePolicy The response policy this module must enforce,
     *		or null.
     *
     * @param handler CallbackHandler used to request information.
     *
     * @param options A Map of module-specific configuration properties.
     *
     * @exception AuthException If module initialization fails, including for
     * the case where the options argument contains elements that are not 
     * supported by the module.
     */
    public void initialize(MessagePolicy requestPolicy,
            MessagePolicy responsePolicy,
            CallbackHandler handler,
            Map options) throws AuthException {

        this.requestPolicy = requestPolicy;
        this.responsePolicy = responsePolicy;

        this.isMandatory = requestPolicy.isMandatory();

        this.handler = handler;
        this.options = options;

        if (options != null) {
            debug = (options.containsKey(DEBUG_OPTIONS_KEY));
            if (debug)
                for (String key : (Set<String>) options.keySet()) {
                    logger.info("key" + key + " value " + options.get(key));
                }
            policyContextID = (String) options.get(POLICY_CONTEXT_OPTIONS_KEY);
        } else {
            debug = false;
            policyContextID = null;
        }
        if (debug)
            logger.info("jmac.debug_is_set_to " + debug); 
        
        assignedGroups = parseAssignGroupsOption(options);

        debugLevel = (logger.isLoggable(Level.FINE) && !debug) ? Level.FINE : Level.INFO;
    }

    /**
     * Get the one or more Class objects representing the message types 
     * supported by the module.
     *
     * @return An array of Class objects, with at least one element 
     * defining a message type supported by the module.
     */
    public Class[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    /**
     * Secure a service response before sending it to the client.
     *
     * This method is called to transform the response message acquired by
     * calling getResponseMessage (on messageInfo) into the mechanism-specific
     * form to be sent by the runtime.
     * <p> This method conveys the outcome of its message processing either
     * by returning an AuthStatus value or by throwing an AuthException.
     *
     * @param messageInfo A contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param serviceSubject A Subject that represents the source of the 
     *          service
     *          response, or null. It may be used by the method implementation
     *          to retrieve Principals and credentials necessary to secure 
     *          the response. If the Subject is not null, 
     *          the method implementation may add additional Principals or 
     *          credentials (pertaining to the source of the service 
     *          response) to the Subject.
     *
     * @return An AuthStatus object representing the completion status of
     *          the processing performed by the method. 
     *          The AuthStatus values that may be returned by this method 
     *          are defined as follows:
     *
     * <ul>
     * <li> AuthStatus.SEND_SUCCESS when the application response 
     * message was successfully secured. The secured response message may be
     * obtained by calling getResponseMessage on messageInfo.
     *
     * <li> AuthStatus.SEND_CONTINUE to indicate that the application response 
     * message (within messageInfo) was replaced with a security message 
     * that should elicit a security-specific response (in the form of a 
     * request) from the peer.
     *
     * This status value serves to inform the calling runtime that
     * (to successfully complete the message exchange) it will
     * need to be capable of continuing the message dialog by processing
     * at least one additional request/response exchange (after having
     * sent the response message returned in messageInfo).
     *
     * When this status value is returned, the application response must 
     * be saved by the authentication module such that it can be recovered
     * when the module's validateRequest message is called to process
     * the elicited response.
     *
     * <li> AuthStatus.SEND_FAILURE to indicate that a failure occurred while
     * securing the response message and that an appropriate failure response
     * message is available by calling getResponseMeessage on messageInfo.
     * </ul>
     *
     * @exception AuthException When the message processing failed without
     *          establishing a failure response message (in messageInfo).
     */
    public AuthStatus secureResponse(MessageInfo messageInfo,
            Subject serviceSubject) throws AuthException {

        boolean wrapped = false;
        HttpServletRequest r = (HttpServletRequest) messageInfo.getRequestMessage();
        while (r != null && r instanceof HttpServletRequestWrapper) {
            r = (HttpServletRequest) ((HttpServletRequestWrapper) r).getRequest();
            wrapped = true;
        }
        if (wrapped) {
            messageInfo.setRequestMessage(r);
        }
        wrapped = false;
        HttpServletResponse s = (HttpServletResponse) messageInfo.getResponseMessage();
        while (s != null && s instanceof HttpServletResponseWrapper) {
            s = (HttpServletResponse) ((HttpServletResponseWrapper) s).getResponse();
            wrapped = true;
        }
        if (wrapped) {
            messageInfo.setResponseMessage(s);
        }

        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Remove method specific principals and credentials from the subject.
     *
     * @param messageInfo a contextual object that encapsulates the
     *          client request and server response objects, and that may be 
     *          used to save state across a sequence of calls made to the 
     *          methods of this interface for the purpose of completing a 
     *          secure message exchange.
     *
     * @param subject the Subject instance from which the Principals and 
     *          credentials are to be removed.
     *
     * @exception AuthException If an error occurs during the Subject 
     *          processing.
     */
    public void cleanSubject(MessageInfo messageInfo, Subject subject)
            throws AuthException {
    }

    protected AuthStatus sendFailureMessage(HttpServletResponse response,
            int status, String message) {
        try {
            response.setStatus(status);
            response.sendError(status, message);
        } catch (Throwable t) {
            // status code has been set, and proper AuthStatus will be returned
            logger.log(Level.WARNING, "jmac.servlet_failed_sending_failure", t);
        } finally {
            return AuthStatus.SEND_FAILURE;
        }
    }

    protected AuthStatus respondWithRedirect(HttpServletResponse response,
            String url) throws AuthException {
        try {
            response.sendRedirect(response.encodeRedirectURL(url));
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "jmac.servlet_redirect_failed", ioe);
            AuthException ae = new AuthException();
            ae.initCause(ioe);
            throw ae;
        }
        return AuthStatus.SEND_CONTINUE;
    }

    protected boolean addGroups(String[] groupNames, Subject clientSubject) {

        if (groupNames != null && groupNames.length > 0) {

            GroupPrincipalCallback gPCB =
                    new GroupPrincipalCallback(clientSubject, groupNames);
            try {
                handler.handle(new Callback[]{gPCB});
                return true;
            } catch (Exception e) {
                // should not happen
                logger.log(Level.WARNING, "jmac.failed_to_assign_groups", e);
            }
        }
        return false;
    }

    protected boolean setCallerPrincipal(CallerPrincipalCallback cPCB) {

        boolean rvalue = true;
        boolean assignGroups = true;
        if (cPCB.getName() == null && cPCB.getPrincipal() == null) {
            assignGroups = false;
        }

        try {
            
            handler.handle((assignGroups ? 
                new Callback[]{
                    cPCB, 
                    new GroupPrincipalCallback(
                        cPCB.getSubject(),assignedGroups)} : 
                new Callback[]{
                    cPCB}));
            
            if (debug || logger.isLoggable(Level.FINE)) {
                logger.log(debugLevel, "jmac.caller_principal",
                        new Object[]{cPCB.getName(), cPCB.getPrincipal()});
            }

        } catch (Exception e) {
            // should not happen
            logger.log(Level.WARNING, "jmac.failed_to_set_caller", e);
            rvalue = false;
        }

        return rvalue;
    }

    protected boolean setCallerPrincipal(String caller, Subject clientSubject) {
        return setCallerPrincipal(new CallerPrincipalCallback(clientSubject, caller));
    }

    protected boolean setCallerPrincipal(Subject clientSubject, Principal caller) {
        return setCallerPrincipal(new CallerPrincipalCallback(clientSubject, caller));
    }

    protected void debugToken(String message, byte[] bytes) {

        if (debug || logger.isLoggable(Level.FINE)) {

            StringBuffer sb = new StringBuffer();
            sb.append("\n");
            sb.append("bytes: ");
            boolean first = true;
            for (byte b : bytes) {
                int i = b;
                if (first) {
                    sb.append(i);
                    first = false;
                } else {
                    sb.append(", " + i);
                }
            }

            logger.log(debugLevel, message, sb);
        }
    }

    protected void debugRequest(HttpServletRequest request) {

        if (debug || logger.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer();
            sb.append("\n");
            try {
                sb.append("Request: " + request.getRequestURL() + "\n");
                sb.append("UserPrincipal: " + request.getUserPrincipal() + "\n");
                sb.append("AuthType: " + request.getAuthType() + "\n");
                sb.append("Headers:" + "\n");
                Enumeration names = request.getHeaderNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    sb.append("\t" + name + "\t" + request.getHeader(name) + "\n");
                }

                logger.log(debugLevel, "jmac.servlet_request", sb);

            } catch (Throwable t) {
                logger.log(Level.WARNING, "jmac.servlet_debug_request", t);
            }
        }
    }

    protected String[] parseAssignGroupsOption(Map options) {
        String[] groups = new String[0];
        if (options != null) {
            String groupList = (String) options.get(ASSIGN_GROUPS_OPTIONS_KEY);
            if (groupList != null) {
                StringTokenizer tokenizer =
                        new StringTokenizer(groupList, " ,:,;");
                Set<String> groupSet = null;
                while (tokenizer.hasMoreTokens()) {
                    if (groupSet == null) {
                        groupSet = new HashSet<String>();
                    }
                    groupSet.add(tokenizer.nextToken());
                }
                if (groupSet != null && !groupSet.isEmpty()) {
                    groups = groupSet.toArray(groups);
                }
            }
        }
        return groups;
    }

    protected static void saveRequest(HttpServletRequest request)
            throws AuthException {

        SavedHttpServletRequest saved = null;

        try {
            saved = new SavedHttpServletRequest(request);
        } catch (Exception e) {
            String msg = "jmac.Save_http_servlet_request_failure";
            AuthException ae = new AuthException(msg);
            ae.initCause(e);
            throw ae;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SAVED_REQUEST_ATTRIBUTE, saved);

    }

    protected static HttpServletRequestWrapper restoreRequest(HttpServletRequest request, boolean matchURL) {

        HttpServletRequestWrapper found = null;
        HttpSession session = request.getSession(false);

        if (session != null) {
            found = (HttpServletRequestWrapper) session.getAttribute(SAVED_REQUEST_ATTRIBUTE);
        }

        if (found != null && (!matchURL ||
                request.getRequestURL().toString().
                equals(found.getRequestURL().toString()))) {
            found.setRequest(request);
            session.removeAttribute(SAVED_REQUEST_ATTRIBUTE);
            return found;
        }

        return null;
    }

    protected static String getSavedRequestURI(HttpServletRequest request) {

        HttpServletRequestWrapper found = null;
        HttpSession session = request.getSession(false);

        if (session != null) {
            found = (HttpServletRequestWrapper) session.getAttribute(SAVED_REQUEST_ATTRIBUTE);
        }

        if (found != null) {
            return found.getRequestURL().toString();
        }

        return null;
    }

    protected static void savePrincipals(HttpServletRequest request, Subject s) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SAVED_PRINCIPAL_ATTRIBUTE, s.getPrincipals());
    }

    protected static boolean restorePrincipals(HttpServletRequest request, Subject s) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Set<Principal> principals =
                    (Set<Principal>) session.getAttribute(SAVED_PRINCIPAL_ATTRIBUTE);
            if (principals != null) {
                session.removeAttribute(SAVED_PRINCIPAL_ATTRIBUTE);
                s.getPrincipals().addAll(principals);
                return true;
            }
        }
        return false;
    }
}
