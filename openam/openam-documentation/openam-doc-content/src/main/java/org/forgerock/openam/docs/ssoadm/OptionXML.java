/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openam.docs.ssoadm;

/**
 * Each ssoadm subcommand option annotation is transformed to a DocBook 5 XML
 * fragment for inclusion in the ssoadm subcommand section.
 *
 * <pre>
 * {@code
 * <varlistentry>
 *  <term><option>--adminid, -u</option></term><!-- Mandatory -->
 *  <listitem><para>Administrator ID of running the command.</para></listitem>
 * </varlistentry>
 *
 * <varlistentry>
 *  <term><option>[--adminid, -u]</option></term><!-- Optional -->
 *  <listitem><para>Administrator ID of running the command.</para></listitem>
 * </varlistentry>
 * }
 * </pre>
 */

public class OptionXML
{
  /**
   * Transform an option string to an XML fragment.
   *
   * @param orig Option string such as
   *  "adminid|u|s|Administrator ID of running the command."
   * @param optional If true, then enclose option string in [].
   * @return XML fragment documenting the option.
   */
  public static String parseOption(String orig, boolean optional)
  {
    String[] tokens = orig.split("\\|");
    String option = optional ?
        "[--" + tokens[0] + ", -" + tokens[1] +"]" :
        "--" + tokens[0] + ", -" + tokens[1];
    String desc = tokens[tokens.length - 1].replaceAll("\\&pipe;", "|")
        .replaceAll("<","&lt;");
    return
        "    <varlistentry>\n" +
        "     <term><option>" + option + "</option></term>\n" +
        "     <listitem><para>" + desc + "</para></listitem>\n" +
        "    </varlistentry>\n";
  }
}
