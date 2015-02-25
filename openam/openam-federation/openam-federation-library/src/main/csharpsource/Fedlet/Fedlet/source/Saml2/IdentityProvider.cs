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
 * $Id: IdentityProvider.cs,v 1.6 2010/01/19 18:23:09 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

using System.Collections;
using System.IO;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Xml;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing all metadata for an Identity Provider.
    /// </summary>
    public class IdentityProvider
    {
        #region Members
        /// <summary>
        /// XML document representing the metadata for this Identity Provider.
        /// </summary>
        private XmlDocument metadata;

        /// <summary>
        /// Namespace Manager for the metadata.
        /// </summary>
        private XmlNamespaceManager metadataNsMgr;

        /// <summary>
        /// XML document representing the extended metadata for this Identity 
        /// Provider.
        /// </summary>
        private XmlDocument extendedMetadata;
        
        /// <summary>
        /// Namespace Manager for the extended metadata.
        /// </summary>
        private XmlNamespaceManager extendedMetadataNsMgr;
        
        /// <summary>
        /// Identity Provider's X509 certificate.
        /// </summary>
        private X509Certificate2 signingCertificate;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the IdentityProvider class.
        /// </summary>
        /// <param name="metadataFileName">Name of file for metdata.</param>
        /// <param name="extendedMetadataFileName">Name of file for extended metadata.</param>
        public IdentityProvider(string metadataFileName, string extendedMetadataFileName)
        {
            try
            {
                this.metadata = new XmlDocument();
                this.metadata.Load(metadataFileName);
                this.metadataNsMgr = new XmlNamespaceManager(this.metadata.NameTable);
                this.metadataNsMgr.AddNamespace("md", "urn:oasis:names:tc:SAML:2.0:metadata");
                this.metadataNsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.metadataNsMgr.AddNamespace("aq", "urn:oasis:names:tc:SAML:metadata:X509:query");

                this.extendedMetadata = new XmlDocument();
                this.extendedMetadata.Load(extendedMetadataFileName);
                this.extendedMetadataNsMgr = new XmlNamespaceManager(this.extendedMetadata.NameTable);
                this.extendedMetadataNsMgr.AddNamespace("mdx", "urn:sun:fm:SAML:2.0:entityconfig");

                // Load now since a) it doesn't change and b) its a 
                // performance dog on Win 2003 64-bit.
                byte[] byteArray = Encoding.UTF8.GetBytes(this.EncodedSigningCertificate);
                this.signingCertificate = new X509Certificate2(byteArray);
            }
            catch (DirectoryNotFoundException dnfe)
            {
                throw new IdentityProviderException(Resources.IdentityProviderDirNotFound, dnfe);
            }
            catch (FileNotFoundException fnfe)
            {
                throw new IdentityProviderException(Resources.IdentityProviderFileNotFound, fnfe);
            }
            catch (XmlException xe)
            {
                throw new IdentityProviderException(Resources.IdentityProviderXmlException, xe);
            }
        }
        #endregion

        #region Properties

        /// <summary>
        /// Gets the entity ID of this identity provider.
        /// </summary>
        public string EntityId
        {
            get
            {
                string xpath = "/md:EntityDescriptor";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                return node.Attributes["entityID"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the encoded X509 certifcate located within the identity
        /// provider's metadata.
        /// </summary>
        public string EncodedSigningCertificate
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:IDPSSODescriptor/md:KeyDescriptor[@use='signing' or (not(@use) and count(../KeyDescriptor[@use='signing']) = 0)][1]/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                if (node != null)
                {
                    return node.InnerText.Trim();
                }
                return null;
            }
        }

        /// <summary>
        /// Gets the encoded X509 certificate located within the identity
        /// provider's metadata. Attribute Authority role use only.
        /// </summary>
        public string EncodedEncryptionCertificate
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:AttributeAuthorityDescriptor/md:KeyDescriptor[@use='encryption' or (not(@use) and count(../KeyDescriptor[@use='encryption']) = 0)][1]/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                if (node != null)
                {
                    return node.InnerText.Trim();
                }
                return null;
            }
        }

        /// <summary>
        /// Gets the encryption algorithm, installed on this identity provider.
        /// Attribute Authority role use only.
        /// </summary>
        public string EncryptionMethodAlgorithm
        {
            get
            {
                string aes128cbc = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
                string xpath = "/md:EntityDescriptor/md:AttributeAuthorityDescriptor/md:KeyDescriptor[@use='encryption' or (not(@use) and count(../KeyDescriptor[@use='encryption']) = 0)][1]/md:EncryptionMethod";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                if (node != null)
                {
                    string value = node.Attributes["Algorithm"].Value;
                    if (!string.IsNullOrWhiteSpace(value))
                    {
                       return value;
                    }
                }
                return aes128cbc;
            }
        }

        /// <summary>
        /// Gets the X509 signing certificate for this identity provider.
        /// </summary>
        public X509Certificate2 SigningCertificate
        {
            get
            {
                return this.signingCertificate;
            }
        }

        /// <summary>
        /// Gets Attribute Query service locations
        /// </summary>
        public XmlNodeList AttributeServiceLocations
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:AttributeAuthorityDescriptor/md:AttributeService";
                XmlNode root = this.metadata.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.metadataNsMgr);
                return nodeList;
            }
        }

        /// <summary>
        /// Obtain the single attribute query location based on the given binding and support for X509 query.
        /// </summary>
        /// <param name="binding">
        /// The binding associated with the desired service.
        /// </param>
        /// <param name="supportsX509Query">
        /// Flag for X509 query type
        /// </param>
        /// <returns>
        /// Service location as defined in the metadata for the specified IDP,
        /// binding and support for X509 query.
        /// </returns>
        public string GetSingleAttributeServiceLocation(string binding, bool supportsX509Query)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:AttributeAuthorityDescriptor/md:AttributeService");
            xpath.Append("[@Binding='");
            xpath.Append(binding);
            if (!supportsX509Query)
            {
                xpath.Append("' and not(@aq:supportsX509Query)]");
            }
            else
            {
                xpath.Append("' and @aq:supportsX509Query='true']");
            }

            XmlNode root = this.metadata.DocumentElement;
            XmlNode node = root.SelectSingleNode(xpath.ToString(), this.metadataNsMgr);
            if (node != null)
            {
                return node.Attributes["Location"].Value.Trim();
            }

            return null;
        }

        /// <summary>
        /// Gets the list of single log out service locations, if present,
        /// otherwise an empty list.
        /// </summary>
        public XmlNodeList SingleLogOutServiceLocations
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:IDPSSODescriptor/md:SingleLogoutService";
                XmlNode root = this.metadata.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.metadataNsMgr);

                return nodeList;
            }
        }

        /// <summary>
        /// Gets the list of single sign on service locations, if present,
        /// otherwise an empty list.
        /// </summary>
        public XmlNodeList SingleSignOnServiceLocations
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:IDPSSODescriptor/md:SingleSignOnService";
                XmlNode root = this.metadata.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.metadataNsMgr);

                return nodeList;
            }
        }

        /// <summary>
        /// Gets a value indicating whether the extended metadata for
        /// WantArtifactResolveSigned is true or false.
        /// </summary>
        public bool WantArtifactResolveSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:IDPSSOConfig/mdx:Attribute[@name='wantArtifactResolveSigned']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);

                if (node != null)
                {
                    string value = node.InnerText.Trim();
                    return Saml2Utils.GetBoolean(value);
                }

                return false;
            }
        }

        /// <summary>
        /// Gets a value indicating whether the metadata value for 
        /// WantAuthnRequestsSigned is true or false.
        /// </summary>
        public bool WantAuthnRequestsSigned
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:IDPSSODescriptor";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);

                if (node != null)
                {
                    string value = node.Attributes["WantAuthnRequestsSigned"].Value;
                    return Saml2Utils.GetBoolean(value);
                }

                return false;
            }
        }

        /// <summary>
        /// Gets a value indicating whether the metadata value for
        /// WantLogoutRequestSigned is true or false.
        /// </summary>
        /// <returns></returns>
        public bool WantLogoutRequestSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:IDPSSOConfig/mdx:Attribute[@name='wantLogoutRequestSigned']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);

                if (node != null)
                {
                    string value = node.InnerText.Trim();
                    return Saml2Utils.GetBoolean(value);
                }

                return false;
            }
        }

        /// <summary>
        /// Gets a value indicating whether the metadata value for
        /// WantLogoutResponseSigned is true or false.
        /// </summary>
        public bool WantLogoutResponseSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:IDPSSOConfig/mdx:Attribute[@name='wantLogoutResponseSigned']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);

                if (node != null)
                {
                    string value = node.InnerText.Trim();
                    return Saml2Utils.GetBoolean(value);
                }

                return false;
            }
        }

        #endregion

        #region Methods
        /// <summary>
        /// Obtain the artifact resolution service location based on the given binding.
        /// </summary>
        /// <param name="binding">The binding associated with the desired service.</param>
        /// <returns>Service location as defined in the metadata for the binding, null if not found.</returns>
        public string GetArtifactResolutionServiceLocation(string binding)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:IDPSSODescriptor/md:ArtifactResolutionService");
            xpath.Append("[@Binding='");
            xpath.Append(binding);
            xpath.Append("']");

            XmlNode root = this.metadata.DocumentElement;
            XmlNode node = root.SelectSingleNode(xpath.ToString(), this.metadataNsMgr);
            if (node != null)
            {
                return node.Attributes["Location"].Value.Trim();
            }

            return null;
        }

        /// <summary>
        /// Obtain the single logout location based on the given binding.
        /// </summary>
        /// <param name="binding">
        /// The binding (should be made into constants / types).
        /// </param>
        /// <returns>
        /// Service location as defined in the metadata for the specified IDP
        /// and binding.
        /// </returns>
        public string GetSingleLogoutServiceLocation(string binding)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:IDPSSODescriptor/md:SingleLogoutService");
            xpath.Append("[@Binding='");
            xpath.Append(binding);
            xpath.Append("']");

            XmlNode root = this.metadata.DocumentElement;
            XmlNode node = root.SelectSingleNode(xpath.ToString(), this.metadataNsMgr);
            if (node != null)
            {
                return node.Attributes["Location"].Value.Trim();
            }

            return null;
        }

        /// <summary>
        /// Obtain the single logout resopnse location based on the given
        /// binding.
        /// </summary>
        /// <param name="binding">
        /// The binding (should be made into constants / types).
        /// </param>
        /// <returns>
        /// Service response location as defined in the metadata for the
        /// specified IDP and binding.
        /// </returns>
        public string GetSingleLogoutServiceResponseLocation(string binding)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:IDPSSODescriptor/md:SingleLogoutService");
            xpath.Append("[@Binding='");
            xpath.Append(binding);
            xpath.Append("']");

            XmlNode root = this.metadata.DocumentElement;
            XmlNode node = root.SelectSingleNode(xpath.ToString(), this.metadataNsMgr);
            if (node != null)
            {
                return node.Attributes["ResponseLocation"].Value.Trim();
            }

            return null;
        }

        /// <summary>
        /// Obtain the single sign on location based on the given binding.
        /// </summary>
        /// <param name="binding">The binding (should be made into constants / types).</param>
        /// <returns>Service location as defined in the metadata for the specified IDP and binding.</returns>
        public string GetSingleSignOnServiceLocation(string binding)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:IDPSSODescriptor/md:SingleSignOnService");
            xpath.Append("[@Binding='");
            xpath.Append(binding);
            xpath.Append("']");

            XmlNode root = this.metadata.DocumentElement;
            XmlNode node = root.SelectSingleNode(xpath.ToString(), this.metadataNsMgr);
            if (node != null)
            {
                return node.Attributes["Location"].Value.Trim();
            }

            return null;
        }
        #endregion
    }
}
