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
 * $Id: ArtifactResolve.cs,v 1.1 2009/06/11 18:37:58 ggennaro Exp $
 */

using System.Text;
using System.Xml;
using System.Xml.XPath;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing the SAMLv2 ArtifactResolve message for use in the
    /// artifact resolution profile.
    /// </summary>
    public class ArtifactResolve
    {
        #region Members
        /// <summary>
        /// Namespace Manager for this class.
        /// </summary>
        private XmlNamespaceManager nsMgr;

        /// <summary>
        /// XML representation of class.
        /// </summary>
        private XmlDocument xml;
        #endregion

        #region Constructor
        /// <summary>
        /// Initializes a new instance of the ArtifactResolve class.
        /// </summary>
        /// <param name="serviceProvider">Service Provider to issue this request</param>
        /// <param name="artifact">SAMLv2 Artifact</param>
        public ArtifactResolve(ServiceProvider serviceProvider, Artifact artifact)
        {
            this.xml = new XmlDocument();
            this.xml.PreserveWhitespace = true;

            this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
            this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
            this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");

            this.Id = Saml2Utils.GenerateId();
            this.IssueInstant = Saml2Utils.GenerateIssueInstant();
            this.Issuer = serviceProvider.EntityId;
            this.Artifact = artifact;

            StringBuilder rawXml = new StringBuilder();
            rawXml.Append("<samlp:ArtifactResolve");
            rawXml.Append(" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"");
            rawXml.Append(" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"");
            rawXml.Append(" ID=\"" + this.Id + "\"");
            rawXml.Append(" Version=\"2.0\"");
            rawXml.Append(" IssueInstant=\"" + this.IssueInstant + "\"");
            rawXml.Append(">");
            rawXml.Append(" <saml:Issuer>" + this.Issuer + "</saml:Issuer>");
            rawXml.Append(" <samlp:Artifact>" + this.Artifact.ToString() + "</samlp:Artifact>");
            rawXml.Append("</samlp:ArtifactResolve>");

            this.xml.LoadXml(rawXml.ToString());
        }
        #endregion

        #region Properties
        /// <summary>
        /// Gets the Artifact.
        /// </summary>
        public Artifact Artifact { get; private set; }

        /// <summary>
        /// Gets the ID.
        /// </summary>
        public string Id { get; private set; }

        /// <summary>
        /// Gets the Issuer.
        /// </summary>
        public string Issuer { get; private set; }

        /// <summary>
        /// Gets the IssueInstant.
        /// </summary>
        public string IssueInstant { get; private set; }

        /// <summary>
        /// Gets the XML representation.
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
        #endregion
    }
}
