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
 * $Id: InstallLog.java,v 1.2 2008/06/25 05:44:02 qcheng Exp $
 *
 */

package com.sun.identity.setup;

import com.iplanet.am.util.SystemProperties;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Installation Log
 */
public class InstallLog {
    private StringBuffer buff = null;
    private final static String FILENAME = "install.log";
    private static InstallLog instance = new InstallLog();
    
    public static InstallLog getInstance() {
        return instance;
    }
    
    private InstallLog() {
    }

    public synchronized void open() {
        buff = new StringBuffer();
    }
    
    public synchronized void close() {
        try {
            String baseDir = SystemProperties.get(
                SystemProperties.CONFIG_PATH);
            FileWriter fout = null;
            try {
                fout = new FileWriter(baseDir + "/" + FILENAME);
                fout.write(buff.toString());
                buff = null;
            } finally {
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Exception ex) {
                    //No handling requried
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public synchronized void write(String str) {
        if (buff != null) {
            buff.append(str);
        }
    }
    
    public synchronized void write(String str, Exception e) {
        if (buff != null) {
            StringWriter wr = new StringWriter();
            e.printStackTrace(new PrintWriter(wr));
            buff.append(str).append(wr.toString());
        }
    }
}
