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
 * $Id: Diffs.java,v 1.2 2008/06/25 05:53:08 qcheng Exp $
 *
 */

package com.sun.identity.tools.diffs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CVS diff utility.
 */
public class Diffs {
    
    /**
     * Creates a new instance of <code>Diffs</code>.
     */
    private Diffs() {
    }
    
    /**
     * Breaks the bulk cvs diff file into individual files.
     *
     * @param filename File name of the bulk diff.
     */
    public static void chop(String filename)
        throws IOException
    {
        int fileIdx = filename.lastIndexOf('/');
        String baseDir = (fileIdx == -1) ? "./" 
            : filename.substring(0, fileIdx+1);
        File fileHandle = new File(filename);
        FileReader in = null;
               
        try {
            in = new FileReader(filename);
            BufferedReader buff = new BufferedReader(in);
            StringBuffer sb = new StringBuffer();
            String curFileName = null;
            String line = buff.readLine();
            
            while (line != null) {
                int idx = line.indexOf("Index: ");
                if (idx == 0) {
                    writeToFile(curFileName, sb.toString());
                    curFileName = baseDir + normalizeFilename(
                        line.substring(7));
                    sb = new StringBuffer();
                }
                
                if (curFileName != null) {
                    sb.append(line).append("\n");
                }
                line = buff.readLine();
            }
            
            writeToFile(curFileName, sb.toString());
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
    }
    
    private static String normalizeFilename(String fileName) {
        fileName = fileName.replace('/', '.');
        return fileName + ".diff";
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
            chop(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
