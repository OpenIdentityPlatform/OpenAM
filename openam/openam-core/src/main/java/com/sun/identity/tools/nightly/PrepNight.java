/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrepNight.java,v 1.4 2008/06/25 05:44:13 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tools.nightly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nightly Preparation Implementation.
 */
public class PrepNight {
    private final static String HEADER =
        "<html><head><title>OpenSSO - Download</title><link rel=\"stylesheet\" href=\"https://opensso.dev.java.net/opensso.css\" /></head><body><p><img src=\"https://opensso.dev.java.net/images/openssoDownload.gif\" width=\"146\" height=\"64\" /></p>";
    private final static String FOOTER = "</body></html>";

    private final static String TEMPLATE =
        "&raquo;&nbsp;<a href=\"{0}\">{0}</a>";
    /**
     * Creates a new instance of <code>Stat</code>.
     */
    private PrepNight() {
    }
    
    private static void createIndexHTML(String baseDir)
        throws IOException
    {
        Set fileNames = getFileContent(baseDir);
        StringBuilder buff = new StringBuilder();
        buff.append(HEADER);

        for (Iterator i = fileNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object[] param = {name};
            buff.append(MessageFormat.format(TEMPLATE, param))
                .append("<br />");
        }

        buff.append(FOOTER);
        writeToFile(baseDir + "/index.html", buff.toString());
    }

    private static Set getFileContent(String baseDir)
        throws IOException
    {
        Set binaries = new TreeSet();
        File dir = new File(baseDir);
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            String name = baseDir + "/"  + files[i];
            if ((new File(name)).isFile()) {
                binaries.add(files[i]);
            }
        }
        return binaries;
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
            createIndexHTML(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
