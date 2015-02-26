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
 * $Id: AuthnResponse.cs,v 1.5 2009/11/11 18:13:39 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2011-2013 ForgeRock Inc.
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
    /// SAMLv2 AuthnResponse object constructed from a response obtained from
    /// an Identity Provider for the hosted Service Provider.
    /// </summary>
    public class AuthnResponse
    {
        #region Members
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
        /// Initializes a new instance of the AuthnResponse class.
        /// </summary>
        /// <param name="samlResponse">Decoded SAMLv2 Response</param>
        public AuthnResponse(string samlResponse)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;
                this.xml.LoadXml(samlResponse);

                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                this.nsMgr.AddNamespace("xenc", "http://www.w3.org/2001/04/xmlenc#");
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.AuthnResponseNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.AuthnResponseXmlException, xe);
            }
        }
        #endregion

        #region Properties

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

        /// <summary>
        /// Gets the signature of the authn response attached to the 
        /// assertion as an XML element.
        /// </summary>
        public IXPathNavigable XmlAssertionSignature
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/ds:Signature";
                XmlNode root = this.xml.DocumentElement;
                XmlNode signatureElement = root.SelectSingleNode(xpath, this.nsMgr);
                return signatureElement;
            }
        }

        /// <summary>
        /// Gets the signature of the authn response attached to the 
        /// response as an XML element.
        /// </summary>
        public IXPathNavigable XmlResponseSignature
        {
            get
            {
                string xpath = "/samlp:Response/ds:Signature";
                XmlNode root = this.xml.DocumentElement;
                XmlNode signatureElement = root.SelectSingleNode(xpath, this.nsMgr);
                return signatureElement;
            }
        }

        /// <summary>
        /// Gets the Assertion ID attribute value of the response.
        /// </summary>
        public string AssertionId
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["ID"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the ID attribute value of the response.
        /// </summary>
        public string Id
        {
            get
            {
                string xpath = "/samlp:Response";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["ID"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the InResponseTo attribute value of the authn response, null
        /// if not present.
        /// </summary>
        public string InResponseTo
        {
            get
            {
                string xpath = "/samlp:Response";
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
        /// Gets the name of the issuer of the authn response.
        /// </summary>
        public string Issuer
        {
            get
            {
                string xpath = "/samlp:Response/saml:Issuer";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.InnerText.Trim();
            }
        }

        /// <summary>
        /// Gets the status code of the authn response within the status element.
        /// </summary>
        public string StatusCode
        {
            get
            {
                string xpath = "/samlp:Response/samlp:Status/samlp:StatusCode";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["Value"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the X509 signature certificate of the authn response attached
        /// to the assertion, null if none provided.
        /// </summary>
        public string AssertionSignatureCertificate
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
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
        /// Gets the X509 signature certificate of the authn response attached
        /// to the response, null if none provided.
        /// </summary>
        public string ResponseSignatureCertificate
        {
            get
            {
                string xpath = "/samlp:Response/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
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
        /// Gets the session index within the authn statement within the authn
        /// response assertion.
        /// </summary>
        public string SessionIndex
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:AuthnStatement";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["SessionIndex"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the name ID of the subject within the authn response assertion.
        /// </summary>
        public string SubjectNameId
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Subject/saml:NameID";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.InnerText.Trim();
            }
        }

        /// <summary>
        /// Gets the extracted "NotBefore" condition from the authn response.
        /// </summary>
        public DateTime ConditionNotBefore
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Conditions";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return DateTime.Parse(node.Attributes["NotBefore"].Value.Trim(), CultureInfo.InvariantCulture);
            }
        }

        /// <summary>
        /// Gets the extracted "NotOnOrAfter" condition from the authn response.
        /// </summary>
        public DateTime ConditionNotOnOrAfter
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Conditions";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return DateTime.Parse(node.Attributes["NotOnOrAfter"].Value.Trim(), CultureInfo.InvariantCulture);
            }
        }

        /// <summary>
        /// Gets the list containing string of entity ID's that are considered
        /// appropriate audiences for this authn response.
        /// </summary>
        public ArrayList ConditionAudiences
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Conditions/saml:AudienceRestriction/saml:Audience";
                XmlNode root = this.xml.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.nsMgr);
                IEnumerator nodes = (IEnumerator)nodeList.GetEnumerator();

                ArrayList audiences = new ArrayList();
                while (nodes.MoveNext())
                {
                    XmlNode node = (XmlNode)nodes.Current;
                    audiences.Add(node.InnerText.Trim());
                }

                return audiences;
            }
        }

        /// <summary>
        /// Gets the property containing the attributes provided in the SAML2
        /// assertion, if provided, otherwise an empty hashtable.
        /// </summary>
        public Hashtable Attributes
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:AttributeStatement/saml:Attribute";
                XmlNode root = this.xml.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.nsMgr);
                IEnumerator nodes = (IEnumerator)nodeList.GetEnumerator();

                Hashtable attributes = new Hashtable();
                while (nodes.MoveNext())
                {
                    XmlNode samlAttribute = (XmlNode)nodes.Current;
                    string name = samlAttribute.Attributes["Name"].Value.Trim();

                    XmlNodeList samlAttributeValues = samlAttribute.SelectNodes("descendant::saml:AttributeValue", this.nsMgr);
                    ArrayList values = new ArrayList();
                    foreach (XmlNode node in samlAttributeValues)
                    {
                        string value = node.InnerText.Trim();
                        values.Add(value);
                    }

                    attributes.Add(name, values);
                }

                return attributes;
            }
        }
        #endregion

        #region Methods
        /// <summary>
        /// Tells whether the SAML response contains any kind of encrypted content.
        /// </summary>
        /// <returns><code>true</code> if the SAML response contains encrypted elements.</returns>
        public bool IsEncrypted()
        {
            string xpath = "//xenc:EncryptedData";
            XmlNode node = xml.DocumentElement;
            return node.SelectNodes(xpath, nsMgr).Count != 0;
        }

        /// <summary>
        /// Tells whether the SAML response contains encrypted assertion tag or not.
        /// If the assertion is encrypted, then the decryption should be executed
        /// before checking the digital signature.
        /// </summary>
        /// <returns><code>true</code> if the assertion is encrypted.</returns>
        public bool isAssertionEncrypted()
        {
            string xpath = "//saml:EncryptedAssertion";
            XmlNode node = xml.DocumentElement;
            return node.SelectNodes(xpath, nsMgr).Count != 0;
        }

        /// <summary>
        /// Recursively decrypts the received SAML response.
        /// </summary>
        /// <param name="serviceProvider">ServiceProvider instance, so we can extract
        /// information about the SP configuration.</param>
        public void Decrypt(ServiceProvider serviceProvider)
        {
            try
            {
                FedletEncryptedXml encXml = new FedletEncryptedXml(xml, serviceProvider);
                encXml.DecryptDocument();
                Normalize();
            }
            catch (CryptographicException ce)
            {
                throw new Saml2Exception(Resources.DecryptionFailed, ce);
            }
        }

        private void Normalize()
        {
            ReplaceParentWithNode("//saml:EncryptedAssertion");
            ReplaceParentWithNode("//saml:EncryptedAttribute");
            ReplaceParentWithNode("//saml:EncryptedID");
        }

        private void ReplaceParentWithNode(string xpath)
        {
            XmlNodeList nodes = xml.SelectNodes(xpath, nsMgr);
            foreach (XmlNode node in nodes)
            {
                foreach (XmlNode child in node.ChildNodes)
                {
                    node.ParentNode.AppendChild(child);
                }
                node.ParentNode.RemoveChild(node);
            }
        }
        #endregion
    }
}
