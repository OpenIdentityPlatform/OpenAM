/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: WarCreator.java,v 1.2 2009/08/18 16:08:55 kevinserwin Exp $
 */
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tools.deployablewar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * This class creates the specialized WAR such as distributed authentication and
 * console WAR files.
 */
public class WarCreator {
    private static ResourceBundle rb;
    private static final Set<String> supportedTypes = new HashSet<String>();

    static {
        supportedTypes.add("console");
        supportedTypes.add("distauth");
        supportedTypes.add("noconsole");
        supportedTypes.add("idpdiscovery");
    }

    public static void main(String[] args) {
        // if locale is null, default locale will be used.
        String locale = getOption(args, "-l", "--locale");
        rb = ResourceBundle.getBundle("deployablewar", getLocale(locale));

        // print usage and quit.
        if (args.length == 0 || hasOption(args, "-?", "--help")) {
            System.out.println();
            System.out.println(rb.getString("usage"));
            System.exit(0);
        }

        // validation.
        String staging = getOption(args, "-s", "--staging");
        String type = getOption(args, "-t", "--type");
        String warfile = getOption(args, "-w", "--warfile");

        if ((staging == null) || (type == null) || (warfile == null)) {
            System.err.println();
            System.err.println(rb.getString("usage"));
            System.exit(1);
        }

        File stagingDir = new File(staging);
        File webinfDir = new File(staging + File.separator + "WEB-INF");
        if (!stagingDir.exists() || !stagingDir.canRead() || !webinfDir.exists()) {
            System.err.println();
            System.err.println(rb.getString("invalid.staging.dir"));
            System.exit(1);
        }

        File typeDir = new File(type);
        if (!typeDir.exists() || !typeDir.canRead()) {
            System.err.println();
            System.err.println(rb.getString("missing.typedir"));
            System.exit(1);
        }

        if (!supportedTypes.contains(type)) {
            System.err.println();
            System.err.println(rb.getString("unsupported.type"));
            System.exit(1);
        }

        // options are all valid, create the WAR now
        create(staging, type, warfile);
    }

    private static boolean hasOption(
        String[] args,
        String shortName,
        String longName
    ) {
        for (String arg : args) {
            if (arg.equals(shortName) || arg.equals(longName)) {
                return true;
            }
        }
        return false;
    }

    private static String getOption(
        String[] args,
        String shortName,
        String longName
    ) {
        for (int i = 0; i < (args.length - 1); i++) {
            if (args[i].equals(shortName) || args[i].equals(longName)) {
                return (args[i + 1].startsWith("-")) ? null : args[i + 1];
            }
        }
        return null;
    }

    private static void create(String staging, String type, String warfile) {
        try {
            JarOutputStream out = new JarOutputStream(new FileOutputStream(
                warfile));
            byte[] buf = new byte[1024];

            // get the contents from mother opensso.war
            List<String> fileList = getFileList("fam-" + type + ".list");
            for (String f : fileList) {
                File test = new File(staging + "/" + f);
                if (test.exists()) {
                    FileInputStream in = new FileInputStream(staging + "/" + f);
                    out.putNextEntry(new JarEntry(f));

                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                } else {
                    System.err.println(MessageFormat.format(rb.getString("missing.file"), test));
                }
            }

            // get the contents for individual specialized WAR
            fileList = getTargetedList(type, type);
            for (String f : fileList) {
                f = f.replaceAll("\\" + System.getProperty("file.separator"), "/");
                out.putNextEntry(new JarEntry(f));
                FileInputStream in = new FileInputStream(type + "/" + f);

                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.closeEntry();
            }

            out.close();

            System.out.println();
            System.out.println(rb.getString("warfile.created"));
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static List<String> getTargetedList(String type, String base)
        throws IOException {
        List<String> list = new ArrayList<String>();
        File dir = new File(type);
        File[] files = dir.listFiles();

        for (File f : files) {
            if (f.isDirectory()) {
                list.addAll(getTargetedList(type + "/" + f.getName(), base));
            } else {
                list.add(f.getPath().substring(base.length() + 1));
            }
        }

        return list;
    }

    private static List<String> getFileList(String listName)
        throws IOException {
        List<String> list = new ArrayList<String>();
        FileReader frdr = null;

        try {
            frdr = new FileReader(listName);
            BufferedReader brdr = new BufferedReader(frdr);
            String line = brdr.readLine();

            while (line != null) {
                if (line.startsWith("./")) {
                    line = line.substring(2);
                }
                list.add(line);
                line = brdr.readLine();
            }
        } finally {
            if (frdr != null) {
                try {
                    frdr.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return list; 
    }

    private static Locale getLocale(String stringformat) {
        if (stringformat == null) {
            return Locale.getDefault();
        }

        StringTokenizer tk = new StringTokenizer(stringformat, "_");
        String lang = "";
        String country = "";
        String variant = "";

        if (tk.hasMoreTokens())
            lang = tk.nextToken();
        if (tk.hasMoreTokens())
            country = tk.nextToken();
        if (tk.hasMoreTokens())
            variant = tk.nextToken();

        return new Locale(lang, country, variant);
    }
}
