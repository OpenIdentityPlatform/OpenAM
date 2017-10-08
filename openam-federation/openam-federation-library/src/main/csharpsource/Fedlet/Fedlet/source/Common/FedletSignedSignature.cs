/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;

namespace Sun.Identity.Common
{
    /// <summary>
    /// Class containing the transform wrapper for the SHA512 signature extension algorithm.
    /// </summary>
    public class RsaPkCs1Sha512SignatureDescription : SignatureDescription
    {
        /// <summary>
        /// constructor - registers the handlers.
        /// </summary>
        public RsaPkCs1Sha512SignatureDescription()
        {
            KeyAlgorithm = typeof(RSACryptoServiceProvider).FullName;
            DigestAlgorithm = typeof(SHA512CryptoServiceProvider).FullName;
            FormatterAlgorithm = typeof(RSAPKCS1SignatureFormatter).FullName;
            DeformatterAlgorithm = typeof(RSAPKCS1SignatureDeformatter).FullName;
        }

        /// <summary>
        /// Called by the framework, this instantiates the deformatter engine for the signtaure handler.
        /// </summary>
        /// <param name="key">The encryption key to use.</param>
        /// <returns>
        /// The asysmmetic signature deformatter for use by the framework.
        /// </returns>
        public override AsymmetricSignatureDeformatter CreateDeformatter(AsymmetricAlgorithm key)
        {
            var sigProcessor = (AsymmetricSignatureDeformatter)CryptoConfig.CreateFromName(DeformatterAlgorithm);
            sigProcessor.SetKey(key);
            sigProcessor.SetHashAlgorithm("SHA512");
            return sigProcessor;
        }

        /// <summary>
        /// Called by the framework, this instantiates the formatter engine for the signtaure handler.
        /// </summary>
        /// <param name="key">The encryption key to use.</param>
        /// <returns>
        /// The asymmetic signature formatter for use by the framework.
        /// </returns>
        public override AsymmetricSignatureFormatter CreateFormatter(AsymmetricAlgorithm key)
        {
            var sigProcessor =
                (AsymmetricSignatureFormatter)CryptoConfig.CreateFromName(FormatterAlgorithm);
            sigProcessor.SetKey(key);
            sigProcessor.SetHashAlgorithm("SHA512");
            return sigProcessor;
        }
    }

    /// <summary>
    /// Class containing the transform wrapper for the SHA384 signature extension algorithm.
    /// </summary>
    public class RsaPkCs1Sha384SignatureDescription : SignatureDescription
    {
        /// <summary>
        /// constructor - registers the handlers.
        /// </summary>
        public RsaPkCs1Sha384SignatureDescription()
        {
            KeyAlgorithm = typeof(RSACryptoServiceProvider).FullName;
            DigestAlgorithm = typeof(SHA384CryptoServiceProvider).FullName;
            FormatterAlgorithm = typeof(RSAPKCS1SignatureFormatter).FullName;
            DeformatterAlgorithm = typeof(RSAPKCS1SignatureDeformatter).FullName;
        }

        /// <summary>
        /// Called by the framework, this instantiates the deformatter engine for the signtaure handler.
        /// </summary>
        /// <param name="key">The encryption key to use.</param>
        /// <returns>
        /// The asysmmetic signature deformatter for use by the framework.
        /// </returns>
        public override AsymmetricSignatureDeformatter CreateDeformatter(AsymmetricAlgorithm key)
        {
            var sigProcessor = (AsymmetricSignatureDeformatter)CryptoConfig.CreateFromName(DeformatterAlgorithm);
            sigProcessor.SetKey(key);
            sigProcessor.SetHashAlgorithm("SHA384");
            return sigProcessor;
        }

        /// <summary>
        /// Called by the framework, this instantiates the formatter engine for the signtaure handler.
        /// </summary>
        /// <param name="key">The encryption key to use.</param>
        /// <returns>
        /// The asymmetic signature formatter for use by the framework.
        /// </returns>
        public override AsymmetricSignatureFormatter CreateFormatter(AsymmetricAlgorithm key)
        {
            var sigProcessor =
                (AsymmetricSignatureFormatter)CryptoConfig.CreateFromName(FormatterAlgorithm);
            sigProcessor.SetKey(key);
            sigProcessor.SetHashAlgorithm("SHA384");
            return sigProcessor;
        }
    }

