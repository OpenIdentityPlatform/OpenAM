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
 * $Id: NightlyIndexHtml.java,v 1.5 2008/06/25 05:44:13 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tools.nightly;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.text.MessageFormat;

/**
 * Nightly Preparation Implementation.
 */
public class NightlyIndexHtml {
    private final static String TEMPLATE =
        "\n&raquo;&nbsp;<a href=\"{0}\">{0}</a><br />";
    private final static String NEW_ENTRY = "<!-- new entry -->";
    private final static String LATEST = "<a href=\"{0}\">latest</a>";

    /**
     * Creates a new instance of <code>NightlyIndexHtml</code>.
     */
    private NightlyIndexHtml() {
    }
    
    private static void create(
        String baseDir,
        String timestamp,
        String indexURL
    ) throws IOException, MalformedURLException {
        URL url = new URL(indexURL);
        URLConnection conn = url.openConnection();
        DataInputStream dis = new DataInputStream(conn.getInputStream());
        StringBuilder buff = new StringBuilder();

        String line = dis.readLine();
        while (line != null) {
            buff.append(line).append("\n");
            line = dis.readLine();
        }
        dis.close();

        String content = buff.toString();
        Object[] param = {timestamp};
        content = content.replaceAll(
            NEW_ENTRY, NEW_ENTRY + MessageFormat.format(TEMPLATE, param));
        content = content.replaceAll(
            "<a href=.+?>latest</a>",
            MessageFormat.format(LATEST, param));
        writeToFile(baseDir + "/top.index.html", content);
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
            create(args[0], args[1], args[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
