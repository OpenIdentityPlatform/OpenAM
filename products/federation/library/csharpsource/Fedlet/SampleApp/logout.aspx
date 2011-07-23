<%--
/*
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
 * $Id: logout.aspx,v 1.2 2010/01/26 01:20:14 ggennaro Exp $
 */
--%>
<%@ Page Language="C#" Debug="true" %>
<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Net" %>
<%@ Import Namespace="System.Xml" %>
<%@ Import Namespace="Sun.Identity.Saml2" %>
<%@ Import Namespace="Sun.Identity.Saml2.Exceptions" %>
<%
    /*
     * Receives the SAMLResponse for Logout from the Identity Provider or
     * receives the SAMLRequest and sends the SAMLResponse from the 
     * Fedlet to the Identity Provider. If no query parameter is specified,
     * a SOAP message will be assumed.
     * 
     * Following are the list of supported query parameters:
     * 
     * Query Parameter    Description
     * ---------------    -----------
     * SAMLRequest        The SAML request for logout sent from the Identity
     *                    Provider.
     *                    
     * -- or --
     * 
     * SAMLResponse       The SAML response for logout sent from the Identity
     *                    Provider.
     *                     
     */

    ServiceProviderUtility serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
    if (serviceProviderUtility == null)
    {
        serviceProviderUtility = new ServiceProviderUtility(Context);
        Cache["spu"] = serviceProviderUtility;
    }

    NameValueCollection parameters = Saml2Utils.GetRequestParameters(Request);
    string samlRequest = parameters[Saml2Constants.RequestParameter];
    string samlResponse = parameters[Saml2Constants.ResponseParameter];
    
    try
    {
        // Perform action based on what was received...
        if (!String.IsNullOrEmpty(samlResponse))
        {
            // process the logout response from SP initiated SLO
            LogoutResponse logoutResponse = serviceProviderUtility.GetLogoutResponse(Context);

            // do local app specific post-logout behavior

            // redirect to either the relay state or the fedlet's default url
            if (!string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                string redirectUrl = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(redirectUrl, serviceProviderUtility.ServiceProvider.RelayStateUrlList);
                Response.Redirect(redirectUrl);
            }
            else
            {
                string fedletUrl = Request.Url.AbsoluteUri.Substring(0, Request.Url.AbsoluteUri.LastIndexOf("/") + 1);
                Response.Redirect(fedletUrl);
            }
        }
        else if (!String.IsNullOrEmpty(samlRequest))
        {
            // obtain the logout request from IDP initiated SLO
            LogoutRequest logoutRequest = serviceProviderUtility.GetLogoutRequest(Context);

            // do local app specific logout

            // send the logout response
            serviceProviderUtility.SendLogoutResponse(Context, logoutRequest);
        }
        else
        {
            // obtain logout soap request
            LogoutRequest logoutRequest = serviceProviderUtility.GetLogoutRequest(Context);
            
            // do local app specific logout
            
            // respond with the soap logout response
            serviceProviderUtility.SendSoapLogoutResponse(Context, logoutRequest);
        }

    }
    catch (Saml2Exception se)
    {
        Response.StatusCode = 400;
        Response.StatusDescription = se.Message;
        Response.End();
    }
    catch (ServiceProviderUtilityException spue)
    {
        Response.StatusCode = 400;
        Response.StatusDescription = spue.Message;
        Response.End();
    }
    
%>