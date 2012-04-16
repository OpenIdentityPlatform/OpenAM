/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MergeSDKProperties.java,v 1.2 2008/06/25 05:44:12 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tools.nightly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Merges <code>Services.properties</code>, <code>DSConfig.properties</code>,
 * <code>ums.properties</code>, <code>sso.properties</code>,
 * <code>SMS.properties</code> and <code>authentication_util.properties</code>
 * to <code>amSDK.properties</code> for all locales.
 */
public class MergeSDKProperties {
    private static Set propertyFileNames = new HashSet();
    private static final String AM_SDK = "amSDK";
    private DirFilter dirFilter = new DirFilter();
    private LocaleFileFilter localeFileFilter = new LocaleFileFilter();

    static {
        propertyFileNames.add("Services");
        propertyFileNames.add("DSConfig");
        propertyFileNames.add("ums");
        propertyFileNames.add("sso");
        propertyFileNames.add("SMS");
        propertyFileNames.add("authentication_util");
    }

    private MergeSDKProperties() {
    }
    

    /*
     * Gets all the sub directories under resources/locale
     * For each sub directory 
     *   combine the locale files contents and write it to SDK properties
     *   file.
     */
    private void create(
        String baseDir,
        String targetDir
    ) throws IOException {
        File dir = new File(baseDir);
        File[] directories = dir.listFiles(dirFilter);
        for (int i = 0; i < directories.length; i++) {
            File d = directories[i];
            mergeFiles(d, targetDir);
        }
    }

    private void mergeFiles(File dirLocale, String targetDir)
        throws IOException
    {
        Map map = new HashMap();
        getLocaleFiles(dirLocale, map);
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String locale = (String)i.next();
            String content = getMergeContent((Set)map.get(locale));
            writeToFile(targetDir + File.separator + AM_SDK + "_" + locale +
                ".properties", content);
        }
    }

    private String getMergeContent(Set files)
        throws IOException {
        StringBuilder buff = new StringBuilder();
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            buff.append(readFile((String)i.next()));
            buff.append("\n");
        }
        return buff.toString();
    }

    private void getLocaleFiles(File dirLocale, Map map) {
        File[] files = dirLocale.listFiles(localeFileFilter);
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String path = f.getPath();
            path = path.substring(path.lastIndexOf(File.separatorChar) +1);
            path = path.substring(0, path.indexOf(".properties"));
            String locale = null;

            for (Iterator j = propertyFileNames.iterator();
                j.hasNext() && (locale == null); ) {
                String m = (String)j.next();
                if (path.startsWith(m + "_")) {
                    locale = path.substring(m.length() +1);
                }
            }
            
            Set set = (Set)map.get(locale);
            if (set == null) {
                set = new HashSet();
                map.put(locale, set);
            }
            set.add(f.getPath());
        }
    }

    private static String readFile(String filename)
        throws IOException {
        StringBuilder buff = new StringBuilder();
        FileReader input = new FileReader(filename);
        BufferedReader bufRead = new BufferedReader(input);

        try {
            String line = bufRead.readLine();
            while (line != null){
                buff.append(line).append("\n");
                line = bufRead.readLine();
            }
        } finally {
            bufRead.close();
        }
        return buff.toString();
    }

    private static void writeToFile(String filename, String content)
        throws IOException
    {
        if (filename != null) {
            File fileHandle = new File(filename);
            FileWriter out = null;
            try {
                out = new FileWriter(filename);
                out.write(content);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            (new MergeSDKProperties()).create(args[0], args[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class DirFilter implements FileFilter {
        public boolean accept(java.io.File a) {
            return a.isDirectory() && !(a.getPath().endsWith("/CVS"));
        }
    }

    public class LocaleFileFilter implements FileFilter {
        public boolean accept(java.io.File a) {
            if (a.isFile() && a.getPath().endsWith(".properties")) {
                String path = a.getPath();
                path = path.substring(path.lastIndexOf(File.separatorChar) +1);
                path = path.substring(0, path.indexOf(".properties"));

                for (Iterator i = propertyFileNames.iterator(); i.hasNext(); ) {
                    String m = (String)i.next();
                    if (path.startsWith(m + "_")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
