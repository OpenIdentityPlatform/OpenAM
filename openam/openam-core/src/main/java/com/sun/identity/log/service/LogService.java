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
 * $Id: LogService.java,v 1.5 2009/12/15 18:00:14 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.log.service;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import com.iplanet.dpro.parser.WebtopParser;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingHdlrEntryImpl;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;
import com.sun.identity.session.util.RestrictedTokenHelper;
import com.sun.identity.session.util.SessionUtils;
import java.util.List;

/**
 * This class parses the xml/http(s) log requests and executes the corresponding
 * log operation.
 */
public class LogService implements RequestHandler {
    /**
     * Sting variable to keep logID by sessionID
     */
    static private String loggedBySid;
    /**
     * The package which contains the LogService and other service classes.
     */
    private static final String pkg = "com.sun.identity.log.service.";
    /**
     * The string that should be used to create the response set.
     */
    private static final String LOG_SERVICE = "iplanet.webtop.service.logging";
    /**
     * The SAX parser instance
     */
    WebtopParser parser = new WebtopParser();
    /**
     * Registers the classes with the SAX parser
     * @throws Exception
     */
    public LogService() throws Exception {
        parser = new WebtopParser();
        parser.register(LogXMLStrings.RECWRITE, pkg+"LogRecWrite");
        parser.register(LogXMLStrings.LOG, pkg+"Log");
        parser.register(LogXMLStrings.LOGRECORD, pkg+"LogRecord");
        parser.register(LogXMLStrings.LEVEL,pkg+"Level");
        parser.register(LogXMLStrings.MSG,pkg+"RecMsg");
        parser.register(LogXMLStrings.RECTYPE,pkg+"RecType");
        parser.register(LogXMLStrings.LOGTYPE,pkg+"LogType");
        parser.register(LogXMLStrings.LOGINFOMAP,pkg+"LogInfoMap");
        parser.register(LogXMLStrings.LOGINFO,pkg+"LogInfo");
        parser.register(LogXMLStrings.INFOKEY,pkg+"InfoKey");
        parser.register(LogXMLStrings.INFOVALUE,pkg+"InfoValue");
        parser.register(LogXMLStrings.PARAMETERS,pkg+"Parameters");
        parser.register(LogXMLStrings.PARAMETER,pkg+"Parameter");
        parser.register(LogXMLStrings.PARAMINDEX,pkg+"ParamIndex");
        parser.register(LogXMLStrings.PARAMVALUE,pkg+"ParamValue");
    }
    /**
     * The method which accepts the request set, parses the xml request and
     * executes the appropriate log operation.
     * @param requests
     * @param servletRequest
     * @param servletResponse
     * @return The response set which contains the result of the log operation.
     */
    public ResponseSet process(List<Request> requests,
    HttpServletRequest servletRequest,
    HttpServletResponse servletResponse,
    ServletContext servletContext) {
        if (Debug.messageEnabled()) {
            Debug.message("LogService.process() called :requests are");
            
            for (Request req : requests) {
                Debug.message("xml = " + req.getContent());
            }
        }
        
        ResponseSet rset = new ResponseSet(LOG_SERVICE);
        for (Request req : requests) {
            // remember sid string is the last item in the log tag
            String xmlRequestString = req.getContent();
            Response res;
            if ((xmlRequestString==null) || xmlRequestString.equals("null")) {
                Debug.error("Received a null log request");
                res = new Response("NULL_LOG_REQUEST");
                rset.addResponse(res);
            }
            else {
                int l = xmlRequestString.length();
                int sidi = xmlRequestString.indexOf("sid=");
                int sidj = xmlRequestString.indexOf("</log");
                loggedBySid = xmlRequestString.substring((sidi+5), (sidj-2));
                try {

                    //NOTE source ip address restrictions are temporary kludge
                    // for 6.1 session hijacking hotpatch

                    InetAddress remoteClient =
                        SessionUtils.getClientAddress(servletRequest);
                        
                    SSOToken ssoToken =
                        RestrictedTokenHelper.resolveRestrictedToken(
                                                     loggedBySid,remoteClient);

                    SSOTokenManager ssom = SSOTokenManager.getInstance();
                    if (!ssom.isValidToken(ssoToken)) {
                        String loggedByID = ssoToken.getPrincipal().getName();
                        Debug.error("LogService::process(): access denied for" +
                            " user :" + loggedByID);
                        res = new Response("UNAUTHORIZED");
                        rset.addResponse(res);
                        return rset;
                    }
                } catch (SSOException e) {
                    Debug.error("LogService::process(): SSOException", e);
                    res = new Response("UNAUTHORIZED");
                    rset.addResponse(res);
                    return rset;
                } catch (Exception e)  {
                    Debug.error("LogService::process(): ", e);
                    res = new Response("ERROR");
                    rset.addResponse(res);
                }
                try {
                    ByteArrayInputStream bin = new ByteArrayInputStream(
                    xmlRequestString.getBytes("UTF-8"));
                    LogOperation op = (LogOperation) parser.parse(bin);
                    res = op.execute();
                } catch(Exception e) {
                        Debug.error("LogService::process():",e);
                    // FORMAT ERROR RESPONSE HERE
                    res = new Response("ERROR");
                    if (MonitoringUtil.isRunning()) {
                        SsoServerLoggingSvcImpl slsi =
                            Agent.getLoggingSvcMBean();
                        SsoServerLoggingHdlrEntryImpl slei =
                            slsi.getHandler(
                                SsoServerLoggingSvcImpl.REMOTE_HANDLER_NAME);
                        slei.incHandlerFailureCount(1);
                    }
                }
                rset.addResponse(res);
            }
        }
        return rset;
    }
}
