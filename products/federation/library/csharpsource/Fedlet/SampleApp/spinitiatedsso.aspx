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
 * $Id: spinitiatedsso.aspx,v 1.2 2010/01/26 01:20:14 ggennaro Exp $
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
     * AllowCreate        Value indicates if IDP is allowed to created a new 
     *                    identifier for the principal if it does not exist.
     *                    Value of this parameter can be true OR false. 
     *                    True - IDP can dynamically create user.
     *                    
     * AssertionConsumerServiceIndex  
     *                    An integer number indicating the location to which 
     *                    the Response message should be returned to the 
     *                    requester.
     * 
     * AuthComparison     The comparison method used to evaluate the requested
     *                    context classes or statements. Allowed values are:
     *                     exact
     *                     minimum
     *                     maximum
     *                     better
     *
     * AuthLevel          The Authentication Level of the Authentication 
     *                    Context to use for Authentication.
     *                    
     * AuthnContextClassRef 
     *                    Specifies the AuthnContext Class References. The 
     *                    value is a pipe separated value with multiple 
     *                    references.
     * 
     * AuthnContextDeclRef  
     *                    Specifies the AuthnContext Declaration Reference. 
     *                    The value is a pipe separated value with multiple 
     *                    references.
     *                    
     * Binding            URI value that identifies a SAML protocol binding 
     *                    to used when returning the Response message. The 
     *                    supported values are:
     *                     HTTP-Artifact
     *                     HTTP-POST (default)
     *                     
     * Consent            Specifies a URI a SAML defined identifier known as 
     *                    Consent Identifiers. These are defined in the SAML2
     *                    Assertions and Protocols Document.
     *                    
     * Destination        A URI Reference indicating the address to which the
     *                    request has been sent.
     *                    
     * ForceAuthN         True or false value indicating if IDP must force 
     *                    authentication OR false if IDP can rely on reusing
     *                    existing security contexts. Default is false.
     *                    True - force authentication
     *                    
     * idpEntityID        Identifier for Identity Provider. If unspecified, 
     *                    first available remote IDP is used.
     *                    
     * IsPassive          True or false value indicating whether the IDP 
     *                    should authenticate passively.  Default is false.
     *                    
     * RelayState         Destination URL to redirect the browser after successful
     *                    login.
     *
     * ReqBinding         URI value that identifies a SAML protocol binding to
     *                    used when sending the AuthnRequest. The supported 
     *                    values are:
     *                     urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect
     *                     urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST
     *                    HTTP-Redirect is the default.
     *                    
     */

    ServiceProviderUtility serviceProviderUtility = (ServiceProviderUtility)Cache["spu"];
    if (serviceProviderUtility == null)
    {
        serviceProviderUtility = new ServiceProviderUtility(Context);
        Cache["spu"] = serviceProviderUtility;
    }

    // Store parameters for initializing SSO
    NameValueCollection parameters = Saml2Utils.GetRequestParameters(Request);
    string idpEntityId = parameters["idpEntityId"];

    // If the IDP entity ID not specified, discover it.
    if (String.IsNullOrEmpty(idpEntityId))
    {
        // Determine if the IDP has already been discovered...
        idpEntityId = IdentityProviderDiscoveryUtils.GetPreferredIdentityProvider(Request);

        if (idpEntityId == null)
        {
            // Discover the IDP by redirecting to the reader service.
            IdentityProviderDiscoveryUtils.StoreRequestParameters(Context);

            Uri readerServiceUrl = IdentityProviderDiscoveryUtils.GetReaderServiceUrl(serviceProviderUtility, Context);

            if (readerServiceUrl != null)
            {
                IdentityProviderDiscoveryUtils.RedirectToReaderService(readerServiceUrl, Context);
                return;
            }
        }

        // Retrieve all previously stored parameters and reset the discovery
        // process if we've exhausted all reader services...
        parameters = IdentityProviderDiscoveryUtils.RetrieveRequestParameters(Context);
        IdentityProviderDiscoveryUtils.ResetDiscovery(Context);
    }
    
    // If the IDP entity ID is still null, use the first one configured
    if (idpEntityId == null)
    {
        IEnumerator idps = serviceProviderUtility.IdentityProviders.Keys.GetEnumerator();
        if (idps.MoveNext())
        {
            idpEntityId = (string) idps.Current;
        }
    }

    // If the binding is null, use POST.
    if (String.IsNullOrEmpty(parameters[Saml2Constants.Binding]))
    {
        parameters[Saml2Constants.Binding] = Saml2Constants.HttpPostProtocolBinding;
    }

    try
    {
        // Check for required parameters...
        if (idpEntityId == null)
        {
            throw new ServiceProviderUtilityException("IDP Entity ID not specified nor discovered.");
        }
        
        // Perform SP initiated SSO
        serviceProviderUtility.SendAuthnRequest(Context, idpEntityId, parameters);
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