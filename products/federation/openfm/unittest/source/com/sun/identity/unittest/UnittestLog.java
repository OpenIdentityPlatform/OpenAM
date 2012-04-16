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
 * $Id: UnittestLog.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */

package com.sun.identity.unittest;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Logging the test activities.
 */
public class UnittestLog {
    private static List buffer = new ArrayList();

    public static synchronized List flush(String datestamp) {
        List tmp = buffer;
        if (datestamp != null) {
            writeToFile(datestamp);
        }
        buffer = new ArrayList();
        return tmp;
    }

    private static void writeToFile(String datestamp) {
        String basedir = SystemProperties.get(SystemProperties.CONFIG_PATH) +
            SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        if (basedir.equals("nullnull")) {
            for(Object obj : buffer) {
                System.out.println(obj);
            }
            return;
        }
        File f = new File(basedir + "/unittest");
        f.mkdir();

        StringBuffer buff = new StringBuffer();

        for (Iterator i = buffer.iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            buff.append(s).append("\n");
        }

        writeToFile(basedir + "/unittest/" + datestamp, buff.toString());
    }

    private static void writeToFile(String fileName, String content) {
        FileWriter fout = null;
        try {
            fout = new FileWriter(fileName, true);
            fout.write(content);
        } catch (IOException e) {
             //No handling requried
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
    }


    public static synchronized void logMessage(String msg) {
        buffer.add("MESSAGE: " + msg);
    }

    public static synchronized void logError(String msg) {
        buffer.add("<font color=\"red\">ERROR: " + msg + "</font>");
    }

    public static synchronized void logError(String err, Throwable e) {
        buffer.add("<font color=\"red\">ERROR: "  + err + " exception: " +
            e.getMessage() + "</font>");
    }
}

