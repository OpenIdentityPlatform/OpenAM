/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.db.opendj.setup;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.forgerock.openam.amsessionstore.db.opendj.OpenDJConfig;

/**
 *
 * @author steve
 */
public class SetupLog {
    private StringBuilder buff = null;
    private final static String FILENAME = "setup.log";
    private static SetupLog instance = new SetupLog();
    
    public static SetupLog getInstance() {
        return instance;
    }
    
    private SetupLog() {
    }

    public synchronized void open() {
        buff = new StringBuilder();
    }
    
    public synchronized void close() {
        try {
            String baseDir = OpenDJConfig.getOdjRoot();
            FileWriter fout = null;
            
            try {
                fout = new FileWriter(baseDir + "/logs/" + FILENAME);
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
