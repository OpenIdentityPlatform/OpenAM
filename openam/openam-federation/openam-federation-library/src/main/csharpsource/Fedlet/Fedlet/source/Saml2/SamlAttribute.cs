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

using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing the SAMLv2 Attribute Query request object.
    /// </summary>
    public class SamlAttribute
    {
        #region Constructors
        /// <summary>
        /// Initializes a new instance of the SamlAttribute class.
        /// </summary>
        /// <param name="name">String representing the name.</param>
        public SamlAttribute(string name)
        {
            if (string.IsNullOrWhiteSpace(name))
            {
                throw new Saml2Exception(Resources.ArtifactNullOrEmpty);
            }

            this.Name = name.Trim();

            if (this.Name.StartsWith("urn"))
            {
                this.NameFormat = Saml2Constants.AttributeNameFormatX500;
            }
            else
            {
                this.NameFormat = Saml2Constants.AttributeNameFormatBasic;
            }
        }
        #endregion

        #region Properties
        /// <summary>
        /// Gets the Attribute name.
        /// </summary>
        public string Name { get; private set; }

        /// <summary>
        /// Gets the Attribute name format.
        /// </summary>
        public string NameFormat { get; private set; }

        /// <summary>
        /// Gets the Attribute friendly name.
        /// </summary>
        public string FriendlyName { get; set; }
        #endregion

        #region Methods
        /// <summary>
        /// 
        /// </summary>
        /// <returns>Returns a string representing SamlAttribute</returns>
        public override string ToString()
        {
            StringBuilder xml = new StringBuilder();
            xml.Append("<saml:Attribute ");

            if (!string.IsNullOrWhiteSpace(this.Name))
            {
                xml.Append(" Name=\"" + this.Name + "\"");
            }
            else
            {
                xml.Append(" Name=\"\"");
            }

            if (!string.IsNullOrWhiteSpace(this.NameFormat))
            {
                xml.Append(" NameFormat=\"" + this.NameFormat + "\"");
            }
            else
            {
                xml.Append(" NameFormat=\"\"");
            }

            if (!string.IsNullOrWhiteSpace(this.FriendlyName))
            {
                xml.Append(" FriendlyName=\"" + this.FriendlyName + "\"");
            }

            xml.Append("/>");

            return xml.ToString();
        }
        #endregion
    }
}
