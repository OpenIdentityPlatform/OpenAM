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
 * $Id: MergeClientConfig.java,v 1.2 2008/06/25 05:50:00 qcheng Exp $
 *
 */

package com.sun.identity.setup;

import com.sun.identity.common.SystemConfigurationUtil;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.StringBuffer;

/**
 * Merge client properties from two files into one with comments retained.
 */
public class MergeClientConfig {
    
    private MergeClientConfig() {
    }
    
    public static void main(String[] args) {
        try {
            StringBuffer file1 = getInputStringBuffer(args[0], false);
            StringBuffer file2 = getInputStringBuffer(args[1], true);

            writeToFile(file1, file2, args[2]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Returns input file as StringBuffer.
     * @param filename Name of the file.
     * @param skipCopyright if false, keep copyright notice in the beginning
     *        of the input file. if true, remove the copyright notice.
     * @return StringBuffer
     */
    private static StringBuffer getInputStringBuffer(String filename, 
        boolean skipCopyright)
        throws Exception {
        StringBuffer buff = new StringBuffer(20480);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename)));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (skipCopyright && line.startsWith("#")) {
                    continue;
                } 
                // done skipping the copyright in the beginning of the file
                skipCopyright = false;
                buff.append(line).append("\n");
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return buff;
    }

    private static void writeToFile(StringBuffer file1, StringBuffer file2, 
        String filename) throws FileNotFoundException, IOException
    {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filename);
            fout.write(file1.toString().getBytes());
            fout.write(file2.toString().getBytes());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

