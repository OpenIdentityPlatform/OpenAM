/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WebHelperCreator.java,v 1.5 2008/09/09 18:15:41 veiming Exp $
 *
 */

package com.sun.identity.federation.cli.tools;

import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.ResourceStrings;
import com.sun.identity.cli.annotation.SubCommandInfo;
import com.sun.identity.cli.definition.AccessManager;
import com.sun.identity.federation.cli.definition.FederationManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebHelperCreator {
    /**
     * Generates CLI Definition class.
     *
     * @param argv Array of annotated class names.
     */
    public static void main(String[] argv) {
        if (argv.length < 1) {
            System.err.println("clihelper-output system property is not set");
            System.exit(1);
        }

        String outfile = argv[0];

        try {
            Class[] classes = {AccessManager.class, FederationManager.class};
            StringBuffer buff = new StringBuffer();
            getCommands(classes, buff);

            BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
            out.write(buff.toString());
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void getCommands(Class[] classes, StringBuffer buff)
        throws Exception {
        buff.append("package com.sun.identity.qatest.common;\n\n");

        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlForm;\n");
        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;\n");
        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;\n");
        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlPage;\n");
        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlSelect;\n");
        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlTextArea;\n");
        buff.append(
            "import com.gargoylesoftware.htmlunit.html.HtmlTextInput;\n");
        buff.append("import com.gargoylesoftware.htmlunit.WebClient;\n");
        buff.append("import java.io.BufferedReader;\n");
        buff.append("import java.io.FileReader;\n");
        buff.append("import java.lang.StringBuffer;\n");
        buff.append("import java.net.URL;\n");
        buff.append("import java.util.Iterator;\n");
        buff.append("import java.util.List;\n\n");

        buff.append("public class FederationManager {\n");
        buff.append("    private String amadmUrl;\n");
        buff.append("    private String amUrl;\n\n");

        buff.append("    public FederationManager(String url) {\n");
        buff.append("        amUrl = url;\n");
        buff.append("        amadmUrl = url + \"/ssoadm.jsp?cmd=\";\n");
        buff.append("    }\n\n\n");

        buff.append("    public static int getExitCode(HtmlPage p) {\n");
        buff.append("        int val = -1;\n");
        buff.append("        String content = ")
            .append("p.getWebResponse().getContentAsString();\n");
        buff.append("        int start = content.indexOf(\"")
            .append("<!-- CLI Exit Code: \");\n");
        buff.append("        if (start != -1) {\n");
        buff.append("            int end = content.indexOf(\"")
            .append("-->\", start);\n");
        buff.append("            if (end != -1) {\n");
        buff.append("                String exitCode = ")
            .append("content.substring(start+20, end-1);\n");
        buff.append("                val = Integer.parseInt(exitCode);\n");
        buff.append("            }\n");
        buff.append("        }\n");
        buff.append("        return val;\n");
        buff.append("    }\n");

        for (int i = 0; i < classes.length; i++) {
            Class clazz = classes[i];
            Field[] fields = clazz.getDeclaredFields();

            for (Field fld : fields) {
                SubCommandInfo info = fld.getAnnotation(SubCommandInfo.class);

                if (info != null) {
                    if ((info.implClassName() == null) ||
                        (info.description() == null)
                    ) {
                        throw new Exception("Incorrect Definition, " +
                            "class=" + clazz.getName() + " field=" +
                                fld.toString());
                    }

                    if (info.webSupport().equals("true")) {
                        List<String> mandatoryOptions = toList(
                            info.mandatoryOptions());
                        List<String> optionalOptions = toList(
                            info.optionalOptions());
                        List<String> optionAliases = toList(
                            info.optionAliases());

                        if ((info.macro() != null) &&
                            (info.macro().length() > 0)
                        ) {
                            Field fldMarco = clazz.getDeclaredField(
                                info.macro());
                            Macro macroInfo =(Macro)fldMarco.getAnnotation(
                                Macro.class);
                            appendToList(mandatoryOptions, 
                                macroInfo.mandatoryOptions());
                            appendToList(optionalOptions,
                                macroInfo.optionalOptions());
                            appendToList(optionAliases,
                                macroInfo.optionAliases());
                        }
                
                        WebHelperMethodCreator wMethod =
                            new WebHelperMethodCreator(fld.getName(),
                                info.description(), mandatoryOptions, 
                                optionalOptions, optionAliases);
                        wMethod.genMethod(buff);
                    }
                }
            }
        }
        buff.append("}\n");
    }

    /**
     * Returns a list of string by adding string in an array to it.
     *
     * @param array Array of String.
     * @return a list of string.
     */
    public static List<String> toList(String[] array) {
        List<String> list = new ArrayList<String>();
        if ((array != null) && (array.length > 0)) {
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
        return list;
    }

    /**
     * Adds string in an array to a list.
     *
     * @param list List of string where new string are to be added.
     * @param array Array of String.
     */
    public static void appendToList(List<String> list, String[] array) {
        if ((array != null) && (array.length > 0)) {
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
    }
}
