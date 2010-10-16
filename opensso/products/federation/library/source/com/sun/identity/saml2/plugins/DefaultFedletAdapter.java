/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultFedletAdapter.java,v 1.2 2009/06/17 03:09:13 exu Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.LogoutResponse;
import com.sun.identity.shared.encode.URLEncDec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
 * The <code>DefaultFedletAdapter</code> class provides default implementation
 * for <code>FedletAdapter</code>.
 */

public class DefaultFedletAdapter extends FedletAdapter {

    private String logoutUrl = null;

    /**
     * Initializes the fedlet adapter, this method will only be executed
     * once after creation of the adapter instance.
     * @param initParams  initial set of parameters configured in the fedlet
     *          for this adapter. One of the parameters named
     *          <code>HOSTED_ENTITY_ID</code> refers to the ID of this
     *          fedlet entity.
     */
    public void initialize(Map initParams) {
    }

    /**
     * Invokes after Fedlet receives SLO request from IDP. It does the work
     * of logout the user.
     * @param request servlet request
     * @param response servlet response
     * @param hostedEntityID entity ID for the fedlet
     * @param idpEntityID entity id for the IDP to which the request is
     *          received from.
     * @param siList List of SessionIndex whose session to be logged out
     * @param nameIDValue nameID value whose session to be logged out
     * @param binding Single Logout binding used,
     *      one of following values:
     *          <code>SAML2Constants.SOAP</code>,
     *          <code>SAML2Constants.HTTP_POST</code>,
     *          <code>SAML2Constants.HTTP_REDIRECT</code>
     * @return <code>true</code> if user is logged out successfully;
     *          <code>false</code> otherwise.
     * @exception SAML2Exception if user want to fail the process.
     */
    public boolean doFedletSLO (
        HttpServletRequest request, 
        HttpServletResponse response, 
        LogoutRequest logoutReq,
        String hostedEntityID, 
        String idpEntityID,
        List siList,
        String nameIDValue,
        String binding)
    throws SAML2Exception {
        boolean status = true;
        String method = "DefaultFedletAdapter:doFedletSLO:";
        try {
            if (logoutUrl == null) {
                BaseConfigType spConfig = SAML2Utils.getSAML2MetaManager()
                    .getSPSSOConfig("/", hostedEntityID);
                List appLogoutURL = (List) SAML2MetaUtils.getAttributes(
                    spConfig).get(SAML2Constants.APP_LOGOUT_URL);
                if ((appLogoutURL != null) && !appLogoutURL.isEmpty()) {
                    logoutUrl = (String) appLogoutURL.get(0);
                }
            }
            if (logoutUrl == null) {
                String deployuri = request.getRequestURI();
                int slashLoc = deployuri.indexOf("/", 1);
                if (slashLoc != -1) {
                    deployuri = deployuri.substring(0, slashLoc);
                }
                if (deployuri != null) {
                    String url = request.getRequestURL().toString();
                    int loc = url.indexOf(deployuri + "/");
                    if (loc != -1) {
                        logoutUrl = url.substring(0, loc + deployuri.length()) +
                            "/logout";
                    }
                } 
            }
            if (logoutUrl == null) {
                return status;
            }
            URL url = new URL(logoutUrl);
            HttpURLConnection conn =
                HttpURLConnectionManager.getConnection(url);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);

            // replay cookies
            String strCookies = SAML2Utils.getCookiesString(request);
            if (strCookies != null) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(method + "Sending cookies : " + 
                        strCookies);
                }
                conn.setRequestProperty("Cookie", strCookies);
            }
            conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

            conn.setRequestProperty("IDP", URLEncDec.encode(idpEntityID));
            conn.setRequestProperty("SP", URLEncDec.encode(hostedEntityID)); 
            conn.setRequestProperty(
                "NameIDValue", URLEncDec.encode(nameIDValue));
            if (siList != null && !siList.isEmpty()) {
                Iterator iter = siList.iterator();
                StringBuffer siValue = new StringBuffer();
                siValue.append((String)iter.next());
                while (iter.hasNext()) {
                    siValue.append(",").append((String)iter.next());
                }
                conn.setRequestProperty(
                    "SessionIndex", URLEncDec.encode(siValue.toString()));
            }
            conn.setRequestProperty("Binding", binding);

            OutputStream outputStream = conn.getOutputStream();
            // Write the request to the HTTP server.
            outputStream.write("".getBytes());
            outputStream.flush();
            outputStream.close();

            // Check response code
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(method + "Response code OK");
                }
                status = true;
            } else {
                SAML2Utils.debug.error(method + "Response code NOT OK: "
                    + conn.getResponseCode());
               status = false;
            }
        } catch (Exception e) {
            status = false;
        }
        return status;
    }

    /**
     * Invokes after Fedlet receives SLO response from IDP and the SLO status
     * is success.
     * @param request servlet request
     * @param response servlet response
     * @param logoutReq SAML2 <code>LogoutRequest</code> object
     * @param logoutRes SAML2 <code>LogoutResponse</code> object
     * @param hostedEntityID entity ID for the fedlet
     * @param idpEntityID entity id for the IDP to which the logout response
     *          is received from.
     * @param binding Single Logout binding used,
     *      one of following values:
     *          <code>SAML2Constants.SOAP</code>,
     *          <code>SAML2Constants.HTTP_POST</code>,
     *          <code>SAML2Constants.HTTP_REDIRECT</code>
     * @exception SAML2Exception if user want to fail the process.
     */
    public void onFedletSLOSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        LogoutRequest logoutReq,
        LogoutResponse logoutRes,
        String hostedEntityID,
        String idpEntityID,
        String binding)
    throws SAML2Exception {
        onFedletSLOSuccessOrFailure(request, response, logoutReq,
            logoutRes, hostedEntityID, idpEntityID, binding, true);
        return;
    }

    /**
     * Invokes after Fedlet receives SLO response from IDP and the SLO status
     * is not success.
     * @param request servlet request
     * @param response servlet response
     * @param logoutReq SAML2 <code>LogoutRequest</code> object
     * @param logoutRes SAML2 <code>LogoutResponse</code> object
     * @param hostedEntityID entity ID for the fedlet
     * @param idpEntityID entity id for the IDP to which the logout response
     *          is received from.
     * @param binding Single Logout binding used,
     *      one of following values:
     *          <code>SAML2Constants.SOAP</code>,
     *          <code>SAML2Constants.HTTP_POST</code>,
     *          <code>SAML2Constants.HTTP_REDIRECT</code>
     * @exception SAML2Exception if user want to fail the process.
     */
    public void onFedletSLOFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        LogoutRequest logoutReq,
        LogoutResponse logoutRes,
        String hostedEntityID,
        String idpEntityID,
        String binding)
    throws SAML2Exception {
        onFedletSLOSuccessOrFailure(request, response, logoutReq,
            logoutRes, hostedEntityID, idpEntityID, binding, false);
        return;
    }

    private void onFedletSLOSuccessOrFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        LogoutRequest logoutReq,
        LogoutResponse logoutRes,
        String hostedEntityID,
        String idpEntityID,
        String binding,
        boolean isSuccess)
    throws SAML2Exception {
        String method = "DefaultFedletAdapter:onFedletSLOSuccessOrFailure:";
        try {
            if (logoutUrl == null) {
                BaseConfigType spConfig = SAML2Utils.getSAML2MetaManager()
                    .getSPSSOConfig("/", hostedEntityID);
                List appLogoutURL = (List) SAML2MetaUtils.getAttributes(
                    spConfig).get(SAML2Constants.APP_LOGOUT_URL);
                if ((appLogoutURL != null) && !appLogoutURL.isEmpty()) {
                    logoutUrl = (String) appLogoutURL.get(0);
                }
            }
            if (logoutUrl == null) {
                String deployuri = request.getRequestURI();
                int slashLoc = deployuri.indexOf("/", 1);
                if (slashLoc != -1) {
                    deployuri = deployuri.substring(0, slashLoc);
                }
                if (deployuri != null) {
                    String url = request.getRequestURL().toString();
                    int loc = url.indexOf(deployuri + "/");
                    if (loc != -1) {
                        logoutUrl = url.substring(0, loc + deployuri.length()) +
                            "/logout";
                    }
                } 
            }
            if (logoutUrl == null) {
                return;
            }
            URL url = new URL(logoutUrl);
            HttpURLConnection conn =
                HttpURLConnectionManager.getConnection(url);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);

            // replay cookies
            String strCookies = SAML2Utils.getCookiesString(request);
            if (strCookies != null) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(method + "Sending cookies : " + 
                        strCookies);
                }
                conn.setRequestProperty("Cookie", strCookies);
            }
            conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

            conn.setRequestProperty("IDP", URLEncDec.encode(idpEntityID));
            conn.setRequestProperty("SP", URLEncDec.encode(hostedEntityID)); 
            if (logoutReq != null) {
                NameID nameID = logoutReq.getNameID();
                if (nameID != null) {
                    conn.setRequestProperty(
                        "NameIDValue", URLEncDec.encode(nameID.getValue()));
                }
                List siList = logoutReq.getSessionIndex(); 
                if ((siList != null) && (!siList.isEmpty())) {
                    conn.setRequestProperty("SessionIndex", 
                        URLEncDec.encode((String)siList.get(0)));
                }
            }
            conn.setRequestProperty("Binding", binding);
            if (isSuccess) {
                conn.setRequestProperty("SLOStatus", "Success");
            } else {
                conn.setRequestProperty("SLOStatus", "Failure");
            }

            OutputStream outputStream = conn.getOutputStream();
            // Write the request to the HTTP server.
            outputStream.write("".getBytes());
            outputStream.flush();
            outputStream.close();
            // Check response code
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(method + "Response code OK");
                }
            } else {
                SAML2Utils.debug.error(method + "Response code NOT OK: "
                    + conn.getResponseCode());
            }
        } catch (Exception e) {
        }
        return;
    }
} 
