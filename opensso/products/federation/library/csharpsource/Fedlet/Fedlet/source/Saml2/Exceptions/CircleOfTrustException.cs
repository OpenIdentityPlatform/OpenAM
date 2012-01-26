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
 * $Id: CircleOfTrustException.cs,v 1.1 2009/05/01 15:19:53 ggennaro Exp $
 */

using System;
using System.Runtime.Serialization;

namespace Sun.Identity.Saml2.Exceptions
{
    /// <summary>
    /// Exception class specific for CircleOfTrust business logic.
    /// </summary>
    [SerializableAttribute]
    public class CircleOfTrustException : Exception, ISerializable
    {
        /// <summary>
        /// Initializes a new instance of the CircleOfTrustException class.
        /// </summary>
        public CircleOfTrustException() : base()
        {
        }

        /// <summary>
        /// Initializes a new instance of the CircleOfTrustException class.
        /// </summary>
        /// <param name="message">Message associated with this exception.</param>
        public CircleOfTrustException(string message) : base(message)
        {
        }

        /// <summary>
        /// Initializes a new instance of the CircleOfTrustException class.
        /// </summary>
        /// <param name="message">Message associated with this exception.</param>
        /// <param name="inner">Inner exception associated with this exception.</param>
        public CircleOfTrustException(string message, Exception inner) : base(message, inner)
        {
        }

        /// <summary>
        /// Initializes a new instance of the CircleOfTrustException class.
        /// </summary>
        /// <param name="info">SerializationInfo used for base class support.</param>
        /// <param name="context">StreamingContext used for base class support.</param>
        protected CircleOfTrustException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}
