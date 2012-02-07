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
 * $Id: AuthnRequestCache.cs,v 1.1 2009/06/11 18:37:58 ggennaro Exp $
 */

using System.Collections;
using System.Text;
using System.Web;
using Sun.Identity.Common;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// <para>
    /// Class managing the last X AuthnRequests associated with the
    /// user's session.  The collection of AuthnRequests are managed within
    /// a Queue added to the user's session to facilitate FIFO and allow
    /// for the ServiceProviderUtility to correctly perform validation
    /// on the AuthnResponse containing a InResponseTo attribute.
    /// </para>
    /// <para>
    /// See the MaximumRequestsStored variable for the value of X.
    /// </para>
    /// </summary>
    public static class AuthnRequestCache
    {
        #region Members
        /// <summary>
        /// Name of session attribute for tracking user's AuthnRequests.
        /// </summary>
        private const string AuthnRequestSessionAttribute = "_authnRequests";

        /// <summary>
        /// Constant to define the maximum number of AuthnRequests to be
        /// stored in the user's session.
        /// </summary>
        private const int MaximumRequestsStored = 5;
        #endregion

        #region Constructor
        #endregion

        #region Properties
        #endregion

        #region Methods
        /// <summary>
        /// Retrieves the queue containing the collection of stored previously
        /// sent AuthnRequests. This collection is represented as a queue and 
        /// is attached to the user's session.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <returns>Queue of previously sent AuthnRequests, null otherwise.</returns>
        internal static Queue GetSentAuthnRequests(HttpContext context)
        {
            return (Queue)context.Session[AuthnRequestCache.AuthnRequestSessionAttribute];
        }

        /// <summary>
        /// Adds the specified AuthnRequest to the collection of previously 
        /// sent requests, maintaining the imposed limit as defined by 
        /// MaximumRequestsStored.  This collection is represented as a
        /// queue and is attached to the user's session.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="authnRequest">AuthnRequest to add to the collection.</param>
        internal static void AddSentAuthnRequest(HttpContext context, AuthnRequest authnRequest)
        {
            Queue authnRequests = AuthnRequestCache.GetSentAuthnRequests(context);

            if (authnRequests == null)
            {
                authnRequests = new Queue(AuthnRequestCache.MaximumRequestsStored);
            }

            if (authnRequests.Count == AuthnRequestCache.MaximumRequestsStored)
            {
                authnRequests.Dequeue();
            }

            authnRequests.Enqueue(authnRequest);
            context.Session[AuthnRequestCache.AuthnRequestSessionAttribute] = authnRequests;

            StringBuilder message = new StringBuilder();
            message.Append("AuthnRequestsCache:\r\n");
            IEnumerator i = authnRequests.GetEnumerator();
            while (i.MoveNext())
            {
                AuthnRequest a = (AuthnRequest)i.Current;
                message.Append(a.Id + "\r\n");
            }

            FedletLogger.Info(message.ToString());
        }

        /// <summary>
        /// Removes the AuthnRequest from the collection of previously 
        /// sent requests based on the provided AuthnRequest.Id value.
        /// This collection is represented as a queue and is attached to 
        /// the user's session.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="authnRequestId">
        /// ID of the AuthnRequest to be removed from the cache.
        /// </param>
        internal static void RemoveSentAuthnRequest(HttpContext context, string authnRequestId)
        {
            Queue originalCache = AuthnRequestCache.GetSentAuthnRequests(context);

            if (originalCache != null)
            {
                Queue revisedCache = new Queue();
                while (originalCache.Count > 0)
                {
                    AuthnRequest temp = (AuthnRequest)originalCache.Dequeue();
                    if (temp.Id != authnRequestId)
                    {
                        revisedCache.Enqueue(temp);
                    }
                }

                context.Session[AuthnRequestCache.AuthnRequestSessionAttribute] = revisedCache;
            }
        }
        #endregion
    }
}
