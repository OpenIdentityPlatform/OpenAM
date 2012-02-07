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
 * $Id: FileHelper.java,v 1.2 2008/06/25 05:44:23 qcheng Exp $
 *
 */

package com.sun.identity.test.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides helper methods for file related operations.
 */
public class FileHelper {
    
    /**
     * Creates a new instance of <code>FileHelper</code>
     */
    private FileHelper() {
    }
    
    /**
     * Returns byte array that contains the content of a file.
     *
     * @param fileName Name of file.
     * @return byte array that contains the content of a file.
     * @throws IOException if file is not found; or content cannot be read.
     */
    public static byte[] getBinary(String fileName) 
        throws IOException
    {
        byte[] content = null;
        InputStream in = null; 
        try {
            File f = new File(fileName);
            in = new FileInputStream(fileName);
            content = new byte[(int)f.length()];
            in.read(content);
        } finally {
            if (in != null) {
            in.close();
            }
        }
        return content;
    }
}
