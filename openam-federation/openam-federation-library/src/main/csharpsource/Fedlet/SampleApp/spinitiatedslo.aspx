<%--
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2009-2010 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: spinitiatedslo.aspx,v 1.3 2010/01/26 01:20:14 ggennaro Exp $
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
     * Following are the list of supported query parameters:
     * 
     * Query Parameter    Description
     * ---------------    -----------
     * Binding            URI value that identifies a SAML protocol binding 
     *                    to used when returning the Response message. The 
     *                    supported values are:
     *                     urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST (default)
     *                     urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect
     *                     urn:oasis:names:tc:SAML:2.0:bindings:SOAP
     *                    
     * Destination        A URI Reference indicating the address to which the
     *                    request has been sent.
     *                     
     * idpEntityID        Identifier for Identity Provider to issue the logout
     *                    request.
     *                    
     * SubjectNameId      Identifier for the name id value specified in the initial
     *                    AuthnResponse.
     *                    
     * SessionIndex       Identifier for the session index specified in the initial
     *                    AuthnResponse.
     *                    
     * RelayState         Destination URL to redirect the browser after successful
     *                    logout.
     *                    
     */

    ServiceProviderUtility serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
    if (serviceProviderUtility == null)
    {
        serviceProviderUtility = new ServiceProviderUtility(Context);
        Cache["spu"] = serviceProviderUtility;
    }

    // Store parameters for initializing SLO
    NameValueCollection parameters = Saml2Utils.GetRequestParameters(Request);
    string idpEntityId = parameters[Saml2Constants.IdpEntityId];

    if (String.IsNullOrEmpty(parameters[Saml2Constants.Binding]))
    {
        // If the binding is null, use HttpRedirect.
        parameters[Saml2Constants.Binding] = Saml2Constants.HttpRedirectProtocolBinding;
    }
    
    if (String.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
    {
        // If the relay state is null, use the fedlet's default page.
        string fedletUrl = Request.Url.AbsoluteUri.Substring(0, Request.Url.AbsoluteUri.LastIndexOf("/"));
        parameters[Saml2Constants.RelayState] = fedletUrl;
    }

    try
    {
        // Check for required parameters...
        if (String.IsNullOrEmpty(idpEntityId))
        {
            throw new ServiceProviderUtilityException("IDP Entity ID not specified.");
        }
        else if (String.IsNullOrEmpty(parameters[Saml2Constants.SubjectNameId]))
        {
            throw new ServiceProviderUtilityException("SubjectNameId not specified.");
        }
        else if (String.IsNullOrEmpty(parameters[Saml2Constants.SessionIndex]))
        {
            throw new ServiceProviderUtilityException("SessionIndex not specified.");
        }

        // Perform SP initiated SSO
        serviceProviderUtility.SendLogoutRequest(Context, idpEntityId, parameters);
        
        // If SOAP was the binding and no exception thrown, redirect to the relay state
        if (parameters[Saml2Constants.Binding] == Saml2Constants.HttpSoapProtocolBinding)
        {
            string relayState = parameters[Saml2Constants.RelayState];
            Saml2Utils.ValidateRelayState(relayState, serviceProviderUtility.ServiceProvider.RelayStateUrlList);
            Response.Redirect(relayState);
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