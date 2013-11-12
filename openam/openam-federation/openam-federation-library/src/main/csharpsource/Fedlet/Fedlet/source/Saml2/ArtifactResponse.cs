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
 * $Id: ArtifactResponse.cs,v 1.2 2009/11/11 18:13:39 ggennaro Exp $
 */

using System;
using System.Collections;
using System.Globalization;
using System.Security.Cryptography;
using System.Xml;
using System.Xml.XPath;
using Sun.Identity.Common;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// SAMLv2 ArtifactResponse object constructed from a response obtained 
    /// from an Identity Provider for the hosted Service Provider.
    /// </summary>
    public class ArtifactResponse
    {
        #region Members
        /// <summary>
        /// AuthnResponse wrapped by this ArtifactResponse.
        /// </summary>
        private AuthnResponse authnResponse;

        /// <summary>
        /// Namespace Manager for this authn response.
        /// </summary>
        private XmlNamespaceManager nsMgr;

        /// <summary>
        /// XML representation of the authn response.
        /// </summary>
        private XmlDocument xml;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the ArtifactResponse class.
        /// </summary>
        /// <param name="artifactResponse">
        /// String representation of the ArtifactResponse xml.
        /// </param>
        public ArtifactResponse(string artifactResponse)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;
                this.xml.LoadXml(artifactResponse);
                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                this.nsMgr.AddNamespace("xenc", "http://www.w3.org/2001/04/xmlenc#");

                string xpath = "/samlp:ArtifactResponse/samlp:Response";
                XmlNode response = this.xml.DocumentElement.SelectSingleNode(xpath, this.nsMgr);
                if (response == null)
                {
                    throw new Saml2Exception(Resources.ArtifactResponseMissingResponse);
                }

                this.authnResponse = new AuthnResponse(response.OuterXml);
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.ArtifactResponseNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.ArtifactResponseXmlException, xe);
            }
        }
        #endregion

        #region Properties

        /// <summary>
        /// Gets the AuthnResponse object enclosed in the artifact response.
        /// </summary>
        public AuthnResponse AuthnResponse
        {
            get
            {
                return this.authnResponse;
            }
        }

        /// <summary>
        /// Gets the ID attribute value of the artifact response.
        /// </summary>
        public string Id
        {
            get
            {
                string xpath = "/samlp:ArtifactResponse";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["ID"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the InResponseTo attribute value of the artifact response, 
        /// null if not present.
        /// </summary>
        public string InResponseTo
        {
            get
            {
                string xpath = "/samlp:ArtifactResponse";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);

                if (node.Attributes["InResponseTo"] == null)
                {
                    return null;
                }

                return node.Attributes["InResponseTo"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the name of the issuer of the artifact response.
        /// </summary>
        public string Issuer
        {
            get
            {
                string xpath = "/samlp:ArtifactResponse/saml:Issuer";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.InnerText.Trim();
            }
        }

        /// <summary>
        /// Gets the X509 signature certificate of the artifact response,
        /// null if none provided.
        /// </summary>
        public string SignatureCertificate
        {
            get
            {
                string xpath = "/samlp:ArtifactResponse/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                if (node == null)
                {
                    return null;
                }

                string value = node.InnerText.Trim();
                return value;
            }
        }

        /// <summary>
        /// Gets the signature of the artifact response as an XML element.
        /// </summary>
        public IXPathNavigable XmlSignature
        {
            get
            {
                string xpath = "/samlp:ArtifactResponse/ds:Signature";
                XmlNode root = this.xml.DocumentElement;
                XmlNode signatureElement = root.SelectSingleNode(xpath, this.nsMgr);
                return signatureElement;
            }
        }

        /// <summary>
        /// Gets the XML representation of the received artifact response.
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
        /// Decrypts the authnResponse, then modifies the artifactresponse too.
        /// </summary>
        /// <param name="serviceProvider">ServiceProvider instance, so we can extract
        /// information about the SP configuration.</param>
        public void Decrypt(ServiceProvider serviceProvider)
        {
            authnResponse.Decrypt(serviceProvider);
            XmlNode node = xml.SelectSingleNode("//samlp:Response", nsMgr).ParentNode;
            node.InnerXml = ((XmlNode)authnResponse.XmlDom).InnerXml;
        }
        #endregion
    }
}
