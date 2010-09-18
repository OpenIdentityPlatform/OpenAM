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
 * $Id: JarUtility.java,v 1.1 2009/02/13 15:36:56 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.cli;

import java.io.File;
import java.util.logging.Level;

/**
 * A class used to executed commands with the jar utility.
 */
public class JarUtility extends CLIUtility {
    private static long commandTimeout = 100000;


    /** Creates a new instance of JarUtility */
    public JarUtility() {
        super(System.getProperty("java.home") + fileseparator + ".." +
                fileseparator + "bin" + fileseparator + "jar");
        StringBuffer jarBuff =
                new StringBuffer(System.getProperty("java.home")).
                append(fileseparator).append("..").append(fileseparator).
                append("bin").append(fileseparator).append("jar");
        String jarPath = jarBuff.toString();
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            log(Level.SEVERE, "JarUtility",
                    "The jar utility was not found at " + jarPath);
            assert false;
        }
    }

    /**
     * Expand an entire jar/war file to a destination directory.
     * warFile - a File object containing a jar or war file
     * destDir - a File object containing the destination directory in which
     *           the contents of zipFile should be extracted.
     * @return - the exit status of the jar command.
     */
    public int expandWar(File warFile, File destDir)
    throws Exception {
        String warFileName = warFile.getAbsolutePath();
        clearArguments(0);
        setWorkingDir(destDir);
        addArgument("xf");
        addArgument(warFileName);
        return(executeCommand(commandTimeout));
    }

    /**
     * Expand a specific file from entire jar or war file to a destination
     * directory.
     * warFile - a File object containing a jar or war file
     * destDir - a File object containing the destination directory in which
     *           the contents of warFile should be extracted.
     * fileToExtract - the specific file in the jar or war file which should be
     *           extracted.
     * @return - the exit status of the jar command.
     */
    public int extractFile(File warFile, File destDir, String fileToExtract)
    throws Exception {
        String warFileName = warFile.getAbsolutePath();
        clearArguments(0);
        setWorkingDir(destDir);
        addArgument("xf");
        addArgument(warFileName);
        addArgument(fileToExtract);
        return(executeCommand(commandTimeout));
    }

}
