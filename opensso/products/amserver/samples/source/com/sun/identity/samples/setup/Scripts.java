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
 * $Id: Scripts.java,v 1.3 2008/06/25 05:41:14 qcheng Exp $
 *
 */

package com.sun.identity.samples.setup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is used in Ant to append chmod command to the setup.sh file.
 */
public class Scripts {
    private static final String SH_CHMOD = "/scripts/chmod.sh";

    public Scripts(String basedir)
        throws IOException
    {
        File dir = new File(basedir + "/scripts");
        File[] shFiles = dir.listFiles(new ScriptFileFilter());

        BufferedWriter fout = new BufferedWriter(new FileWriter(
            basedir + SH_CHMOD));

        for (int i = 0; i < shFiles.length; i++) {
            File file = shFiles[i];
            String sh = file.getName();
            if (!sh.equals("setup.sh") && !sh.equals("chmod.sh")) {
                fout.write("chmod 744 ./scripts/" + sh + "\n");
            }
        }

        fout.close();
    }

    public static void main(String args[]) {
        try {
            new Scripts(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
