/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All Rights Reserved
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
using System.Runtime.Serialization;

namespace Sun.Identity.Saml2.Exceptions
{
    /// <summary>
    /// Exception class specific for SOAP logic.
    /// </summary>
    [SerializableAttribute]
    public class SoapException : Exception, ISerializable
    {
        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        public SoapException()
            : base()
        {
        }

        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        /// <param name="message">Message associated with this exception.</param>
        public SoapException(string message)
            : base(message)
        {
        }

        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        /// <param name="message">Message associated with this exception.</param>
        /// <param name="inner">Inner exception associated with this exception.</param>
        public SoapException(string message, Exception inner)
            : base(message, inner)
        {
        }

        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        /// <param name="info">SerializationInfo used for base class support.</param>
        /// <param name="context">StreamingContext used for base class support.</param>
        protected SoapException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}
