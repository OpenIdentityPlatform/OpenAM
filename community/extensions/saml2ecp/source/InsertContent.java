/* The contents of this file are subject to the terms
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
 * $Id: InsertContent.java,v 1.1 2007/10/04 16:55:29 hengming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Extracts a portion of one file and insert into another file. The portion
 * to be extracted is defined in a Pattern; and the location to be inserted
 * is marked with a Pattern.
 */
public class InsertContent {

    /**
     * Creates an instance of this class.
     *
     */
    public InsertContent() {
    }

    /**
     * Inserts one file into another file.
     *
     * @param srcFileName source file name.
     * @param destFileName destination file name.
     * @param tag Tag in <code>destFile</code> where content is be inserted.
     * @throws IOException if IO operations failed.
     */
    public void inserts(String srcFileName, String destFileName, String tag)
        throws IOException {
        String srcContent = getFileContent(srcFileName);
        if (srcContent.length() > 0) {
            srcContent += "\n" + tag;
            String destContent = getFileContent(destFileName);
            destContent = destContent.replaceFirst(tag, srcContent);
            writeToFile(destFileName, destContent);
        }
    }


    private String getFileContent(String fileName)
        throws IOException {
        StringBuffer sb = new StringBuffer();
        FileReader in = null;

        try {
            in = new FileReader(fileName);
            BufferedReader buff = new BufferedReader(in);
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
            InsertContent it = new InsertContent();
            it.inserts(args[0], args[1], args[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