    /// <summary>
    /// Class containing the transform wrapper for the SHA256 signature extension algorithm.
    /// </summary>
    public class RsaPkCs1Sha256SignatureDescription : SignatureDescription
    {
        /// <summary>
        /// constructor - registers the handlers.
        /// </summary>
        public RsaPkCs1Sha256SignatureDescription()
        {
            KeyAlgorithm = typeof(RSACryptoServiceProvider).FullName;
            DigestAlgorithm = typeof(SHA256CryptoServiceProvider).FullName;
            FormatterAlgorithm = typeof(RSAPKCS1SignatureFormatter).FullName;
            DeformatterAlgorithm = typeof(RSAPKCS1SignatureDeformatter).FullName;
        }

        /// <summary>
        /// Called by the framework, this instantiates the deformatter engine for the signtaure handler.
        /// </summary>
        /// <param name="key">The encryption key to use.</param>
        /// <returns>
        /// The asysmmetic signature deformatter for use by the framework.
        /// </returns>
        public override AsymmetricSignatureDeformatter CreateDeformatter(AsymmetricAlgorithm key)
        {
            var sigProcessor =
                (AsymmetricSignatureDeformatter)CryptoConfig.CreateFromName(DeformatterAlgorithm);
            sigProcessor.SetKey(key);
            sigProcessor.SetHashAlgorithm("SHA256");
            return sigProcessor;
        }

        /// <summary>
        /// Called by the framework, this instantiates the formatter engine for the signtaure handler.
        /// </summary>
        /// <param name="key">The encryption key to use.</param>
        /// <returns>
        /// The asymmetic signature formatter for use by the framework.
        /// </returns>
        public override AsymmetricSignatureFormatter CreateFormatter(AsymmetricAlgorithm key)
        {
            var sigProcessor =
                (AsymmetricSignatureFormatter)CryptoConfig.CreateFromName(FormatterAlgorithm);
            sigProcessor.SetKey(key);
            sigProcessor.SetHashAlgorithm("SHA256");
            return sigProcessor;
        }
    }

    /// <summary>
    /// Thread-safe singleton class to load the extended signature formatters into the crypto engine prior to use.
    /// Once loaded, they are then accessible to all subsequent calls.
    /// </summary>
    public sealed class FedletSignedSignatureSupportSingleton
    {
        /// <summary>
        /// Internal flag to indicate whether the extended support transforms have been correctly registered.
        /// </summary>
        private bool extendedAlgorithmsAvailable;

        /// <summary>
        /// Lazy instatiation of the singleton upon first use. This method is intrisically thread safe.
        /// </summary>
        private static readonly Lazy<FedletSignedSignatureSupportSingleton> lazy =
            new Lazy<FedletSignedSignatureSupportSingleton>(() => new FedletSignedSignatureSupportSingleton());

        /// <summary>
        /// Public method to obtain the singleton instance, creating it if needed.
        /// </summary>
        public static FedletSignedSignatureSupportSingleton Instance { get { return lazy.Value; } }

        /// <summary>
        ///  Public method to obtain the successful registration flag from the singleton, using the lazy creation call above.
        /// </summary>
        public static bool IsInitialised { get { return Instance.extendedAlgorithmsAvailable; } }

        /// <summary>
        /// The private, internal, singleton constructor. This registers the extended signature transform handlers
        /// with the .NET crypto engine and, if successful, sets the available flag to true.
        /// </summary>
        private FedletSignedSignatureSupportSingleton()
        {
            try
            {
                CryptoConfig.AddAlgorithm(typeof(RsaPkCs1Sha512SignatureDescription),
                    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512");
                CryptoConfig.AddAlgorithm(typeof(RsaPkCs1Sha384SignatureDescription),
                    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384");
                CryptoConfig.AddAlgorithm(typeof(RsaPkCs1Sha256SignatureDescription),
                    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
                extendedAlgorithmsAvailable = true;
            }
            catch (Exception ex)
            {
                extendedAlgorithmsAvailable = false;
            }
        }
    }
}
