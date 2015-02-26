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
 * $Id: Artifact.cs,v 1.1 2009/06/11 18:37:58 ggennaro Exp $
 */

using System;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing the SAMLv2 Artifact object. Per the specification,
    /// the artifact is constructed as follows:
    /// <para>
    ///  Artifact = Base64(TypeCode EndpointIndex RemainingArtifact)
    /// </para>
    /// <para>
    ///  TypeCode      = Byte1Byte2
    ///  EndpointIndex = Byte1Byte2
    /// </para>
    /// <para>
    ///  TypeCode          = 0x0004
    ///  RemainingArtifact = SourceID MessageHandle
    /// </para>
    /// <para>
    ///  SourceId      = 20-byte sequence, typically the entity id of the issuer
    ///  MessageHandle = 20-byte sequence
    /// </para>
    /// </summary>
    public class Artifact
    {
        #region Members
        /// <summary>
        /// Constant for the expected length of SAMLv2 artifacts before encoding.
        /// </summary>
        public const int RequiredByteLength = 44;

        /// <summary>
        /// Original string representation of the Artifact object.
        /// </summary>
        private string artifact;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the Artifact class.
        /// </summary>
        /// <param name="samlArt">String representing the artifact.</param>
        public Artifact(string samlArt)
        {
            if (String.IsNullOrEmpty(samlArt))
            {
                throw new Saml2Exception(Resources.ArtifactNullOrEmpty);
            }

            this.artifact = samlArt;

            try
            {
                byte[] byteArray = Convert.FromBase64String(this.artifact);

                if (byteArray.Length != Artifact.RequiredByteLength)
                {
                    throw new Saml2Exception(Resources.ArtifactInvalidLength);
                }

                this.TypeCode = BitConverter.ToString(byteArray, 0, 2).Replace("-", string.Empty);
                this.EndpointIndex = BitConverter.ToString(byteArray, 2, 2).Replace("-", string.Empty);
                this.SourceId = BitConverter.ToString(byteArray, 4, 20).Replace("-", string.Empty);
                this.MessageHandle = BitConverter.ToString(byteArray, 24, 20).Replace("-", string.Empty);
            }
            catch (FormatException)
            {
                throw new Saml2Exception(Resources.ArtifactFailedConversion);
            }
        }
        #endregion

        #region Properties
        /// <summary>
        /// Gets the TypeCode.
        /// </summary>
        public string TypeCode { get; private set; }

        /// <summary>
        /// Gets the EndpointIndex.
        /// </summary>
        public string EndpointIndex { get; private set; }

        /// <summary>
        /// Gets the Source ID.
        /// </summary>
        public string SourceId { get; private set; }

        /// <summary>
        /// Gets the Message Handle.
        /// </summary>
        public string MessageHandle { get; private set; }
        #endregion

        #region Methods
        /// <summary>
        /// Returns the 44 byte representation as a string.
        /// </summary>
        /// <returns>Returns a string representing the 44-byte SAML artifact.</returns>
        public override string ToString()
        {
            return this.artifact;
        }
        #endregion
    }
}