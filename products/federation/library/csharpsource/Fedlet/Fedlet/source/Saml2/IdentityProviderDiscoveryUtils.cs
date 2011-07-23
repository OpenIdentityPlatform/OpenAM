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
 * $Id: IdentityProviderDiscoveryUtils.cs,v 1.2 2009/06/11 18:37:58 ggennaro Exp $
 */

using System;
using System.Collections;
using System.Collections.Specialized;
using System.Text;
using System.Web;
using System.Web.SessionState;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Utilities to assist with the Identity Provider Discovery Profile
    /// within SAMLv2.
    /// </summary>
    public static class IdentityProviderDiscoveryUtils
    {
        #region Members

        /// <summary>
        /// Constant for the name of the common domain cookie.
        /// </summary>
        public const string CommonDomainCookieName = "_saml_idp";

        /// <summary>
        /// Name of the session attribute used for IDP discovery.
        /// </summary>
        private const string CommonDomainDiscoverySessionAttribute = "_cotList";

        /// <summary>
        /// Name of the session attribute used for storing and recovering
        /// the original parameters specified prior to IDP discovery.
        /// </summary>
        private const string OriginalParametersSessionAttribute = "_paramMap";
        
        #endregion

        #region Methods

        /// <summary>
        /// Gets the preferred identity provider entity id based on the value
        /// found in the query string found in the given HttpRequest.
        /// </summary>
        /// <param name="request">HttpRequest containing Common Domain Cookie results.</param>
        /// <returns>Preferred IDP Entity ID, null if not available.</returns>
        public static string GetPreferredIdentityProvider(HttpRequest request)
        {
            string commonDomainCookieValue = request.QueryString[IdentityProviderDiscoveryUtils.CommonDomainCookieName];
            return IdentityProviderDiscoveryUtils.GetPreferredIdentityProvider(commonDomainCookieValue);
        }

        /// <summary>
        /// Gets the preferred identity provider entity id based on the value
        /// found in the specified string.
        /// </summary>
        /// <param name="commonDomainCookieValue">Common Domain Cookie value.</param>
        /// <returns>Preferred IDP Entity ID, null if not available.</returns>
        public static string GetPreferredIdentityProvider(string commonDomainCookieValue)
        {
            string idpEntityId = null;

            if (!String.IsNullOrEmpty(commonDomainCookieValue))
            {
                char[] separator = { ' ' };
                string[] listOfIdpEntityIds = commonDomainCookieValue.Split(separator);

                if (listOfIdpEntityIds.Length > 0)
                {
                    idpEntityId = Saml2Utils.ConvertFromBase64(listOfIdpEntityIds[listOfIdpEntityIds.Length - 1]);
                }
            }

            return idpEntityId;
        }

        /// <summary>
        /// Obtains the next reader service during the discovery process
        /// being managed by a session variable tracking cirlce of trusts
        /// currently being checked.
        /// </summary>
        /// <param name="serviceProviderUtility">ServiceProviderUtility containing circle-of-trust information.</param>
        /// <param name="context">HttpContext containing session, request, and response objects.</param>
        /// <returns>
        /// Returns the URL found in the currently checked circle-of-trust file if specified, null otherwise.
        /// </returns>
        public static Uri GetReaderServiceUrl(ServiceProviderUtility serviceProviderUtility, HttpContext context)
        {
            HttpSessionState session = context.Session;
            Uri readerSvcUrl = null;

            ArrayList cotList = (ArrayList)session[IdentityProviderDiscoveryUtils.CommonDomainDiscoverySessionAttribute];
            if (cotList == null)
            {
                // Obtain the list of currently tracked circle-of-trusts with 
                // reader service if not already known.
                cotList = new ArrayList();
                foreach (string cotName in serviceProviderUtility.CircleOfTrusts.Keys)
                {
                    CircleOfTrust cot = (CircleOfTrust)serviceProviderUtility.CircleOfTrusts[cotName];
                    if (cot.ReaderServiceUrl != null)
                    {
                        cotList.Add(cotName);
                    }
                }
            }

            IEnumerator enumerator = cotList.GetEnumerator();
            if (enumerator.MoveNext())
            {
                // Try the first service in the list
                string cotName = (string)enumerator.Current;
                cotList.Remove(cotName);
                session[IdentityProviderDiscoveryUtils.CommonDomainDiscoverySessionAttribute] = cotList;
                CircleOfTrust cot = (CircleOfTrust)serviceProviderUtility.CircleOfTrusts[cotName];
                readerSvcUrl = new Uri(cot.ReaderServiceUrl.AbsoluteUri);
            }

            return readerSvcUrl;
        }

        /// <summary>
        /// Issues a browser redirect to the specified reader service.
        /// </summary>
        /// <param name="readerServiceUrl">Location of the reader service to send redirect.</param>
        /// <param name="context">HttpContext containing session, request, and response objects.</param>
        public static void RedirectToReaderService(Uri readerServiceUrl, HttpContext context)
        {
            HttpRequest request = context.Request;
            HttpResponse response = context.Response;

            // Set the RelayState for the reader service to the requestede without
            // the query information already saved to the session.
            string relayStateForReaderSvc = request.Url.AbsoluteUri;
            if (!String.IsNullOrEmpty(request.Url.Query))
            {
                relayStateForReaderSvc = relayStateForReaderSvc.Replace(request.Url.Query, string.Empty);
            }

            // Redirect to the service and terminate the calling response.
            StringBuilder redirectUrl = new StringBuilder();
            redirectUrl.Append(readerServiceUrl);
            redirectUrl.Append("?");
            redirectUrl.Append("RelayState=");
            redirectUrl.Append(relayStateForReaderSvc);

            response.Redirect(redirectUrl.ToString(), true);
        }

        /// <summary>
        /// Resets all session variables used during IDP discovery.
        /// </summary>
        /// <param name="context">HttpContext containing session, request, and response objects.</param>
        public static void ResetDiscovery(HttpContext context)
        {
            HttpSessionState session = context.Session;

            session[IdentityProviderDiscoveryUtils.CommonDomainDiscoverySessionAttribute] = null;
            session[IdentityProviderDiscoveryUtils.OriginalParametersSessionAttribute] = null;
        }

        /// <summary>
        /// Stores the querystring parameters found in the request for
        /// later use in the discovery process.
        /// </summary>
        /// <param name="context">HttpContext containing session, request, and response objects.</param>
        /// <returns>
        /// Returns the NameValueCollection containing the parameters stored
        /// into the session from the last invocation of the method 
        /// StoreRequestParameters.
        /// </returns>
        public static NameValueCollection RetrieveRequestParameters(HttpContext context)
        {
            HttpSessionState session = context.Session;
            return (NameValueCollection)session[IdentityProviderDiscoveryUtils.OriginalParametersSessionAttribute];
        }

        /// <summary>
        /// Stores the querystring parameters found in the request for
        /// later use in the discovery process.
        /// </summary>
        /// <param name="context">HttpContext containing session, request, and response objects.</param>
        public static void StoreRequestParameters(HttpContext context)
        {
            HttpSessionState session = context.Session;
            HttpRequest request = context.Request;
            NameValueCollection parameters = (NameValueCollection)session[IdentityProviderDiscoveryUtils.OriginalParametersSessionAttribute];

            if (parameters == null)
            {
                parameters = new NameValueCollection();
            }

            foreach (string name in request.QueryString.Keys)
            {
                parameters[name] = request.QueryString[name];
            }

            session[IdentityProviderDiscoveryUtils.OriginalParametersSessionAttribute] = parameters;
        }

        #endregion
    }
}
