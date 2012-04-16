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
 * $Id: StreamSubstituter.java,v 1.2 2008/06/25 05:42:05 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.am.util.UnicodeInputStreamReader;

/**
 * Copies a Reader to a Writer performing string substitution.
 *
 */
public class StreamSubstituter {
    Map ht = new HashMap();
    // cache for files, key is the file name, value is the StringBuffer 
    // representation of the file 
    private static Map fileMap = new HashMap();

    /**
     * Define a string to substitute.
     * In the content being filtered, all occurrences of ...
     * <pre>
     *     &lt;subst data="key">otherwise&lt;/subst>
     * </pre>
     * will be replaced with the value defined for "key".
     * If the key has not been defined,
     * then "otherwise" will be copied to the output stream.
     * More than one <code>date="key"</code> may be specified in the same
     * &lt;subst> tag in which case they are substituted in order, and
     * "otherwise" is only copied if none of the keys were found.
     * @param key file name used as key for cache
     * @param val string value for representation of the file 
     */
    public void define(String key, String val) {
        ht.put(key, val);
    }

    private static final int st_normal = 0;
    private static final int st_tagId = 1;
    private static final int st_subst = 2;
    private static final int st_subst_attr = 3;
    private static final int st_subst_value = 4;
    private static final int st_subst_value_data = 5;
    private static final int st_subst_body = 6;
    private static final int st_subst_body_tagId = 7;
    //private static Debug debug = AuthD.getAuth().debug;

    /**
     * Load file from file system
     * @param file <code>File</code> object will be cached
     * @return StringBuffer representation of the file 
     * @throws IOException if can not read contents of file.
     */
    private StringBuffer loadFile(File file) 
        throws IOException
    {
        String fileName = file.toString();
        StringBuffer sb = null;
        synchronized(fileMap) {
            sb = (StringBuffer) fileMap.get(fileName);
        }
        if (sb != null) {
            return sb;
        }
        BufferedReader in = null;
        try {
            if (!file.canRead()) {
                //debug.error("Can read file " + fileName);
                throw new IOException("Can read file " + fileName);
            }
            in = new BufferedReader(new UnicodeInputStreamReader(new 
                 FileInputStream(file)));
            sb = new StringBuffer(2000);
            int ch;
            while ((ch = in.read()) != -1) {
                sb.append((char) ch);
            }    
            // add new file to fileMap
            synchronized(fileMap) {
                fileMap.put(fileName, sb);
            }
            return sb;
        } finally {
            if (in != null) {
                in.close();
            }
        }

    }

    /**
     * Filters data from file.
     *
     * @param inputFile that filter will be applied.
     * @param o output stream filtered content will be written.
     * @throws IOException if content can not be read from
     */
    public void filter(File inputFile, PrintWriter o)
        throws IOException
    {
        BufferedWriter bw = new BufferedWriter(o);
        StringBuffer sb = (StringBuffer) fileMap.get(inputFile.toString()); 
        if (sb == null) {
            sb = loadFile(inputFile);
        }
        filter(sb, bw);
        bw.flush();
    }

    /**
     * Filters data from BufferedReader.
     * @param sb stringbuffer has content
     * @param o output stream filtered content will be written.
     * @throws IOException if content can not be written out
     */
    private void filter(StringBuffer sb, BufferedWriter o)
        throws IOException {
        int ch;
        StringBuilder buf = new StringBuilder();
        int state = st_normal;
        boolean shouldCopyBody = false;
    
        int size = sb.length();
        for (int i = 0; i < size; i++) {
            ch = sb.charAt(i);
            switch (state) {
            case st_normal:
                // xxxx<subst data="xxx">xxxx</subst>xxxx
                // -----                             ----
                if (ch == '<') {
                    state = st_tagId;
                    buf.setLength(0);
                    buf.append((char) ch);
                    break;
                }
                o.write(ch);
                break;
            case st_tagId:
                // xxxx<subst data="xxx">xxxx</subst>xxxx
                //      ------
                buf.append((char) ch);
                if (ch == '>') {
                    state = st_normal;
                    o.write(buf.toString());
                    break;
                }
                if (buf.length() == 7) {
                    if (buf.toString().equalsIgnoreCase("<subst ")) {
                        state = st_subst_attr;
                        buf.setLength(0);
                        shouldCopyBody = true;
                        break;
                    }
                    state = st_normal;
                    o.write(buf.toString());
                    break;
                }
                break;
            case st_subst_attr:
                // xxxx<subst  xxxx="xxx">xxxx</subst>xxxx
                //            ------
                switch (ch) {
                case ' ':
                    break;
                case '=':
                    if (buf.toString().equalsIgnoreCase("data")) {
                        state = st_subst_value_data;
                        buf.setLength(0);
                        break;
                    }
                    state = st_subst_value;
                    buf.setLength(0);
                    break;
                case '>':
                    state = st_subst_body;
                    break;
                default:
                    buf.append((char) ch);
                    break;
                }
                break;
            case st_subst_value:
            case st_subst_value_data:
                // xxxx<subst xxxx="xxx">xxxx</subst>xxxx
                //                 -----
                if (ch == '"' || ch == '\'') {
                    if (buf.length() > 0) {
                        if (state == st_subst_value_data) {
                            String val = (String) ht.get(buf.toString());
                            if (val != null) {
                                o.write(val);
                                shouldCopyBody = false;
                            }
                        }
                        state = st_subst_attr;
                        buf.setLength(0);
                        break;
                    }
                    break;
                }
                buf.append((char) ch);
                break;
            case st_subst_body:
                // xxxx<subst data="xxx">xxxx</subst>xxxx
                //                       -----
                if (ch == '<') {
                    state = st_subst_body_tagId;
                    buf.setLength(0);
                    buf.append((char) ch);
                    break;
                }
                if (shouldCopyBody)
                    o.write(ch);
                break;
            case st_subst_body_tagId:
                // xxxx<subst data="xxx">xxxx</subst>xxxx
                //                            -------
                buf.append((char) ch);
                if (buf.length() == 8) {
                    if (buf.toString().equalsIgnoreCase("</subst>")) {
                        state = st_normal;
                        buf.setLength(0);
                        shouldCopyBody = false;
                        break;
                    }
                    state = st_subst_body;
                    if (shouldCopyBody)
                        o.write(buf.toString());
                    break;
                }
                if (ch == '>') {
                    state = st_subst_body;
                    if (shouldCopyBody)
                        o.write(buf.toString());
                    break;
                }
                break;
            default:
                throw new Error("StreamSubstituter: bad state");
            }
        }
    }

    /**
     * Get file content from file to string buffer
     * @param filename will be read from
     * @return StringBuffer representation of file content
     * @throws Exception if file can not be read from.
     */
    public StringBuffer getFileContents(File filename) throws Exception {
         StringBuffer fileContents = new StringBuffer();
         try {
                fileContents = loadFile(filename);
         } catch (Exception e) {
                throw new Exception("Error reading file"+ e);
         }

        return fileContents;
    }
}
