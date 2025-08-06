/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PLLRequestServlet.java,v 1.9 2009/02/12 17:24:13 bina Exp $
 *
 * Portions Copyrighted 2012-2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.iplanet.services.comm.server;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.share.PLLBundle;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.service.NamingService;
import com.sun.identity.shared.Constants;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * The <code>PLLRequestServlet</code> class is used to receive requests from
 * applications and forward those requests to the corresponding servers for
 * processing.
 * </p>
 * The doPost() method gets the XML RequestSet document from the HttpRequest
 * object, then parses the XML documnent and reconstructs a RequestSet object.
 * 
 * @see com.iplanet.services.comm.share.Request
 * @see com.iplanet.services.comm.share.RequestSet
 */

public class PLLRequestServlet extends HttpServlet {

    /* service handlers */
    private static Hashtable requestHandlers = new Hashtable();

    private static final String PROPERTY_MAX_CONTENT_LENGTH = 
        Constants.SERVICES_COMM_SERVER_PLLREQUEST_MAX_CONTENT_LENGTH;

    /* the default content length is set to 16k */
    private static int maxContentLength = 16384;

    private static final String AUTH_SVC_ID = "Auth";

    public void init() throws ServletException {
        String maxContentLengthProp = SystemProperties.get(
                PROPERTY_MAX_CONTENT_LENGTH, String.valueOf(maxContentLength));
        try {
            maxContentLength = Integer.parseInt(maxContentLengthProp);
        } catch (NumberFormatException e) {
            if (PLLServer.pllDebug.messageEnabled()) {
                PLLServer.pllDebug.message("Invalid value ["
                        + maxContentLengthProp + "] for property"
                        + PROPERTY_MAX_CONTENT_LENGTH);
            }
        }
    }

    /*
     * doPost() Accepts POST requests, reads Inpt Stream, forwards the
     * RequestSet XML Flushes the ResponseSet XML to OutputStream @param
     * HttpServletRequest Reference to HttpServletRequest object @param
     * HttpServletResponse Reference to HttpServletResponse object
     * 
     * @see javax.servlet.http.HttpServlet
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, java.io.IOException {

        PLLAuditor auditor = newAuditor(req);

        try {

            int length = req.getContentLength();
            if (length == -1) {
                PLLServer.pllDebug.warning(PLLBundle.getString("unknownLength"));
                throw servletException("unknownLength");
            }

            if (length > maxContentLength) {
                PLLServer.pllDebug.error("content length exceeded configured max request size - " + length);
                throw servletException("largeContentLength");
            }

            byte[] reqData = new byte[length];
            InputStream in = req.getInputStream();
            int rlength = 0;
            int offset = 0;
            while (rlength != length) {
                int r = in.read(reqData, offset, length - offset);
                if (r == -1) {
                    throw servletException("readRequestError");
                }
                rlength += r;
                offset += r;
            }
            String xml = new String(reqData, 0, length, "UTF-8");

            RequestSet set = RequestSet.parseXML(xml);
            String svcid = set.getServiceID();
            if(!AUTH_SVC_ID.equalsIgnoreCase(svcid)) {
                if (PLLServer.pllDebug.messageEnabled()) {
                    PLLServer.pllDebug.message("\nReceived RequestSet XML :\n" + xml);
                }
            }

            String responseXML = handleRequest(auditor, set, req, res);
            res.setContentLength(responseXML.getBytes("UTF-8").length);
            OutputStreamWriter out = new OutputStreamWriter(res.getOutputStream(),
                    "UTF-8");
            try {
                out.write(responseXML);
                out.flush();
            } catch (IOException e) {
                throw e;
            } finally {
                try {
                    out.close();
                } catch (Exception ex) {
                }
            }

        } catch (IOException | ServletException | RuntimeException e) {
            auditor.auditAccessFailure(e.getMessage());
            throw e;
        }

    }

    private PLLAuditor newAuditor(HttpServletRequest httpServletRequest) {
        return new PLLAuditor(
                PLLServer.pllDebug,
                InjectorHolder.getInstance(AuditEventPublisher.class),
                InjectorHolder.getInstance(AuditEventFactory.class),
                httpServletRequest);
    }

    private ServletException servletException(String errorId) {
        return new ServletException(PLLBundle.getString(errorId));
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, java.io.IOException {
        ServletOutputStream out = res.getOutputStream();
        out.print("OpenSSO");
        out.flush();
    }

    /*
     * handleRequest() used by doPost method. Gets the corresponding service
     * handler and obtains ResponseSet from it.
     * 
     * @param String XML RequestSet String - Conforming to RequestSet.dtd @param
     * req HttpServletRequest object @param res HttpServletResponse object
     * @return String XML ResponseSet String - Conforming to ResponseSet.dtd
     * 
     * @see sunir.share.profile.service.server.http.RequestProcessor
     */
    private String handleRequest(PLLAuditor auditor, RequestSet set, HttpServletRequest req, HttpServletResponse res)
            throws ServletException {
        if (!isValid(set)) {
            throw servletException("invalidRequestSet");
        }
        String svcid = set.getServiceID();
        RequestHandler handler = getServiceHandler(svcid);
        if (handler == null) {
            throw servletException("noRequestHandler");
        }
        ResponseSet rset = handler.process(auditor, set.getRequests(), req, res, getServletConfig().getServletContext());
        rset.setRequestSetID(set.getRequestSetID());
        return rset.toXMLString();
    }

    /*
     * Check to see whether the Parser returned a valid RequestSet.
     */
    private boolean isValid(RequestSet set) {
        if (set == null || set.getRequestSetVersion() == null
                || set.getServiceID() == null || set.getRequestSetID() == null
                || set.getRequests().size() < 1) {
            return false;
        }
        return true;
    }

    /*
     * Return the service handler
     */
    private RequestHandler getServiceHandler(String svcid) {
        RequestHandler handler = (RequestHandler) requestHandlers.get(svcid);
        if (handler == null) {
            try {
                if (svcid.equals(WebtopNaming.NAMING_SERVICE))
                    handler = new NamingService();
                else {
                    String svcclass = WebtopNaming.getServiceClass(svcid);
                    if (svcclass != null) {
                        Class<? extends RequestHandler> cl = Class
                                .forName(svcclass)
                                .asSubclass(RequestHandler.class);
                        handler = InjectorHolder.getInstance(cl);
                    } else if (PLLServer.pllDebug.messageEnabled()) {
                        PLLServer.pllDebug.message("Service handler for :"
                                + svcid + " not found");
                    }
                }
                if (handler != null) {
                    requestHandlers.put(svcid, handler);
                }
            } catch (Exception e) {
                PLLServer.pllDebug.message("Cannot get service handler for "
                        + svcid + " :", e);
                return null;
            }
        }
        return handler;
    }
}
