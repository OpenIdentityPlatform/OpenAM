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
 * $Id: CircleOfTrust.cs,v 1.2 2009/05/19 16:01:03 ggennaro Exp $
 */

using System;
using System.Collections.Specialized;
using System.IO;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Circle Of Trust (COT) for the Fedlet in the ASP.NET environment. 
    /// </summary>
    public class CircleOfTrust
    {
        #region Members

        /// <summary>
        /// Name of key of property used for the saml2 reader service url.
        /// </summary>
        private const string Saml2ReaderServiceKey = "sun-fm-saml2-readerservice-url";

        /// <summary>
        /// Name of key of property used for the saml2 writer service url.
        /// </summary>
        private const string Saml2WriterServiceKey = "sun-fm-saml2-writerservice-url";

        /// <summary>
        /// Name of key of property used for list of trusted providers.
        /// </summary>
        private const string TrustedProvidersKey = "sun-fm-trusted-providers";
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the CircleOfTrust class.
        /// </summary>
        /// <param name="fileName">The file used to initiliaze this class.</param>
        public CircleOfTrust(string fileName)
        {
            try
            {
                this.Attributes = new NameValueCollection();

                StreamReader streamReader = new StreamReader(File.OpenRead(fileName));
                char[] separators = { '=' };
                while (streamReader.Peek() >= 0)
                {
                    string line = streamReader.ReadLine();
                    string[] tokens = line.Split(separators);
                    string key = tokens[0];
                    string value = tokens[1];
                    this.Attributes[key] = value;
                }

                streamReader.Close();
            }
            catch (DirectoryNotFoundException dnfe)
            {
                throw new CircleOfTrustException(Resources.CircleOfTrustDirNotFound, dnfe);
            }
            catch (FileNotFoundException fnfe)
            {
                throw new CircleOfTrustException(Resources.CircleOfTrustFileNotFound, fnfe);
            }
            catch (Exception e)
            {
                throw new CircleOfTrustException(Resources.CircleOfTrustUnhandledException, e);
            }
        }
        #endregion

        #region Properties
        /// <summary>
        /// Gets a name-value pair collection of attributes loaded from 
        /// the fedlet.cot configuration file.
        /// </summary>
        public NameValueCollection Attributes { get; private set; }

        /// <summary>
        /// Gets the saml2 reader service url, empty string if not specified,
        /// null attribute is not found.
        /// </summary>
        public Uri ReaderServiceUrl
        {
            get
            {
                string value = this.Attributes[CircleOfTrust.Saml2ReaderServiceKey];

                if (!String.IsNullOrEmpty(value))
                {
                    return new Uri(value);
                }
                else
                {
                    return null;
                }
            }
        }

        /// <summary>
        /// Gets the saml2 writer service url, empty string if not specified,
        /// null attribute is not found.
        /// </summary>
        public Uri WriterServiceUrl
        {
            get
            {
                string value = this.Attributes[CircleOfTrust.Saml2WriterServiceKey];

                if (!String.IsNullOrEmpty(value))
                {
                    return new Uri(value);
                }
                else
                {
                    return null;
                }
            }
        }
        #endregion

        #region Methods

        /// <summary>
        /// Checks service provider and identity provider Entity ID's to
        /// ensure they are found in the Trusted Providers property.
        /// </summary>
        /// <param name="serviceProviderEntityId">Service Provider EntityID</param>
        /// <param name="identityProviderEntityId">Identity Provider EntityID</param>
        /// <returns>True if providers are trusted, false otherwise.</returns>
        public bool AreProvidersTrusted(string serviceProviderEntityId, string identityProviderEntityId) 
        {
            bool results = false;
            string trusted = this.Attributes[CircleOfTrust.TrustedProvidersKey];

            if (trusted != null) 
            {
                string[] separator = { "," };
                string[] values = trusted.Split(separator, StringSplitOptions.RemoveEmptyEntries);
                StringCollection entities = new StringCollection();
                for (int i = 0; i < values.Length; i++)
                {
                    entities.Add(values[i].Trim());
                }

                if (entities.Contains(serviceProviderEntityId) && entities.Contains(identityProviderEntityId))
                {
                    results = true;
                }
            }

            return results;
        }

        #endregion
    }
}
