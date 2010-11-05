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
 * $Id: Stat.java,v 1.2 2008/06/25 05:53:09 qcheng Exp $
 *
 */

package com.sun.identity.tools.diffs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * CVS stat utility.
 */
public class Stat {

    private static final String MODIFIED = "Status: Locally Modified";
    private static final String ADDED = "Status: Locally Added";
    private static final String REMOVED = "Status: Locally Removed";
    private static final String TAG_MODIFIED = "M";
    private static final String TAG_ADDED = "A";
    private static final String TAG_REMOVED = "R";
    private static final String TEMPLATE = "{0} {1}\n";
    
    /**
     * Creates a new instance of <code>Stat</code>.
     */
    private Stat() {
    }
    
    /**
     * Discovers the stat.
     *
     * @param filename File name of the stat.
     */
    public static void discover(String filename)
        throws IOException
    {
        int fileIdx = filename.lastIndexOf('/');
        String baseDir = (fileIdx == -1) ? "./" 
            : filename.substring(0, fileIdx+1);
        String content = getFileContent(filename);
        StringBuffer buff = new StringBuffer();
        discover(content, ADDED, TAG_ADDED, buff);
        discover(content, REMOVED, TAG_REMOVED, buff);
        discover(content, MODIFIED, TAG_MODIFIED, buff);
        writeToFile(baseDir + "stat", buff.toString());
    }

    private static void discover(
        String content,
        String tag,
        String marker,
        StringBuffer buff
    ) {
        int idx = content.indexOf(tag);
        while (idx != -1) {
            int idx2 = content.lastIndexOf("File: ", idx);
            String fileName = null;

            if (idx2 != -1) {
                fileName = content.substring(idx2, idx);

                if (!tag.equals(ADDED)) {
                    int i = content.indexOf("Repository revision", idx);
                    if (i != -1) {
                        i = content.indexOf("/cvs/", i);
                        if (i != -1) {
                            int j = content.indexOf(",v", i);
                            if (j != -1) {
                                fileName = content.substring(i +5, j);
                            }
                        }
                    }
                }

                Object[] params = {marker, fileName};
                buff.append(MessageFormat.format(TEMPLATE, params));
            }
            idx = content.indexOf(tag, idx+1);
        }
    }
    
    private static String getFileContent(String filename)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        File fileHandle = new File(filename);
        FileReader in = null;
               
        try {
            in = new FileReader(filename);
            BufferedReader buff = new BufferedReader(in);
            String curFileName = null;
            String line = buff.readLine();
            
            while (line != null) {
                sb.append(line).append("\n");
                line = buff.readLine();
            }
            
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return sb.toString();
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
            discover(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
