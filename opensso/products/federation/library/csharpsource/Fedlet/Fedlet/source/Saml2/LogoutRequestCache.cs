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
 * $Id: LogoutRequestCache.cs,v 1.1 2009/11/11 18:13:39 ggennaro Exp $
 */

using System.Collections;
using System.Text;
using System.Web;
using Sun.Identity.Common;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// <para>
    /// Class managing the last X LogoutRequests associated with the
    /// user's session.  The collection of LogoutRequests are managed within
    /// a Queue added to the user's session to facilitate FIFO and allow
    /// for the ServiceProviderUtility to correctly perform validation
    /// on the LogoutRequests containing a InResponseTo attribute.
    /// </para>
    /// <para>
    /// See the MaximumRequestsStored variable for the value of X.
    /// </para>
    /// </summary>
    public static class LogoutRequestCache
    {
        #region Members
        /// <summary>
        /// Name of session attribute for tracking user's LogoutRequests.
        /// </summary>
        private const string LogoutRequestSessionAttribute = "_logoutRequests";

        /// <summary>
        /// Constant to define the maximum number of LogoutRequests to be
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
        /// sent LogoutRequests. This collection is represented as a queue and 
        /// is attached to the user's session.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <returns>
        /// Queue of previously sent LogoutRequests, null otherwise.
        /// </returns>
        internal static Queue GetSentLogoutRequests(HttpContext context)
        {
            return (Queue)context.Session[LogoutRequestCache.LogoutRequestSessionAttribute];
        }

        /// <summary>
        /// Adds the specified LogoutRequest to the collection of previously 
        /// sent requests, maintaining the imposed limit as defined by 
        /// MaximumRequestsStored.  This collection is represented as a
        /// queue and is attached to the user's session.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="logoutRequest">
        /// LogoutRequest to add to the collection.
        /// </param>
        internal static void AddSentLogoutRequest(HttpContext context, LogoutRequest logoutRequest)
        {
            Queue logoutRequests = LogoutRequestCache.GetSentLogoutRequests(context);

            if (logoutRequests == null)
            {
                logoutRequests = new Queue(LogoutRequestCache.MaximumRequestsStored);
            }

            if (logoutRequests.Count == LogoutRequestCache.MaximumRequestsStored)
            {
                logoutRequests.Dequeue();
            }

            logoutRequests.Enqueue(logoutRequest);
            context.Session[LogoutRequestCache.LogoutRequestSessionAttribute] = logoutRequests;

            StringBuilder message = new StringBuilder();
            message.Append("LogoutRequestCache:\r\n");
            IEnumerator i = logoutRequests.GetEnumerator();
            while (i.MoveNext())
            {
                LogoutRequest l = (LogoutRequest)i.Current;
                message.Append(l.Id + "\r\n");
            }

            FedletLogger.Info(message.ToString());
        }

        /// <summary>
        /// Removes the LogoutRequest from the collection of previously 
        /// sent requests based on the provided LogoutRequest.Id value.
        /// This collection is represented as a queue and is attached to 
        /// the user's session.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="logoutRequestId">
        /// ID of the LogoutRequest to be removed from the cache.
        /// </param>
        internal static void RemoveSentLogoutRequest(HttpContext context, string logoutRequestId)
        {
            Queue originalCache = LogoutRequestCache.GetSentLogoutRequests(context);

            if (originalCache != null)
            {
                Queue revisedCache = new Queue();
                while (originalCache.Count > 0)
                {
                    LogoutRequest temp = (LogoutRequest)originalCache.Dequeue();
                    if (temp.Id != logoutRequestId)
                    {
                        revisedCache.Enqueue(temp);
                    }
                }

                context.Session[LogoutRequestCache.LogoutRequestSessionAttribute] = revisedCache;
            }
        }
        #endregion
    }
}
