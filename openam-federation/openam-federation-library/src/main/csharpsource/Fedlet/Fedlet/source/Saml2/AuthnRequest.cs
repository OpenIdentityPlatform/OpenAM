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
 * $Id: AuthnRequest.cs,v 1.2 2010/01/19 18:23:09 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

using System;
using System.Collections;
using System.Collections.Specialized;
using System.Globalization;
using System.Text;
using System.Xml;
using System.Xml.XPath;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing the SAMLv2 AuthnRequest message for use in the
    /// SP initiated SSO profile.
    /// </summary>
    public class AuthnRequest
    {
        #region Members
        /// <summary>
        /// Namespace Manager for this authn request.
        /// </summary>
        private XmlNamespaceManager nsMgr;

        /// <summary>
        /// XML representation of the authn request.
        /// </summary>
        private XmlDocument xml;
        #endregion

        #region Constructor
        /// <summary>
        /// Initializes a new instance of the AuthnRequest class.
        /// </summary>
        /// <param name="identityProvider">
        /// IdentityProvider to receive the AuthnRequest
        /// </param>
        /// <param name="serviceProvider">
        /// ServiceProvider to issue the AuthnRequest
        /// </param>
        /// <param name="parameters">
        /// NameValueCollection of varying parameters for use in the 
        /// construction of the AuthnRequest.
        /// </param>
        public AuthnRequest(IdentityProvider identityProvider, ServiceProvider serviceProvider, NameValueCollection parameters)
        {
            this.xml = new XmlDocument();
            this.xml.PreserveWhitespace = true;

            this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
            this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
            this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

            this.Id = Saml2Utils.GenerateId();
            this.IssueInstant = Saml2Utils.GenerateIssueInstant();
            this.Issuer = serviceProvider.EntityId;

            if (parameters != null)
            {
                this.AllowCreate = Saml2Utils.GetBoolean(parameters[Saml2Constants.AllowCreate]);
                this.AssertionConsumerServiceIndex = parameters[Saml2Constants.AssertionConsumerServiceIndex];
                this.Binding = parameters[Saml2Constants.Binding];
                this.Consent = parameters[Saml2Constants.Consent];
                this.Destination = parameters[Saml2Constants.Destination];
                this.ForceAuthn = Saml2Utils.GetBoolean(parameters[Saml2Constants.ForceAuthn]);
                this.IsPassive = Saml2Utils.GetBoolean(parameters[Saml2Constants.IsPassive]);
            }

            string assertionConsumerSvcUrl = null;
            if (!String.IsNullOrEmpty(this.Binding))
            {
                if (!String.IsNullOrEmpty(this.AssertionConsumerServiceIndex))
                {
                    // find assertion consumer service location by binding and index.
                    assertionConsumerSvcUrl = serviceProvider.GetAssertionConsumerServiceLocation(this.Binding, this.AssertionConsumerServiceIndex);
                }
                else
                {
                    // find assertion consumer service location by binding only, using first found.
                    assertionConsumerSvcUrl = serviceProvider.GetAssertionConsumerServiceLocation(this.Binding);
                }
            }

            // neither index nor binding, throw exception
            if (String.IsNullOrEmpty(this.AssertionConsumerServiceIndex) && String.IsNullOrEmpty(assertionConsumerSvcUrl))
            {
                throw new Saml2Exception(Resources.AuthnRequestAssertionConsumerServiceNotDefined);
            }

            // If destination not specified, use SSO location by binding
            if (string.IsNullOrEmpty(this.Destination))
            {
                this.Destination 
                    = identityProvider.GetSingleSignOnServiceLocation(parameters[Saml2Constants.RequestBinding]);

                if (string.IsNullOrEmpty(this.Destination)) 
                {
                    // default to HttpRedirect
                    this.Destination = identityProvider.GetSingleSignOnServiceLocation(Saml2Constants.HttpRedirectProtocolBinding);
                }
            }

            // Get RequestedAuthnContext if parameters are available...
            RequestedAuthnContext reqAuthnContext = GetRequestedAuthnContext(serviceProvider, parameters);

            // Get Scoping if available...
            Scoping scoping = GetScoping(serviceProvider);

            // Generate the XML for the AuthnRequest...
            StringBuilder rawXml = new StringBuilder();
            rawXml.Append("<samlp:AuthnRequest");
            rawXml.Append(" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"");
            rawXml.Append(" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"");
            rawXml.Append(" ID=\"" + this.Id + "\"");
            rawXml.Append(" Version=\"2.0\"");
            rawXml.Append(" IssueInstant=\"" + this.IssueInstant + "\"");
            rawXml.Append(" IsPassive=\"" + (this.IsPassive ? "true" : "false") + "\"");
            rawXml.Append(" ForceAuthn=\"" + (this.ForceAuthn ? "true" : "false") + "\"");

            if (!String.IsNullOrEmpty(this.Consent))
            {
                rawXml.Append(" Consent=\"" + this.Consent + "\"");
            }

            if (!String.IsNullOrEmpty(this.Destination))
            {
                rawXml.Append(" Destination=\"" + this.Destination + "\"");
            }

            if (!String.IsNullOrEmpty(assertionConsumerSvcUrl))
            {
                rawXml.Append(" ProtocolBinding=\"" + this.Binding + "\"");
                rawXml.Append(" AssertionConsumerServiceURL=\"" + assertionConsumerSvcUrl + "\"");
            }
            else
            {
                rawXml.Append(" AssertionConsumerIndex=\"" + this.AssertionConsumerServiceIndex + "\"");
            }

            rawXml.Append(">");
            rawXml.Append("<saml:Issuer>" + serviceProvider.EntityId + "</saml:Issuer>");
            rawXml.Append("<samlp:NameIDPolicy AllowCreate=\"" + (this.AllowCreate ? "true" : "false") + "\" />");

            if (reqAuthnContext != null)
            {
                rawXml.Append(reqAuthnContext.GenerateXmlString());
            }

            if (scoping != null)
            {
                rawXml.Append(scoping.GenerateXmlString());
            }
                        
            rawXml.Append("</samlp:AuthnRequest>");

            this.xml.LoadXml(rawXml.ToString());
        }

        #endregion

        #region Properties

        /// <summary>
        /// Gets a value indicating whether AllowCreate is true or false.
        /// </summary>
        public bool AllowCreate { get; private set; }

        /// <summary>
        /// Gets the AssertionConsumerServiceIndex.
        /// </summary>
        public string AssertionConsumerServiceIndex { get; private set; }

        /// <summary>
        /// Gets the Binding.
        /// </summary>
        public string Binding { get; private set; }

        /// <summary>
        /// Gets the Consent.
        /// </summary>
        public string Consent { get; private set; }

        /// <summary>
        /// Gets the Destination.
        /// </summary>
        public string Destination { get; private set; }

        /// <summary>
        /// Gets a value indicating whether ForceAuthn is true or false.
        /// </summary>
        public bool ForceAuthn { get; private set; }

        /// <summary>
        /// Gets the ID.
        /// </summary>
        public string Id { get; private set; }

        /// <summary>
        /// Gets a value indicating whether IsPassive is true or false.
        /// </summary>
        public bool IsPassive { get; private set; }

        /// <summary>
        /// Gets the Issuer.
        /// </summary>
        public string Issuer { get; private set; }

        /// <summary>
        /// Gets the IssueInstant.
        /// </summary>
        public string IssueInstant { get; private set; }

        /// <summary>
        /// Gets the XML representation of the received authn response.
        /// </summary>
        public IXPathNavigable XmlDom
        {
            get
            {
                return this.xml;
            }
        }
        #endregion

        #region Methods
        /// <summary>
        /// Getst the RequestedAuthnContext element based on supplied 
        /// parameters for the given service provider.
        /// <seealso cref="Saml2Constants.AuthnContextClassRef"/>
        /// <seealso cref="Saml2Constants.AuthnContextDeclRef"/>
        /// <seealso cref="Saml2Constants.AuthLevel"/>
        /// </summary>
        /// <param name="serviceProvider">
        /// Service Provider generating the RequestedAuthnContext.
        /// </param>
        /// <param name="parameters">
        /// NameValueCollection containing necessary parameters for 
        /// constructing the RequetedAuthnContext.
        /// </param>
        /// <returns>RequestedAuthContext object or null if parameters are not present.</returns>
        private static RequestedAuthnContext GetRequestedAuthnContext(ServiceProvider serviceProvider, NameValueCollection parameters)
        {
            RequestedAuthnContext reqAuthnContext = null;

            if (!String.IsNullOrEmpty(parameters[Saml2Constants.AuthnContextClassRef])
                || !String.IsNullOrEmpty(parameters[Saml2Constants.AuthnContextDeclRef])
                || !String.IsNullOrEmpty(parameters[Saml2Constants.AuthLevel]))
            {
                reqAuthnContext = new RequestedAuthnContext();
                ArrayList classRefs = new ArrayList();
                ArrayList declRefs = new ArrayList();

                char[] separators = { '|' };
                if (!String.IsNullOrEmpty(parameters[Saml2Constants.AuthnContextClassRef]))
                {
                    classRefs.AddRange(parameters[Saml2Constants.AuthnContextClassRef].Split(separators));
                }

                if (!String.IsNullOrEmpty(parameters[Saml2Constants.AuthnContextDeclRef]))
                {
                    declRefs.AddRange(parameters[Saml2Constants.AuthnContextDeclRef].Split(separators));
                }

                if (!String.IsNullOrEmpty(parameters[Saml2Constants.AuthLevel]))
                {
                    int authLevel = Convert.ToInt32(parameters[Saml2Constants.AuthLevel], CultureInfo.InvariantCulture);
                    classRefs.Add(serviceProvider.GetAuthnContextClassRefFromAuthLevel(authLevel));
                }

                reqAuthnContext.SetAuthnContextClassRef(classRefs);
                reqAuthnContext.SetAuthnContextDeclRef(declRefs);

                if (!String.IsNullOrEmpty(parameters[Saml2Constants.AuthComparison]))
                {
                    reqAuthnContext.Comparison = parameters[Saml2Constants.AuthComparison];
                }
            }

            return reqAuthnContext;
        }

        private static Scoping GetScoping(ServiceProvider serviceProvider)
        {
            Scoping scoping = null;

            if (serviceProvider.ScopingProxyCount > 0)
            {
                scoping = new Scoping();
                ArrayList idpEntry = new ArrayList();
                idpEntry.AddRange(serviceProvider.ScopingIDPList);
                scoping.SetIDPEntry(idpEntry);
            }

            return scoping;
        }
        #endregion
    }
}
