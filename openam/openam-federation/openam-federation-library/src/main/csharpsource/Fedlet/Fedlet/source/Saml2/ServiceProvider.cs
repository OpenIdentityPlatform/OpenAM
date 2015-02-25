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
 * $Id: ServiceProvider.cs,v 1.6 2010/01/26 01:20:14 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

using System;
using System.Collections;
using System.Globalization;
using System.IO;
using System.Security;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography.Xml;
using System.Text;
using System.Xml;
using Sun.Identity.Common;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Service Provider (SP) for the Fedlet in the ASP.NET environment. 
    /// </summary>
    public class ServiceProvider
    {
        #region Members

        /// <summary>
        /// Constant for the name of the service provider's metadata file.
        /// </summary>
        private static string metadataFilename = "sp.xml";

        /// <summary>
        /// Constant for the name of the service provider's extended metadata
        /// file.
        /// </summary>
        private static string extendedMetadataFilename = "sp-extended.xml";

        /// <summary>
        /// XML document representing the metadata for this Service Provider.
        /// </summary>
        private XmlDocument metadata;

        /// <summary>
        /// Namespace Manager for the metadata.
        /// </summary>
        private XmlNamespaceManager metadataNsMgr;

        /// <summary>
        /// XML document representing the extended metadata for this Service 
        /// Provider.
        /// </summary>
        private XmlDocument extendedMetadata;

        /// <summary>
        /// Namespace Manager for the extended metadata.
        /// </summary>
        private XmlNamespaceManager extendedMetadataNsMgr;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the ServiceProvider class. 
        /// </summary>
        /// <param name="homeFolder">Home folder containing configuration and metadata.</param>
        public ServiceProvider(string homeFolder)
        {
            try
            {
                this.metadata = new XmlDocument();
                this.metadata.Load(homeFolder + "\\" + ServiceProvider.metadataFilename);
                this.metadataNsMgr = new XmlNamespaceManager(this.metadata.NameTable);
                this.metadataNsMgr.AddNamespace("md", "urn:oasis:names:tc:SAML:2.0:metadata");

                this.extendedMetadata = new XmlDocument();
                this.extendedMetadata.Load(homeFolder + "\\" + ServiceProvider.extendedMetadataFilename);
                this.extendedMetadataNsMgr = new XmlNamespaceManager(this.extendedMetadata.NameTable);
                this.extendedMetadataNsMgr.AddNamespace("mdx", "urn:sun:fm:SAML:2.0:entityconfig");
            }
            catch (DirectoryNotFoundException dnfe)
            {
                throw new ServiceProviderException(Resources.ServiceProviderDirNotFound, dnfe);
            }
            catch (FileNotFoundException fnfe)
            {
                throw new ServiceProviderException(Resources.ServiceProviderFileNotFound, fnfe);
            }
            catch (XmlException xe)
            {
                throw new ServiceProviderException(Resources.ServiceProviderXmlException, xe);
            }
        }
        #endregion

        #region Properties
        /// <summary>
        /// Gets a value indicating whether the standard metadata value for 
        /// AuthnRequestsSigned is true or false.
        /// </summary>
        public bool AuthnRequestsSigned
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:SPSSODescriptor";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                string value = node.Attributes["AuthnRequestsSigned"].Value;
                return Saml2Utils.GetBoolean(value);
            }
        }

        /// <summary>
        /// Gets the entity ID for this service provider.
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
        /// Gets the meta alias for this service provider.
        /// </summary>
        public string MetaAlias
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);
                return node.Attributes["metaAlias"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the certificate alias, installed on this service provider, 
        /// for encryption.
        /// </summary>
        public string EncryptionCertificateAlias
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='encryptionCertAlias']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);
                if (node != null)
                {
                    return node.InnerText.Trim();
                }
                return null;
            }
        }

        /// <summary>
        /// Gets the encryption algorithm, installed on this service provider.
        /// </summary>
        public string EncryptionMethodAlgorithm
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:SPSSODescriptor/md:KeyDescriptor[@use='encryption']/md:EncryptionMethod";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                if (node != null)
                {
                    return node.Attributes["Algorithm"].Value;
                }
                return null;
            }
        }

        /// <summary>
        /// Gets a list of relay state URLs that are considered acceptable
        /// as a parameter in the various SAMLv2 profiles.
        /// </summary>
        public ArrayList RelayStateUrlList
        {
            get
            {
                ArrayList values = new ArrayList();
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='relayStateUrlList']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.extendedMetadataNsMgr);

                if (nodeList != null)
                {
                    foreach (XmlNode node in nodeList)
                    {
                        values.Add(node.InnerText.Trim());
                    }
                }

                return values;
            }
        }

        /// <summary>
        /// Gets the certificate alias, installed on this service provider, 
        /// for signing.
        /// </summary>
        public string SigningCertificateAlias
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='signingCertAlias']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);

                if (node != null)
                {
                    return node.InnerText.Trim();
                }

                return null;
            }
        }

        /// <summary>
        /// Gets the certificate alias, installed on this service provider, 
        /// for signing.
        /// </summary>
        public string AttributeQuerySigningCertificateAlias
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:AttributeQueryConfig/mdx:Attribute[@name='signingCertAlias']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);
                if (node != null)
                {
                    return node.InnerText.Trim();
                }
                return null;
            }
        }

        /// <summary>
        /// Gets the certificate alias, installed on this service provider, 
        /// for encryption (AttributeQuery).
        /// </summary>
        public string AttributeQueryEncryptionCertificateAlias
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:AttributeQueryConfig/mdx:Attribute[@name='encryptionCertAlias']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);
                if (node != null)
                {
                    return node.InnerText.Trim();
                }
                return null;
            }
        }

        /// <summary>
        /// Gets a value indicating whether the IPD Proxy setting is true or false.
        /// </summary>
        public bool ScopingIDPProxyEnabled
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='enableIDPProxy']/mdx:Value";
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
        /// Gets the IPD Proxy count
        /// </summary>
        public int ScopingProxyCount
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='idpProxyCount']/mdx:Value";
                if (this.ScopingIDPProxyEnabled)
                {
                    XmlNode root = this.extendedMetadata.DocumentElement;
                    XmlNode node = root.SelectSingleNode(xpath, this.extendedMetadataNsMgr);
                    if (node != null)
                    {
                        return Convert.ToInt32(node.InnerText.Trim(), CultureInfo.InvariantCulture);
                    }
                }
                return 0;
            }
        }

        /// <summary>
        /// Gets the list IPD Proxy entries
        /// </summary>
        public ArrayList ScopingIDPList
        {
            get
            {
                ArrayList values = new ArrayList();
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='idpProxyList']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.extendedMetadataNsMgr);
                if (nodeList != null)
                {
                    foreach (XmlNode node in nodeList)
                    {
                        values.Add(node.InnerText.Trim());
                    }
                }
                return values;
            }
        }

        /// <summary>
        /// Gets a value indicating whether the extended metadata value for 
        /// wantNameIDEncrypted is true or false.
        /// </summary>
        public bool WantNameIDEncryptedAttributeQuery
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:AttributeQueryConfig/mdx:Attribute[@name='wantNameIDEncrypted']/mdx:Value";
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
        /// Gets a value indicating whether the extended metadata value for 
        /// wantArtifactResponseSigned is true or false.
        /// </summary>
        public bool WantArtifactResponseSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='wantArtifactResponseSigned']/mdx:Value";
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
        /// Gets a value indicating whether the standard metadata value for 
        /// WantAssertionsSigned is true or false.
        /// </summary>
        public bool WantAssertionsSigned
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:SPSSODescriptor";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                string value = node.Attributes["WantAssertionsSigned"].Value;
                return Saml2Utils.GetBoolean(value);
            }
        }

        /// <summary>
        /// Gets a value indicating whether the extended metadata value for 
        /// wantPOSTResponseSigned is true or false.
        /// </summary>
        public bool WantPostResponseSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='wantPOSTResponseSigned']/mdx:Value";
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
        /// Gets a value indicating whether the extended metadata value for 
        /// wantLogoutRequestSigned is true or false.
        /// </summary>
        public bool WantLogoutRequestSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='wantLogoutRequestSigned']/mdx:Value";
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
        /// Gets a value indicating whether the extended metadata value for 
        /// wantLogoutResponseSigned is true or false.
        /// </summary>
        public bool WantLogoutResponseSigned
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='wantLogoutResponseSigned']/mdx:Value";
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
        /// Obtain the Signature Transform method.
        /// </summary>
        /// <returns>Signature Transform method as defined in the extended metadata for the SAML signature.</returns>
        public string SignatureTransformMethod
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='signatureTransformMethod']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath.ToString(), this.extendedMetadataNsMgr);

                if (node != null)
                {
                    return node.InnerText.Trim();
                }

                return null;
            }
        }

        /// <summary>
        /// Obtain the Canonicalization method for XML signature.
        /// </summary>
        /// <returns>Canonicalization method as defined in the extended metadata for the SAML signature.</returns>
        public string CanonicalizationMethod
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:IDPSSOConfig/mdx:Attribute[@name='signatureCanonicalizationMethod']/mdx:Value";
                XmlNode root = this.extendedMetadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath.ToString(), this.extendedMetadataNsMgr);
                if (node != null)
                {
                    return node.InnerText.Trim();
                }

                return null;
            }
        }
        
        /// <summary>
        /// Gets a value indicating whether the extended metadata value for 
        /// TrustAllCerts is true or false.
        /// </summary>
        public bool TrustAllCerts
        {
            get
            {
                string xpath = "/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute[@name='trustAllCerts']/mdx:Value";
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
        /// Obtain the assertion consumer service location based on the given binding.
        /// </summary>
        /// <param name="binding">The binding associated with the desired consumer service.</param>
        /// <returns>Service location as defined in the metadata for the binding, null if not found.</returns>
        public string GetAssertionConsumerServiceLocation(string binding)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:SPSSODescriptor/md:AssertionConsumerService");
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
        /// Obtain the assertion consumer service location based on the given binding.
        /// </summary>
        /// <param name="binding">The binding associated with the desired consumer service.</param>
        /// <param name="index">The index associated with the desired consumer service.</param>
        /// <returns>Service location as defined in the metadata for the binding, null if not found.</returns>
        public string GetAssertionConsumerServiceLocation(string binding, string index)
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/md:EntityDescriptor/md:SPSSODescriptor/md:AssertionConsumerService");
            xpath.Append("[@Binding='");
            xpath.Append(binding);
            xpath.Append("' and index='");
            xpath.Append(index);
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
        /// <para>
        /// Obtain the AuthLevel for the given uri reference found in the
        /// service provider extended metadata. An example would like as
        /// follows:
        /// </para>
        /// <para>
        ///  &lt;Attribute name="spAuthncontextClassrefMapping"&gt;
        ///    &lt;Value&gt;urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|0|default&lt;/Value&gt;
        ///  &lt;/Attribute&gt;
        /// </para>
        /// </summary>
        /// <param name="classReference">
        /// AuthnContextClassRef mapped to the desired Auth Level
        /// </param>
        /// <returns>Mapped integer for the given class reference.</returns>
        public int GetAuthLevelFromAuthnContextClassRef(string classReference)
        {
            int authLevel = -1;

            XmlNodeList nodes = this.GetAuthnContextClassRefMap();
            IEnumerator i = nodes.GetEnumerator();

            while (i.MoveNext())
            {
                XmlNode value = (XmlNode)i.Current;
                char[] separators = { '|' };
                string[] results = value.InnerText.Split(separators);
                if (results.Length > 1 && results[0] == classReference)
                {
                    authLevel = Convert.ToInt32(results[1], CultureInfo.InvariantCulture);
                    break;
                }
            }
            
            return authLevel;
        }

        /// <summary>
        /// <para>
        /// Obtain the AuthLevel for the given uri reference found in the
        /// service provider extended metadata. An example would like as
        /// follows:
        /// </para>
        /// <para>
        ///  &lt;Attribute name="spAuthncontextClassrefMapping"&gt;
        ///    &lt;Value&gt;urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|0|default&lt;/Value&gt;
        ///  &lt;/Attribute&gt;
        /// </para>
        /// </summary>
        /// <param name="authLevel">
        /// AuthLevel mapped to the desired AuthnContextClassRef
        /// </param>
        /// <returns>Class reference found for the specified AuthLevel</returns>
        public string GetAuthnContextClassRefFromAuthLevel(int authLevel)
        {
            // Set to default if not found.
            string classReference = Saml2Constants.AuthClassRefPasswordProtectedTransport;

            XmlNodeList nodes = this.GetAuthnContextClassRefMap();
            IEnumerator i = nodes.GetEnumerator();

            while (i.MoveNext())
            {
                XmlNode value = (XmlNode)i.Current;
                char[] separators = { '|' };
                string[] results = value.InnerText.Split(separators);
                if (results.Length > 1 && Convert.ToInt32(results[1], CultureInfo.InvariantCulture) == authLevel)
                {
                    classReference = results[0];
                    break;
                }
            }

            return classReference;
        }

        /// <summary>
        /// Returns a string representing the configured metadata for
        /// this service provider.  This will include key information
        /// as well if the metadata and extended metadata have this
        /// information specified.
        /// </summary>
        /// <param name="signMetadata">
        /// Flag to specify if the exportable metadata should be signed.
        /// </param>
        /// <returns>
        /// String with runtime representation of the metadata for this
        /// service provider.
        /// </returns>
        public string GetExportableMetadata(bool signMetadata)
        {
            XmlDocument exportableXml = (XmlDocument)this.metadata.CloneNode(true);
            XmlNode entityDescriptorNode
                = exportableXml.SelectSingleNode("/md:EntityDescriptor", this.metadataNsMgr);

            if (entityDescriptorNode == null)
            {
                throw new Saml2Exception(Resources.ServiceProviderEntityDescriptorNodeNotFound);
            }

            if (signMetadata && string.IsNullOrEmpty(this.SigningCertificateAlias))
            {
                throw new Saml2Exception(Resources.ServiceProviderCantSignMetadataWithoutCertificateAlias);
            }

            if (signMetadata)
            {
                XmlAttribute descriptorId = exportableXml.CreateAttribute("ID");
                descriptorId.Value = Saml2Utils.GenerateId();
                entityDescriptorNode.Attributes.Append(descriptorId);

                Saml2Utils.SignXml(this.SigningCertificateAlias, exportableXml, descriptorId.Value, true, this);
            }

            return exportableXml.InnerXml;
        }

        /// <summary>
        /// <para>
        /// Returns the XmlNodeList of "Values" maintained in the service
        /// provider's extended metadata under the attribute named
        /// "spAuthncontextClassrefMapping".  For example:
        /// </para>
        /// <para>
        ///  &lt;Attribute name="spAuthncontextClassrefMapping"&gt;
        ///    &lt;Value&gt;urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport|0|default&lt;/Value&gt;
        ///  &lt;/Attribute&gt;
        /// </para>
        /// </summary>
        /// <returns>Returns the XmlNodeList of values found in the metadata.</returns>
        private XmlNodeList GetAuthnContextClassRefMap()
        {
            StringBuilder xpath = new StringBuilder();
            xpath.Append("/mdx:EntityConfig/mdx:SPSSOConfig/mdx:Attribute");
            xpath.Append("[@name='spAuthncontextClassrefMapping']/mdx:Value");

            XmlNode root = this.extendedMetadata.DocumentElement;
            XmlNodeList nodes = root.SelectNodes(xpath.ToString(), this.extendedMetadataNsMgr);
            return nodes;
        }

        #endregion
    }
}
