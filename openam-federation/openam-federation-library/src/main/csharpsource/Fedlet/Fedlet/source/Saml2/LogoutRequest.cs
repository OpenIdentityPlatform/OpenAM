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
 * $Id: LogoutRequest.cs,v 1.2 2010/01/19 18:23:09 ggennaro Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

using System;
using System.Collections.Specialized;
using System.Globalization;
using System.Runtime.Serialization;
using System.Text;
using System.Xml;
using System.Xml.XPath;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// SAMLv2 LogoutRequest object constructed from either a response obtained
    /// from an Identity Provider for the hosted Service Provider or generated
    /// by this Service Provider to be sent to a desired Identity Provider.
    /// </summary>
    [Serializable]
    public class LogoutRequest : ISerializable
    {
        #region Members
        /// <summary>
        /// Namespace Manager for this logout request.
        /// </summary>
        private XmlNamespaceManager nsMgr;

        /// <summary>
        /// XML representation of the logout request.
        /// </summary>
        private XmlDocument xml;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the LogoutRequest class.
        /// </summary>
        /// <param name="samlRequest">Decoded SAMLv2 Logout Request</param>
        public LogoutRequest(string samlRequest)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;
                
                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

                this.xml.LoadXml(samlRequest);
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.LogoutRequestNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.LogoutRequestXmlException, xe);
            }
        }

        /// <summary>
        /// Initializes a new instance of the LogoutRequest class.
        /// </summary>
        /// <param name="identityProvider">
        /// IdentityProvider of the LogoutRequest
        /// </param>
        /// <param name="serviceProvider">
        /// ServiceProvider of the LogoutRequest
        /// </param>
        /// <param name="parameters">
        /// NameValueCollection of varying parameters for use in the 
        /// construction of the LogoutRequest.
        /// </param>
        public LogoutRequest(
            IdentityProvider identityProvider, 
            ServiceProvider serviceProvider, 
            NameValueCollection parameters)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;

                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

                string sessionIndex = null;
                string subjectNameId = null;
                string binding = null;
                string destination = null;

                if (parameters != null)
                {
                    sessionIndex = parameters[Saml2Constants.SessionIndex];
                    subjectNameId = parameters[Saml2Constants.SubjectNameId];
                    binding = parameters[Saml2Constants.Binding];
                    destination = parameters[Saml2Constants.Destination];
                }

                if (String.IsNullOrEmpty(sessionIndex))
                {
                    throw new Saml2Exception(Resources.LogoutRequestSessionIndexNotDefined);
                }
                else if (String.IsNullOrEmpty(subjectNameId))
                {
                    throw new Saml2Exception(Resources.LogoutRequestSubjectNameIdNotDefined);
                }
                else if (serviceProvider == null)
                {
                    throw new Saml2Exception(Resources.LogoutRequestServiceProviderIsNull);
                }
                else if (identityProvider == null)
                {
                    throw new Saml2Exception(Resources.LogoutRequestIdentityProviderIsNull);
                }

                if (string.IsNullOrEmpty(destination))
                {
                    destination = identityProvider.GetSingleLogoutServiceLocation(binding);

                    if (string.IsNullOrEmpty(destination))
                    {
                        // default with HttpRedirect
                        destination = identityProvider.GetSingleLogoutServiceLocation(Saml2Constants.HttpRedirectProtocolBinding);
                    }
                }

                StringBuilder rawXml = new StringBuilder();
                rawXml.Append("<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"");
                rawXml.Append(" ID=\"" + Saml2Utils.GenerateId() + "\"");
                rawXml.Append(" Version=\"2.0\"");
                rawXml.Append(" IssueInstant=\"" + Saml2Utils.GenerateIssueInstant() + "\"");

                if (!String.IsNullOrEmpty(destination))
                {
                    rawXml.Append(" Destination=\"" + destination + "\"");
                }

                rawXml.Append(" >");
                rawXml.Append(" <saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"");
                rawXml.Append("  Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"");
                rawXml.Append("  NameQualifier=\"" + identityProvider.EntityId + "\">" + subjectNameId + "</saml:NameID> ");
                rawXml.Append(" <samlp:SessionIndex xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" + sessionIndex + "</samlp:SessionIndex>");
                rawXml.Append(" <saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + serviceProvider.EntityId + "</saml:Issuer>");
                rawXml.Append("</samlp:LogoutRequest>");

                this.xml.LoadXml(rawXml.ToString());
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.LogoutRequestNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.LogoutRequestXmlException, xe);
            }
        }

        /// <summary>
        /// Initializes a new instance of the LogoutRequest class.
        /// This method is used by the de-serializer to reconstruct the object.
        /// </summary>
        /// <param name="info">The serialized version of the object.</param>
        /// <param name="context">Describes the source and destination of the serialized stream.</param>
        public LogoutRequest(SerializationInfo info, StreamingContext context)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;
                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

                string sourceXml = info.GetString("LogOutRequestXMLParams");
                if (!string.IsNullOrEmpty(sourceXml))
                {
                    this.xml.LoadXml(sourceXml);
                }
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.LogoutRequestNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.LogoutRequestXmlException, xe);
            }
        }
        #endregion

        #region ISerializable Members
        /// <summary>
        /// Converts an LogoutRequest object into its serialized form.
        /// </summary>
        /// <param name="info">The serialized version of the object.</param>
        /// <param name="context">Describes the source and destination of the serialized stream.</param>
        void ISerializable.GetObjectData(SerializationInfo info, StreamingContext context)
        {
            info.AddValue("LogOutRequestXMLParams", ((this.xml != null) ? this.xml.OuterXml : (string)null));
        }
        #endregion

        #region Properties

        /// <summary>
        /// Gets the extracted "NotOnOrAfter" from the logout request,
        /// otherwise return DateTime.MinValue since this is an optional
        /// attribute.
        /// </summary>
        public DateTime NotOnOrAfter
        {
            get
            {
                string xpath = "/samlp:LogoutRequest";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);

                if (node.Attributes["NotOnOrAfter"] != null)
                {
                    return DateTime.Parse(node.Attributes["NotOnOrAfter"].Value.Trim(), CultureInfo.InvariantCulture);
                }

                return DateTime.MinValue;
            }
        }

        /// <summary>
        /// Gets the ID attribute value of the logout request.
        /// </summary>
        public string Id
        {
            get
            {
                string xpath = "/samlp:LogoutRequest";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["ID"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the name of the issuer of the logout request.
        /// </summary>
        public string Issuer
        {
            get
            {
                string xpath = "/samlp:LogoutRequest/saml:Issuer";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.InnerText.Trim();
            }
        }

        /// <summary>
        /// Gets the XML representation of the received logout request.
        /// </summary>
        public IXPathNavigable XmlDom
        {
            get
            {
                return this.xml;
            }
        }

        /// <summary>
        /// Gets the signature of the logout request as an XML element.
        /// </summary>
        public IXPathNavigable XmlSignature
        {
            get
            {
                string xpath = "/samlp:LogoutRequest/ds:Signature";
                XmlNode root = this.xml.DocumentElement;
                XmlNode signatureElement = root.SelectSingleNode(xpath, this.nsMgr);
                return signatureElement;
            }
        }

        #endregion

        #region Methods
        #endregion
    }
}
