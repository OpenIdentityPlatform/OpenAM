/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CLIDefinitionGenerator.java,v 1.9 2008/06/25 05:42:24 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.tools;

import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.ResourceStrings;
import com.sun.identity.cli.annotation.SubCommandInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class generates a CLI Definition class from another annotated class.
 */
public class CLIDefinitionGenerator {
    private static Map<String, String> mapLongToShortOptionName = 
        new HashMap<String, String>();

    /**
     * Generates CLI Definition class.
     *
     * @param argv Array of annotated class names.
     */
    public static void main(String[] argv) {
        String resourceDir = argv[0];

        for (int i = 1; i < argv.length; i++) {
            String className = argv[i];

            try {
                Class clazz = Class.forName(className);
                Field pdtField = clazz.getDeclaredField("product");

                if (pdtField != null) {
                    DefinitionClassInfo classInfo = pdtField.getAnnotation(
                        DefinitionClassInfo.class);

                    try {
                        PrintStream rbOut = createResourcePrintStream(
                            resourceDir, classInfo);
                        getCommonResourceStrings(rbOut, clazz);
                        rbOut.println("product-name=" +
                            classInfo.productName());
                        getCommands(className, clazz, rbOut);
                        rbOut.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else {
                    throw new Exception("Incorrect Definiton, " +
                        "class=" + className + " missing product field"); 
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void getCommonResourceStrings(
        PrintStream rbOut,
        Class clazz
    ) throws Exception {
        Field field = clazz.getDeclaredField("resourcestrings");

        if (field != null) {
            ResourceStrings resStrings = field.getAnnotation(
                ResourceStrings.class);
            List list = toList(resStrings.string());
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                rbOut.println((String)i.next());
            }
        }
    }

    private static PrintStream createResourcePrintStream(
        String resourceDir,
        DefinitionClassInfo classInfo
    ) throws IOException {
        String filePath = resourceDir + File.separator + 
            classInfo.resourceBundle() + ".properties";
        File rbFile = new File(filePath);
        rbFile.createNewFile();
        FileOutputStream rbStream = new FileOutputStream(rbFile);
        return new PrintStream(rbStream);
    }

    private static void getCommands(
        String className,
        Class clazz,
        PrintStream rbOut
    ) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        for (Field fld : fields) {
            SubCommandInfo info = fld.getAnnotation(SubCommandInfo.class);
            if (info != null) {
                if ((info.implClassName() == null) ||
                    (info.description() == null)
                ) {
                    throw new Exception("Incorrect Definition, " +
                        "class=" + className + " field=" + fld.toString());
                }

                List mandatoryOptions = toList(info.mandatoryOptions());
                List optionalOptions = toList(info.optionalOptions());
                List optionAliases = toList(info.optionAliases());

                if ((info.macro() != null) && (info.macro().length() > 0)) {
                    Field fldMarco = clazz.getDeclaredField(info.macro());
                    Macro macroInfo =(Macro)fldMarco.getAnnotation(Macro.class);
                    appendToList(mandatoryOptions, 
                        macroInfo.mandatoryOptions());
                    appendToList(optionalOptions, macroInfo.optionalOptions());
                    appendToList(optionAliases, macroInfo.optionAliases());
                }
                
                validateOption(mandatoryOptions);
                validateOption(optionalOptions);

                String subcmdName = fld.getName().replace('_', '-');

                String resPrefix = "subcmd-" + subcmdName;
                String desc = info.description().replaceAll("&#124;", "|");
                rbOut.println(resPrefix +  "=" + desc);
                createResourceForOptions(resPrefix + "-",
                    mandatoryOptions, rbOut);
                createResourceForOptions(resPrefix + "-",
                    optionalOptions, rbOut);
                addResourceStrings(toList(info.resourceStrings()), rbOut);
            }
        }
    }
    
    private static void validateOption(List options) {
        for (Iterator i = options.iterator(); i.hasNext(); ) {
            String option = (String)i.next();
            int idx = option.indexOf('|');
            String longName = option.substring(0, idx);
            String shortName = option.substring(idx+1, idx+2);

            String test = (String)mapLongToShortOptionName.get(longName);
            if (test == null) {
                mapLongToShortOptionName.put(longName, shortName);
            } else if (!test.equals(shortName)) {
                throw new RuntimeException(
                    "Mismatched names: " + longName + "-> " + test + ", " +
                    shortName);
            }
        }
    }

    private static void addResourceStrings(List res, PrintStream rbOut){
        for (Iterator i = res.iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            rbOut.println(s);
        }
    }

    private static void createResourceForOptions(
        String prefix,
        List options,
        PrintStream rbOut
    ) {
        for (Iterator i = options.iterator(); i.hasNext(); ) {
            String option = (String)i.next();
            String opt = option.substring(0, option.indexOf('|'));
            String description = option.substring(option.lastIndexOf('|') +1);
            String webDescription = null;
            int idxWeb = description.indexOf("<web>");
            if (idxWeb != -1) {
                rbOut.println(prefix + "__web__-" + opt + "="
                    + description.substring(idxWeb + 5));
                description = description.substring(0, idxWeb);
            }

            rbOut.println(prefix + opt + "=" + description);
        }
    }
    
    /**
     * Returns a list of string by adding string in an array to it.
     *
     * @param array Array of String.
     * @return a list of string.
     */
    public static List toList(String[] array) {
        List list = new ArrayList();
        if ((array != null) && (array.length > 0)) {
            list.addAll(Arrays.asList(array));
        }
        return list;
    }

    /**
     * Adds string in an array to a list.
     *
     * @param list List of string where new string are to be added.
     * @param array Array of String.
     */
    public static void appendToList(List list, String[] array) {
        if ((array != null) && (array.length > 0)) {
            list.addAll(Arrays.asList(array));
        }
    }
}
