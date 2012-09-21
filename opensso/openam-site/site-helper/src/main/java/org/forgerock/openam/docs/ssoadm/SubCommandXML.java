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

import java.util.Collection;

/**
 * Each ssoadm subcommand annotation is transformed to a DocBook 5 XML
 * fragment for inclusion in the ssoadm refentry.
 *
 * <pre>
 * {@code
 * <refsect2 xml:id="ssoadm-subcommand">
 *  <title>ssoadm subcommand</title>
 *  <para>description</para>
 *  <para>Usage: <literal>ssoadm subcommand --options [--global-options]</literal></para>
 *  <variablelist>
 *   <title>Options</title>
 *   <varlistentry>
 *    <term><option>--adminid, -u</option></term><!-- Mandatory -->
 *    <listitem><para>Administrator ID of running the command.</para></listitem>
 *   </varlistentry>
 *   <varlistentry>
 *    <term><option>[--adminid, -u]</option></term><!-- Optional -->
 *    <listitem><para>Administrator ID of running the command.</para></listitem>
 *   </varlistentry>
 *  </variablelist>
 * </refsect2>
 * }
 * </pre>
 */

public class SubCommandXML
{
  /**
   * Transform SubCommandInfo annotation content to an XML fragment.
   *
   * @param name Subcommand name
   * @param desc Subcommand description string
   * @param mandatory Subcommand mandatory options
   * @param optional Subcommand optional options
   * @return XML fragment documenting the annotation.
   */
  public static String parseSubCommandInfo(String name, String desc,
      Collection<String> mandatory, Collection<String> optional)
  {
    String description = desc.replaceAll("<", "&lt;");
    String req = "";
    for (String m : mandatory) req += OptionXML.parseOption(m, false);
    String opt = "";
    for (String o : optional) opt += OptionXML.parseOption(o, true);
    return
        "  <refsect2 xml:id=\"ssoadm-" + name + "\">\n" +
        "   <title>ssoadm " + name + "</title>\n" +
        "   <para>" + description + "</para>\n" +
        "   <para>Usage: <literal>ssoadm " + name +
          " --options [--global-options]</literal></para>\n" +
        "   <variablelist>\n" +
        "    <title>Options</title>\n" +
        req +
        opt +
        "   </variablelist>\n" +
        "  </refsect2>";
  }
}
