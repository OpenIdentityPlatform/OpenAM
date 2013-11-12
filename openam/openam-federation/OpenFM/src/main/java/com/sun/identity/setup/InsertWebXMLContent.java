/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: InsertWebXMLContent.java,v 1.1 2009/11/10 08:37:28 mrudul_uchil Exp $
 *
 */

package com.sun.identity.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a portion of one file and insert into another file. The portion
 * to be extracted is defined in a Pattern; and the location to be inserted
 * is marked with a Pattern.
 */
public class InsertWebXMLContent {
    private Pattern pattern;

    /**
     * Creates an instance of this class.
     *
     * @param pattern Pattern defining the portion to be extracted.
     */
    public InsertWebXMLContent(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Inserts a portion of one file and insert into another file.
     *
     * @param extFile File name of which content is to be extracted.
     * @param origFileName File name of which content is be inserted.
     * @throws IOException if IO operations failed.
     */
    public void inserts(String extFile, String origFileName)
        throws IOException {
        String content = getMatched(extFile);
        if ((content != null) && (content.length() > 0)) {
            String orig = getFileContentInserted(origFileName,content);
            writeToFile(origFileName, orig);
        }
    }

    private String getMatched(String fileName)
        throws IOException {
        StringBuffer buff = new StringBuffer();
        String content = getFileContentInserted(fileName,null);
        Matcher m = pattern.matcher(content);
        while (m.find()) {
            buff.append(m.group()).append("\n");
        }
        return buff.toString();
    }

    private String getFileContentInserted(String fileName, String content)
        throws IOException {
        StringBuffer sb = new StringBuffer();
        FileReader in = null;
        boolean insertDone = false;

        try {
            in = new FileReader(fileName);
            BufferedReader buff = new BufferedReader(in);
            String line = buff.readLine();
            while (line != null) {
                String newLine = line.trim();
                if ((!newLine.startsWith("<web-app")) && 
                    (!newLine.startsWith("<?")) &&
                    (!newLine.endsWith("-->")) &&
                    (content != null) &&
                    (!insertDone) &&
                    (!newLine.startsWith("<!--")) &&
                    (newLine.startsWith("<"))) {
                    sb.append(content).append("\n");
                    insertDone = true;
                }
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
        throws IOException {
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
            InsertWebXMLContent it =
                new InsertWebXMLContent(Pattern.compile(args[0],
                Pattern.MULTILINE | Pattern.DOTALL));
            it.inserts(args[1], args[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

